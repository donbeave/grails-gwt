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
import grails.util.BuildSettings
import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.resolve.Dependency

// No point doing this stuff more than once.
if (getBinding().variables.containsKey('_gwt_internal_called')) return
_gwt_internal_called = true

includeTargets << grailsScript('_GrailsInit')

// The targets in this script assume that Init has already been loaded.
// By not explicitly including Init here, we can use this script from
// the Events script.

if (!(getBinding().variables.containsKey('gwtModuleList'))) {
    gwtModuleList = null
}

// The targets in this script assume that Init has already been loaded.
// By not explicitly including Init here, we can use this script from
// the Events script.

// Common properties and closures (used as re-usable functions).
ant.property(environment: 'env')

gwtResolvedDependencies = []
gwtDependencies = []
gwtSrcPath = 'src/gwt'
gwtClassesDir = new File(grailsSettings.projectWorkDir, 'gwtclasses')
gwtJavaCmd = getPropertyValue('gwt.java.cmd', null)
gwtJavacCmd = getPropertyValue('gwt.javac.cmd', null)
gwtOutputPath = getPropertyValue('gwt.output.path', "${basedir}/web-app/gwt")
gwtOutputStyle = getPropertyValue('gwt.output.style', 'OBF')
gwtDisableCompile = getPropertyValue('gwt.compile.disable', 'false').toBoolean()
gwtLibPath = "$basedir/lib/gwt"
gwtLibFile = new File(gwtLibPath)
gwtPluginLibPath = "$gwtPluginDir/lib/gwt"
gwtPluginLibFile = new File(gwtPluginLibPath)
gwtDebugMode = getPropertyValue('gwt.debug', 'false').toBoolean()

grailsSrcPath = 'src/java'

compilerClass = 'com.google.gwt.dev.Compiler'
devModeClass = 'com.google.gwt.dev.DevMode'
codeServerClass = 'com.google.gwt.dev.codeserver.CodeServer'

//
// A target for compiling any GWT modules defined in the project.
//
// Options:
//
//   gwtModuleList - A collection or array of modules that should be compiled.
//                   If this is null or empty, all the modules in the
//                   application will be compiled.
//
target(compileGwtModules: "Compiles any GWT modules in '$gwtSrcPath'.") {
    if (gwtDisableCompile) return

    def GWTCompiler = classLoader.loadClass('org.grails.plugin.gwt.GWTCompiler')

    // Make sure that the I18n properties files are compiled before
    // the modules are, unless we're not keeping the properties files
    // in sync with the Java interfaces.
    if (buildConfig.gwt.sync.i18n instanceof Map || buildConfig.gwt.sync.i18n == true) {
        //compileI18n()
    }

    // Draft compilation.
    if (!(getBinding().variables.containsKey('gwtDraftCompile'))) {
        gwtDraftCompile = null
    }

    if (gwtDraftCompile == null) {
        gwtDraftCompile = getPropertyValue('gwt.draft.compile', false).toBoolean()
    }

    // This triggers the Events scripts in the application and plugins.
    event('GwtCompileStart', ['Starting to compile the GWT modules.'])

    def modules = gwtModuleList ?: GWTCompiler.findModules("${basedir}/${gwtSrcPath}", true)

    event('StatusUpdate', ['Compiling GWT modules'])
    gwtModulesCompiled = true

    def compiler = GWTCompiler.newInstance()

    compiler.baseDir = basedir
    compiler.draft = gwtDraftCompile
    compiler.gwtOutputStyle = gwtOutputStyle
    compiler.gwtOutputPath = gwtOutputPath
//    compiler.compileReport = compileReport
    compiler.gwtModuleList = modules
    compiler.grailsSettings = grailsSettings
    compiler.compilerClass = compilerClass
    compiler.gwtRun = gwtRunWithProps
    // TODO, config max number of threads
    def ret = compiler.compileAll()
    if (ret == 1) {
        event('GwtCompileFail', ['Failed to compile all GWT modules'])
        //This ensures that anything monitoring this process (eg a CI agent), will record this as failed
        throw new RuntimeException("Failed to compile all GWT Modules")
    }

    event('StatusUpdate', ['Finished compiling GWT modules'])
    event('GwtCompileEnd', ['Finished compiling the GWT modules.'])
}

