<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title><spring:message code="app.title"/> <spring:message code="app.title"/></title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<c:set var="showProfileButton" value="true" />
<c:set var="showAddClubButton" value="true" />
<c:set var="showRightButtons" value="true" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<div class="app-boliches">
    <div class="events-container">
        <c:set var="hasEvents" value="false" />
        <c:forEach var="club" items="${clubsForEvents}">
            <c:set var="clubEvents" value="${eventsByClub[club.clubId]}"/>
            <c:if test="${not empty clubEvents}">
                <c:set var="hasEvents" value="true" />
            </c:if>
        </c:forEach>

        <c:if test="${hasEvents}">
            <h2 class="events-title"><spring:message code="title.events"/></h2>

            <div class="events-carousel-wrapper">
                <button class="carousel-btn left" onclick="scrollCarousel(-1)">
                    <i class="fa-solid fa-chevron-left"></i>
                </button>

                <div class="events-carousel" id="eventsCarousel">
                    <c:forEach var="club" items="${clubsForEvents}">
                        <c:set var="clubEvents" value="${eventsByClub[club.clubId]}"/>
                        <c:if test="${not empty clubEvents}">
                            <c:forEach var="e" items="${clubEvents}">
                                <c:url var="eventUrl" value="/clubs/${club.clubId}">
                                    <c:param name="date" value="${e.date}"/>
                                    <c:param name="fromEvent" value="true"/>
                                </c:url>

                                <a class="event-card" href="${eventUrl}">
                                    <div class="event-card-body">
                                        <div class="event-card-image">
                                            <c:choose>
                                                <c:when test="${not empty e.photoId}">
                                                    <div class="event-card-image">
                                                        <img style="object-fit: contain; background-color: black" src="${pageContext.request.contextPath}/photos/${e.photoId}" alt="<spring:message code="form.eventImage" text="Imagen del evento"/>" />
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <div class="event-card-no-image">
                                                        <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
                                                    </div>
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div class="event-club-title">
                                            <i class="fa-solid fa-martini-glass"></i>
                                            <c:out value="${club.clubName}"/>
                                        </div>
                                        <div class="event-date">
                                            <i class="fa-regular fa-calendar"></i>
                                            <c:choose>
                                                <c:when test="${fn:contains(e.date, '-')}">
                                                    <fmt:parseDate var="eventDate" value="${e.date}" pattern="yyyy-MM-dd"/>
                                                </c:when>
                                                <c:otherwise>
                                                    <c:set var="eventDate" value="${e.date}"/>
                                                </c:otherwise>
                                            </c:choose>
                                            <fmt:formatDate value="${eventDate}" pattern="d" var="eventDay"/>
                                            <fmt:formatDate value="${eventDate}" pattern="MMMM" var="eventMonth"/>
                                            <fmt:formatDate value="${eventDate}" pattern="yyyy" var="eventYear"/>
                                                ${eventDay} <spring:message code="pagination.of"/><span class="month">${eventMonth}</span>${eventYear}
                                        </div>
                                        <div class="event-name"><c:out value="${e.eventName}"/></div>
                                        <c:if test="${not empty e.description}">
                                            <div class="event-desc">
                                                <c:choose>
                                                    <c:when test="${fn:length(e.description) > 80}">
                                                        <c:out value="${fn:substring(e.description, 0, 80)}"/>...
                                                    </c:when>
                                                    <c:otherwise>
                                                        <c:out value="${e.description}"/>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </c:if>
                                    </div>
                                </a>
                            </c:forEach>
                        </c:if>
                    </c:forEach>
                </div>

                <button class="carousel-btn right" onclick="scrollCarousel(1)">
                    <i class="fa-solid fa-chevron-right"></i>
                </button>
            </div>
        </c:if>
    </div>


    <div class="filter-container">
        <spring:message code="filter.name" var="phName"/>
        <spring:message code="filter.minRating" var="phMinrating"/>
        <spring:message code="filter.maxRating" var="phMaxrating"/>
        <c:set var="anyFilterApplied" value="${not empty name or not empty zone or not empty genre or not empty price or not empty isBar or not empty sort}" />
        <form method="get" action="<c:url value='/clubs' />" class="filter-bar ${anyFilterApplied ? 'has-filters' : ''}">            <div class="filter-bar">
                <!-- Name -->
                <div class="input-with-icon">
                    <i class="fa-solid fa-magnifying-glass" aria-hidden="true"></i>
                    <input
                            type="text"
                            name="name"
                            placeholder=""
                            aria-label="<c:out value='${phName}'/>"
                            class="filter-item"
                            value="<c:out value='${not empty param.name ? param.name : name}'/>"
                    />
                </div>

                <!-- Zone -->
                <select name="zone" class="filter-item">
                    <option value=""><spring:message code="filter.zone"/></option>
                    <c:forEach var="z" items="${zones}">
                        <option value="${z.dbValue}"
                            ${ (zone == z.dbValue || param.zone == z.dbValue) ? 'selected' : '' }>
                            <spring:message code="zone.${z.dbValue}"/>
                        </option>
                    </c:forEach>
                </select>

                <!-- Genre -->
                <select name="genre" class="filter-item">
                    <option value=""><spring:message code="filter.genre"/></option>

                    <c:forEach var="g" items="${genres}">
                        <c:set var="code" value="${g.name() != null ? g.name() : g}" />
                        <option value="${code}"
                            ${ (genre == code || param.genre == code) ? 'selected' : '' }>
                            <spring:message code="genre.${code}" text="${code}"/>
                        </option>
                    </c:forEach>
                </select>

                <!-- Price -->
                <select name="price" class="filter-item">
                    <option value=""><spring:message code="filter.price"/></option>
                    <option value="alto"  ${ (price == 'alto'  || param.price == 'alto')  ? 'selected' : '' }><spring:message code="filter.price.high"/></option>
                    <option value="medio" ${ (price == 'medio' || param.price == 'medio') ? 'selected' : '' }><spring:message code="filter.price.medium"/></option>
                    <option value="bajo"  ${ (price == 'bajo'  || param.price == 'bajo')  ? 'selected' : '' }><spring:message code="filter.price.low"/></option>
                </select>

                <!-- Venue type -->
                <select name="isBar" class="filter-item">
                    <option value=""><spring:message code="form.venueType"/></option>
                    <option value="true"
                    ${ (isBar == true || isBar == 'true' || param.isBar == 'true') ? 'selected' : '' }>
                        <spring:message code="label.bar"/>
                    </option>
                    <option value="false"
                    ${ (isBar == false || isBar == 'false' || param.isBar == 'false') ? 'selected' : '' }>
                        <spring:message code="label.notBar"/>
                    </option>
                </select>

                <!-- Sort -->
                <select name="sort" class="filter-item filter-sort">
                    <option value=""><spring:message code="filter.sort" text="Ordenar por"/></option>

                    <option value="availability_d0_desc" ${ (sort == 'availability_d0_desc' || param.sort == 'availability_d0_desc') ? 'selected' : '' }>
                        <spring:message code="filter.sort.availability.today" text="Disponibles HOY (más primero)"/>
                    </option>
                    <option value="availability_d1_desc" ${ (sort == 'availability_d1_desc' || param.sort == 'availability_d1_desc') ? 'selected' : '' }>
                        <spring:message code="filter.sort.availability.tomorrow" text="Disponibles MAÑANA (más primero)"/>
                    </option>
                    <option value="availability_d2_desc" ${ (sort == 'availability_d2_desc' || param.sort == 'availability_d2_desc') ? 'selected' : '' }>
                        <spring:message code="filter.sort.availability.dayafter" text="Disponibles PASADO (más primero)"/>
                    </option>

                    <option value="rating_desc" ${ (sort == 'rating_desc' || param.sort == 'rating_desc') ? 'selected' : '' }>
                        <spring:message code="filter.sort.ratingDesc" text="Rating (mayor a menor)"/>
                    </option>
                    <option value="rating_asc"  ${ (sort == 'rating_asc'  || param.sort == 'rating_asc')  ? 'selected' : '' }>
                        <spring:message code="filter.sort.ratingAsc" text="Rating (menor a mayor)"/>
                    </option>
                </select>
            </div>
            <button type="submit" class="btn btn-filter">
                <i class="fa-solid fa-filter"></i>&nbsp;<spring:message code="button.filter"/>
            </button>
        </form>
        <c:set var="anyFilterApplied" value="${not empty param.name or not empty name or not empty param.zone or not empty zone or not empty param.genre or not empty genre or not empty param.price or not empty price or not empty param.isBar or not empty isBar or not empty param.sort or not empty sort}" />
        <button id="resetButton" type="button" class="btn-reset" <c:if test="${not anyFilterApplied}">style="display:none;"</c:if>
                onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-rotate-right"></i>
            <spring:message code="button.reset"/>
        </button>
    </div>


    <c:if test="${not empty zone}">
        <c:set var="zoneLabel" value="${zone}" />
        <c:forEach var="z" items="${zones}">
            <c:if test="${zone == z.dbValue or zone == z}">
                <c:set var="zoneLabel" value="${z.displayName}" />
            </c:if>
        </c:forEach>
    </c:if>

    <c:if test="${empty clubs and (anyFilterApplied or invalidDay)}">
        <div class="no-results">
            <c:choose>
                <c:when test="${invalidDay}">
                    <spring:message code="validation.day.not.allowed"
                                    text="Solo se pueden reservar mesas los días jueves, viernes y sábado"/>
                </c:when>
                <c:otherwise>
                    <spring:message code="error.noresults"/>
                </c:otherwise>
            </c:choose>

            <button type="button" class="btn-reset" style="margin-top:16px;"
                    onclick="window.location.href='<c:url value="/clubs"/>'">
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
    <c:if test="${totalPages > 1}">
        <div class="pagination">
            <c:if test="${page > 1}">
                <a href="?${queryParams}&page=${page - 1}" class="page-btn">
                    <i class="fa-solid fa-chevron-left"></i>
                </a>
            </c:if>

            <span class="page-info">
                    ${page} <spring:message code="pagination.of"/> ${totalPages}
            </span>

            <c:if test="${page < totalPages}">
                <a href="?${queryParams}&page=${page + 1}" class="page-btn">
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
                const clubId = this.getAttribute('data-id') || this.getAttribute('dataId');
                if (clubId) {
                    window.location.href = '<c:url value="/clubs/" />' + clubId;
                }
            });
            card.style.cursor = 'pointer';
        });
    });
    function scrollCarousel(direction) {
        const carousel = document.getElementById('eventsCarousel');
        const scrollAmount = 300;
        carousel.scrollBy({ left: direction * scrollAmount, behavior: 'smooth' });
    }
    document.addEventListener('DOMContentLoaded', function() {
        const form = document.querySelector('form.filter-bar');
        const resetBtn = document.getElementById('resetButton');
        if (!form || !resetBtn) return;

        const inputs = Array.from(form.querySelectorAll('input[name], select[name]'));

        function anyFilled() {
            return inputs.some(el => {
                const v = el.value;
                if (!v) return false;
                if (el.tagName.toLowerCase() === 'input') return v.trim() !== '';
                return v !== '';
            });
        }

        function toggleReset() {
            const hasFilters = anyFilled();
            resetBtn.style.display = hasFilters ? '' : 'none';

            if (hasFilters) {
                form.classList.add('has-filters');
            } else {
                form.classList.remove('has-filters');
            }
        }

        inputs.forEach(el => {
            el.addEventListener('input', toggleReset);
            el.addEventListener('change', toggleReset);
        });

        toggleReset();
    });
</script>
</body>
</html>
