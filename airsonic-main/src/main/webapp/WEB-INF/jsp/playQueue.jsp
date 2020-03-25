<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<%@ include file="table.jsp" %>
<script type="text/javascript" src="<c:url value='/script/mediaelement/mediaelement-and-player.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/playQueueCast.js'/>"></script>
<script src="<c:url value='/script/mediaelement/plugins/speed/speed.min.js'/>"></script>
<script src="<c:url value='/script/mediaelement/plugins/speed/speed-i18n.js'/>"></script>
<link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/speed/speed.min.css'/>">
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
    "use strict";
    
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
    
    var playQueue = {
        playerId: ${model.player.id},

        // These variables store the media player state, received via websockets in the
        // playQueueCallback function below.

        // List of songs (of type PlayQueueInfo.Entry)
        songs: [],

        // Stream URL of the media being played
        currentStreamUrl: null,

        currentSongIndex: -1,

        // Is autorepeat enabled?
        repeatStatus: 'OFF',

        // Is the "shuffle radio" playing? (More > Shuffle Radio)
        shuffleRadioEnabled: false,

        // Is the "internet radio" playing?
        internetRadioEnabled: false,

        // Is the play queue visible?
        isVisible: false,

        // Initialize the Cast player (ChromeCast support)
        CastPlayer: new CastPlayer(),

        musicTable: null,
        audioPlayer: null,

        init() {
            var ratingOnImage = "<spring:theme code='ratingOnImage'/>";
            var ratingOffImage = "<spring:theme code='ratingOffImage'/>";
            var pq = this;

            pq.musicTable = $("#playQueueMusic").DataTable( {
                deferRender: true,
                createdRow(row, data, dataIndex, cells) {
                    if (pq.currentSongIndex == dataIndex) {
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
                          if (pq.currentSongIndex != -1) {
                              dt.row(pq.currentSongIndex).show().draw(false);
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
                ajax(ajaxData, callback) {
                    for ( var i=0, len=pq.songs.length ; i<len ; i++ ) {
                      pq.songs[i].seq = i;
                    }
                    callback({data: pq.songs});
                },
                stripeClasses: ["bgcolor1", "bgcolor2"],
                columnDefs: [{ targets: "_all", orderable: false }],
                columns: [
                    { data: "seq", className: "detail fit", visible: true },
                    { data: "starred",
                      name: "starred",
                      className: "fit not-draggable",
                      render(starred, type) {
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
                      render(title, type, row) {
                          if (type == "display") {
                              var img = "<img class='currentImage' src=\"<spring:theme code='currentImage'/>\" alt='' style='display: none; padding-right: 0.5em' />";
                              if (!${model.player.externalWithPlaylist}) {
                                  return img + $("<a>").addClass("titleUrl").attr("href", "javascript:void(0)").attr("title", title).attr("alt", title).text(title)[0].outerHTML;
                              } else {
                                  return img + $("<span>").attr("title", title).attr("alt", title).text(title)[0].outerHTML;
                              }
                          }
                          return title;
                      }
                    },
                    { data: "album",
                      visible: ${model.visibility.albumVisible},
                      className: "detail truncate",
                      render(album, type, row) {
                          if (type == "display") {
                              return $("<a>").attr("href", row.albumUrl).attr("target", !pq.internetRadioEnabled ? "main" : "_blank").attr("rel", !pq.internetRadioEnabled ? "" : "noopener noreferrer").attr("title", album).attr("alt", album).text(album)[0].outerHTML;
                          }
                          return album;
                      }
                    },
                    { data: "artist",
                      className: "detail truncate",
                      visible: ${model.visibility.artistVisible},
                      render(artist, type) {
                          if (type == "display") {
                              return $("<span>").attr("title", artist).attr("alt", artist).text(artist)[0].outerHTML;
                          }
                          return artist;
                      }
                    },
                    { data: "genre",
                      className: "detail truncate",
                      visible: ${model.visibility.genreVisible},
                      render(genre, type) {
                          if (type == "display") {
                              return $("<span>").attr("title", genre).attr("alt", genre).text(genre)[0].outerHTML;
                          }
                          return genre;
                      }
                    },
                    { data: "year", className: "detail fit rightalign", visible: ${model.visibility.yearVisible} },
                    { data: "format", className: "detail fit rightalign", visible: ${model.visibility.formatVisible} },
                    { data: "fileSize", className: "detail fit rightalign", visible: ${model.visibility.fileSizeVisible} },
                    { data: "durationAsString", className: "detail fit rightalign", visible: ${model.visibility.durationVisible} },
                    { data: "bitRate", className: "detail fit rightalign", visible: ${model.visibility.bitRateVisible} }
                ]
            } );

            $("#playQueueMusic tbody").on( "click", ".starSong", function () {
                pq.onStar(pq.musicTable.row( $(this).parents('tr') ).index());
            } );
            $("#playQueueMusic tbody").on( "click", ".removeSong", function () {
                pq.onRemove(pq.musicTable.row( $(this).parents('tr') ).index());
            } );
            $("#playQueueMusic tbody").on( "click", ".titleUrl", function () {
                pq.onSkip(pq.musicTable.row( $(this).parents('tr') ).index());
            } );
            pq.musicTable.on( "row-reordered", function (e, diff, edit) {
                pq.musicTable.one( "draw", function () {
                    pq.onRearrange(pq.musicTable.rows().indexes().toArray());
                });
            });

            $("#playQueueHeading").html("<h2><fmt:message key='playlist.more.playlist'/></h2>");

            top.StompClient.subscribe("playQueue.jsp", {
                // Now playing
                '/topic/nowPlaying/current/add'(msg) {
                    var nowPlayingInfo = JSON.parse(msg.body);
                    pq.onNowPlayingChanged(nowPlayingInfo);
                },
                '/app/nowPlaying/current'(msg) {
                    var nowPlayingInfos = JSON.parse(msg.body);
                    for (var i = 0, len = nowPlayingInfos.length; i < len; i++) {
                        if (pq.onNowPlayingChanged(nowPlayingInfos[i])) {
                            break;
                        }
                    }
                },

                // Playlists
                '/user/queue/playlists/writable'(msg) {
                    pq.playlistSelectionCallback(JSON.parse(msg.body));
                },
                '/user/queue/playlists/files/append'(msg) {
                    pq.playlistUpdatedCallback(JSON.parse(msg.body), "<fmt:message key='playlist.toast.appendtoplaylist'/>");
                },
                '/user/queue/playlists/create/playqueue'(msg) {
                    pq.playlistUpdatedCallback(JSON.parse(msg.body), "<fmt:message key='playlist.toast.saveasplaylist'/>");
                },

                // Playqueues
                '/user/queue/playqueues/${model.player.id}/playstatus'(msg) {
                    pq.playQueuePlayStatusCallback(JSON.parse(msg.body));
                },
                '/user/queue/playqueues/${model.player.id}/updated'(msg) {
                    pq.playQueueCallback(JSON.parse(msg.body));
                },
                '/user/queue/playqueues/${model.player.id}/skip'(msg) {
                    pq.playQueueSkipCallback(JSON.parse(msg.body));
                },
                '/user/queue/playqueues/${model.player.id}/save'(msg) {
                    $().toastmessage("showSuccessToast", "<fmt:message key='playlist.toast.saveplayqueue'/> (" + JSON.parse(msg.body) + ")");
                },
                '/user/queue/playqueues/${model.player.id}/repeat'(msg) {
                    pq.playQueueRepeatStatusCallback(JSON.parse(msg.body));
                },
                '/user/queue/playqueues/${model.player.id}/jukebox/gain'(msg) {
                    pq.jukeBoxGainCallback(JSON.parse(msg.body));
                },
                '/user/queue/playqueues/${model.player.id}/jukebox/position'(msg) {
                    pq.jukeBoxPositionCallback(JSON.parse(msg.body));
                },
                //one-time
                '/app/playqueues/${model.player.id}/get'(msg) {
                    pq.playQueueCallback(JSON.parse(msg.body));
                }
            });

            $("#dialog-select-playlist").dialog({resizable: true, height: 220, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.cancel"/>"() {
                        $(this).dialog("close");
                    }
                }});

            <c:if test="${model.player.web}">pq.createMediaElementPlayer();</c:if>
            <c:if test="${model.autoHide}">pq.initAutoHide();</c:if>
            pq.onTogglePlayQueue(${!model.autoHide});
        },

        // Show/hide play queue
        onHidePlayQueue() {
            var pq = this;
            $("#playQueueMusic_wrapper").hide('slide', {direction:"down"}, 50, function() {
                pq.isVisible = false;
                $(".playqueue-shown").hide();
                $(".playqueue-hidden").show();
            });
        },
        onShowPlayQueue() {
            var pq = this;
            $("#playQueueMusic_wrapper").show('slide', {direction:"down"}, 50, function() {
                pq.isVisible = true;
                $(".playqueue-shown").show();
                $(".playqueue-hidden").hide();
            });
        },
        onTogglePlayQueue(visible) {
            if (visible) {
                this.onShowPlayQueue();
            } else {
                this.onHidePlayQueue();
            }
        },
        initAutoHide() {
            var pq = this;
            $(".playqueue-container").mouseleave(function (event) {
                pq.onHidePlayQueue();
            });

            $(".playqueue-container").mouseenter(function () {
                pq.onShowPlayQueue();
            });
        },

        isJavaJukeboxPresent() {
            return $("#javaJukeboxPlayerControlBarContainer").length==1;
        },

        playQueuePlayStatusCallback(status) {
            if (this.isJavaJukeboxPresent()) {
                if (status == "PLAYING") {
                    this.javaJukeboxStartCallback();
                } else {
                    this.javaJukeboxStopCallback();
                }
            }
        },

        jukeBoxPositionCallback(pos) {
            if (this.isJavaJukeboxPresent()) {
                this.javaJukeboxPositionCallback(pos);
            }
        },
        jukeBoxGainCallback(gain) {
            $("#jukeboxVolume").slider("option", "value", Math.floor(gain * 100)); // update UI
            if (this.isJavaJukeboxPresent()) {
                this.javaJukeboxGainCallback(gain);
            }
        },
        onJukeboxVolumeChanged() {
            var value = parseInt($("#jukeboxVolume").slider("option", "value"));
            top.StompClient.send("/app/playqueues/${model.player.id}/jukebox/gain", value / 100);
        },
        onCastVolumeChanged() {
            var value = parseInt($("#castVolume").slider("option", "value"));
            this.CastPlayer.setCastVolume(value / 100, false);
        },

        /**
         * Increase or decrease volume by a certain amount
         *
         * @param gain amount to add or remove from the current volume
         */
        onGainAdd(gain) {
            if (this.CastPlayer.castSession) {
                var volume = parseInt($("#castVolume").slider("option", "value")) + gain;
                if (volume > 100) volume = 100;
                if (volume < 0) volume = 0;
                this.CastPlayer.setCastVolume(volume / 100, false);
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
        },

        onNowPlayingChanged(nowPlayingInfo) {
            if (nowPlayingInfo != null && nowPlayingInfo.streamUrl != this.currentStreamUrl && nowPlayingInfo.playerId == ${model.player.id}) {
            <c:if test="${not model.player.web}">
                // TODO this should be keying off skip callbacks (and skip callbacks should be getting emitted)
                // otherwise there is an issue with the same song appearing multiple times on the playqueue (we'll always select the first)
                this.currentStreamUrl = nowPlayingInfo.streamUrl;
                this.currentSongIndex = this.getCurrentSongIndex();

                this.playQueueSkipCallback({index: this.currentSongIndex, offset: 0});
            </c:if>
                return true;
            }
            return false;
        },

        onEnded() {
            this.onNext(this.repeatStatus);
        },

        createMediaElementPlayer() {
            // Manually run MediaElement.js initialization.
            this.audioPlayer = new MediaElementPlayer("audioPlayer", {
                alwaysShowControls: true,
                enableKeyboard: false,
                useDefaultControls: true,
                features: ["speed"],
                defaultSpeed: "1.00",
                speeds: ["8.00", "2.00", "1.50", "1.25", "1.00", "0.75", "0.5"],
                success(mediaElement, originalNode, instance) {
                    // "hack" html5 renderer and reinitialize speed
                    instance.media.rendererName = "html5";
                    instance.buildspeed(instance, instance.getElement(instance.controls), instance.getElement(instance.layers), instance.media);
                }
            });

            // Once playback reaches the end, go to the next song, if any.
            $('#audioPlayer').on("ended", () => this.onEnded());
        },

        onClear() {
            var ok = true;
        <c:if test="${model.partyMode}">
            ok = confirm("<fmt:message key="playlist.confirmclear"/>");
        </c:if>
            if (ok) {
                top.StompClient.send("/app/playqueues/${model.player.id}/clear", "");
            }
        },

        /**
         * Start/resume playing from the current playlist
         */
        onStart() {
            if (this.CastPlayer.castSession) {
                this.CastPlayer.playCast();
            } else if ($('#audioPlayer').get(0)) {
                if ($('#audioPlayer').get(0).src) {
                    $('#audioPlayer').get(0).play();  // Resume playing if the player was paused
                } else {
                    this.onSkip(0);  // Start the first track if the player was not yet loaded
                }
            } else {
                top.StompClient.send("/app/playqueues/${model.player.id}/start", "");
            }
        },

        /**
         * Pause playing
         */
        onStop() {
            if (this.CastPlayer.castSession) {
                this.CastPlayer.pauseCast();
            } else if ($('#audioPlayer').get(0)) {
                $('#audioPlayer').get(0).pause();
            } else {
                top.StompClient.send("/app/playqueues/${model.player.id}/stop", "");
            }
        },

        /**
         * Toggle play/pause
         *
         * FIXME: Only works for the Web player for now
         */
        onToggleStartStop() {
            if (this.CastPlayer.castSession) {
                var playing = this.CastPlayer.mediaSession && this.CastPlayer.mediaSession.playerState == chrome.cast.media.PlayerState.PLAYING;
                if (playing) {
                    this.onStop();
                } else {
                    this.onStart();
                }
            } else if ($('#audioPlayer').get(0)) {
                var playing = $("#audioPlayer").get(0).paused != null && !$("#audioPlayer").get(0).paused;
                if (playing) {
                    this.onStop();
                } else {
                    this.onStart();
                }
            } else {
                top.StompClient.send("/app/playqueues/${model.player.id}/toggleStartStop", "");
            }
        },

        playQueueSkipCallback(location) {
            if (location.index < 0 || location.index >= this.songs.length) {
                return;
            }

            var song = this.songs[location.index];
            this.currentStreamUrl = song.streamUrl;
            this.currentSongIndex = location.index;
            this.updateCurrentImage();

          <c:choose>
          <c:when test="${model.player.web}">
            this.webSkip(song, location.offset / 1000);
          </c:when>
          <c:otherwise>
            if (this.isJavaJukeboxPresent()) {
                this.updateJavaJukeboxPlayerControlBar(song, location.offset / 1000);
            }
          </c:otherwise>
          </c:choose>

            this.updateWindowTitle(song);

          <c:if test="${model.notify}">
            this.showNotification(song);
          </c:if>
        },

        onSkip(index, offset) {
          <c:choose>
          <c:when test="${model.player.web}">
            this.playQueueSkipCallback({index: index, offset: offset});
          </c:when>
          <c:otherwise>
            top.StompClient.send("/app/playqueues/${model.player.id}/skip", JSON.stringify({index: index, offset: offset}));
          </c:otherwise>
          </c:choose>
        },

        webSkip(song, position) {
            // Handle ChromeCast player.
            if (this.CastPlayer.castSession) {
                this.CastPlayer.loadCastMedia(song, position);
            // Handle MediaElement (HTML5) player.
            } else {
                this.loadMediaElementPlayer(song, position);
            }
        },

        loadMediaElementPlayer(song, position) {
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
        },

        onNext(repeatStatus) {
            var index = this.currentSongIndex;
            if (this.shuffleRadioEnabled && (index + 1) >= this.songs.length) {
                top.StompClient.send("/app/playqueues/${model.player.id}/reloadsearch", "");
            } else if (repeatStatus == 'TRACK') {
                this.onSkip(index);
            } else {
                index = index + 1;
                if (repeatStatus == 'QUEUE') {
                    index = index % this.songs.length;
                }
                this.onSkip(index);
            }
        },
        onPrevious() {
            this.onSkip(this.currentSongIndex - 1);
        },
        onPlay(id) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/mediafile", JSON.stringify({id: id}));
        },
        onPlayShuffle(albumListType, offset, count, genre, decade) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/shuffle", JSON.stringify({albumListType: albumListType, offset: offset, count: count, genre: genre, decade: decade}));
        },
        onPlayPlaylist(id, index) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/playlist", JSON.stringify({id: id, index: index}));
        },
        onPlayInternetRadio(id, index) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/radio", JSON.stringify({id: id, index: index}));
        },
        onPlayTopSong(id, index) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/topsongs", JSON.stringify({id: id, index: index}));
        },
        onPlayPodcastChannel(id) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/podcastchannel", JSON.stringify({id: id}));
        },
        onPlayPodcastEpisode(id) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/podcastepisode", JSON.stringify({id: id}));
        },
        onPlayNewestPodcastEpisode(index) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/podcastepisode/newest", JSON.stringify({index: index}));
        },
        onPlayStarred() {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/starred", "");
        },
        onPlayRandom(id, count) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/random", JSON.stringify({id: id, count: count}));
        },
        onPlaySimilar(id, count) {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/similar", JSON.stringify({id: id, count: count}));
        },
        onAdd(id) {
            top.StompClient.send("/app/playqueues/${model.player.id}/add", JSON.stringify({ids: [id]}));
        },
        onAddNext(id) {
            top.StompClient.send("/app/playqueues/${model.player.id}/add", JSON.stringify({ids: [id], index: this.currentSongIndex + 1}));
        },
        onAddPlaylist(id) {
            top.StompClient.send("/app/playqueues/${model.player.id}/add/playlist", JSON.stringify({id: id}));
        },
        onShuffle() {
            top.StompClient.send("/app/playqueues/${model.player.id}/shuffle", "");
        },
        onStar(index) {
            this.songs[index].starred = !this.songs[index].starred;

            if (this.songs[index].starred) {
                top.StompClient.send("/app/rate/mediafile/star", this.songs[index].id);
            } else {
                top.StompClient.send("/app/rate/mediafile/unstar", this.songs[index].id);
            }
            this.musicTable.cell(index, "starred:name").invalidate().draw();
        },
        onStarCurrent() {
            this.onStar(this.currentSongIndex);
        },
        onRemove(index) {
            top.StompClient.send("/app/playqueues/${model.player.id}/remove", JSON.stringify([index]));
        },
        onRemoveSelected() {
            top.StompClient.send("/app/playqueues/${model.player.id}/remove", JSON.stringify(this.musicTable.rows({ selected: true }).indexes().toArray()));
        },

        onRearrange(indexes) {
            top.StompClient.send("/app/playqueues/${model.player.id}/rearrange", JSON.stringify(indexes));
        },
        onToggleRepeat() {
            top.StompClient.send("/app/playqueues/${model.player.id}/toggleRepeat", "");
        },
        onUndo() {
            top.StompClient.send("/app/playqueues/${model.player.id}/undo", "");
        },
        onSortByTrack() {
            top.StompClient.send("/app/playqueues/${model.player.id}/sort", "TRACK");
        },
        onSortByArtist() {
            top.StompClient.send("/app/playqueues/${model.player.id}/sort", "ARTIST");
        },
        onSortByAlbum() {
            top.StompClient.send("/app/playqueues/${model.player.id}/sort", "ALBUM");
        },
        onSavePlayQueue() {
            var positionMillis = $('#audioPlayer').get(0) ? Math.round(1000.0 * $('#audioPlayer').get(0).currentTime) : 0;
            top.StompClient.send("/app/playqueues/${model.player.id}/save", JSON.stringify({index: this.currentSongIndex, offset: positionMillis}));
        },
        onLoadPlayQueue() {
            top.StompClient.send("/app/playqueues/${model.player.id}/play/saved", "");
        },
        onSavePlaylist() {
            top.StompClient.send("/app/playlists/create/playqueue", "${model.player.id}");
        },
        onAppendPlaylist() {
            // retrieve writable lists so we can open dialog to ask user which playlist to append to
            top.StompClient.send("/app/playlists/writable", "");
        },
        playlistSelectionCallback(playlists) {
            $("#dialog-select-playlist-list").empty();
            var pq = this;
            for (var i = 0; i < playlists.length; i++) {
                var playlist = playlists[i];
                $("<p>").addClass("dense").append(
                    $("<b>").append(
                        $("<a>").attr("href","#").attr("onclick", "playQueue.appendPlaylist(" + playlist.id + ")").text(playlist.name)))
                .appendTo("#dialog-select-playlist-list");
            }
            $("#dialog-select-playlist").dialog("open");
        },
        appendPlaylist(playlistId) {
            $("#dialog-select-playlist").dialog("close");

            var mediaFileIds = this.musicTable.rows({selected:true}).data().map(function(d) { return d.id; }).toArray();

            top.StompClient.send("/app/playlists/files/append", JSON.stringify({id: playlistId, modifierIds: mediaFileIds}));
        },

        playlistUpdatedCallback(playlistId, toastMsg) {
            if (!top.main.location.href.endsWith("playlist.view?id=" + playlistId)) {
                // change page
                top.main.location.href = "playlist.view?id=" + playlistId;
            }
            $().toastmessage("showSuccessToast", toastMsg);
        },

        playQueueRepeatStatusCallback(incomingStatus) {
            this.repeatStatus = incomingStatus;
            if ($("#toggleRepeat").length != 0) {
                if (this.shuffleRadioEnabled) {
                    $("#toggleRepeat").html("<fmt:message key="playlist.repeat_radio"/>");
                } else if (this.repeatStatus == 'QUEUE') {
                    $("#toggleRepeat").attr('src', '<spring:theme code="repeatAll"/>');
                    $("#toggleRepeat").attr('alt', 'Repeat All/Queue');
                } else if (this.repeatStatus == 'OFF') {
                    $("#toggleRepeat").attr('src', '<spring:theme code="repeatOff"/>');
                    $("#toggleRepeat").attr('alt', 'Repeat Off');
                } else if (this.repeatStatus == 'TRACK') {
                    $("#toggleRepeat").attr('src', '<spring:theme code="repeatOne"/>');
                    $("#toggleRepeat").attr('alt', 'Repeat One/Track');
                }
            }
        },

        playQueueCallback(playQueue) {
            this.songs = playQueue.entries;
            this.shuffleRadioEnabled = playQueue.shuffleRadioEnabled;
            this.internetRadioEnabled = playQueue.internetRadioEnabled;

            // If an internet radio has no sources, display a message to the user.
            if (this.internetRadioEnabled && this.songs.length == 0) {
                top.main.$().toastmessage("showErrorToast", "<fmt:message key="playlist.toast.radioerror"/>");
                this.onStop();
            }

            if ($("#start").length != 0) {
                $("#start").toggle(!playQueue.stopEnabled);
                $("#stop").toggle(playQueue.stopEnabled);
            }

            this.playQueueRepeatStatusCallback(playQueue.repeatStatus);

            // Disable some UI items if internet radio is playing
            $("select#moreActions #loadPlayQueue").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #savePlayQueue").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #savePlaylist").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #downloadPlaylist").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #sharePlaylist").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #sortByTrack").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #sortByAlbum").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #sortByArtist").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #selectAll").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #selectNone").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #removeSelected").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #download").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #appendPlaylist").prop("disabled", this.internetRadioEnabled);
            $("#shuffleQueue").toggleLink(!this.internetRadioEnabled);
            $("#repeatQueue").toggleLink(!this.internetRadioEnabled);
            $("#undoQueue").toggleLink(!this.internetRadioEnabled);

            if (this.songs.length == 0) {
                $("#playQueueInfo").text("");
            } else {
                $("#playQueueInfo").html("&nbsp;|&nbsp;" + this.songs.length + " <fmt:message key='playlist2.songs'/> &nbsp;|&nbsp;" + playQueue.durationAsString);
            }

            if (this.internetRadioEnabled) {
                this.musicTable.column("starred:name").visible(false, false);
                this.musicTable.column("remove:name").visible(false, false);
                this.musicTable.column("songcheckbox:name").visible(false, false);
            } else {
                this.musicTable.column("starred:name").visible(true, false);
                this.musicTable.column("remove:name").visible(true, false);
                this.musicTable.column("songcheckbox:name").visible(true, false);
            }

            this.currentSongIndex = this.getCurrentSongIndex();
            this.musicTable.ajax.reload().columns.adjust();
            this.updateCurrentImage();

            if (playQueue.sendM3U) {
                parent.frames.main.location.href="play.m3u?";
            }

            this.jukeBoxGainCallback(playQueue.gain);
        },

        updateWindowTitle(song) {
            top.document.title = song.title + " - " + song.artist + " - Airsonic";
        },

        showNotification(song) {
            if (!("Notification" in window)) {
                return;
            }
            if (Notification.permission === "granted") {
                this.createNotification(song);
            } else if (Notification.permission !== 'denied') {
                Notification.requestPermission(function (permission) {
                    Notification.permission = permission;
                    if (permission === "granted") {
                        this.createNotification(song);
                    }
                });
            }
        },
        createNotification(song) {
            var n = new Notification(song.title, {
                tag: "airsonic",
                body: song.artist + " - " + song.album,
                icon: "coverArt.view?id=" + song.id + "&size=110"
            });
            n.onshow = function() {
                setTimeout(function() {n.close()}, 5000);
            }
        },

        updateCurrentImage() {
            $(this.musicTable.rows().nodes()).removeClass("currently-playing").find(".currentImage").hide();
            $(this.musicTable.row(this.currentSongIndex).node()).addClass("currently-playing").find(".currentImage").show();
        },

        getCurrentSongIndex() {
            for (var i = 0, len = this.songs.length; i < len; i++) {
                if (this.songs[i].streamUrl == this.currentStreamUrl) {
                    return i;
                }
            }
            return -1;
        },

        <!-- actionSelected() is invoked when the users selects from the "More actions..." combo box. -->
        actionSelected(id) {
            var selectedIndexes = this.getSelectedIndexes();
            if (id == "top") {
                return;
            } else if (id == "savePlayQueue") {
                this.onSavePlayQueue();
            } else if (id == "loadPlayQueue") {
                this.onLoadPlayQueue();
            } else if (id == "savePlaylist") {
                this.onSavePlaylist();
            } else if (id == "downloadPlaylist") {
                location.href = "download.view?player=${model.player.id}";
            } else if (id == "sharePlaylist") {
                parent.frames.main.location.href = "createShare.view?player=${model.player.id}&" + selectedIndexes;
            } else if (id == "sortByTrack") {
                this.onSortByTrack();
            } else if (id == "sortByArtist") {
                this.onSortByArtist();
            } else if (id == "sortByAlbum") {
                this.onSortByAlbum();
            } else if (id == "selectAll") {
                this.selectAll(true);
            } else if (id == "selectNone") {
                this.selectAll(false);
            } else if (id == "removeSelected") {
                this.onRemoveSelected();
            } else if (id == "download" && selectedIndexes != "") {
                location.href = "download.view?player=${model.player.id}&" + selectedIndexes;
            } else if (id == "appendPlaylist" && selectedIndexes != "") {
                this.onAppendPlaylist();
            }
            $("#moreActions").prop("selectedIndex", 0);
        },

        getSelectedIndexes() {
            return this.musicTable.rows({ selected: true }).indexes().map(function(i) { return "i=" + i; }).join("&");
        },

        selectAll(b) {
            if (b) {
                this.musicTable.rows().select();
            } else {
                this.musicTable.rows().deselect();
            }
        }
    };

    $(document).ready(() => playQueue.init());
