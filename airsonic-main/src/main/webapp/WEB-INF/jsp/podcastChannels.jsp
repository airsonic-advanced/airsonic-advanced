<!DOCTYPE html>
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

<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="table.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
    <script type="text/javascript" language="javascript">
      var podcasts = [];
      var user = "${model.username}";
      var viewAsList = ${model.viewAsList};
      var podcastsTable;

      function init() {
        podcastsTable = $("#podcastsTable").DataTable( {
            deferRender: true,
            createdRow(row, data, dataIndex, cells) {
                var rowNode = $(row);
                if (rowNode.hasClass("selected")) {
                    rowNode.find(".podcastIndex input").prop("checked", true);
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
                selector: ".podcastIndex"
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
                  className: "fit not-draggable podcastIndex centeralign",
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
                  className: "detail fit",
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
             updateSelectAllCheckboxStatus();
        } );
        podcastsTable.on( 'deselect', function ( e, dt, type, indexes ) {
             podcastsTable.cells( indexes, "podcastcheckbox:name" ).nodes().to$().find("input").prop("checked", false);
             updateSelectAllCheckboxStatus();
        } );
        $("#podcastsTable tbody").on( "click", ".playSong", function () {
            onPlay(podcastsTable.row( $(this).parents('tr') ).data().id);
        } );
        $("#podcastsTable tbody").on( "click", ".removePodcast", function () {
            onDelete([podcastsTable.row( $(this).parents('tr') ).data().id]);
        } );
        $(".podcastsSelectAll").on( "change", function (e) {
            selectAll(e.target.checked);
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
            '/user/queue/settings/viewAsList': function(msg) {
                viewChangedCallback(JSON.parse(msg.body));
            },
            // Add existing (initial population, one time)
            '/app/podcasts/all': function(msg) {
                populatePodcastCallback(JSON.parse(msg.body));
            }
        });

        viewSelectorRefresh();
        toggleViewDependentComponents();
      }

      function onPlay(id) {
        top.playQueue.onPlayPodcastChannel(id);
      }
      function onDelete(ids) {
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
      }
      function updatedPodcastCallback(id) {
        top.StompClient.send("/app/podcasts/channel", id);
      }
      function getPodcastCallback(podcast) {
        deletedPodcastCallback(podcast.id);
        podcasts.push(podcast);
        podcastsTable.ajax.reload().columns.adjust();
        generateThumb(podcast, 30);
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
            this.onDelete(podcastsTable.rows({ selected: true }).data().map(m => m.id).toArray());
        }
      }

      function selectAll(b) {
          if (b) {
              podcastsTable.rows().select();
          } else {
              podcastsTable.rows().deselect();
          }
      }

      function updateSelectAllCheckboxStatus() {
          if (podcastsTable.rows({selected: true}).indexes().length == 0) {
              $('.podcastsSelectAll').prop('checked', false);
              $('.podcastsSelectAll').prop('indeterminate', false);
          } else if (podcastsTable.rows({selected: true}).indexes().length == podcastsTable.rows().indexes().length) {
              $('.podcastsSelectAll').prop('checked', true);
              $('.podcastsSelectAll').prop('indeterminate', false);
          } else {
              $('.podcastsSelectAll').prop('indeterminate', true);
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

<table class="music indent hover nowrap stripe compact" id="podcastsTable" style="cursor: pointer; width: 100%; margin-top: 5px;">
</table>

<div class="tableSpacer"></div>

<div id="moreactions" style="white-space:nowrap;">
    <span class="header">
        <select id="moreActions" onchange="actionSelected(options[selectedIndex].id)">
            <option id="top" selected="selected"><fmt:message key="playlist.more"/></option>
            <optgroup label="<fmt:message key='playlists.more.selection'/>">
                <option id="removeSelected"><fmt:message key="playlist.remove"/></option>
            </optgroup>
        </select>
    </span>
</div>

<div class="tableSpacer"></div>

<div id="thumbs_wrapper">
    <p id="nopodcasts"><em><fmt:message key="podcastreceiver.empty"/></em></p>
    <div id="thumbs"></div>
</div>

<c:if test="${not empty model.newestEpisodes}">
    <h2 style="margin-top:1em"><fmt:message key="podcastreceiver.newestepisodes"/></h2>
    <table class="music indent">
        <c:forEach items="${model.newestEpisodes}" var="episode" varStatus="i">
            <tr>
                <c:import url="playButtons.jsp">
                    <c:param name="id" value="${episode.mediaFileId}"/>
                    <c:param name="podcastEpisodeId" value="${episode.id}"/>
                    <c:param name="playEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                    <c:param name="addEnabled" value="${model.user.streamRole and not model.partyMode}"/>
                    <c:param name="asTable" value="true"/>
                    <c:param name="onPlay" value="top.playQueue.onPlayNewestPodcastEpisode(${i.index})"/>
                </c:import>
                <c:set var="channelTitle" value="${model.channelMap[episode.channelId].title}"/>

                <td class="truncate">
                    <span title="${episode.title}" class="songTitle">${episode.title}</span>
                </td>

                <td class="truncate">
                    <a href="podcastChannel.view?id=${episode.channelId}"><span class="detail" title="${channelTitle}">${channelTitle}</span></a>
                </td>

                <td class="fit">
                    <span class="detail">${episode.duration}</span>
                </td>

                <td class="fit">
                    <span class="detail"><javatime:format value="${episode.publishDate}" style="M-"/></span>
                </td>

            </tr>
        </c:forEach>
    </table>
</c:if>

<table style="padding-top:1em"><tr>
    <c:if test="${model.user.podcastRole}">
        <td style="padding-right:2em"><div class="forward"><a href="podcastReceiverAdmin.view?refresh"><fmt:message key="podcastreceiver.check"/></a></div></td>
    </c:if>
    <c:if test="${model.user.podcastRole}">
        <td style="padding-right:2em"><div class="forward"><a href="rest/exportPodcasts/opml" download><fmt:message key="podcastreceiver.export"/></a></div></td>
    </c:if>
    <c:if test="${model.user.adminRole}">
        <td style="padding-right:2em"><div class="forward"><a href="podcastSettings.view?"><fmt:message key="podcastreceiver.settings"/></a></div></td>
    </c:if>
</tr></table>

<c:if test="${model.user.podcastRole}">
    <form:form method="post" action="podcastReceiverAdmin.view?">
        <table>
            <tr>
                <td><fmt:message key="podcastreceiver.subscribe"/></td>
                <td><input type="text" name="add" value="http://" style="width:30em" onclick="select()"/></td>
                <td><input type="submit" value="<fmt:message key='common.ok'/>"/></td>
            </tr>
        </table>
    </form:form>
</c:if>

</body>
</html>
