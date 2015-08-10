package me.tatarka

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertNotNull

class RetrolambdaGroovyPluginTest {
    @Test
    void testCreateTask() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'groovy'
        project.apply plugin: 'me.tatarka.retrolambda'
        project.evaluate()
        assertNotNull project.tasks.findByName('compileRetrolambdaMain')
    }
    
    @Test
    void testCreateSourceSetTask() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'groovy'
        project.apply plugin: 'me.tatarka.retrolambda'
        project.sourceSets {
            other
        }
        project.evaluate()
        assertNotNull project.tasks.findByName('compileRetrolambdaOther')
    }
}