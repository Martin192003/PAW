<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <meta charset="UTF-8">
    <title>Mis boliches</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
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
    <a href="<c:url value='/clubs/activity?userId=8966c8cf-b910-4d61-b748-c0b77acd9d9a' />"
       class="sidebar-btn ${activePage == 'clubs' ? 'active' : ''}">Mis boliches</a>

    <a href="#"
       class="sidebar-btn ${activePage == 'reservas' ? 'active' : ''}">Mis reservas</a>

    <a href="<c:url value='/clubs/profile?userId=8966c8cf-b910-4d61-b748-c0b77acd9d9a' />"
       class="sidebar-btn ${activePage == 'profile' ? 'active' : ''}">Mi perfil</a>

    <a href="#"
       class="sidebar-btn ${activePage == 'ayuda' ? 'active' : ''}">Ayuda</a>
</div>

<div class="app-boliches">
    <h1>Mis boliches</h1>
    <c:forEach var="club" items="${clubs}">
        <t:card size="card-lg" cssClass="boliche-card" dataId="${club.clubId}">
            <div class="card-content">
                <div class="card-left">
                    <c:choose>
                        <c:when test="${not empty photoUrlByClub[club.clubId]}">
                            <img src="<c:url value='${photoUrlByClub[club.clubId]}' />"
                                 alt="${club.clubName}" class="card-image" />
                        </c:when>
                        <c:otherwise>
                            <div class="card-no-image">
                                Bling <br>&nbsp;&nbsp;Bling
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="card-right">
                    <div class="card-text-title"><c:out value="${club.clubName}"/></div>
                    <span class="card-text-body">
                        <i class="fa-solid fa-location-dot"></i> <c:out value="${club.location}"/><br>
                            ${club.rating}<br>
                        <c:out value="${club.description}"/>
                    </span>
                    <br>
                    <a href="<c:url value='/clubs/handle?clubId=${club.clubId}' />" class="edit-field-btn">
                        Editar Boliche
                    </a>
                </div>
            </div>
        </t:card>
    </c:forEach>
</div>
</body>
</html>