<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" isErrorPage="true" %>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>

    <script type="text/javascript" language="javascript">
      function createNewCredsFnc(event) {
        $("#createNewCreds").dialog({resizable: true, width: 600, title: "<fmt:message key='credentialsettings.addcredentials'/>", position: { my: "top", at: "top", of: window },
            buttons: {
                "<fmt:message key='common.cancel'/>": function() {
                    $(this).dialog("close");
                },
                "<fmt:message key='common.create'/>": function() {
                    $(document.getElementById("newCreds")).submit();
                }
            }});

        event.preventDefault();  
      }

      function selectButtonCheckboxAndSubmit(clickedButton, event) {
        clickedButton.childNodes[1].checked=true;
      }

      var appsSettings = ${sub:toJson(appsMap)};
      var decodableEncoders = ${decodableEncodersJson};
      var defaultEncoderDecodableOnly = "${preferredEncoderDecodableOnly}";
      var nonDecodableEncoders = ${nonDecodableEncodersJson};
      var defaultEncoderNonDecodableAllowed = "${preferredEncoderNonDecodableAllowed}";
      var encoderAliases = ${encoderAliasesJson};

      function bindNewCredsForm() {
          var app = $('#app').val();

          // enable or disable user name field
          if (appsSettings[app].usernameRequired) {
            $('#username').val('').attr('value', '');
            $('#username').prop('disabled', false);
          } else {
            $('#username').prop('disabled', true);
            $('#username').val('Not required').attr('value', 'Not required');
          }

          var el = $('#encoder');
          el.empty();

          var defaultOptionIncluded = false;
          // add decodable options
          el.append($("<option value='notselectable' disabled='disabled'>Decodable</option>"));
          $.each(decodableEncoders, function(k, v) {
            if (v == defaultEncoderNonDecodableAllowed) {
              defaultOptionIncluded = true;
            }
            el.append($("<option></option>").attr("value", v).text(encoderAliases[v] != null ? encoderAliases[v] : v));
          });

          // enable or disable nondecodable options
          if (appsSettings[app].nonDecodableEncodersAllowed) {
            el.append($("<option value='notselectable' disabled='disabled'>Non-Decodable</option>"));
            $.each(nonDecodableEncoders, function(k, v) {
              if (v == defaultEncoderNonDecodableAllowed) {
                defaultOptionIncluded = true;
              }
              el.append($("<option></option>").attr("value", v).text(encoderAliases[v] != null ? encoderAliases[v] : v));
            });
          }

          el.val(defaultOptionIncluded ? defaultEncoderNonDecodableAllowed : defaultEncoderDecodableOnly);
      }

      function bindComponentHandling() {
        bindNewCredsForm();

        // need to submit all fields, so remove the disabled restriction
        $('#newCreds').submit(function(e) {
          $('#username').removeAttr('disabled');
        });

        // createNewCreds handling
        $('#app').change(function(ev) {
          bindNewCredsForm();
        });

        // grey out last delete button for airsonic creds
        if ($('.airsonic-cred').length == 1) {
          $('.airsonic-cred .delete-cred').prop('disabled', true);
        }
        
        // blur out sensitive data
        $('.sensitive').hover(function() {
          $(this).removeClass('blur');
        }).mouseout(function() {
          $(this).addClass('blur');
        });

        <c:if test="${open_CreateCredsDialog}">
        // open create dialog automatically if ordained by server
        $('#createcredsbutton').click();
        </c:if>
      }

      $(document).ready(bindComponentHandling);
    </script>
</head>

<body class="mainframe bgcolor1">
<script type="text/javascript" src="<c:url value='/script/wz_tooltip.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/tip_balloon.js'/>"></script>

<c:import url="settingsHeader.jsp">
    <c:param name="cat" value="credentials"/>
    <c:param name="restricted" value="${not adminRole}"/>
    <c:param name="toast" value="${settings_toast}"/>
</c:import>

