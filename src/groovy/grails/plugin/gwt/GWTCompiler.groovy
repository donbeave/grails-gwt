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
package grails.plugin.gwt

import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Manage GWT Compilation.
 * Remove from scripting environment to give more opportunity to use threading when this is appropriate
 *
 * @author Predrag Knežević
 * @author <a href='mailto:david.dawson@dawsonsystems.com'>David Dawson</a>
 * @author <a href='mailto:alexey@zhokhov.com'>Alexey Zhokhov</a>
 */
class GWTCompiler {
    List modules

    def numThreads
    boolean draft = false
    def baseDir
    def logDir
    def deployDir
    def extraDir

    def gwtOutputStyle = 'PRETTY'
    def gwtOutputPath
    def compileReport
    def optimizationLevel
    def logLevel
    def classMetadata
    def castChecking
    def closureCompiler
    def aggressiveOptimization
    def jsInteropMode // GWT 2.7+ only
    def gwtModuleList
    def grailsSettings
    def gwtRun
    def compilerClass
    def failed

    //Set BuildConfig gwt.parallel=false  to force the use of this, otherwise it will be auto selected based on modules and number of processor cores.
    private int numCompileWorkers = 0

    public int compileAll() {
        // clean target path
        new File('target/gwt').deleteDir()
        new File('target/gwt').mkdirs()

        logDir = new File(baseDir, 'target/gwt/logs')
        logDir.mkdirs()

        extraDir = new File(baseDir, 'target/gwt/extras')
        extraDir.mkdirs()

        // clean output path
        new File(gwtOutputPath).deleteDir()
        new File(gwtOutputPath).mkdirs()

        long then = System.currentTimeMillis()
        modules = gwtModuleList ?: findModules("${baseDir}/src/gwt")

        if (numThreads) {
            println "Configured to use ${numThreads} (detected ${Runtime.runtime.availableProcessors()} available hardware threads)"
        } else {
            println "Auto configuring to use all of the ${Runtime.runtime.availableProcessors()} available hardware threads"
            numThreads = Runtime.runtime.availableProcessors()
        }

        if (compileReport)
            println 'Will generate a compilation report'
        if (gwtOutputStyle)
            println "Using GWT JS Style ${gwtOutputStyle}"
        if (draft)
            println 'Draft compilation (not for production)'
        if (optimizationLevel)
            println "Optimization level: ${optimizationLevel}"
        if (logLevel)
            println "Log level: ${logLevel}"
        if (classMetadata == false)
            println 'Disable class metadata'
        if (castChecking == false)
            println 'Disable cast checking'
        if (aggressiveOptimization == false)
            println 'Disable aggressive optimization'
        if (closureCompiler)
            println 'Using Closure compiler'
        if (jsInteropMode)
            println "JS interop mode: ${jsInteropMode}"

        println "Will compile ${modules.size()} modules"

        failed = []

        if (shouldUseFullParallel()) {
            fullParallelCompile()
        } else {
            gwtWorkerCompile()
        }

        long now = System.currentTimeMillis()

        println "Compilation run completed in ${(now - then) / 1000} seconds"

        if (failed) {
            println 'The following modules have FAILED COMPILATION :-'
            failed.each {
                println "     * ${it}"
            }
            println "\nGWT Compilation has FAILED, logs are available at ${logDir.absolutePath}, dumping to console."

            failed.each {
                println "************************************************************************************************"
                println "    ${it}"
                println "************************************************************************************************"
                new File(logDir, "FAILED-${it}.log").eachLine {
                    println "  | ${it}"
                }
            }
            return 1
        }
        return 0
    }

    boolean shouldUseFullParallel() {
        if (grailsSettings.config.gwt.parallel != null)
            return grailsSettings.config.gwt.parallel

        return modules.size() > 2
    }

