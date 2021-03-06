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
package org.codehaus.cargo.deployer.jetty;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;

/**
 * This servlet is used to control deploy, undeploy, redeploy, start, and stop a web application
 * within the jetty server.
 * 
 */
public class DeployerServlet extends HttpServlet
{

    /**
     * The ContectHandlerCollection for the server.
     */
    private ContextHandlerCollection chc;

    /**
     * The location of the server's configuration directory.
     */
    private String configHome;

    /**
     * The location of the server's webapp directory
     */
    private String webAppDirectory;

    /**
     * Creates the DeployerServlet and gives the servlet reference to the server in which it is
     * deployed.This gives the servlet access to the server internals which allows for deployment
     * control.
     * @param server The server object for the currently running server
     */
    public DeployerServlet(Server server)
    {
        // TODO find a better means of determining the configuration and webapp directories
        if (System.getProperty("config.home") != null)
        {
            this.configHome = System.getProperty("config.home");
        }
        else
        {
            this.configHome = System.getProperty("jetty.home");
        }
        this.webAppDirectory = configHome + File.separator + "webapps";

        // TODO there could potentially be more than one context handler collection and there is
        // also the chance that a web application can be deployed under the same context. These
        // situations should be looked after but are currently not.
        Handler[] handles = server.getChildHandlers();
        for (Handler handle : handles)
        {
            if (handle instanceof ContextHandlerCollection)
            {
                chc = (ContextHandlerCollection) handle;
                break;
            }
        }
    }

    /**
     * Performs the http GET method.
     * @param request The http request
     * @param response The http response
     * @throws ServletException If a servlet exception occurs
     * @throws IOException If an io exception occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String contextPath = request.getParameter("path");
        String warURL = request.getParameter("war");

        String command = request.getServletPath();

        if (command.equals("/deploy"))
        {
            deploy(response, contextPath, warURL);
        }
        else if (command.equals("/undeploy"))
        {
            undeploy(response, contextPath);
        }
        else
        {
            response.sendError(400, "Command " + command + " is unknown");
        }
    }

    /**
     * Performs the http PUT method.
     * @param request The http request
     * @param response The http response
     * @throws ServletException If a servlet exception occurs
     * @throws IOException If an io exception occurs
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        String command = request.getServletPath();
        if (command.equals("/deploy"))
        {
            String contextPath = request.getParameter("path");
            deployArchive(request, response, contextPath);
        }
        else
        {
            sendError(response, "Command " + command + " is not recognized with PUT");
        }
    }

    /**
     * Deploys the archive to the server.
     * @param request The http request
     * @param response The http response
     * @param contextPath The context path for the deployed archive
     * @throws IOException If an io exception occurs
     */
    protected void deployArchive(HttpServletRequest request, HttpServletResponse response,
            String contextPath) throws IOException
    {
        Log.debug("Remotely deploying a remote web archive with context " + contextPath);

        if (contextPath == null)
        {
            sendError(response, "The path variable is not set");
        }
        else if (!contextPath.startsWith("/"))
        {
            sendError(response, "The path variable must start with /");
        }
        else if (getContextHandler(contextPath) != null)
        {
            sendError(response, "The webapp context path is already in use");
        }
        else
        {
            Log.debug("trying to get the remote web archive");
            String webappLocation = webAppDirectory + contextPath
                    + (contextPath.equals("/") ? "ROOT" : "") + ".war";
            File webappFile = new File(webappLocation);
            InputStream inputStream = new BufferedInputStream(request.getInputStream());
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(webappFile),
                8096);

            // transfer the data across
            int i = inputStream.read();
            while (i != -1)
            {
                outputStream.write(i);
                i = inputStream.read();
            }

            // close and flush readers
            inputStream.close();
            outputStream.flush();
            outputStream.close();

            // deploy webapp
            WebAppContext webappcontext = new WebAppContext();
            webappcontext.setContextPath(contextPath);
            webappcontext.setWar(webappLocation);
            webappcontext.setDefaultsDescriptor(configHome + "/etc/webdefault.xml");
            chc.addHandler(webappcontext);
            try
            {
                webappcontext.start();
            }
            catch (Exception e)
            {
                sendError(response, "Unexpected error when trying to start the webapp");
                Log.warn(e);
                return;
            }
        }

