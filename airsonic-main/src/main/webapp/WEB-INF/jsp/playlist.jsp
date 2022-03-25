<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="table.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
    <script type="text/javascript" language="javascript">
        var playlistId = ${model.playlist.id};
        var songs = [];

        let previousSortingOrder = { "col": 0, "dir": "asc", "indexes": [] };

        function init() {
            var ratingOnImage = "<spring:theme code='ratingOnImage'/>";
            var ratingOffImage = "<spring:theme code='ratingOffImage'/>";

            playlistMusicTable = $("#playlistMusic").DataTable( {
                deferRender: true,
                createdRow(row, data, dataIndex, cells) {
                    var rowNode = $(row);
                    if (rowNode.hasClass("selected")) {
                        rowNode.find(".playlistSongIndex input").prop("checked", true);
                    }
                },
                colReorder: true,
                fixedHeader: true,
                stateSave: true,
                stateDuration: 60 * 60 * 24 * 365,
                ordering: true,
                order: [],
              <c:if test="${!model.editAllowed}">
                orderFixed: [ 0, 'asc' ],
              </c:if>
                orderMulti: false,
                pageLength: ${model.initialPaginationSize},
              <c:set var="paginationaddition" value="${fn:contains(' 10 20 50 100 -1', ' '.concat(model.initialPaginationSize)) ? '' : ', '.concat(model.initialPaginationSize)}" />
                lengthMenu: [[10, 20, 50, 100, -1 ${paginationaddition}], [10, 20, 50, 100, "All" ${paginationaddition}]],
                processing: true,
                autoWidth: true,
                scrollCollapse: true,
                select: {
                style: "multi",
                    selector: ".playlistSongIndex"
                },
              <c:if test="${model.editAllowed}">
                rowReorder: {
                    dataSrc: "seq",
                    selector: "td:not(.not-draggable)"
                },
              </c:if>
                language: {
                    emptyTable: "<fmt:message key='playlist2.empty'/>"
                },
                ajax: function(ajaxData, callback) {
                    for ( var i=0, len=songs.length ; i<len ; i++ ) {
                      songs[i].seq = i;
                    }
                    callback({data: songs});
                },
                stripeClasses: ["bgcolor2", "bgcolor1"],
              <c:choose>
                <c:when test="${model.editAllowed}">
                columnDefs: [{ targets: "_all", orderable: true }],
                </c:when>
                <c:otherwise>
                columnDefs: [{ targets: "_all", orderable: false }],
                </c:otherwise>
              </c:choose>
                columns: [
                    { data: "seq", className: "detail fit", visible: true },
                    { data: null,
                      searchable: false,
                      name: "playlistsongcheckbox",
                      className: "fit not-draggable playlistSongIndex centeralign",
                      title: "<input type='checkbox' class='playlistSelectAll'>",
                      defaultContent: "<input type='checkbox'>"
                    },
                    { data: "starred",
                      name: "starred",
                      className: "fit not-draggable centeralign",
                      render: function(starred, type) {
                          if (type == "display") {
                              return "<img class='starSong' src='" + (starred ? ratingOnImage : ratingOffImage) + "' style='height:18px;' alt='' title=''>";
                          }
                          return starred ? "onlystarred" : "unstarred";
                      }
                    },
                    { data: "present",
                      searchable: false,
                      name: "play",
                      className: "fit not-draggable centeralign",
                      render: function(present, type, row) {
                          if (type == "display") {
                              if (present) {
                                  return "<img class='playSong' src=\"<spring:theme code='playImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.play'/>\" title=\"<fmt:message key='common.play'/>\">";
                              } else {
                                  return "";
                              }
                          }
                          return present ? "available" : "missing";
                      }
                    },
                    { data: "present",
                      searchable: false,
                      name: "addLast",
                      className: "fit not-draggable centeralign",
                      render: function(present, type, row) {
                          if (type == "display") {
                              if (present) {
                                  return "<img class='addSongLast' src=\"<spring:theme code='addImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.add'/>\" title=\"<fmt:message key='common.add'/>\">";
                              } else {
                                  return "";
                              }
                          }
                          return present ? "available" : "missing";
                      }
                    },
                    { data: "present",
                      searchable: false,
                      name: "addNext",
                      className: "fit not-draggable centeralign",
                      render: function(present, type, row) {
                          if (type == "display") {
                              if (present) {
                                  return "<img class='addSongNext' src=\"<spring:theme code='addNextImage'/>\" style='height:18px;' alt=\"<fmt:message key='main.addnext'/>\" title=\"<fmt:message key='main.addnext'/>\">";
                              } else {
                                  return "";
                              }
                          }
                          return present ? "available" : "missing";
                      }
                    },
                    { data: "present",
                      className: "detail fit",
                      render: function(present, type, row) {
                          if (type == "display") {
                              if (present) {
                                  return "";
                              } else {
                                  return "<span class='playlist-missing'><fmt:message key='playlist.missing'/></span>";
                              }
                          }
                          return present ? "available" : "missing";
                      }
                    },
                    { data: "trackNumber", className: "detail fit", visible: ${model.visibility.trackNumberVisible}, title: "<fmt:message key='personalsettings.tracknumber'/>" },
                    { data: "discNumber", className: "detail fit", visible: ${model.visibility.discNumberVisible}, title: "<fmt:message key='personalsettings.discnumber'/>" },
                    { data: "title",
                      className: "detail songTitle truncate",
                      title: "<fmt:message key='edittags.songtitle'/>",
                      render: function(title, type, row) {
                          if (type == "display" && title != null) {
                              return $("<span>", {title: title, alt: title, text: title})[0].outerHTML;
                          }
                          return title;
                      }
                    },
                    { data: "album",
                      className: "detail truncate",
                      visible: ${model.visibility.albumVisible},
                      title: "<fmt:message key='personalsettings.album'/>",
                      render: function(album, type, row) {
                          if (type == "display" && album != null) {
                              return $("<a>", {title: album, alt: album, text: album, target: "main"}).attr("href", "main.view?id=" + row.id)[0].outerHTML;
                          }
                          return album;
                      }
                    },
                    { data: "artist",
                      className: "detail truncate",
                      visible: ${model.visibility.artistVisible},
                      title: "<fmt:message key='personalsettings.artist'/>",
                      render: function(artist, type, row) {
                          if (type == "display" && artist != null) {
                              return $("<span>", {title: artist, alt: artist, text: artist})[0].outerHTML;
                          }
                          return artist;
                      }
                    },
                    { data: "albumArtist",
                      className: "detail truncate",
                      visible: ${model.visibility.albumArtistVisible},
                      title: "<fmt:message key='personalsettings.albumartist'/>",
                      render: function(artist, type, row) {
                          if (type == "display" && artist != null) {
                              return $("<span>", {title: artist, alt: artist, text: artist})[0].outerHTML;
                          }
                          return artist;
                      }
                    },
                    { data: "genre",
                      className: "detail truncate",
                      visible: ${model.visibility.genreVisible},
                      title: "<fmt:message key='personalsettings.genre'/>",
                      render(genre, type) {
                          if (type == "display" && genre != null) {
                              return $("<span>", {title: genre, alt: genre, text: genre})[0].outerHTML;
                          }
                          return genre;
                      }
                    },
                    { data: "year", className: "detail fit rightalign", visible: ${model.visibility.yearVisible}, title: "<fmt:message key='personalsettings.year'/>" },
                    { data: "format", className: "detail fit rightalign", visible: ${model.visibility.formatVisible}, title: "<fmt:message key='personalsettings.format'/>" },
                    { data: "fileSize", className: "detail fit rightalign", visible: ${model.visibility.fileSizeVisible}, title: "<fmt:message key='personalsettings.filesize'/>" },
                    { data: "duration",
                      className: "detail fit rightalign",
                      visible: ${model.visibility.durationVisible},
                      title: "<fmt:message key='personalsettings.duration'/>",
                      render: function(data, type, row) {
                          if (type == "display" && data != null) {
                              return formatDuration(Math.round(data));
                          }
                          return data;
                      }
                    },
                    { data: "bitRate", className: "detail fit rightalign", visible: ${model.visibility.bitRateVisible}, title: "<fmt:message key='personalsettings.bitrate'/>" },
                    { data: "playCount", className: "detail fit rightalign", visible: ${model.visibility.playCountVisible}, title: "<fmt:message key='personalsettings.playcount'/>" },
	                { data: "lastPlayed",
	                  className: "detail fit rightalign",
	                  visible: ${model.visibility.lastPlayedVisible},
	                  title: "<fmt:message key='personalsettings.lastplayed'/>",
	                  render: function(data, type, row) {
	                      if (data != null) {
	                          if (type == "display") {
	                              return new Date(data).toLocaleString();
	                          }
	                          return new Date(data).getTime();
	                      }
	                      return data;
	                  }
	                },
	                { data: "lastScanned",
	                  className: "detail fit rightalign",
	                  visible: ${model.visibility.lastScannedVisible},
	                  title: "<fmt:message key='personalsettings.lastscanned'/>",
	                  render: function(data, type, row) {
	                      if (data != null) {
	                          if (type == "display") {
	                              return new Date(data).toLocaleString();
	                          }
	                          return new Date(data).getTime();
	                      }
	                      return data;
	                  }
	                },
	                { data: "created",
	                  className: "detail fit rightalign",
	                  visible: ${model.visibility.createdVisible},
	                  title: "<fmt:message key='personalsettings.created'/>",
	                  render: function(data, type, row) {
	                      if (data != null) {
	                          if (type == "display") {
	                              return new Date(data).toLocaleString();
	                          }
	                          return new Date(data).getTime();
	                      }
	                      return data;
	                  }
	                },
	                { data: "changed",
	                  className: "detail fit rightalign",
	                  visible: ${model.visibility.changedVisible},
	                  title: "<fmt:message key='personalsettings.changed'/>",
	                  render: function(data, type, row) {
	                      if (data != null) {
	                          if (type == "display") {
	                              return new Date(data).toLocaleString();
	                          }
	                          return new Date(data).getTime();
	                      }
	                      return data;
	                  }
	                },
                    { data: null,
                      searchable: false,
                      name: "remove",
                      visible: ${model.editAllowed},
                      className: "fit not-draggable centeralign",
                      defaultContent: "<img class='removeSong' src=\"<spring:theme code='removeImage'/>\" style='height:18px;' alt=\"<fmt:message key='playlist.remove'/>\" title=\"<fmt:message key='playlist.remove'/>\">"
                    }
                ]
            } );

            playlistMusicTable.on( 'select', function ( e, dt, type, indexes ) {
                playlistMusicTable.cells( indexes, "playlistsongcheckbox:name" ).nodes().to$().find("input").prop("checked", true);
                updateSelectAllCheckboxStatus();
            } );
            playlistMusicTable.on( 'deselect', function ( e, dt, type, indexes ) {
                playlistMusicTable.cells( indexes, "playlistsongcheckbox:name" ).nodes().to$().find("input").prop("checked", false);
                updateSelectAllCheckboxStatus();
            } );
            $("#playlistMusic tbody").on( "click", ".starSong", function () {
                onStar(playlistMusicTable.row( $(this).parents('tr') ).index());
            } );
            $("#playlistMusic tbody").on( "click", ".playSong", function () {
                onPlay(playlistMusicTable.row( $(this).parents('tr') ).index());
            } );
            $("#playlistMusic tbody").on( "click", ".addSongLast", function () {
                onAdd(playlistMusicTable.row( $(this).parents('tr') ).index());
            } );
            $("#playlistMusic tbody").on( "click", ".addSongNext", function () {
                onAddNext(playlistMusicTable.row( $(this).parents('tr') ).index());
            } );
            $(".playlistSelectAll").on( "change", function (e) {
                selectAll(e.target.checked);
            } );

            top.StompClient.subscribe("playlist.jsp", {
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
            });

          <c:if test="${model.editAllowed}">
            $("#playlistMusic tbody").on( "click", ".removeSong", function () {
                onRemove([playlistMusicTable.row( $(this).parents('tr') ).index()]);
            } );
            playlistMusicTable.on( "row-reordered", function (e, diff, edit) {
                if (diff.length > 0) {
                    playlistMusicTable.one( "draw", function () {
                        onRearrange(playlistMusicTable.rows().indexes().toArray());
                    });
                    playlistMusicTable.order([0, "asc"]);
                }
            });
            playlistMusicTable.on("order", (e, settings, ordArr) => {
                if (previousSortingOrder.col !== ordArr[0].col || previousSortingOrder.dir !== ordArr[0].dir) {
                    playlistMusicTable.order([ordArr[0].col, ordArr[0].dir]);
                    playlistMusicTable.one( "draw", function () {
                        onRearrange(playlistMusicTable.rows().indexes().toArray());
                    });
                    previousSortingOrder = { "col": ordArr[0].col, "dir": ordArr[0].dir };
                }
            });

            $("#dialog-edit").dialog({resizable: true, width:400, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.save"/>": function() {
                        $(this).dialog("close");
                        var name = $("#newName").val();
                        var comment = $("#newComment").val();
                        var shared = $("#newShared").is(":checked");
                        top.StompClient.send("/app/playlists/update", JSON.stringify({id: playlistId, name: name, comment: comment, shared: shared}));
                    },
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    }
                }});

            $("#dialog-delete").dialog({resizable: false, height: 170, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.delete"/>": function() {
                        $(this).dialog("close");
                        top.StompClient.send("/app/playlists/delete", playlistId);
                    },
                    "<fmt:message key="common.cancel"/>": function() {
                        $(this).dialog("close");
                    } 
                }});
          </c:if>
        }

        function updatePlaylistEntries() {
            top.StompClient.send("/app/playlists/files/" + playlistId, "");
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
                $("#duration").text(formatDuration(Math.round(playlist.duration)));
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
            playlistMusicTable.ajax.reload().columns.adjust();
        }

        function onPlay(index) {
            top.playQueue.onPlayPlaylist(playlistId, index);
        }
        function onPlayAll() {
            top.playQueue.onPlayPlaylist(playlistId);
        }
        function onAddAll() {
            top.playQueue.onAddPlaylist(playlistId);
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
            songs[index].starred = !songs[index].starred;

            if (songs[index].starred) {
                top.StompClient.send("/app/rate/mediafile/star", JSON.stringify([songs[index].id]));
            } else {
                top.StompClient.send("/app/rate/mediafile/unstar", JSON.stringify([songs[index].id]));
            }
            playlistMusicTable.cell(index, "starred:name").invalidate();
        }
      <c:if test="${model.editAllowed}">
        function onRemove(indexes) {
            top.StompClient.send("/app/playlists/files/remove", JSON.stringify({id: playlistId, modifierIds: indexes}));
        }
        function onRearrange(indexes) {
            top.StompClient.send("/app/playlists/files/rearrange", JSON.stringify({id: playlistId, modifierIds: indexes}));
        }
        function onEditPlaylist() {
            $("#dialog-edit").dialog("open");
        }
        function onDeletePlaylist() {
            $("#dialog-delete").dialog("open");
        }
        <!-- actionSelected() is invoked when the users selects from the "More actions..." combo box. -->
        function actionSelected(id) {
          if (id == "top") {
              return;
          } else if (id == "removeSelected") {
              this.onRemove(playlistMusicTable.rows({ selected: true }).indexes().toArray());
          }
        }
      </c:if>

        function selectAll(b) {
            if (b) {
                playlistMusicTable.rows().select();
            } else {
                playlistMusicTable.rows().deselect();
            }
        }

        function updateSelectAllCheckboxStatus() {
            if (playlistMusicTable.rows({selected: true}).indexes().length == 0) {
                $('.playlistSelectAll').prop('checked', false);
                $('.playlistSelectAll').prop('indeterminate', false);
            } else if (playlistMusicTable.rows({selected: true}).indexes().length == playlistMusicTable.rows().indexes().length) {
                $('.playlistSelectAll').prop('checked', true);
                $('.playlistSelectAll').prop('indeterminate', false);
            } else {
                $('.playlistSelectAll').prop('indeterminate', true);
            }
        }
    </script>

    <style type="text/css">
        .playlist-missing {
            color: red;
            border: 1px solid red;
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
    <c:param name="coverArtSize" value="160"/>
</c:import>
</div>

<h1><a href="playlists.view"><fmt:message key="left.playlists"/></a> &raquo; <span id="name"></span></h1>
<h2>
    <span class="header"><a href="javascript:void(0)" onclick="onPlayAll();"><fmt:message key="common.play"/></a></span>
        | <span class="header"><a href="javascript:void(0)" onclick="onAddAll();"><fmt:message key="main.addall"/></a></span>
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

<table class="music indent hover nowrap stripe compact <c:if test='${!model.visibility.headerVisible}'>hide-table-header</c:if>" id="playlistMusic" style="cursor: pointer; width: 100%;">
</table>

<c:if test="${model.editAllowed}">
<div id="moreactions" style="white-space:nowrap;">
    <span class="header">
        <select id="moreActions" onchange="actionSelected(options[selectedIndex].id)">
            <option id="top" selected="selected"><fmt:message key="playlist.more"/></option>
            <optgroup label="<fmt:message key='playlist.more.selection'/>">
                <option id="removeSelected"><fmt:message key="playlist.remove"/></option>
            </optgroup>
        </select>
    </span>
</div>

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
