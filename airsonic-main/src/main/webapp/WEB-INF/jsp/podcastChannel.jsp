<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

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

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>

    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
    <script type="text/javascript" language="javascript">
        var podcastEpisodes = [];
        function init() {
            <c:forEach items="${model.episodes}" var="episode" varStatus="i">
            var episode = {};
            episode.id = '${episode.id}';
            episode.mediaFileId = '${episode.mediaFileId}';
            episode.status = '${episode.status}';
            podcastEpisodes.push(episode);
            </c:forEach>

            top.StompClient.subscribe("podcastChannel.jsp", {
                '/user/queue/mediafile/directory/get': function(msg) {
                    getMediaDirectoryCallback(JSON.parse(msg.body));
                },
                '/user/queue/playlists/writable': function(msg) {
                    playlistSelectionCallback(JSON.parse(msg.body));
                },
                '/user/queue/playlists/files/append': function(msg) {
                    $().toastmessage("showSuccessToast", "<fmt:message key='playlist.toast.appendtoplaylist'/>");
                },
            });

            $("#dialog-select-playlist").dialog({resizable: true, height: 350, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    }
                }
            });

            $("#dialog-delete").dialog({resizable: false, height: 170, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.delete"/>": function() {
                        location.href = "podcastReceiverAdmin.view?channelId=${model.channel.id}" +
                                "&deleteChannel=${model.channel.id}";
                    },
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    }
                }});
        }

        // need to keep track if a request was sent because plaQueue may also send a request
        var awaitingAppendPlaylistRequest = false;
        function onAppendPlaylist() {
            awaitingAppendPlaylistRequest = true;
            // retrieve writable lists so we can open dialog to ask user which playlist to append to
            top.StompClient.send("/app/playlists/writable", "");
        }
        function playlistSelectionCallback(playlists) {
            if (!awaitingAppendPlaylistRequest) {
                return;
            }
            awaitingAppendPlaylistRequest = false;
            $("#dialog-select-playlist-list").empty();
            for (var i = 0; i < playlists.length; i++) {
                var playlist = playlists[i];
                $("<p class='dense'><b><a href='#' onclick='appendPlaylist(" + playlist.id + ")'>" + escapeHtml(playlist.name)
                    + "</a></b></p>").appendTo("#dialog-select-playlist-list");
            }
            $("#dialog-select-playlist").dialog("open");
        }

        function appendPlaylist(playlistId) {
            $("#dialog-select-playlist").dialog("close");
            var mediaFileIds = getSelectedEpisodesMediaId();
            top.StompClient.send("/app/playlists/files/append", JSON.stringify({id: playlistId, modifierIds: mediaFileIds}));
        }

        function getMediaDirectory(mediaFileId) {
            getMediaDirectories([mediaFileId]);
        }

        function getMediaDirectories(ids, paths) {
            top.StompClient.send("/app/mediafile/directory/get", JSON.stringify({ids: ids, paths: paths}));
        }
        function getMediaDirectoryCallback(mediaDirObj) {
            if (downloadMedia) {
                var selectedMediaIds = getSelectedEpisodesMediaId();
                var selectedFilesIndexesToDownload = mediaDirObj.files.reduce(function(accumulator, file, index) {
                    if (selectedMediaIds.indexOf(file.id.toString()) > -1) {
                        accumulator.push(index);
                    }
                    return accumulator;
                }, []);
                location.href = "download.view?id=" + ${model.channel.mediaFileId}  + "&" + selectedFilesIndexesToDownload.map(i => "i=" + i).join("&");
                downloadMedia = false;
            }
        }

        var downloadMedia = false;
        // actionSelected() is invoked when the users selects from the "More actions..." combo box.
        function actionSelected(id) {
            if (id == "top") {
                return;
            } else if (id == "selectAll") {
                selectAll(true);
            } else if (id == "selectNone") {
                selectAll(false);
            } else if (id == "download") {
                if (getSelectedEpisodesMediaId().length > 0) {
                    downloadMedia = true;
                    getMediaDirectory(${model.channel.mediaFileId});
                }
            } else if (id == "appendPlaylist") {
                if (getSelectedEpisodesMediaId().length > 0) {
                    onAppendPlaylist();
                }
            }
            $("#moreActions").prop("selectedIndex", 0);
        }

        function downloadSelected() {
            location.href = "podcastReceiverAdmin.view?channelId=${model.channel.id}&" +
                    getSelectedEpisodes().map(i => "downloadEpisode=" + i).join("&");
        }

        function deleteChannel() {
            $("#dialog-delete").dialog("open");
        }

        function deleteSelected() {
            location.href = "podcastReceiverAdmin.view?channelId=${model.channel.id}&" +
                    getSelectedEpisodes().map(i => "deleteEpisode=" + i).join("&");
        }

        function refreshChannels() {
            location.href = "podcastReceiverAdmin.view?refresh&channelId=${model.channel.id}";
        }

        function refreshPage() {
            location.href = "podcastChannel.view?id=${model.channel.id}";
        }

        function selectAll(checked) {
            for (var i = 0; i < ${fn:length(model.episodes)}; i++) {
                $("#episode" + i).prop("checked", checked);
            }
        }

        function getSelectedEpisodes() {
            var result = [];
            for (var i = 0; i < ${fn:length(model.episodes)}; i++) {
                var checkbox = $("#episode" + i);
                if (checkbox.is(":checked")) {
                    result.push(checkbox.val());
                }
            }
            return result;
        }

        function getSelectedEpisodesMediaId() {
            var result = [];
            for (var i = 0; i < ${fn:length(model.episodes)}; i++) {
                var checkbox = $("#episode" + i);
                if (checkbox.is(":checked")) {
                    if (podcastEpisodes[i].status == 'COMPLETED') {
                        result.push(podcastEpisodes[i].mediaFileId);
                    } else {
                        $().toastmessage("showErrorToast", "<fmt:message key="podcastreceiver.episodedownloadnotcomplete"/>");
                    }
                }
            }
            return result;
        }

    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">

