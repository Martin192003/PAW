<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/contactForm.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/verify.css' />" />
    <meta charset="UTF-8">
    <title><spring:message code="title.verify"/></title>
</head>
<body>
<div class="verify-page">
    <div class="app-title">
        <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
    </div>
    <div class="verify-card">
        <div class="verify-content">
            <h1 class="verify-title"><spring:message code="title.verify"/></h1>

            <spring:message code="placeholder.token" var="phToken"/>
            <form action="${pageContext.request.contextPath}/verify" method="post" class="verify-form">
                <label for="token"><spring:message code="verify.enterToken"/></label>
                <input type="text" id="token" name="token" placeholder="${phToken}" class="form-control" required>
                <div class="verify-buttons">
                    <button type="submit" class="btn btn-success">
                        <spring:message code="button.verify"/>
                    </button>
                    <a href="<c:url value='/' />" class="return-index"><spring:message code="return.index"/></a>
                </div>
            </form>

            <c:if test="${not empty success}">
                <p class="verify-success">${success}</p>
            </c:if>
            <c:if test="${not empty error}">
                <p class="verify-error">${error}</p>
            </c:if>
        </div>
    </div>
</div>
</body>
</html>
