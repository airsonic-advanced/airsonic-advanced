<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
<script type="text/javascript" language="javascript">
    var index = 0;
    var fileCount = ${fn:length(model.songs)};
    function setArtist() {
        var artist = $("input[name='artistAll']").val();
        for (var i = 0; i < fileCount; i++) {
            $("input[name='artist" + i + "']").val(artist);
        }
    }
    function setAlbum() {
        var album = $("input[name='albumAll']").val();
        for (var i = 0; i < fileCount; i++) {
            $("input[name='album" + i + "']").val(album);
        }
    }
    function setYear() {
        var year = $("input[name='yearAll']").val();
        for (var i = 0; i < fileCount; i++) {
            $("input[name='year" + i + "']").val(year);
        }
    }
    function setGenre() {
        var genre = $("input[name='genreAll']").val();
        for (var i = 0; i < fileCount; i++) {
            $("input[name='genre" + i + "']").val(genre);
        }
    }
    function suggestTitle() {
        for (var i = 0; i < fileCount; i++) {
            var title = $("input[name='suggestedTitle" + i + "']").val();
            $("input[name='title" + i + "']").val(title);
        }
    }
    function resetTitle() {
        for (var i = 0; i < fileCount; i++) {
            var title = $("input[name='originalTitle" + i + "']").val();
            $("input[name='title" + i + "']").val(title);
        }
    }
    function suggestTrack() {
        for (var i = 0; i < fileCount; i++) {
            var track = $("input[name='suggestedTrack" + i + "']").val();
            $("input[name='track" + i + "']").val(track);
        }
    }
    function resetTrack() {
        for (var i = 0; i < fileCount; i++) {
            var track = $("input[name='originalTrack" + i + "']").val();
            $("input[name='track" + i + "']").val(track);
        }
    }
    function updateTags() {
        document.getElementById("save").disabled = true;
        index = 0;
        $("#errors").empty();
        for (var i = 0; i < fileCount; i++) {
            $("#status" + i).empty();
        }
        updateNextTag();
    }
    function updateNextTag() {
        var id = $("input[name='id" + index + "']").val();
        var artist = $("input[name='artist" + index + "']").val();
        var track = $("input[name='track" + index + "']").val();
        var album = $("input[name='album" + index + "']").val();
        var title = $("input[name='title" + index + "']").val();
        var year = $("input[name='year" + index + "']").val();
        var genre = $("input[name='genre" + index + "']").val();
        $("#status" + index).append("<fmt:message key="edittags.working"/>");
        top.StompClient.send("/app/tags/edit", JSON.stringify({mediaFileId: id, artist: artist, track: track, album: album, title: title, year: year, genre: genre}));
    }
    function setTagsCallback(result) {
        var message;
        if (result == "SKIPPED") {
            message = "<fmt:message key="edittags.skipped"/>";
        } else if (result == "UPDATED") {
            message = "<b><fmt:message key="edittags.updated"/></b>";
        } else {
            message = "<div class='warning'><fmt:message key="edittags.error"/></div>";
            $("#errors").append("<br>" + result + "<br>");
        }
        $("#status" + index).empty().append(message);
        index++;
        if (index < fileCount) {
            updateNextTag();
        } else {
            document.getElementById("save").disabled = false;
        }
    }

    function init() {
        top.StompClient.subscribe("editTags.jsp", {
            "/user/queue/tags/edit": function(msg) {
                setTagsCallback(msg.body);
            }
        });
        $("input[name='artistAll']").keypress(function(event) {
            if (e.which == 13) {
                setArtist();
                event.preventDefault();
            }
        });
        $("input[name='albumAll']").keypress(function(event) {
            if (e.which == 13) {
                setAlbum();
                event.preventDefault();
            }
        });
        $("input[name='yearAll']").keypress(function(event) {
            if (e.which == 13) {
                setYear();
                event.preventDefault();
            }
        });
    }
</script>
</head>
<body class="mainframe bgcolor1" onload="init()">
<h1><fmt:message key="edittags.title"/></h1>
<sub:url value="main.view" var="backUrl"><sub:param name="id" value="${model.id}"/></sub:url>
<div class="back"><a href="${backUrl}"><fmt:message key="common.back"/></a></div>

