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
  ~  Copyright 2014 (C) Sindre Mehus
  --%>

<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="table.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/jquery.fancyzoom.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
    <style type="text/css">
        #topSongsSpacer {
            height: 1em;
        }
        #topSongsHeader {
            display: inline-block;
        }
    </style>
<script type="text/javascript" language="javascript">

    var topSongs = [];
    var artistTopSongsTable = null;

    function init() {
        $("a.fancy").fancyZoom({
            minBorder: 30
        });
        <c:if test="${model.showArtistInfo}">
        var ratingOnImage = "<spring:theme code='ratingOnImage'/>";
        var ratingOffImage = "<spring:theme code='ratingOffImage'/>";

        artistTopSongsTable = $("#artistTopSongs").DataTable( {
            deferRender: true,
            ordering: true,
            order: [],
            orderFixed: [ 0, 'asc' ],
            orderMulti: false,
            lengthMenu: [[10, 20, 50, 100, -1], [10, 20, 50, 100, "All"]],
            processing: true,
            autoWidth: true,
            scrollCollapse: true,
            scrollY: "60vh",
            dom: "<'#topSongsHeader'><'#topSongsSpacer'>lfrtip",
            ajax: function(ajaxData, callback) {
                for ( var i=0, len=topSongs.length ; i<len ; i++ ) {
                  topSongs[i].seq = i;
                }
                callback({data: topSongs});
            },
            stripeClasses: ["bgcolor2", "bgcolor1"],
            columnDefs: [{ targets: "_all", orderable: false }],
            columns: [
                { data: "seq", className: "detail fit", visible: true },
                { data: "starred",
                  name: "starred",
                  className: "fit not-draggable",
                  render: function(starred, type) {
                      if (type == "display") {
                          return "<img class='starSong' src='" + (starred ? ratingOnImage : ratingOffImage) + "' style='height:18px;' alt='' title=''>";
                      }
                      return starred ? "onlystarred" : "unstarred";
                  }
                },
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
                  name: "addNext",
                  className: "fit not-draggable",
                  defaultContent: "<img class='addSongNext' src=\"<spring:theme code='addNextImage'/>\" style='height:18px;' alt=\"<fmt:message key='main.addnext'/>\" title=\"<fmt:message key='main.addnext'/>\">"
                },
                { data: "title",
                  className: "detail songTitle truncate",
                  render: function(title, type, row) {
                      if (type == "display") {
                          return $("<span>").attr("title", title).attr("alt", title).text(title)[0].outerHTML;
                      }
                      return title;
                  }
                },
                { data: "album",
                  className: "detail truncate",
                  render: function(album, type, row) {
                      if (type == "display") {
                          return $("<a>").attr("href", "main.view?id=" + row.id).attr("target", "main").attr("title", album).attr("alt", album).text(album)[0].outerHTML;
                      }
                      return album;
                  }
                },
                { data: "artist",
                  className: "detail truncate",
                  render: function(artist, type, row) {
                      if (type == "display") {
                          return $("<span>").attr("title", artist).attr("alt", artist).text(artist)[0].outerHTML;
                      }
                      return artist;
                  }
                },
                { data: "durationAsString", className: "detail fit rightalign" }
            ]
        } );

        $("#artistTopSongs tbody").on( "click", ".starSong", function () {
            toggleStarTopSong(artistTopSongsTable.row( $(this).parents('tr') ).index());
        } );
        $("#artistTopSongs tbody").on( "click", ".playSong", function () {
            playTopSong(artistTopSongsTable.row( $(this).parents('tr') ).index());
        } );
        $("#artistTopSongs tbody").on( "click", ".addSongLast", function () {
            addTopSong(artistTopSongsTable.row( $(this).parents('tr') ).index());
        } );
        $("#artistTopSongs tbody").on( "click", ".addSongNext", function () {
            addNextTopSong(artistTopSongsTable.row( $(this).parents('tr') ).index());
        } );

        $("#topSongsHeader").html("<h2><fmt:message key='main.topsongs'/></h2>");
        $("#artistTopSongs_wrapper").hide();

        top.StompClient.subscribe("artistMain.jsp", {
            "/user/queue/artist/info": function(msg) {
                loadArtistInfoCallback(JSON.parse(msg.body));
            }
        }, loadArtistInfo);
        </c:if>
    }

    <c:if test="${model.showArtistInfo}">
    function loadArtistInfo() {
        top.StompClient.send("/app/artist/info", JSON.stringify({mediaFileId: ${model.dir.id}, maxSimilarArtists: 8, maxTopSongs: 50}));
    }

    function loadArtistInfoCallback(artistInfo) {
        if (artistInfo.similarArtists.length > 0) {

            var html = "";
            for (var i = 0; i < artistInfo.similarArtists.length; i++) {
                html += "<a href='main.view?id=" + artistInfo.similarArtists[i].mediaFileId + "' target='main'>" +
                        escapeHtml(artistInfo.similarArtists[i].artistName) + "</a>";
                if (i < artistInfo.similarArtists.length - 1) {
                    html += " <span class='similar-artist-divider'>|</span> ";
                }
            }
            $("#similarArtists").append(html);
            $("#similarArtists").show();
            $("#similarArtistsTitle").show();
            $("#similarArtistsRadio").show();
            $("#artistInfoTable").show();
        }

        if (artistInfo.artistBio && artistInfo.artistBio.biography) {
            $("#artistBio").append(artistInfo.artistBio.biography);
            if (artistInfo.artistBio.largeImageUrl) {
                $("#artistImage").attr({
                      "src": artistInfo.artistBio.largeImageUrl,
                      "class": "fancy"
                });
                $("#artistImageZoom").attr("href", artistInfo.artistBio.largeImageUrl);
                $("#artistImage").show();
                $("#artistInfoTable").show();
            }
        }

        this.topSongs = artistInfo.topSongs;

        if (topSongs.length > 0) {
            $("#playTopSongs").show();
            $("#artistTopSongs_wrapper").show();
        }

        artistTopSongsTable.ajax.reload().columns.adjust();
    }
    </c:if>

    function toggleStarTopSong(index) {
        topSongs[index].starred = !topSongs[index].starred;

        if (topSongs[index].starred) {
            top.StompClient.send("/app/rate/mediafile/star", topSongs[index].id);
        } else {
            top.StompClient.send("/app/rate/mediafile/unstar", topSongs[index].id);
        }
        artistTopSongsTable.cell(index, "starred:name").invalidate().draw();
    }

    function toggleStar(mediaFileId, imageId) {
        if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOnImage"/>") != -1) {
            $(imageId).attr("src", "<spring:theme code="ratingOffImage"/>");
            top.StompClient.send("/app/rate/mediafile/unstar", mediaFileId);
        }
        else if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOffImage"/>") != -1) {
            $(imageId).attr("src", "<spring:theme code="ratingOnImage"/>");
            top.StompClient.send("/app/rate/mediafile/star", mediaFileId);
        }
    }
    function playAll() {
        top.playQueue.onPlay(${model.dir.id});
    }
    function playRandom() {
        top.playQueue.onPlayRandom(${model.dir.id}, 40);
    }
    function addAll() {
        top.playQueue.onAdd(${model.dir.id});
    }
    function playSimilar() {
        top.playQueue.onPlaySimilar(${model.dir.id}, 50);
    }
    function playAllTopSongs() {
        top.playQueue.onPlayTopSong(${model.dir.id});
    }
    function playTopSong(index) {
        top.playQueue.onPlayTopSong(${model.dir.id}, index);
    }
    function addTopSong(index) {
        top.playQueue.onAdd(topSongs[index].id);
        $().toastmessage('showSuccessToast', '<fmt:message key="main.addlast.toast"/>')
    }
    function addNextTopSong(index) {
        top.playQueue.onAddNext(topSongs[index].id);
        $().toastmessage('showSuccessToast', '<fmt:message key="main.addnext.toast"/>')
    }
    function showAllAlbums() {
        window.location.href = updateQueryStringParameter(window.location.href, "showAll", "1");
    }
    function toggleComment() {
        $("#commentForm").toggle();
        $("#comment").toggle();
    }
