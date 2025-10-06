<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <title><spring:message code="handler.reservation"/></title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookingUserHandler.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

<c:set var="showRightButtons" value="true" />
<c:set var="showProfileButton" value="false" />
<c:set var="showAddClubButton" value="false" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<div class="detail-back">
    <button type="button" class="btn btn-success" onclick="window.history.back();">
        <i class="fa fa-arrow-left"></i>
    </button>
</div>
<div class="app-boliches booking-page">
    <h1 class="clubActivity-title">
        <span><spring:message code="booking.title.prefix"/> <br>#<c:out value="${booking.bookingId}"/></span>
        <c:choose>
            <c:when test="${fn:trim(fn:toUpperCase(booking.status)) == 'PENDIENTE'}">
                <span class="status-badge is-pending"><spring:message code="booking.status.pending" text="Pendiente"/></span>
            </c:when>
            <c:when test="${fn:trim(fn:toUpperCase(booking.status)) == 'CONFIRMADA'}">
                <span class="status-badge is-confirmed"><spring:message code="booking.status.confirmed" text="Confirmada"/></span>
            </c:when>
            <c:when test="${fn:trim(fn:toUpperCase(booking.status)) == 'CANCELADA'}">
                <span class="status-badge is-cancelled"><spring:message code="booking.status.cancelled" text="Cancelada"/></span>
            </c:when>
            <c:otherwise>
                <span class="status-badge"><c:out value="${booking.status}"/></span>
            </c:otherwise>
        </c:choose>
    </h1>

    <!-- GRID PRINCIPAL -->
    <section class="booking-grid">
        <article class="card-lg booking-card">
            <h2 class="panel-title"><spring:message code="booking.details.title" text="Detalles de la reserva"/></h2>
            <dl class="details">
                <div class="row">
                    <dt><spring:message code="user.username" text="Usuario"/></dt>
                    <dd><c:out value="${user.username}"/></dd>
                </div>
                <div class="row">
                    <dt>Email</dt>
                    <dd><c:out value="${user.email}"/></dd>
                </div>
                <div class="row">
                    <dt><spring:message code="user.surname" text="Apellido"/></dt>
                    <dd><c:out value="${user.surname}"/></dd>
                </div>
                <div class="row">
                    <dt><spring:message code="table.number" text="Mesa"/></dt>
                    <dd><c:out value="${table.tableNumber}"/></dd>
                </div>
                <div class="row">
                    <dt><spring:message code="club.name" text="Boliche"/></dt>
                    <dd class="truncate"><c:out value="${clubName}"/></dd>
                </div>
                <div class="row">
                    <dt><spring:message code="booking.start" text="Inicio"/></dt>
                    <dd><c:out value="${booking.bookingStart}"/></dd>
                </div>
                <div class="row">
                    <dt><spring:message code="booking.end" text="Fin"/></dt>
                    <dd><c:out value="${booking.bookingEnd}"/></dd>
                </div>
                <div class="row total">
                    <dt><spring:message code="booking.total" text="Total"/></dt>
                    <dd>$ <c:out value="${booking.totalAmount}"/></dd>
                </div>
            </dl>
            <div class="booking-actions">
                <form method="post" action="<c:url value='/bookings/${booking.bookingId}/cancel' />">
                    <button type="submit" class="btn btn-secondary">
                        <i class="fa fa-trash"></i>
                        <spring:message code="booking.action.cancel" text="Cancelar reserva"/>
                    </button>
                </form>
            </div>
        </article>

        <aside class="right-col">
            <article class="card-lg booking-card">
                <h2 class="panel-title"><spring:message code="booking.receipt.title" text="Comprobante"/></h2>

                <c:choose>
                    <c:when test="${hasReceipt}">
                        <div class="receipt-thumb">
                            <img src="<c:url value='/bookings/${booking.bookingId}/receipt'/>" alt="Receipt"/>
                        </div>
                        <div class="receipt-actions">
                            <form method="post" action="<c:url value='/bookings/${booking.bookingId}/receipt/delete'/>">
                                <button type="submit" class="btn btn-secondary">
                                    <spring:message code="receipt.delete" text="Eliminar"/>
                                </button>
                            </form>
                            <form>
                                <button type="button" class="btn btn-success" onclick="window.open('<c:url value="/bookings/${booking.bookingId}/receipt"/>', '_blank')">
                                    <spring:message code="receipt.view.tab" text="Ver en pestaña"/>
                                </button>
                            </form>
                        </div>
                    </c:when>

                    <c:otherwise>
                        <p class="muted"><spring:message code="receipt.missing" text="No hay comprobante cargado."/></p>
                    </c:otherwise>
                </c:choose>

                <c:if test="${!hasReceipt}">
                    <form:form method="post" modelAttribute="receiptForm" enctype="multipart/form-data" action="${pageContext.request.contextPath}/bookings/${booking.bookingId}/receipt" class="upload-form">
                        <div class="form-group">
                            <form:input type="file" path="file"/>
                            <p cssClass="error-message"><form:errors path="file"/></p>
                        </div>
                        <button type="submit" class="btn btn-success btn-sm" >
                                <spring:message code="receipt.upload" text="Subir comprobante"/>
                        </button>
                    </form:form>
                </c:if>
            </article>
        </aside>
    </section>
</div>


</body>
</html>
