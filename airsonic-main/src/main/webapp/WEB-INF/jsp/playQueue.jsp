<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="websocket.jsp" %>
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
    </style>
</head>

<body class="bgcolor2 playlistframe" onload="init()">

<span id="dummy-animation-target" style="max-width: ${model.autoHide ? 50 : 150}px; display: none"></span>

<script type="text/javascript" language="javascript">
    var playerId = ${model.player.id};

    // These variables store the media player state, received via websockets in the
    // playQueueCallback function below.

    // List of songs (of type PlayQueueInfo.Entry)
    var songs = null;

    // Stream URL of the media being played
    var currentStreamUrl = null;

    var currentSongIndex = -1;

    // Is autorepeat enabled?
    var repeatStatus = 'OFF';

    // Is the "shuffle radio" playing? (More > Shuffle Radio)
    var shuffleRadioEnabled = false;

    // Is the "internet radio" playing?
    var internetRadioEnabled = false;

    // Is the play queue visible? (Initially hidden if set to "auto-hide" in the settings)
    var isVisible = ${model.autoHide ? 'false' : 'true'};

    // Initialize the Cast player (ChromeCast support)
    var CastPlayer = new CastPlayer();

    function init() {
        <c:if test="${model.autoHide}">initAutoHide();</c:if>

        StompClient.subscribe({
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
      setFrameHeight(50);
      isVisible = false;
    }

    function onShowPlayQueue() {
      var height = $("body").height() + 25;
      height = Math.min(height, window.top.innerHeight * 0.8);
      setFrameHeight(height);
      isVisible = true;
    }

    function onTogglePlayQueue() {
      if (isVisible) onHidePlayQueue();
      else onShowPlayQueue();
    }

    function initAutoHide() {
        $(window).mouseleave(function (event) {
            if (event.clientY < 30) onHidePlayQueue();
        });

        $(window).mouseenter(function () {
            onShowPlayQueue();
        });
    }

    function setFrameHeight(height) {
        <%-- Disable animation in Chrome. It stopped working in Chrome 44. --%>
        var duration = navigator.userAgent.indexOf("Chrome") != -1 ? 0 : 400;

        $("#dummy-animation-target").stop();
        $("#dummy-animation-target").animate({"max-width": height}, {
            step: function (now, fx) {
                top.document.getElementById("playQueueFrameset").rows = "*," + now;
            },
            duration: duration
        });
    }

    function onNowPlayingChanged(nowPlayingInfo) {
        if (nowPlayingInfo != null && nowPlayingInfo.streamUrl != currentStreamUrl && nowPlayingInfo.playerId == ${model.player.id}) {
        <c:if test="${not model.player.web}">
            currentStreamUrl = nowPlayingInfo.streamUrl;
            currentSongIndex = getCurrentSongIndex();
            updateCurrentImage();
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
            StompClient.send("/app/playqueues/${model.player.id}/clear", "");
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
                skip(0);  // Start the first track if the player was not yet loaded
            }
        } else {
            StompClient.send("/app/playqueues/${model.player.id}/start", "");
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
            StompClient.send("/app/playqueues/${model.player.id}/stop", "");
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
            StompClient.send("/app/playqueues/${model.player.id}/toggleStartStop", "");
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
        StompClient.send("/app/playqueues/${model.player.id}/jukebox/gain", value / 100);
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
            StompClient.send("/app/playqueues/${model.player.id}/jukebox/gain", volume / 100);
            // UI updated at callback
        }
    }

    function playQueueSkipCallback(location) {
      <c:choose>
      <c:when test="${model.player.web}">
        skip(location.index, location.offset / 1000);
      </c:when>
      <c:otherwise>
        currentStreamUrl = songs[location.index].streamUrl;
        currentSongIndex = location.index;
        if (isJavaJukeboxPresent()) {
            updateJavaJukeboxPlayerControlBar(songs[location.index], location.offset / 1000);
        }
      </c:otherwise>
      </c:choose>
    }

    function onSkip(index, offset) {
    <c:choose>
    <c:when test="${model.player.web}">
        playQueueSkipCallback({index: index, offset: offset});
    </c:when>
    <c:otherwise>
        StompClient.send("/app/playqueues/${model.player.id}/skip", JSON.stringify({index: index, offset: offset}));
    </c:otherwise>
    </c:choose>
    }

    function skip(index, position) {
        if (index < 0 || index >= songs.length) {
            return;
        }

        var song = songs[index];
        currentStreamUrl = song.streamUrl;
        currentSongIndex = index;
        updateCurrentImage();

        // Handle ChromeCast player.
        if (CastPlayer.castSession) {
            CastPlayer.loadCastMedia(song, position);
        // Handle MediaElement (HTML5) player.
        } else {
            loadMediaElementPlayer(song, position);
        }

        updateWindowTitle(song);

        <c:if test="${model.notify}">
        showNotification(song);
        </c:if>
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
            StompClient.send("/app/playqueues/${model.player.id}/reloadsearch", "");
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
        StompClient.send("/app/playqueues/${model.player.id}/play/mediafile", JSON.stringify({id: id}));
    }
    function onPlayShuffle(albumListType, offset, count, genre, decade) {
        StompClient.send("/app/playqueues/${model.player.id}/play/shuffle", JSON.stringify({albumListType: albumListType, offset: offset, count: count, genre: genre, decade: decade}));
    }
    function onPlayPlaylist(id, index) {
        StompClient.send("/app/playqueues/${model.player.id}/play/playlist", JSON.stringify({id: id, index: index}));
    }
    function onPlayInternetRadio(id, index) {
        StompClient.send("/app/playqueues/${model.player.id}/play/radio", JSON.stringify({id: id, index: index}));
    }
    function onPlayTopSong(id, index) {
        StompClient.send("/app/playqueues/${model.player.id}/play/topsongs", JSON.stringify({id: id, index: index}));
    }
    function onPlayPodcastChannel(id) {
        StompClient.send("/app/playqueues/${model.player.id}/play/podcastchannel", JSON.stringify({id: id}));
    }
    function onPlayPodcastEpisode(id) {
        StompClient.send("/app/playqueues/${model.player.id}/play/podcastepisode", JSON.stringify({id: id}));
    }
    function onPlayNewestPodcastEpisode(index) {
        StompClient.send("/app/playqueues/${model.player.id}/play/podcastepisode/newest", JSON.stringify({index: index}));
    }
    function onPlayStarred() {
        StompClient.send("/app/playqueues/${model.player.id}/play/starred", "");
    }
    function onPlayRandom(id, count) {
        StompClient.send("/app/playqueues/${model.player.id}/play/random", JSON.stringify({id: id, count: count}));
    }
    function onPlaySimilar(id, count) {
        StompClient.send("/app/playqueues/${model.player.id}/play/similar", JSON.stringify({id: id, count: count}));
    }
    function onAdd(id) {
        StompClient.send("/app/playqueues/${model.player.id}/add", JSON.stringify({ids: [id]}));
    }
    function onAddNext(id) {
        StompClient.send("/app/playqueues/${model.player.id}/add", JSON.stringify({ids: [id], index: currentSongIndex + 1}));
    }
    function onAddPlaylist(id) {
        StompClient.send("/app/playqueues/${model.player.id}/add/playlist", JSON.stringify({id: id}));
    }
    function onShuffle() {
        StompClient.send("/app/playqueues/${model.player.id}/shuffle", "");
    }
    function toggleStar(mediaFileId, imageId) {
        if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOnImage"/>") != -1) {
            $(imageId).attr("src", "<spring:theme code="ratingOffImage"/>");
            StompClient.send("/app/rate/mediafile/unstar", mediaFileId);
        }
        else if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOffImage"/>") != -1) {
            $(imageId).attr("src", "<spring:theme code="ratingOnImage"/>");
            StompClient.send("/app/rate/mediafile/star", mediaFileId);
        }
    }
    function onStar(index) {
        toggleStar(songs[index].id, '#starSong' + index);
    }
    function onStarCurrent() {
        onStar(currentSongIndex);
    }
    function onRemove(index) {
        StompClient.send("/app/playqueues/${model.player.id}/remove", JSON.stringify([index]));
    }
    function onRemoveSelected() {
        var indexes = [];
        for (var i = 0; i < songs.length; i++) {
            if ($("#songIndex" + i).is(":checked")) {
                indexes.push(i);
            }
        }
        StompClient.send("/app/playqueues/${model.player.id}/remove", JSON.stringify(indexes));
    }

    function onRearrange(indexes) {
        StompClient.send("/app/playqueues/${model.player.id}/rearrange", JSON.stringify(indexes));
    }
    function onToggleRepeat() {
        StompClient.send("/app/playqueues/${model.player.id}/toggleRepeat", "");
    }
    function onUndo() {
        StompClient.send("/app/playqueues/${model.player.id}/undo", "");
    }
    function onSortByTrack() {
        StompClient.send("/app/playqueues/${model.player.id}/sort", "TRACK");
    }
    function onSortByArtist() {
        StompClient.send("/app/playqueues/${model.player.id}/sort", "ARTIST");
    }
    function onSortByAlbum() {
        StompClient.send("/app/playqueues/${model.player.id}/sort", "ALBUM");
    }
    function onSavePlayQueue() {
        var positionMillis = $('#audioPlayer').get(0) ? Math.round(1000.0 * $('#audioPlayer').get(0).currentTime) : 0;
        StompClient.send("/app/playqueues/${model.player.id}/save", JSON.stringify({index: currentSongIndex, offset: positionMillis}));
    }
    function onLoadPlayQueue() {
        StompClient.send("/app/playqueues/${model.player.id}/play/saved", "");
    }
    function onSavePlaylist() {
        StompClient.send("/app/playlists/create/playqueue", "${model.player.id}");
    }
    function onAppendPlaylist() {
        // retrieve writable lists so we can open dialog to ask user which playlist to append to
        StompClient.send("/app/playlists/writable", "");
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

        var mediaFileIds = [];
        for (var i = 0; i < songs.length; i++) {
            if ($("#songIndex" + i).is(":checked")) {
                mediaFileIds.push(songs[i].id);
            }
        }

        StompClient.send("/app/playlists/files/append", JSON.stringify({id: playlistId, modifierIds: mediaFileIds}));
    }

    function playlistUpdatedCallback(playerId, toastMsg) {
        top.main.location.href = "playlist.view?id=" + playlistId;
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
            $("#songCountAndDuration").text("");
            $("#empty").show();
        } else {
            $("#songCountAndDuration").html(songs.length + " <fmt:message key="playlist2.songs"/> &ndash; " + playQueue.durationAsString);
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

            node.find("#trackNumber" + id).text(song.trackNumber);

            if (!internetRadioEnabled) {
                // Show star/remove buttons in all cases...
                node.find("#starSong" + id).show();
                node.find("#removeSong" + id).show();
                node.find("#songIndex" + id).show();

                // Show star rating
                if (song.starred) {
                    node.find("#starSong" + id).attr("src", "<spring:theme code='ratingOnImage'/>");
                } else {
                    node.find("#starSong" + id).attr("src", "<spring:theme code='ratingOffImage'/>");
                }
            } else {
                // ...except from when internet radio is playing.
                node.find("#starSong" + id).hide();
                node.find("#removeSong" + id).hide();
                node.find("#songIndex" + id).hide();
            }

            if (node.find("#currentImage" + id) && song.streamUrl == currentStreamUrl) {
                node.find("#currentImage" + id).show();
                if (isJavaJukeboxPresent()) {
                    updateJavaJukeboxPlayerControlBar(song);
                }
            }

            node.find("#title" + id).text(song.title).attr("title", song.title);
            node.find("#titleUrl" + id).text(song.title).attr("title", song.title).click(function () {onSkip(parseInt(this.id.substring(8)))});

            node.find("#album" + id).text(song.album).attr("title", song.album);
            node.find("#albumUrl" + id).attr("href", song.albumUrl);
            // Open external internet radio links in new windows
            if (internetRadioEnabled) {
                node.find("#albumUrl" + id).attr({
                    target: "_blank",
                    rel: "noopener noreferrer",
                });
            }
            node.find("#artist" + id).text(song.artist).attr("title", song.artist);
            node.find("#genre" + id).text(song.genre);
            node.find("#year" + id).text(parseInt(song.year) ? song.year : "");
            node.find("#bitRate" + id).text(song.bitRate);
            node.find("#duration" + id).text(song.durationAsString)
            node.find("#format" + id).text(song.format);
            node.find("#fileSize" + id).text(song.fileSize);

            node.addClass((id % 2 == 0) ? "bgcolor1" : "bgcolor2");

            // Note: show() method causes page to scroll to top.
            node.css("display", "table-row");
        }

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
        $(".currentImage").hide();
        $("#currentImage" + currentSongIndex).show();
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
        var result = "";
        for (var i = 0; i < songs.length; i++) {
            if ($("#songIndex" + i).is(":checked")) {
                result += "i=" + i + "&";
            }
        }
        return result;
    }

    function selectAll(b) {
        for (var i = 0; i < songs.length; i++) {
            if (b) {
                $("#songIndex" + i).attr("checked", "checked");
            } else {
                $("#songIndex" + i).removeAttr("checked");
            }
        }
    }

