<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
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

        $(document).ready(function () {
            updateShownOptions();
            $('select#configType').on('change', function () {
                updateShownOptions();
            });
        });
    </script>
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
            <tr>
                <td><fmt:message key="databasesettings.usertablequote"/></td>
                <td>
                    <form:input path="usertableQuote" size="1" htmlEscape="true"/>
                    <c:import url="helpToolTip.jsp"><c:param name="topic" value="usertablequote"/></c:import>
                </td>
            </tr>
        </table>
        <p class="warning"><fmt:message key="databasesettings.jdbclibrary"/></p>
    </div>

    <p class="warning"><fmt:message key="databasettings.restartRequired"/></p>

    <p>
        <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em">
        <a href="nowPlaying.view"><input type="button" value="<fmt:message key='common.cancel'/>"></a>
    </p>

</form:form>

</body>
</html>
