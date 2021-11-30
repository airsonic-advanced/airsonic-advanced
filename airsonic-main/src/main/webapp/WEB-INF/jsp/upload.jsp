<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>

<html><head>
    <%@ include file="head.jsp" %>
</head><body>

<h1><fmt:message key="upload.title"/></h1>

<c:forEach items="${model.uploadedFiles}" var="file">
    <p><fmt:message key="upload.success"><fmt:param value="${fn:escapeXml(file.toString())}"/></fmt:message></p>
</c:forEach>

<c:forEach items="${model.unzippedFiles}" var="file">
    <fmt:message key="upload.unzipped"><fmt:param value="${fn:escapeXml(file.toString())}"/></fmt:message><br/>
</c:forEach>

<c:forEach items="${model.exceptions}" var="exception">
    <p><fmt:message key="upload.failed"><fmt:param value="${fn:escapeXml(exception.message)}"/></fmt:message></p>
</c:forEach>

<c:choose>
    <c:when test="${empty model.uploadedFiles}">
        <p><fmt:message key="upload.empty"/></p>
    </c:when>
</c:choose>

<div class="back"><a href="javascript:history.back()"><fmt:message key="common.back"/></a></div>
</body></html>
