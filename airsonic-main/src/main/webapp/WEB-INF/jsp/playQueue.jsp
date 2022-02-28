<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ include file="include.jsp" %>
<%@ include file="jquery.jsp" %>
<%@ include file="table.jsp" %>
<script type="text/javascript" src="<c:url value='/script/utils.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/mediaelement/mediaelement-and-player.min.js'/>"></script>
<script src="<c:url value='/script/mediaelement/plugins/speed/speed.min.js'/>"></script>
<script src="<c:url value='/script/mediaelement/plugins/speed/speed-i18n.js'/>"></script>
<script src="<c:url value='/script/mediaelement/plugins/chromecast/chromecast.min.js'/>"></script>
<script src="<c:url value='/script/mediaelement/plugins/chromecast/chromecast-i18n.js'/>"></script>
<link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/speed/speed.min.css'/>">
<link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/chromecast/chromecast.min.css'/>">

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
        
        bookmarks: {},
        autoBookmark: ${model.autoBookmark},
        audioBookmarkFrequency: ${model.audioBookmarkFrequency},

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
                        rowNode.find(".songIndex").prop("checked", true);
                    }
                },
                colReorder: true,
                fixedHeader: true,
                stateSave: true,
                stateDuration: 60 * 60 * 24 * 365,
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
                              var container = $('#playQueueMusic_wrapper .dataTables_scrollBody');
                              var target = $('.currently-playing');
                              container.animate({
                                scrollTop: Math.floor(target.offset().top - container.offset().top + container.scrollTop())
                              }, 150, function() {
                                target.fadeOut(100, function() {target.fadeIn(100);});
                              });
                          }
                      }
                    }
                ],
                processing: true,
                autoWidth: true,
                scrollCollapse: true,
                scrollY: "calc(80vh - 350px)",
                scrollX: true,
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
                    { data: null,
                      searchable: false,
                      name: "songcheckbox",
                      className: "fit not-draggable centeralign",
                      title: "<input type='checkbox' class='songSelectAll'>",
                      defaultContent: "<input type='checkbox' class='songIndex'>"
                    },
                    { data: "starred",
                      name: "starred",
                      className: "fit not-draggable centeralign",
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
                      className: "fit not-draggable centeralign",
                      defaultContent: "<img class='removeSong' src=\"<spring:theme code='removeImage'/>\" style='height:18px;' alt=\"<fmt:message key='playlist.remove'/>\" title=\"<fmt:message key='playlist.remove'/>\">"
                    },
                    { data: "trackNumber", className: "detail fit", visible: ${model.visibility.trackNumberVisible}, title: "<fmt:message key='personalsettings.tracknumber'/>" },
                    { data: "discNumber", className: "detail fit", visible: ${model.visibility.discNumberVisible}, title: "<fmt:message key='personalsettings.discnumber'/>" },
                    { data: "title",
                      className: "detail songTitle truncate",
                      title: "<fmt:message key='edittags.songtitle'/>",
                      render(title, type, row) {
                          if (type == "display") {
                              var img = "<img class='currentImage' src=\"<spring:theme code='currentImage'/>\" alt='' style='display: none; padding-right: 0.5em' />";
                              if (playQueue.player.tech != "EXTERNAL_WITH_PLAYLIST") {
                                  return img + $("<a>", {title: title, alt: title, text: title}).addClass("titleUrl").attr("href", "javascript:void(0)")[0].outerHTML;
                              } else {
                                  return img + $("<span>", {title: title, alt: title, text: title})[0].outerHTML;
                              }
                          }
                          return title;
                      }
                    },
                    { data: "album",
                      className: "detail truncate",
                      visible: ${model.visibility.albumVisible},
                      title: "<fmt:message key='personalsettings.album'/>",
                      render(album, type, row) {
                          if (type == "display" && album != null) {
                              return $("<a>", {title: album, alt: album, text: album}).attr("href", row.albumUrl).attr("target", !pq.internetRadioEnabled ? "main" : "_blank").attr("rel", !pq.internetRadioEnabled ? "" : "noopener noreferrer")[0].outerHTML;
                          }
                          return album;
                      }
                    },
                    { data: "artist",
                      className: "detail truncate",
                      visible: ${model.visibility.artistVisible},
                      title: "<fmt:message key='personalsettings.artist'/>",
                      render(artist, type) {
                          if (type == "display" && artist != null) {
                              return $("<span>", {title: artist, alt: artist, text: artist})[0].outerHTML;
                          }
                          return artist;
                      }
                    },
                    { data: "albumArtist",
                      className: "detail truncate",
                      visible: ${model.visibility.artistVisible},
                      title: "<fmt:message key='personalsettings.albumartist'/>",
                      render(artist, type) {
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
                      render(genre, type) {
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
	                }
                ]
            } );

            pq.musicTable.on( 'select', function ( e, dt, type, indexes ) {
                pq.musicTable.cells( indexes, "songcheckbox:name" ).nodes().to$().find("input").prop("checked", true);
                pq.updateSelectAllCheckboxStatus();
            } );
            pq.musicTable.on( 'deselect', function ( e, dt, type, indexes ) {
                pq.musicTable.cells( indexes, "songcheckbox:name" ).nodes().to$().find("input").prop("checked", false);
                pq.updateSelectAllCheckboxStatus();
            } );
            $("#playQueueMusic tbody").on( "click", ".starSong", function () {
                pq.onToggleStar(pq.musicTable.row( $(this).parents('tr') ).index());
            } );
            $("#playQueueMusic tbody").on( "click", ".removeSong", function () {
                pq.onRemove(pq.musicTable.row( $(this).parents('tr') ).index());
            } );
            $("#playQueueMusic tbody").on( "click", ".titleUrl", function () {
                pq.onSkip(pq.musicTable.row( $(this).parents('tr') ).index());
            } );
            $(".songSelectAll").on( "change", function (e) {
                pq.selectAll(e.target.checked);
            } );
            pq.musicTable.on( "row-reordered", function (e, diff, edit) {
                if (diff.length > 0) {
                    pq.musicTable.one( "draw", function () {
                        pq.onRearrange(pq.musicTable.rows().indexes().toArray());
                    });
                }
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
                },

                // Bookmarks
                '/user/queue/bookmarks/added': function(msg) {
	                pq.addedBookmarksCallback(JSON.parse(msg.body));
	            },
	            '/user/queue/bookmarks/deleted': function(msg) {
	                pq.deleteBookmarksCallback(JSON.parse(msg.body));
	            },
	            '/user/queue/bookmarks/get': function(msg) {
	                pq.getBookmarkCallback(JSON.parse(msg.body));
	            },
	            //one-time population only
	            '/app/bookmarks/list': function(msg) {
	                pq.getBookmarksCallback(JSON.parse(msg.body));
	            }
            });

            var dialogSize = getJQueryUiDialogPlaylistSize("playQueue");
            $("#dialog-select-playlist").dialog({resizable: true, height: dialogSize.height, width: dialogSize.width, autoOpen: false,
                buttons: {
                    "<fmt:message key="common.cancel"/>"() {
                        $(this).dialog("close");
                    }
                },
                resizeStop: function (event, ui) { setJQueryUiDialogPlaylistSize("playQueue", ui.size) }
            });

            pq.createMediaElementPlayer();
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
                  if (pq.player.tech == 'WEB') {
                      //if (this.CastPlayer.castSession) {
                      //    pq.CastPlayer.stopCastApp();
                      //}

                      // no need to change src on audioPlayer because start button will see currentSongIndex
                      //pq.audioPlayer.setSrc(null);
                  }

                  //switch to new player
                  $("#playerSelector").val(player.id);
                  pq.player = player;
                  $(".player-tech-" + player.tech.toLowerCase()).show();
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
            //nothing for now
        },
        jukeBoxGainCallback(gain) {
            $("#jukeboxVolume").slider("option", "value", Math.floor(gain * 100)); // update UI
        },
        onJukeboxVolumeChanged() {
            var value = parseInt($("#jukeboxVolume").slider("option", "value"));
            top.StompClient.send("/app/playqueues/" + this.player.id + "/jukebox/gain", value / 100);
        },

        /**
         * Increase or decrease volume by a certain amount
         *
         * @param gain amount to add or remove from the current volume
         */
        onGainAdd(gain) {
            if (this.player.tech == 'WEB') {
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
            this.setBookmark();
            this.onNext(this.repeatStatus);
        },

        setBookmark() {
            if (this.autoBookmark) {
                var song = this.songs[this.currentSongIndex];
                var positionMillis = Math.round(this.audioPlayer.currentTime * 1000);
                top.StompClient.send("/app/bookmarks/set", JSON.stringify({positionMillis: positionMillis, comment: "Played on Web Player " + this.player.id, mediaFileId: song.id}));
            }
        },
        lastProgressionBookmarkTime: 0,
        updateProgressionBookmark() {
            if (this.autoBookmark) {
                var song = this.songs[this.currentSongIndex];
                var position = Math.round(this.audioPlayer.currentTime);
                if ((this.lastProgressionBookmarkTime != position) && (position % this.audioBookmarkFrequency == 0)) {
                    this.lastProgressionBookmarkTime = position;
                    this.setBookmark();
                }
            }
        },

        deleteBookmarksCallback(mediaFileId) {
            delete this.bookmarks[mediaFileId];
        },
        addedBookmarksCallback(mediaFileId) {
            // get new (added in callback)
            top.StompClient.send("/app/bookmarks/get", mediaFileId);
        },
        getBookmarkCallback(bookmark) {
            this.bookmarks[bookmark.mediaFileEntry.id] = bookmark;
        },
        getBookmarksCallback(bookmarks) {
            this.bookmarks = bookmarks;
        },

        createMediaElementPlayer() {
            var pq = this;
            // Manually run MediaElement.js initialization.
            this.audioPlayer = new MediaElementPlayer("audioPlayer", {
                alwaysShowControls: true,
                enableKeyboard: false,
                useDefaultControls: true,
                castAppID: "4FBFE470",
                features: ["speed", "chromecast"],
                defaultSpeed: "1.00",
                speeds: ["8.00", "2.00", "1.50", "1.25", "1.00", "0.75", "0.5"],
                success(mediaElement, originalNode, instance) {
                    // "hack" html5 renderer and reinitialize speed
                    instance.media.rendererName = "html5";
                    instance.buildspeed(instance, instance.getElement(instance.controls), instance.getElement(instance.layers), instance.media);

                    // Once playback reaches the end, go to the next song, if any.
                    $(mediaElement).on("ended", () => pq.onEnded());
                    $(mediaElement).on("timeupdate", () => pq.updateProgressionBookmark());
                    $(mediaElement).on("seeked", () => pq.setBookmark());
                    $(mediaElement).on("paused", () => pq.setBookmark());

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
                if (this.player.tech == 'WEB' && !nonWebOnly) {
                    if (this.audioPlayer.src) {
                        this.audioPlayer.play();  // Resume playing if the player was paused
                    } else {
                        this.onSkip(0);  // Start the first track if the player was not yet loaded
                    }
                }
            } else {
                $("#audioStop").hide();
                $("#audioStart").show();
                if (this.player.tech == 'WEB' && !nonWebOnly) {
                    this.audioPlayer.pause();
                }
            }
        },

        /**
         * Start/resume playing from the current playlist
         */
        onStart() {
            // simulate immediate callback
            if (this.player.tech == 'WEB') {
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
            if (this.player.tech == 'WEB') {
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
            if (this.player.tech == 'WEB') {
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
            }

            this.updateWindowTitle(song);
            this.showMediaSessionMetadata(song);

          <c:if test="${model.notify}">
            this.showNotification(song);
          </c:if>
        },

        onSkip(index, offset, nobookmarks) {
            if (!offset && !nobookmarks) {
                var bookmark = this.bookmarks[this.songs[index].id];
                if (bookmark) {
                    offset = bookmark.positionMillis;
                }
            }
            if (this.player.tech == 'WEB') {
                this.playQueueSkipCallback({index: index, offset: offset});
            } else if (this.player.tech != 'EXTERNAL_WITH_PLAYLIST') {
                top.StompClient.send("/app/playqueues/" + this.player.id + "/skip", JSON.stringify({index: index, offset: offset}));
            }
        },

        webSkip(song, position) {
            this.loadMediaElementPlayer(song, position);
        },

        loadMediaElementPlayer(song, position) {
            var player = this.audioPlayer;

            // Is this a new song?
            if (player.src == null || !player.src.endsWith(song.streamUrl)) {
                // Stop the current playing song and change the media source.
                player.src = song.streamUrl;
                player.node.setAttribute('type', song.contentType);
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
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/mediafile", JSON.stringify({id: Array.isArray(id) ? id[0] : id}));
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
            top.StompClient.send("/app/playqueues/" + this.player.id + "/add", JSON.stringify({ids: Array.isArray(id) ? id : [id]}));
        },
        onAddNext(id) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/add", JSON.stringify({ids: Array.isArray(id) ? id : [id], index: this.currentSongIndex + 1}));
        },
        onAddPlaylist(id) {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/add/playlist", JSON.stringify({id: id}));
        },
        onShuffle() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/shuffle", "");
        },
        onStar(indices, status) {
            var par = this;
            var ids = indices.map(index => {
                par.songs[index].starred = status;
                par.musicTable.cell(index, "starred:name").invalidate();
                return par.songs[index].id;
            });
            
            if (status) {
                top.StompClient.send("/app/rate/mediafile/star", JSON.stringify(ids));
            } else {
                top.StompClient.send("/app/rate/mediafile/unstar", JSON.stringify(ids));
            }
            
        },
        onToggleStar(index) {
            this.onStar([index], !this.songs[index].starred);
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
                positionMillis = Math.round(this.audioPlayer.currentTime * 1000.0);
            }
            top.StompClient.send("/app/playqueues/" + this.player.id + "/save", JSON.stringify({index: this.currentSongIndex, offset: positionMillis}));
        },
        onLoadPlayQueue() {
            top.StompClient.send("/app/playqueues/" + this.player.id + "/play/saved", "");
        },
        onSavePlaylist() {
            top.StompClient.send("/app/playlists/create/playqueue", this.player.id);
        },
        // need to keep track if a request was sent because mediaMain may also send a request
        awaitingAppendPlaylistRequest: false,
        onAppendPlaylist() {
            this.awaitingAppendPlaylistRequest = true;
            // retrieve writable lists so we can open dialog to ask user which playlist to append to
            top.StompClient.send("/app/playlists/writable", "");
        },
        playlistSelectionCallback(playlists) {
            if (!this.awaitingAppendPlaylistRequest) {
                return;
            }
            this.awaitingAppendPlaylistRequest = false;
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
            $("select#moreActions #removeSelected").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #download").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #appendPlaylist").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #star").prop("disabled", this.internetRadioEnabled);
            $("select#moreActions #unstar").prop("disabled", this.internetRadioEnabled);
            $("#shuffleQueue").toggleLink(!this.internetRadioEnabled);
            $("#repeatQueue").toggleLink(!this.internetRadioEnabled);
            $("#undoQueue").toggleLink(!this.internetRadioEnabled);

            if (this.songs.length == 0) {
                $("#playQueueInfo").text("");
            } else {
                var totDuration = this.songs.map(s => s.duration).filter(d => d != null).reduce((a,b) => a + b, 0);
                $("#playQueueInfo").html("&nbsp;|&nbsp;" + this.songs.length + " <fmt:message key='playlist2.songs'/> &nbsp;|&nbsp;" + formatDuration(Math.round(totDuration)));
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

        showMediaSessionMetadata(song) {
            if ('mediaSession' in navigator) {
                var metadata = new MediaMetadata({
                    title: song.title,
                    artist: song.artist,
                    album: song.album,
                    artwork: [
                        { src: "coverArt.view?id=" + song.id + "&size=96", sizes: '96x96', type: 'image/jpeg' },
                        { src: "coverArt.view?id=" + song.id + "&size=128", sizes: '128x128', type: 'image/jpeg' },
                        { src: "coverArt.view?id=" + song.id + "&size=256", sizes: '256x256', type: 'image/jpeg' },
                        { src: "coverArt.view?id=" + song.id + "&size=512", sizes: '512x512', type: 'image/jpeg' }
                    ]
                });
                navigator.mediaSession.metadata = metadata;
                navigator.mediaSession.setActionHandler('play', () => this.onStart());
                navigator.mediaSession.setActionHandler('pause', () => this.onStop());
                navigator.mediaSession.setActionHandler('previoustrack', () => this.onPrevious());
                navigator.mediaSession.setActionHandler('nexttrack', () => this.onNext('OFF'));
            }
        },
        showNotification(song) {
            if (!("Notification" in window)) {
                return;
            }
            if (Notification.permission === "granted") {
                this.createNotification(song);
            } else if (Notification.permission !== 'denied') {
                Notification.requestPermission(function (permission) {
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
            var selectedIndexes;
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
                parent.frames.main.location.href = "createShare.view?player=" + this.player.id + "&" + this.querize(this.getSelectedIndexes(), "i");
            } else if (id == "sortByTrack") {
                this.onSortByTrack();
            } else if (id == "sortByArtist") {
                this.onSortByArtist();
            } else if (id == "sortByAlbum") {
                this.onSortByAlbum();
            } else if (id == "removeSelected") {
                this.onRemoveSelected();
            } else if ((selectedIndexes = this.getSelectedIndexes()).length > 0 && id == "star") { // define selectedIndexes first so it always evaluates
                this.onStar(selectedIndexes, true);
            } else if (id == "unstar" && selectedIndexes.length > 0) {
                this.onStar(selectedIndexes, false);
            } else if (id == "download" && selectedIndexes.length > 0) {
                location.href = "download.view?player=" + this.player.id + "&" + querize(selectedIndexes, "i");
            } else if (id == "appendPlaylist" && selectedIndexes.length > 0) {
                this.onAppendPlaylist();
            }
            $("#moreActions").prop("selectedIndex", 0);
        },
        getSelectedIndexes() {
            return this.musicTable.rows({ selected: true }).indexes().toArray();
        },
        querize(arr, queryVar) {
            return arr.map(i => queryVar + "=" + i).join("&");
        },

        selectAll(b) {
            if (b) {
                this.musicTable.rows().select();
            } else {
                this.musicTable.rows().deselect();
            }
        },

        updateSelectAllCheckboxStatus() {
            var pq = this;
            if (pq.musicTable.rows({selected: true}).indexes().length == 0) {
                $('.songSelectAll').prop('checked', false);
                $('.songSelectAll').prop('indeterminate', false);
            } else if (pq.musicTable.rows({selected: true}).indexes().length == pq.musicTable.rows().indexes().length) {
                $('.songSelectAll').prop('checked', true);
                $('.songSelectAll').prop('indeterminate', false);
            } else {
                $('.songSelectAll').prop('indeterminate', true);
            }
        },

        playerSettingsPage() {
            top.frames.main.location.href = "playerSettings.view?id=" + this.player.id;
        }
    };

    $(document).ready(() => playQueue.init());
</script>

<table class="music indent hover nowrap stripe compact <c:if test='${!model.visibility.headerVisible}'>hide-table-header</c:if>" id="playQueueMusic" style="cursor:pointer; width: 100%;"></table>

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

    <div class="player-tech player-tech-web" style="white-space:nowrap;flex:1 1 300px;">
        <div id="player" style="height:40px">
            <audio id="audioPlayer" style="width:100%; height:40px" tabindex="-1" ></audio>
        </div>
    </div>

  <c:if test="${model.user.streamRole}">
    <div class="player-tech player-tech-non-web" style="white-space:nowrap;">
        <img alt="Start" id="audioStart" src="<spring:theme code='castPlayImage'/>" onclick="playQueue.onStart()" style="cursor:pointer">
        <img alt="Stop" id="audioStop" src="<spring:theme code='castPauseImage'/>" onclick="playQueue.onStop()" style="cursor:pointer; display:none">
    </div>
  </c:if>

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
                    <option id="removeSelected"><fmt:message key="playlist.remove"/></option>
                  <c:if test="${model.user.downloadRole}">
                    <option id="download"><fmt:message key="common.download"/></option>
                  </c:if>
                    <option id="appendPlaylist"><fmt:message key="playlist.append"/></option>
                    <option id="star"><fmt:message key="playlist.more.star"/></option>
                    <option id="unstar"><fmt:message key="playlist.more.unstar"/></option>
                </optgroup>
            </select>
        </span>
    </div>

  <c:if test="${not model.autoHide}">
    <div style="white-space:nowrap; text-align:right; padding-left:1.5em; padding-right:1.5em">
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
