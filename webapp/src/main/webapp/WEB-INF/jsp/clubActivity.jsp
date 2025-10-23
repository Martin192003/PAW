<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <meta charset="UTF-8">
    <title><spring:message code="title.myClubs"/></title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/clubActivity.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<c:set var="showRightButtons" value="true" />
<c:set var="showProfileButton" value="false" />
<c:set var="showAddClubButton" value="true" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<c:set var="activePage" value="clubs" />
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>

<div class="app-boliches">
    <div class="detail-back">
        <button type="button" class="btn btn-success" onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
    </div>
    <h1 class="clubActivity-title"><spring:message code="title.myClubs"/></h1>
    <c:forEach var="club" items="${clubs}">
        <t:card size="card-lg" cssClass="boliche-card" dataId="${club.clubId}">
            <div class="card-content">
                <div class="card-left">
                    <c:choose>
                        <c:when test="${not empty photoUrlByClub[club.clubId]}">
                            <img src="<c:url value='${photoUrlByClub[club.clubId]}' />"
                                 alt="<c:out value='${club.clubName}'/>" class="card-image" />
                        </c:when>
                        <c:otherwise>
                            <div class="card-no-image">
                                <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="card-right">
                    <div class="card-text-title">
                        <c:choose>
                            <c:when test="${not empty club.clubName and fn:length(club.clubName) > 30}">
                                <c:out value="${fn:substring(club.clubName, 0, 30)}"/>...
                            </c:when>
                            <c:otherwise>
                                <c:out value="${club.clubName}"/>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <span class="card-text-body">
                    <i class="fa-solid fa-location-dot"></i> <c:out value="${club.location}"/><br>
                    <i class="fa-solid fa-music"></i> <spring:message code="genre.${club.musicGenre}"/><br>
                    <i class="fa-solid fa-dollar-sign"></i> <spring:message code="price.${club.priceRange}"/><br>

                    <c:if test="${not empty club.rating and club.rating gt 0.0}">
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
                    </c:if>
                    <c:choose>
                        <c:when test="${not empty club.description and fn:length(club.description) > 80}">
                            <c:out value="${fn:substring(club.description, 0, 80)}"/>...
                        </c:when>
                        <c:otherwise>
                            <c:out value="${club.description}"/>
                        </c:otherwise>
                    </c:choose>
                </span>
                </div>
            </div>
        </t:card>
    </c:forEach>
    <c:if test="${totalPages > 1}">
        <div class="pagination">
            <c:if test="${page > 1}">
                <a href="?userId=${userId}&page=${page - 1}" class="page-btn">
                    <i class="fa-solid fa-chevron-left"></i>
                </a>
            </c:if>

            <span class="page-info">${page} de ${totalPages}</span>

            <c:if test="${page < totalPages}">
                <a href="?userId=${userId}&page=${page + 1}" class="page-btn">
                    <i class="fa-solid fa-chevron-right"></i>
                </a>
            </c:if>
        </div>
    </c:if>
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

