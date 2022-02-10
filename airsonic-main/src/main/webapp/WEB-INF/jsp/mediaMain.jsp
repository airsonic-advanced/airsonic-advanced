<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="table.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/jquery.fancyzoom.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
    <style type="text/css">
        #topSongsHeader, #filesHeader, #subDirsHeader {
            display: inline-block;
            border: dotted;
            border-width: thin;
            padding: 0.25em 1em 0.25em 1em;
        }
        .headerNotSelected, .starSong, .playSong, .addSongNext, .addSongLast {
            cursor: pointer;
        }
        #subDirsTable_wrapper {
            flex-grow: 1;
            flex-shrink: 1;
        }
        .duration {
            position: absolute;
            bottom: 3px;
            right: 3px;
            color: #d3d3d3;
            background-color: black;
            opacity: 0.8;
            padding-right:3px;
            padding-left:3px;
        }
    </style>
<script type="text/javascript" language="javascript">
    var mediaDir = {files: [], subDirs: []};
    var filesTable = null;
    var subDirsTable = null;
    var subDirsTableData = [];
    var thumbsData = [];
    var viewAsList = ${model.viewAsList};
    var topSongs = [];
    var artistTopSongsTable = null;
    var initialIds = ${model.initialIdsJSON};
    var initialPaths = ${model.initialPathsJSON};

    function getMediaDirectory(mediaFileId) {
        getMediaDirectories([mediaFileId]);
    }

    function getMediaDirectories(ids, paths) {
        top.StompClient.send("/app/mediafile/directory/get", JSON.stringify({ids: ids, paths: paths}));
    }

    function getMediaDirectoryCallback(mediaDirObj) {
        if (mediaDirObj.contentType == 'notFound') {
            window.location = 'notFound.view?';
            return;
        }
        if (mediaDirObj.contentType == 'home') {
            window.location = 'home.view?';
            return;
        }
        if (mediaDirObj.contentType == 'accessDenied') {
            window.location = 'accessDenied.view?';
            return;
        }

        mediaDir = mediaDirObj;

        if (!mediaDir.subDirs) {
            mediaDir.subDirs = [];
        }
        if (!mediaDir.siblingAlbums) {
            mediaDir.siblingAlbums = [];
        }

        generateMediaDirThumb();
        refreshViewDependentComponents();

      <c:if test="${model.showArtistInfo}">
        loadArtistInfo();
      </c:if>

        // directory star
        updateStarImage();

        // ancestors
        populateAncestors();

        // comments
        updateComments();

        // search components
        if (mediaDir.album == null && mediaDir.artist == null) {
            $('.external-search').hide();
        } else {
            $('.external-search').show();
        }

        if (mediaDir.musicBrainzReleaseId == null) {
            $('.external-search.musicbrainz').hide();
        } else {
            $('.external-search.musicbrainz').show();
        }

        updatePlayCountData();

        getRatings();
    }

    function refreshViewDependentComponents() {
        // show only appropriate components
        $(".pagetype-dependent:not(.type-" + mediaDir.contentType + ")").hide();
        $(".pagetype-dependent.type-" + mediaDir.contentType).show();
        $('#subDirsTable_wrapper').show();

        if (mediaDir.contentType == 'artist') {
            $('#filesTable_wrapper').hide();
            $('#artistInfoTable').addClass('bgcolor2 dropshadow');
        } else {
             $('#artistInfoTable').removeClass('bgcolor2 dropshadow');
             $('#filesTable_wrapper').show();
        }

        // mix subdirectories and siblings, not an issue for non-albums because sibs are always empty
        subDirsTableData = mediaDir.subDirs.concat(mediaDir.siblingAlbums);

        // separate albums and non-albums if not list view
        thumbsData = [];
        if (!viewAsList) {
            thumbsData = subDirsTableData.filter(x => x.entryType == 'ALBUM');
            subDirsTableData = subDirsTableData.filter(x => x.entryType != 'ALBUM');

            if (mediaDir.contentType == 'video') {
                thumbsData = thumbsData.concat(mediaDir.files);
                $('#filesTable_wrapper, #moreActions').hide();
            }
        }

        if (mediaDir.files.length == 0) {
            $('#filesTable_wrapper, #moreActions').hide();
        }

        if (subDirsTableData.length == 0) {
            $('#subDirsTable_wrapper').hide();
        }

        // subdirs heading
        subDirsHeading();

        filesTable.ajax.reload().columns.adjust();
        subDirsTable.ajax.reload().columns.adjust();

        generateThumbs();
    }

    function generateMediaDirThumb() {
        var urlBase = "<c:url value='/coverArtJsp.view'/>";
        $.get(urlBase + '?coverArtSize=${model.coverArtSizeLarge}&showZoom=true&showChange=${model.user.coverArtRole}&albumId=' + mediaDir.id, data => {
            $('#mediaDirThumb').html(data);
        });
    }

    function generateThumbs() {
        var urlBase = "<c:url value='/coverArtJsp.view'/>";
        $('#thumbs').html('');

        thumbsData.filter(t => t.entryType != 'VIDEO').forEach((t,i) => {
            //append container first to keep order intact when async callback happens
            $('#thumbs').append('<div class="albumThumb"></div>');
            $.get(urlBase + '?hideOverflow=true&showLink=true&appearAfter=' + (i * 30) + '&coverArtSize=${model.coverArtSizeMedium}&captionCount=2&caption2=' + (t.year ? t.year : '') + '&caption1=' + encodeURIComponent(t.title) +'&albumId=' + t.id, data => {
                $('#thumbs .albumThumb:nth-child(' + (i + 1) + ')').append(data);
            });
        });

        var videoUrlBase = "<c:url value='/videoPlayer.view'/>";
        var covertArtUrlBase = "<c:url value='/coverArt.view'/>";
        thumbsData.filter(t => t.entryType == 'VIDEO').forEach(t => {
            var vidUrl = videoUrlBase + '?id=' + t.id;
            var vid = '' +
            '<div class="albumThumb">' +
              '<div class="coverart dropshadow" style="width:213px">' +
                '<div style="position:relative">' +
                  '<div>' +
                    '<a href="' + vidUrl + '">' +
                      '<img src="' + covertArtUrlBase + '?size=120&id=' + t.id + '" height="120" width="213" alt="" onmouseover="startPreview(this, ' + t.id + ', ' + t.duration + ')" onmouseout="stopPreview()" />' +
                    '</a>' +
                  '</div>' +
                  '<div class="detail duration">' + t.durationString + '</div>' +
                '</div>' +
                $('<div>').addClass('caption1').attr('title', t.title).append(
                  $('<a>').attr('href', vidUrl).attr('title', t.title).text(t.title))[0].outerHTML +
              '</div>' +
            '</div>';

            $('#thumbs').append(vid);
        });
    }

    function getRatings() {
        var urlBase = "<c:url value='/ratingJsp.view'/>";
        if (mediaDir.averageRating && mediaDir.averageRating > 0) {
            $.get(urlBase + '?readonly=true&rating=' + mediaDir.averageRating, data => {
                $('#avgRating').html('&nbsp;&nbsp;'+data).show();
            });
        } else {
            $('#avgRating').html('').hide();
        }

        $.get(urlBase + '?readonly=false&id=' + mediaDir.id + '&rating=' + mediaDir.userRating, data => {
            $('#userRating').html(data);
        });
    }

    function updatePlayCountData() {
        var playcount = '';
        if (mediaDir.playCount) {
            playcount += '<fmt:message key="main.playcount"><fmt:param value="%v%"/></fmt:message>'.replace('%v%', mediaDir.playCount);
        }
        if (mediaDir.lastPlayed) {
            playcount += ' <fmt:message key="main.lastplayed"><fmt:param value="%v%" /></fmt:message>'.replace('%v%', new Date(mediaDir.lastPlayed));
        }
        $('#playCountData').text(playcount);
    }

    function updateComments() {
        $('#comment').text(mediaDir.comment);
        $('#commentForm textarea').text(mediaDir.comment);
        $('#commentForm input[name="id"]').val(mediaDir.id);
    }

    function subDirsHeading() {
        var subDirHeading = 'Subdirectories';

        if (mediaDir.contentType == 'album' && viewAsList) {
            subDirHeading = 'Subdirectories and Sibling Albums';
        }
        $("#subDirsHeader").html("<h2 style='margin: 0'>" + subDirHeading + "</h2>");
    }

    function init() {
        $("a.fancy").fancyZoom({
            minBorder: 30
        });

        var dialogSize = getJQueryUiDialogPlaylistSize("mediaMain");
        $("#dialog-select-playlist").dialog({resizable: true, height: dialogSize.height, width: dialogSize.width, autoOpen: false,
            buttons: {
                "<fmt:message key="common.cancel"/>": function() {
                    $(this).dialog("close");
                }
            },
            resizeStop: function (event, ui) { setJQueryUiDialogPlaylistSize("mediaMain", ui.size) }
        });

        var ratingOnImage = "<spring:theme code='ratingOnImage'/>";
        var ratingOffImage = "<spring:theme code='ratingOffImage'/>";

        /** FILES **/
        filesTable = $("#filesTable").DataTable( {
            deferRender: true,
            createdRow(row, data, dataIndex, cells) {
                var rowNode = $(row);
                if (rowNode.hasClass("selected")) {
                    rowNode.find(".songIndex input").prop("checked", true);
                }
            },
            colReorder: true,
            stateSave: true,
            stateDuration: 60 * 60 * 24 * 365,
            ordering: true,
            order: [],
            //orderFixed: [ 0, 'asc' ],
            orderMulti: true,
            pageLength: ${model.initialPaginationSizeFiles},
          <c:set var="paginationaddition" value="${fn:contains(' 10 20 50 100 -1', ' '.concat(model.initialPaginationSizeFiles)) ? '' : ', '.concat(model.initialPaginationSizeFiles)}" />
            lengthMenu: [[10, 20, 50, 100, -1 ${paginationaddition}], [10, 20, 50, 100, "All" ${paginationaddition}]],
            processing: true,
            autoWidth: true,
            scrollCollapse: true,
            dom: "<'#filesHeader'><'tableSpacer'>lfrtip",
            select: {
                style: "multi",
                selector: ".songIndex"
            },
            ajax: function(ajaxData, callback) {
                for ( var i=0, len=mediaDir.files.length ; i<len ; i++ ) {
                  mediaDir.files[i].seq = i;
                }
                callback({data: mediaDir.files});
            },
            stripeClasses: ["bgcolor2", "bgcolor1"],
            columnDefs: [{ targets: "_all", orderable: true }],
            columns: [
                { data: "seq", className: "detail fit", visible: true },
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
                { data: null,
                  searchable: false,
                  name: "play",
                  visible: ${model.user.streamRole and not model.partyMode},
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='playSong' src=\"<spring:theme code='playImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.play'/>\" title=\"<fmt:message key='common.play'/>\">"
                },
                { data: "entryType",
                  searchable: false,
                  name: "addLast",
                  visible: ${model.user.streamRole and not model.partyMode},
                  className: "fit not-draggable centeralign",
                  render: function(entryType, type, row) {
                      if (type == "display" && entryType != "VIDEO" && entryType != "DIRECTORY" && entryType != "ALBUM") {
                          return "<img class='addSongLast' src=\"<spring:theme code='addImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.add'/>\" title=\"<fmt:message key='common.add'/>\">";
                      }
                      return "";
                  }
                },
                { data: "entryType",
                  searchable: false,
                  name: "addNext",
                  visible: ${model.user.streamRole and not model.partyMode},
                  className: "fit not-draggable centeralign",
                  render: function(entryType, type, row) {
                      if (type == "display" && entryType != "VIDEO" && entryType != "DIRECTORY" && entryType != "ALBUM") {
                          return "<img class='addSongNext' src=\"<spring:theme code='addNextImage'/>\" style='height:18px;' alt=\"<fmt:message key='main.addnext'/>\" title=\"<fmt:message key='main.addnext'/>\">";
                      }
                      return "";
                  }
                },
                { data: null,
                  searchable: false,
                  name: "songcheckbox",
                  className: "fit not-draggable songIndex centeralign",
                  defaultContent: "<input type='checkbox'>"
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
                          return $("<span>", {title: album, alt: album, text: album})[0].outerHTML;
                      }
                      return album;
                  }
                },
                { data: "artist",
                  className: "detail truncate",
                  visible: ${model.visibility.artistVisible},
                  title: "<fmt:message key='personalsettings.artist'/>",
                  render: function(artist, type) {
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
                  render: function(genre, type) {
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
                { data: "entryType",
                  className: "detail fit rightalign truncate",
                  visible: ${model.visibility.entryTypeVisible},
                  title: "<fmt:message key='personalsettings.entrytype'/>",
                  render: function(entryType, type, row) {
                      if (type == "display" && entryType != null) {
                          var media = entryType.toLowerCase();
                          if (entryType == 'VIDEO') {
                              media += " " + row.dimensions
                          }
                          return $("<span>", {title: media, alt: media, text: media})[0].outerHTML;
                      }
                      return entryType;
                  }
                }
            ]
        } );

        filesTable.on( 'select', function ( e, dt, type, indexes ) {
             filesTable.cells( indexes, "songcheckbox:name" ).nodes().to$().find("input").prop("checked", true);
        } );
        filesTable.on( 'deselect', function ( e, dt, type, indexes ) {
             filesTable.cells( indexes, "songcheckbox:name" ).nodes().to$().find("input").prop("checked", false);
        } );
        $("#filesTable tbody").on( "click", ".starSong", function () {
            onToggleStar(filesTable.row( $(this).parents('tr') ));
        } );
        $("#filesTable tbody").on( "click", ".playSong", function () {
            onPlay(filesTable.row( $(this).parents('tr') ));
        } );
        $("#filesTable tbody").on( "click", ".addSongLast", function () {
            onAdd(filesTable.row( $(this).parents('tr') ));
        } );
        $("#filesTable tbody").on( "click", ".addSongNext", function () {
            onAddNext(filesTable.row( $(this).parents('tr') ));
        } );

        $("#filesHeader").html("<h2 style='margin: 0'>Files</h2>");

        /** SUBDIRS **/
        subDirsTable = $("#subDirsTable").DataTable( {
            deferRender: true,
            colReorder: true,
            stateSave: true,
            stateDuration: 60 * 60 * 24 * 365,
            ordering: true,
            order: [],
            orderFixed: [ 0, 'asc' ],
            orderMulti: false,
            pageLength: ${model.initialPaginationSizeFolders},
          <c:set var="paginationaddition" value="${fn:contains(' 10 20 50 100 -1', ' '.concat(model.initialPaginationSizeFolders)) ? '' : ', '.concat(model.initialPaginationSizeFolders)}" />
            lengthMenu: [[10, 20, 50, 100, -1 ${paginationaddition}], [10, 20, 50, 100, "All" ${paginationaddition}]],
            processing: true,
            autoWidth: true,
            scrollCollapse: true,
            //scrollY: "60vh",
            dom: "<'#subDirsHeader'><'tableSpacer'>lfrtip",
            ajax: function(ajaxData, callback) {
                for ( var i=0, len=subDirsTableData.length ; i<len ; i++ ) {
                    subDirsTableData[i].seq = i;
                }
                callback({data: subDirsTableData});
            },
            stripeClasses: ["bgcolor2", "bgcolor1"],
            columnDefs: [{ targets: "_all", orderable: false }],
            columns: [
                { data: "seq", className: "detail fit", visible: true },
                { data: null,
                  searchable: false,
                  name: "play",
                  visible: ${model.user.streamRole and not model.partyMode},
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='playSong' src=\"<spring:theme code='playImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.play'/>\" title=\"<fmt:message key='common.play'/>\">"
                },
                { data: "entryType",
                  searchable: false,
                  name: "addLast",
                  visible: ${model.user.streamRole and not model.partyMode},
                  className: "fit not-draggable centeralign",
                  render: function(entryType, type, row) {
                      if (type == "display") {
                          return "<img class='addSongLast' src=\"<spring:theme code='addImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.add'/>\" title=\"<fmt:message key='common.add'/>\">";
                      }
                      return "";
                  }
                },
                { data: "entryType",
                  searchable: false,
                  name: "addNext",
                  visible: ${model.user.streamRole and not model.partyMode},
                  className: "fit not-draggable centeralign",
                  render: function(entryType, type, row) {
                      if (type == "display") {
                          return "<img class='addSongNext' src=\"<spring:theme code='addNextImage'/>\" style='height:18px;' alt=\"<fmt:message key='main.addnext'/>\" title=\"<fmt:message key='main.addnext'/>\">";
                      }
                      return "";
                  }
                },
                { data: "title",
                  className: "detail songTitle truncate",
                  render: function(title, type, row) {
                      if (type == "display" && title != null) {
                          return $("<a>", {title: title, alt: title, text: title, href: "#"})[0].outerHTML;
                      }
                      return title;
                  }
                },
                { data: "year", className: "detail fit rightalign" }
            ]
        } );

        $("#subDirsTable tbody").on( "click", ".songTitle a", function () {
            getMediaDirectory(subDirsTable.row( $(this).parents('tr') ).data().id);
        } );
        $("#subDirsTable tbody").on( "click", ".playSong", function () {
            onPlay(subDirsTable.row( $(this).parents('tr') ));
        } );
        $("#subDirsTable tbody").on( "click", ".addSongLast", function () {
            onAdd(subDirsTable.row( $(this).parents('tr') ));
        } );
        $("#subDirsTable tbody").on( "click", ".addSongNext", function () {
            onAddNext(subDirsTable.row( $(this).parents('tr') ));
        } );

        /** ARTIST TOP SONGS **/
        artistTopSongsTable = $("#artistTopSongsTable").DataTable( {
            deferRender: true,
            colReorder: true,
            stateSave: true,
            stateDuration: 60 * 60 * 24 * 365,
            ordering: true,
            order: [],
            orderFixed: [ 0, 'asc' ],
            orderMulti: false,
            pageLength: ${model.initialPaginationSizeFolders},
          <c:set var="paginationaddition" value="${fn:contains(' 10 20 50 100 -1', ' '.concat(model.initialPaginationSizeFolders)) ? '' : ', '.concat(model.initialPaginationSizeFolders)}" />
            lengthMenu: [[10, 20, 50, 100, -1 ${paginationaddition}], [10, 20, 50, 100, "All" ${paginationaddition}]],
            processing: true,
            autoWidth: true,
            scrollCollapse: true,
            scrollY: "60vh",
            dom: "<'#topSongsHeader'><'tableSpacer'>lfrtip",
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
                  className: "fit not-draggable centeralign",
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
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='playSong' src=\"<spring:theme code='playImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.play'/>\" title=\"<fmt:message key='common.play'/>\">"
                },
                { data: null,
                  searchable: false,
                  name: "addLast",
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='addSongLast' src=\"<spring:theme code='addImage'/>\" style='height:18px;' alt=\"<fmt:message key='common.add'/>\" title=\"<fmt:message key='common.add'/>\">"
                },
                { data: null,
                  searchable: false,
                  name: "addNext",
                  className: "fit not-draggable centeralign",
                  defaultContent: "<img class='addSongNext' src=\"<spring:theme code='addNextImage'/>\" style='height:18px;' alt=\"<fmt:message key='main.addnext'/>\" title=\"<fmt:message key='main.addnext'/>\">"
                },
                { data: "title",
                  className: "detail songTitle truncate",
                  render: function(title, type, row) {
                      if (type == "display" && title != null) {
                          return $("<span>", {title: title, alt: title, text: title})[0].outerHTML;
                      }
                      return title;
                  }
                },
                { data: "album",
                  className: "detail truncate",
                  render: function(album, type, row) {
                      if (type == "display" && album != null) {
                          return $("<a>", {title: album, alt: album, text: album, target: "main"}).attr("href", "main.view?id=" + row.id)[0].outerHTML;
                      }
                      return album;
                  }
                },
                { data: "artist",
                  className: "detail truncate",
                  render: function(artist, type, row) {
                      if (type == "display" && artist != null) {
                          return $("<span>", {title: artist, alt: artist, text: artist})[0].outerHTML;
                      }
                      return artist;
                  }
                },
                { data: "duration",
                  className: "detail fit rightalign",
                  render: function(data, type, row) {
                      if (type == "display" && data != null) {
                          return formatDuration(Math.round(data));
                      }
                      return data;
                  }
                }
            ]
        } );

        $("#artistTopSongsTable tbody").on( "click", ".starSong", function () {
            onToggleStar(artistTopSongsTable.row( $(this).parents('tr') ));
        } );
        $("#artistTopSongsTable tbody").on( "click", ".playSong", function () {
            playTopSong(artistTopSongsTable.row( $(this).parents('tr') ).index());
        } );
        $("#artistTopSongsTable tbody").on( "click", ".addSongLast", function () {
            onAdd(artistTopSongsTable.row( $(this).parents('tr') ));
        } );
        $("#artistTopSongsTable tbody").on( "click", ".addSongNext", function () {
            onAddNext(artistTopSongsTable.row( $(this).parents('tr') ));
        } );

        $("#topSongsHeader").html("<h2><fmt:message key='main.topsongs'/></h2>");
        $("#artistTopSongsTable_wrapper").hide();

        viewSelectorRefresh();

        top.StompClient.subscribe("mediaMain.jsp", {
            '/user/queue/mediafile/directory/get': function(msg) {
                getMediaDirectoryCallback(JSON.parse(msg.body));
            },
            '/user/queue/playlists/writable': function(msg) {
                playlistSelectionCallback(JSON.parse(msg.body));
            },
            '/user/queue/playlists/files/append': function(msg) {
                $().toastmessage("showSuccessToast", "<fmt:message key='playlist.toast.appendtoplaylist'/>");
            },
            "/user/queue/settings/viewAsList": function(msg) {
                viewChangedCallback(JSON.parse(msg.body));
            }
          <c:if test="${model.showArtistInfo}">
            ,"/user/queue/artist/info": function(msg) {
                loadArtistInfoCallback(JSON.parse(msg.body));
            }
          </c:if>
        }, function() {
            if (initialIds.length != 0 || initialPaths.length != 0) {
                getMediaDirectories(initialIds, initialPaths);
            }
        });
    }

  <c:if test="${model.showArtistInfo}">
    function loadArtistInfo() {
        top.StompClient.send("/app/artist/info", JSON.stringify({mediaFileId: mediaDir.id, maxSimilarArtists: 8, maxTopSongs: 100}));
    }

    function loadArtistInfoCallback(artistInfo) {
        $('#artistTitle h2').text(mediaDir.title);
        if (artistInfo.similarArtists.length > 0) {
            var html = "";
            for (var i = 0; i < artistInfo.similarArtists.length; i++) {
                html += $('<a>').attr('href', '#').attr('onclick', 'getMediaDirectory(' + artistInfo.similarArtists[i].mediaFileId + ')').text(artistInfo.similarArtists[i].artistName)[0].outerHTML;

                if (i < artistInfo.similarArtists.length - 1) {
                    html += " <span class='similar-artist-divider'>|</span> ";
                }
            }
            $("#similarArtists").html(html);
        } else {
            $('#similarArtistsContainer').hide();
        }

        if (artistInfo.artistBio && artistInfo.artistBio.biography) {
            $("#artistBio").html(artistInfo.artistBio.biography);
            if (artistInfo.artistBio.largeImageUrl) {
                $("#artistImage").attr({
                      "src": artistInfo.artistBio.largeImageUrl,
                      "class": "fancy"
                });
                $("#artistImageZoom").attr("href", artistInfo.artistBio.largeImageUrl);
                $("#artistImageZoom").show();
            } else {
                $("#artistImage").attr("href", '');
                $("#artistImageZoom").attr("href", '');
                $("#artistImageZoom").hide();
            }
        }

        this.topSongs = artistInfo.topSongs;

        if (topSongs.length > 0 && mediaDir.contentType == 'artist') {
            $("#artistTopSongsTable_wrapper").show();
            $("#playTopSongs").show();
        } else {
            $("#playTopSongs").hide();
            $("#artistTopSongsTable_wrapper").hide();
        }

        artistTopSongsTable.ajax.reload().columns.adjust();

    }
  </c:if>

    function populateAncestors() {
        var ancestors = $("#ancestors").empty();
        for (var len = mediaDir.ancestors.length, i = len - 1; i >= 0; i--) {
            var ancestor = mediaDir.ancestors[i];
            ancestors.append($("<a>").attr("href", "#").attr("onclick", "getMediaDirectory(" + ancestor.id + ")").text(ancestor.title));
            ancestors.append(" » ");
        }
        ancestors.append(mediaDir.title);
    }

    function toggleMediaDirStar(status) {
        if (mediaDir.starred != status) {
            mediaDir.starred = status;
            if (mediaDir.starred) {
                top.StompClient.send("/app/rate/mediafile/star", JSON.stringify([mediaDir.id]));
            } else {
                top.StompClient.send("/app/rate/mediafile/unstar", JSON.stringify([mediaDir.id]));
            }

            updateStarImage();
        }
    }

    function updateStarImage() {
        if (mediaDir.starred) {
            $("#starImage").attr("src", "<spring:theme code='ratingOnImage'/>");
        } else {
            $("#starImage").attr("src", "<spring:theme code='ratingOffImage'/>");
        }
    }

    function onStar(table, indices, status) {
        var ids = table.rows(indices).data().map(data => {
            data.starred = status;
            table.cell(data.seq, "starred:name").invalidate();
            return data.id;
        }).toArray();

        if (status) {
            top.StompClient.send("/app/rate/mediafile/star", JSON.stringify(ids));
        } else {
            top.StompClient.send("/app/rate/mediafile/unstar", JSON.stringify(ids));
        }
    }
    function onToggleStar(row) {
        onStar(row.table(), [row.data().seq], !row.data().starred);
    }
    function onPlay(row) {
        var data = row.data();
        if (data.entryType == 'VIDEO') {
            var urlBase = "<c:url value='/videoPlayer.view'/>";
            var url = urlBase + "?id=" + data.id;
            top.main.location = url;
        } else {
            top.playQueue.onPlay(data.id);
        }
    }
    function onAdd(row) {
        top.playQueue.onAdd(row.data().id);
        $().toastmessage('showSuccessToast', '<fmt:message key="main.addlast.toast"/>')
    }
    function onAddNext(row) {
        top.playQueue.onAddNext(row.data().id);
        $().toastmessage('showSuccessToast', '<fmt:message key="main.addnext.toast"/>')
    }

    function playAll() {
        top.playQueue.onPlay(mediaDir.id);
    }

    function playRandom() {
        top.playQueue.onPlayRandom(mediaDir.id, 40);
    }

    function addAll() {
        top.playQueue.onAdd(mediaDir.id);
    }

    function playSimilar() {
        top.playQueue.onPlaySimilar(mediaDir.id, 50);
    }

    function toggleComment() {
        $("#commentForm").toggle();
        $("#comment").toggle();
    }

    /** Albums Only **/
    // actionSelected() is invoked when the users selects from the "More actions..." combo box.
    function actionSelected(id) {
        var selectedIndexes;
        if (id == "top") {
            return;
        } else if (id == "selectAll") {
            selectAll(true);
        } else if (id == "selectNone") {
            selectAll(false);
        } else if ((selectedIndexes = getSelectedIndexes()).length > 0 && id == "star") {
            onStar(filesTable, selectedIndexes, true);
        } else if (id == "unstar" && selectedIndexes.length > 0) {
            onStar(filesTable, selectedIndexes, false);
        } else if (id == "share" && selectedIndexes.length > 0) {
            location.href = "createShare.view?id=" + mediaDir.id  + "&" + querize(selectedIndexes, "i");
        } else if (id == "download" && selectedIndexes.length > 0) {
            location.href = "download.view?id=" + mediaDir.id  + "&" + querize(selectedIndexes, "i");
        } else if (id == "appendPlaylist" && selectedIndexes.length > 0) {
            onAppendPlaylist();
        }
        $("#moreActions").prop("selectedIndex", 0);
    }

    function getSelectedIndexes() {
        return filesTable.rows({ selected: true }).indexes().toArray();
    }

    function querize(arr, queryVar) {
        return arr.map(i => queryVar + "=" + i).join("&");
    }

    function selectAll(b) {
        if (b) {
            filesTable.rows().select();
        } else {
            filesTable.rows().deselect();
        }
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

        var mediaFileIds = filesTable.rows({selected:true}).data().map(function(d) { return d.id; }).toArray();

        top.StompClient.send("/app/playlists/files/append", JSON.stringify({id: playlistId, modifierIds: mediaFileIds}));
    }

    /** Artists Only **/
    function playAllTopSongs() {
        top.playQueue.onPlayTopSong(mediaDir.id);
    }
    function playTopSong(index) {
        top.playQueue.onPlayTopSong(mediaDir.id, index);
    }

    /** Videos Only **/
    var videoPreview = {
        image: null,
        id: null,
        duration: null,
        timer: null,
        offset: null,
        step: null,
        size: 120
    };

    function startPreview(img, id, duration) {
        stopPreview();
        videoPreview.image = $(img);
        videoPreview.step = Math.max(5, Math.round(duration / 50));
        videoPreview.offset = videoPreview.step;
        videoPreview.id = id;
        videoPreview.duration = duration;
        updatePreview();
        videoPreview.timer = window.setInterval(updatePreview, 1000);
    }

    function updatePreview() {
        videoPreview.image.attr("src", "coverArt.view?id=" + videoPreview.id + "&size=" + videoPreview.size + "&offset=" + videoPreview.offset);
        videoPreview.offset += videoPreview.step;
        if (videoPreview.offset > videoPreview.duration) {
            stopPreview();
        }
    }

    function stopPreview() {
        if (videoPreview.timer != null) {
            window.clearInterval(videoPreview.timer);
            videoPreview.timer = null;
        }
        if (videoPreview.image != null) {
            videoPreview.image.attr("src", "coverArt.view?id=" + videoPreview.id + "&size=" + videoPreview.size);
        }
    }

    function editTagsPage() {
        window.location = "editTags.view?id=" + mediaDir.id;
    }

    function downloadAll() {
        location.href = "download.view?id=" + mediaDir.id;
    }

    function shareAlbum() {
        location.href = "createShare.view?id=" + mediaDir.id;
    }

    function searchExternally(vendor) {
        var url = null;
        switch(vendor) {
            case "google":
                url = 'https://www.google.com/search?q=' + encodeURIComponent('"' + mediaDir.artist + '" "' + mediaDir.album + '"');
                break;
            case "wikipedia":
                url = 'https://en.wikipedia.org/wiki/Special:Search?go=Go&search=' + encodeURIComponent('"' + mediaDir.album + '"');
                break;
            case "allmusic":
                url = 'https://www.allmusic.com/search/albums/' + encodeURIComponent('"' + mediaDir.artist + '" "' + mediaDir.album + '"');
                break;
            case "lastfm":
                url = 'https://www.last.fm/search?type=album&q=' + encodeURIComponent('"' + mediaDir.artist + '" "' + mediaDir.album + '"');
                break;
            case "discogs":
                url = 'https://www.discogs.com/search/?type=release&q=' + encodeURIComponent('"' + mediaDir.artist + '" "' + mediaDir.album + '"');
                break;
            case "musicbrainz":
                url = mediaDir.musicBrainzReleaseId != null ? ('https://musicbrainz.org/release/' + mediaDir.musicBrainzReleaseId) : null;
                break;
        }

        if (url != null) {
            window.open(url, '_blank', 'noopener noreferrer');
        }
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
            refreshViewDependentComponents();
        }
    }
