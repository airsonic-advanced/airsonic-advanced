<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="table.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>

    <script type="text/javascript" language="javascript">
      var playlists = [];
      var user = "${model.username}";
      var viewAsList = ${model.viewAsList};
      var playlistsTable;

      function init() {
        playlistsTable = $("#playlistsTable").DataTable( {
            deferRender: true,
            createdRow(row, data, dataIndex, cells) {
                var rowNode = $(row);
                if (rowNode.hasClass("selected")) {
                    rowNode.find(".playlistIndex input").prop("checked", true);
                }
            },
            colReorder: true,
            stateSave: true,
            stateDuration: 60 * 60 * 24 * 365,
            ordering: true,
            order: [],
            //orderFixed: [ 0, 'asc' ],
            orderMulti: true,
            pageLength: ${model.initialPaginationSize},
          <c:set var="paginationaddition" value="${fn:contains(' 10 20 50 100 -1', ' '.concat(model.initialPaginationSize)) ? '' : ', '.concat(model.initialPaginationSizeFiles)}" />
            lengthMenu: [[10, 20, 50, 100, -1 ${paginationaddition}], [10, 20, 50, 100, "All" ${paginationaddition}]],
            processing: true,
            autoWidth: true,
            scrollCollapse: true,
            //dom: "<'#filesHeader'><'tableSpacer'>lfrtip",
            select: {
                style: "multi",
                selector: ".playlistIndex"
            },
            ajax: function(ajaxData, callback) {
                for ( var i=0, len=playlists.length ; i<len ; i++ ) {
                  playlists[i].seq = i;
                }
                callback({data: playlists});
            },
            language: {
                emptyTable: "<fmt:message key='playlist2.noplaylists'/>"
            },
            stripeClasses: ["bgcolor2", "bgcolor1"],
            columnDefs: [{ targets: "_all", orderable: true }],
            columns: [
                { data: "id", className: "detail fit" },
                { data: null,
                  searchable: false,
                  name: "play",
                  className: "fit not-draggable",
                  defaultContent: "<img class='playSong' src=\"<spring:theme code='playImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.play'/>\" title=\"<fmt:message key='common.play'/>\">"
                },
                { data: null,
                  searchable: false,
                  name: "addLast",
                  className: "fit not-draggable",
                  defaultContent: "<img class='addSongLast' src=\"<spring:theme code='addImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.add'/>\" title=\"<fmt:message key='common.add'/>\">"
                },
                { data: null,
                  searchable: false,
                  name: "playlistcheckbox",
                  className: "fit not-draggable playlistIndex",
                  defaultContent: "<input type='checkbox'>"
                },
                { data: "name",
                  className: "detail fit",
                  title: "<fmt:message key='playlists.name'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<a>", {title: data, alt: data, text: data, target: "main"}).attr("href", "playlist.view?id=" + row.id)[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "comment",
                  className: "detail truncate",
                  title: "<fmt:message key='playlists.comment'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: data, alt: data, text: data})[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "shared", className: "detail truncate", title: "<fmt:message key='playlists.shared'/>"
                },
                { data: "fileCount", className: "detail fit rightalign", title: "<fmt:message key='playlists.filecount'/>" },
                { data: "duration",
                  className: "detail fit rightalign",
                  title: "<fmt:message key='playlists.duration'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return formatDuration(Math.round(data));
                      }
                      return data;
                  }
                },
                { data: "username",
                  className: "detail truncate",
                  title: "<fmt:message key='playlists.creator'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: data, alt: data, text: data})[0].outerHTML;
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
                { data: "importedFrom",
                  className: "detail fit rightalign truncate",
                  title: "<fmt:message key='playlists.import'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: data, alt: data, text: data})[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: null,
                  searchable: false,
                  name: "export",
                  className: "fit not-draggable",
                  defaultContent: "<img class='exportFile' src=\"<spring:theme code='exportFileImage'/>\" style='height:18px;' alt=\"<fmt:message key='playlist2.export'/>\" title=\"<fmt:message key='playlist2.export'/>\">"
                },
                { data: null,
                  searchable: false,
                  name: "remove",
                  className: "fit not-draggable",
                  render: function(data, type, row) {
                      if (type == "display" && row.username == user) {
                          return "<img class='removePlaylist' src=\"<spring:theme code='removeImage'/>\" style='height:18px;' alt=\"<fmt:message key='playlist.remove'/>\" title=\"<fmt:message key='playlist.remove'/>\">";
                      }
                      return data;
                  }
                }
            ]
        } );

        playlistsTable.on( 'select', function ( e, dt, type, indexes ) {
             playlistsTable.cells( indexes, "playlistcheckbox:name" ).nodes().to$().find("input").prop("checked", true);
        } );
        playlistsTable.on( 'deselect', function ( e, dt, type, indexes ) {
             playlistsTable.cells( indexes, "playlistcheckbox:name" ).nodes().to$().find("input").prop("checked", false);
        } );
        $("#playlistsTable tbody").on( "click", ".playSong", function () {
            onPlay(playlistsTable.row( $(this).parents('tr') ).index());
        } );
        $("#playlistsTable tbody").on( "click", ".addSongLast", function () {
            onAdd(playlistsTable.row( $(this).parents('tr') ).index());
        } );
        $("#playlistsTable tbody").on( "click", ".exportFile", function () {
            onExport(playlistsTable.row( $(this).parents('tr') ).index());
        } );
        $("#playlistsTable tbody").on( "click", ".removePlaylist", function () {
            onDelete(playlistsTable.row( $(this).parents('tr') ).index());
        } );

        top.StompClient.subscribe("playlists.jsp", {
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
            "/user/queue/settings/viewAsList": function(msg) {
                viewChangedCallback(JSON.parse(msg.body));
            },
            // Add existing (initial population, one time)
            '/app/playlists/readable': function(msg) {
                populatePlaylistCallback(JSON.parse(msg.body));
            }
        });
        
        viewSelectorRefresh();
        toggleViewDependentComponents();
      }

      function onPlay(index) {
        top.playQueue.onPlayPlaylist(playlists[index].id);
      }
      function onAdd(index) {
        top.playQueue.onAddPlaylist(playlists[index].id);
      }
      function onExport(index) {
        location.href="exportPlaylist.view?id="+playlists[index].id;
      }
      function onDelete(index) {
        top.StompClient.send("/app/playlists/delete", playlists[index].id);
      }

      function deletedPlaylistCallback(id) {
        playlists = playlists.filter(p => p.id != id);
        playlistsTable.ajax.reload().columns.adjust();
        removeThumb(id);
        if (playlists.length == 0) {
          $('#noplaylists').show();
        }
      }
      function populatePlaylistCallback(incoming) {
        playlists = incoming;
        playlistsTable.ajax.reload().columns.adjust();
        generateThumbs();
      }
      function updatedPlaylistCallback(playlist) {
        deletedPlaylistCallback(playlist.id);
        playlists.push(playlist);
        playlistsTable.ajax.reload().columns.adjust();
        generateThumb(playlist, 30);
      }
      function createEmptyPlaylist() {
        top.StompClient.send("/app/playlists/create/empty", "");
      }

      function viewSelectorRefresh() {
        if (viewAsList) {
            $('#viewAsList').addClass('headerSelected').removeClass('headerNotSelected');
            $('#viewAsGrid').addClass('headerNotSelected').removeClass('headerSelected');
        } else {
            $('#viewAsGrid').addClass('headerSelected').removeClass('headerNotSelected');
            $('#viewAsList').addClass('headerNotSelected').removeClass('headerSelected');
        }
      }

      function setViewAsList(view) {
        if (view != viewAsList) {
            top.StompClient.send("/app/settings/viewAsList", view);
            viewChangedCallback(view);
        }
      }
      function viewChangedCallback(view) {
        if (view != viewAsList) {
            viewAsList = view;
            viewSelectorRefresh();
            toggleViewDependentComponents();
        }
      }
      function toggleViewDependentComponents() {
        if (!viewAsList) {
            $('#thumbs_wrapper').show();
            $('#playlistsTable_wrapper').hide();
        } else {
            $('#thumbs_wrapper').hide();
            $('#playlistsTable_wrapper').show();
        }
      }
      function generateThumbs() {
        $('#thumbs').html('');

        playlists.forEach((p, i) => generateThumb(p, i * 30));
      }
      function generateThumb(playlist, delay) {
        $('#noplaylists').hide();
        var urlBase = "<c:url value='/coverArtJsp.view'/>";
        //append container first to keep order intact when async callback happens
        $('#thumbs').append('<div class="albumThumb" id="playlistThumb-' + playlist.id + '"></div>');
        var delayString = delay ? ("&appearAfter=" + delay) : "";
        $.get(urlBase + '?hideOverflow=true&showLink=true' + delayString + '&coverArtSize=160&captionCount=2&caption2=' + encodeURIComponent(playlist.fileCount + ' <fmt:message key="playlist2.songs"/> &ndash; ' + playlist.duration) + '&caption1=' + encodeURIComponent(playlist.name) +'&playlistId=' + playlist.id, data => {
            $('#playlistThumb-' + playlist.id).append(data);
        });
      }
      function removeThumb(playlistId) {
        $('#playlistThumb-' + playlistId).remove();
      }
    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">