<h2>Credentials Management</h2>
<div>
<h3>Credentials</h3>
<form:form method="put" action="credentialsSettings.view" modelAttribute="command">
<table id="credentialsTable">
  <tr>
    <th style="padding:0 0.5em 0 0.5em;border-style:double">ID</th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double"><fmt:message key='credentialsettings.app'/></th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double"><fmt:message key='credentialsettings.user'/></th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double"><fmt:message key='credentialsettings.comments'/></th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double"><fmt:message key='credentialsettings.created'/></th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double"><fmt:message key='credentialsettings.updated'/></th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double"><fmt:message key='credentialsettings.encoder'/></th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double"><fmt:message key='credentialsettings.expires'/><c:import url="helpToolTip.jsp"><c:param name="topic" value="credentialsdates"/></c:import></th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double"><fmt:message key='common.delete'/></th>
  </tr>
  <tr>
    <td style="text-align:center;border-style:dotted" colspan=9>Airsonic Credentials <c:import url="helpToolTip.jsp"><c:param name="topic" value="credentialsairsonic"/></c:import></td>
  </tr>
  <c:forEach items="${command.credentials}" var="cred" varStatus="loopStatus">
    <c:if test="${cred.app == 'AIRSONIC'}">
    <tr class="airsonic-cred">
      <td style="padding:0 0.5em 0 0.5em">${loopStatus.index}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.app.name}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.username}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.comment}</td>
      <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${cred.created}" style="SS" /></td>
      <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${cred.updated}" style="SS" /></td>
      <td style="padding:0 0.5em 0 0.5em">
        <form:select path="credentials[${loopStatus.index}].encoder" cssStyle="width:9em">
          <c:if test="${ cred.displayComments.contains( 'decodablecred' ) }">
            <c:if test="${!decodableEncoders.contains(cred.encoder) && !nonDecodableEncoders.contains(cred.encoder)}">
              <form:option selected="selected" value="${cred.encoder}" label="${encoderAliases[cred.encoder] != null ? encoderAliases[cred.encoder] : cred.encoder}"/>
            </c:if>
            <form:option value="notselectable" label="Decodable" disabled="true"/>
            <c:forEach items="${decodableEncoders}" var="migratableType">
              <c:set var="displayLabelValue" value="${encoderAliases[migratableType] != null ? encoderAliases[migratableType] : migratableType}"/>
              <c:if test="${migratableType != cred.encoder}" >
                <form:option value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
              <c:if test="${migratableType == cred.encoder}" >
                <form:option selected="selected" value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
            </c:forEach>
            <form:option value="notselectable" label="Non-decodable" disabled="true"/>
            <c:forEach items="${nonDecodableEncoders}" var="migratableType">
              <c:set var="displayLabelValue" value="${encoderAliases[migratableType] != null ? encoderAliases[migratableType] : migratableType}"/>
              <c:if test="${migratableType != cred.encoder}" >
                <form:option value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
              <c:if test="${migratableType == cred.encoder}" >
                <form:option selected="selected" value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
            </c:forEach>
          </c:if>
          <c:if test="${ !cred.displayComments.contains( 'decodablecred' ) }">
            <form:option selected="selected" value="${cred.encoder}" label="${encoderAliases[cred.encoder] != null ? encoderAliases[cred.encoder] : cred.encoder}"/>
          </c:if>
        </form:select>
      </td>
      <td><form:input type="datetime-local" path="credentials[${loopStatus.index}].expiration" /></td>
      <td style="text-align:center;">
        <form:checkbox path="credentials[${loopStatus.index}].markedForDeletion" class="delete-cred"/>
      </td>
      <td class="warning">
        <form:hidden path="credentials[${loopStatus.index}].hash" />
        <form:errors class="warning" path="credentials[${loopStatus.index}].hash" cssStyle="width:15em"/>
      </td>
    </tr>
    </c:if>
  </c:forEach>
  <c:if test="${ldapAuthEnabledForUser}">
    <tr>
      <td class="warning" style="text-align:center" colspan=9><i><fmt:message key="credentialsettings.ldapauthenabledforuser"/></i></td>
    </tr>
  </c:if>
  <tr><td> </td></tr>

  <tr>
    <td style="text-align:center;border-style:dotted" colspan=9>Third-party Credentials <c:import url="helpToolTip.jsp"><c:param name="topic" value="credentialsthirdparty"/></c:import></td>
  </tr>
  <c:forEach items="${command.credentials}" var="cred" varStatus="loopStatus">
    <c:if test="${cred.app != 'AIRSONIC'}" >
    <tr>
      <td style="padding:0 0.5em 0 0.5em">${loopStatus.index}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.app.name}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.username}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.comment}</td>
      <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${cred.created}" style="SS" /></td>
      <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${cred.updated}" style="SS" /></td>
      <td style="padding:0 0.5em 0 0.5em">
        <form:select path="credentials[${loopStatus.index}].encoder" cssStyle="width:9em">
          <c:if test="${!decodableEncoders.contains(cred.encoder)}">
            <form:option selected="selected" value="${cred.encoder}" label="${encoderAliases[cred.encoder] != null ? encoderAliases[cred.encoder] : cred.encoder}"/>
          </c:if>
          <form:option value="notselectable" label="Decodable" disabled="true"/>
          <c:forEach items="${decodableEncoders}" var="migratableType">
              <c:set var="displayLabelValue" value="${encoderAliases[migratableType] != null ? encoderAliases[migratableType] : migratableType}"/>
              <c:if test="${migratableType != cred.encoder}" >
                <form:option value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
              <c:if test="${migratableType == cred.encoder}" >
                <form:option selected="selected" value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
          </c:forEach>
        </form:select>
      </td>
      <td><form:input type="datetime-local" path="credentials[${loopStatus.index}].expiration" /></td>
      <td style="text-align:center;">
        <form:checkbox path="credentials[${loopStatus.index}].markedForDeletion" class="delete-cred" />
      </td>
      <td class="warning">
        <form:hidden path="credentials[${loopStatus.index}].hash" />
        <form:errors class="warning" path="credentials[${loopStatus.index}].hash" cssStyle="width:15em"/>
      </td>
    </tr>
    </c:if>
  </c:forEach>
