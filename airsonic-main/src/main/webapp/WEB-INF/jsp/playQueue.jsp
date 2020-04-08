<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ include file="head.jsp" %>
<%@ include file="jquery.jsp" %>
<%@ include file="table.jsp" %>
<script type="text/javascript" src="<c:url value='/script/mediaelement/mediaelement-and-player.min.js'/>"></script>
<script src="<c:url value='/script/mediaelement/plugins/speed/speed.min.js'/>"></script>
<script src="<c:url value='/script/mediaelement/plugins/speed/speed-i18n.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/playQueueCast.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/playQueue/javaJukeboxPlayerControlBar.js'/>"></script>
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
        // Changed when player is changed
        player: {},

        // These variables store the media player state, received via websockets in the
        // playQueueCallback function below.

        // List of songs (of type PlayQueueInfo.Entry)
        songs: [],

        // Stream URL of the media being played
        currentStreamUrl: null,

        currentSongIndex: -1,

        // Is autorepeat enabled?
        repeatStatus: 'OFF',

        // Play status on the server
        playStatus: 'PLAYING',

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
                    var rowNode = $(row);
                    if (pq.currentSongIndex == dataIndex) {
                        rowNode.addClass("currently-playing").find(".currentImage").show();
                    }

                    if (rowNode.hasClass("selected")) {
                        rowNode.find(".songIndex input").prop("checked", true);
                    }
                },
                ordering: true,
                order: [],
                orderFixed: [ 0, 'asc' ],
                orderMulti: false,
                pageLength: ${model.initialPaginationSize},
              <c:set var="paginationaddition" value="${fn:contains(' 10 20 50 100 -1', ' '.concat(model.initialPaginationSize)) ? '' : ', '.concat(model.initialPaginationSize)}" />
                lengthMenu: [[10, 20, 50, 100, -1 ${paginationaddition}], [10, 20, 50, 100, "All" ${paginationaddition}]],
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
                              if (playQueue.player.tech != "EXTERNAL_WITH_PLAYLIST") {
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
                          if (type == "display" && album != null) {
                              return $("<a>").attr("href", row.albumUrl).attr("target", !pq.internetRadioEnabled ? "main" : "_blank").attr("rel", !pq.internetRadioEnabled ? "" : "noopener noreferrer").attr("title", album).attr("alt", album).text(album)[0].outerHTML;
                          }
                          return album;
                      }
                    },
                    { data: "artist",
                      className: "detail truncate",
                      visible: ${model.visibility.artistVisible},
                      render(artist, type) {
                          if (type == "display" && artist != null) {
                              return $("<span>").attr("title", artist).attr("alt", artist).text(artist)[0].outerHTML;
                          }
                          return artist;
                      }
                    },
                    { data: "genre",
                      className: "detail truncate",
                      visible: ${model.visibility.genreVisible},
                      render(genre, type) {
                          if (type == "display" && genre != null) {
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

            pq.musicTable.on( 'select', function ( e, dt, type, indexes ) {
                pq.musicTable.cells( indexes, "songcheckbox:name" ).nodes().to$().find("input").prop("checked", true);
            } );
            pq.musicTable.on( 'deselect', function ( e, dt, type, indexes ) {
                pq.musicTable.cells( indexes, "songcheckbox:name" ).nodes().to$().find("input").prop("checked", false);
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

                // Players
                '/user/queue/players/updated'(msg) {
                    pq.onPlayerUpdated(JSON.parse(msg.body));
                },
                '/user/queue/players/created'(msg) {
                    pq.onPlayerCreated(JSON.parse(msg.body));
                },
                '/topic/players/deleted'(msg) {
                    pq.onPlayerDeleted(JSON.parse(msg.body));
                }
            });

            $("#dialog-select-playlist").dialog({resizable: true, height: 220, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.cancel"/>"() {
                        $(this).dialog("close");
                    }
                }});

            pq.createMediaElementPlayer();
            JavaJukeBox.initJavaJukeboxPlayerControlBar();
            <c:if test="${model.autoHide}">pq.initAutoHide();</c:if>
            pq.onTogglePlayQueue(${!model.autoHide});

            // load default player
            pq.onPlayerChanged(null);
        },

        onPlayerChanged(playerId) {
            var pq = this;
            var playerUrl = "<c:url value='/playQueue/player'/>";
            if (playerId) {
                playerUrl = playerUrl + "?player=" + playerId;
            }
            // use ajax to set player cookie (not possible via websocket)
            $.ajax(playerUrl, {
              method: "GET",
              success: player => {
                  pq.onStop();
                  // emulate callback because callbacks for the player will be deregistered by the time call returns
                  // TODO if error happens, we won't receive it
                  pq.playQueuePlayStatusCallback("STOPPED");

                  //reset everything
                  $(".player-tech").hide();
                  pq.unsubscribePlayerSpecificCallbacks();
                  pq.currentStreamUrl = null;
                  pq.currentSongIndex = -1;
                  if (pq.player.tech == 'JAVA_JUKEBOX') {
                      JavaJukeBox.reset();
                  } else if (pq.player.tech == 'WEB') {
                      if (this.CastPlayer.castSession) {
                          pq.CastPlayer.stopCastApp();
                      }
                      // no need to change src on audioPlayer because start button will see currentSongIndex
                      //pq.audioPlayer.setSrc(null);
                  }

                  //switch to new player
                  $("#playerSelector").val(player.id);
                  pq.player = player;
                  $(".player-tech-" + player.tech.toLowerCase()).show();
                  if (player.tech == 'JAVA_JUKEBOX') {
                      //show regular jukebox controls also
                      $(".player-tech-jukebox").show();
                  }
                  if (player.tech != 'WEB') {
                      $(".player-tech-non-web").show();
                  }
                  pq.subscribePlayerSpecificCallbacks();
              }
            });
        },

        playerSpecificCallbacks: {},

        unsubscribePlayerSpecificCallbacks() {
            for (var topic in this.playerSpecificCallbacks) {
                top.StompClient.unsubscribe(topic, "playQueue.jsp");
                delete this.playerSpecificCallbacks[topic];
            }
        },

        subscribePlayerSpecificCallbacks() {
            if (this.player.id) {
                var pq = this;

                // Playqueues
                pq.playerSpecificCallbacks['/user/queue/playqueues/' + this.player.id + '/playstatus'] = function(msg) {
                    pq.playQueuePlayStatusCallback(JSON.parse(msg.body));
                };
                pq.playerSpecificCallbacks['/user/queue/playqueues/' + this.player.id + '/updated'] = function(msg) {
                    pq.playQueueCallback(JSON.parse(msg.body));
                };
                pq.playerSpecificCallbacks['/user/queue/playqueues/' + this.player.id + '/skip'] = function(msg) {
                    pq.playQueueSkipCallback(JSON.parse(msg.body));
                };
                pq.playerSpecificCallbacks['/user/queue/playqueues/' + this.player.id + '/save'] = function(msg) {
                    $().toastmessage("showSuccessToast", "<fmt:message key='playlist.toast.saveplayqueue'/> (" + JSON.parse(msg.body) + ")");
                };
                pq.playerSpecificCallbacks['/user/queue/playqueues/' + this.player.id + '/repeat'] = function(msg) {
                    pq.playQueueRepeatStatusCallback(JSON.parse(msg.body));
                };
                pq.playerSpecificCallbacks['/user/queue/playqueues/' + this.player.id + '/jukebox/gain'] = function(msg) {
                    pq.jukeBoxGainCallback(JSON.parse(msg.body));
                };
                pq.playerSpecificCallbacks['/user/queue/playqueues/' + this.player.id + '/jukebox/position'] = function(msg) {
                    pq.jukeBoxPositionCallback(JSON.parse(msg.body));
                };
                //one-time
                pq.playerSpecificCallbacks['/app/playqueues/' + this.player.id + '/get'] = function(msg) {
                    pq.playQueueCallback(JSON.parse(msg.body), true);
                };

                top.StompClient.subscribe("playQueue.jsp", pq.playerSpecificCallbacks);
            }
        },

        // Player updates
        onPlayerCreated(player) {
            // create node if necessary
            if ($("#playerSelector option[value='" + player.id + "']").length == 0) {
                $("#playerSelector").append($("<option>").val(player.id).text(player.description));
            }
            if (this.player.id == player.id) {
                $("#playerSelector").val(player.id);
            }
        },
        onPlayerUpdated(player) {
            this.onPlayerCreated(player);
            $("#playerSelector option[value='" + player.id + "']").replaceWith($("<option>").val(player.id).text(player.description));
            if (this.player.id == player.id) {
                $("#playerSelector").val(player.id);
            }
            if (this.player.id == player.id && this.player.tech != player.tech) {
                this.onPlayerChanged(player.id);
            }
        },
        onPlayerDeleted(id) {
            $("#playerSelector option[value='" + id + "']").remove();

            if (this.player.id == id) {
                this.onPlayerChanged(null);
            }
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

        jukeBoxPositionCallback(pos) {
            if (this.player.tech == 'JAVA_JUKEBOX') {
                JavaJukeBox.javaJukeboxPositionCallback(pos);
            }
        },
        jukeBoxGainCallback(gain) {
            $("#jukeboxVolume").slider("option", "value", Math.floor(gain * 100)); // update UI
        },
        onJukeboxVolumeChanged() {
            var value = parseInt($("#jukeboxVolume").slider("option", "value"));
            top.StompClient.send("/app/playqueues/" + this.player.id + "/jukebox/gain", value / 100);
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
            } else if (this.player.tech == 'WEB') {
                var volume = parseFloat(this.audioPlayer.volume)*100 + gain;
                if (volume > 100) volume = 100;
                if (volume < 0) volume = 0;
                this.audioPlayer.volume = volume / 100;
            } else {
                var volume = parseInt($("#jukeboxVolume").slider("option", "value")) + gain;
                if (volume > 100) volume = 100;
                if (volume < 0) volume = 0;
                top.StompClient.send("/app/playqueues/" + this.player.id + "/jukebox/gain", volume / 100);
                // UI updated at callback
            }
        },

        onNowPlayingChanged(nowPlayingInfo) {
            if (nowPlayingInfo != null && nowPlayingInfo.streamUrl != this.currentStreamUrl && nowPlayingInfo.playerId == this.player.id) {
                if (this.player.tech != 'WEB') {
                    // TODO this should be keying off skip callbacks (and skip callbacks should be getting emitted)
                    // otherwise there is an issue with the same song appearing multiple times on the playqueue (we'll always select the first)
                    this.currentStreamUrl = nowPlayingInfo.streamUrl;
                    this.currentSongIndex = this.getCurrentSongIndex();

                    this.playQueueSkipCallback({index: this.currentSongIndex, offset: 0});
                }
                return true;
            }
            return false;
        },

        onEnded() {
            this.onNext(this.repeatStatus);
        },

        createMediaElementPlayer() {
            var pq = this;
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

                    // Once playback reaches the end, go to the next song, if any.
                    $(mediaElement).on("ended", () => pq.onEnded());

                    // skip to first song if no src loaded
                    $(".mejs__controls .mejs__button.mejs__playpause-button.mejs__play button").on("click", () => {
                        if (!instance.src || pq.currentSongIndex == -1) {
                            pq.onSkip(0);
                            return false;
                        }
                    });
                }
            });
        },

        onClear() {
            var ok = true;
        <c:if test="${model.partyMode}">
            ok = confirm("<fmt:message key="playlist.confirmclear"/>");
        </c:if>
            if (ok) {
                top.StompClient.send("/app/playqueues/" + this.player.id + "/clear", "");
            }
        },

        playQueuePlayStatusCallback(status, nonWebOnly) {
            this.playStatus = status;
            if (status == "PLAYING") {
                $("#audioStart").hide();
                $("#audioStop").show();
                if (this.CastPlayer.castSession && !nonWebOnly) {
                    this.CastPlayer.playCast();
                } else if (this.player.tech == 'WEB' && !nonWebOnly) {
                    if (this.audioPlayer.src) {
                        this.audioPlayer.play();  // Resume playing if the player was paused
                    } else {
                        this.onSkip(0);  // Start the first track if the player was not yet loaded
                    }
                } else {
                    if (this.player.tech == 'JAVA_JUKEBOX') {
                        JavaJukeBox.javaJukeboxStartCallback();
                    }
                }
            } else {
                $("#audioStop").hide();
                $("#audioStart").show();
                if (this.CastPlayer.castSession && !nonWebOnly) {
                    this.CastPlayer.pauseCast();
                } else if (this.player.tech == 'WEB' && !nonWebOnly) {
                    this.audioPlayer.pause();
                } else {
                    if (this.player.tech == 'JAVA_JUKEBOX') {
                        JavaJukeBox.javaJukeboxStopCallback();
                    }
                }
            }
        },

        /**
         * Start/resume playing from the current playlist
         */
        onStart() {
            // simulate immediate callback
            if (this.CastPlayer.castSession || this.player.tech == 'WEB') {
                this.playQueuePlayStatusCallback("PLAYING");
            } else {
                top.StompClient.send("/app/playqueues/" + this.player.id + "/start", "");
            }
        },

        /**
         * Pause playing
         */
        onStop() {
            // simulate immediate callback
            if (this.CastPlayer.castSession || this.player.tech == 'WEB') {
                this.playQueuePlayStatusCallback("STOPPED");
            } else {
                if (this.player.id) {
                    top.StompClient.send("/app/playqueues/" + this.player.id + "/stop", "");
                }
            }
        },

        /**
         * Toggle play/pause
         * TODO: Nobody calls this
         */
        onToggleStartStop() {
            if (this.CastPlayer.castSession) {
                var playing = this.CastPlayer.mediaSession && this.CastPlayer.mediaSession.playerState == chrome.cast.media.PlayerState.PLAYING;
                if (playing) {
                    this.onStop();
                } else {
                    this.onStart();
                }
            } else if (this.player.tech == 'WEB') {
                if (this.audioPlayer.paused) {
                    this.onStart();
                } else {
                    this.onStop();
                }
            } else {
                top.StompClient.send("/app/playqueues/" + this.player.id + "/toggleStartStop", "");
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

            if (this.player.tech == 'WEB') {
                this.webSkip(song, location.offset / 1000);
            } else if (this.player.tech == 'JAVA_JUKEBOX') {
                JavaJukeBox.updateJavaJukeboxPlayerControlBar(song, location.offset / 1000);
            }

            this.updateWindowTitle(song);

          <c:if test="${model.notify}">
            this.showNotification(song);
          </c:if>
        },

        onSkip(index, offset) {
            if (this.player.tech == 'WEB') {
                this.playQueueSkipCallback({index: index, offset: offset});
            } else if (this.player.tech != 'EXTERNAL_WITH_PLAYLIST') {
                top.StompClient.send("/app/playqueues/" + this.player.id + "/skip", JSON.stringify({index: index, offset: offset}));
            }
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
            var player = this.audioPlayer;

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
                top.StompClient.send("/app/playqueues/" + this.player.id + "/reloadsearch", "");
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
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/mediafile", JSON.stringify({id: id}));
        },
        onPlayShuffle(albumListType, offset, count, genre, decade) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/shuffle", JSON.stringify({albumListType: albumListType, offset: offset, count: count, genre: genre, decade: decade}));
        },
        onPlayPlaylist(id, index) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/playlist", JSON.stringify({id: id, index: index}));
        },
        onPlayInternetRadio(id, index) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/radio", JSON.stringify({id: id, index: index}));
        },
        onPlayTopSong(id, index) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/topsongs", JSON.stringify({id: id, index: index}));
        },
        onPlayPodcastChannel(id) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/podcastchannel", JSON.stringify({id: id}));
        },
        onPlayPodcastEpisode(id) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/podcastepisode", JSON.stringify({id: id}));
        },
        onPlayNewestPodcastEpisode(index) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/podcastepisode/newest", JSON.stringify({index: index}));
        },
        onPlayStarred() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/starred", "");
        },
        onPlayRandom(id, count) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/random", JSON.stringify({id: id, count: count}));
        },
        onPlaySimilar(id, count) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/similar", JSON.stringify({id: id, count: count}));
        },
        onAdd(id) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/add", JSON.stringify({ids: [id]}));
        },
        onAddNext(id) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/add", JSON.stringify({ids: [id], index: this.currentSongIndex + 1}));
        },
        onAddPlaylist(id) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/add/playlist", JSON.stringify({id: id}));
        },
        onShuffle() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/shuffle", "");
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
            top.StompClient.send("/app/playqueues/" + this.player.id + "/remove", JSON.stringify([index]));
        },
        onRemoveSelected() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/remove", JSON.stringify(this.musicTable.rows({ selected: true }).indexes().toArray()));
        },

        onRearrange(indexes) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/rearrange", JSON.stringify(indexes));
        },
        onToggleRepeat() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/toggleRepeat", "");
        },
        onUndo() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/undo", "");
        },
        onSortByTrack() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/sort", "TRACK");
        },
        onSortByArtist() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/sort", "ARTIST");
        },
        onSortByAlbum() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/sort", "ALBUM");
        },
        onSavePlayQueue() {
            var positionMillis = 0;
            if (this.player.tech == 'WEB') {
                poitionMillis = Math.round(this.audioPlayer.currentTime * 1000.0);
            }
            top.StompClient.send("/app/playqueues/" + this.player.id + "/save", JSON.stringify({index: this.currentSongIndex, offset: positionMillis}));
        },
        onLoadPlayQueue() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/saved", "");
        },
        onSavePlaylist() {
            top.StompClient.send("/app/playlists/create/playqueue", this.player.id);
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

        playQueueCallback(playQueue, initial) {
            this.songs = playQueue.entries;
            this.shuffleRadioEnabled = playQueue.shuffleRadioEnabled;
            this.internetRadioEnabled = playQueue.internetRadioEnabled;

            // If an internet radio has no sources, display a message to the user.
            if (this.internetRadioEnabled && this.songs.length == 0) {
                top.main.$().toastmessage("showErrorToast", "<fmt:message key="playlist.toast.radioerror"/>");
                this.onStop();
            }

            this.playQueueRepeatStatusCallback(playQueue.repeatStatus);
            this.playQueuePlayStatusCallback(playQueue.playStatus, true);

            // download m3u for external player only once at the beginning, every subsequent change is just reflected on the server
            // download m3u for external with playlist player every time the playlist changes
            if ((this.player.tech == 'EXTERNAL' && initial) || this.player.tech == 'EXTERNAL_WITH_PLAYLIST') {
                parent.frames.main.location.href="play.m3u?";
            }

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
                location.href = "download.view?player=" + this.player.id;
            } else if (id == "sharePlaylist") {
                parent.frames.main.location.href = "createShare.view?player=" + this.player.id + "&" + selectedIndexes;
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
                location.href = "download.view?player=" + this.player.id + "&" + selectedIndexes;
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
        },

        playerSettingsPage() {
            top.frames.main.location.href = "playerSettings.view?id=" + this.player.id;
        }
    };

    $(document).ready(() => playQueue.init());