</script>
</head>
<body class="mainframe bgcolor1" onload="init();">
<div style="float:left">
    <h1>
        <img id="starImage" src="<spring:theme code='ratingOffImage'/>"
             onclick="toggleMediaDirStar(!mediaDir.starred); return false;" style="cursor:pointer;height:18px;" alt="">

        <span id="ancestors" style="vertical-align: middle"></span>

        <div id="avgRating" style="display: inline-block"></div>
    </h1>

    <c:if test="${not model.partyMode}">
        <h2>
            <c:if test="${model.user.streamRole}">
                <div class="pagetype-dependent type-album type-artist" style="display: inline-block;">
                    <span class="header"><a href="javascript:playAll()"><fmt:message key="main.playall"/></a></span> |
                    <span class="header"><a href="javascript:playRandom()"><fmt:message key="main.playrandom"/></a></span> |
                    <span class="header"><a href="javascript:addAll()"><fmt:message key="main.addall"/></a></span> |
                </div>
            </c:if>

            <c:if test="${model.user.downloadRole}">
                <div class="pagetype-dependent type-album" style="display: inline-block;">
                    <span class="header"><a href="javascript:downloadAll()"><fmt:message key="main.downloadall"/></a></span> |
                </div>
            </c:if>

            <c:if test="${model.user.coverArtRole}">
                <div class="pagetype-dependent type-album" style="display: inline-block;">
                    <span class="header"><a href="javascript:editTagsPage()"><fmt:message key="main.tags"/></a></span> |
                </div>
            </c:if>

            <c:if test="${model.user.commentRole}">
                <span class="header"><a href="javascript:toggleComment()"><fmt:message key="main.comment"/></a></span> |
            </c:if>
        </h2>
    </c:if>