target(compileI18n: 'Compiles any i18n properties files for any GWT modules in \'' + gwtSrcPath + '\'.') {
    // This triggers the Events scripts in the application and plugins.
    event('GwtCompileI18nStart', ['Starting to compile the i18n properties files.'])

    def GWTCompiler = classLoader.loadClass('org.grails.plugin.gwt.GWTCompiler')

    // Compile any i18n properties files that match the filename
    // "<Module>Constants.properties".
    def modules = gwtModuleList ?: GWTCompiler.findModules("${basedir}/${gwtSrcPath}", false)
    modules += gwtModuleList ?: GWTCompiler.findModules("${basedir}/${grailsSrcPath}", false)

    event('StatusUpdate', ['Compiling GWT i18n properties files'])

    def suffixes = ['Constants', 'Messages']
    modules.each { moduleName ->
        event('StatusUpdate', [('Module: ' + String.valueOf(moduleName))])

        // Split the module name into package and name parts. The
        // package part includes the trailing '.'.
        def pkg = ""
        def pos = moduleName.lastIndexOf('.')
        if (pos > -1) {
            pkg = moduleName[0..pos]
            moduleName = moduleName[(pos + 1)..-1]
        }

        // Check whether the corresponding properties file exists.
        suffixes.each { suffix ->
            def i18nName = "${pkg}client.${moduleName}${suffix}"
            def i18nFile = new File("${basedir}/${gwtSrcPath}", i18nName.replace('.' as char, '/' as char) + ".properties")
            def generatedFile = new File(i18nFile.parentFile, i18nFile.name - ".properties" + ".java")

            if (!i18nFile.exists()) {
                event('StatusUpdate', ["No i18n ${suffix} file found"])
            } else if (i18nFile.lastModified() < generatedFile.lastModified()) {
                // The generated file is newer than the associated
                // properties file, so skip this one.
                println "Skipping ${i18nFile.name} - the Java file is newer than the properties file."
                return
            } else {
                gwtRun('com.google.gwt.i18n.tools.I18NSync') {
                    jvmarg(value: '-Djava.awt.headless=true')
                    arg(value: '-out')
                    arg(value: gwtSrcPath)
                    if (suffix == 'Messages') {
                        arg(value: '-createMessages')
                    }
                    arg(value: i18nName)
                }

                event('StatusUpdate', ["Created class ${i18nName}"])
            }
        }
    }

    event('StatusUpdate', ['Finished compiling the i18n properties files.'])
    event('GwtCompileI18nEnd', ['Finished compiling the i18n properties files.'])
}

gwtClientServer = "${serverHost ?: 'localhost'}:${serverPort}"

target(runGwtClient: 'Runs the GWT hosted mode client.') {
    event('StatusUpdate', ['Starting the GWT hosted mode client.'])

    fixJavac()

    event('GwtRunHostedStart', ['Starting the GWT hosted mode client.'])

    def GWTCompiler = classLoader.loadClass('org.grails.plugin.gwt.GWTCompiler')

    def modules = GWTCompiler.findModules("${basedir}/${gwtSrcPath}", true)

    if (!modules) {
        event("StatusError", ['No GWT modules with entry points are available in src/gwt'])
        exit(1)
    }

    event('StatusUpdate', ["Found ${modules.size()} modules"])

    // GWT dev mode process does not need parent Gant process for anything.
    // Hence it is a good idea to spawn in, making parent script to continue and eventually exit
    // freeing allocated memory that could be significant (up to 512MB in the default Grails installation)
    gwtRunWithProps(devModeClass, [spawn: true, fork: true]) {
        // Hosted mode requires run with 32-bit parameter on Mac OS X (only need for Apple's compiled JVM).
        if (antProject.properties.'os.name' == 'Mac OS X') {
            def osVersion = antProject.properties.'os.version'.split(/\./)
            def javaVersion = antProject.properties.'java.version'
            if (osVersion[0].toInteger() == 10 && osVersion[1].toInteger() >= 6 && javaVersion.startsWith('1.6')) {
                jvmarg(value: '-d32')
            }
        }

        // Enable remote debugging if required.
        if (argsMap['debug']) {
            def debugPort = !(argsMap['debug'] instanceof Boolean) ? argsMap['debug'].toInteger() : 5006
            jvmarg(value: '-Xdebug')
            jvmarg(value: '-Xnoagent')
            jvmarg(value: "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${debugPort}")
        }

        if (argsMap['bindAddress']) {
            arg(value: '-bindAddress')
            arg(value: argsMap['bindAddress'])
        }

        arg(value: '-noserver')
        sysproperty(key: 'gwt.persistentunitcachedir', value: "${basedir}/target/gwt/unitCache")

        arg(value: '-war')
        arg(value: gwtOutputPath)

        arg(value: '-startupUrl')
        arg(value: "http://${gwtClientServer}/${grailsAppName}")

        arg(line: modules.join(' '))
    }
}