</table>
<p style="padding-top:1em"><fmt:message key="credentialsettings.immutable"/></p>
<p style="padding-top:1em;padding-bottom:1em">
    <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em"/>
    <input type="button" id="createcredsbutton" value="<fmt:message key='credentialsettings.addcredentials'/>" onclick="createNewCredsFnc(event)"/>
    <input type="reset" value="<fmt:message key='common.cancel'/>" />
</p>
</form:form>
</div>

<div id="createNewCreds" style="display:none">
  <form:form method="post" action="credentialsSettings.view" modelAttribute="newCreds">
    <table style="white-space:nowrap" class="indent">
      <tr>
        <td><fmt:message key="credentialsettings.app"/></td>
        <td>
          <form:select path="app" cssStyle="width:15em">
            <form:options items="${apps}" itemLabel="name" />
          </form:select>
        </td>
        <td class="warning"><form:errors path="app" cssStyle="width:15em"/></td>
      </tr>

      <tr>
        <td><fmt:message key="login.username"/></td>
        <td><form:input path="username" size="20"/></td>
        <td class="warning"><form:errors path="username" cssStyle="width:15em"/></td>
      </tr>

      <tr>
        <td><fmt:message key="credentialsettings.encoder"/></td>
        <td>
          <form:select path="encoder" cssStyle="width:15em"></form:select>
          <td class="warning"><form:errors path="encoder" cssStyle="width:15em"/></td>
        </td>
      </tr>

      <tr>
        <td><fmt:message key="usersettings.newpassword"/></td>
        <td><form:password path="credential" size="20"/></td>
        <td class="warning"><form:errors path="credential" cssStyle="width:15em"/></td>
      </tr>

      <tr>
        <td><fmt:message key="usersettings.confirmpassword"/></td>
        <td><form:password path="confirmCredential" size="20"/></td>
       <td class="warning"><form:errors path="confirmCredential" cssStyle="width:15em"/></td>
      </tr>

      <tr>
        <td><fmt:message key="credentialsettings.expires"/></td>
        <td>
          <form:input type="datetime-local" path="expiration" />
          <c:import url="helpToolTip.jsp"><c:param name="topic" value="credentialsdates"/></c:import>
        </td>
      </tr>
    </table>
    <form:errors class="warning" cssStyle="width:15em"/>
  </form:form>
</div>