        sendMessage(response, "Webapp deployed at context " + contextPath);
    }

    /**
     * Returns the file if it exists for the specified context path. If the file does not exist then
     * it will return null.
     * @param contextPath The context path for the web app
     * @return The file associated with the context path
     */
    protected File getFile(String contextPath)
    {

        String fileName;
        // the contextPath should always begin with a forward slash but adding
        // this logic if case of future modifications and to prevent a NPE.
        int slashPos = contextPath.indexOf('/');
        if (slashPos >= 0)
        {
            fileName = contextPath.substring(slashPos + 1);
        }
        else
        {
            fileName = contextPath;
        }

        File webappArchive = new File(webAppDirectory + File.separator + fileName + ".war");
        File webappDirectory = new File(webAppDirectory + File.separator + fileName);

        File returnedFile = null;

        if (webappArchive.exists())
        {
            returnedFile = webappArchive;
        }
        else if (webappDirectory.exists())
        {
            returnedFile = webappDirectory;
        }

        return returnedFile;
    }

    /**
     * Sends the given message back in the http response.
     * @param response The http response
     * @param message The message to be send
     * @throws IOException If an io exception occurs
     */
    protected void sendMessage(HttpServletResponse response, String message) throws IOException
    {
        PrintWriter writer = response.getWriter();
        writer.println("OK - " + message);
    }

    /**
     * Sends the given error message back in the http response.
     * @param response The http response
     * @param message The error message to be send
     * @throws IOException If an io exception occurs
     */
    protected void sendError(HttpServletResponse response, String message) throws IOException
    {
        PrintWriter writer = response.getWriter();
        writer.println("Error - " + message);
    }

    /**
     * Deploy the war to the given context path.
     * @param response The http response
     * @param contextPath The context path to use
     * @param warURL The location of the war
     * @throws IOException If an io exception occurs
     */
    protected void deploy(HttpServletResponse response, String contextPath, String warURL)
        throws IOException
    {
        String context = contextPath;
        boolean error = false;

        // if the contextPath is null, then we should use the context from the
        // war's name
        if (context == null)
        {
            File file = new File(warURL);
            String fileName = file.getName();
            if (fileName.endsWith(".war"))
            {
                fileName = fileName.substring(0, fileName.lastIndexOf(".war"));
            }
            // need to add a forward slash to the beginning
            context = "/" + fileName;
        }

        // check to make sure that another application is not already deployed
        // to the same context
        if (getContextHandler(context) != null)
        {
            sendError(response, "An application is already deployed at this context : "
                    + context);
            error = true;
        }
        else if (!context.startsWith("/"))
        {
            sendError(response, "The path does not start with a forward slash");
            error = true;
        }

        if (error)
        {
            return;
        }
        else
        {
            String webappDestLocation = webAppDirectory + context + ".war";
            File webappDest = new File(webappDestLocation);

            URI uri = null;
            try
            {
                uri = new URI(warURL);
            }
            catch (URISyntaxException e)
            {
                sendError(response, "Cannot parse URL " + warURL);
                Log.warn(e);
                return;
            }

            File webappSource = new File(uri);

            FileInputStream fileInputStream = new FileInputStream(webappSource);
            FileOutputStream fileOutputStream = new FileOutputStream(webappDest);

            int i = fileInputStream.read();
            while (i != -1)
            {
                fileOutputStream.write(i);
                i = fileInputStream.read();
            }

            fileInputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();

            WebAppContext webappcontext = new WebAppContext();
            webappcontext.setContextPath(context);
            webappcontext.setWar(webappDestLocation);
            chc.addHandler(webappcontext);
            try
            {
                webappcontext.start();
            }
            catch (Exception e)
            {
                sendError(response, "Unexpected error when trying to start the webapp");
                Log.warn(e);
                return;
            }
        }

        sendMessage(response, "Webapp deployed at context " + contextPath);
    }

    /**
     * Undeploy the webapp with the given context path.
     * @param response The http response
     * @param contextPath The context path
     * @throws IOException If an IO exception occurs
     */
    protected void undeploy(HttpServletResponse response, String contextPath) throws IOException
    {
        boolean error = false;
        if (!contextPath.startsWith("/"))
        {
            sendError(response, "Path must start with a forward slash");
            error = true;
        }
        ContextHandler handler = getContextHandler(contextPath);
        if (handler == null)
        {
            sendError(response, "Could not find handler for the context");
            error = true;
        }
        try
        {
            handler.stop();
        }
        catch (Exception e)
        {
            sendError(response, "Could not stop context handler");
            Log.warn(e);
            error = true;
        }

        if (error)
        {
            return;
        }
        else
        {
            chc.removeHandler(handler);

            // NOTE THIS ACTUALLY DELETES THE FILE FROM THE FILESYSTEM
            // PLEASE BE VERY CAREFUL WHEN MAKING CHANGES HERE
            String webAppLocation = getWebAppLocation((WebAppContext) handler);
            File webAppFile = new File(webAppLocation);

            if (!webAppFile.exists())
            {
                sendError(response, "Can't find a valid file for this context path");
            }
            else if (!webAppFile.getPath().startsWith(webAppDirectory))
            {
                sendMessage(response, "Webapp with " + contextPath
                    + " context has been undeployed but not removed from the filesystem");
            }
            else
            {
                boolean deleted = false;
                if (webAppFile.isFile())
                {
                    deleted = webAppFile.delete();
                }
                else
                {
                    // we are dealing with a directory, which is a pain to
                    // delete so
                    // we have to call a separate method
                    deleteDirectory(webAppFile);
                    deleted = !webAppFile.exists();
                }

                if (deleted)
                {
                    sendMessage(response, "Webapp with " + contextPath
                            + " context has been undeployed and removed from the filesystem");
                }
                else
                {
                    sendError(response, "Webapp with " + contextPath
                            + " context has been undeployed"
                            + " but it couldn't be removed from the filesystem");
                }
            }
        }
    }

    /**
     * Recursively delete the webapp directory.
     * @param webAppFile The file to delete
     */
    protected void deleteDirectory(File webAppFile)
    {
        if (webAppFile.isDirectory())
        {
            File[] children = webAppFile.listFiles();
            for (File element : children)
            {
                deleteDirectory(element);
            }
            webAppFile.delete();
        }
        else
        {
            webAppFile.delete();
        }
    }

    /**
     * Returns the file location for the specified webapp.
     * @param webapp The webapp
     * @return The location of the webapp
     */
    protected String getWebAppLocation(WebAppContext webapp)
    {
        // getWar will return either the file location or a URL based on how the
        // webapp was deloyed
        String location = webapp.getWar();

        // manually comparing strings since easier than trying to do it with the
        // java.net.URL methods
        if (location.startsWith("jar:"))
        {
            location = location.substring("jar:".length());
            // sometime it will end with !/ we need to remove this
            if (location.endsWith("!/"))
            {
                location = location.substring(0, location.length() - "!/".length());
            }
        }
        if (location.startsWith("file:"))
        {
            location = location.substring("file:".length());
        }

        return location;
    }

    /**
     * Returns the context handler for the given context.
     * @param context The webapp context
     * @return The context handler
     */
    protected ContextHandler getContextHandler(String context)
    {
        // Note: this is very inefficient, but I think its the only way that
        // will
        // work. It would have been nice if they used a map and a context could
        // have been used to retrieve the handler.
        Handler[] handlers = chc.getHandlers();
        for (Handler handler : handlers)
        {
            if (handler instanceof ContextHandler)
            {
                if (((ContextHandler) handler).getContextPath().equals(context))
                {
                    return (ContextHandler) handler;
                }
            }
        }

        // return null if no instance was found
        return null;
    }

}
