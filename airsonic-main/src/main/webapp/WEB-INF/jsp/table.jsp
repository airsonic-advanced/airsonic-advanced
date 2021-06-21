<%@ page contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<link rel="stylesheet" href="<c:url value='/script/DataTables/datatables.min.css'/>" type="text/css" >
<style type="text/css">
    table.dataTable thead .sorting, table.dataTable thead .sorting_asc, table.dataTable thead .sorting_desc, table.dataTable thead .sorting_asc_disabled, table.dataTable thead .sorting_desc_disabled {
        background: none !important;
    }
    table.dataTable.hide-table-header thead {
        display: none !important;
    }
</style>
<c:set var="styleSheet"><spring:theme code="styleSheet"/></c:set>
<link rel="stylesheet" href="<c:url value='/${styleSheet}'/>" type="text/css">
<script type="text/javascript" src="<c:url value='/script/DataTables/datatables.min.js'/>"></script>
<script type="text/javascript" src="<c:url value='/script/DataTables/row.show.js'/>"></script>