</script>

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

                </tr></table>
        </div>
    </c:otherwise>
</c:choose>


<h2 style="float:left"><fmt:message key="playlist.more.playlist"/></h2>
<h2 id="songCountAndDuration" style="float:right;padding-right:1em"></h2>
<div style="clear:both"></div>
<p id="empty"><em><fmt:message key="playlist.empty"/></em></p>

<table class="music indent" style="cursor:pointer">
    <tbody id="playlistBody">
        <tr id="pattern" style="display:none;margin:0;padding:0;border:0">
            <td class="fit">
                <img id="starSong" onclick="onStar(parseInt(this.id.substring(8)))" src="<spring:theme code='ratingOffImage'/>"
                     style="cursor:pointer;height:18px;" alt="" title=""></td>
            <td class="fit">
                <img id="removeSong" onclick="onRemove(parseInt(this.id.substring(10)))" src="<spring:theme code='removeImage'/>"
                     style="cursor:pointer; height:18px;" alt="<fmt:message key='playlist.remove'/>" title="<fmt:message key='playlist.remove'/>"></td>
            <td class="fit"><input type="checkbox" class="checkbox" id="songIndex"></td>

            <c:if test="${model.visibility.trackNumberVisible}">
                <td class="fit rightalign"><span class="detail" id="trackNumber">1</span></td>
            </c:if>

            <td class="truncate">
                <img id="currentImage" class="currentImage" src="<spring:theme code='currentImage'/>" alt="" style="display:none;padding-right: 0.5em">
                <c:choose>
                    <c:when test="${model.player.externalWithPlaylist}">
                        <span id="title" class="songTitle">Title</span>
                    </c:when>
                    <c:otherwise>
                        <span class="songTitle"><a id="titleUrl" href="javascript:void(0)">Title</a></span>
                    </c:otherwise>
                </c:choose>
            </td>

            <c:if test="${model.visibility.albumVisible}">
                <td class="truncate"><a id="albumUrl" target="main"><span id="album" class="detail">Album</span></a></td>
            </c:if>
            <c:if test="${model.visibility.artistVisible}">
                <td class="truncate"><span id="artist" class="detail">Artist</span></td>
            </c:if>
            <c:if test="${model.visibility.genreVisible}">
                <td class="truncate"><span id="genre" class="detail">Genre</span></td>
            </c:if>
            <c:if test="${model.visibility.yearVisible}">
                <td class="fit rightalign"><span id="year" class="detail">Year</span></td>
            </c:if>
            <c:if test="${model.visibility.formatVisible}">
                <td class="fit rightalign"><span id="format" class="detail">Format</span></td>
            </c:if>
            <c:if test="${model.visibility.fileSizeVisible}">
                <td class="fit rightalign"><span id="fileSize" class="detail">Format</span></td>
            </c:if>
            <c:if test="${model.visibility.durationVisible}">
                <td class="fit rightalign"><span id="duration" class="detail">Duration</span></td>
            </c:if>
            <c:if test="${model.visibility.bitRateVisible}">
                <td class="fit rightalign"><span id="bitRate" class="detail">Bit Rate</span></td>
            </c:if>
        </tr>
    </tbody>
</table>

<div style="height:3.2em"></div>

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
