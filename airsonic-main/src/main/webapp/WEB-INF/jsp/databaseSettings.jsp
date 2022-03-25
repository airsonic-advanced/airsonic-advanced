<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%--@elvariable id="command" type="org.airsonic.player.command.DatabaseSettingsCommand"--%>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script>
        function updateShownOptions() {
            $(".hideawayDatabaseOptions").hide();
            var value = $('select#configType').val();
            var objToShow = $("#" + value + "DatabaseOptions");
            if (objToShow.length) {
                objToShow.show();
            }
            if(value != 'BUILTIN') {
                $("#nonBUILTINDatabaseOptions").show();
            }
        }

        function uploadStatus(uploadInfo) {
            var progressBarHolder = $("#progressBarHolder" + uploadInfo.transferId);

            if (uploadInfo.bytesTotal <= 0 || uploadInfo.bytesUploaded > uploadInfo.bytesTotal) {
                if (progressBarHolder.length != 0) {
                    // remove it
                    progressBarHolder.remove();
                }
            } else {
                if (progressBarHolder.length == 0) {
                    // create it
                    progressBarHolder = $("<div id='progressBarHolder" + uploadInfo.transferId + "'><p class='detail progress-text'/><div class='progress-bar'><div class='progress-bar-content'></div></div></div>");
                    $("#progressBars").append(progressBarHolder);
                }

                var progressBarContent = progressBarHolder.find(".progress-bar-content");
                var progressText = progressBarHolder.find(".progress-text");

                var percent = Math.ceil((uploadInfo.bytesUploaded / uploadInfo.bytesTotal) * 100);
                progressBarContent.width(parseInt(percent * 3.5));
                progressText.text(percent + "<fmt:message key='more.upload.progress'/>");
            }
        }

        function backupDB() {
            $("#backupdb").prop("disabled", true);
            $.get("databaseSettings/backup");
        }

        function exportDB() {
            $("#exportdb").prop("disabled", true);
            location.href="databaseSettings/export";
        }

        function importDB() {
            $("#importdb").prop("disabled", true);
        }

        function backupStatus(msg) {
            if (msg == "ended") {
               $("#backupdb").prop("disabled", false);
            }
            $().toastmessage('showSuccessToast', 'Backup DB: ' + msg);
        }

        function importStatus(msg) {
            if (msg == "ended") {
               $("#importdb").prop("disabled", false);
            }
            $().toastmessage('showSuccessToast', 'Import DB: ' + msg);
        }

        function exportStatus(msg) {
            if (msg == "ended") {
               $("#exportdb").prop("disabled", false);
            }
            $().toastmessage('showSuccessToast', 'Export DB: ' + msg);
        }

        $(document).ready(function () {
            updateShownOptions();
            $('select#configType').on('change', function () {
                updateShownOptions();
            });
            top.StompClient.subscribe("databaseSettingsController.jsp", {
                '/user/queue/uploads/status': function(msg) { uploadStatus(JSON.parse(msg.body)); },
                '/topic/backupStatus': function(msg) { backupStatus(msg.body); },
                '/topic/importStatus': function(msg) { importStatus(msg.body); },
                '/topic/exportStatus': function(msg) { exportStatus(msg.body); }
            });
        });
    </script>
    <style>
    </style>
