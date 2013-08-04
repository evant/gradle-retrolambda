package me.tatarka

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created with IntelliJ IDEA.
 * User: evan
 * Date: 8/4/13
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class RetrolambdaPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('retrolambda', RetrolambdaExtension)

        project.configurations {
            retrolambdaConfig
        }

        project.dependencies {
            retrolambdaConfig project.retrolambda.compile
        }

        project.task('compileRetrolambda', dependsOn: project.tasks.matching {task ->
            !task.name.equals('compileRetrolambda') && task.name.startsWith('compileRetrolambda')
        })  {
            description = "Converts all java 8 class files to java 6 or 7"
        }

        if (project.plugins.hasPlugin('java')) {
            project.apply plugin: RetrolambdaPluginJava
        }

        if (project.plugins.hasPlugin('android')) {
            project.apply plugin: RetrolambdaPluginAndroid
        }

        if (project.plugins.hasPlugin('application')) {
            project.tasks.findByName('run').dependsOn('compileRetrolambda')
        }
    }

}
