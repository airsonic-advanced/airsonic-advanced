<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <%@ include file="websocket.jsp" %>
    <title><fmt:message key="lyrics.title"/></title>

    <script type="text/javascript" language="javascript">
        // independent page, so StompClient does not belong to window.top
        function init() {
            StompClient.subscribe("lyrics.jsp", {
                "/user/queue/lyrics/get": function(msg) {
                    getLyricsCallback(JSON.parse(msg.body));
                }
            }, function() {
                getLyrics('${model.artist}', '${model.song}');
            });
        }

        function getLyrics(artist, song) {
            $("#wait").css("display", "inline");
            $("#lyrics").css("display", "none");
            $("#noLyricsFound").css("display", "none");
            $("#tryLater").css("display", "none");
            StompClient.send("/app/lyrics/get", JSON.stringify({artist: artist, song: song}));
        }

        function getLyricsCallback(lyricsInfo) {
            $("#lyricsHeader").text(lyricsInfo.artist + " - " + lyricsInfo.title);
            var lyrics;
            if (lyricsInfo.lyrics != null) {
                lyrics = lyricsInfo.lyrics.replace(/\n/g, "<br>");
            }
            $("#lyricsText").html(lyrics);
            $("#wait").css("display", "none");
            if (lyricsInfo.tryLater) {
                $("#tryLater").css("display", "inline");
            } else if (lyrics != null) {
                $("#lyrics").css("display", "inline");
            } else {
                $("#noLyricsFound").css("display", "inline");
            }
        }

        function search() {
            getLyrics($('#artist').val(), $('#song').val());
        }
    </script>

</head>
<body class="mainframe bgcolor1" onload="init();">

<form action="#" onsubmit="search();return false;">
    <table>
        <tr>
            <td><fmt:message key="lyrics.artist"/></td>
            <td style="padding-left:0.50em"><input id="artist" type="text" size="40" value="${model.artist}" tabindex="1"/></td>
            <td style="padding-left:0.75em"><input type="submit" value="<fmt:message key='lyrics.search'/>" style="width:6em"
                                                   tabindex="3"/></td>
        </tr>
        <tr>
            <td><fmt:message key="lyrics.song"/></td>
            <td style="padding-left:0.50em"><input id="song" type="text" size="40" value="${model.song}" tabindex="2"/></td>
            <td style="padding-left:0.75em"><input type="button" value="<fmt:message key='common.close'/>" style="width:6em"
                                                   onclick="self.close()" tabindex="4"/></td>
        </tr>
    </table>
</form>
<hr/>
<h2 id="wait"><fmt:message key="lyrics.wait"/></h2>
<h2 id="noLyricsFound" style="display:none"><fmt:message key="lyrics.nolyricsfound"/></h2>
<p id="tryLater" style="display:none"><b><fmt:message key="lyrics.trylater"/></b></p>

<div id="lyrics" style="display:none;">
    <h2 id="lyricsHeader" style="text-align:center;margin-bottom:1em"></h2>

    <div id="lyricsText"></div>

    <p class="detail" style="text-align:right">
        <fmt:message key="lyrics.courtesy"/>
    </p>
</div>

<hr/>
<p style="text-align:center">
    <a href="javascript:self.close()">[<fmt:message key="common.close"/>]</a>
</p>

</body>
</html>
