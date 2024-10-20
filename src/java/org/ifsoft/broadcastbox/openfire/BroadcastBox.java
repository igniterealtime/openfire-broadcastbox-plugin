/*
 * Copyright (C) 2017 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.ifsoft.broadcastbox.openfire;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;
import java.nio.file.*;
import java.nio.charset.*;
import java.security.Security;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.net.SASLAuthentication;
import org.jivesoftware.openfire.http.HttpBindManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.sasl.AnonymousSaslServer;
import org.jivesoftware.openfire.muc.*;
import org.jivesoftware.openfire.user.UserManager;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.jivesoftware.util.StringUtils;

import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.plus.annotation.ContainerInitializer;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.servlets.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.InstanceManager;
import org.apache.tomcat.SimpleInstanceManager;
import org.eclipse.jetty.util.security.*;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.*;

import java.lang.reflect.*;
import java.util.*;

import org.jitsi.util.OSUtils;
import de.mxro.process.*;
import de.mxro.process.internal.*;
import net.sf.json.*;
import org.xmpp.packet.*;

public class BroadcastBox implements Plugin, PropertyEventListener, ProcessListener, MUCEventListener
{
    private static final Logger Log = LoggerFactory.getLogger(BroadcastBox.class);
    private XProcess broadcastboxThread = null;
    private String broadcastboxExePath = null;
    private String broadcastboxHomePath = null;
    private Path broadcastboxRoot = null;
    private ExecutorService executor;
    private WebAppContext jspService;	
    private ServletContextHandler webContext;		
    private Cache muc_properties;	
    private WhipIQHandler whipIQHandler;	
    private WhepIQHandler whepIQHandler;
	
    public static BroadcastBox self;	

    public void destroyPlugin()     {
        PropertyEventDispatcher.removeListener(this);
        MUCEventDispatcher.removeListener(this);		

        try {
            if (executor != null)  executor.shutdown();
            if (broadcastboxThread != null) broadcastboxThread.destory();
            if (jspService != null) HttpBindManager.getInstance().removeJettyHandler(jspService);
			if (webContext != null) HttpBindManager.getInstance().removeJettyHandler(webContext);				
			if (whipIQHandler != null) whipIQHandler.stopHandler();			
			if (whepIQHandler != null) whepIQHandler.stopHandler();				

            Log.info("broadcastbox terminated");
        }
        catch (Exception e) {
            Log.error("BroadcastBox destroyPlugin", e);
        }
    }

    public void initializePlugin(final PluginManager manager, final File pluginDirectory)     {	
		muc_properties = CacheFactory.createLocalCache("MUC Room Properties");	
		
        PropertyEventDispatcher.addListener(this);
        MUCEventDispatcher.addListener(this);

		whipIQHandler = new WhipIQHandler();
		whipIQHandler.startHandler();		
		XMPPServer.getInstance().getIQRouter().addHandler(whipIQHandler);	
		XMPPServer.getInstance().getIQDiscoInfoHandler().addServerFeature("urn:xmpp:whip:0");				
		XMPPServer.getInstance().getIQDiscoInfoHandler().addServerFeature("urn:xmpp:whip:ice:0");
		
		whepIQHandler = new WhepIQHandler();
		whepIQHandler.startHandler();		
		XMPPServer.getInstance().getIQRouter().addHandler(whepIQHandler);	
		XMPPServer.getInstance().getIQDiscoInfoHandler().addServerFeature("urn:xmpp:whep:0");				
		XMPPServer.getInstance().getIQDiscoInfoHandler().addServerFeature("urn:xmpp:whep:ice:0");
		XMPPServer.getInstance().getIQDiscoInfoHandler().addServerFeature("urn:xmpp:whep:ext:0");		
		
        checkNatives(pluginDirectory);
        executor = Executors.newCachedThreadPool();
        startJSP(pluginDirectory);
        startGoProcesses(pluginDirectory);
        self = this;					
		
        Log.info("broadcastbox initiated");
    }

    public static String getPort() {
        return "8080";
    }

    public static String getUDPPort() {
        return "10000";
    }

    public String getHome() {
        return broadcastboxHomePath;
    }

    public static String getUrl() {
        return "http://" + XMPPServer.getInstance().getServerInfo().getHostname() + ":" + getPort();
    }

    public static String getIpAddress() {
        String ourHostname = XMPPServer.getInstance().getServerInfo().getHostname();
        String ourIpAddress = "127.0.0.1";

        try {
            ourIpAddress = InetAddress.getByName(ourHostname).getHostAddress();
        } catch (Exception e) {

        }

        return ourIpAddress;
    }

	public static String getPublicIpAddress() {
		return "";
	}
	
    public void onOutputLine(final String line)     {
        Log.info("onOutputLine " + line);
    }

    public void onProcessQuit(int code)     {
        Log.info("onProcessQuit " + code);
    }

    public void onOutputClosed() {
        Log.error("onOutputClosed");
    }

    public void onErrorLine(final String line)     {
        Log.info(line);
    }

    public void onError(final Throwable t)     {
        Log.error("Thread error", t);
    }

    private void startJSP(File pluginDirectory)     {
        jspService = new WebAppContext(null, pluginDirectory.getPath() + "/classes/jsp",  "/broadcastbox-jsp");
        jspService.setClassLoader(this.getClass().getClassLoader());
        jspService.getMimeTypes().addMimeMapping("wasm", "application/wasm");

        final List<ContainerInitializer> initializers = new ArrayList<>();
        initializers.add(new ContainerInitializer(new JettyJasperInitializer(), null));
        jspService.setAttribute("org.eclipse.jetty.containerInitializers", initializers);
        jspService.setAttribute(InstanceManager.class.getName(), new SimpleInstanceManager());		

        Log.info("BroadcastBox jsp service enabled");
        HttpBindManager.getInstance().addJettyHandler(jspService);
    }

    private void startGoProcesses(File pluginDirectory)     {
        boolean broadcastboxEnabled = JiveGlobals.getBooleanProperty("broadcastbox.enabled", true);

        if (broadcastboxExePath != null && broadcastboxEnabled)	{
			String publicIpaddr = JiveGlobals.getProperty("broadcastbox.ipaddr.public", getPublicIpAddress());
			String ipaddr = JiveGlobals.getProperty("broadcastbox.ipaddr", getIpAddress());
			String tcpPort = JiveGlobals.getProperty("broadcastbox.port", getPort());	
			String udpPort = JiveGlobals.getProperty("broadcastbox.port.udp", getUDPPort());			
			String webUrl = "http://" + ipaddr + ":" + tcpPort;
			
			Engine.environment.put("APP_ENV", "production");
			Engine.environment.put("HTTP_ADDRESS", ipaddr + ":" + tcpPort);
			Engine.environment.put("UDP_MUX_PORT_WHEP", udpPort);
			Engine.environment.put("UDP_MUX_PORT_WHIP", udpPort);			
			Engine.environment.put("UDP_MUX_PORT", udpPort);
			Engine.environment.put("NETWORK_TEST_ON_START", "false");
			
			if (publicIpaddr != null && !publicIpaddr.trim().equals("")) {
				Engine.environment.put("NAT_1_TO_1_IP", publicIpaddr);
			}
            
			Engine.environment.put("STUN_SERVERS", "stun1.l.google.com:19305|stun1.l.google.com:19302|stun4.l.google.com:19302|stun.frozenmountain.com:3478|stun.freeswitch.org:3478");			

 			broadcastboxThread = Spawn.startProcess(broadcastboxExePath, new File(broadcastboxHomePath), this);	

            webContext = new ServletContextHandler(null, "/", ServletContextHandler.SESSIONS);
            webContext.setClassLoader(this.getClass().getClassLoader());			
			ServletHolder proxyServlet = new ServletHolder(ProxyServlet.Transparent.class);
			proxyServlet.setInitParameter("proxyTo", webUrl);
			proxyServlet.setInitParameter("prefix", "/");
			webContext.addServlet(proxyServlet, "/*");	
			HttpBindManager.getInstance().addJettyHandler(webContext);
			
            Log.info("BroadcastBox enabled " + broadcastboxExePath);

        } else {
            Log.info("BroadcastBox disabled");
        }
    }

    private void checkNatives(File pluginDirectory)     {
        try
        {
            broadcastboxRoot = JiveGlobals.getHomePath().resolve("broadcastbox");

            if (!Files.exists(broadcastboxRoot))
            {
                Files.createDirectories(broadcastboxRoot);
            }
			

            broadcastboxHomePath = pluginDirectory.getAbsolutePath() + File.separator + "classes";

            if(OSUtils.IS_LINUX64)
            {
                broadcastboxHomePath = broadcastboxHomePath + File.separator + "linux-64";
                broadcastboxExePath = broadcastboxHomePath + File.separator + "broadcast-box";
                makeFileExecutable(broadcastboxExePath);
            }
            else if(OSUtils.IS_WINDOWS64)
            {
                broadcastboxHomePath = broadcastboxHomePath + File.separator + "win-64";
                broadcastboxExePath = broadcastboxHomePath + File.separator + "broadcast-box.exe";
                makeFileExecutable(broadcastboxExePath);				

            } else {
                Log.error("checkNatives unknown OS " + pluginDirectory.getAbsolutePath());
                return;
            }
        }
        catch (Exception e)
        {
            Log.error("checkNatives error", e);
        }
    }

    private void makeFileExecutable(String path)     {
        File file = new File(path);
        file.setReadable(true, true);
        file.setWritable(true, true);
        file.setExecutable(true, true);
        Log.info("checkNatives broadcastbox executable path " + path);
    }

    // -------------------------------------------------------
    //
    //  MUCEventListener
    //
    // -------------------------------------------------------

    public void roomCreated(JID roomJID)     {
		if (JiveGlobals.getBooleanProperty("broadcastbox.muc.enabled", false)) {		
			MUCRoom room = XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatService(roomJID).getChatRoom(roomJID.getNode());
		}
    }

    public void roomDestroyed(JID roomJID)     {

    }

    public void occupantJoined(JID roomJID, JID user, String nickname) {

    }

    public void occupantLeft(JID roomJID, JID user, String nickname) {

    }

	public void occupantNickKicked(JID roomJID, String nickname) {
		
	}
	
    public void nicknameChanged(JID roomJID, JID user, String oldNickname, String newNickname) {

    }

    public void messageReceived(JID roomJID, JID user, String nickname, Message message)  {
		final String from = user.toBareJID();
		final String room = roomJID.getNode();	
		final String body = message.getBody();
				
    }

    public void roomSubjectChanged(JID roomJID, JID user, String newSubject) {

    }

    public void privateMessageRecieved(JID a, JID b, Message message) {

    }
	
    //-------------------------------------------------------
    //
    //  PropertyEventListener
    //
    //-------------------------------------------------------


    public void propertySet(String property, Map params) {

    }

    public void propertyDeleted(String property, Map<String, Object> params) {

    }

    public void xmlPropertySet(String property, Map<String, Object> params) {

    }

    public void xmlPropertyDeleted(String property, Map<String, Object> params) {

    }

	
    //-------------------------------------------------------
    //
    //  Utility methods
    //
    //-------------------------------------------------------	

	public String getJson(String urlToRead)  {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		StringBuilder result = new StringBuilder();
		String ipaddr = JiveGlobals.getProperty("broadcastbox.ipaddr", getIpAddress());
		String tcpPort = JiveGlobals.getProperty("broadcastbox.port", getPort());			
		String uri = "http://" + ipaddr + ":" + tcpPort + urlToRead;

		try {
			url = new URL(uri);
			conn = (HttpURLConnection) url.openConnection();		
			conn.setRequestMethod("GET");
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();

		} catch (Exception e) {
			Log.error("getJson", e);
		}

		return result.toString();
	}	
}