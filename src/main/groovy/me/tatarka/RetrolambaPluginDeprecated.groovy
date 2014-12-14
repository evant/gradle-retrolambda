package me.tatarka;

import org.gradle.api.Project

/**
 * Created by evan on 12/11/14.
 */
class RetrolambaPluginDeprecated extends RetrolambdaPlugin {
    @Override
    public void apply(Project project) {
        project.logger.warn("plugin name 'retrolambda' is deprecated and will be removed in the next release, please change to 'me.tatarka.retrolambda'")
        super.apply(project);
    }
}
