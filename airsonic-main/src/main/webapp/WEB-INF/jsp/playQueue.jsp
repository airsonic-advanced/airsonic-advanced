<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="table.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/mediaelement/mediaelement-and-player.min.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/playQueueCast.js'/>"></script>
    <style type="text/css">
        .ui-slider .ui-slider-handle {
            width: 11px;
            height: 11px;
            cursor: pointer;
        }
        .ui-slider a {
            outline:none;
        }
        .ui-slider {
            cursor: pointer;
        }
        #playQueueSpacer {
            height: 1em;
        }
        #playQueueHeading {
            display: inline-block;
        }
        #playQueueInfo {
            display: inline-block;
        }
    </style>

<script type="text/javascript" language="javascript">
    var playerId = ${model.player.id};

    // These variables store the media player state, received via websockets in the
    // playQueueCallback function below.

    // List of songs (of type PlayQueueInfo.Entry)
    var songs = [];

    // Stream URL of the media being played
    var currentStreamUrl = null;

    var currentSongIndex = -1;

    // Is autorepeat enabled?
    var repeatStatus = 'OFF';

    // Is the "shuffle radio" playing? (More > Shuffle Radio)
    var shuffleRadioEnabled = false;

    // Is the "internet radio" playing?
    var internetRadioEnabled = false;

    // Is the play queue visible?
    var isVisible = false;

    // Initialize the Cast player (ChromeCast support)
    var CastPlayer = new CastPlayer();

    var musicTable = null;

    function init() {
        var ratingOnImage = "<spring:theme code='ratingOnImage'/>";
        var ratingOffImage = "<spring:theme code='ratingOffImage'/>";

        musicTable = $("#playQueueMusic").DataTable( {
            deferRender: true,
            createdRow: function(row, data, dataIndex, cells) {
                if (currentSongIndex == dataIndex) {
                    $(row).addClass("currently-playing").find(".currentImage").show();
                }
            },
            ordering: true,
            order: [],
            orderFixed: [ 0, 'asc' ],
            orderMulti: false,
            lengthMenu: [[10, 20, 50, 100, -1], [10, 20, 50, 100, "All"]],
            buttons: [
                {
                  text: "<fmt:message key='main.nowplaying'/>",
                  action: function (e, dt, node, config) {
                      if (currentSongIndex != -1) {
                          dt.row(currentSongIndex).show().draw(false);
                      }
                  }
                }
            ],
            processing: true,
            autoWidth: true,
            scrollCollapse: true,
            scrollY: "60vh",
            dom: "<'#playQueueHeading'><'#playQueueInfo'><'#playQueueSpacer'>lfrtipB",
            select: {
                style: "multi",
                selector: ".songIndex"
            },
            rowReorder: {
                dataSrc: "seq",
                selector: "td:not(.not-draggable)"
            },
            language: {
                emptyTable: "<fmt:message key='playlist.empty'/>"
            },
            ajax: function(ajaxData, callback) {
                for ( var i=0, len=songs.length ; i<len ; i++ ) {
                  songs[i].seq = i;
                }
                callback({data: songs});
            },
            stripeClasses: ["bgcolor1", "bgcolor2"],
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
                  name: "remove",
                  className: "fit not-draggable",
                  defaultContent: "<img class='removeSong' src=\"<spring:theme code='removeImage'/>\" style='height:18px;' alt=\"<fmt:message key='playlist.remove'/>\" title=\"<fmt:message key='playlist.remove'/>\">"
                },
                { data: null,
                  searchable: false,
                  name: "songcheckbox",
                  className: "fit not-draggable",
                  defaultContent: "<input type='checkbox' class='songIndex'>"
                },
                { data: "trackNumber", className: "detail fit", visible: ${model.visibility.trackNumberVisible} },
                { data: "title",
                  className: "detail songTitle truncate",
                  render: function(title, type, row) {
                      if (type == "display") {
                          var img = "<img class='currentImage' src=\"<spring:theme code='currentImage'/>\" alt='' style='display:none;padding-right: 0.5em' />";
                          if (!${model.player.externalWithPlaylist}) {
                              return img + "<a class='titleUrl' href='javascript:void(0)'>" + title + "</a>";
                          } else {
                              return img + title;
                          }
                      }
                      return title;
                  }
                },
                { data: "album",
                  visible: ${model.visibility.albumVisible},
                  className: "detail truncate",
                  render: function(album, type, row) {
                      if (type == "display") {
                          return "<a href='"+ row.albumUrl + "' target='" + (!internetRadioEnabled ? "main" : "_blank' rel='noopener noreferrer") + "'>" + album + "</a>";
                      }
                      return album;
                  }
                },
                { data: "artist", className: "detail truncate", visible: ${model.visibility.artistVisible} },
                { data: "genre", className: "detail truncate", visible: ${model.visibility.genreVisible} },
                { data: "year", className: "detail fit rightalign", visible: ${model.visibility.yearVisible} },
                { data: "format", className: "detail fit rightalign", visible: ${model.visibility.formatVisible} },
                { data: "fileSize", className: "detail fit rightalign", visible: ${model.visibility.fileSizeVisible} },
                { data: "durationAsString", className: "detail fit rightalign", visible: ${model.visibility.durationVisible} },
                { data: "bitRate", className: "detail fit rightalign", visible: ${model.visibility.bitRateVisible} }
            ]
        } );

        $("#playQueueMusic tbody").on( "click", ".starSong", function () {
            onStar(musicTable.row( $(this).parents('tr') ).index());
        } );
        $("#playQueueMusic tbody").on( "click", ".removeSong", function () {
            onRemove(musicTable.row( $(this).parents('tr') ).index());
        } );
        $("#playQueueMusic tbody").on( "click", ".titleUrl", function () {
            onSkip(musicTable.row( $(this).parents('tr') ).index());
        } );
        musicTable.on( "row-reordered", function (e, diff, edit) {
            musicTable.one( "draw", function () {
                onRearrange(musicTable.rows().indexes().toArray());
            });
        });

        $("#playQueueHeading").html("<h2><fmt:message key='playlist.more.playlist'/></h2>");

        top.StompClient.subscribe("playQueue.jsp", {
            // Now playing
            '/topic/nowPlaying/current/add': function(msg) {
                var nowPlayingInfo = JSON.parse(msg.body);
                onNowPlayingChanged(nowPlayingInfo);
            },
            '/app/nowPlaying/current': function(msg) {
                var nowPlayingInfos = JSON.parse(msg.body);
                for (var i = 0; i < nowPlayingInfos.length; i++) {
                    if (onNowPlayingChanged(nowPlayingInfos[i])) {
                        break;
                    }
                }
            },

            // Playlists
            '/user/queue/playlists/writable': function(msg) {
                playlistSelectionCallback(JSON.parse(msg.body));
            },
            '/user/queue/playlists/files/append': function(msg) {
                playlistUpdatedCallback(JSON.parse(msg.body), "<fmt:message key='playlist.toast.appendtoplaylist'/>");
            },
            '/user/queue/playlists/create/playqueue': function(msg) {
                playlistUpdatedCallback(JSON.parse(msg.body), "<fmt:message key='playlist.toast.saveasplaylist'/>");
            },

            // Playqueues
            '/user/queue/playqueues/${model.player.id}/playstatus': function(msg) {
                playQueuePlayStatusCallback(JSON.parse(msg.body));
            },
            '/user/queue/playqueues/${model.player.id}/updated': function(msg) {
                playQueueCallback(JSON.parse(msg.body));
            },
            '/user/queue/playqueues/${model.player.id}/skip': function(msg) {
                playQueueSkipCallback(JSON.parse(msg.body));
            },
            '/user/queue/playqueues/${model.player.id}/save': function(msg) {
                $().toastmessage("showSuccessToast", "<fmt:message key='playlist.toast.saveplayqueue'/> (" + JSON.parse(msg.body) + ")");
            },
            '/user/queue/playqueues/${model.player.id}/repeat': function(msg) {
                playQueueRepeatStatusCallback(JSON.parse(msg.body));
            },
            '/user/queue/playqueues/${model.player.id}/jukebox/gain': function(msg) {
                jukeBoxGainCallback(JSON.parse(msg.body));
            },
            '/user/queue/playqueues/${model.player.id}/jukebox/position': function(msg) {
                jukeBoxPositionCallback(JSON.parse(msg.body));
            },
            //one-time
            '/app/playqueues/${model.player.id}/get': function(msg) {
                playQueueCallback(JSON.parse(msg.body));
            }
        });

        $("#dialog-select-playlist").dialog({resizable: true, height: 220, autoOpen: false,
            buttons: {
                "<fmt:message key="common.cancel"/>": function() {
                    $(this).dialog("close");
                }
            }});

        <c:if test="${model.player.web}">createMediaElementPlayer();</c:if>
        <c:if test="${model.autoHide}">initAutoHide();</c:if>
        onTogglePlayQueue(true);

        /** Toggle between <a> and <span> in order to disable play queue action buttons */
        $.fn.toggleLink = function(newState) {
            $(this).each(function(ix, elt) {

                var node, currentState;
                if (elt.tagName.toLowerCase() === "a") currentState = true;
                else if (elt.tagName.toLowerCase() === "span") currentState = false;
                else return true;
                if (typeof newState === 'undefined') newState = !currentState;
                if (newState === currentState) return true;

                if (newState) node = document.createElement("a");
                else node = document.createElement("span");

                node.innerHTML = elt.innerHTML;
                if (elt.hasAttribute("id")) node.setAttribute("id", elt.getAttribute("id"));
                if (elt.hasAttribute("style")) node.setAttribute("style", elt.getAttribute("style"));
                if (elt.hasAttribute("class")) node.setAttribute("class", elt.getAttribute("class"));

                if (newState) {
                    if (elt.hasAttribute("data-href")) node.setAttribute("href", elt.getAttribute("data-href"));
                    node.classList.remove("disabled");
                    node.removeAttribute("aria-disabled");
                } else {
                    if (elt.hasAttribute("href")) node.setAttribute("data-href", elt.getAttribute("href"));
                    node.classList.add("disabled");
                    node.setAttribute("aria-disabled", "true");
                }

                elt.parentNode.replaceChild(node, elt);
                return true;
            });
        };
    }

    function playQueuePlayStatusCallback(status) {
        if (isJavaJukeboxPresent()) {
            if (status == "PLAYING") {
                javaJukeboxStartCallback();
            } else {
                javaJukeboxStopCallback();
            }
        }
    }

    function onHidePlayQueue() {
      top.document.getElementById("playQueueFrameset").rows = "*,50";
      isVisible = false;
      $("#spacer").show();
      $(".playqueue-shown").hide();
      $(".playqueue-hidden").show();
    }

    function onShowPlayQueue() {
      top.document.getElementById("playQueueFrameset").rows = "*," + Math.floor(window.top.innerHeight * 0.7);
      isVisible = true;
      $("#spacer").hide();
      $(".playqueue-shown").show();
      $(".playqueue-hidden").hide();
    }

    function onTogglePlayQueue(visible) {
      if (visible) {
          onShowPlayQueue();
      } else {
          onHidePlayQueue();
      }
    }

    function initAutoHide() {
        $(window).mouseleave(function (event) {
            if (event.clientY < 30) onHidePlayQueue();
        });

        $(window).mouseenter(function () {
            onShowPlayQueue();
        });
    }

    function onNowPlayingChanged(nowPlayingInfo) {
        if (nowPlayingInfo != null && nowPlayingInfo.streamUrl != currentStreamUrl && nowPlayingInfo.playerId == ${model.player.id}) {
        <c:if test="${not model.player.web}">
            // TODO this should be keying off skip callbacks (and skip callbacks should be getting emitted)
            // otherwise there is an issue with the same song appearing multiple times on the playqueue (we'll always select the first)
            currentStreamUrl = nowPlayingInfo.streamUrl;
            currentSongIndex = getCurrentSongIndex();

            playQueueSkipCallback({index: currentSongIndex, offset: 0});
        </c:if>
            return true;
        }
        return false;
    }

    function onEnded() {
        onNext(repeatStatus);
    }

    function createMediaElementPlayer() {
        // Manually run MediaElement.js initialization.
        //
        // Warning: Bugs will happen if MediaElement.js is not initialized when
        // we modify the media elements (e.g. adding event handlers). Running
        // MediaElement.js's automatic initialization does not guarantee that
        // (it depends on when we call createMediaElementPlayer at load time).
        $('#audioPlayer').mediaelementplayer();

        // Once playback reaches the end, go to the next song, if any.
        $('#audioPlayer').on("ended", onEnded);
    }

    function onClear() {
        var ok = true;
    <c:if test="${model.partyMode}">
        ok = confirm("<fmt:message key="playlist.confirmclear"/>");
    </c:if>
        if (ok) {
            top.StompClient.send("/app/playqueues/${model.player.id}/clear", "");
        }
    }

    /**
     * Start/resume playing from the current playlist
     */
    function onStart() {
        if (CastPlayer.castSession) {
            CastPlayer.playCast();
        } else if ($('#audioPlayer').get(0)) {
            if ($('#audioPlayer').get(0).src) {
                $('#audioPlayer').get(0).play();  // Resume playing if the player was paused
            } else {
                onSkip(0);  // Start the first track if the player was not yet loaded
            }
        } else {
            top.StompClient.send("/app/playqueues/${model.player.id}/start", "");
        }
    }

    /**
     * Pause playing
     */
    function onStop() {
        if (CastPlayer.castSession) {
            CastPlayer.pauseCast();
        } else if ($('#audioPlayer').get(0)) {
            $('#audioPlayer').get(0).pause();
        } else {
            top.StompClient.send("/app/playqueues/${model.player.id}/stop", "");
        }
    }

    /**
     * Toggle play/pause
     *
     * FIXME: Only works for the Web player for now
     */
    function onToggleStartStop() {
        if (CastPlayer.castSession) {
            var playing = CastPlayer.mediaSession && CastPlayer.mediaSession.playerState == chrome.cast.media.PlayerState.PLAYING;
            if (playing) {
                onStop();
            } else {
                onStart();
            }
        } else if ($('#audioPlayer').get(0)) {
            var playing = $("#audioPlayer").get(0).paused != null && !$("#audioPlayer").get(0).paused;
            if (playing) {
                onStop();
            } else {
                onStart();
            }
        } else {
            top.StompClient.send("/app/playqueues/${model.player.id}/toggleStartStop", "");
        }
    }

    function jukeBoxPositionCallback(pos) {
        if (isJavaJukeboxPresent()) {
            javaJukeboxPositionCallback(pos);
        }
    }
    function jukeBoxGainCallback(gain) {
        $("#jukeboxVolume").slider("option", "value", Math.floor(gain * 100)); // update UI
        if (isJavaJukeboxPresent()) {
            javaJukeboxGainCallback(gain);
        }
    }
    function onJukeboxVolumeChanged() {
        var value = parseInt($("#jukeboxVolume").slider("option", "value"));
        top.StompClient.send("/app/playqueues/${model.player.id}/jukebox/gain", value / 100);
    }
    function onCastVolumeChanged() {
        var value = parseInt($("#castVolume").slider("option", "value"));
        CastPlayer.setCastVolume(value / 100, false);
    }

    /**
     * Increase or decrease volume by a certain amount
     *
     * @param gain amount to add or remove from the current volume
     */
    function onGainAdd(gain) {
        if (CastPlayer.castSession) {
            var volume = parseInt($("#castVolume").slider("option", "value")) + gain;
            if (volume > 100) volume = 100;
            if (volume < 0) volume = 0;
            CastPlayer.setCastVolume(volume / 100, false);
            $("#castVolume").slider("option", "value", volume); // Need to update UI
        } else if ($('#audioPlayer').get(0)) {
            var volume = parseFloat($('#audioPlayer').get(0).volume)*100 + gain;
            if (volume > 100) volume = 100;
            if (volume < 0) volume = 0;
            $('#audioPlayer').get(0).volume = volume / 100;
        } else {
            var volume = parseInt($("#jukeboxVolume").slider("option", "value")) + gain;
            if (volume > 100) volume = 100;
            if (volume < 0) volume = 0;
            top.StompClient.send("/app/playqueues/${model.player.id}/jukebox/gain", volume / 100);
            // UI updated at callback
        }
    }

    function playQueueSkipCallback(location) {
        if (location.index < 0 || location.index >= songs.length) {
            return;
        }

        var song = songs[location.index];
        currentStreamUrl = song.streamUrl;
        currentSongIndex = location.index;
        updateCurrentImage();

      <c:choose>
      <c:when test="${model.player.web}">
        webSkip(song, location.offset / 1000);
      </c:when>
      <c:otherwise>
        if (isJavaJukeboxPresent()) {
            updateJavaJukeboxPlayerControlBar(song, location.offset / 1000);
        }
      </c:otherwise>
      </c:choose>

        updateWindowTitle(song);

      <c:if test="${model.notify}">
        showNotification(song);
      </c:if>
    }

    function onSkip(index, offset) {
    <c:choose>
    <c:when test="${model.player.web}">
        playQueueSkipCallback({index: index, offset: offset});
    </c:when>
    <c:otherwise>
        top.StompClient.send("/app/playqueues/${model.player.id}/skip", JSON.stringify({index: index, offset: offset}));
    </c:otherwise>
    </c:choose>
    }

    function webSkip(song, position) {
        // Handle ChromeCast player.
        if (CastPlayer.castSession) {
            CastPlayer.loadCastMedia(song, position);
        // Handle MediaElement (HTML5) player.
        } else {
            loadMediaElementPlayer(song, position);
        }
    }

    function loadMediaElementPlayer(song, position) {
        var player = $('#audioPlayer').get(0);

        // Is this a new song?
        if (player.src == null || !player.src.endsWith(song.streamUrl)) {
            // Stop the current playing song and change the media source.
            player.src = song.streamUrl;
            // Inform MEJS that we need to load a new media source. The
            // 'canplay' event will be fired once playback is possible.
            player.load();
            // The 'skip' function takes a 'position' argument. We don't
            // usually send it, and in this case it's better to do nothing.
            // Otherwise, the 'canplay' event will also be fired after
            // setting 'currentTime'.
            if (position && position > 0) {
                player.currentTime = position;
            }

        // Are we seeking on an already-playing song?
        } else {
            // Seeking also starts playing. The 'canplay' event will be
            // fired after setting 'currentTime'.
            player.currentTime = position || 0;
        }

        // Start playback immediately.
        player.play();
    }

    function onNext(repeatStatus) {
        var index = currentSongIndex;
        if (shuffleRadioEnabled && (index + 1) >= songs.length) {
            top.StompClient.send("/app/playqueues/${model.player.id}/reloadsearch", "");
        } else if (repeatStatus == 'TRACK') {
            onSkip(index);
        } else {
            index = index + 1;
            if (repeatStatus == 'QUEUE') {
                index = index % songs.length;
            }
            onSkip(index);
        }
    }
    function onPrevious() {
        onSkip(currentSongIndex - 1);
    }
    function onPlay(id) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/mediafile", JSON.stringify({id: id}));
    }
    function onPlayShuffle(albumListType, offset, count, genre, decade) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/shuffle", JSON.stringify({albumListType: albumListType, offset: offset, count: count, genre: genre, decade: decade}));
    }
    function onPlayPlaylist(id, index) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/playlist", JSON.stringify({id: id, index: index}));
    }
    function onPlayInternetRadio(id, index) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/radio", JSON.stringify({id: id, index: index}));
    }
    function onPlayTopSong(id, index) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/topsongs", JSON.stringify({id: id, index: index}));
    }
    function onPlayPodcastChannel(id) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/podcastchannel", JSON.stringify({id: id}));
    }
    function onPlayPodcastEpisode(id) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/podcastepisode", JSON.stringify({id: id}));
    }
    function onPlayNewestPodcastEpisode(index) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/podcastepisode/newest", JSON.stringify({index: index}));
    }
    function onPlayStarred() {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/starred", "");
    }
    function onPlayRandom(id, count) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/random", JSON.stringify({id: id, count: count}));
    }
    function onPlaySimilar(id, count) {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/similar", JSON.stringify({id: id, count: count}));
    }
    function onAdd(id) {
        top.StompClient.send("/app/playqueues/${model.player.id}/add", JSON.stringify({ids: [id]}));
    }
    function onAddNext(id) {
        top.StompClient.send("/app/playqueues/${model.player.id}/add", JSON.stringify({ids: [id], index: currentSongIndex + 1}));
    }
    function onAddPlaylist(id) {
        top.StompClient.send("/app/playqueues/${model.player.id}/add/playlist", JSON.stringify({id: id}));
    }
    function onShuffle() {
        top.StompClient.send("/app/playqueues/${model.player.id}/shuffle", "");
    }
    function onStar(index) {
        songs[index].starred = !songs[index].starred;

        if (songs[index].starred) {
            top.StompClient.send("/app/rate/mediafile/star", songs[index].id);
        } else {
            top.StompClient.send("/app/rate/mediafile/unstar", songs[index].id);
        }
        musicTable.cell(index, "starred:name").invalidate().draw();
    }
    function onStarCurrent() {
        onStar(currentSongIndex);
    }
    function onRemove(index) {
        top.StompClient.send("/app/playqueues/${model.player.id}/remove", JSON.stringify([index]));
    }
    function onRemoveSelected() {
        top.StompClient.send("/app/playqueues/${model.player.id}/remove", JSON.stringify(musicTable.rows({ selected: true }).indexes().toArray()));
    }

    function onRearrange(indexes) {
        top.StompClient.send("/app/playqueues/${model.player.id}/rearrange", JSON.stringify(indexes));
    }
    function onToggleRepeat() {
        top.StompClient.send("/app/playqueues/${model.player.id}/toggleRepeat", "");
    }
    function onUndo() {
        top.StompClient.send("/app/playqueues/${model.player.id}/undo", "");
    }
    function onSortByTrack() {
        top.StompClient.send("/app/playqueues/${model.player.id}/sort", "TRACK");
    }
    function onSortByArtist() {
        top.StompClient.send("/app/playqueues/${model.player.id}/sort", "ARTIST");
    }
    function onSortByAlbum() {
        top.StompClient.send("/app/playqueues/${model.player.id}/sort", "ALBUM");
    }
    function onSavePlayQueue() {
        var positionMillis = $('#audioPlayer').get(0) ? Math.round(1000.0 * $('#audioPlayer').get(0).currentTime) : 0;
        top.StompClient.send("/app/playqueues/${model.player.id}/save", JSON.stringify({index: currentSongIndex, offset: positionMillis}));
    }
    function onLoadPlayQueue() {
        top.StompClient.send("/app/playqueues/${model.player.id}/play/saved", "");
    }
    function onSavePlaylist() {
        top.StompClient.send("/app/playlists/create/playqueue", "${model.player.id}");
    }
    function onAppendPlaylist() {
        // retrieve writable lists so we can open dialog to ask user which playlist to append to
        top.StompClient.send("/app/playlists/writable", "");
    }
    function playlistSelectionCallback(playlists) {
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

        var mediaFileIds = musicTable.rows({selected:true}).data().map(function(d) { return d.id; }).toArray();

        top.StompClient.send("/app/playlists/files/append", JSON.stringify({id: playlistId, modifierIds: mediaFileIds}));
    }

    function playlistUpdatedCallback(playlistId, toastMsg) {
        if (!top.main.location.href.endsWith("playlist.view?id=" + playlistId)) {
            // change page
            top.main.location.href = "playlist.view?id=" + playlistId;
        }
        $().toastmessage("showSuccessToast", toastMsg);
    }

    function isJavaJukeboxPresent() {
        return $("#javaJukeboxPlayerControlBarContainer").length==1;
    }

    function playQueueRepeatStatusCallback(incomingStatus) {
        repeatStatus = incomingStatus;
        if ($("#toggleRepeat").length != 0) {
            if (shuffleRadioEnabled) {
                $("#toggleRepeat").html("<fmt:message key="playlist.repeat_radio"/>");
            } else if (repeatStatus == 'QUEUE') {
                $("#toggleRepeat").attr('src', '<spring:theme code="repeatAll"/>');
                $("#toggleRepeat").attr('alt', 'Repeat All/Queue');
            } else if (repeatStatus == 'OFF') {
                $("#toggleRepeat").attr('src', '<spring:theme code="repeatOff"/>');
                $("#toggleRepeat").attr('alt', 'Repeat Off');
            } else if (repeatStatus == 'TRACK') {
                $("#toggleRepeat").attr('src', '<spring:theme code="repeatOne"/>');
                $("#toggleRepeat").attr('alt', 'Repeat One/Track');
            }
        }
    }

    function playQueueCallback(playQueue) {
        songs = playQueue.entries;
        shuffleRadioEnabled = playQueue.shuffleRadioEnabled;
        internetRadioEnabled = playQueue.internetRadioEnabled;

        // If an internet radio has no sources, display a message to the user.
        if (internetRadioEnabled && songs.length == 0) {
            top.main.$().toastmessage("showErrorToast", "<fmt:message key="playlist.toast.radioerror"/>");
            onStop();
        }

        if ($("#start").length != 0) {
            $("#start").toggle(!playQueue.stopEnabled);
            $("#stop").toggle(playQueue.stopEnabled);
        }

        playQueueRepeatStatusCallback(playQueue.repeatStatus);

        // Disable some UI items if internet radio is playing
        $("select#moreActions #loadPlayQueue").prop("disabled", internetRadioEnabled);
        $("select#moreActions #savePlayQueue").prop("disabled", internetRadioEnabled);
        $("select#moreActions #savePlaylist").prop("disabled", internetRadioEnabled);
        $("select#moreActions #downloadPlaylist").prop("disabled", internetRadioEnabled);
        $("select#moreActions #sharePlaylist").prop("disabled", internetRadioEnabled);
        $("select#moreActions #sortByTrack").prop("disabled", internetRadioEnabled);
        $("select#moreActions #sortByAlbum").prop("disabled", internetRadioEnabled);
        $("select#moreActions #sortByArtist").prop("disabled", internetRadioEnabled);
        $("select#moreActions #selectAll").prop("disabled", internetRadioEnabled);
        $("select#moreActions #selectNone").prop("disabled", internetRadioEnabled);
        $("select#moreActions #removeSelected").prop("disabled", internetRadioEnabled);
        $("select#moreActions #download").prop("disabled", internetRadioEnabled);
        $("select#moreActions #appendPlaylist").prop("disabled", internetRadioEnabled);
        $("#shuffleQueue").toggleLink(!internetRadioEnabled);
        $("#repeatQueue").toggleLink(!internetRadioEnabled);
        $("#undoQueue").toggleLink(!internetRadioEnabled);

        if (songs.length == 0) {
            $("#playQueueInfo").text("");
        } else {
            $("#playQueueInfo").html("&nbsp;|&nbsp;" + songs.length + " <fmt:message key='playlist2.songs'/> &nbsp;|&nbsp;" + playQueue.durationAsString);
        }

        if (internetRadioEnabled) {
            musicTable.column("starred:name").visible(false, false);
            musicTable.column("remove:name").visible(false, false);
            musicTable.column("songcheckbox:name").visible(false, false);
        } else {
            musicTable.column("starred:name").visible(true, false);
            musicTable.column("remove:name").visible(true, false);
            musicTable.column("songcheckbox:name").visible(true, false);
        }

        currentSongIndex = getCurrentSongIndex();
        musicTable.ajax.reload().columns.adjust();
        updateCurrentImage();

        if (playQueue.sendM3U) {
            parent.frames.main.location.href="play.m3u?";
        }

        jukeBoxGainCallback(playQueue.gain);
    }

    function updateWindowTitle(song) {
        top.document.title = song.title + " - " + song.artist + " - Airsonic";
    }

    function showNotification(song) {
        if (!("Notification" in window)) {
            return;
        }
        if (Notification.permission === "granted") {
            createNotification(song);
        }
        else if (Notification.permission !== 'denied') {
            Notification.requestPermission(function (permission) {
                Notification.permission = permission;
                if (permission === "granted") {
                    createNotification(song);
                }
            });
        }
    }

    function createNotification(song) {
        var n = new Notification(song.title, {
            tag: "airsonic",
            body: song.artist + " - " + song.album,
            icon: "coverArt.view?id=" + song.id + "&size=110"
        });
        n.onshow = function() {
            setTimeout(function() {n.close()}, 5000);
        }
    }

    function updateCurrentImage() {
        $(musicTable.rows().nodes()).removeClass("currently-playing").find(".currentImage").hide();
        $(musicTable.row(currentSongIndex).node()).addClass("currently-playing").find(".currentImage").show();
    }

    function getCurrentSongIndex() {
        for (var i = 0; i < songs.length; i++) {
            if (songs[i].streamUrl == currentStreamUrl) {
                return i;
            }
        }
        return -1;
    }

    <!-- actionSelected() is invoked when the users selects from the "More actions..." combo box. -->
    function actionSelected(id) {
        var selectedIndexes = getSelectedIndexes();
        if (id == "top") {
            return;
        } else if (id == "savePlayQueue") {
            onSavePlayQueue();
        } else if (id == "loadPlayQueue") {
            onLoadPlayQueue();
        } else if (id == "savePlaylist") {
            onSavePlaylist();
        } else if (id == "downloadPlaylist") {
            location.href = "download.view?player=${model.player.id}";
        } else if (id == "sharePlaylist") {
            parent.frames.main.location.href = "createShare.view?player=${model.player.id}&" + getSelectedIndexes();
        } else if (id == "sortByTrack") {
            onSortByTrack();
        } else if (id == "sortByArtist") {
            onSortByArtist();
        } else if (id == "sortByAlbum") {
            onSortByAlbum();
        } else if (id == "selectAll") {
            selectAll(true);
        } else if (id == "selectNone") {
            selectAll(false);
        } else if (id == "removeSelected") {
            onRemoveSelected();
        } else if (id == "download" && selectedIndexes != "") {
            location.href = "download.view?player=${model.player.id}&" + selectedIndexes;
        } else if (id == "appendPlaylist" && selectedIndexes != "") {
            onAppendPlaylist();
        }
        $("#moreActions").prop("selectedIndex", 0);
    }

    function getSelectedIndexes() {
        return musicTable.rows({ selected: true }).indexes().map(function(i) { return "i=" + i; }).join("&");
    }

    function selectAll(b) {
        if (b) {
            musicTable.rows().select();
        } else {
            musicTable.rows().deselect();
        }
    }
