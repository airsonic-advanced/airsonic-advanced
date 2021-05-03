<%--@elvariable id="model" type="java.util.Map"--%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <meta name="og:type" content="album"/>
    <script type="text/javascript" src="<c:url value='/script/mediaelement/mediaelement-and-player.min.js'/>"></script>
    <script type="text/javascript" src="<c:url value='/script/mediaelement/plugins/playlist/playlist.min.js'/>"></script>
    <link type="text/css" rel="stylesheet" href="<c:url value='/script/mediaelement/plugins/playlist/playlist.min.css'/>">
    <c:if test="${not empty model.media}">
        <meta name="og:title"
              content="${fn:escapeXml(model.media[0].file.artist)} &mdash; ${fn:escapeXml(model.media[0].file.albumName)}"/>
        <meta name="og:image" content="${model.media[0].coverArtUrl}"/>
    </c:if>
</head>

<body class="mainframe bgcolor1" style="height:100%;margin:0;">
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
    <audio id='player'>
    </audio>
  </c:if>
  <c:if test="${model.videoPresent}">
    <video id='player'>
    </video>
  </c:if>

    <div class="detail" style="text-align:center;">Streaming by <a href="https://airsonic.github.io/"
								   rel="noopener noreferrer"
                                                                   target="_blank"><b>Airsonic</b></a></div>

</div>

<script type="text/javascript">
    new MediaElementPlayer('player', {
        useDefaultControls: true,
        features: ['playlist', 'loop'],
        currentMessage: "",
        playlistTitle: "${model.share.description}",
        playlist: [
          <c:forEach items="${model.media}" var="song">
            {
                "src": "${song.streamUrl}",
                "title": "${fn:escapeXml(song.file.title)}",
                "type": "${song.contentType}",
                "data-playlist-thumbnail": "${song.coverArtUrl}",
                "data-playlist-description": "${fn:escapeXml(song.file.artist)}"
            },
          </c:forEach>
        ],
        audioWidth: 600
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
