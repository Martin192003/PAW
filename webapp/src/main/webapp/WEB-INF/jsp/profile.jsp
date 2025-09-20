<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <meta charset="UTF-8">
    <title>Mi perfil</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/profile.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title">Bling <br>&nbsp;&nbsp;Bling</div>
    </a>
</div>
<c:set var="activePage" value="profile" />
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>
<div class="profile-page">
    <h1 class="profile-title">Mi perfil</h1>
    <div class="profile-card">
        <div class="profile-info-row">
            <span class="profile-label">Nombre:</span>
            <span class="profile-value">${user.username}</span>
        </div>
        <div class="profile-info-row">
            <span class="profile-label">Apellido:</span>
            <span class="profile-value">${user.surname}</span>
        </div>
        <div class="profile-info-row">
            <span class="profile-label">Email:</span>
            <span class="profile-value">${user.email}</span>
        </div>
        <div class="profile-info-row">
            <span class="profile-label">Celular:</span>
            <span class="profile-value">${user.cellphone}</span>
        </div>
        <div class="profile-info-row">
            <span class="profile-label">Rol:</span>
            <span class="profile-value">${user.role}</span>
        </div>
        <div class="profile-info-row">
            <span class="profile-label">Fecha de creación:</span>
            <span class="profile-value">${user.createdAt}</span>
        </div>
    </div>
</div>
</body>
</html>