</script>
</head>

<body class="bgcolor2 playlistframe" onload="init()">

<c:choose>
    <c:when test="${model.player.javaJukebox}">
        <div id="javaJukeboxPlayerControlBarContainer">
            <%@ include file="javaJukeboxPlayerControlBar.jspf" %>
        </div>
    </c:when>
    <c:otherwise>
        <div class="bgcolor2" style="position:fixed; bottom:0; width:100%;padding-top:10px;">
            <table style="white-space:nowrap; margin-bottom:0;">
                <tr style="white-space:nowrap;">
                    <c:if test="${model.user.settingsRole and fn:length(model.players) gt 1}">
                        <td style="padding-right: 5px"><select name="player" onchange="location='playQueue.view?player=' + options[selectedIndex].value;">
                            <c:forEach items="${model.players}" var="player">
                                <option ${player.id eq model.player.id ? "selected" : ""} value="${player.id}">${player.shortDescription}</option>
                            </c:forEach>
                        </select></td>
                    </c:if>
                    <c:if test="${model.player.web}">
                        <td>
                            <div id="player" style="width:340px; height:40px">
                                <audio id="audioPlayer" data-mejsoptions='{"alwaysShowControls": true, "enableKeyboard": false}' width="340px" height="40px" tabindex="-1" />
                            </div>
                            <div id="castPlayer" style="display: none">
                                <div style="float:left">
                                    <img alt="Play" id="castPlay" src="<spring:theme code='castPlayImage'/>" onclick="CastPlayer.playCast()" style="cursor:pointer">
                                    <img alt="Pause" id="castPause" src="<spring:theme code='castPauseImage'/>" onclick="CastPlayer.pauseCast()" style="cursor:pointer; display:none">
                                    <img alt="Mute on" id="castMuteOn" src="<spring:theme code='volumeImage'/>" onclick="CastPlayer.castMuteOn()" style="cursor:pointer">
                                    <img alt="Mute off" id="castMuteOff" src="<spring:theme code='muteImage'/>" onclick="CastPlayer.castMuteOff()" style="cursor:pointer; display:none">
                                </div>
                                <div style="float:left">
                                    <div id="castVolume" style="width:80px;height:4px;margin-left:10px;margin-right:10px;margin-top:8px"></div>
                                    <script type="text/javascript">
                                        $("#castVolume").slider({max: 100, value: 50, animate: "fast", range: "min"});
                                        $("#castVolume").on("slidestop", onCastVolumeChanged);
                                    </script>
                                </div>
                            </div>
                        </td>
                        <td>
                            <img alt="Cast on" id="castOn" src="<spring:theme code='castIdleImage'/>" onclick="CastPlayer.launchCastApp()" style="cursor:pointer; display:none">
                            <img alt="Cast off" id="castOff" src="<spring:theme code='castActiveImage'/>" onclick="CastPlayer.stopCastApp()" style="cursor:pointer; display:none">
                        </td>
                    </c:if>

                    <c:if test="${model.user.streamRole and not model.player.web}">
                        <td>
                            <img alt="Start" id="start" src="<spring:theme code='castPlayImage'/>" onclick="onStart()" style="cursor:pointer">
                            <img alt="Stop" id="stop" src="<spring:theme code='castPauseImage'/>" onclick="onStop()" style="cursor:pointer; display:none">
                        </td>
                    </c:if>

                    <c:if test="${model.player.jukebox}">
                        <td style="white-space:nowrap;">
                            <img src="<spring:theme code='volumeImage'/>" alt="">
                        </td>
                        <td style="white-space:nowrap;">
                            <div id="jukeboxVolume" style="width:80px;height:4px"></div>
                            <script type="text/javascript">
                                $("#jukeboxVolume").slider({max: 100, value: 50, animate: "fast", range: "min"});
                                $("#jukeboxVolume").on("slidestop", onJukeboxVolumeChanged);
                            </script>
                        </td>
                    </c:if>

                    <c:if test="${model.player.web}">
                        <td><span class="header">
                            <img src="<spring:theme code='backImage'/>" alt="Play next" title="Play next" onclick="onPrevious()" style="cursor:pointer"></span>
                        </td>
                        <td><span class="header">
                            <img src="<spring:theme code='forwardImage'/>" alt="Play next" title="Play next" onclick="onNext('OFF')" style="cursor:pointer"></span> |
                        </td>
                    </c:if>

                    <td style="white-space:nowrap;">
                      <span class="header">
                        <a href="javascript:onClear()" class="player-control">
                            <img src="<spring:theme code='clearImage'/>" alt="Clear playlist" title="Clear playlist" style="cursor:pointer; height:18px">
                        </a>
                      </span> |</td>

                    <td style="white-space:nowrap;">
                      <span class="header">
                        <a href="javascript:onShuffle()" id="shuffleQueue" class="player-control">
                            <img src="<spring:theme code='shuffleImage'/>" alt="Shuffle" title="Shuffle" style="cursor:pointer; height:18px">
                        </a>
                      </span> |</td>

                    <c:if test="${model.player.web or model.player.jukebox or model.player.external}">
                        <td style="white-space:nowrap;">
                          <span class="header">
                            <a href="javascript:onToggleRepeat()" id="repeatQueue" class="player-control">
                              <img id="toggleRepeat" src="<spring:theme code='repeatOff'/>" alt="Toggle repeat" title="Toggle repeat" style="cursor:pointer; height:18px">
                            </a>
                          </span> |</td>
                    </c:if>

                    <td style="white-space:nowrap;">
                      <span class="header">
                        <a href="javascript:onUndo()" id="undoQueue" class="player-control">
                          <img src="<spring:theme code='undoImage'/>" alt="Undo" title="Undo" style="cursor:pointer; height:18px">
                        </a>
                      </span>  |</td>

                    <c:if test="${model.user.settingsRole}">
                        <td style="white-space:nowrap;">
                          <span class="header">
                            <a href="playerSettings.view?id=${model.player.id}" target="main" class="player-control">
                              <img src="<spring:theme code='settingsImage'/>" alt="Settings" title="Settings" style="cursor:pointer; height:18px">
                            </a>
                          </span> |</td>
                    </c:if>

                    <td style="white-space:nowrap;"><select id="moreActions" onchange="actionSelected(this.options[selectedIndex].id)">
                        <option id="top" selected="selected"><fmt:message key="playlist.more"/></option>
                        <optgroup label="<fmt:message key='playlist.more.playlist'/>">
                            <option id="savePlayQueue"><fmt:message key="playlist.saveplayqueue"/></option>
                            <option id="loadPlayQueue"><fmt:message key="playlist.loadplayqueue"/></option>
                            <option id="savePlaylist"><fmt:message key="playlist.save"/></option>
                            <c:if test="${model.user.downloadRole}">
                            <option id="downloadPlaylist"><fmt:message key="common.download"/></option>
                            </c:if>
                            <c:if test="${model.user.shareRole}">
                            <option id="sharePlaylist"><fmt:message key="main.more.share"/></option>
                            </c:if>
                            <option id="sortByTrack"><fmt:message key="playlist.more.sortbytrack"/></option>
                            <option id="sortByAlbum"><fmt:message key="playlist.more.sortbyalbum"/></option>
                            <option id="sortByArtist"><fmt:message key="playlist.more.sortbyartist"/></option>
                        </optgroup>
                        <optgroup label="<fmt:message key='playlist.more.selection'/>">
                            <option id="selectAll"><fmt:message key="playlist.more.selectall"/></option>
                            <option id="selectNone"><fmt:message key="playlist.more.selectnone"/></option>
                            <option id="removeSelected"><fmt:message key="playlist.remove"/></option>
                            <c:if test="${model.user.downloadRole}">
                                <option id="download"><fmt:message key="common.download"/></option>
                            </c:if>
                            <option id="appendPlaylist"><fmt:message key="playlist.append"/></option>
                        </optgroup>
                    </select>
                    </td>

                    <c:if test="${not model.autoHide}">
                    <td style="white-space:nowrap; text-align:right; width:100%; padding-right:1.5em">
                      <a href="javascript:onTogglePlayQueue(!isVisible)">
                        <img class="playqueue-shown" src="<spring:theme code='playQueueHide'/>" alt="Hide play queue" title="Hide play queue" style="cursor:pointer; height:18px;"/>
                        <img class="playqueue-hidden" src="<spring:theme code='playQueueShow'/>" alt="Show play queue" title="Show play queue" style="cursor:pointer; height:18px; display: none;"/>
                      </a>
                    </td>
                    </c:if>

                </tr></table>
        </div>
    </c:otherwise>
</c:choose>

<div id="spacer" style="height:55px"></div>
<table class="music indent hover nowrap stripe compact hide-table-header" id="playQueueMusic" style="cursor:pointer"></table>

<div id="dialog-select-playlist" title="<fmt:message key='main.addtoplaylist.title'/>" style="display: none;">
    <p><fmt:message key="main.addtoplaylist.text"/></p>
    <div id="dialog-select-playlist-list"></div>
</div>

<script type="text/javascript">
    window['__onGCastApiAvailable'] = function(isAvailable) {
        if (isAvailable) {
            CastPlayer.initializeCastPlayer();
        }
    };
</script>
<script type="text/javascript" src="https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1"></script>

</body></html>
