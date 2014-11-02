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
includeTargets << grailsScript('_GrailsArgParsing')
includeTargets << grailsScript('_GrailsCreateArtifacts')
includeTargets << new File("${gwtPluginDir}/scripts/_GwtCreate.groovy")

USAGE = """
    create-gwt-event MODULEPKG.EVENTNAME
    create-gwt-event MODULEPKG [SUBPKG] EVENTNAME

where
    MODULEPKG = The package name of the event's GWT module.
    SUBPKG    = The name of an optional sub-package in which the event
                class will go, which will be a sub-package of "client".
    EVENTNAME = The name of the event, without the "Event" suffix.
"""

target(default: 'Creates a new GWT application event with associated handler.') {
    depends(parseArguments)
    promptForName(type: "")

    // We support either one argument or three.
    def params = argsMap['params']
    if (!params || params.size() > 3) {
        println 'Unexpected number of command arguments.'
        println()
        println "USAGE:${USAGE}"
        exit(1)
    } else if (!params[0]) {
        println 'An event name must be given.'
        exit(1)
    }

    // If we only have one argument, we must split it into package and
    // name parts. Otherwise, we just use the provided arguments as is.
    def modulePackage, eventName
    def subPackage = ""
    if (params.size() == 1) {
        (modulePackage, eventName) = packageAndName(params[0])
    } else {
        modulePackage = params[0]
        if (params.size() == 2) {
            eventName = params[1]
        } else {
            subPackage = '.' + params[1]
            eventName = params[2]
        }
    }

    // Now create the event file.
    def eventPackage = "${modulePackage}.client${subPackage}"
    installGwtTemplate(eventPackage, eventName, 'GwtEvent.java')

    // Now for the handler file.
    installGwtTemplate(eventPackage, eventName, 'GwtHandler.java')
}
