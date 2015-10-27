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
import grails.converters.XML
import grails.util.GrailsNameUtils as GCU
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo

includeTargets << grailsScript('_GrailsArgParsing')
includeTargets << grailsScript('_GrailsCreateArtifacts')
includeTargets << grailsScript('_GrailsCompile')
includeTargets << new File("${gwtPluginDir}/scripts/_GwtCreate.groovy")

/**
 * @author <a href='mailto:p.ledbrook@cacoethes.co.uk'>Peter Ledbrook</a>
 * @author <a href='mailto:alexey@zhokhov.com'>Alexey Zhokhov</a>
 */
target(default: 'Creates a new GSP page for hosting a GWT UI.') {
    depends(compile, parseArguments)

    // This script takes multiple arguments (in fact, at least two),
    // so split the given string into separate parameters, using
    // whitespace as the delimiter.
    def argArray = argsMap['params']

    if (argArray.size() < 1) {
        println 'At least two arguments must be given to this script.'
        println()
        println 'USAGE: create-gwt-page GSPFILE [MODULE]'
        exit(1)
    }

    // Location of the template page.
    def templatePath = "${gwtPluginDir}/src/templates/artifacts"
    def templateFile = "${templatePath}/GwtHostPage.gsp"

    // Now look at the first argument, which should be the location
    // of the page to create.
    def targetFile = null
    def m = argArray[0] =~ /(\w+)[\/\\]\w+\.gsp/
    if (m.matches()) {
        // The first group is the controller name, the second is the
        // view file. Does the controller already exist?
        def controllerName = GCU.getClassNameRepresentation(m[0][1]) + 'Controller'
        if (!new File("${basedir}/grails-app/controllers/${controllerName}.groovy").exists()) {
            // Controller doesn't exist - does the user want to create
            // it?
            ant.input(
                    addProperty: "${controllerName}.auto.create",
                    message: "${controllerName} does not exist - do you want to create it now? [y/n]")

            if (ant.antProject.properties."${controllerName}.auto.create" == 'y') {
                // User wants to create the controller, so do so.
                createArtifact(name: m[0][1], suffix: 'Controller', type: 'Controller', path: 'grails-app/controllers')
                createUnitTest(name: m[0][1], suffix: 'Controller', superClass: 'ControllerUnitTestCase')
                ant.mkdir(dir: "${basedir}/grails-app/views/${propertyName}")
            }
        }

        // The target file is written into the 'views' directory.
        targetFile = "grails-app/views/${argArray[0]}"
    } else {
        // Create the file in 'web-app'.
        targetFile = "web-app/${argArray[0]}"
    }

    def GWTCompiler = classLoader.loadClass('grails.plugin.gwt.GWTCompiler')

    String module = argArray.size() > 1 ? argArray[1] : null

    if (!module) {
        def modules = GWTCompiler.findModules("${basedir}/${gwtSrcPath}", true)

        if (!modules) {
            println 'Does not find any GWT module.'
            exit(1)
        }

        modules.eachWithIndex { def entry, int i ->
            println "${i + 1}) ${entry}"
        }

        ant.input(addProperty: 'moduleIndex',
                message: 'Please select one module from list by input position number or full name\n')

        try {
            if (moduleIndex.toInteger())
                module = modules.get(moduleIndex.toInteger() - 1)
        } catch (NumberFormatException e) {
            module = moduleIndex
        }
    }

    try {
        def override =
                XML.parse(new FileInputStream("$basedir/$gwtSrcPath/${packageToPath(module)}.gwt.xml"), 'UTF-8').@'rename-to'

        if (override && !override.equals(''))
            module = override
    } catch (FileNotFoundException e) {
        println e.message
        exit(1)
    }

    // Copy the template file to the target location.
    ant.copy(file: templateFile, tofile: targetFile, overwrite: true)

    String nocacheFile = "gwt/${module}/${module}.nocache.js"
    String injector = '<script type="text/javascript"\n' +
            '            src="${resource(dir: \'js/' + nocacheFile + '\')}?${new Date().time}\"></script>\n'

    // check for Asset-Pipeline plugin
    for (GrailsPluginInfo info in pluginSettings.getPluginInfos()) {
        if (info.name.equals('asset-pipeline')) {
            injector = '<g:if test="${!grails.util.Environment.isDevelopmentMode()}">\n' +
                    '        <meta name="gwt:property"\n              content="baseUrl=//${grails.util.Holders.grailsApplication.mainContext.getBean(\'grailsLinkGenerator\').serverBaseURL}/gwt/' + module + '/"/>\n' +
                    '    </g:if>\n' +
                    "    <asset:script src=\"${nocacheFile}\"/>"
        }
    }

    // Replace the tokens in the template.
    ant.replace(file: targetFile) {
        replacefilter(token: '@gwt.injector@', value: injector)
    }

    event('CreatedFile', [targetFile])
}