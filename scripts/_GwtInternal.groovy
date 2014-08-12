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
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.report.ResolveReport
import org.apache.ivy.core.resolve.DownloadOptions
import org.apache.ivy.core.resolve.ResolveOptions
import org.codehaus.groovy.grails.resolve.Dependency
import org.codehaus.groovy.grails.resolve.DependencyReport
import org.codehaus.groovy.grails.resolve.IvyDependencyManager

// This script may be run more than once, because the _Events script
// includes targets from it.
if (getBinding().variables.containsKey("_gwt_internal_called")) return
_gwt_internal_called = true

includeTargets << grailsScript("_GrailsInit")

// The targets in this script assume that Init has already been loaded.
// By not explicitly including Init here, we can use this script from
// the Events script.

// This construct makes a 'gwtForceCompile' option available to scripts
// that use these targets. We only define the property if it hasn't
// already been defined. We cannot simply initialise it here because
// all targets appear to trigger the Events script, which might then
// include this script, which would then result in the property value
// being overwritten.
//
// The events mechanism is a source of great frustration!
if (!(getBinding().variables.containsKey("gwtForceCompile"))) {
    gwtForceCompile = false
}

// We do the same for 'gwtModuleList'.
if (!(getBinding().variables.containsKey("gwtModuleList"))) {
    gwtModuleList = null
}

// Common properties and closures (used as re-usable functions).
ant.property(environment: "env")
gwtResolvedDependencies = []
gwtSrcPath = "src/gwt"
grailsSrcPath = "src/java"
gwtClassesCompiled = false
gwtDependencies = [] as Set
gwtProdTestDirPath = ".gwt-test-temp"
gwtTestTypeName = "gwt"
gwtTestTypesRegistered = false
gwtProdTestTypeName = "gwtprod"
gwtRelativeTestSrcPath = "gwt"
gwtTargetDir = new File("${basedir}/target/gwt")
gwtClassesDir = new File(grailsSettings.projectWorkDir, "gwtclasses")
gwtJavacCmd = getPropertyValue("gwt.javac.cmd", null)
gwtJavaCmd = getPropertyValue("gwt.java.cmd", null)
gwtOutputPath = getPropertyValue("gwt.output.path", "${basedir}/web-app/gwt")
gwtOutputStyle = getPropertyValue("gwt.output.style", "OBF")
gwtDisableCompile = getPropertyValue("gwt.compile.disable", "false").toBoolean()
gwtHostedModeOutput = getPropertyValue("gwt.hosted.output.path", "tomcat/classes") // Default is where gwt shell runs its embedded tomcat
gwtModulesCompiled = false
gwtLibPath = "$basedir/lib/gwt"
gwtLibFile = new File(gwtLibPath)

//
// A target to check for existence of the GWT Home
//
target(checkGwtHome: "Stops if GWT_HOME does not exist") {
    resolveGwtDependencies()
    compilerClass = 'com.google.gwt.dev.Compiler'

    if (getBinding().variables.containsKey('gwtResolvedDependencies')) {
        grailsSettings.testDependencies << gwtClassesDir

        maybeUseGwtLibDir()

        maybeUseProvidedDependencies()

        addGwtResolvedDeps()
    }

    // Is this project using Google Gin?
    usingGoogleGin = false
    if (gwtLibFile.exists() || buildConfig.gwt.use.provided.deps == true || gwtResolvedDependencies) {
        ant.available(classname: "com.google.gwt.inject.client.Ginjector", property: "usingGin") {
            ant.classpath {
                if (gwtLibFile.exists()) {
                    fileset(dir: gwtLibPath) {
                        include(name: "*.jar")
                    }
                }

                gwtResolvedDependencies.each { dep ->
                    pathElement(location: dep.absolutePath)
                }
                if (buildConfig.gwt.use.provided.deps == true) {
                    if (grailsSettings.metaClass.hasProperty(grailsSettings, "providedDependencies")) {
                        grailsSettings.providedDependencies.each { dep ->
                            pathElement(location: dep.absolutePath)
                        }
                    }
                }
            }
        }

        usingGoogleGin = ant.project.properties.usingGin != null
    }
}

