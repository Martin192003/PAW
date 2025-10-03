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
    <title>Recuperar Contraseña</title>
</head>
<body>
<div class="login-page">
    <div class="login-group">
        <div class="app-title">
            <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
        </div>
        <div class="login-card">
            <div class="login-title">
                Recuperar Contraseña
            </div>
            <p>Ingresa tu email y te enviaremos instrucciones para recuperar tu contraseña.</p>

            <form method="post" action="${pageContext.request.contextPath}/forgot-password">
                <div class="form-group">
                    <input id="email" name="email" type="email" class="form-control" placeholder="Email" required/>
                </div>
                <div class="login-buttons">
                    <button type="submit" class="btn btn-success">Enviar</button>
                    <a href="<c:url value='/login' />" class="login-forgot">Volver al Login</a>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>