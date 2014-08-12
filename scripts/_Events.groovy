/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
includeTargets << new File("${gwtPluginDir}/scripts/_GwtInternal.groovy")

eventGwtCompileStart = {
    compileGwtClasses()
}

void compileGwtClasses() {
    // Hack to work around an issue in Google Gin:
    //
    //    http://code.google.com/p/google-gin/issues/detail?id=36
    //
    ant.mkdir(dir: gwtClassesDir)
    gwtJavac(destDir: gwtClassesDir, includes: "**/*.java") {
        src(path: 'src/gwt')//current project gwt modules
        //include any sources from any included plugins
        buildConfig?.gwt?.plugins?.each { pluginName ->
            def pluginDir = binding.variables["${pluginName}PluginDir"]
            if (pluginDir && new File("${pluginDir}/src/gwt").exists()) {
                src(path: "${pluginDir}/src/gwt")
            }
        }
        ant.classpath {
            gwtResolvedDependencies.each { File f ->
                pathElement(location: f.absolutePath)
            }

            if (gwtLibFile.exists()) {
                fileset(dir: gwtLibPath) {
                    include(name: "*.jar")
                }
            }

            pathElement(location: grailsSettings.classesDir.path)

            // Fix to get this working with Grails 1.3+. We have to
            // add the directory where plugin classes are compiled
            // to. Pre-1.3, plugin classes were compiled to the same
            // directory as the application classes.
            if (grailsSettings.metaClass.hasProperty(grailsSettings, "pluginClassesDir")) {
                pathElement(location: grailsSettings.pluginClassesDir.path)
            }
        }
    }
}