target(runCodeServer: 'Runs the Super Dev Mode server.') {
    event('StatusUpdate', ['Starting the GWT Super Dev Mode server.'])

    fixJavac()

    event('GwtRunHostedStart', ['Starting the GWT Super Dev Mode server.'])

    // Check for GWT 2.5 super dev mode.
    ant.available(classname: "com.google.gwt.dev.codeserver.CodeServer", property: 'isCodeServerAvailable') {
        ant.classpath {
            gwtResolvedDependencies.each { File f ->
                pathElement(location: f.absolutePath)
            }
        }
    }

    if (ant.project.properties.isCodeServerAvailable == null) {
        event('StatusError', ['Super Dev Mode only support in GWT >= 2.5.0 version'])
        exit(1)
    }

    def GWTCompiler = classLoader.loadClass('org.grails.plugin.gwt.GWTCompiler')

    def modules = GWTCompiler.findModules("${basedir}/${gwtSrcPath}", true)

    if (!argsMap['module']) {
        if (!modules) {
            event("StatusError", ['No GWT modules with entry points are available in src/gwt'])
            exit(1)
        }

        event('StatusUpdate', ["Found ${modules.size()} modules"])
    }

    gwtRunWithProps(codeServerClass, [spawn: false, fork: true]) {
        if (argsMap['bindAddress']) {
            arg(value: '-bindAddress')
            arg(value: argsMap['bindAddress'])
        }
        if (argsMap['module']) {
            arg(line: argsMap['module'])
        } else {
            arg(line: modules.join(" "))
        }
    }
}

addGwtDependencies = {
    println 'Adding GWT dependencies ...'

    if (getPropertyValue('gwt.version', null)) {
        addGwtCoreToDependencies(getPropertyValue('gwt.version', null))
    }
    if (buildConfig.gwt.gin.version)
        addGinToDependencies(buildConfig.gwt.gin.version)
    if (buildConfig.gwt.gwtp.version)
        addGwtpToDependencies(buildConfig.gwt.gwtp.version)
    if (buildConfig.gwt.dependencies) {
        buildConfig.gwt.dependencies.each { depDefinition ->
            def m = depDefinition =~ /([a-zA-Z0-9\-\/\._+=]*?):([a-zA-Z0-9\-\/\._+=]+?):([a-zA-Z0-9\-\/\._+=]+)/
            if (m.matches()) {
                String name = m[0][2]
                def group = m[0][1]
                def version = m[0][3]

                addDependency(group, name, version)
            } else {
                println "${depDefinition} isn't a valid definition, exiting"
                exit(1)
            }
        }
    }

    maybeUseGwtLibDir()
}

resolveGwtDependencies = {
    println 'Resolving GWT dependencies ...'

    resolveMavenDependencies()
}

