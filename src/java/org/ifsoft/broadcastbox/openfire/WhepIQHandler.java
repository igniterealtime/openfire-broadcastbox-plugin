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
import java.util.concurrent.*;

import net.sf.json.*;

/**
 * custom IQ handler for WHEP
 */
public class WhepIQHandler extends IQHandler implements ServerFeaturesProvider
{
    private final static Logger Log = LoggerFactory.getLogger( WhepIQHandler.class );	
	private final static String domain = XMPPServer.getInstance().getServerInfo().getXMPPDomain();
	
    public static final String ELEMENT_NAME = "whip";
    public static final String NAMESPACE1 = "urn:xmpp:whip:0";
    public static final String NAMESPACE2 = "urn:xmpp:whip:ice:0";	
    public static final String NAMESPACE3 = "urn:xmpp:whip:ext:0";	
	
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
		if (iq.getType() == IQ.Type.get) {
			IQ reply = IQ.createResultIQ(iq);

			try {
				Log.debug("Whep handleIQ \n" + iq.toString());
				final Element element = iq.getChildElement();
				final String from = iq.getFrom().toBareJID();
					
				if (element != null) {
					final Element childElement = reply.setChildElement(ELEMENT_NAME, NAMESPACE1);					
					final String offer = childElement.element("sdp").getText();
					final String answer = getSDPAnswer(offer);
					childElement.addElement("sdp").setText(answer);
					
					// TODO	- create/config PEP node
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
	
	private String getSDPAnswer(String offer) {
		String answer = "";
		String ipaddr = JiveGlobals.getProperty("broadcastbox.ipaddr", BroadcastBox.getIpAddress());
		String tcpPort = JiveGlobals.getProperty("broadcastbox.port", BroadcastBox.getPort());				
		String webUrl = "http://" + ipaddr + ":" + tcpPort;
		
		// TODO
		return answer;
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
}
