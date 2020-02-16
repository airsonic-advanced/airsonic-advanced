<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="websocket.jsp" %>
    <script type="text/javascript" language="javascript">
        var playlistId = ${model.playlist.id};
        var songs;

        function init() {
            StompClient.subscribe({
                '/user/queue/playlists/deleted': function(msg) {
                    deletedPlaylistCallback(JSON.parse(msg.body));
                },
                '/topic/playlists/deleted': function(msg) {
                    deletedPlaylistCallback(JSON.parse(msg.body));
                },
                '/user/queue/playlists/updated': function(msg) {
                    updatedPlaylistCallback(JSON.parse(msg.body));
                },
                '/topic/playlists/updated': function(msg) {
                    updatedPlaylistCallback(JSON.parse(msg.body));
                },
                '/user/queue/playlists/files/${model.playlist.id}': function(msg) {
                    updatedPlaylistEntriesCallback(JSON.parse(msg.body));
                },
                //one-time population only
                '/app/playlists/${model.playlist.id}': function(msg) {
                    updatedPlaylistCallback(JSON.parse(msg.body));
                }
            }, false, updatePlaylistEntries);

            <c:if test="${model.editAllowed}">
            $("#dialog-edit").dialog({resizable: true, width:400, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.save"/>": function() {
                        $(this).dialog("close");
                        var name = $("#newName").val();
                        var comment = $("#newComment").val();
                        var shared = $("#newShared").is(":checked");
                        StompClient.send("/app/playlists/update", JSON.stringify({id: playlistId, name: name, comment: comment, shared: shared}));
                    },
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    }
                }});

            $("#dialog-delete").dialog({resizable: false, height: 170, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.delete"/>": function() {
                        $(this).dialog("close");
                        StompClient.send("/app/playlists/delete", playlistId);
                    },
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    } 
                }});

            $("#playlistBody").sortable({
                stop: function(event, ui) {
                    var indexes = [];
                    $("#playlistBody").children().each(function() {
                        var id = $(this).attr("id").replace("pattern", "");
                        if (id.length > 0) {
                            indexes.push(parseInt(id));
                        }
                    });
                    onRearrange(indexes);
                },
                cursor: "move",
                axis: "y",
                containment: "parent",
                helper: function(e, tr) {
                    var originals = tr.children();
                    var trclone = tr.clone();
                    trclone.children().each(function(index) {
                        // Set cloned cell sizes to match the original sizes
                        $(this).width(originals.eq(index).width());
                        $(this).css("maxWidth", originals.eq(index).width());
                        $(this).css("border-top", "1px solid black");
                        $(this).css("border-bottom", "1px solid black");
                    });
                    return trclone;
                }
            });
            </c:if>
        }
        
        function updatePlaylistEntries() {
            StompClient.send("/app/playlists/files/" + playlistId, "");
        }

        function deletedPlaylistCallback(id) {
            $().toastmessage('showSuccessToast', '<fmt:message key="playlist.toast.deletedplaylist"/> ' + id);
            if (playlistId == id) {
                location = "playlists.view";
            }
        }

        function updatedPlaylistCallback(playlist) {
            if (playlistId == playlist.id) {
                if (playlist.filesChanged) {
                    updatePlaylistEntries();
                }

                $("#name").text(playlist.name);
                $("#songCount").text(playlist.fileCount);
                $("#duration").text(playlist.durationAsString);
                $("#comment").text(playlist.comment);
                $("#lastupdated").text('<fmt:message key="playlist2.lastupdated"/> ' + new Date(playlist.changed));

                if (playlist.shared) {
                    $("#shared").html("<fmt:message key="playlist2.shared"/>");
                } else {
                    $("#shared").html("<fmt:message key="playlist2.notshared"/>");
                }

                $("#newName").val(playlist.name);
                $("#newComment").val(playlist.comment);
                $("#newShared").prop("checked", playlist.shared);
            }
        }

        function updatedPlaylistEntriesCallback(entries) {
            this.songs = entries

            if (songs.length == 0) {
                $("#empty").show();
            } else {
                $("#empty").hide();
            }

            // Delete all the rows except for the "pattern" row
            $("#playlistBody").children().not("#pattern").remove();

            // Create a new set cloned from the pattern row
            var id = songs.length;
            while (id--) {
                var song  = songs[id];
                var node = cloneNodeBySelector("#pattern", id);
                node.insertAfter("#pattern");

                if (song.starred) {
                    node.find("#starSong" + id).attr("src", "<spring:theme code='ratingOnImage'/>");
                } else {
                    node.find("#starSong" + id).attr("src", "<spring:theme code='ratingOffImage'/>");
                }
                if (!song.present) {
                    node.find("#missing" + id).show();
                    node.find("#play" + id).hide();
                    node.find("#add" + id).hide();
                    node.find("#addNext" + id).hide();
                }
                node.find("#index" + id).text(id);
                node.find("#title" + id).text(song.title);
                node.find("#title" + id).attr("title", song.title);
                node.find("#album" + id).text(song.album);
                node.find("#album" + id).attr("title", song.album);
                node.find("#albumUrl" + id).attr("href", "main.view?id=" + song.id);
                node.find("#artist" + id).text(song.artist);
                node.find("#artist" + id).attr("title", song.artist);
                node.find("#songDuration" + id).text(song.durationAsString);

                // Note: show() method causes page to scroll to top.
                node.css("display", "table-row");
            }
        }

        function onPlay(index) {
            top.playQueue.onPlayPlaylist(playlistId, index);
        }
        function onPlayAll() {
            top.playQueue.onPlayPlaylist(playlistId);
        }
        function onAdd(index) {
            top.playQueue.onAdd(songs[index].id);
            $().toastmessage('showSuccessToast', '<fmt:message key="main.addlast.toast"/>')
        }
        function onAddNext(index) {
            top.playQueue.onAddNext(songs[index].id);
            $().toastmessage('showSuccessToast', '<fmt:message key="main.addnext.toast"/>')
        }
        function onStar(index) {
            var imageId = "#starSong" + index;
            var mediaFileId = songs[index].id
            if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOnImage"/>") != -1) {
                $(imageId).attr("src", "<spring:theme code="ratingOffImage"/>");
                StompClient.send("/app/rate/unstar", mediaFileId);
            }
            else if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOffImage"/>") != -1) {
                $(imageId).attr("src", "<spring:theme code="ratingOnImage"/>");
                StompClient.send("/app/rate/star", mediaFileId);
            }
        }
        <c:if test="${model.editAllowed}">
        function onRemove(index) {
            StompClient.send("/app/playlists/files/remove", JSON.stringify({id: playlistId, modifierIds: [index]}));
        }
        function onRearrange(indexes) {
            StompClient.send("/app/playlists/files/rearrange", JSON.stringify({id: playlistId, modifierIds: indexes}));
        }
        function onEditPlaylist() {
            $("#dialog-edit").dialog("open");
        }
        function onDeletePlaylist() {
            $("#dialog-delete").dialog("open");
        }
        </c:if>

    </script>

    <style type="text/css">
        .playlist-missing {
            color: red;
            border: 1px solid red;
            display: none;
            font-size: 90%;
            padding-left: 5px;
            padding-right: 5px;
            margin-right: 5px;
        }
    </style>