</head>
<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value='/script/wz_tooltip.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/tip_balloon.js'/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="database"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<form:form modelAttribute="command" action="databaseSettings.view" method="post">
    <p><fmt:message key="databasesettings.moreinfo"/></p>

    <table style="white-space:nowrap" class="indent">
        <tr>
            <td><fmt:message key="databasesettings.configtype"/></td>
            <td>
                <form:select path="configType" cssStyle="width:12em" id="configType">
                    <form:option value="BUILTIN" label="Built-in"/>
                    <form:option value="EXTERNAL" label="External"/>
                    <form:option value="JNDI" label="JNDI"/>
                </form:select>
                <c:import url="helpToolTip.jsp"><c:param name="topic" value="databaseConfigType"/></c:import>
            </td>
        </tr>
    </table>

    <div id="EXTERNALDatabaseOptions" class="hideawayDatabaseOptions">
        <table style="white-space:nowrap;" class="indent">
            <table style="white-space:nowrap;" class="indent">
                <tr>
                    <td><fmt:message key="databasesettings.driver"/></td>
                    <td>
                        <form:input path="driver" size="30"/>
                        <c:import url="helpToolTip.jsp"><c:param name="topic" value="jdbcdriver"/></c:import>
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="databasesettings.url"/></td>
                    <td>
                        <form:input path="url" size="58"/>
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="databasesettings.username"/></td>
                    <td>
                        <form:input path="username" size="36"/>
                    </td>
                </tr>
                <tr>
                    <td><fmt:message key="databasesettings.password"/></td>
                    <td>
                        <form:input path="password" size="36"/>
                    </td>
                </tr>
            </table>
        </table>
    </div>

    <div id="JNDIDatabaseOptions" class="hideawayDatabaseOptions">
        <table style="white-space:nowrap;" class="indent">
            <tr>
                <td><fmt:message key="databasesettings.jndiname"/></td>
                <td>
                    <form:input path="JNDIName" size="36"/>
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="jndiname"/></c:import>
                </td>
            </tr>
        </table>
    </div>
    <div id="nonBUILTINDatabaseOptions" class="hideawayDatabaseOptions">
        <table style="white-space:nowrap" class="indent">
            <tr>
                <td><fmt:message key="databasesettings.mysqlvarcharmaxlength"/></td>
                <td>
                    <form:input path="mysqlVarcharMaxlength" size="8"/>
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="mysqlvarcharmaxlength"/></c:import>
                </td>
            </tr>
        </table>
        <p class="warning"><fmt:message key="databasesettings.jdbclibrary"/></p>
    </div>

    <p class="warning"><fmt:message key="databasettings.restartRequired"/></p>

  <c:choose>
   <c:when test="${command.backuppable}">
    <p><fmt:message key="databasesettings.autodbbackup"/></p>
    <table>
      <tr>
        <td><fmt:message key="databasesettings.dbbackupschedule"/></td>

        <td>
          <form:select path="dbBackupInterval" cssStyle="width:20em">
            <fmt:message key="podcastsettings.interval.manually" var="never"/>
            <fmt:message key="podcastsettings.interval.hourly" var="hourly"/>
            <fmt:message key="podcastsettings.interval.daily" var="daily"/>
            <fmt:message key="podcastsettings.interval.weekly" var="weekly"/>
            <fmt:message key="podcastsettings.interval.monthly" var="monthly"/>

            <form:option value="-1" label="${never}"/>
            <form:option value="1" label="${hourly}"/>
            <form:option value="24" label="${daily}"/>
            <form:option value="168" label="${weekly}"/>
            <form:option value="720" label="${monthly}"/>
          </form:select>
        </td>
      </tr>
      <tr>
        <td><fmt:message key="podcastsettings.keep"/></td>

        <td>
          <form:select path="dbBackupRetentionCount" cssStyle="width:20em">
            <fmt:message key="databasesettings.keep.all" var="all"/>
            <fmt:message key="databasesettings.keep.one" var="one"/>

            <form:option value="-1" label="${all}"/>
            <form:option value="1" label="${one}"/>

            <c:forTokens items="2 3 4 5 10 20 30 50 100 500 1000" delims=" " var="count">
                <fmt:message key="databasesettings.keep.many" var="many"><fmt:param value="${count}"/></fmt:message>
                <form:option value="${count}" label="${many}"/>
            </c:forTokens>

          </form:select>
        </td>
      </tr>
    </table>
   </c:when>
   <c:otherwise>
    <form:hidden path="dbBackupInterval"/>
    <form:hidden path="dbBackupRetentionCount"/>
   </c:otherwise>
  </c:choose>

    <p>
        <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em">
        <a href="nowPlaying.view"><input type="button" value="<fmt:message key='common.cancel'/>"></a>
    </p>

</form:form>

<div style="display:table;">
  <div style="display:table-row;">
  <c:if test="${command.backuppable}">
    <div style="display:table-cell; padding:0.5em;">
      <button id="backupdb" onclick="backupDB()">
        <fmt:message key="databasesettings.backup"/>
        <c:import url="helpToolTip.jsp"><c:param name="topic" value="backupdb"/></c:import>
      </button>
    </div>
  </c:if>

    <div style="display:table-cell; padding:0.5em;">
      <button id="exportdb" onclick="exportDB()">
        <img src="<spring:theme code='downloadImage'/>" alt=""/>
        <fmt:message key="databasesettings.export"/>
        <c:import url="helpToolTip.jsp"><c:param name="topic" value="exportdb"/></c:import>
      </button>
    </div>

    <form method="post" enctype="multipart/form-data" action="upload.view?${_csrf.parameterName}=${_csrf.token}" onsubmit="importDB()">
        <input type="hidden" id="dir" name="dir" value="${command.importFolder}"/>
        <input type="hidden" name="callback" value="${command.callback}"/>
        <input type="hidden" name="unzip" value="true"/>
        <div style="display:table-cell; padding:0.5em; padding-right:0;"><input type="file" id="file" name="file" size="40" multiple="multiple"/></div>
        <div style="display:table-cell; padding:0.5em; padding-left:0;">
          <button id="importdb" type="submit">
            <img src="<spring:theme code='uploadImage'/>" alt=""/>
            <span style="vertical-align: middle"><fmt:message key="databasesettings.import"/></span>
            <c:import url="helpToolTip.jsp"><c:param name="topic" value="importdb"/></c:import>
          </button>
        </div>
    </form>
  </div>
  <div id="progressBars"></div>
</div>


</body>
</html>