<h1 style="padding-bottom: 1em">
    <img src="<spring:theme code='playlistImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="left.playlists"/></span>
</h1>

<div style="float:right;padding-right:1em">
    <img id="viewAsList" src="<spring:theme code='viewAsListImage'/>" alt="" class="headerSelected" style="margin-right:8px" onclick="setViewAsList(true)"/>
    <img id="viewAsGrid" src="<spring:theme code='viewAsGridImage'/>" alt="" class="headerNotSelected" onclick="setViewAsList(false)"/>
</div>
<div style="clear:both"></div>

<div class="tableSpacer"></div>

<table class="music indent hover nowrap stripe compact" id="playlistsTable" style="cursor: pointer; width: 100%; margin-top: 5px;">
</table>

<div class="tableSpacer"></div>

<div id="thumbs_wrapper">
    <p id="noplaylists"><em><fmt:message key="playlist2.noplaylists"/></em></p>
    <div id="thumbs"></div>
</div>

<h2>
  <span class="header"><a href="javascript:void(0)" onclick="createEmptyPlaylist();"><fmt:message key="left.createplaylist"/></a></span> | 
  <span class="header"><a href="importPlaylist.view" target="main"><fmt:message key="left.importplaylist"/></a></span>
</h2>

</body>
</html>