</script>

</head><body class="mainframe bgcolor1" onload="init();">

<div style="float:left">
    <h1>
        <img id="starImage" style="height:18px" src="<spring:theme code='${not empty model.dir.starredDate ? \'ratingOnImage\' : \'ratingOffImage\'}'/>"
             onclick="toggleStar(${model.dir.id}, '#starImage'); return false;" style="cursor:pointer;height:18px;" alt="">

        <span style="vertical-align: middle">
            <c:forEach items="${model.ancestors}" var="ancestor">
                <sub:url value="main.view" var="ancestorUrl">
                    <sub:param name="id" value="${ancestor.id}"/>
                </sub:url>
                <a href="${ancestorUrl}">${fn:escapeXml(ancestor.name)}</a> &raquo;
            </c:forEach>
            ${fn:escapeXml(model.dir.name)}
        </span>
    </h1>

    <c:if test="${not model.partyMode}">
        <h2>
            <c:if test="${model.navigateUpAllowed}">
                <sub:url value="main.view" var="upUrl">
                    <sub:param name="id" value="${model.parent.id}"/>
                </sub:url>
                <span class="header"><a href="${upUrl}"><fmt:message key="main.up"/></a></span>
                <c:set var="needSep" value="true"/>
            </c:if>

            <c:if test="${model.user.streamRole}">
                <c:if test="${needSep}">|</c:if>
                <span class="header"><a href="javascript:playAll()"><fmt:message key="main.playall"/></a></span> |
                <span class="header"><a href="javascript:playRandom(0)"><fmt:message key="main.playrandom"/></a></span> |
                <span class="header"><a href="javascript:addAll(0)"><fmt:message key="main.addall"/></a></span>
                <c:set var="needSep" value="true"/>
            </c:if>

            <c:if test="${model.user.commentRole}">
                <c:if test="${needSep}">|</c:if>
                <span class="header"><a href="javascript:toggleComment()"><fmt:message key="main.comment"/></a></span>
            </c:if>
        </h2>
    </c:if>
