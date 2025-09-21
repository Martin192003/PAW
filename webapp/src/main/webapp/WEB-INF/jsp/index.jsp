<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="app.title"/> <spring:message code="app.title"/></title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<c:set var="showRightButtons" value="true" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<div class="app-boliches">
    <div class="filter-container">
        <spring:message code="filter.name" var="phName"/>
        <spring:message code="filter.minRating" var="phMinrating"/>
        <spring:message code="filter.maxRating" var="phMaxrating"/>
        <form method="get" action="<c:url value='/clubs' />" class="filter-bar">
            <div class="filter-bar">
                <input type="text" name="name" placeholder="${phName}"
                       class="filter-item" value="${name != null ? name : ''}" />

                <select name="zone" class="filter-item">
                    <option value=""><spring:message code="filter.zone"/></option>
                    <c:forEach var="z" items="${zones}" >
                        <option value="${z.dbValue}" ${zone == z.dbValue ? 'selected' : ''}>
                            <spring:message code="zone.${z.dbValue}"/>
                        </option>
                    </c:forEach>
                </select>

                <select name="genre" class="filter-item">
                    <option value=""><spring:message code="filter.genre"/></option>
                    <c:forEach var="g" items="${genres}">
                        <option value="${g}" ${genre == g ? 'selected' : ''}>
                            <spring:message code="genre.${g}"/>
                        </option>
                    </c:forEach>
                </select>

                <select name="price" class="filter-item">
                    <option value=""><spring:message code="filter.price"/></option>
                    <option value="alto" ${price == 'alto' ? 'selected' : ''}><spring:message code="filter.price.high"/></option>
                    <option value="medio" ${price == 'medio' ? 'selected' : ''}><spring:message code="filter.price.medium"/></option>
                    <option value="bajo" ${price == 'bajo' ? 'selected' : ''}><spring:message code="filter.price.low"/></option>
                </select>
                <select name="isBar" class="filter-item">
                    <option value=""><spring:message code="filter.venueType"/></option>
                    <option value="true" ${isBar == true ? 'selected' : ''}>
                        <spring:message code="label.bar"/>
                    </option>
                    <option value="false" ${isBar == false ? 'selected' : ''}>
                        <spring:message code="label.notBar"/>
                    </option>
                </select>
                <select name="sort" class="filter-item filter-sort">
                    <option value=""><spring:message code="filter.sort" text="Ordenar por"/></option>
                    <option value="rating_desc" ${sort == 'rating_desc' ? 'selected' : ''}>
                        <spring:message code="filter.sort.ratingDesc" text="Rating (mayor a menor)"/>
                    </option>
                    <option value="rating_asc" ${sort == 'rating_asc' ? 'selected' : ''}>
                        <spring:message code="filter.sort.ratingAsc" text="Rating (menor a mayor)"/>
                    </option>
                </select>
            </div>

            <button type="submit" class="btn btn-filter">
                <i class="fa-solid fa-filter"></i>
                &nbsp;
                <spring:message code="button.filter"/>
            </button>
        </form>
        <button type="button" class="btn-reset" onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-rotate-right"></i>
            <spring:message code="button.reset"/>
        </button>
    </div>

    <c:set var="anyFilterApplied" value="${not empty name or not empty zone or not empty genre or not empty price or not empty minRating or not empty maxRating}" />

    <c:if test="${not empty zone}">
        <c:set var="zoneLabel" value="${zone}" />
        <c:forEach var="z" items="${zones}">
            <c:if test="${zone == z.dbValue or zone == z}">
                <c:set var="zoneLabel" value="${z.displayName}" />
            </c:if>
        </c:forEach>
    </c:if>

    <c:if test="${empty clubs and anyFilterApplied}">
        <div class="no-results">
            <spring:message code="error.noresults"/>
            <button type="button" class="btn-reset" style="margin-top:16px;" onclick="window.location.href='<c:url value="/clubs"/>'">
                <i class="fa-solid fa-rotate-right"></i>
                <spring:message code="button.reset"/>
            </button>
        </div>
    </c:if>

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
                                <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="card-right">
                    <div class="card-text-title"><c:out value="${club.clubName}"/></div>
                    <span class="card-text-body">
                    <i class="fa-solid fa-location-dot"></i> <c:out value="${club.location}"/><br>
                    <i class="fa-solid fa-music"></i> <spring:message code="genre.${club.musicGenre}"/><br>
                    <i class="fa-solid fa-dollar-sign"></i> <spring:message code="price.${club.priceRange}"/><br>

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

                <div class="bar-badge ${club.isBar ? 'bar' : 'nightclub'}">
                    <c:choose>
                        <c:when test="${club.isBar}">
                            <i class="fa-solid fa-beer-mug-empty"></i>
                            <spring:message code="label.bar" text="Bar"/>
                        </c:when>
                        <c:otherwise>
                            <i class="fa-solid fa-martini-glass"></i>
                            <spring:message code="label.notBar" text="Nightclub"/>
                        </c:otherwise>
                    </c:choose>
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
