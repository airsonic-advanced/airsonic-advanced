<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>

    <script type="text/javascript" language="javascript">
      function createNewCredsFnc(event) {
        $("#createNewCreds").dialog({resizable: true, width: 600, title: "<fmt:message key='credentials.create'/>", position: { my: "top", at: "top", of: window },
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

      function deleteCreds(clickedButton, event) {
        clickedButton.childNodes[0].checked=true;
      }

      var appsSettings = ${appsCredsSettingsJson};
      var decodableEncoders = ${decodableEncodersJson};
      var defaultDecodableEncoder = "${defaultDecodableEncoder}";
      var nonDecodableEncoders = ${nonDecodableEncodersJson};
      var defaultEncoder = "${defaultEncoder}";
      var encoderAliases = ${encoderAliasesJson};

      function bindNewCredsForm() {
          var location = $('#location').val();

          // enable or disable user name field
          if (appsSettings[location].usernameRequired) {
            $('#username').val('').attr('value', '');
            $('#username').prop('disabled', false);
          } else {
            $('#username').prop('disabled', true);
            $('#username').val('Not required').attr('value', 'Not required');
          }

          var el = $('#type');
          el.empty();

          var defaultOptionIncluded = false;
          // add decodable options
          el.append($("<option value='notselectable' disabled='disabled'>Decodable</option>"));
          $.each(decodableEncoders, function(k, v) {
            if (v == defaultEncoder) {
              defaultOptionIncluded = true;
            }
            el.append($("<option></option>").attr("value", v).text(encoderAliases[v] != null ? encoderAliases[v] : v));
          });

          // enable or disable nondecodable options
          if (appsSettings[location].nonDecodableEncodersAllowed) {
            el.append($("<option value='notselectable' disabled='disabled'>Non-Decodable</option>"));
            $.each(nonDecodableEncoders, function(k, v) {
              if (v == defaultEncoder) {
                defaultOptionIncluded = true;
              }
              el.append($("<option></option>").attr("value", v).text(encoderAliases[v] != null ? encoderAliases[v] : v));
            });
          }

          el.val(defaultOptionIncluded ? defaultEncoder : defaultDecodableEncoder);
      }

      function bindComponentHandling() {
        bindNewCredsForm();

        $('#newCreds').submit(function(e) {
          $('#username').removeAttr('disabled');
        });

        // createNewCreds handling
        $('#location').change(function(ev) {
          bindNewCredsForm();
        });
        
        <c:if test="${open_CreateCredsDialog}">
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
<h3>Credentials</h3>
<form:form method="put" action="credentialsSettings.view" modelAttribute="command">
<table id="credentialsTable">
  <tr>
    <th style="padding:0 0.5em 0 0.5em;border-style:double">ID</th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double">App</th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double">User</th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double">Comments</th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double">Created</th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double">Updated</th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double">Encoder</th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double">Expires</th>
    <th style="padding:0 0.5em 0 0.5em;border-style:double"><fmt:message key='common.delete'/></th>
  </tr>
  <tr>
    <td style="text-align:center;border-style:dotted" colspan=9>Airsonic Credentials</td>
  </tr>
  <c:forEach items="${command.credentials}" var="cred" varStatus="loopStatus">
    <c:if test="${cred.location == 'airsonic'}" >
    <tr>
      <td style="padding:0 0.5em 0 0.5em">${loopStatus.index}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.location}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.username}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.comment}</td>
      <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${cred.created}" style="SS" /></td>
      <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${cred.updated}" style="SS" /></td>
      <td style="padding:0 0.5em 0 0.5em">
        <form:select path="credentials[${loopStatus.index}].type" cssStyle="width:9em">
          <c:if test="${ cred.displayComments.contains( 'decodablecred' ) }">
            <c:if test="${!decodableEncoders.contains(cred.type) && !nonDecodableEncoders.contains(cred.type)}">
              <form:option selected="selected" value="${cred.type}" label="${encoderAliases[cred.type] != null ? encoderAliases[cred.type] : cred.type}"/>
            </c:if>
            <form:option value="notselectable" label="Decodable" disabled="true"/>
            <c:forEach items="${decodableEncoders}" var="migratableType">
              <c:set var="displayLabelValue" value="${encoderAliases[migratableType] != null ? encoderAliases[migratableType] : migratableType}"/>
              <c:if test="${migratableType != cred.type}" >
                <form:option value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
              <c:if test="${migratableType == cred.type}" >
                <form:option selected="selected" value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
            </c:forEach>
            <form:option value="notselectable" label="Non-decodable" disabled="true"/>
            <c:forEach items="${nonDecodableEncoders}" var="migratableType">
              <c:set var="displayLabelValue" value="${encoderAliases[migratableType] != null ? encoderAliases[migratableType] : migratableType}"/>
              <c:if test="${migratableType != cred.type}" >
                <form:option value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
              <c:if test="${migratableType == cred.type}" >
                <form:option selected="selected" value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
            </c:forEach>
          </c:if>
          <c:if test="${ !cred.displayComments.contains( 'decodablecred' ) }">
            <form:option selected="selected" value="${cred.type}" label="${encoderAliases[cred.type] != null ? encoderAliases[cred.type] : cred.type}"/>
          </c:if>
        </form:select>
      </td>
      <td><form:input type="datetime-local" path="credentials[${loopStatus.index}].expiration" /></td>
      <td style="text-align:center;"><button onclick="deleteCreds(this, event)"><form:checkbox path="credentials[${loopStatus.index}].markedForDeletion" cssStyle="display:none" /><fmt:message key='common.delete'/></button></td>
      <form:hidden path="credentials[${loopStatus.index}].hash" />
    </tr>
    </c:if>
  </c:forEach>
  <tr>
    <td style="text-align:center;border-style:dotted" colspan=9>Third-party Credentials</td>
  </tr>
  <c:forEach items="${command.credentials}" var="cred" varStatus="loopStatus">
    <c:if test="${cred.location != 'airsonic'}" >
    <tr>
      <td style="padding:0 0.5em 0 0.5em">${loopStatus.index}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.location}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.username}</td>
      <td style="padding:0 0.5em 0 0.5em">${cred.comment}<form:errors path="credentials[${loopStatus.index}].markedForDeletion" cssStyle="width:15em"/></td>
      <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${cred.created}" style="SS" /></td>
      <td style="padding:0 0.5em 0 0.5em"><javatime:format value="${cred.updated}" style="SS" /></td>
      <td style="padding:0 0.5em 0 0.5em">
        <form:select path="credentials[${loopStatus.index}].type" cssStyle="width:9em">
          <c:if test="${!decodableEncoders.contains(cred.type)}">
            <form:option selected="selected" value="${cred.type}" label="${encoderAliases[cred.type] != null ? encoderAliases[cred.type] : cred.type}"/>
          </c:if>
          <form:option value="notselectable" label="Decodable" disabled="true"/>
          <c:forEach items="${decodableEncoders}" var="migratableType">
              <c:set var="displayLabelValue" value="${encoderAliases[migratableType] != null ? encoderAliases[migratableType] : migratableType}"/>
              <c:if test="${migratableType != cred.type}" >
                <form:option value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
              <c:if test="${migratableType == cred.type}" >
                <form:option selected="selected" value="${migratableType}" label="${displayLabelValue}"/>
              </c:if>
          </c:forEach>
        </form:select>
      </td>
      <td><form:input type="datetime-local" path="credentials[${loopStatus.index}].expiration" /></td>
      <td style="text-align:center;"><button onclick="deleteCreds(this, event)"><form:checkbox path="credentials[${loopStatus.index}].markedForDeletion" cssStyle="display:none" /><fmt:message key='common.delete'/></button></td>
      <form:hidden path="credentials[${loopStatus.index}].hash" />
    </tr>
    </c:if>
  </c:forEach>
