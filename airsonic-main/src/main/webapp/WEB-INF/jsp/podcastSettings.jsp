<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%--@elvariable id="command" type="org.airsonic.player.command.PodcastSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
</head>
<body class="mainframe bgcolor1">

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="podcast"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form:form modelAttribute="command" action="podcastSettings.view" method="post">

<table class="indent">
    <tr>
        <th><fmt:message key="podcastsettings.podcast"/></th>
        <th><fmt:message key="podcastsettings.update"/></th>
        <th><fmt:message key="podcastsettings.keep"/></th>
        <th><fmt:message key="podcastsettings.download"/></th>
        <th style="padding-left:1em"><fmt:message key="common.delete"/></th>
    </tr>
  <c:forEach items="${command.rules}" var="rule" varStatus="loopStatus">
    <tr>
        <td>
          ${rule.id} - ${fn:escapeXml(rule.name)}
          <form:hidden path="rules[${loopStatus.index}].id" value="${rule.id}" />
        </td>
        <td>
            <form:select path="rules[${loopStatus.index}].interval" cssStyle="width:20em">
                <fmt:message key="podcastsettings.interval.manually" var="never"/>
                <fmt:message key="podcastsettings.interval.hourly" var="hourly"/>
                <fmt:message key="podcastsettings.interval.daily" var="daily"/>
                <fmt:message key="podcastsettings.interval.weekly" var="weekly"/>

                <form:option value="-1" label="${never}"/>
                <form:option value="1" label="${hourly}"/>
                <form:option value="24" label="${daily}"/>
                <form:option value="168" label="${weekly}"/>
            </form:select>
        </td>
        <td>
            <form:select path="rules[${loopStatus.index}].episodeRetentionCount" cssStyle="width:20em">
                <fmt:message key="podcastsettings.keep.all" var="all"/>
                <fmt:message key="podcastsettings.keep.one" var="one"/>

                <form:option value="-1" label="${all}"/>
                <form:option value="1" label="${one}"/>

                <c:forTokens items="2 3 4 5 10 20 30 50" delims=" " var="count">
                    <fmt:message key="podcastsettings.keep.many" var="many"><fmt:param value="${count}"/></fmt:message>
                    <form:option value="${count}" label="${many}"/>
                </c:forTokens>

            </form:select>
        </td>
        <td>
            <form:select path="rules[${loopStatus.index}].episodeDownloadCount" cssStyle="width:20em">
                <fmt:message key="podcastsettings.download.all" var="all"/>
                <fmt:message key="podcastsettings.download.one" var="one"/>
                <fmt:message key="podcastsettings.download.none" var="none"/>

                <form:option value="-1" label="${all}"/>
                <form:option value="1" label="${one}"/>

                <c:forTokens items="2 3 4 5 10" delims=" " var="count">
                    <fmt:message key="podcastsettings.download.many" var="many"><fmt:param value="${count}"/></fmt:message>
                    <form:option value="${count}" label="${many}"/>
                </c:forTokens>
                <form:option value="0" label="${none}"/>

            </form:select>
        </td>
        <td align="center" style="padding-left:1em"><c:if test="${rule.id != -1}"><form:checkbox path="rules[${loopStatus.index}].delete" cssClass="checkbox"/></c:if></td>
    </tr>
  </c:forEach>
  <tr>
    <th colspan="5" align="left" style="padding-top:1em"><fmt:message key="podcastsettings.ruleadd"/></th>
  </tr>
  <tr>
        <td>
            <form:select path="newRule.id" cssStyle="width:13em">
                <form:option selected="true" value="" label="-"/>
              <c:forEach items="${command.noRuleChannels}" var="channel" varStatus="loopStatus">
                <form:option value="${channel.id}" label="${channel.id} - ${fn:escapeXml(channel.name)}"/>
              </c:forEach>
            </form:select>
        </td>
        <td>
            <form:select path="newRule.interval" cssStyle="width:20em">
                <fmt:message key="podcastsettings.interval.manually" var="never"/>
                <fmt:message key="podcastsettings.interval.hourly" var="hourly"/>
                <fmt:message key="podcastsettings.interval.daily" var="daily"/>
                <fmt:message key="podcastsettings.interval.weekly" var="weekly"/>

                <form:option value="-1" label="${never}"/>
                <form:option value="1" label="${hourly}"/>
                <form:option value="24" label="${daily}"/>
                <form:option value="168" label="${weekly}"/>
            </form:select>
        </td>
        <td>
            <form:select path="newRule.episodeRetentionCount" cssStyle="width:20em">
                <fmt:message key="podcastsettings.keep.all" var="all"/>
                <fmt:message key="podcastsettings.keep.one" var="one"/>

                <form:option value="-1" label="${all}"/>
                <form:option value="1" label="${one}"/>

                <c:forTokens items="2 3 4 5 10 20 30 50" delims=" " var="count">
                    <fmt:message key="podcastsettings.keep.many" var="many"><fmt:param value="${count}"/></fmt:message>
                    <form:option value="${count}" label="${many}"/>
                </c:forTokens>

            </form:select>
        </td>
        <td>
            <form:select path="newRule.episodeDownloadCount" cssStyle="width:20em">
                <fmt:message key="podcastsettings.download.all" var="all"/>
                <fmt:message key="podcastsettings.download.one" var="one"/>
                <fmt:message key="podcastsettings.download.none" var="none"/>

                <form:option value="-1" label="${all}"/>
                <form:option value="1" label="${one}"/>

                <c:forTokens items="2 3 4 5 10" delims=" " var="count">
                    <fmt:message key="podcastsettings.download.many" var="many"><fmt:param value="${count}"/></fmt:message>
                    <form:option value="${count}" label="${many}"/>
                </c:forTokens>
                <form:option value="0" label="${none}"/>

            </form:select>
        </td>
        <td align="center" style="padding-left:1em"></td>
    </tr>
</table>
<div class="tableSpacer"></div>
<div>
    <fmt:message key="podcastsettings.folder" />
    <form:select path="folderId" items="${command.folders}" cssStyle="width:20em"></form:select>
</div>
<div class="tableSpacer"></div>
<div>
    <span><fmt:message key="podcastsettings.podcastindexintegration" /></span>
</div>
<div class="tableSpacer"></div>
<div>
    <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em">
    <a href='nowPlaying.view'><input type="button" value="<fmt:message key='common.cancel'/>"></a>
</div>

</form:form>

</body></html>