//
// A target for compiling any GWT modules defined in the project.
//
// Options:
//
//   gwtForceCompile - Set to true to force module compilation. Otherwise
//                     the modules are only compiled if the environment is
//                     production or the 'nocache.js' file is missing.
//
//   gwtModuleList - A collection or array of modules that should be compiled.
//                   If this is null or empty, all the modules in the
//                   application will be compiled.
//
target(compileGwtModules: "Compiles any GWT modules in '$gwtSrcPath'.") {
    if (gwtDisableCompile) return

    // Make sure that the I18n properties files are compiled before
    // the modules are, unless we're not keeping the properties files
    // in sync with the Java interfaces.
    if (buildConfig.gwt.sync.i18n instanceof Map || buildConfig.gwt.sync.i18n == true) {
        compileI18n()
    }

    // Draft compilation.
    if (!(getBinding().variables.containsKey("gwtDraftCompile"))) {
        gwtDraftCompile = null
    }

    if (gwtDraftCompile == null) {
        gwtDraftCompile = getPropertyValue("gwt.draft.compile", false).toBoolean()
    }

    // This triggers the Events scripts in the application and plugins.
    event("GwtCompileStart", ["Starting to compile the GWT modules."])

    // Compile any GWT modules. This requires the GWT 'dev' JAR file,
    // so the user must have defined the GWT_HOME environment variable
    // so that we can locate that JAR.
    def modules = gwtModuleList ?: findModules("${basedir}/${gwtSrcPath}", true)
    event("StatusUpdate", ["Compiling GWT modules"])
    gwtModulesCompiled = true

    def parClass = classLoader.loadClass('org.codehaus.groovy.grails.plugins.gwt.GWTCompiler')

    def compiler = parClass.newInstance()

    compiler.baseDir = basedir
    compiler.draft = gwtDraftCompile
    compiler.gwtOutputStyle = gwtOutputStyle
    compiler.gwtOutputPath = gwtOutputPath
//    compiler.compileReport = compileReport
    compiler.gwtModuleList = gwtModuleList
    compiler.grailsSettings = grailsSettings
    compiler.compilerClass = compilerClass
    compiler.gwtRun = gwtRunWithProps
    compiler.gwtForceCompile = gwtForceCompile
    //TODO, config max number of threads
    def ret = compiler.compileAll()
    if (ret == 1) {
        event("GwtCompileFail", ["Failed to compile all GWT modules"])
        //This ensures that anything monitoring this process (eg a CI agent), will record this as failed
        throw new RuntimeException("Failed to compile all GWT Modules")
    }

    event("StatusUpdate", ["Finished compiling GWT modules"])
    event("GwtCompileEnd", ["Finished compiling the GWT modules."])
}

// This is only used when running under hosted mode and you have server
// code (ie. test service classes) used by your client code during testing.
target(compileServerCode: "Compiles gwt server code into tomcat/classes directory.") {
    ant.mkdir(dir: gwtHostedModeOutput)
    gwtJavac(destdir: gwtHostedModeOutput, debug: "yes") {
        // Have to prefix this with 'ant' because the Init script
        // includes a 'classpath' target.
        ant.classpath {
            // Include a GWT-specific lib directory if it exists.
            if (gwtLibFile.exists()) {
                fileset(dir: gwtLibPath) {
                    include(name: "*.jar")
                }
            }
            if (gwtResolvedDependencies) {
                gwtResolvedDependencies.each { File f ->
                    pathElement(location: f.absolutePath)
                }
            }
        }

        if (new File("${basedir}/${gwtSrcPath}").exists()) {
            src(path: "${basedir}/${gwtSrcPath}")
        }

        src(path: "${basedir}/${grailsSrcPath}")
        include(name: "**/server/**")
    }
}

