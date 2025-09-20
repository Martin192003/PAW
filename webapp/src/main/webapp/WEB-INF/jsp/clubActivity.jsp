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
    <link rel="stylesheet" href="<c:url value='/resources/css/clubActivity.css' />">
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
        <a href="<c:url value='/clubs/activity?userId=737d9292-ace9-4c34-be06-905e4aca0cd0' />"
           class="sidebar-btn ${activePage == 'clubs' ? 'active' : ''}">Mis boliches</a>
    </c:if>

    <a href="#"
       class="sidebar-btn ${activePage == 'reservas' ? 'active' : ''}">Mis reservas</a>

    <a href="<c:url value='/clubs/profile?userId=737d9292-ace9-4c34-be06-905e4aca0cd0' />"
       class="sidebar-btn ${activePage == 'profile' ? 'active' : ''}">Mi perfil</a>

    <a href="#"
       class="sidebar-btn ${activePage == 'ayuda' ? 'active' : ''}">Ayuda</a>
</div>

<div class="app-boliches">
    <h1 class="clubActivity-title">Mis boliches</h1>
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
                </div>
            </div>
        </t:card>
    </c:forEach>
</div>

<script>
    document.addEventListener('DOMContentLoaded', function() {
        const cards = document.querySelectorAll('.boliche-card');
        cards.forEach(card => {
            card.addEventListener('click', function() {
                const clubId = this.getAttribute('data-id');
                if (clubId) {
                    window.location.href = '<c:url value="/clubs/handle?clubId=" />' + clubId;
                }
            });
            card.style.cursor = 'pointer';
        });
    });
</script>
</body>
</html>
