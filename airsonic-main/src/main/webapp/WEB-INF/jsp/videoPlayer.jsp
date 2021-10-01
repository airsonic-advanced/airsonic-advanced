<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value='/script/mediaelement/mediaelement-and-player.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/speed/speed.min.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/speed/speed-i18n.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/quality/quality.min.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/quality/quality-i18n.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/chromecast/chromecast.min.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/chromecast/chromecast-i18n.js'/>"></script>
    <link rel="stylesheet" type="text/css" href="<c:url value='/style/videoPlayer.css'/>">
    <link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/speed/speed.min.css'/>">
    <link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/quality/quality.min.css'/>">
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
        #videoPlayer {
            height: 100%;
            width: 100%;
        }
    </style>

    <script type="text/javascript" language="javascript">
        function toggleStar(mediaFileId, imageId) {
            if ($(imageId).attr("src").indexOf("<spring:theme code='ratingOnImage'/>") != -1) {
                $(imageId).attr("src", "<spring:theme code="ratingOffImage"/>");
                top.StompClient.send("/app/rate/mediafile/unstar", JSON.stringify([mediaFileId]));
            }
            else if ($(imageId).attr("src").indexOf("<spring:theme code='ratingOffImage'/>") != -1) {
                $(imageId).attr("src", "<spring:theme code='ratingOnImage'/>");
                top.StompClient.send("/app/rate/mediafile/star", JSON.stringify([mediaFileId]));
            }
        }

        var videoModel = {
          duration: ${empty model.video.duration ? -1 : model.video.duration},
          videoTitle: "${model.video.title}",
          streamable: ${model.streamable},
          castable: ${model.castable},
          streamUrls: ${sub:toJson(model.streamUrls)},
          remoteCoverArtUrl: "${model.remoteCoverArtUrl}",
          remoteStreamUrl: "${model.remoteStreamUrl}",
          remoteCaptionsListUrl: "${model.remoteCaptionsUrl}",
          remoteCaptions: [],
          currentUrl: "${model.defaultBitRate}",
          videoId: "${model.video.id}",
          contentType: "${model.contentType}",
          hideShare: ${model.user.shareRole ? 'true': 'false'},
          hideDownload: ${model.user.downloadRole ? 'true': 'false'}
        }

        function init() {
            $.get(videoModel.remoteCaptionsListUrl, data => {
                videoModel.remoteCaptions = data;
            });
            var videoPlayer = new MediaElementPlayer("videoPlayer", {
                alwaysShowControls: true,
                enableKeyboard: true,
                useDefaultControls: true,
                enableTracks: true,
                castAppID: "4FBFE470",
                features: ["speed", "quality", "chromecast"],
                hls: {
                    path: "<c:url value='/script/mediaelement/renderers/hls-1.0.10/hls.min.js'/>"
                },
                dash: {
                    path: "<c:url value='/script/mediaelement/renderers/dash.all-4.0.1.min.js'/>"
                },
                defaultSpeed: "1.00",
                speeds: ["8.00", "2.00", "1.50", "1.25", "1.00", "0.75", "0.5"],
                defaultQuality: "${model.defaultBitRate}",
                videoWidth: "100%",
                videoHeight: "100%",
                qualityChangeCallback: function(media, node, newQuality, url, event) {
                    videoModel.currentUrl = newQuality;
                },
                success(mediaElement, originalNode, instance) {
                    if (videoModel.streamable) {
                        // "hack" html5 renderer and reinitialize speed
                        instance.media.rendererName = "html5";
                        instance.buildspeed(instance, instance.getElement(instance.controls), instance.getElement(instance.layers), instance.media);
                    }
                    <c:if test="${model.user.shareRole}">
                    $("#share").on('click', () => location.href = "createShare.view?id=" + videoModel.videoId);
                    </c:if>
                    <c:if test="${model.user.downloadRole}">
                    $("#download").on('click', () => location.href = "download.view?id=" + videoModel.videoId);
                    </c:if>
                    // add dimensions to playing vid
                    instance.setSrc($('#videoPlayer source[data-quality="${model.defaultBitRate}"]')[0].src);
                }
            });
            // add dimensions to play at
            $('#videoPlayer source').each((s, d) => d.src = d.src + "&size=" + Math.floor($('.mejs__container').width()) + "x" + Math.floor($('.mejs__container').height()));
        }
    </script>
</head>

<body class="mainframe bgcolor1" style="padding-bottom:0.5em" onload="init();">

<div style="width:100%;height:100%;display:flex;flex-direction:column;">
    <div style="flex:0 1 auto">
		<video id="videoPlayer">
		  <c:forEach items="${model.streamUrls}" var="streamUrl">
		    <source src="${streamUrl.value}" data-quality="${streamUrl.key}" type="${model.streamType}">
		  </c:forEach>
		  <c:forEach items="${model.captions}" var="caption">
		    <c:url value="/captions" var="suburl">
		        <c:param name="id" value="${model.video.id}" />
		        <c:param name="captionId" value="${caption.identifier}" />
		    </c:url>
		    <track src="${caption.url}" label="${caption.identifier} (${caption.language})" srclang="${caption.language}" kind="subtitles">
		  </c:forEach>
		</video>
	</div>

	<h1 style="padding-top:1em;padding-bottom:0.5em;">
	    <span style="vertical-align:middle">${fn:escapeXml(model.video.title)}</span>
	</h1>

	<div>
	  <c:if test="${model.user.shareRole}">
	    <div id="share"></div>
	  </c:if>
	  <c:if test="${model.user.downloadRole}">
	    <div id="download"></div>
	  </c:if>
	  <img id="starImage" src="<spring:theme code='${not empty model.video.starredDate ? \'ratingOnImage\' : \'ratingOffImage\'}'/>"
	         onclick="toggleStar(${model.video.id}, '#starImage'); return false;" style="cursor:pointer; width:20px;height:20px;margin-left:5px;" alt="">
	</div>

	<sub:url value="main.view" var="backUrl"><sub:param name="id" value="${model.video.id}"/></sub:url>
	<div class="back" style="float:left;padding-right:2em;margin-top:1em;"><a href="${backUrl}"><fmt:message key="common.back"/></a></div>
	<div style="clear: both"></div>
</div>

</body>
</html>
