<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>

    <script type="text/javascript" language="javascript">
        function cloneNodeBySelector(selector, idToAppend) {
            var cloned = $(selector).clone();
            cloned.each(function() {
                if (this.id) {
                    this.id = this.id + idToAppend;
                }
            });
            cloned.find("*").each(function() {
                if (this.id) {
                    this.id = this.id + idToAppend;
                }
            });

            return cloned;
        }

        function setImage(imageUrl) {
            $("#wait").show();
            $("#result").hide();
            $("#success").hide();
            $("#error").hide();
            $("#errorDetails").hide();
            $("#noImagesFound").hide();
            top.StompClient.send("/app/coverart/set", JSON.stringify({id: ${model.id}, url: imageUrl}));
        }

        function setImageComplete(errorDetails) {
            $("#wait").hide();
            if (errorDetails != "OK") {
                $("#errorDetails").text(errorDetails).show();
                $("#error").show();
            } else {
                $("#success").show();
            }
        }

        function searchComplete(searchResults) {
            $("#wait").hide();

            if (searchResults.length > 0) {
                var images = $("#images");
                images.empty();

                for (var i = 0; i < searchResults.length; i++) {
                    var result = searchResults[i];
                    var node = cloneNodeBySelector("#template", i);
                    node.appendTo(images);

                    node.find(".search-result-link").attr("href", "javascript:setImage('" + result.imageUrl + "');");
                    node.find(".search-result-image").attr("src", result.imageUrl);
                    node.find(".search-result-artist").text(result.artist);
                    node.find(".search-result-album").text(result.album);

                    node.show();
                }

                $("#result").show();
            } else {
                $("#noImagesFound").show();
            }
        }

        function search() {
            $("#wait").show();
            $("#result").hide();
            $("#success").hide();
            $("#error").hide();
            $("#errorDetails").hide();
            $("#noImagesFound").hide();

            var artist = $("#artist").val();
            var album = $("#album").val();
            top.StompClient.send("/app/coverart/search", JSON.stringify({artist: artist, album: album}));
        }

        function init() {
            top.StompClient.subscribe("changeCoverArt.jsp", {
                "/user/queue/coverart/search": function(msg) {
                    searchComplete(JSON.parse(msg.body));
                },
                "/user/queue/coverart/set": function(msg) {
                    setImageComplete(msg.body);
                }
            }, search);
        }
    </script>
</head>
<body class="mainframe bgcolor1" onload="init()">
<h1><fmt:message key="changecoverart.title"/></h1>
<form action="javascript:search()">
    <sec:csrfInput />
    <table class="indent"><tr>
        <td><input id="artist" name="artist" placeholder="<fmt:message key='changecoverart.artist'/>" size="35" type="text" value="${model.artist}" onclick="select()"/></td>
        <td><input id="album" name="album" placeholder="<fmt:message key='changecoverart.album'/>" size="35" type="text" value="${model.album}" onclick="select()"/></td>
        <td style="padding-left:0.5em"><input type="submit" value="<fmt:message key='changecoverart.search'/>"/></td>
    </tr></table>
</form>

<form action="javascript:setImage($('input[name=\'url\']').val())">
    <sec:csrfInput />
    <table><tr>
        <td><label for="url"><fmt:message key="changecoverart.address"/></label></td>
        <td style="padding-left:0.5em"><input type="text" name="url" size="50" id="url" value="http://" onclick="select()"/></td>
        <td style="padding-left:0.5em"><input type="submit" value="<fmt:message key='common.ok'/>"></td>
    </tr></table>
</form>
<sub:url value="main.view" var="backUrl"><sub:param name="id" value="${model.id}"/></sub:url>
<div style="padding-top:0.5em;padding-bottom:0.5em">
    <div class="back"><a href="${backUrl}"><fmt:message key="common.back"/></a></div>
</div>

<h2 id="wait" style="display:none"><fmt:message key="changecoverart.wait"/></h2>
<h2 id="noImagesFound" style="display:none"><fmt:message key="changecoverart.noimagesfound"/></h2>
<h2 id="success" style="display:none"><fmt:message key="changecoverart.success"/></h2>
<h2 id="error" style="display:none"><fmt:message key="changecoverart.error"/></h2>
<div id="errorDetails" class="warning" style="display:none">
</div>

<div id="result" style="padding-top:2em">
    <div style="clear:both;"></div>
    <div id="images"></div>
    <div style="clear:both;"></div>
    <a href="https://last.fm/" target="_blank" rel="noopener noreferrer">
        <img alt="Lastfm icon" src="<c:url value='/icons/lastfm.gif'/>">
    </a>
    <span class="detail" style="padding-left:1em"><fmt:message key="changecoverart.courtesy"/></span>
</div>

<div id="template" class="coverart dropshadow" style="float:left;margin-right:2.0em;margin-bottom:2.0em;width:250px;display:none">
    <div>
        <a class="search-result-link">
            <img alt="Search result" class="search-result-image" style="width:250px;height:250px">
        </a>
        <div class="search-result-artist caption1"></div>
        <div class="search-result-album caption2"></div>
    </div>
</div>

</body></html>

