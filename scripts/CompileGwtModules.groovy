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
includeTargets << new File("${gwtPluginDir}/scripts/_GwtInternal.groovy")
includeTargets << grailsScript('_GrailsCompile')

USAGE = """
    compile-gwt-modules [--draft]

where
    --draft  = Compiler uses draft mode, resulting in less optimised
               JavaScript
"""

target(default: 'Compiles the GWT modules to JavaScript.') {
    depends(parseArguments)

    // If arguments are provided, treat them as a list of modules to
    // compile.
    gwtModuleList = argsMap['params']

    // Handle draft compilation mode. We assign a default value of
    // 'null' so that we know whether we can override with the
    // gwt.draft.compile setting.
    gwtDraftCompile = argsMap['draft'] ?: null

    // Compile the GWT modules. We use the 'compile' target because
    // 'compileGwtModules' depends on it and the module compilation
    // is triggered by the end of the standard Grails compilation
    // (at the moment).
    compile()

    compileGwtModules()
}