<div style="float:left;margin-right:1.5em;margin-bottom:1.5em">
<c:import url="coverArt.jsp">
    <c:param name="podcastChannelId" value="${model.channel.id}"/>
    <c:param name="coverArtSize" value="200"/>
</c:import>
</div>

<h1 id="name"><a href="podcastChannels.view"><fmt:message key="podcastreceiver.title"/></a> &raquo; ${fn:escapeXml(model.channel.title)}</h1>
<h2>
    <span class="header"><a href="javascript:top.playQueue.onPlayPodcastChannel(${model.channel.id})"><fmt:message key="main.playall"/></a></span>

    <c:if test="${model.user.podcastRole}">
        | <span class="header"><a href="javascript:deleteChannel()"><fmt:message key="common.delete"/></a></span>
        | <span class="header"><a href="javascript:refreshChannels()"><fmt:message key="podcastreceiver.check"/></a></span>
    </c:if>
</h2>

<div class="detail" style="padding-top:0.2em;white-space:normal;width:80%">${fn:escapeXml(model.channel.description)}</div>

<div class="detail" style="padding-top:1.0em">
    <fmt:message key="podcastreceiver.episodes"><fmt:param value="${fn:length(model.episodes)}"/></fmt:message> &ndash;
    <fmt:message key="podcastreceiver.status.${fn:toLowerCase(model.channel.status)}"/>
    <c:if test="${model.channel.status eq 'ERROR'}">
        <span class="warning">${model.channel.errorMessage}</span>
    </c:if>
</div>

<div style="height:0.7em;clear:both"></div>

