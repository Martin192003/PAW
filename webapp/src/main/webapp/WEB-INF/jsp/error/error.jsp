<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8"/>
    <title>Error <c:out value="${status}" /></title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/error.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

<!-- Topbar seguro -->
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>

<div class="error-page">
    <div class="error-card">
        <div class="error-title">
            <spring:message code="title.error"/> <br>&nbsp;&nbsp;<c:out value="${status}" />
        </div>
        <div class="card-text-title">
            <spring:message code="error.oops"/>
        </div>
        <p class="card-text-body">
            <c:choose>
                <c:when test="${not empty message}">
                    <c:out value="${message}" />
                </c:when>
                <c:otherwise>
                    <spring:message code="error.default" text="Ha ocurrido un error inesperado."/>
                </c:otherwise>
            </c:choose>
        </p>
        <a class="error-btn" href="<c:url value='/'/>">
            <spring:message code="button.return.main" text="Volver al inicio"/>
        </a>
    </div>
</div>

</body>
</html>



