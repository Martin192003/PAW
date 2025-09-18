<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
    <style>
        body {
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
        }
        .error-card {
            max-width: 500px;
            padding: 2rem;
            text-align: center;
        }
        .error-title {
            padding: 1rem;
            font-size: 3rem;
            margin-bottom: 20px;
            border-radius: 8px;
            background: #222;
            color: #9c27b0;
            font-family: 'Darklinos', Arial, sans-serif;
            text-shadow: 2px 2px 4px rgba(216, 0, 183, 0.3);
            line-height: 1.2;
        }
    </style>
</head>
<body>

<div class="card error-card">
    <div class="error-title">
        Error <br>&nbsp;&nbsp;<c:out value="${status}" />
    </div>

    <div class="card-text-title" style="font-size: 2rem; margin-bottom: 1rem; color: #9c27b0;">¡Oops! Ha ocurrido un error.</div>
    <p class="card-text-body" style="font-size: 1.2rem; color: #ccc;">
        <c:out value="${message}" />
    </p>

    <div style="margin-top:2rem;">
        <a class="btn error-btn" href="<c:url value='/'/>">Volver a la página principal</a>
    </div>

</div>

</body>
</html>