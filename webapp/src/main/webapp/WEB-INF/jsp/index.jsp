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
        <form method="get" action="<c:url value='/clubs' />" class="filter-bar">
            <div class="filter-bar">
                <input type="text" name="name" placeholder="Nombre"
                       class="filter-item" value="${name != null ? name : ''}"/>

                <select name="zone" class="filter-item">
                    <option value="">Selecciona una zona</option>
                    <c:forEach var="z" items="${zones}">
                        <option value="${z}" ${zone == z ? 'selected' : ''}>${z}</option>
                    </c:forEach>
                </select>

                <select name="genre" class="filter-item">
                    <option value="">Selecciona un género</option>
                    <c:forEach var="g" items="${genres}">
                        <option value="${g}" ${genre == g ? 'selected' : ''}>${g}</option>
                    </c:forEach>
                </select>

                <select name="price" class="filter-item">
                    <option value="">Seleccione un precio</option>
                    <option value="alto" ${price == 'alto' ? 'selected' : ''}>Alto</option>
                    <option value="medio" ${price == 'medio' ? 'selected' : ''}>Medio</option>
                    <option value="bajo" ${price == 'bajo' ? 'selected' : ''}>Bajo</option>
                </select>

                <input type="number" name="minRating" placeholder="Punt. mínima"
                       class="filter-item" min="0" max="5" step="0.1"
                       value="${minRating != null ? minRating : ''}"/>
                <input type="number" name="maxRating" placeholder="Punt. máxima"
                       class="filter-item" min="0" max="5" step="0.1"
                       value="${maxRating != null ? maxRating : ''}"/>
            </div>
            <button type="submit" class="btn btn-filter">
                <i class="fa-solid fa-filter"></i>
                &nbsp;
                Filtrar
            </button>
        </form>
        <button type="button" class="btn-reset" onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-rotate-left"></i>
            Reset
        </button>
    </div>
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
                    window.location.href = '<c:url value="/clubs/" />' + clubId;
                }
            });
            card.style.cursor = 'pointer';
        });
    });
</script>
</body>
</html>
