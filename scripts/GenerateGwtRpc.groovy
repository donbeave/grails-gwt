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
includeTargets << grailsScript("_GrailsBootstrap")

getInput = { String message, String code = "confirm.message" ->
    ant.input(message: message, addproperty: code, validargs: "y,n,a,s")
    return ant.project.getProperty(code)
}

target(default: "Generates the GWT RPC client interfaces for specified services (or all GWT services).") {
    depends(parseArguments, bootstrap)

    def interfaceGenerator = grailsApp.mainContext.getBean("gwtInterfaceGenerator")
    def skipAll = false
    def forceCreate = argsMap["force"]

    grailsApp.serviceClasses.each { serviceWrapper ->
        if (skipAll) return

        // Skip any services that aren't exposed to GWT.
        if (!interfaceGenerator.isGwtExposed(serviceWrapper.clazz)) return

        if (interfaceGenerator.getInterfacesExist(serviceWrapper.clazz)) {
            if (!isInteractive) {
                println "Skipping service $serviceWrapper.shortName: you're in non-interactive mode"
                return
            }

            if (!forceCreate) {
                ant.input(message: "GWT interfaces exist already for service ${serviceWrapper.shortName}." +
                        " Would you like to overwrite them ( y = yes, n = no, a = all, q = quit )?",
                        addproperty: "gwt.generate.confirm",
                        validargs: "y,n,a,q")

                switch (ant.project.getProperty("gwt.generate.confirm")) {
                    case "a":
                        forceCreate = true
                        break

                    case "y":
                        break

                    case "q":
                        skipAll = true
                        return

                    case "n":
                        println "Skipping service $serviceWrapper.shortName"
                        return
                }
            }
        }

        // Generate GWT client interfaces for this service.
        println "Generating GWT interfaces for service ${serviceWrapper.shortName}"
        interfaceGenerator.generateInterfaces(serviceWrapper.clazz)
    }
}
