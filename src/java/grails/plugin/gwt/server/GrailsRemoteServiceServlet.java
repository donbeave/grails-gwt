/*
 * Copyright 2015 the original author or authors.
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
package grails.plugin.gwt.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.URL;

/**
 * Used to overcome the problem with grails resource plugin conflict with GWT
 * <p>
 * GWT will use the URL to find resources, but the url has /static in it.
 * <p>
 * issue#62 https://github.com/simplicityitself/grails-gwt/issues/62
 * <p>
 * Created by ryan on 15-04-04.
 */
public class GrailsRemoteServiceServlet extends RemoteServiceServlet {

    Logger logger = LoggerFactory.getLogger(GrailsRemoteServiceServlet.class);

    /**
     * Strip off the /static from the beginning of a module url, then pass to the super's implementation
     * <p>
     * issue#62
     * <p>
     * Does not affect other uses of static
     * <p>
     * Borrow's from loadSerializationPolicy
     *
     * @param request
     * @param moduleBaseURL
     * @param strongName
     * @return
     */
    @Override
    protected SerializationPolicy doGetSerializationPolicy(
            HttpServletRequest request, String moduleBaseURL, String strongName) {
        //moduleBaseURL.replace("/static", "") is so much easy(lazy)
        String returnURL = moduleBaseURL;
        logger.debug("doGetSerializationPolicy url:" + moduleBaseURL);
        try {
            URL url = new URL(moduleBaseURL);
            String host = url.getHost();
            int port = url.getPort();
            String protocol = url.getProtocol();
            String basePath = url.getPath();
            logger.debug("basePath:" + basePath);
            if (basePath.startsWith("/static")) {
                logger.debug("found static!");
                basePath = basePath.replaceFirst("/static", "");
                URL newUrl = new URL(protocol, host, port, basePath);
                returnURL = newUrl.toString();
            }

        } catch (Exception e) { // default implementation will handle this
            logger.error("Exception in GrailsRemoteService Servlet parsing url" + e.getMessage());
        }
        return super.doGetSerializationPolicy(request, returnURL, strongName);
    }

}