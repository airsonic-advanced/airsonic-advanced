<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<%--
  ~ This file is part of Airsonic.
  ~
  ~  Airsonic is free software: you can redistribute it and/or modify
  ~  it under the terms of the GNU General Public License as published by
  ~  the Free Software Foundation, either version 3 of the License, or
  ~  (at your option) any later version.
  ~
  ~  Airsonic is distributed in the hope that it will be useful,
  ~  but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~  Copyright 2015 (C) Sindre Mehus
  --%>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/scripts.js'/>"></script>
</head>

<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value='/script/wz_tooltip.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/tip_balloon.js'/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="sonos"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form method="post" action="sonosSettings.view">
    <sec:csrfInput/>

    <div>
        <input type="checkbox" name="sonosEnabled" id="sonosEnabled" ${model.sonosEnabled ? "checked" : ""}/>
        <label for="sonosEnabled"><fmt:message key="sonossettings.enabled"/></label>
    </div>

    <p class="detail" style="width:60%;white-space:normal"><fmt:message key="sonossettings.description"/></p>

    <p><label><fmt:message key="sonossettings.linkMethod"/></label></p>
    <div><label>
        <input type="radio" name="sonosLinkMethod" value="APPLICATION_LINK"
               <c:if test="${model.sonosLinkMethod == 'APPLICATION_LINK'}">checked="checked"</c:if>>
        <fmt:message key="sonossettings.linkMethod.applicationLink"/>
    </label></div>
    <p class="detail" style="width:60%;white-space:normal;padding-top:0">
        <fmt:message key="sonossettings.linkMethod.applicationLink.description"/>
    </p>
    <p class="detail" style="width:60%;white-space:normal"><fmt:message key="sonossettings.existinglinks"/>
    <table id="sonosLinksTable">
        <tr>
          <th style="padding:0 0.5em 0 0.5em;border-style:double">Linkcode</th>
          <th style="padding:0 0.5em 0 0.5em;border-style:double">User</th>
          <th style="padding:0 0.5em 0 0.5em;border-style:double">Household</th>
          <th style="padding:0 0.5em 0 0.5em;border-style:double">Initiator</th>
          <th style="padding:0 0.5em 0 0.5em;border-style:double">Initiated</th>
        </tr>
        <tr>
            <td style="text-align:center;border-style:dotted" colspan=5><fmt:message key="sonossettings.existinglinks"/></td>
        </tr>
      <c:forEach items="${model.existingLinks}" var="elink">
        <tr>
          <td style="padding:0 0.5em 0 0.5em">${elink.linkcode}</td>
          <td style="padding:0 0.5em 0 0.5em">${elink.username}</td>
          <td style="padding:0 0.5em 0 0.5em">${elink.householdId}</td>
          <td style="padding:0 0.5em 0 0.5em">${elink.initiator}</td>
          <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${elink.initiated}" style="SS" /></td>
        </tr>
      </c:forEach>
        <tr>
            <td style="text-align:center;border-style:dotted" colspan=5><fmt:message key="sonossettings.pendinglinks"/></td>
        </tr>
      <c:forEach items="${model.pendingLinks}" var="plink">
        <tr>
          <td style="padding:0 0.5em 0 0.5em">${plink.key}</td>
          <td style="padding:0 0.5em 0 0.5em"></td>
          <td style="padding:0 0.5em 0 0.5em">${plink.value.left}</td>
          <td style="padding:0 0.5em 0 0.5em">${plink.value.middle}</td>
          <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${plink.value.right}" style="SS" /></td>
        </tr>
      </c:forEach>
    </table>
    </p>
    <div><label>
        <input type="radio" name="sonosLinkMethod" value="ANONYMOUS"
               <c:if test="${model.sonosLinkMethod == 'ANONYMOUS'}">checked="checked"</c:if>>
        <fmt:message key="sonossettings.linkMethod.anonymous"/>
    </label></div>
    <p class="detail" style="width:60%;white-space:normal;padding-top:0">
        <fmt:message key="sonossettings.linkMethod.anonymous.description"/>
    </p>
    <p></p>
    <p></p>
    <div>
        <fmt:message key="sonossettings.callbackHostAddress"/>
        <input name="callbackHostAddress" id="callbackHostAddress" size="80"
               value="<c:out value="${model.callbackHostAddress}" escapeXml="true"/>"/>
    </div>
    <p class="detail" style="width:60%;white-space:normal;padding-top:0">
        <fmt:message key="sonossettings.callbackHostAddress.description"/>
    </p>
    <p></p>

    <div>
        <fmt:message key="sonossettings.servicename"/>
        <input name="sonosServiceName" id="sonosServiceName" size="40"
               value="<c:out value='${model.sonosServiceName}' escapeXml='true'/>"/>
    </div>
    <p class="detail" style="width:60%;white-space:normal;padding-top:0">
        <fmt:message key="sonossettings.servicename.description"/>
    </p>
    <p></p>

    <p>
        <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em">
        <a href='nowPlaying.view'><input type="button" value="<fmt:message key='common.cancel'/>"></a>
    </p>

    <p></p>
    <h2 <c:if test="${empty model.returnCodes}">hidden</c:if>><fmt:message key="sonossettings.returnCodes"/></h2>
    <p></p>
    <div style="padding:5" class="log indent" <c:if test="${empty model.returnCodes}">hidden</c:if>>
        <c:forEach items="${model.returnCodes}" var="returnCode">
            <p><fmt:message key="${returnCode}"/></p>
        </c:forEach>
    </div>


</form>

</body>
</html>
