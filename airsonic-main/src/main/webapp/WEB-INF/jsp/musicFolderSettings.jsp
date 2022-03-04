<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%--@elvariable id="command" type="org.airsonic.player.command.MusicFolderSettingsCommand"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>

    <script type="text/javascript">
        function init() {
            $("#newMusicFolderName").attr("placeholder", "<fmt:message key="musicfoldersettings.name"/>");
            $("#newMusicFolderPath").attr("placeholder", "<fmt:message key="musicfoldersettings.path"/>");

            <c:if test="${settings_reload}">
            parent.frames.left.location.href="left.view?";
            </c:if>

            updateClearFullScan();

            $("#fullScan").change(function() { updateClearFullScan(); });
        }

        function updateClearFullScan() {
            if (!$("#fullScan").prop("checked")) {
                $("#clearFullScanSettingAfterScan").prop("disabled", true);
                $("#clearFullScanSettingAfterScan").prop("checked", false);
            } else {
                $("#clearFullScanSettingAfterScan").prop("disabled", false);
            }
        }

        function podcastEnabler(event, el) {
            $('.podcast-enable-radio[value="false"]').prop("checked", true);
            $(el).prop("checked", true);
        }
    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">
<script type="text/javascript" src="<c:url value='/script/wz_tooltip.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/tip_balloon.js'/>"></script>


<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="musicFolder"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form:form modelAttribute="command" action="musicFolderSettings.view" method="post">

<table class="indent">
    <tr>
        <th><fmt:message key="musicfoldersettings.name"/></th>
        <th><fmt:message key="musicfoldersettings.path"/></th>
        <th><fmt:message key="status.type"/></th>
        <th><fmt:message key="musicfoldersettings.enabled"/></th>
        <th><fmt:message key="common.delete"/></th>
        <th></th>
    </tr>

    <c:forEach items="${command.musicFolders}" var="folder" varStatus="loopStatus">
        <tr>
            <td><form:input path="musicFolders[${loopStatus.index}].name" style="width:90%"/></td>
            <td><form:input path="musicFolders[${loopStatus.index}].path" style="width:90%"/></td>
            <td align="center"><span><c:out value="${folder.type}"/></span></td>
            <td align="center">
              <c:if test="${folder.type != 'PODCAST'}">
                <form:checkbox path="musicFolders[${loopStatus.index}].enabled" cssClass="checkbox"/>
              </c:if>
              <c:if test="${folder.type == 'PODCAST'}">
                <form:radiobutton path="musicFolders[${loopStatus.index}].enabled" value="true" cssClass="podcast-enable-radio" onchange="podcastEnabler(event, this)"/>
                <form:radiobutton path="musicFolders[${loopStatus.index}].enabled" value="false" cssClass="podcast-enable-radio" cssStyle="display:none;"/>
              </c:if>
            </td>
            <td align="center"><form:checkbox path="musicFolders[${loopStatus.index}].delete" cssClass="checkbox"/></td>
            <td>
              <c:if test="${not folder.existing}"><span class="warning"><fmt:message key="musicfoldersettings.notfound"/></span></c:if>
              <c:if test="${folder.overlap}"><span><fmt:message key="musicfoldersettings.overlap"><fmt:param value="${folder.overlapStatus}"/></fmt:message></span></c:if>
            </td>
        </tr>
    </c:forEach>

    <tr>
        <td colspan="6" align="left" style="padding-top:1em"><span class="detail"><fmt:message key="musicfoldersettings.podcastfoldernote"/></span></td>
    </tr>

    <tr>
        <th colspan="6" align="left" style="padding-top:1em"><fmt:message key="musicfoldersettings.deleted"/></th>
    </tr>

    <c:forEach items="${command.deletedMusicFolders}" var="folder" varStatus="loopStatus">
        <tr>
            <td><span><c:out value="${folder.name}"/></span></td>
            <td><span><c:out value="${folder.path}"/></span></td>
            <td align="center"><span><c:out value="${folder.type}"/></span></td>
            <td></td>
            <td></td>
            <td>
              <c:if test="${not folder.existing}"><span class="warning"><fmt:message key="musicfoldersettings.notfound"/></span></c:if>
              <c:if test="${folder.overlap}"><span><fmt:message key="musicfoldersettings.overlap"><fmt:param value="${folder.overlapStatus}"/></fmt:message></span></c:if>
            </td>
        </tr>
    </c:forEach>

    <tr>
        <td colspan="6" align="left" style="padding-top:1em"><span class="detail"><fmt:message key="musicfoldersettings.deletenote"/></span></td>
    </tr>

    <c:if test="${not empty command.musicFolders}">
        <tr>
            <th colspan="6" align="left" style="padding-top:1em"><fmt:message key="musicfoldersettings.add"/></th>
        </tr>
    </c:if>

    <tr>
        <td><form:input id="newMusicFolderName" path="newMusicFolder.name" style="width:90%"/></td>
        <td><form:input id="newMusicFolderPath" path="newMusicFolder.path" style="width:90%"/></td>
        <td align="center">
          <form:select id="newMusicFolderType" path="newMusicFolder.type" >
            <form:options items="${command.musicFolderTypes}" />
          </form:select>
        </td>
        <td align="center"><form:checkbox path="newMusicFolder.enabled" cssClass="checkbox"/></td>
        <td></td>
        <td></td>
    </tr>

