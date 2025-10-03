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
    <link rel="stylesheet" href="<c:url value='/resources/css/login.css' />" />
    <meta charset="UTF-8">
    <title><spring:message code="reset.password.title"/></title>
</head>
<body>
<div class="login-page">
    <div class="login-group">
        <div class="app-title">
            <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
        </div>
        <div class="login-card">
            <div class="login-title">
                <spring:message code="reset.password.title"/>
            </div>

            <spring:message code="reset.password.new" var="phNewPassword"/>
            <spring:message code="reset.password.confirm" var="phConfirmPassword"/>

            <form method="post" action="${pageContext.request.contextPath}/reset-password">
                <input type="hidden" name="token" value="${token}"/>
                <div class="form-group">
                    <input id="password" name="password" type="password" class="form-control"
                           placeholder="${phNewPassword}" required minlength="6"/>
                </div>
                <div class="form-group">
                    <input id="confirmPassword" name="confirmPassword" type="password" class="form-control"
                           placeholder="${phConfirmPassword}" required minlength="6"/>
                </div>

                <c:if test="${not empty error}">
                    <div class="error" role="alert">
                            ${error}
                    </div>
                </c:if>

                <c:if test="${not empty success}">
                    <div class="success" role="alert">
                            ${success}
                    </div>
                </c:if>

                <div class="login-buttons">
                    <button type="submit" class="btn btn-success"><spring:message code="reset.password.button"/></button>
                    <a href="<c:url value='/login' />" class="login-forgot"><spring:message code="reset.password.back.login"/></a>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>