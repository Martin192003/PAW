<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <title>Gestion de reserva</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookingUserHandler.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookings.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/clubActivity.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<c:set var="showRightButtons" value="true" />
<c:set var="showProfileButton" value="false" />
<c:set var="showAddClubButton" value="false" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>

    <div class="app-boliches">
        <h1 class="clubActivity-title">Reserva #<c:out value="${booking.bookingId}"/></h1>
        <div class="card-lg booking-details" dataId="${booking.bookingId}">
            <div class="booking-row">
                <span class="booking-label">Usuario:</span>
                <span class="booking-value"><c:out value="${booking.user.username}"/></span>
            </div>
            <div class="booking-row">
                <span class="booking-label">Mesa:</span>
                <span class="booking-value"><c:out value="${table.tableNumber}"/></span>
            </div>
            <div class="booking-row">
                <span class="booking-label">Boliche:</span>
                <span class="booking-value"><c:out value="${clubName}"/></span>
            </div>
            <div class="booking-row">
                <span class="booking-label">Inicio:</span>
                <span class="booking-value"><c:out value="${booking.bookingStart}"/></span>
            </div>
            <div class="booking-row">
                <span class="booking-label">Fin:</span>
                <span class="booking-value"><c:out value="${booking.bookingEnd}"/></span>
            </div>
            <div class="booking-row">
                <span class="booking-label">Estado:</span>
                <span class="booking-value">
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
                </span>
            </div>
            <div class="booking-row">
                <span class="booking-label">Total:</span>
                <span class="booking-value"><c:out value="${booking.totalAmount}"/></span>
            </div>
            <div class="booking-actions">
                <form method="post" action="<c:url value='/admin/bookings/${booking.bookingId}/cancel' />" style="display:inline;">
                    <button type="submit" class="btn btn-danger">
                        <i class="fa fa-trash"></i> Eliminar reserva
                    </button>
                </form>
                <form method="post" action="<c:url value='/admin/bookings/${booking.bookingId}/confirm' />" style="display:inline; margin-left:10px;">
                    <button type="submit" class="btn btn-success">
                        <i class="fa fa-check"></i> Confirmar reserva
                    </button>
                </form>
            </div>
        </div>
    </div>
</body>
</html>
