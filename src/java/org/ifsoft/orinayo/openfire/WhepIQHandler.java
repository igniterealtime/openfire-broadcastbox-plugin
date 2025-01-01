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
 
package org.ifsoft.orinayo.openfire;

import org.dom4j.Element;
import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.handler.IQHandler;
import org.jivesoftware.openfire.disco.ServerFeaturesProvider;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.muc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.Presence;
import org.xmpp.packet.Message;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import java.nio.charset.StandardCharsets;
import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;

import net.sf.json.*;

/**
 * custom IQ handler for WHEP
 */
public class WhepIQHandler extends IQHandler implements ServerFeaturesProvider
{
    private final static Logger Log = LoggerFactory.getLogger( WhepIQHandler.class );	
	private final static String domain = XMPPServer.getInstance().getServerInfo().getXMPPDomain();
	
    public static final String ELEMENT_NAME = "whep";
    public static final String NAMESPACE1 = "urn:xmpp:whep:0";
    public static final String NAMESPACE2 = "urn:xmpp:whep:ice:0";	
    public static final String NAMESPACE3 = "urn:xmpp:whep:ext:0";	
	
	public void startHandler() {

	}

	public void stopHandler() {
	
	}
	
    public WhepIQHandler() {
        super("Whep IQ Handler");
    }

    @Override
    public IQ handleIQ(IQ iq)
    {
		if (iq.getType() == IQ.Type.set) {
			IQ reply = IQ.createResultIQ(iq);

			try {
				Log.debug("Whep handleIQ \n" + iq.toString());
				final Element whep = iq.getChildElement();
				final String id = whep.attribute("id").getText();
					
				if (whep != null) {
					final Element sdp = whep.element("sdp");
					final String offer = sdp.getText();
					
					final String ipaddr = JiveGlobals.getProperty("orinayo.ipaddr", BroadcastBox.getIpAddress());
					final String tcpPort = JiveGlobals.getProperty("orinayo.port", BroadcastBox.getPort());				
					final String webUrl = "http://" + ipaddr + ":" + tcpPort + "/api/whep";
					
					final String answer = getSDP(webUrl, id, offer);
					final Element childElement = reply.setChildElement(ELEMENT_NAME, NAMESPACE1);					
					childElement.addElement("sdp").setText(answer);
				}
				else {
					reply.setError(new PacketError(PacketError.Condition.not_allowed, PacketError.Type.modify, "request element is missing"));
				}
				return reply;

			} catch(Exception e) {
				Log.error("Whep handleIQ", e);
				reply.setError(new PacketError(PacketError.Condition.internal_server_error, PacketError.Type.modify, e.toString()));
				return reply;
			}
		}
		return null;
    }						

    @Override
    public IQHandlerInfo getInfo() {
        return new IQHandlerInfo(ELEMENT_NAME, NAMESPACE1);
    }
	
    @Override
    public Iterator<String> getFeatures()
    {
        final ArrayList<String> features = new ArrayList<>();
        features.add( NAMESPACE1 );
		features.add( NAMESPACE2 );
		features.add( NAMESPACE3 );		
        return features.iterator();
    }		
	
	private String getSDP(String urlToRead, String streamKey, String sdp)  {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String accumulator = "";
		StringBuilder result = new StringBuilder();
		String authHeaderValue = "Bearer " + streamKey;
		
		Log.info("getSDP offer " + urlToRead + " " + authHeaderValue + "\n" + sdp);
		
		try {
			url = new URL(urlToRead);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("Authorization", authHeaderValue);			

			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");  
			conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.setRequestProperty("Charset", "UTF-8");
			conn.setRequestProperty("Accept", "text/event-stream");
			
			conn.getOutputStream().write(sdp.getBytes(StandardCharsets.UTF_8));
			rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			while ((line = rd.readLine()) != null) {
				accumulator = accumulator + line + "\n";				
			}
			rd.close();				

		} catch (Exception e) {
			Log.error("getSDP", e);
		}
		Log.info("getSDP answer " + urlToRead + " " + authHeaderValue + "\n" + accumulator);		
		return accumulator;
	}	
}