</script>

<table class="music indent hover nowrap stripe compact hide-table-header" id="playQueueMusic" style="cursor:pointer; width: 100%;"></table>

<div class="bgcolor2 playqueue-controlbar">
  <c:if test="${model.user.settingsRole and model.players.size() > 1}">
    <div style="padding-right: 5px">
        <select id="playerSelector" name="playerSelector" onchange="playQueue.onPlayerChanged(options[selectedIndex].value)">
          <c:forEach items="${model.players}" var="player">
            <option value="${player.id}">${player.shortDescription}</option>
          </c:forEach>
        </select>
    </div>
  </c:if>

    <div class="player-tech player-tech-web" style="white-space:nowrap;">
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
                    $("#castVolume").on("slidestop", () => playQueue.onCastVolumeChanged());
                </script>
            </div>
        </div>
    </div>
    <div class="player-tech player-tech-web" style="white-space:nowrap;">
        <img alt="Cast on" id="castOn" src="<spring:theme code='castIdleImage'/>" onclick="playQueue.CastPlayer.launchCastApp()" style="cursor:pointer; display:none">
        <img alt="Cast off" id="castOff" src="<spring:theme code='castActiveImage'/>" onclick="playQueue.CastPlayer.stopCastApp()" style="cursor:pointer; display:none">
    </div>

  <c:if test="${model.user.streamRole}">
    <div class="player-tech player-tech-non-web" style="white-space:nowrap;">
        <img alt="Start" id="audioStart" src="<spring:theme code='castPlayImage'/>" onclick="playQueue.onStart()" style="cursor:pointer">
        <img alt="Stop" id="audioStop" src="<spring:theme code='castPauseImage'/>" onclick="playQueue.onStop()" style="cursor:pointer; display:none">
    </div>
  </c:if>

    <div class="player-tech player-tech-java_jukebox" style="white-space:nowrap;">
        <span id="playingPositionDisplay" class="javaJukeBoxPlayerControlBarSongTime"/>
    </div>
    <div class="player-tech player-tech-java_jukebox" style="white-space:nowrap;">
        <div id="javaJukeboxSongPositionSlider"></div>
    </div>
    <div class="player-tech player-tech-java_jukebox" style="white-space:nowrap;">
        <span id="playingDurationDisplay" class="javaJukeBoxPlayerControlBarSongTime"/>
    </div>

    <div class="player-tech player-tech-jukebox" style="white-space:nowrap;">
        <img src="<spring:theme code='volumeImage'/>" alt="">
    </div>
    <div class="player-tech player-tech-jukebox" style="white-space:nowrap;">
        <div id="jukeboxVolume" style="width:80px;height:4px"></div>
        <script type="text/javascript">
            $("#jukeboxVolume").slider({max: 100, value: 50, animate: "fast", range: "min"});
            $("#jukeboxVolume").on("slidestop", () => playQueue.onJukeboxVolumeChanged());
        </script>
    </div>

    <div class="player-tech player-tech-web" style="white-space:nowrap;">
        <span class="header">
            <img src="<spring:theme code='backImage'/>" alt="Play previous" title="Play previous" onclick="playQueue.onPrevious()" style="cursor:pointer">
        </span>
    </div>
    <div class="player-tech player-tech-web" style="white-space:nowrap;">
        <span class="header">
            <img src="<spring:theme code='forwardImage'/>" alt="Play next" title="Play next" onclick="playQueue.onNext('OFF')" style="cursor:pointer">
        </span> |
    </div>

    <div style="white-space:nowrap;">
        <span class="header">
            <a href="javascript:playQueue.onClear()" class="player-control">
                <img src="<spring:theme code='clearImage'/>" alt="Clear playlist" title="Clear playlist" style="cursor:pointer; height:18px">
            </a>
        </span> |
    </div>

    <div style="white-space:nowrap;">
        <span class="header">
            <a href="javascript:playQueue.onShuffle()" id="shuffleQueue" class="player-control">
                <img src="<spring:theme code='shuffleImage'/>" alt="Shuffle" title="Shuffle" style="cursor:pointer; height:18px">
            </a>
        </span> |
    </div>

    <div class="player-tech player-tech-web player-tech-jukebox player-tech-external" style="white-space:nowrap;">
        <span class="header">
            <a href="javascript:playQueue.onToggleRepeat()" id="repeatQueue" class="player-control">
                <img id="toggleRepeat" src="<spring:theme code='repeatOff'/>" alt="Toggle repeat" title="Toggle repeat" style="cursor:pointer; height:18px">
            </a>
        </span> |
    </div>

    <div style="white-space:nowrap;">
        <span class="header">
            <a href="javascript:playQueue.onUndo()" id="undoQueue" class="player-control">
                <img src="<spring:theme code='undoImage'/>" alt="Undo" title="Undo" style="cursor:pointer; height:18px">
            </a>
        </span>  |
    </div>

  <c:if test="${model.user.settingsRole}">
    <div style="white-space:nowrap;">
        <span class="header">
            <a href="javascript:playQueue.playerSettingsPage()" class="player-control">
                <img src="<spring:theme code='settingsImage'/>" alt="Settings" title="Settings" style="cursor:pointer; height:18px">
            </a>
        </span> |
    </div>
  </c:if>

    <div style="white-space:nowrap;">
        <span class="header">
            <select id="moreActions" onchange="playQueue.actionSelected(options[selectedIndex].id)">
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
        </span>
    </div>

  <c:if test="${not model.autoHide}">
    <div style="white-space:nowrap; text-align:right; width:100%; padding-right:1.5em">
        <a href="javascript:playQueue.onTogglePlayQueue(!playQueue.isVisible)">
            <img class="playqueue-shown" src="<spring:theme code='playQueueHide'/>" alt="Hide play queue" title="Hide play queue" style="cursor:pointer; height:18px;"/>
            <img class="playqueue-hidden" src="<spring:theme code='playQueueShow'/>" alt="Show play queue" title="Show play queue" style="cursor:pointer; height:18px; display: none;"/>
        </a>
    </div>
  </c:if>

</div>

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
