<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <title>Reservas de mis clubes</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/clubActivity.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookings.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookingUserHandler.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>


<c:set var="showRightButtons" value="true" />
<c:set var="showProfileButton" value="false" />
<c:set var="showAddClubButton" value="true" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<c:set var="activePage" value="clubsBookings" />
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>

    <div class="app-bookings">
        <h1>Reservas de mis clubes</h1>
        <c:if test="${empty bookings}">
            <div style="color:#e53935; font-size:1.1rem; margin-top:24px;">No hay reservas en tus clubes.</div>
        </c:if>
        <c:forEach var="booking" items="${bookings}">
        <a href="<c:url value='/admin/booking?bookingId=${booking.bookingId}' />" style="text-decoration:none; color:inherit;">
            <div class="card-lg booking-card" style="position:relative; margin-bottom:24px;">
                    <span><b>ID Reserva:</b> <c:out value="${booking.bookingId}"/></span><br/>
                    <span><b>Boliche:</b> <c:out value="${clubNames[booking.bookingId]}"/></span><br/>
                    <div class="booking-status-container">
                        <c:choose>
                            <c:when test="${fn:trim(fn:toUpperCase(booking.status)) == 'PENDIENTE'}">
                                <span class="booking-status booking-status-pending">Pendiente</span>
                            </c:when>
                            <c:when test="${fn:trim(fn:toUpperCase(booking.status)) == 'CONFIRMADA'}">
                                <span class="booking-status booking-status-confirmed">Confirmada</span>
                            </c:when>
                            <c:when test="${fn:trim(fn:toUpperCase(booking.status)) == 'CANCELADA'}">
                                <span class="booking-status booking-status-cancelled">Cancelada</span>
                            </c:when>
                            <c:otherwise>
                                <span class="booking-status">${booking.status}</span>
                            </c:otherwise>
                        </c:choose>
                    </div>
            </div>
        </a>
        </c:forEach>
    </div>
</body>
</html>
