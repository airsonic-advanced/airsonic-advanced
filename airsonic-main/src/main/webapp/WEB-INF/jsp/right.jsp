<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>

    <script type="text/javascript">
        function scanningStatus(msg) {
            var scanInfo = JSON.parse(msg.body);
            $("#scanCount").text(scanInfo.count);
            if (scanInfo.scanning) {
                $("#scanningStatus").show();
            } else {
                $("#scanningStatus").hide();
            }
        }

        function init() {
            top.StompClient.subscribe("right.jsp", {
                // no need to populate initial because the updates will occur frequently enough and are self-sufficient
                '/topic/scanStatus': scanningStatus
                <c:if test="${model.showNowPlaying}">
                ,
                '/topic/nowPlaying/current/add': function(msg) {
	                addStatus(JSON.parse(msg.body), 'nowPlaying');
	            },
	            '/topic/nowPlaying/current/remove': function(msg) {
	                removeStatus(JSON.parse(msg.body), 'nowPlaying');
	            },
	            '/topic/nowPlaying/recent/add': function(msg) {
	                addStatus(JSON.parse(msg.body), 'recentlyPlayed');
	            },
	            '/topic/nowPlaying/recent/remove': function(msg) {
	                removeStatus(JSON.parse(msg.body), 'recentlyPlayed');
	            },
	            // Add existing (initial population, one time)
	            '/app/nowPlaying/current': function(msg) {
	                $('#nowPlayingTable').empty();
	                removeStatus({}, 'nowPlaying');
	                var statuses = JSON.parse(msg.body);
	                for (var i = 0; i < statuses.length; i++) {
	                    addStatus(statuses[i], 'nowPlaying');
	                }
	            },
	            '/app/nowPlaying/recent': function(msg) {
	                $('#recentlyPlayedTable').empty();
	                removeStatus({}, 'recentlyPlayed');
	                var statuses = JSON.parse(msg.body);
	                for (var i = 0; i < statuses.length; i++) {
	                    addStatus(statuses[i], 'recentlyPlayed');
	                }
	            }
                </c:if>
            });
        }

        function addStatus(status, table) {
            var html = "";
            html += "<tr><td colspan='2' class='detail' style='padding-top:1em;white-space:nowrap'>";

            if (status.avatarUrl) {
                html += "<img alt='Avatar' src='" + status.avatarUrl + "' style='padding-right:5pt;width:30px;height:30px'>";
            }
            html += "<b>" + status.username + "</b></td></tr>";

            html += "<tr><td class='detail' style='padding-right:1em'>" +
                    "<a title='" + status.tooltip + "' target='main' href='" + status.albumUrl + "'>";

            if (status.artist != null) {
                html += status.artist + "<br/>";
            }

            html += "<span class='songTitle'>" + status.title + "</span></a><br/>";
            if (status.lyricsUrl != null) {
                html += "<span class='forward'><a href='" + status.lyricsUrl + "' onclick=\"return popupSize(this, 'lyrics', 500, 550)\">" +
                        "<fmt:message key="main.lyrics"/>" + "</a></span>";
            }
            html += "</td><td>" +
                    "<a title='" + status.tooltip + "' target='main' href='" + status.albumUrl + "'>" +
                    "<img alt='Cover art' src='" + status.coverArtUrl + "' class='dropshadow' height='60' width='60'></a>" +
                    "</td></tr>";

            var minutesAgo = status.minutesAgo;
            if (minutesAgo > 4) {
                html += "<tr><td class='detail' colspan='2'>" + minutesAgo + " <fmt:message key="main.minutesago"/></td></tr>";
            }

            $('.playstatus-'+status.transferId).remove();
            $('#' + table + 'Table').append($(html).addClass('playstatus-'+status.transferId).addClass('mediafile-'+status.mediaFileId));
            $('#' + table).show();
        }

        function removeStatus(status, table) {
            $('.playstatus-'+status.transferId+'.mediafile-'+status.mediaFileId).remove();
            if ($('#' + table + 'Table').children().length == 0) {
                $('#' + table).hide();
            }
        }
    </script>
</head>
<body class="bgcolor1 rightframe" style="padding-top:2em" onload="init()">

<c:if test="${model.newVersionAvailable}">
    <div class="warning" style="padding-bottom: 1em">
        <fmt:message key="top.upgrade"><fmt:param value="${model.brand}"/><fmt:param value="${model.latestVersion}"/><fmt:param value="${model.latestVersion.url}"/></fmt:message>
    </div>
</c:if>

<div id="scanningStatus" style="display: none;" class="warning">
    <img src="<spring:theme code='scanningImage'/>" title="" alt=""> <fmt:message key="main.scanning"/> <span id="scanCount"></span>
</div>

<div id="nowPlaying" style='display:none'>
    <h2><fmt:message key="main.nowplaying"/></h2>
    <table id="nowPlayingTable" style='width:100%'></table>
</div>

<div id="recentlyPlayed" style='display:none'>
    <h2><fmt:message key="main.recentlyplayed"/></h2>
    <table id="recentlyPlayedTable" style='width:100%'></table>
</div>

</body>
</html>
