<%@ page import="org.jivesoftware.util.*,
				 org.ifsoft.broadcastbox.openfire.*,
                 java.util.*,
				 net.sf.json.*,
                 java.net.URLEncoder"                 
    errorPage="error.jsp"
%>
<%@ page import="org.xmpp.packet.JID" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>

<% 
    BroadcastBox plugin = BroadcastBox.self;

	// [{"streamKey":"Bearer deleolajide","whepSessionsCount":1}]
	
	JSONArray connections = new JSONArray();
	String json = BroadcastBox.self.getJson("/api/status");
	
	if (json != null && !"".equals(json)) {
		connections = new JSONArray(json);
	}
	
	int sessionCount = connections.length();	
%>
<html>
    <head>
        <title><fmt:message key="config.page.summary"/></title>
        <meta name="pageID" content="broadcastbox-summary"/>
    </head>
    <body>

<%
	String service_url = JiveGlobals.getProperty("broadcastbox.url",  plugin.getUrl());
	String publish_url = service_url + "/";	
%>	

<% if (request.getParameter("deletesuccess") != null) { %>

    <div class="jive-success">
    <table cellpadding="0" cellspacing="0" border="0">
    <tbody>
        <tr><td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt=""></td>
        <td class="jive-icon-label">
        <fmt:message key="broadcastbox.session.expired" />
        </td></tr>
    </tbody>
    </table>
    </div><br>

<%  
	} 
%>	
<div class='jive-contentBox'>
<fmt:message key="broadcastbox.client.url.desc">
	<fmt:param value="<%=publish_url%>"/>
</fmt:message>
<p>&nbsp;</p>
<table cellpadding="0" cellspacing="0" border="0" width="100%">
<thead>
    <tr>
        <th width="50%" nowrap align="left"><fmt:message key="broadcastbox.client.stream.id" /></th>
        <th width="50%" nowrap align="center"><fmt:message key="broadcastbox.client.stream.subscribers" /></th>		  		
    </tr>
</thead>
<tbody>

<% 

	for (int s=0; s<connections.length(); s++) {
		JSONObject connection = connections.getJSONObject(s);
		String roomName = connection.getString("streamKey");
		int subscribers = connection.getJSONArray("whepSessions").length();
		String subscribe_url = service_url + "/" + roomName;	
%>
		<tr>
			<td width="50%" nowrap align="left"><a href="<%= subscribe_url %>"><%= roomName %></a></td>	
			<td width="50%" nowrap align="center"><%= subscribers %></td>
		</tr>
<%	
      }
%>		
</tbody>
</table>
</div>
</body>
</html>