</div>

<div style="float:right;padding-right:1em">
    <img id="viewAsList" src="<spring:theme code='viewAsListImage'/>" alt="" class="headerSelected" style="margin-right:8px" onclick="setViewAsList(true)"/>
    <img id="viewAsGrid" src="<spring:theme code='viewAsGridImage'/>" alt="" class="headerNotSelected" onclick="setViewAsList(false)"/>
</div>
<div style="clear:both"></div>

<div class="detail pagetype-dependent type-album">
    <c:if test="${model.user.commentRole}">
        <div id="userRating" style="display: inline-block;"></div>
    </c:if>

    <c:if test="${model.user.shareRole}">
        <span class="header"><a href="javascript:shareAlbum()"><img src="<spring:theme code='shareSmallImage'/>" style="height:18px;" alt=""></a>
            <a href="javascript:shareAlbum()"><fmt:message key="main.sharealbum"/></a> </span> |
    </c:if>

        <span class="header external-search"><fmt:message key="top.search"/>: <a href="#" onclick="searchExternally('google')">Google</a> |</span>
        <span class="header external-search"><a href="#" onclick="searchExternally('wikipedia')">Wikipedia</a> |</span>
        <span class="header external-search"><a href="#" onclick="searchExternally('allmusic')">allmusic</a> |</span>
        <span class="header external-search"><a href="#" onclick="searchExternally('lastfm')">Last.fm</a> |</span>
        <span class="header external-search"><a href="#" onclick="searchExternally('discogs')">Discogs</a> |</span>
        <span class="header external-search musicbrainz"><a href="#" onclick="searchExternally('musicbrainz')">MusicBrainz</a> |</span>
