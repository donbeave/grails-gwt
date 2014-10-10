/*
 * Copyright 2007-2008 Peter Ledbrook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

class GwtGrailsPlugin {

    def version = '2.0-SNAPSHOT'
    def grailsVersion = '2.3 > *'

    def title = 'The Google Web Toolkit for Grails.'
    def description = """\
Incorporates GWT into Grails. In particular, GWT host pages can be
GSPs and standard Grails services can be used to handle client RPC
requests.
"""

    def documentation = "http://simplicityitself.github.com/grails-gwt/guide/"

    def license = 'APACHE'

    def pluginExcludes = [
            'web-app/**'
    ]

    def srcDir = 'src/gwt'

    def issueManagement = [system: 'GITHUB',
                           url   : 'https://github.com/donbeave/grails-gwt/issues']
    def scm = [url: 'https://github.com/donbeave/grails-gwt']

}