<table class="music">
    <c:forEach items="${model.episodes}" var="episode" varStatus="i">

        <tr>

            <td class="fit"><input type="checkbox" id="episode${i.index}" value="${episode.id}"/></td>

            <c:choose>
                <c:when test="${empty episode.mediaFileId or episode.status ne 'COMPLETED'}">
                    <td colspan="4"></td>
                </c:when>
                <c:otherwise>
                    <c:import url="playButtons.jsp">
                        <c:param name="id" value="${episode.mediaFileId}"/>
                        <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                        <c:param name="addEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                        <c:param name="asTable" value="true"/>
                        <c:param name="onPlay" value="top.playQueue.onPlayPodcastEpisode(${episode.id})"/>
                    </c:import>
                </c:otherwise>
            </c:choose>

            <td class="truncate">
                    <span title="${episode.title}" class="songTitle">${episode.title}</span>
            </td>

            <td class="fit">
                <span class="detail">${episode.duration}</span>
            </td>

            <td class="fit">
                <span class="detail"><javatime:format value="${episode.publishDate}" style="M-"/></span>
            </td>

            <td class="fit" style="text-align:center">
                <span class="detail">
                    <c:choose>
                        <c:when test="${episode.status eq 'DOWNLOADING'}">
                            <fmt:formatNumber type="percent" value="${episode.completionRate}"/>
                        </c:when>
                        <c:otherwise>
                            <fmt:message key="podcastreceiver.status.${fn:toLowerCase(episode.status)}"/>
                        </c:otherwise>
                    </c:choose>
                </span>
            </td>

            <td class="truncate">
                <c:choose>
                    <c:when test="${episode.status eq 'ERROR'}">
                        <span class="detail warning" title="${episode.errorMessage}">${episode.errorMessage}</span>
                    </c:when>
                    <c:otherwise>
                        <span class="detail" title="${episode.description}">${episode.description}</span>
                    </c:otherwise>
                </c:choose>
            </td>

        </tr>
    </c:forEach>

</table>

<select id="moreActions" onchange="actionSelected(this.options[selectedIndex].id);" style="margin-bottom:1.0em">
    <option id="top" selected="selected"><fmt:message key="playlist.more"/></option>
    <option id="selectAll">&nbsp;&nbsp;<fmt:message key="playlist.more.selectall"/></option>
    <option id="selectNone">&nbsp;&nbsp;<fmt:message key="playlist.more.selectnone"/></option>
    <c:if test="${model.user.downloadRole}">
        <option id="download">&nbsp;&nbsp;<fmt:message key="common.download"/></option>
    </c:if>
    <option id="appendPlaylist">&nbsp;&nbsp;<fmt:message key="playlist.append"/></option>
</select>

<table style="padding-top:1em">
    <tr>
        <c:if test="${model.user.podcastRole}">
            <td style="padding-right:2em">
                <div class="forward"><a href="javascript:downloadSelected()"><fmt:message key="podcastreceiver.downloadselected"/></a></div>
            </td>
            <td style="padding-right:2em">
                <div class="forward"><a href="javascript:deleteSelected()"><fmt:message key="podcastreceiver.deleteselected"/></a></div>
            </td>
        </c:if>
        <td style="padding-right:2em">
            <div class="forward"><a href="javascript:refreshPage()"><fmt:message key="podcastreceiver.refresh"/></a>
            </div>
        </td>
        <c:if test="${model.user.adminRole}">
            <td style="padding-right:2em">
                <div class="forward"><a href="podcastSettings.view?"><fmt:message key="podcastreceiver.settings"/></a>
                </div>
            </td>
        </c:if>
    </tr>
</table>

<div id="dialog-delete" title="<fmt:message key='common.confirm'/>" style="display: none;">
    <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>
        <fmt:message key="podcastreceiver.confirmdelete"/></p>
</div>

<div id="dialog-select-playlist" title="<fmt:message key='main.addtoplaylist.title'/>" style="display: none;">
    <p><fmt:message key="main.addtoplaylist.text"/></p>
    <div id="dialog-select-playlist-list"></div>
</div>

</body>
</html>
