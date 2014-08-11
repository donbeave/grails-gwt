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
package org.codehaus.groovy.grails.plugins.gwt

import grails.plugins.gwt.shared.Action
import grails.plugins.gwt.shared.Response
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Implementation of the plugin's action service, which handles all
 * GWT client requests that use actions. The service simply delegates
 * the action processing to the appropriate action handler. The action
 * handler should have the same name as the corresponding action, just
 * with a "Handler" suffix.
 */
class GwtActionService implements grails.plugins.gwt.client.GwtActionService, ApplicationContextAware {
    static expose = ["gwt"]

    ApplicationContext applicationContext

    Response execute(Action action) {
        // Get the class name of the action, because we need it to find
        // the corresponding action bean.
        def name = GrailsClassUtils.getShortName(action.getClass())

        // Prefix the name with "gwt" and add a "Handler" suffix to get
        // hold of the appropriate bean.
        def handlerBeanName = "gwt${name}Handler"
        if (applicationContext.containsBean(handlerBeanName)) {
            def actionBean = applicationContext.getBean(handlerBeanName)
            return actionBean.execute(action)
        } else {
            throw new RuntimeException("No action handler configured for ${name}")
        }
    }
}
