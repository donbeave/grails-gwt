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

// Create the directory for storing GWT files.
ant.mkdir(dir: "${basedir}/assets/javascript/gwt")

// add gwt-user.jar and the others to compile dependencies 
// otherwise it is not possible to compile plugin classes
// if installing plugin is just a part of some other grails workflow (i.e. testing)

// updating classpath might fail for an unknown reason at the moment
// therefore wrap it in try-catch block
// just to avoid stoping the install process.
// Updating classpath here is only important
// if the plugin installation is happening within some other task
// that requires code compilation
// this could be avoided by doing 'grails refresh-dependencies' first
try {
    includeTargets << new File("${gwtPluginDir}/scripts/_GwtInternal.groovy")
} catch (Throwable e) {
    // show the error message and the stacktrace
    println 'error by installing gwt plugin: ' + e.message
    e.printStackTrace()
}