<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <meta charset="UTF-8">
    <title><spring:message code="title.myClubs"/></title>
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
        <div class="app-title"><spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/></div>
    </a>
</div>
<c:set var="activePage" value="clubs" />
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>

<div class="app-boliches">
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
                            <c:when test="${not empty club.clubName and fn:length(club.clubName) > 20}">
                                <c:out value="${fn:substring(club.clubName, 0, 20)}"/>...
                            </c:when>
                            <c:otherwise>
                                <c:out value="${club.clubName}"/>
                            </c:otherwise>
                        </c:choose>
                    </div>
                    <span class="card-text-body">
                        <i class="fa-solid fa-location-dot"></i> <c:out value="${club.location}"/><br>
                            ${club.rating}<br>
                        <c:choose>
                            <c:when test="${not empty club.description and fn:length(club.description) > 75}">
                                <c:out value="${fn:substring(club.description, 0, 75)}"/>...
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

