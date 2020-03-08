<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<link rel="stylesheet" href="<c:url value='/style/smoothness/jquery-ui-1.12.1.min.css'/>" type="text/css">
<link rel="stylesheet" href="<c:url value='/script/jquery.toastmessage/css/jquery.toastmessage.css' />" type="text/css" >
<script type="text/javascript" src="<c:url value='/script/jquery-3.4.0.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/jquery-ui-1.12.1.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/jquery.toastmessage/jquery.toastmessage.js'/>"></script>
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
</script>