</div>

<div id="playCountData" class="detail pagetype-dependent type-album"></div>

<div id="comment" class="albumComment"></div>

<div id="commentForm" style="display:none">
    <form method="post" action="setMusicFileInfo.view">
        <sec:csrfInput />
        <input type="hidden" name="action" value="comment">
        <input type="hidden" id="commentFormId" name="id" value="">
        <textarea name="comment" rows="6" cols="70"></textarea>
        <input type="submit" value="<fmt:message key='common.save'/>">
    </form>
</div>

<div class="tableSpacer"></div>

<table id="filesTable" class="music indent hover nowrap stripe compact <c:if test='${!model.visibility.headerVisible}'>hide-table-header</c:if>" style="width: 100%;">
</table>

<select id="moreActions" class="pagetype-dependent type-album type-video" onchange="actionSelected(this.options[selectedIndex].id);" style="margin-bottom:1.0em">
    <option id="top" selected="selected"><fmt:message key="main.more.selection"/></option>
    <option id="selectAll">&nbsp;&nbsp;<fmt:message key="playlist.more.selectall"/></option>
    <option id="selectNone">&nbsp;&nbsp;<fmt:message key="playlist.more.selectnone"/></option>
    <c:if test="${model.user.downloadRole}">
        <option id="download">&nbsp;&nbsp;<fmt:message key="common.download"/></option>
    </c:if>
    <c:if test="${model.user.shareRole}">
        <option id="share">&nbsp;&nbsp;<fmt:message key="main.more.share"/></option>
    </c:if>
    <option id="appendPlaylist">&nbsp;&nbsp;<fmt:message key="playlist.append"/></option>
    <option id="star">&nbsp;&nbsp;<fmt:message key="playlist.more.star"/></option>
    <option id="unstar">&nbsp;&nbsp;<fmt:message key="playlist.more.unstar"/></option>
