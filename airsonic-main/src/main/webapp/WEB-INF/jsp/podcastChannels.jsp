<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="table.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
    <script type="text/javascript" language="javascript">
      var podcasts = [];
      var searchResults = [];
      var newestPodcasts = [];
      var user = "${model.username}";
      var viewAsList = ${model.viewAsList};
      var podcastsTable;
      var podcastIndexTable;
      var newestPodcastTable;

      function init() {
        podcastsTable = $("#podcastsTable").DataTable( {
            deferRender: true,
            createdRow(row, data, dataIndex, cells) {
                var rowNode = $(row);
                if (rowNode.hasClass("selected")) {
                    rowNode.find(".podcastCheckbox input").prop("checked", true);
                }
            },
            colReorder: true,
            fixedHeader: true,
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
                selector: ".podcastCheckbox"
            },
            ajax: function(ajaxData, callback) {
                for ( var i=0, len=podcasts.length ; i<len ; i++ ) {
                  podcasts[i].seq = i;
                }
                callback({data: podcasts});
            },
            language: {
                emptyTable: "<fmt:message key="podcastreceiver.empty"/>"
            },
            stripeClasses: ["bgcolor2", "bgcolor1"],
            columnDefs: [{ targets: "_all", orderable: true }],
            columns: [
                { data: "seq", className: "detail fit" },
                { data: null,
                  searchable: false,
                  name: "podcastcheckbox",
                  className: "fit not-draggable podcastCheckbox centeralign",
                  title: "<input type='checkbox' class='podcastsSelectAll centeralign'>",
                  defaultContent: "<input type='checkbox'>"
                },
                { data: null,
                  searchable: false,
                  name: "play",
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='playSong' src=\"<spring:theme code='playImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.play'/>\" title=\"<fmt:message key='common.play'/>\">"
                },
                { data: "id",
                  className: "detail fit centeralign",
                  title: "<fmt:message key='podcastreceiver.id'/>"
                },
                { data: "title",
                  className: "detail fit songTitle",
                  title: "<fmt:message key='edittags.songtitle'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<a>", {title: data, alt: data, text: data, target: "main"}).attr("href", "podcastChannel.view?id=" + row.id)[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "description",
                  className: "detail truncate",
                  title: "<fmt:message key='playlists.comment'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: data, alt: data, text: data})[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "url",
                  className: "detail truncate",
                  title: "<fmt:message key='podcastreceiver.url'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: data, alt: data, text: data})[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "fileCount", className: "detail fit centeralign", title: "<fmt:message key='podcastreceiver.filecount'/>" },
                { data: "downloadedCount", className: "detail fit centeralign", title: "<fmt:message key='podcastreceiver.downloadedcount'/>" },
                { data: "status",
                  className: "detail fit centeralign",
                  title: "<fmt:message key='status.title'/>",
                },
                { data: null,
                  searchable: false,
                  name: "remove",
                  className: "fit not-draggable centeralign",
                  render: function(data, type, row) {
                      if (type == "display") {
                          return "<img class='removePodcast' src=\"<spring:theme code='removeImage'/>\" style='height:18px;' alt=\"<fmt:message key='playlist.remove'/>\" title=\"<fmt:message key='playlist.remove'/>\">";
                      }
                      return data;
                  }
                }
            ]
        } );

        podcastsTable.on( 'select', function ( e, dt, type, indexes ) {
             podcastsTable.cells( indexes, "podcastcheckbox:name" ).nodes().to$().find("input").prop("checked", true);
             updateSelectAllCheckboxStatus(podcastsTable, '.podcastsSelectAll');
        } );
        podcastsTable.on( 'deselect', function ( e, dt, type, indexes ) {
             podcastsTable.cells( indexes, "podcastcheckbox:name" ).nodes().to$().find("input").prop("checked", false);
             updateSelectAllCheckboxStatus(podcastsTable, '.podcastsSelectAll');
        } );
        $("#podcastsTable tbody").on( "click", ".playSong", function () {
            onPlayChannel(podcastsTable.row( $(this).parents('tr') ).data().id);
        } );
        $("#podcastsTable tbody").on( "click", ".removePodcast", function () {
            onDeleteChannels([podcastsTable.row( $(this).parents('tr') ).data().id]);
        } );
        $(".podcastsSelectAll").on( "change", function (e) {
            selectAll(podcastsTable, e.target.checked);
        } );

        newestPodcastTable = $("#newestPodcastTable").DataTable( {
            deferRender: true,
            createdRow(row, data, dataIndex, cells) {
                var rowNode = $(row);
                if (rowNode.hasClass("selected")) {
                    rowNode.find(".newestPodcastCheckbox input").prop("checked", true);
                }
            },
            colReorder: true,
            fixedHeader: true,
            stateSave: false,
            //stateDuration: 60 * 60 * 24 * 365,
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
                selector: ".newestPodcastCheckbox"
            },
            ajax: function(ajaxData, callback) {
                for ( var i=0, len=newestPodcasts.length ; i<len ; i++ ) {
                  newestPodcasts[i].seq = i;
                }
                callback({data: newestPodcasts});
            },
            stripeClasses: ["bgcolor2", "bgcolor1"],
            columnDefs: [{ targets: "_all", orderable: true }],
            columns: [
                { data: "seq", className: "detail fit" },
                { data: null,
                  searchable: false,
                  name: "newestpodcastcheckbox",
                  className: "fit not-draggable newestPodcastCheckbox centeralign",
                  title: "<input type='checkbox' class='newestPodcastSelectAll centeralign'>",
                  defaultContent: "<input type='checkbox'>"
                },
                { data: null,
                  searchable: false,
                  visible: ${model.user.streamRole and not model.partyMode},
                  name: "play",
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='playSong' src=\"<spring:theme code='playImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.play'/>\" title=\"<fmt:message key='common.play'/>\">"
                },
                { data: null,
                  searchable: false,
                  visible: ${model.user.streamRole and not model.partyMode},
                  name: "addLast",
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='addSongLast' src=\"<spring:theme code='addImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.add'/>\" title=\"<fmt:message key='common.add'/>\">"
                },
                { data: null,
                  searchable: false,
                  visible: ${model.user.streamRole and not model.partyMode},
                  name: "addNext",
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='addSongNext' src=\"<spring:theme code='addNextImage'/>\" style='height:18px;' alt=\"<fmt:message key='main.addnext'/>\" title=\"<fmt:message key='main.addnext'/>\">"
                },
                { data: "id",
                  className: "detail fit centeralign",
                  title: "<fmt:message key='podcastreceiver.id'/>"
                },
                { data: "title",
                  className: "detail fit songTitle",
                  title: "<fmt:message key='edittags.songtitle'/>"
                },
                { data: "channelId",
                  className: "detail truncate",
                  name: "channel",
                  title: "<fmt:message key='podcastreceiver.channel'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          var channels = podcasts.filter(p => p.id == data);
                          var name = data;
                          if (channels.length == 1) {
                             name = channels[0].title;
                          }
                          return $("<a>", {title: name, alt: name, text: name, target: "main"}).attr("href", "podcastChannel.view?id=" + data)[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "duration",
                  className: "detail fit",
                  title: "<fmt:message key='playlists.duration'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: data, alt: data, text: data})[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "publishDate",
                  className: "detail fit",
                  title: "<fmt:message key='podcastreceiver.published'/>",
                  render: function(data, type, row) {
                      if (data != null) {
                          if (type == "display") {
                              return new Date(data).toLocaleString();
                          }
                          return new Date(data).getTime();
                      }
                      return data;
                  }
                }
            ]
        } );

        newestPodcastTable.on( 'select', function ( e, dt, type, indexes ) {
             newestPodcastTable.cells( indexes, "newestpodcastcheckbox:name" ).nodes().to$().find("input").prop("checked", true);
             updateSelectAllCheckboxStatus(newestPodcastTable, '.newestPodcastSelectAll');
        } );
        newestPodcastTable.on( 'deselect', function ( e, dt, type, indexes ) {
             newestPodcastTable.cells( indexes, "newestpodcastcheckbox:name" ).nodes().to$().find("input").prop("checked", false);
             updateSelectAllCheckboxStatus(newestPodcastTable, '.newestPodcastSelectAll');
        } );
        $("#newestPodcastTable tbody").on( "click", ".playSong", function () {
            onPlayEpisode(newestPodcastTable.row( $(this).parents('tr') ).data().id);
        } );
        $("#newestPodcastTable tbody").on( "click", ".addSongLast", function () {
            onAddLastMediaFile(newestPodcastTable.row( $(this).parents('tr') ).data().mediaFileId);
        } );
        $("#newestPodcastTable tbody").on( "click", ".addSongNext", function () {
            onAddNextMediaFile(newestPodcastTable.row( $(this).parents('tr') ).data().mediaFileId);
        } );
        $(".newestPodcastSelectAll").on( "change", function (e) {
            selectAll(newestPodcastTable, e.target.checked);
        } );

        podcastIndexTable = $("#podcastIndexTable").DataTable( {
            deferRender: true,
            createdRow(row, data, dataIndex, cells) {
                var rowNode = $(row);
                if (rowNode.hasClass("selected")) {
                    rowNode.find(".podcastIndexCheckbox input").prop("checked", true);
                }
            },
            colReorder: true,
            fixedHeader: true,
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
                selector: ".podcastIndexCheckbox"
            },
            ajax: function(ajaxData, callback) {
                for ( var i=0, len=searchResults.length ; i<len ; i++ ) {
                  searchResults[i].seq = i;
                }
                callback({data: searchResults});
            },
            language: {
                emptyTable: "<fmt:message key="podcastreceiver.nosearchresults"/>"
            },
            stripeClasses: ["bgcolor2", "bgcolor1"],
            columnDefs: [{ targets: "_all", orderable: true }],
            columns: [
                { data: "seq", className: "detail fit" },
                { data: null,
                  searchable: false,
                  name: "podcastindexcheckbox",
                  className: "fit not-draggable podcastIndexCheckbox centeralign",
                  title: "<input type='checkbox' class='podcastIndexSelectAll centeralign'>",
                  defaultContent: "<input type='checkbox'>"
                },
                { data: "id",
                  className: "detail fit centeralign",
                  title: "<fmt:message key='podcastreceiver.id'/>"
                },
                { data: "title",
                  className: "detail fit songTitle",
                  title: "<fmt:message key='edittags.songtitle'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: data, alt: data, text: data})[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "description",
                  className: "detail truncate",
                  title: "<fmt:message key='playlists.comment'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: data, alt: data, text: data})[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "url",
                  className: "detail truncate",
                  title: "<fmt:message key='podcastreceiver.url'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: data, alt: data, text: data})[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "language", className: "detail fit centeralign", title: "<fmt:message key='podcastreceiver.language'/>" },
                { data: "categories",
                  className: "detail fit centeralign",
                  title: "<fmt:message key='podcastreceiver.categories'/>",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return $("<span>", {title: Object.values(data), alt: Object.values(data), text: Object.values(data)})[0].outerHTML;
                      }
                      return data;
                  }
                },
                { data: "dead",
                  searchable: false,
                  className: "detail fit centeralign",
                  title: "<fmt:message key='podcastreceiver.dead'/>",
                  render: function(data, type, row) {
                      return data ? "Yes" : "No";
                  }
                },
                { data: "locked",
                  searchable: false,
                  className: "detail fit centeralign",
                  title: "<fmt:message key='podcastreceiver.locked'/>",
                  render: function(data, type, row) {
                      return data ? "Yes" : "No";
                  }
                },
                { data: "lastUpdateTime",
                  className: "detail fit centeralign",
                  title: "<fmt:message key='credentialsettings.updated'/>",
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
                  name: "subscribe",
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='createChannel' src=\"<spring:theme code='exportFileImage'/>\" style='height:18px;' alt=\"<fmt:message key='playlist2.export'/>\" title=\"<fmt:message key='playlist2.export'/>\">"
                }
            ]
        } );

        podcastIndexTable.on( 'select', function ( e, dt, type, indexes ) {
             podcastIndexTable.cells( indexes, "podcastindexcheckbox:name" ).nodes().to$().find("input").prop("checked", true);
             updateSelectAllCheckboxStatus(podcastIndexTable, '.podcastIndexSelectAll');
        } );
        podcastIndexTable.on( 'deselect', function ( e, dt, type, indexes ) {
             podcastIndexTable.cells( indexes, "podcastindexcheckbox:name" ).nodes().to$().find("input").prop("checked", false);
             updateSelectAllCheckboxStatus(podcastIndexTable, '.podcastIndexSelectAll');
        } );
        $("#podcastIndexTable tbody").on( "click", ".createChannel", function () {
            onCreateChannel(podcastIndexTable.row( $(this).parents('tr') ).data().url);
        } );
        $(".podcastIndexSelectAll").on( "change", function (e) {
            selectAll(podcastIndexTable, e.target.checked);
        } );

        top.StompClient.subscribe("podcastChannels.jsp", {
            '/topic/podcasts/deleted': function(msg) {
                deletedPodcastCallback(JSON.parse(msg.body));
            },
            '/topic/podcasts/updated': function(msg) {
                updatedPodcastCallback(JSON.parse(msg.body));
            },
            '/user/queue/podcasts/channel': function(msg) {
                getPodcastCallback(JSON.parse(msg.body));
            },
            '/user/queue/podcasts/search': function(msg) {
                searchCallback(JSON.parse(msg.body));
            },
            '/user/queue/settings/viewAsList': function(msg) {
                viewChangedCallback(JSON.parse(msg.body));
            },
            // Add existing (initial population, one time)
            '/app/podcasts/all': function(msg) {
                populatePodcastCallback(JSON.parse(msg.body));
            },
            '/app/podcasts/episodes/newest': function(msg) {
                populateNewestPodcastsCallback(JSON.parse(msg.body));
            }
        });

        $('#podcastindexsearch').on('change', evt => onSearch(evt.target.value));
        $('#directsubscribeok').on('click', evt => onCreateChannel($('#directsubscribe').val()));
        $('#refreshAllChannels').on('click', evt => onRefreshAllChannels());
        $('#exportOpml').on('click', evt => onExportOpml());
        $('#podcastSettings').on('click', evt => top.main.location.href = "podcastSettings.view?");

        viewSelectorRefresh();
        toggleViewDependentComponents();
      }

      function onPlayChannel(id) {
        top.playQueue.onPlayPodcastChannel(id);
      }
      function onDeleteChannels(ids) {
        top.StompClient.send("/app/podcasts/delete", JSON.stringify(ids));
      }

      function deletedPodcastCallback(id) {
        podcasts = podcasts.filter(p => p.id != id);
        podcastsTable.ajax.reload().columns.adjust();
        removeThumb(id);
        if (podcasts.length == 0) {
          $('#nopodcasts').show();
        }
      }
      function populatePodcastCallback(incoming) {
        podcasts = incoming;
        podcastsTable.ajax.reload().columns.adjust();
        generateThumbs();
        // reload podcast names
        newestPodcastTable.ajax.reload().columns.adjust();
      }
      function populateNewestPodcastsCallback(incoming) {
        newestPodcasts = incoming;
        newestPodcastTable.ajax.reload().columns.adjust();
      }
      function updatedPodcastCallback(id) {
        top.StompClient.send("/app/podcasts/channel", id);
      }
      function getPodcastCallback(podcast) {
        deletedPodcastCallback(podcast.id);
        podcasts.push(podcast);
        podcastsTable.ajax.reload().columns.adjust();
        generateThumb(podcast, 30);
        // reload podcast names
        newestPodcastTable.ajax.reload().columns.adjust();
      }

      function onPlayEpisode(id) {
        top.playQueue.onPlayPodcastEpisode(id);
      }
      function onAddLastMediaFile(mid) {
        if (mid != null) {
          top.playQueue.onAdd(mid);
        }
      }
      function onAddNextMediaFile(mid) {
        if (mid != null) {
          top.playQueue.onAddNext(mid);
        }
      }

      function onSearch(query) {
          top.StompClient.send("/app/podcasts/search", query);
      }
      function searchCallback(results) {
          searchResults = results;
          podcastIndexTable.ajax.reload().columns.adjust();
      }
      function onCreateChannel(url) {
          top.StompClient.send("/app/podcasts/create", url);
      }

      //simulates a tag being clicked for now (button behaving like a link)
      function onExportOpml() {
          const a = document.createElement('a');
          a.href = "rest/exportPodcasts/opml";
          a.download = true;
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
      }

      function onRefreshAllChannels() {
          onRefreshChannels(podcasts.map(p => p.id));
      }
      function onRefreshChannels(ids) {
          top.StompClient.send("/app/podcasts/refresh", JSON.stringify(ids));
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
            $('#podcastsTable_wrapper').hide();
            $('#moreactions').hide();
        } else {
            $('#thumbs_wrapper').hide();
            $('#podcastsTable_wrapper').show();
            $('#moreactions').show();
        }
      }
      function generateThumbs() {
        $('#thumbs').html('');

        podcasts.forEach((p, i) => generateThumb(p, i * 30));
      }
      function generateThumb(podcast, delay) {
        $('#nopodcasts').hide();
        var urlBase = "<c:url value='/coverArtJsp.view'/>";
        //append container first to keep order intact when async callback happens
        $('#thumbs').append('<div class="albumThumb" id="podcastThumb-' + podcast.id + '"></div>');
        var delayString = delay ? ("&appearAfter=" + delay) : "";
        $.get(urlBase + '?hideOverflow=true&showLink=true' + delayString + '&coverArtSize=160&captionCount=2&caption2=' + encodeURIComponent(podcast.fileCount + ' <fmt:message key="podcastreceiver.episodes" /> ') + '&caption1=' + encodeURIComponent(podcast.title) +'&podcastChannelId=' + podcast.id, data => {
            $('#podcastThumb-' + podcast.id).append(data);
        });
      }
      function removeThumb(podcastId) {
        $('#podcastThumb-' + podcastId).remove();
      }

      <!-- actionSelected() is invoked when the users selects from the "More actions..." combo box. -->
      function actionSelected(id) {
        if (id == "top") {
            return;
        } else if (id == "removeSelected") {
            this.onDeleteChannels(podcastsTable.rows({ selected: true }).data().map(m => m.id).toArray());
        } else if (id == "refreshSelected") {
            this.onRefreshChannels(podcastsTable.rows({ selected: true }).data().map(m => m.id).toArray());
        } else if (id == "subscribeSelected") {
            podcastIndexTable.rows({ selected: true }).data().map(m => m.url).toArray().forEach(url => this.onCreateChannel(url));
        }
      }

      function selectAll(table, b) {
          if (b) {
              table.rows().select();
          } else {
              table.rows().deselect();
          }
      }

      function updateSelectAllCheckboxStatus(table, selectAllCheckboxClass) {
          if (table.rows({selected: true}).indexes().length == 0) {
              $(selectAllCheckboxClass).prop('checked', false);
              $(selectAllCheckboxClass).prop('indeterminate', false);
          } else if (table.rows({selected: true}).indexes().length == table.rows().indexes().length) {
              $(selectAllCheckboxClass).prop('checked', true);
              $(selectAllCheckboxClass).prop('indeterminate', false);
          } else {
              $(selectAllCheckboxClass).prop('indeterminate', true);
          }
      }
    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">

