package me.tatarka
import com.android.build.api.transform.*
import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.compile.JavaCompile

import static com.android.build.api.transform.Status.*
import static me.tatarka.RetrolambdaPlugin.javaVersionToBytecode
/**
 * Transform java 8 class files to java 5, 6, or 7 use retrolambda
 */
@CompileStatic
class RetrolambdaTransform extends Transform {

    private final Project project
    private final RetrolambdaExtension retrolambda
    private final Map<String, JavaCompile> javaCompileTasks = new HashMap<>()

    public RetrolambdaTransform(Project project, RetrolambdaExtension retrolambda) {
        this.project = project
        this.retrolambda = retrolambda
    }

    /**
     * We need to set this later because the classpath is not fully calculated until the last
     * possible moment when the java compile task runs. While a Transform currently doesn't have any
     * variant information, we can guess the variant based off the input path.
     */
    public void putJavaCompileTask(String dirName, JavaCompile javaCompileTask) {
        javaCompileTasks.put(dirName, javaCompileTask)
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        context.logging.captureStandardOutput(LogLevel.INFO)

        inputs.each { TransformInput input ->
            def outputDir = outputProvider.getContentLocation("retrolambda", outputTypes, scopes, Format.DIRECTORY)

            // Instead of looping, it might be better to figure out a way to pass multiple input
            // dirs into retrolambda. Luckily, the common case is only one.
            input.directoryInputs.each { DirectoryInput directoryInput ->
                File inputFile = directoryInput.file
                FileCollection changed
                if (isIncremental) {
                    changed = project.files()
                    directoryInput.changedFiles.each { File file, Status status ->
                        if (status == ADDED || status == CHANGED) {
                            changed += project.files(file);
                        }
                        if (status == CHANGED || status == REMOVED) {
                            deleteRelated(toOutput(inputFile, outputDir, file))
                        }
                    }
                } else {
                    changed = null
                }

                def exec = new RetrolambdaExec(project)
                exec.inputDir = inputFile
                exec.outputDir = outputDir
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
        file.parentFile.eachFile {
            if (it.name.matches(/$className\$\$/ + /Lambda.*\.class$/)) {
                it.delete()
            }
        }
    }

    /**
     * @see com.android.builder.core.VariantConfiguration#getDirName()
     */
    private JavaCompile findJavaCompileForInput(File inputDir) {

        String dirName = null
        // always use forward slashes
        for (File dir = inputDir; dir != null; dir = dir.parentFile) {
            String name0 = dir.name
            def parent0 = dir.parentFile
            if (parent0 != null) {
                String name1 = "$parent0.name/$name0"
                def parent1 = parent0.parentFile
                if (parent1 != null) {
                    String name2 = "$parent1.name/$name1"
                    if (javaCompileTasks.containsKey(name2)) {
                        dirName = name2
                        break;
                    }
                }
                if (javaCompileTasks.containsKey(name1)) {
                    dirName = name1
                    break;
                }
            }
            if (javaCompileTasks.containsKey(name0)) {
                dirName = name0
                break;
            }
        }

        if (dirName == null) {
            throw new ProjectConfigurationException("Unable to determine the build variant for $inputDir", null);
        }

        return javaCompileTasks.get(dirName)
    }

    private FileCollection getClasspath(File inputFile, Collection<TransformInput> referencedInputs) {

        def javaCompileTask = findJavaCompileForInput(inputFile)

        def classpathFiles = javaCompileTask.classpath
        referencedInputs.each { TransformInput input -> classpathFiles += project.files(input.directoryInputs*.file) }

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
    Set<QualifiedContent.ContentType> getInputTypes() {
        return Collections.singleton(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return Collections.singleton(QualifiedContent.Scope.PROJECT)
    }

    @Override
    Set<QualifiedContent.Scope> getReferencedScopes() {
        return Collections.singleton(QualifiedContent.Scope.TESTED_CODE)
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
