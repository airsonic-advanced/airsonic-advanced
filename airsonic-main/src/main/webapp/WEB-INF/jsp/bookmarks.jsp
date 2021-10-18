<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="table.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>

    <script type="text/javascript" language="javascript">

        var bookmarks = {};
        var bookmarksTable;

        function init() {
        	var ratingOnImage = "<spring:theme code='ratingOnImage'/>";
            var ratingOffImage = "<spring:theme code='ratingOffImage'/>";

            bookmarksTable = $("#bookmarksTable").DataTable( {
                deferRender: true,
                colReorder: true,
                stateSave: true,
                stateDuration: 60 * 60 * 24 * 365,
                ordering: true,
                order: [],
                orderMulti: true,
                pageLength: ${model.initialPaginationSize},
              <c:set var="paginationaddition" value="${fn:contains(' 10 20 50 100 -1', ' '.concat(model.initialPaginationSize)) ? '' : ', '.concat(model.initialPaginationSize)}" />
                lengthMenu: [[10, 20, 50, 100, -1 ${paginationaddition}], [10, 20, 50, 100, "All" ${paginationaddition}]],
                processing: true,
                autoWidth: true,
                scrollCollapse: true,
                language: {
                    emptyTable: "<fmt:message key='bookmarks.empty'/>"
                },
                ajax: function(ajaxData, callback) {
                    callback({data: Object.values(bookmarks)});
                },
                stripeClasses: ["bgcolor2", "bgcolor1"],
                columnDefs: [{ targets: "_all", orderable: true }],
                columns: [
                    { data: "id", className: "detail fit", visible: true },
                    { data: "mediaFileEntry.starred",
                      name: "starred",
                      className: "fit not-draggable",
                      render: function(starred, type) {
                          if (type == "display") {
                              return "<img class='starSong' src='" + (starred ? ratingOnImage : ratingOffImage) + "' style='height:18px;' alt='' title=''>";
                          }
                          return starred ? "onlystarred" : "unstarred";
                      }
                    },
                    { data: "mediaFileEntry.present",
                      searchable: false,
                      name: "play",
                      className: "fit not-draggable",
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
                    { data: "mediaFileEntry.present",
                      searchable: false,
                      name: "addLast",
                      className: "fit not-draggable",
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
                    { data: "mediaFileEntry.present",
                      searchable: false,
                      name: "addNext",
                      className: "fit not-draggable",
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
                    { data: "mediaFileEntry.title",
                      className: "detail songTitle truncate",
                      title: "<fmt:message key='edittags.songtitle'/>",
                      render: function(title, type, row) {
                          if (type == "display" && title != null) {
                              return $("<span>", {title: title, alt: title, text: title})[0].outerHTML;
                          }
                          return title;
                      }
                    },
                    { data: "mediaFileEntry.album",
                      className: "detail truncate",
                      title: "<fmt:message key='personalsettings.album'/>",
                      render: function(album, type, row) {
                          if (type == "display" && album != null) {
                              return $("<a>", {title: album, alt: album, text: album, target: "main"}).attr("href", "main.view?id=" + row.id)[0].outerHTML;
                          }
                          return album;
                      }
                    },
                    { data: "mediaFileEntry.artist",
                      className: "detail truncate",
                      title: "<fmt:message key='personalsettings.artist'/>",
                      render: function(artist, type, row) {
                          if (type == "display" && artist != null) {
                              return $("<span>", {title: artist, alt: artist, text: artist})[0].outerHTML;
                          }
                          return artist;
                      }
                    },
                    { data: "comment",
                      className: "detail truncate",
                      title: "<fmt:message key='sharesettings.description'/>",
                      render(comment, type) {
                          if (type == "display" && comment != null) {
                              return $("<span>", {title: comment, alt: comment, text: comment})[0].outerHTML;
                          }
                          return comment;
                      }
                    },
                    { data: "positionMillis",
                      className: "detail fit rightalign",
                      title: "<fmt:message key='bookmarks.position'/>",
                      render: function(data, type, row) {
                          if (type == "display" && data != null) {
                              return formatDuration(Math.round(data/1000)) + " / " + formatDuration(Math.round(row.mediaFileEntry.duration));
                          }
                          return data;
                      }
                    },
	                { data: "created",
	                  className: "detail fit rightalign",
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
                      className: "fit not-draggable",
                      defaultContent: "<img class='removeBookmark' src=\"<spring:theme code='removeImage'/>\" style='height:18px;' alt=\"<fmt:message key='playlist.remove'/>\" title=\"<fmt:message key='playlist.remove'/>\">"
                    }
                ]
            } );

            $("#bookmarksTable tbody").on( "click", ".starSong", function () {
                onStar(bookmarksTable.row( $(this).parents('tr') ).index());
            } );
            $("#bookmarksTable tbody").on( "click", ".playSong", function () {
                onPlay(bookmarksTable.row( $(this).parents('tr') ).index());
            } );
            $("#bookmarksTable tbody").on( "click", ".addSongLast", function () {
                onAdd(bookmarksTable.row( $(this).parents('tr') ).index());
            } );
            $("#bookmarksTable tbody").on( "click", ".addSongNext", function () {
                onAddNext(bookmarksTable.row( $(this).parents('tr') ).index());
            } );
            $("#bookmarksTable tbody").on( "click", ".removeBookmark", function () {
                onRemoveBookmark(bookmarksTable.row( $(this).parents('tr') ).index());
            } );

            top.StompClient.subscribe("bookmarks.jsp", {
	            '/user/queue/bookmarks/added': function(msg) {
	                addedBookmarksCallback(JSON.parse(msg.body));
	            },
	            '/user/queue/bookmarks/deleted': function(msg) {
	                deleteBookmarksCallback(JSON.parse(msg.body));
	            },
	            '/user/queue/bookmarks/get': function(msg) {
	                getBookmarkCallback(JSON.parse(msg.body));
	            },
	            //one-time population only
	            '/app/bookmarks/list': function(msg) {
	                getBookmarksCallback(JSON.parse(msg.body));
	            }
	        });
        }

        function onAdd(index) {
            top.playQueue.onAdd(bookmarksTable.row(index).data().mediaFileEntry.id);
            $().toastmessage('showSuccessToast', '<fmt:message key="main.addlast.toast"/>')
        }
        function onAddNext(index) {
            top.playQueue.onAddNext(bookmarksTable.row(index).data().mediaFileEntry.id);
            $().toastmessage('showSuccessToast', '<fmt:message key="main.addnext.toast"/>')
        }
        function onStar(index) {
            var bookmark = bookmarksTable.row(index).data();
            bookmark.mediaFileEntry.starred = !bookmark.mediaFileEntry.starred;

            if (bookmark.mediaFileEntry.starred) {
                top.StompClient.send("/app/rate/mediafile/star", JSON.stringify([bookmark.mediaFileEntry.id]));
            } else {
                top.StompClient.send("/app/rate/mediafile/unstar", JSON.stringify([bookmark.mediaFileEntry.id]));
            }
            bookmarksTable.cell(index, "starred:name").invalidate();
        }
        function onRemoveBookmark(index) {
            top.StompClient.send("/app/bookmarks/delete", bookmarksTable.row(index).data().mediaFileEntry.id);
        }
        function deleteBookmarksCallback(mediaFileId) {
            delete this.bookmarks[mediaFileId];
            bookmarksTable.ajax.reload().columns.adjust();
        }
        function addedBookmarksCallback(mediaFileId) {
            // get new (added in callback)
            top.StompClient.send("/app/bookmarks/get", mediaFileId);
        }
        function getBookmarkCallback(bookmark) {
            this.bookmarks[bookmark.mediaFileEntry.id] = bookmark;
            bookmarksTable.ajax.reload().columns.adjust();
        }
        function getBookmarksCallback(bookmarks) {
            this.bookmarks = bookmarks;
            bookmarksTable.ajax.reload().columns.adjust();
        }
    </script>
</head>

<body class="mainframe bgcolor1" onload="init()">

<h1>
    &nbsp;&nbsp;<fmt:message key="top.bookmarks"/>
</h1>

<table class="music indent hover nowrap stripe compact" id="bookmarksTable" style="cursor: pointer; width: 100%; margin-top: 5px;">
</table>

<p style="width:60%">
    <fmt:message key="bookmarks.info"/>
</p>

</body>
</html>