<h1 style="padding-bottom: 1em">
    <img src="<spring:theme code='podcastLargeImage'/>" alt="">
    <span style="vertical-align: middle"><fmt:message key="podcastreceiver.title"/></span>
</h1>

<div style="float:right;padding-right:1em">
    <img id="viewAsList" src="<spring:theme code='viewAsListImage'/>" alt="" class="headerSelected" style="margin-right:8px" onclick="setViewAsList(true)"/>
    <img id="viewAsGrid" src="<spring:theme code='viewAsGridImage'/>" alt="" class="headerNotSelected" onclick="setViewAsList(false)"/>
</div>
<div style="clear:both"></div>

<div class="tableSpacer"></div>
<table class="music indent hover nowrap stripe compact" id="podcastsTable" style="width: 100%; margin-top: 5px;"></table>

<div id="moreactions" style="white-space:nowrap;">
    <span class="header">
        <select id="moreActions" onchange="actionSelected(options[selectedIndex].id)">
            <option id="top" selected="selected"><fmt:message key="playlist.more"/></option>
            <optgroup label="<fmt:message key='podcastreceiver.selectedchannels'/>">
                <option id="removeSelected"><fmt:message key="playlist.remove"/></option>
                <option id="refreshSelected"><fmt:message key="podcastreceiver.check"/></option>
            </optgroup>
        </select>
    </span>
