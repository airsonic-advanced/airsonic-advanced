<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="websocket.jsp" %>
    <link rel="alternate" type="application/rss+xml" title="Airsonic Podcast" href="podcast.view?suffix=.rss">
    <style>
        iframe {
            border: none;
        }
        .entire-panel {
            display: flex;
            flex-direction: column;
            height: 98vh;
        }
        .main-navigation {
            width: 100%;
            height: 0%;
            flex: 10%;
        }
        .lower {
            flex: 90%;
            display: flex;
            flex-direction: row;
            height: 100%;
        }
        div.left-nav-container {
            flex: 16%;
        }
        .left-navigation {
            width: 100%;
            height: 100%;
        }
        .non-left-navigation-container {
            flex: 84%;
            display: flex;
            flex-direction: column;
            height: 100%;
        }
        .main-right-container {
            flex: 1;
            display: flex;
            flex-direction: row;
        }
        .playqueue-container {
            padding-left: 1em;
        }
        .main-panel {
            flex: 80%;
        }
        .right-info-container {
            flex: 20%;
        }
        .right-info {
            width: 100%;
            height: 100%;
        }
        .hidden-panel {
            display: none;
        }
    </style>
</head>

<body class="bgcolor2" style="height: 100%;">
    <div class="entire-panel">
        <iframe id="upper" name="upper" src="top.view?" class="bgcolor2 main-navigation"></iframe>

        <div class="lower">
            <div class="bgcolor2 left-nav-container" ${!model.showSideBar ? "style='display: none;'" : ""}>
                <iframe id="left" name="left" src="left.view?" class="left-navigation"></iframe>
            </div>
            <div class="non-left-navigation-container">
                <div class="main-right-container">
                    <iframe id="main" name="main" src="${empty param.main? 'nowPlaying.view' : param.main}" class="bgcolor1 main-panel"></iframe>
                    <div class="bgcolor1 right-info-container" ${!model.showRight ? "style='display: none;'" : ""}>
                        <iframe id="right" name="right" src="right.view?" class="right-info"></iframe>
                    </div>
                </div>
                <div class="bgcolor2 playqueue-container">
                    <c:import url="/playQueue.view" />
                </div>
           </div>
       </div>

       <iframe id="hidden" name="hidden" class="hidden-panel"></iframe>
   </div>
</body>
<!--
<frameset rows="80,*,0" border="0" framespacing="0" frameborder="0">
    <frame name="upper" src="top.view?" class="bgcolor2">
    <frameset id="mainFrameset" cols=${model.showSideBar ? "230,*" : "0,*"} border="0" framespacing="0" frameborder="0">
        <frame name="left" src="left.view?" marginwidth="0" marginheight="0" class="bgcolor2">

        <frameset id="playQueueFrameset" rows=${model.autoHidePlayQueue ? "*,50" : "*,150"} border="0" framespacing="0" frameborder="0">
            <frameset cols="*,${model.showRight ? 235 : 0}" border="0" framespacing="0" frameborder="0">
                <frame name="main" src="nowPlaying.view?" marginwidth="0" marginheight="0" class="bgcolor1">
                <frame name="right" src="right.view?" class="bgcolor1">
            </frameset>
            <frame name="playQueue" src="playQueue.view?" class="bgcolor2">
        </frameset>
    </frameset>
    <frame name="hidden" frameborder="0" noresize="noresize">

</frameset>
-->
</html>
