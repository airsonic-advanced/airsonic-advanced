<!DOCTYPE html>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>

<html><head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript">
        if (window != window.top) {
            top.location.href = location.href;
        }
    </script>

</head>
<body style="min-width:550px" class="mainframe bgcolor1">
<fmt:message key="sonos.link.ok"/>
</body>
</html>
