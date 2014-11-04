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

/**
 * @author <a href='mailto:p.ledbrook@cacoethes.co.uk'>Peter Ledbrook</a>
 * @author <a href='mailto:david.dawson@dawsonsystems.com'>David Dawson</a>
 * @author <a href='mailto:donbeave@gmail.com'>Alexey Zhokhov</a>
 */
target(default: 'Runs the GWT hosted mode client.') {
    depends(parseArguments)

    if (argsMap['params']) {
        // Check whether a host and port have been specified.
        def m = argsMap["params"][0] =~ /([a-zA-Z][\w\.]*)?:?(\d+)?/
        if (m.matches()) {
            // The user can specify a host, a port, or both if separated
            // by a colon. If either or both are not given, the appropriate
            // defaults are used.
            gwtClientServer = (m[0][1] ? m[0][1] : 'localhost') + ":" +
                    (m[0][2] ? m[0][2] : 8080)
        }
    }

    compile()

    // Start the hosted mode client.
    runGwtClient()
}