</head>
<body class="mainframe bgcolor1" onload="init()">

<div style="float:left;margin-right:1.5em;margin-bottom:1.5em">
<c:import url="coverArt.jsp">
    <c:param name="playlistId" value="${model.playlist.id}"/>
    <c:param name="coverArtSize" value="200"/>
</c:import>
</div>

<h1><a href="playlists.view"><fmt:message key="left.playlists"/></a> &raquo; <span id="name"></span></h1>
<h2>
    <span class="header"><a href="javascript:void(0)" onclick="onPlayAll();"><fmt:message key="common.play"/></a></span>

    <c:if test="${model.user.downloadRole}">
        <c:url value="download.view" var="downloadUrl"><c:param name="playlist" value="${model.playlist.id}"/></c:url>
        | <span class="header"><a href="${downloadUrl}"><fmt:message key="common.download"/></a></span>
    </c:if>
    <c:if test="${model.user.shareRole}">
        <c:url value="createShare.view" var="shareUrl"><c:param name="playlist" value="${model.playlist.id}"/></c:url>
        | <span class="header"><a href="${shareUrl}"><fmt:message key="share.title"/></a></span>
    </c:if>
    <c:if test="${model.editAllowed}">
        | <span class="header"><a href="javascript:void(0)" onclick="onEditPlaylist();"><fmt:message key="common.edit"/></a></span>
        | <span class="header"><a href="javascript:void(0)" onclick="onDeletePlaylist();"><fmt:message key="common.delete"/></a></span>
    </c:if>
    <c:url value="exportPlaylist.view" var="exportUrl"><c:param name="id" value="${model.playlist.id}"/></c:url>
    | <span class="header"><a href="${exportUrl}"><fmt:message key="playlist2.export"/></a></span>

