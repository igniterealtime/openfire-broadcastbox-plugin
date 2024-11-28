<%@ page import="java.util.*" %>
<%@ page import="org.ifsoft.orinayo.openfire.*" %>
<%@ page import="org.jivesoftware.openfire.*" %>
<%@ page import="org.jivesoftware.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%

    boolean update = request.getParameter("update") != null;
    String errorMessage = null;

    // Get handle on the plugin
    BroadcastBox plugin = BroadcastBox.self;

    if (update)
    {    
        String port = request.getParameter("port");     
        JiveGlobals.setProperty("orinayo.port", port);   
		
        String portRangeMin = request.getParameter("port_udp");     
        JiveGlobals.setProperty("orinayo.port.udp", portRangeMin); 		
        
        String ipaddr = request.getParameter("ipaddr");     
        JiveGlobals.setProperty("orinayo.ipaddr", ipaddr);      

        String ipaddrPublic = request.getParameter("ipaddr_public");     
        JiveGlobals.setProperty("orinayo.ipaddr.public", ipaddrPublic); 		
        
        String url = request.getParameter("url");     
        JiveGlobals.setProperty("orinayo.url", url);         
        
        String enabled = request.getParameter("enabled");
        JiveGlobals.setProperty("orinayo.enabled", (enabled != null && enabled.equals("on")) ? "true": "false");  		
    }

    String service_url = plugin.getUrl();    

%>
<html>
<head>
   <title><fmt:message key="config.page.settings" /></title>
   <meta name="pageID" content="orinayo-settings"/>
</head>
<body>
<% if (errorMessage != null) { %>
<div class="error">
    <%= errorMessage%>
</div>
<br/>
<% } %>

<div class="jive-table">
<form action="broadcast-box-settings.jsp" method="post">
    <p>
        <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
            <thead> 
            <tr>
                <th colspan="2"><fmt:message key="config.page.settings.description"/></th>
            </tr>
            </thead>
            <tbody>  
            <tr>
                <td nowrap  colspan="2">
                    <input type="checkbox" name="enabled"<%= (JiveGlobals.getProperty("orinayo.enabled", "true").equals("true")) ? " checked" : "" %>>
                    <fmt:message key="config.page.configuration.enabled" />       
                </td>  
            </tr>	
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.ipaddr"/>
                </td>
                <td><input type="text" size="50" maxlength="100" name="ipaddr" required
                       value="<%= JiveGlobals.getProperty("orinayo.ipaddr", plugin.getIpAddress()) %>">
                </td>                               
            </tr>			
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.ipaddr.public"/>
                </td>
                <td><input type="text" size="50" maxlength="100" name="ipaddr_public" required
                       value="<%= JiveGlobals.getProperty("orinayo.ipaddr.public", plugin.getPublicIpAddress()) %>">
                </td>                               
            </tr>                   
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.port"/>
                </td>
                <td><input type="text" size="50" maxlength="100" name="port" required
                       value="<%= JiveGlobals.getProperty("orinayo.port", plugin.getPort()) %>">
                </td>                               
            </tr>  
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.port.udp"/>
                </td>
                <td><input type="text" size="50" maxlength="100" name="port_udp" required
                       value="<%= JiveGlobals.getProperty("orinayo.port.udp", plugin.getUDPPort()) %>">
                </td>                               
            </tr>  	           
            <tr>
                <td align="left" width="150">
                    <fmt:message key="config.page.configuration.url"/>
                </td>
                <td><input type="text" size="50" maxlength="100" name="url" required
                       value="<%= JiveGlobals.getProperty("orinayo.url", service_url) %>">
                </td>                               
            </tr>            
            </tbody>
        </table>
    </p>
   <p>
        <table class="jive-table" cellpadding="0" cellspacing="0" border="0" width="100%">
            <thead> 
            <tr>
                <th colspan="2"><fmt:message key="config.page.configuration.save.title"/></th>
            </tr>
            </thead>
            <tbody>         
            <tr>
                <th colspan="2"><input type="submit" name="update" value="<fmt:message key="config.page.configuration.submit" />">&nbsp;&nbsp;<fmt:message key="config.page.configuration.restart.warning"/></th>
            </tr>       
            </tbody>            
        </table> 
    </p>
</form>
</div>
</body>
</html>
