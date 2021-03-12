<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/mediaelement/mediaelement-and-player.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/speed/speed.min.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/speed/speed-i18n.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/quality/quality.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/quality/quality-i18n.js'/>"></script>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/videoPlayer.css'/>">
    <link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/speed/speed.min.css'/>">
    <link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/quality/quality.min.css'/>">

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
        #videoPlayer {
            height: 100%;
            width: 100%;
        }
    </style>

    <script type="text/javascript" language="javascript">
        function toggleStar(mediaFileId, imageId) {
            if ($(imageId).attr("src").indexOf("<spring:theme code='ratingOnImage'/>") != -1) {
                $(imageId).attr("src", "<spring:theme code="ratingOffImage"/>");
                top.StompClient.send("/app/rate/mediafile/unstar", mediaFileId);
            }
            else if ($(imageId).attr("src").indexOf("<spring:theme code='ratingOffImage'/>") != -1) {
                $(imageId).attr("src", "<spring:theme code='ratingOnImage'/>");
                top.StompClient.send("/app/rate/mediafile/star", mediaFileId);
            }
        }
        var model = {
          duration: ${empty model.video.duration ? 0: model.video.duration},
          remoteStreamUrl: "${model.remoteStreamUrl}",
          video_title: "${model.video.title}",
          captions: ${model.captions},
          streamable: ${model.streamable},
          castable: ${model.castable},
          remoteCoverArtUrl: "${model.remoteCoverArtUrl}",
          streamUrl: "${model.streamUrl}",
          video_id: "${model.video.id}",
          hide_share: ${model.user.shareRole ? 1: 0},
          hide_download: ${model.user.downloadRole ? 1: 0}
        }
        function init() {
            var videoPlayer = new MediaElementPlayer("videoPlayer", {
                alwaysShowControls: true,
                enableKeyboard: false,
                useDefaultControls: true,
                features: ["tracks", "speed", "fullscreen", "quality"],
                defaultSpeed: "1.00",
                speeds: ["8.00", "2.00", "1.50", "1.25", "1.00", "0.75", "0.5"],
                defaultQuality: "${model.defaultBitRate} Kbps",
                qualityChangeCallback( media, node, newQuality, source ) {
                    console.log(media, node, newQuality, source);
                },
                success(mediaElement, originalNode, instance) {
                    // "hack" html5 renderer and reinitialize speed
                    instance.media.rendererName = "html5";
                    instance.buildspeed(instance, instance.getElement(instance.controls), instance.getElement(instance.layers), instance.media);

		            if (model.captions) {
			            const track = document.createElement('track');
						track.kind = 'subtitles';
						track.label = 'Default';
						track.src = 'captions.view?id='+model.video_id;
						track.srclang = 'en';

						// We are assuming there is only one `track` tag;
						// if there are more, implement logic to override the necessary one(s)
						if (instance.trackFiles !== null) {
							instance.trackFiles = [track];
						}
						instance.trackFiles = [track];
						instance.rebuildtracks();
						//instance.tracks = [track];
						//instance.findTracks();
						// This way we are ensuring ALL tracks are being loaded, starting from the first one
						//instance.loadTrack(0);
						//instance.setTrack('mep_0_track_0_subtitles_en');
		            }
                }
            });

            //var initVidSrc = "${model.streamUrl}";
            //videoPlayer.src = initVidSrc + "&maxBitRate=${model.defaultBitRate}";
        }
    </script>
    <!--<script type="text/javascript" src="<c:url value='/script/videoPlayerCast.js'/>"></script>-->
</head>

<body class="mainframe bgcolor1" style="padding-bottom:0.5em" onload="init();">

    <div>
        <!--<div id="overlay">
            <div id="overlay_text">Playing on Chromecast</div>
        </div>-->
        <video id="videoPlayer">
          <c:forEach items="${model.bitRates}" var="bitRate">
            <source src="${model.streamUrl}&maxBitRate=${bitRate}" data-quality="${bitRate} Kbps">
          </c:forEach>
        </video>
        <!--<div id="media_control">
            <div id="progress_slider"></div>
            <div id="play"></div>
            <div id="pause"></div>
            <div id="progress">0:00</div>
            <div id="duration">0:00</div>
            <div id="audio_on"></div>
            <div id="audio_off"></div>
            <div id="volume_slider"></div>
            <div id="fullscreen"></div>
            <select name="bitrate_menu" id="bitrate_menu">
                <c:forEach items="${model.bitRates}" var="bitRate">
                    <c:choose>
                        <c:when test="${bitRate eq model.defaultBitRate}">
                            <option selected="selected" value="${bitRate}">${bitRate} Kbps</option>
                        </c:when>
                        <c:otherwise>
                            <option value="${bitRate}">${bitRate} Kbps</option>
                        </c:otherwise>
                    </c:choose>
                </c:forEach>
            </select>
            <div id="share"></div>
            <div id="download"></div>
            <div id="casticonactive"></div>
            <div id="casticonidle"></div>
        </div>
    </div>
    <div id="debug"></div>

    <script type="text/javascript">
        var CastPlayer = new CastPlayer();
    </script>
-->

<h1 style="padding-top:1em;padding-bottom:0.5em;">
    <img id="starImage" src="<spring:theme code='${not empty model.video.starredDate ? \'ratingOnImage\' : \'ratingOffImage\'}'/>"
         onclick="toggleStar(${model.video.id}, '#starImage'); return false;" style="cursor:pointer" alt="">
    <span style="vertical-align:middle">${fn:escapeXml(model.video.title)}</span>
</h1>

<sub:url value="main.view" var="backUrl"><sub:param name="id" value="${model.video.id}"/></sub:url>

<div class="back" style="float:left;padding-right:2em"><a href="${backUrl}"><fmt:message key="common.back"/></a></div>
<div style="clear: both"></div>
<script type="text/javascript" src="https://www.gstatic.com/cv/js/sender/v1/cast_sender.js?loadCastFramework=1"></script>

</body>
</html>
