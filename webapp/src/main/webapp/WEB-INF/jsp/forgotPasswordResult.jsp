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
    <link rel="stylesheet" href="<c:url value='/resources/css/login.css' />" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <meta charset="UTF-8">
    <title><spring:message code="forgot.password.title"/></title>
</head>
<body>
<div class="login-page">
    <div class="login-group">
        <button type="button" class="btn btn-success btn-back" onclick="history.back();">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
        <div class="app-title">
            <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
        </div>
        <div class="login-card">
            <div class="login-title">
                <spring:message code="forgot.password.title"/>
            </div>
            <p class="login-description"><spring:message code="forgot.password.description"/></p>
            <spring:message code="forgot.password.email.placeholder" var="phEmail"/>
            <form:form method="post" modelAttribute="forgotPasswordForm" action="${pageContext.request.contextPath}/forgot-password">
                <div class="form-group">
                    <form:input path="email" type="te" cssClass="form-control" placeholder="${phEmail}" />
                    <form:errors path="email" cssClass="error"/>
                </div>

                <c:if test="${not empty success}">
                    <div class="success" role="alert"><spring:message code="forgot.password.sent"/></div>
                </c:if>

                <div class="login-buttons">
                    <button type="submit" class="btn btn-success">
                        <spring:message code="forgot.password.button"/>
                    </button>
                </div>
            </form:form>
        </div>
    </div>
</div>
</body>
</html>