<c:if test="${adminRole}">
<div>
  <h3><fmt:message key='credentialsettings.admincontrols'/></h3>
  <form:form method="post" action="${pageContext.request.contextPath}/credentialsSettings/admin" modelAttribute="adminControls">

    <table style="white-space:nowrap" class="indent">
      <tr><th colspan=4 style="text-align:left"><fmt:message key='credentialsettings.systemchecks'/></th></tr>
      <c:if test="${adminControls.legacyCredsPresent}">
      <tr>
        <td><fmt:message key="credentialsettings.legacycredspresent"/><c:import url="helpToolTip.jsp"><c:param name="topic" value="credentialslegacypasswords"/></c:import></td>
        <td>
          <button onclick="selectButtonCheckboxAndSubmit(this, event)">
            <form:checkbox path="migrateLegacyCredsToNonLegacyDefault" cssStyle="display:none" />
            <fmt:message key='credentialsettings.adminmigratelegacytononlegacydefault'/>
          </button>
        </td>
        <td>
          <button onclick="selectButtonCheckboxAndSubmit(this, event)">
            <form:checkbox path="migrateLegacyCredsToNonLegacyDecodableOnly" cssStyle="display:none" />
            <fmt:message key='credentialsettings.adminmigratelegacytononlegacydecodableonly'/>
          </button>
        </td>
      </tr>
      </c:if>
      <c:if test="${adminControls.openCredsPresent}">
      <tr>
        <td colspan=4 class="warning"><fmt:message key="credentialsettings.opencredspresent"/></td>
      </tr>
      </c:if>
      <c:if test="${adminControls.defaultAdminCredsPresent}">
      <tr>
        <td colspan=4 class="warning"><fmt:message key="credentialsettings.defaultadmincredspresent"/></td>
      </tr>
      </c:if>
    </table>

    <table style="white-space:nowrap" class="indent">
      <tr><th style="text-align:left"><fmt:message key='credentialsettings.encoders'/></th></tr>
      <tr>
        <td><fmt:message key="credentialsettings.nondecodableencoder"/></td>
        <td>
          <form:select path="nonDecodableEncoder" style="width:12em">
            <c:forEach items="${nonDecodableEncoders}" var="migratableType">
              <c:set var="displayLabelValue" value="${encoderAliases[migratableType] != null ? encoderAliases[migratableType] : migratableType}"/>
              <c:if test="${migratableType != nonDecodableEncoder}" >
                <form:option value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
              <c:if test="${migratableType == nonDecodableEncoder}" >
                <form:option selected="selected" value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
            </c:forEach>
          </form:select>
        </td>
        <td>
          <button onclick="selectButtonCheckboxAndSubmit(this, event)">
            <form:checkbox path="nonDecodableEncoderChanged" cssStyle="display:none" />
            <fmt:message key='common.save'/>
          </button>
        </td>
        <td class="warning">
          <form:errors path="nonDecodableEncoder"/>
        </td>
      </tr>
      <tr>
        <td><fmt:message key="credentialsettings.decodableencoder"/></td>
        <td>
          <form:select path="decodableEncoder" style="width:12em">
            <c:forEach items="${decodableEncoders}" var="migratableType">
              <c:set var="displayLabelValue" value="${encoderAliases[migratableType] != null ? encoderAliases[migratableType] : migratableType}"/>
              <c:if test="${migratableType != decodableEncoder}" >
                <form:option value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
              <c:if test="${migratableType == decodableEncoder}" >
                <form:option selected="selected" value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
            </c:forEach>
          </form:select>
        </td>
        <td>
          <button onclick="selectButtonCheckboxAndSubmit(this, event)">
            <form:checkbox path="decodableEncoderChanged" cssStyle="display:none" />
            <fmt:message key='common.save'/>
          </button>
        </td>
        <td class="warning">
          <form:errors path="decodableEncoder"/>
        </td>
      </tr>
      <tr>
        <td><label for="preferNonDecodableCheckbox"><fmt:message key="credentialsettings.prefernondecodablepasswords"/>  </label></td>
        <td style="text-align:center;">
          <form:checkbox id="preferNonDecodableCheckbox" path="preferNonDecodable" />
        </td>
        <td>
          <button onclick="selectButtonCheckboxAndSubmit(this, event)">
            <form:checkbox path="nonDecodablePreferenceChanged" cssStyle="display:none" />
            <fmt:message key='common.save'/>
          </button>
        </td>
      </tr>
      
      <tr><td>&nbsp;</td></tr>

      <tr><th style="text-align:left">Keys</th></tr>
      <tr><td colspan=3><fmt:message key='credentialsettings.keepkeyssafe'/></td></tr>
      <tr>
        <td>JWT Key</td>
        <td>
          <form:input path="jwtKey" style="width:15em" class="sensitive blur"/>
          <c:import url="helpToolTip.jsp"><c:param name="topic" value="credentialsjwtkey"/></c:import>
        </td>
        <td>
          <button onclick="selectButtonCheckboxAndSubmit(this, event)">
            <form:checkbox path="jwtKeyChanged" cssStyle="display:none" />
            <fmt:message key='common.save'/>
          </button>
        </td>
      </tr>
      <tr>
        <td>Encryption Key Password</td>
        <td>
          <form:input path="encryptionKey" style="width:15em" class="sensitive blur"/>
          <c:import url="helpToolTip.jsp"><c:param name="topic" value="credentialsencryptionkey"/></c:import>
        </td>
        <td>
          <button onclick="selectButtonCheckboxAndSubmit(this, event)">
            <form:checkbox path="encryptionKeyChanged" cssStyle="display:none" />
            <fmt:message key='common.save'/>
          </button>
        </td>
      </tr>
      <tr>
        <td>Encryption Key Salt</td>
        <td colspan=2 style="font-size:90%;" class="sensitive blur">
          ${adminControls.encryptionKeySalt}
        </td>
      </tr>
    </table>

    <form:errors class="warning" cssStyle="width:15em"/>
  </form:form>
</div>
</c:if>

</body></html>