compileGwtClasses = {
    // Hack to work around an issue in Google Gin:
    //
    //    http://code.google.com/p/google-gin/issues/detail?id=36
    //
    ant.mkdir(dir: gwtClassesDir)
    gwtJavac(destDir: gwtClassesDir, includes: "**/*.java") {
        src(path: 'src/gwt') //current project gwt modules
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
            if (gwtPluginLibFile.exists()) {
                fileset(dir: gwtPluginLibPath) {
                    include(name: '*.jar')
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

gwtJava = { Map options, Closure body ->
    if (gwtJavaCmd) {
        ant.echo message: "Using ${gwtJavaCmd} for invoking GWT tools"
        options['jvm'] = gwtJavaCmd
    }
    def localAnt = new AntBuilder()
    if (gwtDebugMode)
        localAnt.project.getBuildListeners().firstElement().setMessageOutputLevel(4)
    body = body.clone()
    body = body.curry(localAnt)
    localAnt.java(options, body)
    return localAnt.project.properties
}

gwtJavac = { Map options, Closure body ->
    if (gwtJavacCmd) {
        ant.echo message: "Using ${gwtJavacCmd} for compiling GWT classes"
        options['fork'] = true
        options['executable'] = gwtJavacCmd
        // set target to java version of JDK used by Grails
        options['target'] = ant.project.properties['ant.java.version']
    }
    if (gwtDebugMode)
        ant.project.getBuildListeners().firstElement().setMessageOutputLevel(4)
    ant.javac(options, body)
}

gwtRunWithProps = { String className, Map properties, Closure body ->
    properties.classname = className
    return gwtJava(properties) { ant ->
        // Have to prefix this with 'ant' because the Init
        // script includes a 'classpath' target.
        ant.classpath {
            gwtResolvedDependencies.each {
                pathElement(location: it.absolutePath)
            }

            // Include a GWT-specific lib directory if it exists.
            if (gwtLibFile.exists()) {
                fileset(dir: gwtLibPath) {
                    include(name: '*.jar')
                }
            }
            if (gwtPluginLibFile.exists()) {
                fileset(dir: gwtPluginLibPath) {
                    include(name: '*.jar')
                }
            }

            // Must include src/java and src/gwt in classpath so that
            // the source files can be translated.
            if (new File("${basedir}/${gwtSrcPath}").exists()) {
                pathElement(location: "${basedir}/${gwtSrcPath}")
            }
            pathElement(location: "${basedir}/${grailsSrcPath}")
            pathElement(location: grailsSettings.classesDir.path)
            pathElement(location: gwtClassesDir.path)

            // Fix to get this working with Grails 1.3+. We have to
            // add the directory where plugin classes are compiled
            // to. Pre-1.3, plugin classes were compiled to the same
            // directory as the application classes.
            if (grailsSettings.metaClass.hasProperty(grailsSettings, 'pluginClassesDir')) {
                pathElement(location: grailsSettings.pluginClassesDir.path)
            }

            // Add the plugin's module paths.
            pathElement(location: "${gwtPluginDir}/${gwtSrcPath}")
            pathElement(location: "${gwtPluginDir}/${grailsSrcPath}")

            //add any modules from plugins defined by gwt.plugins in BuildConfig
            buildConfig?.gwt?.plugins?.each { pluginName ->
                pluginName = GrailsNameUtils.getPropertyNameForLowerCaseHyphenSeparatedName(pluginName)
                def pluginDir = binding.variables["${pluginName}PluginDir"]
                if (pluginDir) {
                    pathElement(location: "${pluginDir}/src/gwt")
                    pathElement(location: "${pluginDir}/src/java")
                    event('StatusUpdate', ["Added plugin ${pluginName} to the GWT project"])
                } else {
                    event('StatusError', ["Plugin ${pluginName} cannot be added to the GWT project, as it cannot be found"])
                }
            }
        }

        if (buildConfig.gwt.run.args) {
            def c = buildConfig.gwt.run.args.clone()
            c.delegate = delegate
            c()
        } else {
            // Bump the max heap size up by default.
            jvmarg value: '-Xmx256m'
        }

        body.delegate = delegate
        delegate
        body()
    }
}

def addGwtCoreToDependencies(String version) {
    println "Adding GWT ${version}"

    addDependency('com.google.gwt', 'gwt-dev', version)
    addDependency('com.google.gwt', 'gwt-user', version)
    addDependency('com.google.gwt', 'gwt-servlet', version)

    // GWT version >= 2.5.0
    def versionComponents = parseVersion(version)
    if (versionComponents[0] > 2 || (versionComponents[0] == 2 && versionComponents[1] >= 5)) {
        addDependency('com.google.gwt', 'gwt-codeserver', version)
        addDependency('org.json', 'json', '20090211')
    }
}

def addGinToDependencies(String version) {
    println "Adding Google Gin ${version} to GWT environment"

    addDependency('com.google.gwt.inject', 'gin', version)

    if (version.contains('1.0')) {
        addDependency('com.google.inject', 'guice', '2.0')
    } else if (version.contains('1.5.0') || version.startsWith('2.')) {
        addDependency('com.google.inject', 'guice', '3.0')
        addDependency('com.google.inject.extensions', 'guice-assistedinject', '3.0')
        addDependency('javax.inject', 'javax.inject', '1')
        addDependency('aopalliance', 'aopalliance', '1.0')
    } else {
        println "Google Gin ${version} not supported by plugin, please manage the dependencies manually"
        exit(1)
    }
}

def addGwtpToDependencies(String version) {
    println "Adding GWTP ${version} to GWT environment"

    addDependency('com.gwtplatform', 'gwtp-mvp-client', version)
    addDependency('com.gwtplatform', 'gwtp-mvp-shared', version)
    addDependency('com.gwtplatform', 'gwtp-clients-common', version)
    addDependency('commons-lang', 'commons-lang', '2.6')
    addDependency('org.apache.velocity', 'velocity', '1.7')
}

def addDependency(String group, String name, String version, String wildcard = null) {
    //Create a dependency with the supplied information
    final dependency = new Dependency(group, name, version)
    dependency.exported = false
    if (wildcard)
        dependency.exclude(wildcard)

    addMavenDependency(dependency)
    gwtDependencies << dependency
}

def addMavenDependency(dependency) {
    grailsSettings.dependencyManager.addDependency(dependency, BuildSettings.PROVIDED_SCOPE)
}

def resolveMavenDependencies() {
    def artifacts = grailsSettings.dependencyManager.resolve(BuildSettings.PROVIDED_SCOPE).resolvedArtifacts
    artifacts.addAll(grailsSettings.dependencyManager.resolve(BuildSettings.COMPILE_SCOPE).resolvedArtifacts)
    artifacts.addAll(grailsSettings.dependencyManager.resolve(BuildSettings.RUNTIME_SCOPE).resolvedArtifacts)
    artifacts.addAll(grailsSettings.dependencyManager.resolve(BuildSettings.BUILD_SCOPE).resolvedArtifacts)
    artifacts = artifacts.unique()

    gwtDependencies.each { dependency ->
        def dependencyJar = artifacts.find {
            it.dependency.group.equals(dependency.group) && it.dependency.name.equals(dependency.name)
        }?.file

        if (dependencyJar) {
            // add artifacts to the list of Grails provided dependencies
            // this enables SpringSource STS to build Eclipse's classpath properly
            grailsSettings.providedDependencies << dependencyJar

            if (!gwtResolvedDependencies.contains(dependencyJar))
                gwtResolvedDependencies << dependencyJar
        }
    }
}

def maybeUseGwtLibDir() {
    if (gwtLibFile.exists()) {
        println "Adding lib/gwt/* to the GWT environment"

        gwtLibFile.eachFileMatch(~/.+\.jar$/) { dependencyJar ->
            // add artifacts to the list of Grails provided dependencies
            // this enables SpringSource STS to build Eclipse's classpath properly
            grailsSettings.providedDependencies << dependencyJar
            if (!gwtResolvedDependencies.contains(dependencyJar))
                gwtResolvedDependencies << dependencyJar
        }
    }
}

addDependenciesToClasspath = {
    gwtResolvedDependencies.each { File f ->
        if (!f.name.contains('gwt-dev')) {
            //println "Adding ${f.name} to classpath"
            rootLoader.addURL(f.toURL())

            if (classLoader) {
                classLoader.addURL(f.toURL())
            }
        }
    }
}

def parseVersion(String version) {
    version.tokenize('.').collect { it.toInteger() }
}

def fixJavac() {
    if (System.properties.'os.name' == 'Mac OS X') {
        def javaVersion = System.properties.'java.version'

        if (javaVersion.startsWith('1.8'))
            gwtJavacCmd = 'javac'
    }
}

addGwtDependencies()
resolveGwtDependencies()