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
includeTargets << grailsScript("_GrailsArgParsing")
includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << new File("${gwtPluginDir}/scripts/_GwtCreate.groovy")

USAGE = """
    create-gwt-i18n [--constants-only] [--messages-only] MODULEPKG.MODULENAME

where
    --constants-only = Only create a <module>Constants.properties file.
    --messages-only  = Only create a <module>Messages.properties file.
    MODULEPKG        = The package name of the associated GWT module.
    MODULENAME       = The name of the module.
"""

/**
 * grails create-gwt-i18n MODULE
 */
target(default: "Creates a new i18n properties file for a GWT module.") {
    depends(parseArguments)
    promptForName(type: "")

    def i18nSuffixes = ["Constants", "Messages"]
    if (argsMap["constants-only"] && argsMap["messages-only"]) {
        println "You can only specify one of 'constants-only' and 'messages-only' - they are mutually exclusive."
        return 1
    } else if (argsMap["constants-only"]) {
        i18nSuffixes.remove("Messages")
    } else if (argsMap["messages-only"]) {
        i18nSuffixes.remove("Constants")
    }

    // There must be one and only one argument in 'params'.
    def params = argsMap["params"]
    if (!params || params.size() > 1) {
        println "Unexpected number of command arguments."
        println()
        println "USAGE:${USAGE}"
        exit(1)
    } else if (!params[0]) {
        println "A module name must be given."
        exit(1)
    }

    // If we only have one argument, we must split it into package and
    // name parts. Otherwise, we just use the provided arguments as is.
    def (modulePackage, moduleName) = packageAndName(params[0])

    // Now create the properties file(s).
    def pkg = modulePackage + ".client"
    i18nSuffixes.each { suffix ->
        installGwtTemplate(pkg, moduleName, "Gwt${suffix}.properties")
    }
}
