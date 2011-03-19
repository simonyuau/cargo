/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol.
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
package org.codehaus.cargo.container.stub;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployer.DeployableMonitor;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.DeployerType;
import org.codehaus.cargo.util.log.LoggedObject;

/**
 * Mock for a local {@link org.codehaus.cargo.container.deployer.Deployer}. We need a static mock
 * rather than a dynamic mock (which we could get using JMock for example) because we're testing
 * factory classes which create an object out of a class name.
 * 
 * @version $Id$
 */
public class InstalledLocalDeployerStub extends LoggedObject implements Deployer
{
    /**
     * Voluntarily do nothing for testing. {@inheritdoc}
     * @param container Ignored.
     */
    public InstalledLocalDeployerStub(InstalledLocalContainer container)
    {
        // Nothing
    }

    /**
     * Voluntarily do nothing for testing. {@inheritdoc}
     * @param deployable Ignored.
     */
    public void deploy(Deployable deployable)
    {
        // Nothing
    }

    /**
     * Voluntarily do nothing for testing. {@inheritdoc}
     * @param deployable Ignored.
     * @param monitor Ignored.
     */
    public void deploy(Deployable deployable, DeployableMonitor monitor)
    {
        // Nothing
    }

    /**
     * Voluntarily do nothing for testing. {@inheritdoc}
     * @param deployable Ignored.
     */
    public void undeploy(Deployable deployable)
    {
        // Nothing
    }

    /**
     * Voluntarily do nothing for testing. {@inheritdoc}
     * @param deployable Ignored.
     * @param monitor Ignored.
     */
    public void undeploy(Deployable deployable, DeployableMonitor monitor)
    {
        // Nothing
    }

    /**
     * Voluntarily do nothing for testing. {@inheritdoc}
     * @param deployable Ignored.
     */
    public void redeploy(Deployable deployable)
    {
        // Nothing
    }

    /**
     * Voluntarily do nothing for testing. {@inheritdoc}
     * @param deployable Ignored.
     */
    public void start(Deployable deployable)
    {
        // Nothing
    }

    /**
     * Voluntarily do nothing for testing. {@inheritdoc}
     * @param deployable Ignored.
     */
    public void stop(Deployable deployable)
    {
        // Nothing
    }

    /**
     * {@inheritdoc}
     * @return {@link DeployerType#INSTALLED}
     */
    public DeployerType getType()
    {
        return DeployerType.INSTALLED;
    }
}
