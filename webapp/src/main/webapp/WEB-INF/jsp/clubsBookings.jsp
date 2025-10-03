<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <title><spring:message code="bookings.title.myclubs"/></title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookings.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>


<c:set var="showRightButtons" value="true" />
<c:set var="showProfileButton" value="false" />
<c:set var="showAddClubButton" value="false" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<c:set var="activePage" value="clubsBookings" />
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>

    <div class="app-bookings">
        <div class="detail-back">
            <button type="button" class="btn btn-success" onclick="window.location.href='<c:url value="/clubs"/>'">
                <i class="fa-solid fa-arrow-left"></i>
            </button>
        </div>
        <h1><spring:message code="bookings.reservations.myclubs"/></h1>
        <c:if test="${empty bookings}">
            <div style="color:#ffffff; font-size:1.1rem; margin-top:24px;"><spring:message code="bookings.reservations.nobookings"/></div>
        </c:if>
        <c:forEach var="booking" items="${bookings}">
        <a href="<c:url value='/admin/booking?bookingId=${booking.bookingId}' />" style="text-decoration:none; color:inherit;">
            <div class="card-lg booking-card" style="position:relative; margin-bottom:24px;">
                    <span><b><spring:message code="bookings.id"/></b> <c:out value="${booking.bookingId}"/></span><br/>
                    <span><b><spring:message code="bookings.club"/></b> <c:out value="${clubNames[booking.bookingId]}"/></span><br/>
                    <div class="booking-status-container">
                        <c:choose>
                            <c:when test="${fn:trim(fn:toUpperCase(booking.status)) == 'PENDIENTE'}">
                                <p class="status-badge is-pending"><spring:message code="booking.status.pending" text="Pendiente"/></p>
                            </c:when>
                            <c:when test="${fn:trim(fn:toUpperCase(booking.status)) == 'CONFIRMADA'}">
                                <p class="status-badge is-confirmed"><spring:message code="booking.status.confirmed" text="Confirmada"/></p>
                            </c:when>
                            <c:when test="${fn:trim(fn:toUpperCase(booking.status)) == 'CANCELADA'}">
                                <p class="status-badge is-cancelled"><spring:message code="booking.status.cancelled" text="Cancelada"/></p>
                            </c:when>
                            <c:otherwise>
                                <p class="status-badge">${booking.status}</p>
                            </c:otherwise>
                        </c:choose>
                    </div>
            </div>
        </a>
        </c:forEach>
    </div>
</body>
</html>