</h2>

<div id="comment" class="detail" style="padding-top:0.2em"></div>

<div class="detail" style="padding-top:0.2em">
    <span id="songCount"></span> <fmt:message key="playlist2.songs"/> &ndash; <span id="duration"></span>
</div>
<div class="detail" style="padding-top:0.2em">
    <fmt:message key="playlist2.created" var="created">
        <fmt:param>${model.playlist.username}</fmt:param>
        <fmt:param><javatime:format style="L-" value="${model.playlist.created}"/></fmt:param>
    </fmt:message>
    ${fn:escapeXml(created)}.
</div>
<div class="detail" style="padding-top:0.2em">
    <span id="lastupdated"></span>.
</div>
<div class="detail" style="padding-top:0.2em">
    <span id="shared"></span>.
</div>

<div style="height:0.7em;clear:both"></div>

<p id="empty" style="display: none;"><em><fmt:message key="playlist2.empty"/></em></p>

<table class="music" style="cursor:pointer">
    <tbody id="playlistBody">
    <tr id="pattern" style="display:none;margin:0;padding:0;border:0">
        <td class="fit">
            <img id="starSong" onclick="onStar(this.id.substring(8))" src="<spring:theme code='ratingOffImage'/>"
                 style="cursor:pointer;height:18px;" alt="" title=""></td>
        <td class="fit">
            <img id="play" src="<spring:theme code='playImage'/>" alt="<fmt:message key='common.play'/>" title="<fmt:message key='common.play'/>"
                 style="padding-right:0.1em;cursor:pointer;height:18px;" onclick="onPlay(this.id.substring(4))"></td>
        <td class="fit">
            <img id="add" src="<spring:theme code='addImage'/>" alt="<fmt:message key='common.add'/>" title="<fmt:message key='common.add'/>"
                 style="padding-right:0.1em;cursor:pointer;height:18px;" onclick="onAdd(this.id.substring(3))"></td>
        <td class="fit" style="padding-right:30px">
            <img id="addNext" src="<spring:theme code='addNextImage'/>" alt="<fmt:message key='main.addnext'/>" title="<fmt:message key='main.addnext'/>"
                 style="padding-right:0.1em;cursor:pointer;height:18px;" onclick="onAddNext(this.id.substring(7))"></td>

        <td class="fit rightalign"><span id="index">1</span></td>
        <td class="fit"><span id="missing" class="playlist-missing"><fmt:message key="playlist.missing"/></span></td>
        <td class="truncate"><span id="title" class="songTitle">Title</span></td>
        <td class="truncate"><a id="albumUrl" target="main"><span id="album" class="detail">Album</span></a></td>
        <td class="truncate"><span id="artist" class="detail">Artist</span></td>
        <td class="fit rightalign"><span id="songDuration" class="detail">Duration</span></td>

        <c:if test="${model.editAllowed}">
            <td class="fit">
                <img id="removeSong" onclick="onRemove(this.id.substring(10))" src="<spring:theme code='removeImage'/>"
                     style="cursor:pointer;height:18px;" alt="<fmt:message key='playlist.remove'/>" title="<fmt:message key='playlist.remove'/>"></td>
        </c:if>
    </tr>
    </tbody>
</table>

<c:if test="${model.editAllowed}">
<div id="dialog-delete" title="<fmt:message key='common.confirm'/>" style="display: none;">
    <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>
        <fmt:message key="playlist2.confirmdelete"/></p>
</div>

<div id="dialog-edit" title="<fmt:message key='common.edit'/>" style="display: none;">
    <form>
        <label for="newName" style="display:block;"><fmt:message key="playlist2.name"/></label>
        <input type="text" name="newName" id="newName" value="" class="ui-widget-content"
               style="display:block;width:95%;"/>
        <label for="newComment" style="display:block;margin-top:1em"><fmt:message key="playlist2.comment"/></label>
        <input type="text" name="newComment" id="newComment" value="" class="ui-widget-content"
               style="display:block;width:95%;"/>
        <input type="checkbox" name="newShared" id="newShared" style="margin-top:1.5em" class="ui-widget-content"/>
        <label for="newShared"><fmt:message key="playlist2.public"/></label>
    </form>
</div>
</c:if>

</body></html>