</div>

<%@ include file="viewSelector.jsp" %>
<div style="clear:both"></div>

<div id="comment" class="albumComment">${model.dir.comment}</div>

<div id="commentForm" style="display:none">
    <form method="post" action="setMusicFileInfo.view">
        <sec:csrfInput />
        <input type="hidden" name="action" value="comment">
        <input type="hidden" name="id" value="${model.dir.id}">
        <textarea name="comment" rows="6" cols="70">${model.dir.comment}</textarea>
        <input type="submit" value="<fmt:message key='common.save'/>">
    </form>
</div>

<c:choose>
    <c:when test="${model.viewAsList}">
        <table class="music indent">
            <c:forEach items="${model.subDirs}" var="subDir">
                <tr>
                    <c:import url="playButtons.jsp">
                        <c:param name="id" value="${subDir.id}"/>
                        <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                        <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                        <c:param name="asTable" value="true"/>
                    </c:import>
                    <td class="truncate"><a href="main.view?id=${subDir.id}" title="${fn:escapeXml(subDir.name)}">${fn:escapeXml(subDir.name)}</a></td>
                    <td class="fit rightalign detail">${subDir.year}</td>
                </tr>
            </c:forEach>
        </table>
        <c:if test="${model.thereIsMore}">
            <input id="showAllButton" class="albumOverflowButton" type="button" value="<fmt:message key='main.showall'/>" onclick="showAllAlbums()">
        </c:if>
    </c:when>

    <c:otherwise>
        <table class="music indent">
            <c:forEach items="${model.subDirs}" var="subDir">
                <c:if test="${not subDir.album}">
                    <tr>
                        <c:import url="playButtons.jsp">
                            <c:param name="id" value="${subDir.id}"/>
                            <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>
                        <td class="truncate"><a href="main.view?id=${subDir.id}" title="${fn:escapeXml(subDir.name)}">${fn:escapeXml(subDir.name)}</a></td>
                        <td class="fit rightalign detail">${subDir.year}</td>
                    </tr>
                </c:if>
            </c:forEach>
        </table>

        <div style="float: left;padding-top: 1.5em">
            <c:set var="albumCount" value="0"/>
            <c:forEach items="${model.subDirs}" var="subDir" varStatus="loopStatus">
                <c:if test="${subDir.album}">
                    <c:set var="albumCount" value="${albumCount + 1}"/>
                    <div class="albumThumb">
                        <c:import url="coverArt.jsp">
                            <c:param name="albumId" value="${subDir.id}"/>
                            <c:param name="caption1" value="${fn:escapeXml(subDir.name)}"/>
                            <c:param name="caption2" value="${subDir.year}"/>
                            <c:param name="captionCount" value="2"/>
                            <c:param name="coverArtSize" value="${model.coverArtSizeMedium}"/>
                            <c:param name="showLink" value="true"/>
                            <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
                            <c:param name="hideOverflow" value="true"/>
                        </c:import>
                    </div>
                </c:if>
            </c:forEach>
            <c:if test="${model.thereIsMore}">
                <input id="showAllButton" class="albumOverflowButton" type="button" value="<fmt:message key='main.showall'/>" onclick="showAllAlbums()">
            </c:if>
        </div>
    </c:otherwise>
</c:choose>

<table id="artistInfoTable" style="padding:2em;clear:both;display:none" class="bgcolor2 dropshadow">
    <tr>
        <td rowspan="5" style="vertical-align: top">
            <a id="artistImageZoom" rel="zoom" href="void">
                <img id="artistImage" class="dropshadow" alt="" style="margin-right:2em; display:none; max-width:300px; max-height:300px">
            </a>
        </td>
        <td style="text-align:center"><h2>${fn:escapeXml(model.dir.name)}</h2></td>
    </tr>
    <tr>
        <td id="artistBio" style="padding-bottom: 0.5em"></td>
    </tr>
    <tr><td style="padding-bottom: 0.5em">
        <span id="similarArtistsTitle" style="padding-right: 0.5em; display: none"><fmt:message key="main.similarartists"/>:</span>
        <span id="similarArtists"></span>
    </td></tr>
    <tr><td style="text-align:center">
        <input id="similarArtistsRadio" style="display:none;margin-top:1em;margin-right:0.3em;cursor:pointer" type="button" value="<fmt:message key='main.startradio'/>" onclick="playSimilar()">
        <input id="playTopSongs" style="display:none;margin-top:1em;margin-left:0.3em;cursor:pointer" type="button" value="<fmt:message key='main.playtopsongs'/>" onclick="playAllTopSongs()">
    </td></tr>
    <tr><td style="height: 100%"></td></tr>
</table>

<table id="artistTopSongs" class="music indent hover nowrap stripe compact hide-table-header" style="width: 100%;">
</table>

</body>
</html>