</table>
<p style="padding-top:1em;padding-bottom:1em">
    <input type="submit" value="<fmt:message key='common.save'/>" style="margin-right:0.3em"/>
    <input type="button" id="createcredsbutton" value="<fmt:message key='common.create'/>" onclick="createNewCredsFnc(event)"/>
    <input type="reset" value="<fmt:message key='common.cancel'/>" />
</p>
</form:form>

<div id="createNewCreds" style="display:none">
  <form:form method="post" action="credentialsSettings.view" modelAttribute="newCreds">
    <table style="white-space:nowrap" class="indent">
      <tr>
        <td><fmt:message key="credentials.app"/></td>
        <td>
          <form:select path="location" cssStyle="width:15em">
            <form:options items="${apps}" />
          </form:select>
        </td>
        <td class="warning"><form:errors path="location" cssStyle="width:15em"/></td>
      </tr>

      <tr>
        <td><fmt:message key="login.username"/></td>
        <td><form:input path="username" size="20"/></td>
        <td class="warning"><form:errors path="username" cssStyle="width:15em"/></td>
      </tr>

      <tr>
        <td><fmt:message key="credentials.encoder"/></td>
        <td>
          <form:select path="type" cssStyle="width:15em"></form:select>
          <c:import url="helpToolTip.jsp"><c:param name="topic" value="language"/></c:import>
          <td class="warning"><form:errors path="type" cssStyle="width:15em"/></td>
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
        <td><fmt:message key="credentials.expiration"/></td>
        <td>
          <form:input type="datetime-local" path="expiration" />
          <c:import url="helpToolTip.jsp"><c:param name="topic" value="language"/></c:import>
        </td>
      </tr>
    </table>
    <form:errors class="warning" cssStyle="width:15em"/>
  </form:form>
</div>

<h3>Notes</h3>
Airsonic supports two primary means of its own authentication for clients. Clients can send either the password openly (<code>p</code>), or can send an md5-hash of the password along with the salt (<code>t+s</code>).<br />
By default, new passwords are stored in their hashed form, which is secure, cannot be decoded to the original password, and works for the <code>p</code> auth mechanism.<br />
For technical reasons, supporting <code>t+s</code> method, however, requires storing the decodable non-hashed password itself, which may be less secure (especially if stored in an open form).<br />
Therefore, store Airsonic passwords in a decodable manner ONLY if your client requires <code>t+s</code> auth to work.<br />
<h4>Note for legacy passwords:</h4>
Legacy passwords were stored in a decodable form.<br />
You should migrate your legacy passwords to a non-decodable hashed form or confirm that you want them to remain in a decodable form (for <code>t+s</code> style auth).<br />
All credentials that have not been explicitly migrated or confirmed WILL BE force-migrated to a nondecodable form in future versions.<br />
<h4>Note for third-party credentials:</h4>
These MUST be stored in a decodable format for Airsonic to send to third-parties.<br />
<h4>Note for encryption:</h4>
Airsonic will use the encryption keys provided, otherwise it will generate keys and use those. Make sure the keys are secured.<br />
<h4>Note for entering dates</h4>
Expiration dates may be entered as yyyy-MM-dd'T'HH:mm (e.g. 2025-01-15T23:03) if browser doesn't show a datetime-picker

<c:if test="${settings_reload}">
    <script language="javascript" type="text/javascript">
        parent.location.href="index.view?";
    </script>
</c:if>

</body></html>