target(compileI18n: "Compiles any i18n properties files for any GWT modules in '$gwtSrcPath'.") {
    // This triggers the Events scripts in the application and plugins.
    event("GwtCompileI18nStart", ["Starting to compile the i18n properties files."])

    // Compile any i18n properties files that match the filename
    // "<Module>Constants.properties".
    def modules = gwtModuleList ?: findModules("${basedir}/${gwtSrcPath}", false)
    modules += gwtModuleList ?: findModules("${basedir}/${grailsSrcPath}", false)

    event("StatusUpdate", ["Compiling GWT i18n properties files"])

    def suffixes = ["Constants", "Messages"]
    modules.each { moduleName ->
        event("StatusUpdate", ["Module: ${moduleName}"])

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
                event("StatusUpdate", ["No i18n ${suffix} file found"])
            } else if (i18nFile.lastModified() < generatedFile.lastModified()) {
                // The generated file is newer than the associated
                // properties file, so skip this one.
                println "Skipping ${i18nFile.name} - the Java file is newer than the properties file."
                return
            } else {
                gwtRun("com.google.gwt.i18n.tools.I18NSync") {
                    jvmarg(value: '-Djava.awt.headless=true')
                    arg(value: "-out")
                    arg(value: gwtSrcPath)
                    if (suffix == "Messages") {
                        arg(value: "-createMessages")
                    }
                    arg(value: i18nName)
                }

                event("StatusUpdate", ["Created class ${i18nName}"])
            }
        }
    }

    event("StatusUpdate", ["Finished compiling the i18n properties files."])
    event("GwtCompileI18nEnd", ["Finished compiling the i18n properties files."])
}

target(gwtClean: "Cleans the files generated by GWT.") {
    // Start by removing the directory containing all the javascript
    // files.
    new File("${basedir}/target/gwt").deleteDir()
    new File("${basedir}/gwt-unitCache").deleteDir()
    new File("${basedir}/web-app/WEB-INF/deploy").deleteDir()

    boolean individualClean = true
    if (new File("${basedir}/web-app") != new File(gwtOutputPath)) {
        ant.delete(dir: gwtOutputPath)
        individualClean = false
    }
    ant.delete(dir: gwtProdTestDirPath)
    ant.delete(dir: gwtClassesDir.path)
    ant.delete(dir: gwtTargetDir.path)
    // Now remove any generated i18n files, unless we're not keeping
    // the properties files in sync with the Java interfaces.
    if (buildConfig.gwt.sync.i18n == false) return 0

    def modules = gwtModuleList ?: findModules("${basedir}/${gwtSrcPath}", false)
    modules += gwtModuleList ?: findModules("${basedir}/${grailsSrcPath}", false)

    modules.each { moduleName ->
        // Split the module name into package and name parts. The
        // package part includes the trailing '.'.
        def pkg = ""
        def pos = moduleName.lastIndexOf('.')
        if (pos > -1) {
            pkg = moduleName[0..pos]
            moduleName = moduleName[(pos + 1)..-1]
        }

        // If there is a properties file, delete the corresponding
        // constants file. If it doesn't exist, that doesn't matter:
        // nothing will happen.
        def pkgPath = pkg.replace('.' as char, '/' as char)
        if (individualClean) {
            ant.delete(quiet: true, dir: new File("${gwtOutputPath}/${pkgPath}").path)
        }
        def i18nRoot = new File("${basedir}/${gwtSrcPath}", "${pkgPath}/client")

        def suffixes = ["Constants", "Messages"]
        suffixes.each { suffix ->
            def i18nPropFile = new File(i18nRoot, "${moduleName}${suffix}.properties")
            if (i18nPropFile.exists()) {
                ant.delete(file: new File(i18nRoot, "${moduleName}${suffix}.java").path)
            }
        }
    }
}

gwtClientServer = "${serverHost ?: 'localhost'}:${serverPort}"

