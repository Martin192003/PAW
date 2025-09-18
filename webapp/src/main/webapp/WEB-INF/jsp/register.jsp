<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/register.css' />" />
    <meta charset="UTF-8">
    <title>Register</title>
</head>
<body>

<div class="register-page">
    <div class="register-group">
        <div class="app-title">
            Bling <br>&nbsp;&nbsp;Bling
        </div>
        <div class="register-card">
            <div class="register-title">
                Crear cuenta
            </div>

            <c:url var="registerUrl" value="/register" />

            <form:form modelAttribute="userForm" method="post" action="${registerUrl}">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

            <div class="form-group">
                <form:input path="username" cssClass="form-control" placeholder="Nombre de usuario"/>
                <form:errors path="username" cssClass="error"/>
            </div>
                <div class="form-group">
                    <form:input path="surname" cssClass="form-control" placeholder="Apellido"/>
                    <form:errors path="surname" cssClass="error"/>
                </div>

                <div class="form-group">
                    <form:password path="password" cssClass="form-control" placeholder="Contraseña"/>
                    <form:errors path="password" cssClass="error"/>
                </div>

                <div class="form-group">
                    <form:input path="email" cssClass="form-control" placeholder="Email"/>
                    <form:errors path="email" cssClass="error"/>
                </div>

                <div class="form-group">
                    <form:input path="cellphone" cssClass="form-control" placeholder="Celular"/>
                    <form:errors path="cellphone" cssClass="error"/>
                </div>

                <div class="register-buttons">
                    <button type="submit" class="btn btn-success">Registrarse</button>
                    <a href="<c:url value='/login' />" class="register-login">¿Ya tienes cuenta? Inicia sesión</a>
                </div>
                </form:form>
            </div>
        </div>
    </div>

</body>
</html>