</div>

<div id="thumbs_wrapper">
    <p id="nopodcasts"><em><fmt:message key="podcastreceiver.empty"/></em></p>
    <div id="thumbs"></div>
</div>

<div class="tableSpacer"></div>
<div>
    <c:if test="${model.user.podcastRole}">
        <button id="refreshAllChannels"><fmt:message key="podcastreceiver.check"/></button>
    </c:if>
    <c:if test="${model.user.podcastRole}">
        <button id="exportOpml"><fmt:message key="podcastreceiver.export"/></button>
    </c:if>
    <c:if test="${model.user.adminRole}">
        <button id="podcastSettings"><fmt:message key="podcastreceiver.settings"/></button>
    </c:if>
</div>

<div class="tableSpacer"></div>
<h3><fmt:message key="podcastreceiver.newestepisodes"/></h3>

<table class="music indent hover nowrap stripe compact" id="newestPodcastTable" style="width: 100%; margin-top: 5px;"></table>

<c:if test="${model.user.podcastRole}">
    <div class="tableSpacer"></div>
    <h3><fmt:message key="podcastreceiver.subscribe"/></h3>

    <div>
      <label for="directsubscribe"><span><fmt:message key="podcastreceiver.directly"/></span></label>
      <input type="text" name="directsubscribe" id="directsubscribe" value="http://" style="width:30em" onclick="select()"/>
      <button for="directsubscribe" id="directsubscribeok"><fmt:message key='common.ok'/></button>
    </div>

    <span><fmt:message key="podcastreceiver.or"/></span>
  <c:if test="${model.podcastIndexEnabled}">
    <div>
        <label for="podcastindexsearch"><span><fmt:message key="podcastreceiver.podcastindexsearch"/></span></label>
        <input type="text" name="podcastindexsearch" id="podcastindexsearch" value="" style="width:30em"/>
    </div>

    <div class="tableSpacer"></div>
    <table class="music indent hover nowrap stripe compact" id="podcastIndexTable" style="width: 100%; margin-top: 5px;"></table>

    <div class="tableSpacer"></div>
    <div id="moreactionsSearch" style="white-space:nowrap;">
        <span class="header">
            <select onchange="actionSelected(options[selectedIndex].id)">
                <option id="top" selected="selected"><fmt:message key="playlist.more"/></option>
                <optgroup label="<fmt:message key='podcastreceiver.selectedsearches'/>">
                    <option id="subscribeSelected"><fmt:message key="podcastreceiver.subscribe"/></option>
                </optgroup>
            </select>
        </span>
    </div>
  </c:if>
  <c:if test="${!model.podcastIndexEnabled}">
    <div><span><fmt:message key="podcastreceiver.enablepodcastindex"/></span></div>
  </c:if>
</c:if>

</body>
</html>
