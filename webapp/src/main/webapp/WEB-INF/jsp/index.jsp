<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bling Bling</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title">Bling <br>&nbsp;&nbsp;Bling</div>
    </a>
</div>

<div class="app-boliches">
    <div class="filter-container">
        <div class="filter-bar">
            <input type="text" name="nombre" placeholder="Nombre" class="filter-item" />

            <select name="zona" class="filter-item" required>
                <option value="" disabled selected>Selecciona una zona</option>
                <option value="caba">Ciudad Autónoma de Buenos Aires</option>
                <option value="norte">Norte</option>
                <option value="sur">Sur</option>
                <option value="oeste">Oeste</option>
            </select>

            <select name="genero" class="filter-item" required>
                <option value="" disabled selected>Selecciona un género</option>
                <option value="rock">Rock</option>
                <option value="pop">Pop</option>
                <option value="cachengue">Cachengue</option>
                <option value="techno">Techno</option>
                <option value="reggaeton">Reggaeton</option>
                <option value="cumbia">Cumbia</option>
                <option value="trap">Trap</option>
                <option value="jazz">Jazz</option>
                <option value="otro">Otro</option>
            </select>

            <select name="precio" class="filter-item" required>
                <option value="" disabled selected>Seleccione un precio</option>
                <option value="alto">Alto</option>
                <option value="medio">Medio</option>
                <option value="bajo">Bajo</option>
            </select>

            <input type="number" name="puntuacionMin" placeholder="Punt. mínima" class="filter-item" min="0" max="5" step="0.1"/>
            <input type="number" name="puntuacionMax" placeholder="Punt. máxima" class="filter-item" min="0" max="5" step="0.1"/>
        </div>
        <button type="submit" class="btn btn-filter">Filtrar</button>
    </div>
    <c:forEach var="club" items="${clubs}">
        <t:card size="card-lg" cssClass="boliche-card" dataId="${club.clubId}">
            <div class="card-content">
                <div class="card-left">
                    <img src="<c:url value='${photoUrlByClub[club.clubId]}' />"
                         alt="${club.clubName}" class="card-image" />
                </div>
                <div class="card-right">
                    <div class="card-text-title">${club.clubName}</div>
                    <span class="card-text-body">
                    <i class="fa-solid fa-location-dot"></i> ${club.location}<br>
                     <c:set var="ratingInt" value="${Math.floor(club.rating)}" />
                    <c:set var="decimal" value="${club.rating - ratingInt}" />
                    <c:set var="halfStar" value="${decimal > 0 ? 1 : 0}" />
                    <c:set var="fullStars" value="${ratingInt}" />
                    <c:set var="emptyStars" value="${5 - (fullStars + halfStar)}" />
                    <c:forEach var="i" begin="1" end="${fullStars}">
                        <i class="fa-solid fa-star" style="color:#FFD700;"></i>
                    </c:forEach>
                    <c:if test="${halfStar == 1}">
                        <i class="fa-solid fa-star-half-stroke" style="color:#FFD700;"></i>
                    </c:if>
                    <c:forEach var="i" begin="1" end="${emptyStars}">
                        <i class="fa-regular fa-star" style="color:#bab8b8;"></i>
                    </c:forEach>

                    ${club.rating}<br>
                    ${club.description}
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
                    window.location.href = '<c:url value="/clubs/" />' + clubId;
                }
            });
            card.style.cursor = 'pointer';
        });
    });
</script>
</body>
</html>
