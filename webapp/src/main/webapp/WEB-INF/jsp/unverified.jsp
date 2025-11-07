<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
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
    <title><spring:message code="title.unverified"/></title>
</head>
<body>
<div class="verify-page">
    <div class="app-title">
        <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
    </div>
    <div class="verify-card">
        <div class="verify-content">
            <h1 class="verify-title"><spring:message code="title.accountNotVerified"/></h1>
            <p><spring:message code="message.accountNotVerified"/></p>
            <form action="<c:url value='/resend-verification' />" method="post" class="verify-form">
            <input type="hidden" name="usernameOrEmail" value="${username}">
                <div class="verify-buttons">
                    <button type="submit" class="btn btn-success">
                        <spring:message code="button.resendVerification"/>
                    </button>
                    <a href="<c:url value='/clubs' />" class="return-index">
                        <spring:message code="button.return.main"/>
                    </a>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>