target(runGwtClient: "Runs the GWT hosted mode client.") {
    event("StatusUpdate", ["Starting the GWT hosted mode client."])
    event("GwtRunHostedStart", ["Starting the GWT hosted mode client."])

    // Check for GWT 2.0 hosted mode.
    ant.available(classname: "com.google.gwt.dev.DevMode", property: "isGwt20") {
        ant.classpath {
            gwtResolvedDependencies.each { File f ->
                pathElement(location: f.absolutePath)
            }
        }
    }

    def runClass = "com.google.gwt.dev.DevMode"

    event("StatusUpdate", ["Found ${modules.size()} modules"])

    // GWT dev mode process does not need parent Gant process for anything.
    // Hence it is a good idea to spawn in, making parent script to continue and eventually exit
    // freeing allocated memory that could be significant (up to 512MB in the default Grails installation)
    gwtRunWithProps(runClass, [spawn: true, fork: true]) {
        // Hosted mode requires a special JVM argument on Mac OS X.
        if (antProject.properties.'os.name' == 'Mac OS X') {
            def osVersion = antProject.properties.'os.version'.split(/\./)
            def javaVersion = antProject.properties.'java.version'
            if (osVersion[0].toInteger() == 10 && osVersion[1].toInteger() >= 6 && !javaVersion.startsWith('1.7')) {
                jvmarg(value: '-d32')
            }
        }

        // Enable remote debugging if required.
        if (argsMap["debug"]) {
            def debugPort = !(argsMap["debug"] instanceof Boolean) ? argsMap["debug"].toInteger() : 5006
            jvmarg(value: "-Xdebug")
            jvmarg(value: "-Xnoagent")
            jvmarg(value: "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=${debugPort}")
        }

        if (argsMap["bindAddress"]) {
            arg(value: "-bindAddress")
            arg(value: argsMap["bindAddress"])
        }

        arg(value: "-noserver")
        sysproperty(key: "gwt.persistentunitcachedir", value: "${basedir}/target/gwt/unitCache")

        arg(value: "-out")
        arg(value: gwtOutputPath)

        arg(value: "http://${gwtClientServer}/${grailsAppName}")
    }
}

target(runCodeServer: "Runs the Super Dev Mode server.") {
    event("StatusUpdate", ["Starting the GWT Super Dev Mode server."])
    event("GwtRunHostedStart", ["Starting the GWT Super Dev Mode server."])

    // Check for super dev mode availablility (since GWT 2.5).
    ant.available(classname: "com.google.gwt.dev.codeserver.CodeServer", property: "isSuperDevModeAvailable") {
        ant.classpath {
            gwtResolvedDependencies.each { File f ->
                pathElement(location: f.absolutePath)
            }
        }
    }

    def isSuperDevModeAvailable = ant.project.properties.isSuperDevModeAvailable != null

    if (!isSuperDevModeAvailable) {
        event("StatusError", ["Super Dev Mode requires GWT 2.5.0 or newer"])
        exit(1)
    }

    def runClass = "com.google.gwt.dev.codeserver.CodeServer"
    def modules = findModules("${basedir}/${gwtSrcPath}", true)

    if (!modules) {
        event("StatusError", ["No GWT modules with entry points are available in src/gwt"])
        exit(1)
    }

    gwtRunWithProps(runClass, [spawn: false, fork: true]) {
        if (argsMap["bindAddress"]) {
            arg(value: "-bindAddress")
            arg(value: argsMap["bindAddress"])
        }
        if (argsMap["module"]) {
            arg(line: argsMap["module"])
        } else {
            arg(line: modules.join(" "))
        }
    }
}

gwtJava = { Map options, Closure body ->
    if (gwtJavaCmd) {
        ant.echo message: "Using ${gwtJavaCmd} for invoking GWT tools"
        options["jvm"] = gwtJavaCmd
    }
    def localAnt = new AntBuilder()
    body = body.clone()
    body = body.curry(localAnt)
    localAnt.java(options, body)
    return localAnt.project.properties
}

gwtJavac = { Map options, Closure body ->
    if (gwtJavacCmd) {
        ant.echo message: "Using ${gwtJavacCmd} for compiling GWT classes"
        options["fork"] = true
        options["executable"] = gwtJavacCmd
        // set target to java version of JDK used by Grails
        options["target"] = ant.project.properties['ant.java.version']
    }
    ant.javac(options, body)
}