</table>

    <p class="forward"><a href="userSettings.view"><fmt:message key="musicfoldersettings.access"/></a></p>
    <p class="detail" style="width:60%;white-space:normal;margin-top:-10px;">
        <fmt:message key="musicfoldersettings.access.description"/>
    </p>
    
    <div>
	<fmt:message key="musicfoldersettings.uploadsfolder"/>
	<form:input path="uploadsFolder" size="70"/>
        <c:import url="helpToolTip.jsp"><c:param name="topic" value="uploadsfolderpattern"/></c:import>
    </div>

    <div>
	<fmt:message key="musicfoldersettings.excludepattern"/>
	<form:input path="excludePatternString" size="70"/>
        <c:import url="helpToolTip.jsp"><c:param name="topic" value="excludepattern"/></c:import>
    </div>

    <div>
	<form:checkbox path="ignoreSymLinks" id="ignoreSymLinks"/>
     	<form:label path="ignoreSymLinks"><fmt:message key="musicfoldersettings.ignoresymlinks"/></form:label>
    </div>

    <div>
        <form:checkbox path="fullScan" cssClass="checkbox" id="fullScan"/>
        <form:label path="fullScan"><fmt:message key="musicfoldersettings.fullscan"/></form:label>
        <c:import url="helpToolTip.jsp"><c:param name="topic" value="fullscan"/></c:import>
    </div>
    <div style="margin-left:1em;">
        <form:checkbox path="clearFullScanSettingAfterScan" cssClass="checkbox" id="clearFullScanSettingAfterScan"/>
        <form:label path="clearFullScanSettingAfterScan"><fmt:message key="musicfoldersettings.fullscanclear"/></form:label>
        <c:import url="helpToolTip.jsp"><c:param name="topic" value="clearfullscan"/></c:import>
    </div>

    <div style="padding-top: 0.5em;padding-bottom: 0.3em">
        <span style="white-space: nowrap">
            <fmt:message key="musicfoldersettings.scan"/>
            <form:select path="interval">
                <fmt:message key="musicfoldersettings.interval.never" var="never"/>
                <fmt:message key="musicfoldersettings.interval.one" var="one"/>
                <form:option value="-1" label="${never}"/>
                <form:option value="1" label="${one}"/>

                <c:forTokens items="2 3 7 14 30 60" delims=" " var="interval">
                    <fmt:message key="musicfoldersettings.interval.many" var="many"><fmt:param value="${interval}"/></fmt:message>
                    <form:option value="${interval}" label="${many}"/>
                </c:forTokens>
            </form:select>
            <form:select path="hour">
                <c:forEach begin="0" end="23" var="hour">
                    <fmt:message key="musicfoldersettings.hour" var="hourLabel"><fmt:param value="${hour}"/></fmt:message>
                    <form:option value="${hour}" label="${hourLabel}"/>
                </c:forEach>
            </form:select>
        </span>
    </div>

    <table>
        <tr>
            <td><div class="forward"><a href="musicFolderSettings.view?scanNow"><fmt:message key="musicfoldersettings.scannow"/></a></div></td>
            <td><c:import url="helpToolTip.jsp"><c:param name="topic" value="scanMediaFolders"/></c:import></td>
        </tr>
    </table>

    <c:if test="${command.scanning}">
        <p style="width:60%"><b><fmt:message key="musicfoldersettings.nowscanning"/></b></p>
    </c:if>

    <div>
        <form:checkbox path="fastCache" cssClass="checkbox" id="fastCache"/>
        <form:label path="fastCache"><fmt:message key="musicfoldersettings.fastcache"/></form:label>
    </div>

    <p class="detail" style="width:60%;white-space:normal;">
        <fmt:message key="musicfoldersettings.fastcache.description"/>
    </p>

    <p class="forward"><a href="musicFolderSettings.view?expunge"><fmt:message key="musicfoldersettings.expunge"/></a></p>
    <p class="detail" style="width:60%;white-space:normal;margin-top:-10px;">
        <fmt:message key="musicfoldersettings.expunge.description"/>
    </p>

    <%--<div>--%>
        <%--<form:checkbox path="organizeByFolderStructure" cssClass="checkbox" id="organizeByFolderStructure"/>--%>
        <%--<form:label path="organizeByFolderStructure"><fmt:message key="musicfoldersettings.organizebyfolderstructure"/></form:label>--%>
    <%--</div>--%>

    <%--<p class="detail" style="width:60%;white-space:normal;">--%>
        <%--<fmt:message key="musicfoldersettings.organizebyfolderstructure.description"/>--%>
    <%--</p>--%>

    <p >
        <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em">
        <a href='nowPlaying.view'><input type="button" value="<fmt:message key='common.cancel'/>"></a>
    </p>

</form:form>

</body></html>
