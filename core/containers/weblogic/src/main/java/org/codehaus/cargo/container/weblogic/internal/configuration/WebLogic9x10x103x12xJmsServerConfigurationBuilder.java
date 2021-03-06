/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol, 2011-2015 Ali Tokmen.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * ========================================================================
 */
package org.codehaus.cargo.container.weblogic.internal.configuration;

import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.configuration.entry.Resource;

/**
 * Create WLST script for adding JMS server to Weblogic domain.
 */
public class WebLogic9x10x103x12xJmsServerConfigurationBuilder extends
    AbstractWebLogicJmsConfigurationBuilder
{
    /**
     * Sets configuration containing all needed information for building JMS resources.
     *
     * @param configuration containing all needed informations.
     */
    public WebLogic9x10x103x12xJmsServerConfigurationBuilder(LocalConfiguration configuration)
    {
        super(configuration);
    }

    /**
     * {@inheritDoc}
     */
    public String toConfigurationEntry(Resource resource)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("create('%s', 'JMSServer')", resource.getId()));
        buffer.append(NEW_LINE);
        buffer.append("cd('/')");
        buffer.append(NEW_LINE);
        buffer.append(String.format("assign('JMSServer', '%s', 'Target', '%s')",
            resource.getId(), getServerName()));

        return buffer.toString();
    }
}
