<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
    <link rel="stylesheet" href="<c:url value='/resources/css/register.css' />" />
    <meta charset="UTF-8">
    <title><spring:message code="button.register"/></title>
</head>
<body>

<div class="register-page">
    <div class="register-group">
        <div class="app-title">
            <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
        </div>
        <div class="register-card">
            <div class="register-title">
                <spring:message code="title.register"/>
            </div>
            <c:url var="registerUrl" value="/register" />
            <spring:message code="form.username" var="phUsername"/>
            <spring:message code="form.name" var="phName"/>
            <spring:message code="form.surname" var="phSurname"/>
            <spring:message code="form.password" var="phPassword"/>
            <spring:message code="form.email" var="phEmail"/>
            <spring:message code="form.cellphone" var="phCellphone"/>
            
            <form:form modelAttribute="userForm" method="post" action="${registerUrl}">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

            <div class="form-group">
                <form:input path="username" cssClass="form-control" placeholder="${phUsername}"/>
                <form:errors path="username" cssClass="error"/>
            </div>
                <div class="form-group">
                    <form:input path="name" cssClass="form-control" placeholder="${phName}"/>
                    <form:errors path="name" cssClass="error"/>
                </div>
                <div class="form-group">
                    <form:input path="surname" cssClass="form-control" placeholder="${phSurname}"/>
                    <form:errors path="surname" cssClass="error"/>
                </div>

                <div class="form-group">
                    <form:password path="password" cssClass="form-control" placeholder="${phPassword}"/>
                    <form:errors path="password" cssClass="error"/>
                </div>

                <div class="form-group">
                    <form:input path="email" cssClass="form-control" placeholder="${phEmail}"/>
                    <form:errors path="email" cssClass="error"/>
                </div>

                <div class="form-group">
                    <form:input path="cellphone" cssClass="form-control" placeholder="${phCellphone}"/>
                    <form:errors path="cellphone" cssClass="error"/>
                </div>

                <div class="register-buttons">
                    <button type="submit" class="btn btn-success"><spring:message code="button.register"/></button>
                    <a href="<c:url value='/login' />" class="register-login"><spring:message code="register.hasaccount"/></a>
                </div>
                </form:form>
            </div>
        </div>
    </div>

</body>
</html>
