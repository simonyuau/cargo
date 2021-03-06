/*
 * ========================================================================
 *
 * Codehaus CARGO, copyright 2004-2011 Vincent Massol, 2012-2015 Ali Tokmen.
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
package org.codehaus.cargo.container.deployer;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.cargo.container.internal.util.HttpUtils;
import org.codehaus.cargo.util.log.LoggedObject;

/**
 * Monitor that verifies if a {@link org.codehaus.cargo.container.deployable.Deployable} is deployed
 * by pinging a URL provided by the user.
 * 
 */
public class URLDeployableMonitor extends LoggedObject implements DeployableMonitor
{
    /**
     * List of {@link DeployableMonitorListener} that we will notify when the
     * {@link org.codehaus.cargo.container.deployable.Deployable} is deployed or undeployed.
     */
    private List<DeployableMonitorListener> listeners;

    /**
     * The URL to ping.
     */
    private URL pingURL;

    /**
     * Useful HTTP methods (specifically the ping method).
     */
    private HttpUtils httpUtils;

    /**
     * The timeout after which we stop waiting for deployment.
     */
    private long timeout;

    /**
     * String that must be contained in the HTTP response.
     */
    private String contains;

    /**
     * @param pingURL the URL to be pinged and which will tell when the
     * {@link org.codehaus.cargo.container.deployable.Deployable} is deployed
     */
    public URLDeployableMonitor(URL pingURL)
    {
        this(pingURL, 20000L);
    }

    /**
     * @param pingURL the URL to be pinged and which will tell when the
     * {@link org.codehaus.cargo.container.deployable.Deployable} is deployed
     * @param timeout the timeout after which we stop monitoring the deployment
     */
    public URLDeployableMonitor(URL pingURL, long timeout)
    {
        this(pingURL, timeout, null);
    }

    /**
     * @param pingURL the URL to be pinged and which will tell when the
     * {@link org.codehaus.cargo.container.deployable.Deployable} is deployed
     * @param timeout the timeout after which we stop monitoring the deployment
     * @param contains a string that must be contained
     */
    public URLDeployableMonitor(URL pingURL, long timeout, String contains)
    {
        this.listeners = new ArrayList<DeployableMonitorListener>();
        this.httpUtils = new HttpUtils();
        this.timeout = timeout;
        this.pingURL = pingURL;
        this.contains = contains;
    }

    /**
     * {@inheritDoc}
     * @see org.codehaus.cargo.container.deployer.DeployableMonitor#getDeployableName()
     */
    public String getDeployableName()
    {
        return this.pingURL.toString();
    }

    /**
     * {@inheritDoc}
     * @see DeployableMonitor#registerListener(DeployableMonitorListener)
     */
    public void registerListener(DeployableMonitorListener listener)
    {
        this.listeners.add(listener);
    }

    /**
     * @see DeployableMonitor#monitor()
     */
    public void monitor()
    {
        getLogger().debug("Checking URL [" + this.pingURL + "] for status using a timeout of ["
            + this.timeout + "] ms...", this.getClass().getName());

        // We check if the deployable is servicing requests by pinging a URL specified by the user
        HttpUtils.HttpResult results = new HttpUtils.HttpResult();
        boolean isDeployed = this.httpUtils.ping(this.pingURL, results, getTimeout());
        if (isDeployed && this.contains != null && results.responseBody != null)
        {
            isDeployed = results.responseBody.contains(this.contains);
        }

        String msg = "URL [" + this.pingURL + "] is ";
        if (isDeployed)
        {
            msg += "responding...";
        }
        else
        {
            msg += "not responding: " + results.responseCode + " " + results.responseMessage;
        }
        getLogger().debug(msg, this.getClass().getName());

        for (DeployableMonitorListener listener : listeners)
        {
            getLogger().debug("Notifying monitor listener [" + listener + "]",
                this.getClass().getName());

            if (isDeployed)
            {
                listener.deployed();
            }
            else
            {
                listener.undeployed();
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see DeployableMonitor#getTimeout()
     */
    public long getTimeout()
    {
        return this.timeout;
    }
}