</select>

<div class="tableSpacer"></div>

<div style="display: flex; flex-flow: row wrap;">
    <div id="mediaDirThumb" class="albumThumb pagetype-dependent type-album"></div>
    <table id="subDirsTable" class="music indent hover nowrap stripe compact hide-table-header" style="width: 100%;"></table>
</div>

<div class="tableSpacer"></div>

<div id="thumbs"></div>

<table id="artistInfoTable" style="padding:2em;clear:both" class="bgcolor2 dropshadow pagetype-dependent type-album type-artist">
    <tr class="pagetype-dependent type-artist">
        <td rowspan="5" style="vertical-align: top">
            <a id="artistImageZoom" rel="zoom" href="#">
                <img id="artistImage" class="dropshadow" alt="" style="margin-right:2em; max-width:300px; max-height:300px">
            </a>
        </td>
        <td id="artistTitle" style="text-align:center"><h2></h2></td>
    </tr>
    <tr class="pagetype-dependent type-artist">
        <td id="artistBio" style="padding-bottom: 0.5em"></td>
    </tr>
    <tr id="similarArtistsContainer" class="pagetype-dependent type-album type-artist"><td style="padding-bottom: 0.5em">
        <span style="padding-right: 0.5em; display: none"><fmt:message key="main.similarartists"/>:</span>
        <span id="similarArtists"></span>
    </td></tr>
    <tr><td style="text-align:center">
        <div>
            <div class="pagetype-dependent type-album type-artist" style="display: inline-block"><button id="similarArtistsRadio" style="margin-top:1em;margin-right:0.3em;cursor:pointer" onclick="playSimilar()"><fmt:message key='main.startradio'/></button></div>
            <div class="pagetype-dependent type-artist" style="display: inline-block"><button id="playTopSongs" style="margin-top:1em;margin-left:0.3em;cursor:pointer" onclick="playAllTopSongs()"><fmt:message key='main.playtopsongs'/></button></div>
        </div>
    </td></tr>
    <tr><td style="height: 100%"></td></tr>
</table>

<table id="artistTopSongsTable" class="music indent hover nowrap stripe compact hide-table-header" style="width: 100%;">
</table>

<div id="dialog-select-playlist" title="<fmt:message key='main.addtoplaylist.title'/>" style="display: none;">
    <p><fmt:message key="main.addtoplaylist.text"/></p>
    <div id="dialog-select-playlist-list"></div>
</div>

</body>
</html>