gwtRun = { String className, Closure body ->
    return gwtRunWithProps(className, [fork: true], body)
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

            // We allow users to specify GWT dependencies via the "provided"
            // configuration. Ideally, we would add a custom "gwt" conf,
            // but that's not possible with Grails at the moment.
            if (buildConfig.gwt.use.provided.deps == true) {
                if (grailsSettings.metaClass.hasProperty(grailsSettings, "providedDependencies")) {
                    grailsSettings.providedDependencies.each { dep ->
                        pathElement(location: dep.absolutePath)
                    }
                }
            }

            // Include a GWT-specific lib directory if it exists.
            if (gwtLibFile.exists()) {
                fileset(dir: gwtLibPath) {
                    include(name: "*.jar")
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
            if (grailsSettings.metaClass.hasProperty(grailsSettings, "pluginClassesDir")) {
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
                    event("StatusUpdate", ["Added plugin ${pluginName} to the GWT project"])
                } else {
                    event("StatusError", ["Plugin ${pluginName} cannot be added to the GWT project, as it cannot be found"])
                }
            }

            // Add the DTO source path if that plugin is installed in
            // the current project.
            if (getBinding().variables.containsKey("dtoPluginDir")) {
                pathElement(location: "${dtoPluginDir}/${grailsSrcPath}")
            }
        }

        if (buildConfig.gwt.run.args) {
            def c = buildConfig.gwt.run.args.clone()
            c.delegate = delegate
            c()
        } else {
            // Bump the max heap size up by default.
            jvmarg value: "-Xmx256m"
        }

        body.delegate = delegate
        body()
    }
}

/**
 * Searches a given directory for any GWT module files, and
 * returns a list of their fully-qualified names.
 * @param searchDir A string path specifying the directory
 * to search in.
 * @param entryPointOnly Whether to find modules that contains entry-points (ie. GWT clients)
 * @return a list of fully-qualified module names.
 */
def findModules(String searchDir, boolean entryPointOnly) {
    def modules = []
    def baseLength = searchDir.size()

    def searchDirFile = new File(searchDir)
    if (searchDirFile.exists()) {
        searchDirFile.eachFileRecurse { File file ->
            // Replace Windows separators with Unix ones.
            def filePath = file.path.replace('\\' as char, '/' as char)

            // Chop off the search directory.
            filePath = filePath.substring(baseLength + 1)

            // Now check whether this path matches a module file.
            def m = filePath =~ /([\w\/]+)\.gwt\.xml$/
            if (m.count > 0) {
                // now check if this module has an entry point
                // if there's no entry point, then it's not necessary to compile the module
                if (!entryPointOnly || file.text =~ /entry-point/ || file.text =~ /com.gwtplatform.mvp/) {
                    // Extract the fully-qualified module name.
                    modules << m[0][1].replace('/' as char, '.' as char)
                }
            }
        }
    }

    return modules
}

def resolveGwtDependencies() {
    if (getPropertyValue('gwt.version', null)) {
        addGwtCoreToDependencies(getPropertyValue('gwt.version', null))
    }
    if (buildConfig.gwt.gin.version) {
        addGinToDependencies(buildConfig.gwt.gin.version)
    }
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

    addDependency('javax.validation', 'validation-api', '1.0.0.GA')
    addDependency('javax.validation', "validation-api", '1.0.0.GA', 'sources')
}

def addGinToDependencies(String version) {
    println "Adding Google Gin ${version} to the GWT environment"

    addDependency('com.google.gwt.inject', 'gin', version)

    if (version.contains('1.0')) {
        addDependency('com.google.inject', 'guice', '2.0')
    } else if (version.contains("1.5.0") || version.startsWith('2.')) {
        addDependency('com.google.inject', 'guice', '3.0')
        addDependency('com.google.inject.extensions', 'guice-assistedinject', '3.0')
        addDependency('javax.inject', 'javax.inject', '1')
        addDependency('aopalliance', 'aopalliance', '1.0')
    } else {
        println "Google Gin ${version} not supported by plugin, please manage the dependencies manually"
        exit(1)
    }
}

