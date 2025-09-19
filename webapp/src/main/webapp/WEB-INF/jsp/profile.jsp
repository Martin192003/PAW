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

<div class="sidebar">
    <c:if test="${user.role.name() == 'ADMIN'}">
        <a href="<c:url value='/clubs/activity?userId=8b780366-0279-48c0-b822-8170d2a7bf7f' />"
           class="sidebar-btn ${activePage == 'clubs' ? 'active' : ''}">Mis boliches</a>
    </c:if>

    <a href="#"
       class="sidebar-btn ${activePage == 'reservas' ? 'active' : ''}">Mis reservas</a>

    <a href="<c:url value='/clubs/profile?userId=8b780366-0279-48c0-b822-8170d2a7bf7f' />"
       class="sidebar-btn ${activePage == 'profile' ? 'active' : ''}">Mi perfil</a>

    <a href="#"
       class="sidebar-btn ${activePage == 'ayuda' ? 'active' : ''}">Ayuda</a>
</div>


<div class="mi-perfil">
    <h1>Mi perfil</h1>
    <div class="perfil-card">
        <p><strong>Nombre:</strong> ${user.username}</p>
        <p><strong>Apellido:</strong> ${user.surname}</p>
        <p><strong>Email:</strong> ${user.email}</p>
        <p><strong>Celular:</strong> ${user.cellphone}</p>
        <p><strong>Rol:</strong> ${user.role}</p>
        <p><strong>Fecha de creación:</strong> ${user.createdAt}</p>
    </div>
</div>
</body>
</html>