<table class="ruleTable indent">
    <tr>
        <th class="ruleTableHeader"><fmt:message key="edittags.file"/></th>
        <th class="ruleTableHeader"><fmt:message key="edittags.track"/></th>
        <th class="ruleTableHeader"><fmt:message key="edittags.songtitle"/></th>
        <th class="ruleTableHeader"><fmt:message key="edittags.artist"/></th>
        <th class="ruleTableHeader"><fmt:message key="edittags.album"/></th>
        <th class="ruleTableHeader"><fmt:message key="edittags.year"/></th>
        <th class="ruleTableHeader"><fmt:message key="edittags.genre"/></th>
        <th class="ruleTableHeader" width="60pt"><fmt:message key="edittags.status"/></th>
    </tr>
    <tr>
        <th class="ruleTableHeader"/>
        <th class="ruleTableHeader"><a href="javascript:suggestTrack()"><fmt:message key="edittags.suggest.short"/></a> |
            <a href="javascript:resetTrack()"><fmt:message key="edittags.reset.short"/></a></th>
        <th class="ruleTableHeader"><a href="javascript:suggestTitle()"><fmt:message key="edittags.suggest"/></a> |
            <a href="javascript:resetTitle()"><fmt:message key="edittags.reset"/></a></th>
        <th class="ruleTableHeader" style="white-space: nowrap"><input type="text" name="artistAll" size="15" value="${model.defaultArtist}"/>&nbsp;<a href="javascript:setArtist()"><fmt:message key="edittags.set"/></a></th>
        <th class="ruleTableHeader" style="white-space: nowrap"><input type="text" name="albumAll" size="15" value="${model.defaultAlbum}"/>&nbsp;<a href="javascript:setAlbum()"><fmt:message key="edittags.set"/></a></th>
        <th class="ruleTableHeader" style="white-space: nowrap"><input type="text" name="yearAll" size="5" value="${model.defaultYear}"/>&nbsp;<a href="javascript:setYear()"><fmt:message key="edittags.set"/></a></th>
        <th class="ruleTableHeader" style="white-space: nowrap">
            <select name="genreAll" style="width:7em">
                <option value=""/>
                <c:forEach items="${model.allGenres}" var="genre">
                    <option ${genre eq model.defaultGenre ? "selected" : ""} value="${genre}">${genre}</option>
                </c:forEach>
            </select>

            <a href="javascript:setGenre()"><fmt:message key="edittags.set"/></a>
        </th>
        <th class="ruleTableHeader"/>
    </tr>

    <c:forEach items="${model.songs}" var="song" varStatus="loopStatus">
        <tr>
            <str:truncateNicely lower="25" upper="25" var="fileName">${song.fileName}</str:truncateNicely>
            <input type="hidden" name="id${loopStatus.index}" value="${song.id}"/>
            <input type="hidden" name="suggestedTitle${loopStatus.index}" value="${song.suggestedTitle}"/>
            <input type="hidden" name="originalTitle${loopStatus.index}" value="${song.title}"/>
            <input type="hidden" name="suggestedTrack${loopStatus.index}" value="${song.suggestedTrack}"/>
            <input type="hidden" name="originalTrack${loopStatus.index}" value="${song.track}"/>
            <td class="ruleTableCell" title="${song.fileName}">${fileName}</td>
            <td class="ruleTableCell"><input type="text" size="5" name="track${loopStatus.index}" value="${song.track}"/></td>
            <td class="ruleTableCell"><input type="text" size="30" name="title${loopStatus.index}" value="${song.title}"/></td>
            <td class="ruleTableCell"><input type="text" size="15" name="artist${loopStatus.index}" value="${song.artist}"/></td>
            <td class="ruleTableCell"><input type="text" size="15" name="album${loopStatus.index}" value="${song.album}"/></td>
            <td class="ruleTableCell"><input type="text" size="5"  name="year${loopStatus.index}" value="${song.year}"/></td>
            <td class="ruleTableCell"><input type="text" name="genre${loopStatus.index}" value="${song.genre}" style="width:7em"/></td>
            <td class="ruleTableCell"><div id="status${loopStatus.index}"/></td>
        </tr>
    </c:forEach>

</table>

<p><input type="submit" id="save" value="<fmt:message key='common.save'/>" onclick="updateTags()"/></p>
<div class="warning" id="errors"/>
</body></html>