def addDependency(group, name, version, type = null) {
    if (grailsSettings.dependencyManager in IvyDependencyManager) {
        if (type != null && grailsSettings.dependencyManager.ivySettings.defaultRepositoryCacheManager.ivyPattern.indexOf('[classifier') == -1) {
            println """WARN: source dependencies might not be properly resolved with
the current configuration, please add the following line at the top of
grails.project.dependency.resolution in grails-app/conf/BuildConfig.groovy:

dependencyManager.ivySettings.defaultCacheIvyPattern = "[organisation]/[module](/[branch])/ivy-[revision](-[classifier]).xml"

"""
        }
        def extraAttrs = type == null ? [:] : ['m:classifier': type]
        ModuleRevisionId mrid = ModuleRevisionId.newInstance(group, name, version, extraAttrs)
        addModuleToDependencies(mrid, 'default')
    } else {
        addMavenModuleToDependencies(group, name, version)
    }
}

def addModuleToDependencies(ModuleRevisionId mrid, type) {
    ResolveReport report = grailsSettings.dependencyManager.resolveEngine.resolve(mrid, new ResolveOptions(confs: [type] as String[], transitive: false, outputReport: true, download: true, useCacheOnly: false), false)

    if (report.hasError()) {
        println 'GWT Dependency resolution has errors, exiting'
        exit(1)
    }
    report.artifacts.each { Artifact artifact ->
        ArtifactDownloadReport rep = grailsSettings.dependencyManager.resolveEngine.download(artifact, new DownloadOptions(log: DownloadOptions.LOG_DOWNLOAD_ONLY))
        def jarFile = rep.localFile
        gwtResolvedDependencies << jarFile
        // add artifacts to the list of Grails provided dependencies
        // this enables SpringSource STS to build Eclipse's classpath properly
        if (grailsSettings.metaClass.hasProperty(grailsSettings, 'providedDependencies')) {
            if (!grailsSettings.providedDependencies.contains(jarFile)) {
                grailsSettings.providedDependencies << jarFile
            }
        }
    }
}

def addMavenModuleToDependencies(group, name, version, scope = BuildSettings.PROVIDED_SCOPE) {
    //Create a dependency with the supplied information
    Dependency dependency = new Dependency(group, name, version)
    dependency.exported = false
    //Add the dependency as "provided"
    grailsSettings.dependencyManager.addDependency(dependency, scope)
    DependencyReport dependencyReport = grailsSettings.dependencyManager.resolve(scope)
    if (dependencyReport.hasError()) {
        println "GWT Dependency resolution has errors (${dependencyReport.getResolveError().getMessage()}), exiting"
        exit(1)
    }
    dependencyReport.getJarFiles().each { dependencyJar ->
        if (!gwtResolvedDependencies.contains(dependencyJar)) {
            gwtResolvedDependencies << dependencyJar
        }
        // add artifacts to the list of Grails provided dependencies
        // this enables SpringSource STS to build Eclipse's classpath properly
        if (grailsSettings.metaClass.hasProperty(grailsSettings, 'providedDependencies')) {
            if (!grailsSettings.providedDependencies.contains(dependencyJar)) {
                grailsSettings.providedDependencies << dependencyJar
            }
        }
    }
}

def addGwtResolvedDeps() {
    gwtResolvedDependencies.each { File f ->
        if (!f.name.contains("gwt-dev")) {
            //println "Adding ${f.name} to classpath"
            rootLoader.addURL(f.toURL())

            if (classLoader) {
                classLoader.addURL(f.toURL())
            }
        }
        if (f.name.startsWith("gwt-dev") || f.name.startsWith("gwt-user")) {
            haveGwtOnClasspath = true
        }
        gwtDependencies << f
    }
}

def maybeUseProvidedDependencies() {
    if (buildConfig.gwt.use.provided.deps == true) {
        println "Adding provided dependencies to the GWT environment"
        grailsSettings.providedDependencies.each { dep ->
            grailsSettings.testDependencies << dep
            gwtDependencies << dep
        }
    }
}

def maybeUseGwtLibDir() {
    if (gwtLibFile.exists()) {
        println "Adding lib/gwt/* to the GWT environment"
        gwtLibFile.eachFileMatch(~/.+\.jar$/) { f ->
            grailsSettings.testDependencies << f
            gwtDependencies << f
        }
    }
}

def parseVersion(String version) {
    version.tokenize('.').collect { it.toInteger() }
}