/*
 * Copyright 2009 the original author or authors.
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

/**
 * @author <a href='mailto:p.ledbrook@cacoethes.co.uk'>Peter Ledbrook</a>
 * @author Predrag Knežević
 * @author Milan Skuhra
 * @author <a href='mailto:david.dawson@dawsonsystems.com'>David Dawson</a>
 * @author John Rellis
 * @author <a href='mailto:donbeave@gmail.com'>Alexey Zhokhov</a>
 */
try {
    includeTargets << new File(gwtPluginDir, 'scripts/_GwtInternal.groovy')
} catch (e) {
}

eventGwtCompileStart = {
    compileGwtClasses()
}

eventGwtRunHostedStart = {
    compileGwtClasses()
}

eventCompileStart = {
    addDependenciesToClasspath()
}

eventConfigureWarNameEnd = {
    compileGwtModules()
}

//
// The GWT libs must be copied to the WAR file. In addition, although
// we don't do dynamic compilation in production mode, the plugin
// groovy class gets compiled with the UnableToCompleteException in
// the class file. Thus, we also have to include this particular file
// in the system.
//
eventCreateWarStart = { warName, stagingDir ->
    if (gwtResolvedDependencies) {
        def gwtDevJar = gwtResolvedDependencies.find { it.name.contains('gwt-dev') }

        // Extract the UnableToCompleteException file from gwt-dev-*.jar
        ant.unjar(dest: "${stagingDir}/WEB-INF/classes") {
            patternset(includes: 'com/google/gwt/core/ext/UnableToCompleteException.class')
            path(location: gwtDevJar.absolutePath)
        }
    }

}
