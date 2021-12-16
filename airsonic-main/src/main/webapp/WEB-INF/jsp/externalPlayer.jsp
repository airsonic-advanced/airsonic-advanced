<!DOCTYPE html>
<%--@elvariable id="model" type="java.util.Map"--%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <meta name="og:type" content="album"/>
    <script type="text/javascript" src="<c:url value='/script/mediaelement/mediaelement-and-player.min.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/mediaelement/plugins/playlist/playlist.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/speed/speed.min.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/speed/speed-i18n.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/quality/quality.min.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/quality/quality-i18n.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/chromecast/chromecast.js'/>"></script>
    <script src="<c:url value='/script/mediaelement/plugins/chromecast/chromecast-i18n.js'/>"></script>

    <link type="text/css" rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/playlist/playlist.min.css'/>">
    <link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/speed/speed.min.css'/>">
    <link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/quality/quality.min.css'/>">
    <link rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/chromecast/chromecast.min.css'/>">

    <c:if test="${not empty model.media}">
        <meta name="og:title"
              content="${fn:escapeXml(model.media[0].file.artist)} &mdash; ${fn:escapeXml(model.media[0].file.albumName)}"/>
        <meta name="og:image" content="${model.media[0].coverArtUrl}"/>
    </c:if>
</head>

<body class="mainframe bgcolor1" style="height:100vh;margin:0;">
<div class="external box">
    <div class="header">
        <h1>
            <c:choose>
                <c:when test="${empty model.share or empty model.media}">
                    Sorry, the content is not available.
                </c:when>
                <c:otherwise>
                    ${empty model.share.description ? model.media[0].file.artist : fn:escapeXml(model.share.description)}
                </c:otherwise>
            </c:choose>
        </h1>
        <div>
            <h2 style="margin:0;">${empty model.share.description ? model.media[0].file.albumName : fn:escapeXml(model.share.username)}</h2>
        </div>
    </div>

  <c:if test="${!model.videoPresent}">
    <audio id='player'
        data-cast-title="[Your title]"
        data-cast-description="[Your optional description]"
        poster="/path/to/poster.jpg">
    </audio>
  </c:if>
  <c:if test="${model.videoPresent}">
    <video id='player' style="width:100%; height:100%;"
        data-cast-title="[Your title]"
        data-cast-description="[Your optional description]"
        poster="/path/to/poster.jpg">
    </video>
  </c:if>

    <div class="detail" style="text-align:center;">Streaming by <a href="https://airsonic.github.io/"
								   rel="noopener noreferrer"
                                                                   target="_blank"><b>Airsonic</b></a></div>

</div>

<script type="text/javascript">
    var player = new MediaElementPlayer('player', {
        useDefaultControls: true,
        features: ['speed', 'quality', 'chromecast', 'playlist', 'prevtrack', 'nexttrack', 'shuffle', 'loop'],
        currentMessage: "",
        defaultSpeed: "1.00",
        speeds: ["8.00", "2.00", "1.50", "1.25", "1.00", "0.75", "0.5"],
        castTitle: "Airsonic",
        castAppID: "4FBFE470",
        castPolicy: "tab",
        castEnableTracks: true,
        playlistTitle: "${model.share.description}",
        playlist: [
          <c:forEach items="${model.media}" var="song">
            {
                "src": "${song.streamUrl}",
                "title": "${fn:escapeXml(song.file.title)}",
                "type": "${song.contentType}",
                "data-playlist-thumbnail": "${song.coverArtUrl}",
                "data-playlist-description": "${fn:escapeXml(song.file.artist)}",
                "data-playlist-caption": "${song.captionsUrl}"
            },
          </c:forEach>
        ],
        audioWidth: 600,
        success: function(m, n, p) {
            $(p.playlistLayer).on('newplaylistsrc', e => {
                m.originalNode.setAttribute('type', e.detail['type']);
                $.get(e.detail['data-playlist-caption'], data => {
                    const tracks = data.map(s => {
                        const track = document.createElement('track');
                        track.kind = 'subtitles';
                        track.label = s.identifier + " (" + s.language + ")";
                        track.src = s.url;
                        track.srclang = s.language;

                        return track;
                    });

                    p.trackFiles = tracks;
                    p.rebuildtracks();
                });

                p.buildspeed(p, p.getElement(p.controls), p.getElement(p.layers), m);
            });
        }
    });
</script>
<style>
    .external .mejs-container.mejs-audio, .external .mejs-container.mejs-video, .mejs__container.mejs__audio, .mejs__container.mejs__video {
        margin: auto;
        margin-top: 2%;
        margin-bottom: 2%;
        flex-grow: 1;
        flex-shrink: 1;
        flex-basis: auto;
    }
    .external.box {
        display: flex;
        flex-flow: column;
        height: 100%;
    }
    .external > .header {
        padding-top: 2em;
        margin: auto;
        width: 500px;
        flex-grow: 0;
        flex-shrink: 1;
        flex-basis: auto;
    }
    .external > .detail {
        flex-grow: 0;
        flex-shrink: 1;
        flex-basis: 40px;
    }
</style>
</body>
</html>