    def gwtWorkerCompile() {
        numCompileWorkers = grailsSettings.config.gwt.local.workers ?: numThreads

        println "Selected GWT Worker parallel compilation with ${numCompileWorkers} worker threads"

        modules.each { moduleName ->
            try {
                compile(moduleName)
            } catch (Exception ex) {
                failed << moduleName
                if (!(ex instanceof GwtCompilationException))
                    ex.printStackTrace()
            }
        }
    }

    def fullParallelCompile() {
        println 'Selected Full Parallel compilation :-'
        def executor = Executors.newFixedThreadPool(numThreads)

        int remaining = 0

        modules.each { moduleName ->
            remaining++
            executor.submit({
                try {
                    compile(moduleName)
                } catch (Exception ex) {
                    failed << moduleName
                    if (!(ex instanceof GwtCompilationException)) {
                        ex.printStackTrace()
                    }
                } finally {
                    synchronized (executor) {
                        remaining--
                        executor.notifyAll()
                    }
                }
            } as Callable)
        }

        while (remaining > 0) {
            synchronized (executor) {
                executor.wait(500)
            }
        }

        executor.shutdownNow()
    }

    def compile(String moduleName) {
        println "  Compiling ${moduleName}"

        def logFile = new File(logDir, "${moduleName}.log")

        logFile.delete()

        logFile << "================================================================\n"
        logFile << "   Compilation started at ${new Date()}\n"
        logFile << "================================================================\n\n"

        logFile << "Base Dir = ${baseDir}\n"

        try {

            def result = gwtRun(compilerClass, [resultproperty: 'result', fork: true, output: "${logFile.absoluteFile}", error: "${logFile.absoluteFile}", append: true]) {
                if (grailsSettings.config.gwt.compile.args) {
                    def c = grailsSettings.config.gwt.compile.args.clone()
                    c.delegate = delegate
                    c()
                }

                jvmarg(value: '-Djava.awt.headless=true')

                arg(value: '-saveSource')
                arg(value: '-style')
                arg(value: gwtOutputStyle)

                sysproperty(key: 'gwt.persistentunitcachedir', value: "${baseDir}/target/gwt/unitCache")

                // Multi-threaded compilation.
                if (numCompileWorkers > 0) {
                    arg(value: '-localWorkers')
                    arg(value: numCompileWorkers)
                }

                // Draft compile - GWT 2.0+ only
                if (draft)
                    arg(value: '-draftCompile')

                if (compileReport)
                    arg(value: '-compileReport')

                if (optimizationLevel) {
                    arg(value: '-optimize')
                    arg(value: optimizationLevel)
                }

                if (logLevel) {
                    arg(value: '-logLevel')
                    arg(value: logLevel)
                }

                if (classMetadata == false)
                    arg(value: '-XdisableClassMetadata')

                if (castChecking == false)
                    arg(value: '-XdisableCastChecking')

                if (aggressiveOptimization == false)
                    arg(value: '-XdisableAggressiveOptimization')

                if (closureCompiler)
                    arg(value: '-XenableClosureCompiler')

                if (jsInteropMode) {
                    arg(value: '-XjsInteropMode')
                    arg(value: jsInteropMode)
                }

                arg(value: '-war')
                arg(value: gwtOutputPath)

                arg(value: '-deploy')
                arg(value: deployDir)

                arg(value: '-extra')
                arg(value: extraDir)

                arg(value: moduleName)
            }
            logFile << '================================================================\n'
            logFile << "   Compilation finished at ${new Date()}\n"
            logFile << '================================================================\n\n'

            if (result.result != "0") {
                def newLogFile = new File(logDir, "FAILED-${moduleName}.log")
                newLogFile.delete()
                logFile.renameTo(newLogFile)
                println "   module ${moduleName} FAILED, output is available in ${newLogFile.absolutePath}"
                throw new GwtCompilationException()
            } else {
                println "   module ${moduleName} SUCCEEDED"
            }

        } catch (Exception ex) {
            logFile << ex.getMessage()
            throw ex
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
    static findModules(String searchDir, boolean entryPointOnly = true) {
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

}

class GwtCompilationException extends Exception {}