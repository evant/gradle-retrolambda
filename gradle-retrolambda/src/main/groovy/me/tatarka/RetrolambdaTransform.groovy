/**
 Copyright 2016 Evan Tatarka

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

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
                exec.classpath = getClasspath(outputDir, referencedInputs) + project.files(inputFile)
                exec.includedFiles = changed
                exec.defaultMethods = retrolambda.defaultMethods
                exec.jvmArgs = retrolambda.jvmArgs
                exec.quiet = retrolambda.quiet
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

    private FileCollection getClasspath(File outputDir, Collection<TransformInput> referencedInputs) {
        // Extract the variant from the output path assuming it's in the form like:
        // - '*/intermediates/transforms/retrolambda/<VARIANT>
        // - '*/intermediates/transforms/retrolambda/<VARIANT>/folders/1/1/retrolambda
        // This will no longer be needed when the transform api supports per-variant transforms
        String[] parts = outputDir.toURI().path.split('/intermediates/transforms/retrolambda/|/folders/[0-9]+')

        if (parts.length < 2) {
            throw new ProjectConfigurationException('Could not extract variant from output dir: ' + outputDir, null)
        }

        String variantName = parts[1];
        def javaCompileTask = javaCompileTasks.get(variantName)

        if (javaCompileTask == null) {
            throw new ProjectConfigurationException('Missing javaCompileTask for variant: ' + variantName + ' from output dir: ' + outputDir, null)
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
        return Collections.<QualifiedContent.ContentType> singleton(QualifiedContent.DefaultContentType.CLASSES)
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
                .put("quiet", retrolambda.quiet)
                .build();
    }

    @Override
    public boolean isIncremental() {
        return retrolambda.incremental
    }
}
