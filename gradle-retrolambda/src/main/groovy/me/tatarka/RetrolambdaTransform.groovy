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
import org.gradle.api.file.DeleteSpec
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.file.archive.ZipFileTree
import org.gradle.api.internal.file.collections.FileTreeAdapter
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.compile.JavaCompile

import java.nio.file.Paths

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

        RetrolambdaArgs args = new RetrolambdaArgs(referencedInputs)
        args.outputDir = outputProvider.getContentLocation("retrolambda", outputTypes, scopes, Format.DIRECTORY)

        for (TransformInput input : inputs) {
            for (JarInput jarInput : input.getJarInputs()) {
                println("jar input: ${jarInput.file} status:${jarInput.status} incremental: ${isIncremental}")
                File file = jarInput.file
                if (jarInput.status != NOTCHANGED) {
                    FileTree zipTree = project.zipTree(file)
                    zipTree.each {
                        File output = toOutput(zipTreeRoot(zipTree), args.outputDir, new File(it.path))
                        if (output.exists()) {
                            output.delete()
                            deleteRelated(output)
                        }
                    }
                }
                if (isIncremental) {
                    if (jarInput.status == ADDED || jarInput.status == CHANGED) {
                        args.jars.add(file)
                    }
                } else {
                    args.jars.add(file)
                }
            }

            for (DirectoryInput directoryInput : input.directoryInputs) {
                if (args.inputDir != null) {
                    // Retrolambda can only support 1 input dir, just run it with what we got.
                    runRetrolambda(args)
                    args = new RetrolambdaArgs(referencedInputs)
                }
                args.inputDir = directoryInput.file
                if (isIncremental) {
                    for (Map.Entry<File, Status> entry : directoryInput.changedFiles) {
                        File file = entry.key; Status status = entry.value
                        if (status == ADDED || status == CHANGED) {
                            args.includedFiles.add(file)
                        }
                        if (status == CHANGED || status == REMOVED) {
                            File output = toOutput(args.inputDir, args.outputDir, file)
                            output.delete()
                            deleteRelated(output)
                        }
                    }
                }
            }
        }

        runRetrolambda(args)
    }

    private void runRetrolambda(RetrolambdaArgs args) {
        RetrolambdaExec exec = new RetrolambdaExec(project)
        exec.inputDir = args.inputDir
        exec.outputDir = args.outputDir
        exec.bytecodeVersion = javaVersionToBytecode(retrolambda.javaVersion)
        exec.classpath = getClasspath(args.outputDir, args.referencedInputs) + project.files(args.inputDir)
        if (!args.includedFiles.isEmpty()) {
            exec.includedFiles = project.files(args.includedFiles)
        }
        if (!args.jars.isEmpty()) {
            exec.jars = project.files(args.jars)
        }
        exec.defaultMethods = retrolambda.defaultMethods
        exec.jvmArgs = retrolambda.jvmArgs
        exec.exec()
    }

    private static File toOutput(File inputDir, File outputDir, File file) {
        return outputDir.toPath().resolve(inputDir.toPath().relativize(file.toPath())).toFile()
    }

    private static File zipTreeRoot(FileTree files) {
        //TODO: is there a way to do this without internal apis?
        return (((FileTreeAdapter) files).tree as ZipFileTree).mirror.dir
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
        EnumSet<QualifiedContent.Scope> set = EnumSet.of(
                QualifiedContent.Scope.PROJECT,
                QualifiedContent.Scope.SUB_PROJECTS
        )
        if (retrolambda.processLibraries) {
            set.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
            set.add(QualifiedContent.Scope.PROJECT_LOCAL_DEPS)
            set.add(QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS)
        }
        return set
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
                .put("processLibraries", retrolambda.processLibraries)
                .put("jdk", retrolambda.tryGetJdk())
                .build()
    }

    @Override
    public boolean isIncremental() {
        return retrolambda.incremental
    }

    private static class RetrolambdaArgs {
        final Collection<TransformInput> referencedInputs

        File inputDir
        File outputDir
        List<File> includedFiles = new ArrayList<>()
        List<File> jars = new ArrayList<>()

        RetrolambdaArgs(Collection<TransformInput> referencedInputs) {
            this.referencedInputs = referencedInputs
        }
    }
}
