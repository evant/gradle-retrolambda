package me.tatarka

import com.android.build.api.transform.*
import com.google.common.collect.ImmutableMap
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.reflect.DirectInstantiator

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

        for (TransformInput input : inputs) {
            def outputDir = outputProvider.getContentLocation("retrolambda", outputTypes, scopes, Format.DIRECTORY)

            // Instead of looping, it might be better to figure out a way to pass multiple input
            // dirs into retrolambda. Luckily, the common case is only one.
            for (DirectoryInput directoryInput : input.directoryInputs) {
                File inputFile = directoryInput.file
                FileCollection changed
                if (isIncremental) {
                    changed = project.files()
                    for (Map.Entry<File, Status> entry : directoryInput.changedFiles) {
                        File file = entry.key; Status status = entry.value
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

    private FileCollection getClasspath(File inputFile, Collection<TransformInput> referencedInputs) {
        String buildName = inputFile.name
        String flavorName = inputFile.parentFile.name

        // If either one starts with a number or is 'folders', it's probably the result of a transform, keep moving
        // up the dir structure until we find the right folders.
        // Yes I know this is bad, but hopefully per-variant transforms will land soon.
        File current = inputFile
        while (Character.isDigit(buildName.charAt(0)) || Character.isDigit(flavorName.charAt(0)) || buildName.equals("folders") || flavorName.equals("folders")) {
            current = current.parentFile
            buildName = current.name
            flavorName = current.parentFile.name
        }

        def javaCompileTask = javaCompileTasks.get(flavorName + '/' + buildName)
        if (javaCompileTask == null) {
            // Flavor might not exist
            javaCompileTask = javaCompileTasks.get(buildName)
        }

        def classpathFiles = javaCompileTask.classpath
        for (TransformInput input : referencedInputs) {
             classpathFiles += project.files(input.directoryInputs*.file) 
        }

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
        return Collections.<QualifiedContent.ContentType>singleton(QualifiedContent.DefaultContentType.CLASSES)
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
