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
 * @author <a href='mailto:alexey@zhokhov.com'>Alexey Zhokhov</a>
 */
target(default: 'Cleans the compiled GWT module files.') {
    depends(compile)

    gwtClean()
}