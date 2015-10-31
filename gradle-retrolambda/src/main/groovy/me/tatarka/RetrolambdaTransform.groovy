package me.tatarka

import com.android.build.transform.api.*
import com.android.ddmlib.Log
import com.android.utils.Pair
import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.compile.JavaCompile

import static com.android.build.transform.api.TransformInput.FileStatus.*
import static me.tatarka.RetrolambdaPlugin.javaVersionToBytecode

/**
 * Transform java 8 class files to java 5, 6, or 7 use retrolambda
 */
@CompileStatic
class RetrolambdaTransform extends Transform implements AsInputTransform {

    private final Project project
    private final RetrolambdaExtension retrolambda
    private final Map<Pair<String, String>, JavaCompile> javaCompileTasks = new HashMap<>()

    public RetrolambdaTransform(Project project, RetrolambdaExtension retrolambda) {
        this.project = project
        this.retrolambda = retrolambda
    }

    /**
     * We need to set this later because the classpath is not fully calculated until the last
     * possible moment when the java compile task runs. While a Transform currently doesn't have any
     * variant information, we can guess the variant based off the input path.
     */
    public void putJavaCompileTask(String flavorName, String buildTypeName, JavaCompile javaCompileTask) {
        javaCompileTasks.put(Pair.of(flavorName, buildTypeName), javaCompileTask)
    }

    @Override
    void transform(Context context, Map<TransformInput, TransformOutput> inputs, Collection<TransformInput> referencedInputs, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        context.logging.captureStandardOutput(LogLevel.INFO)
        project.logger.debug("running transform: " + name);

        inputs.each { TransformInput input, TransformOutput output ->
            // Instead of looping, it might be better to figure out a way to pass multiple input
            // dirs into retrolambda. Luckily, the common case is only one.
            input.files.each { File inputFile ->
                FileCollection changed
                if (isIncremental) {
                    changed = project.files()
                    input.changedFiles.each { File file, TransformInput.FileStatus status ->
                        project.logger.debug("input (incremental): " + file);
                        if (status == ADDED || status == CHANGED) {
                            changed += project.files(file);
                        }
                        if (status == CHANGED || status == REMOVED) {
                            deleteRelated(toOutput(inputFile, output.outFile, file))
                        }
                    }
                } else {
                    changed = null
                    input.files.each { File file ->
                        project.logger.debug("input: " + file);
                    }
                }

                project.logger.debug("output: " + output.outFile);

                def exec = new RetrolambdaExec(project)
                exec.inputDir = inputFile
                exec.outputDir = output.outFile
                exec.bytecodeVersion = javaVersionToBytecode(retrolambda.javaVersion)
                exec.classpath = getClasspath(inputFile, referencedInputs) + project.files(inputFile)
                exec.includedFiles = changed
                exec.defaultMethods = retrolambda.defaultMethods
                exec.jvmArgs = retrolambda.jvmArgs
                exec.exec()
            }
        }
    }

    private static File toOutput(File inputDir, File outputDir, File file) {
        return outputDir.toPath().resolve(inputDir.toPath().relativize(file.toPath())).toFile()
    }

    private void deleteRelated(File file) {
        def className = file.name.replaceFirst(/\.class$/, '')
        // Delete any generated Lambda classes
        project.logger.debug("Deleting related for " + className + " in " + file.parentFile)
        file.parentFile.eachFile {
            if (it.name.matches(/$className\$\$/ + /Lambda.*\.class$/)) {
                project.logger.debug("Deleted " + it)
                it.delete()
            }
        }
    }

    private FileCollection getClasspath(File inputFile, Collection<TransformInput> referencedInputs) {
        def buildName = inputFile.name
        def flavorName = inputFile.parentFile.name

        def javaCompileTask = javaCompileTasks.get(Pair.of(flavorName, buildName))
        if (javaCompileTask == null) {
            // Flavor might not exist
            javaCompileTask = javaCompileTasks.get(Pair.of("", buildName))
        }

        def classpathFiles = javaCompileTask.classpath
        referencedInputs.each { TransformInput input -> classpathFiles += project.files(input.files) }

        // bootClasspath isn't set until the last possible moment because it's expensive to look
        // up the android sdk path.
        def bootClasspath = javaCompileTask.options.bootClasspath
        if (bootClasspath) {
            classpathFiles += project.files(bootClasspath.tokenize(File.pathSeparator))
        } else {
            // If this is null it means the javaCompile task didn't need to run, however, we still
            // need to run but can't without the bootClasspath. Just fail and ask the user to rebuild.
            throw new ProjectConfigurationException("Unable to obtain the bootClasspath. This may happen if your javaCompile tasks didn't run but retrolambda did. You must rebuild your project or otherwise force javaCompile to run.", null)
        }
        return classpathFiles
    }

    @Override
    public String getName() {
        return "retrolambda"
    }

    @Override
    public Set<ScopedContent.ContentType> getInputTypes() {
        return Collections.singleton(ScopedContent.ContentType.CLASSES)
    }

    @Override
    public Set<ScopedContent.ContentType> getOutputTypes() {
        return Collections.singleton(ScopedContent.ContentType.CLASSES)
    }

    @Override
    public Set<ScopedContent.Scope> getScopes() {
        return Collections.singleton(ScopedContent.Scope.PROJECT)
    }

    @Override
    public Set<ScopedContent.Scope> getReferencedScopes() {
        return Collections.singleton(ScopedContent.Scope.TESTED_CODE)
    }

    @Override
    public ScopedContent.Format getOutputFormat() {
        return ScopedContent.Format.SINGLE_FOLDER
    }

    @Override
    public Map<String, Object> getParameterInputs() {
        return ImmutableMap.<String, Object> builder()
                .put("bytecodeVersion", retrolambda.bytecodeVersion)
                .put("jvmArgs", retrolambda.jvmArgs)
                .put("incremental", retrolambda.incremental)
                .put("defaultMethods", retrolambda.defaultMethods)
                .put("jdk", retrolambda.tryGetJdk())
                .build();
    }

    @Override
    public boolean isIncremental() {
        return retrolambda.incremental
    }
}