</script>

<table class="music indent hover nowrap stripe compact hide-table-header" id="playQueueMusic" style="cursor:pointer; width: 100%;"></table>

<c:choose>
    <c:when test="${model.player.javaJukebox}">
        <div id="javaJukeboxPlayerControlBarContainer">
            <%@ include file="javaJukeboxPlayerControlBar.jspf" %>
        </div>
    </c:when>
    <c:otherwise>
        <div class="bgcolor2" style="width:100%; padding-top:10px; z-index:1">
            <table style="white-space:nowrap; margin-bottom:0;">
                <tr style="white-space:nowrap;">
                    <c:if test="${model.user.settingsRole and model.players.size() > 1}">
                        <td style="padding-right: 5px"><select name="player" onchange="location='playQueue.view?player=' + options[selectedIndex].value;">
                            <c:forEach items="${model.players}" var="player">
                                <option ${player.id eq model.player.id ? "selected" : ""} value="${player.id}">${player.shortDescription}</option>
                            </c:forEach>
                        </select></td>
                    </c:if>
                    <c:if test="${model.player.web}">
                        <td>
                            <div id="player" style="width:340px; height:40px">
                                <audio id="audioPlayer" width="340px" height="40px" tabindex="-1" />
                            </div>
                            <div id="castPlayer" style="display: none">
                                <div style="float:left">
                                    <img alt="Play" id="castPlay" src="<spring:theme code='castPlayImage'/>" onclick="playQueue.CastPlayer.playCast()" style="cursor:pointer">
                                    <img alt="Pause" id="castPause" src="<spring:theme code='castPauseImage'/>" onclick="playQueue.CastPlayer.pauseCast()" style="cursor:pointer; display:none">
                                    <img alt="Mute on" id="castMuteOn" src="<spring:theme code='volumeImage'/>" onclick="playQueue.CastPlayer.castMuteOn()" style="cursor:pointer">
                                    <img alt="Mute off" id="castMuteOff" src="<spring:theme code='muteImage'/>" onclick="playQueue.CastPlayer.castMuteOff()" style="cursor:pointer; display:none">
                                </div>
                                <div style="float:left">
                                    <div id="castVolume" style="width:80px;height:4px;margin-left:10px;margin-right:10px;margin-top:8px"></div>
                                    <script type="text/javascript">
                                        $("#castVolume").slider({max: 100, value: 50, animate: "fast", range: "min"});
                                        $("#castVolume").on("slidestop", playQueue.onCastVolumeChanged);
                                    </script>
                                </div>
                            </div>
                        </td>
                        <td>
                            <img alt="Cast on" id="castOn" src="<spring:theme code='castIdleImage'/>" onclick="playQueue.CastPlayer.launchCastApp()" style="cursor:pointer; display:none">
                            <img alt="Cast off" id="castOff" src="<spring:theme code='castActiveImage'/>" onclick="playQueue.CastPlayer.stopCastApp()" style="cursor:pointer; display:none">
                        </td>
                    </c:if>

                    <c:if test="${model.user.streamRole and not model.player.web}">
                        <td>
                            <img alt="Start" id="start" src="<spring:theme code='castPlayImage'/>" onclick="playQueue.onStart()" style="cursor:pointer">
                            <img alt="Stop" id="stop" src="<spring:theme code='castPauseImage'/>" onclick="playQueue.onStop()" style="cursor:pointer; display:none">
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
                                $("#jukeboxVolume").on("slidestop", playQueue.onJukeboxVolumeChanged);
                            </script>
                        </td>
                    </c:if>

                    <c:if test="${model.player.web}">
                        <td><span class="header">
                            <img src="<spring:theme code='backImage'/>" alt="Play previous" title="Play previous" onclick="playQueue.onPrevious()" style="cursor:pointer"></span>
                        </td>
                        <td><span class="header">
                            <img src="<spring:theme code='forwardImage'/>" alt="Play next" title="Play next" onclick="playQueue.onNext('OFF')" style="cursor:pointer"></span> |
                        </td>
                    </c:if>

                    <td style="white-space:nowrap;">
                      <span class="header">
                        <a href="javascript:playQueue.onClear()" class="player-control">
                            <img src="<spring:theme code='clearImage'/>" alt="Clear playlist" title="Clear playlist" style="cursor:pointer; height:18px">
                        </a>
                      </span> |</td>

                    <td style="white-space:nowrap;">
                      <span class="header">
                        <a href="javascript:playQueue.onShuffle()" id="shuffleQueue" class="player-control">
                            <img src="<spring:theme code='shuffleImage'/>" alt="Shuffle" title="Shuffle" style="cursor:pointer; height:18px">
                        </a>
                      </span> |</td>

                    <c:if test="${model.player.web or model.player.jukebox or model.player.external}">
                        <td style="white-space:nowrap;">
                          <span class="header">
                            <a href="javascript:playQueue.onToggleRepeat()" id="repeatQueue" class="player-control">
                              <img id="toggleRepeat" src="<spring:theme code='repeatOff'/>" alt="Toggle repeat" title="Toggle repeat" style="cursor:pointer; height:18px">
                            </a>
                          </span> |</td>
                    </c:if>

                    <td style="white-space:nowrap;">
                      <span class="header">
                        <a href="javascript:playQueue.onUndo()" id="undoQueue" class="player-control">
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

                    <td style="white-space:nowrap;"><select id="moreActions" onchange="playQueue.actionSelected(this.options[selectedIndex].id)">
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
                      <a href="javascript:playQueue.onTogglePlayQueue(!playQueue.isVisible)">
                        <img class="playqueue-shown" src="<spring:theme code='playQueueHide'/>" alt="Hide play queue" title="Hide play queue" style="cursor:pointer; height:18px;"/>
                        <img class="playqueue-hidden" src="<spring:theme code='playQueueShow'/>" alt="Show play queue" title="Show play queue" style="cursor:pointer; height:18px; display: none;"/>
                      </a>
                    </td>
                    </c:if>

                </tr></table>
        </div>
    </c:otherwise>
</c:choose>

<div id="dialog-select-playlist" title="<fmt:message key='main.addtoplaylist.title'/>" style="display: none;">
    <p><fmt:message key="main.addtoplaylist.text"/></p>
    <div id="dialog-select-playlist-list"></div>
</div>

<script type="text/javascript">
    window['__onGCastApiAvailable'] = function(isAvailable) {
        if (isAvailable) {
            playQueue.CastPlayer.initializeCastPlayer();
        }
    };
</script>
<script type="text/javascript" src="https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1"></script>
