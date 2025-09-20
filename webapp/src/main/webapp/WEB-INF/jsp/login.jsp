<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/contactForm.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/login.css' />" />
    <meta charset="UTF-8">
    <title>Login</title>
</head>
<body>

<div class="login-page">
    <div class="login-group">
        <div class="app-title">
            Bling <br>&nbsp;&nbsp;Bling
        </div>
        <div class="login-card">
            <div class="login-title">
                Inicia sesión
            </div>
            <form action="<c:url value='/login' />" method="post" enctype="application/x-www-form-urlencoded">
                <div class="form-group">
                    <input id="usernameOrEmail" name="usernameOrEmail" type="text" class="form-control" placeholder="Usuario o email"/>
                </div>
                <div class="form-group">
                    <input id="password" name="password" type="password" class="form-control" placeholder="Contraseña"/>
                </div>
                <div class="login-buttons">
                    <button type="submit" class="btn btn-success">Ingresar</button>
                    <a href="<c:url value='/register' />" class="login-forgot">¿No tienes cuenta? Registrate</a>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>
