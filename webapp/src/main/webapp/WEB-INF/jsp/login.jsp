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
    <meta charset="UTF-8">
    <title><spring:message code="title.login"/> </title>
</head>
<body>

<div class="login-page">
    <div class="login-group">
        <div class="app-title">
            <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
        </div>
        <div class="login-card">
            <div class="login-title">
                <spring:message code="title.login"/>
            </div>
            <spring:message code="form.username" var="phUser"/>
            <spring:message code="form.password" var="phPassword"/>

            <form method="post" action="${pageContext.request.contextPath}/login">
                <div class="form-group">
                    <input id="usernameOrEmail" name="usernameOrEmail" type="text" class="form-control" placeholder="${phUser}"/>
                </div>
                <div class="form-group">
                    <input id="password" name="password" type="password" class="form-control" placeholder="${phPassword}"/>
                </div>
                <div>
                    <label>
                        <input name="j_rememberme" type="checkbox"/> <spring:message code="remember_me"/>
                    </label>
                </div>
                <c:if test="${param.error != null}">
                    <div class="error" role="alert">
                        <spring:message code="login.invalid.credentials"/>
                    </div>
                </c:if>
                <div class="login-buttons">
                    <button type="submit" class="btn btn-success"> <spring:message code="button.login"/> </button>
                    <a href="<c:url value='/register' />" class="login-forgot"><spring:message code="login.noaccount"/></a>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>

