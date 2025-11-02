<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
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
<c:set var="showProfileButton" value="true" />
<c:set var="showAddClubButton" value="false" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<div class="detail-back">
    <button type="button" class="btn btn-success" onclick="window.location.href='<c:url value="/bookings"><c:param name="userId" value="${user.id}"/></c:url>';">
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
                    <dd><c:out value="${bookingStartLocal}"/></dd>
                </div>
                <div class="row">
                    <dt><spring:message code="booking.end" text="Fin"/></dt>
                    <dd><c:out value="${bookingEndLocal}"/></dd>
                </div>
                <div class="row">
                    <dt><spring:message code="placeholder.cbu" text="CBU"/></dt>
                    <dd><c:out value="${clubCbu}"/></dd>
                </div>
                <div class="row total">
                    <dt><spring:message code="booking.total" text="Total"/></dt>
                    <dd>
                        <c:choose>
                            <c:when test="${not empty table.price}">
                                <c:choose>
                                    <c:when test="${not empty table.discount and table.discount > 0}">
                                        <c:set var="priceToPay" value="${table.price * (1 - table.discount / 100)}" />
                                    </c:when>
                                    <c:otherwise>
                                        <c:set var="priceToPay" value="${table.price}" />
                                    </c:otherwise>
                                </c:choose>
                                $ <fmt:formatNumber value="${priceToPay}" minFractionDigits="0" maxFractionDigits="0"/>
                                <c:if test="${not empty table.discount and table.discount > 0}">
                                    <span class="discount-right muted">(<spring:message code="tables.discount"/>:<fmt:formatNumber value="${table.discount}" minFractionDigits="0" maxFractionDigits="0"/>%)</span>
                                </c:if>
                            </c:when>
                            <c:otherwise>
                                $ <c:out value="${booking.totalAmount}"/>
                            </c:otherwise>
                        </c:choose>
                    </dd>
                </div>
            </dl>
            <div class="booking-actions">
                <c:if test="${fn:trim(fn:toUpperCase(booking.status)) != 'CANCELADA'}">
                    <button id="userCancelBtn" type="button" class="btn btn-secondary"
                            onclick="openCancelBookingModal('${booking.bookingId}')">
                        <i class="fa fa-trash"></i>
                        <spring:message code="booking.action.cancel" text="Cancelar reserva"/>
                    </button>
                </c:if>
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
                        <c:if test="${fn:trim(fn:toUpperCase(booking.status)) != 'CANCELADA'}">
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
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <p class="muted"><spring:message code="receipt.missing" text="No hay comprobante cargado."/></p>
                        <c:if test="${fn:trim(fn:toUpperCase(booking.status)) != 'CANCELADA'}">
                            <form:form method="post" modelAttribute="receiptForm" enctype="multipart/form-data"
                                       action="${pageContext.request.contextPath}/bookings/${booking.bookingId}/receipt"
                                       class="upload-form">
                                <div class="form-group">
                                    <form:input type="file" path="file"/>
                                    <p cssClass="error-message"><form:errors path="file"/></p>
                                </div>
                                <button type="submit" class="btn btn-success btn-sm">
                                    <spring:message code="receipt.upload" text="Subir comprobante"/>
                                </button>
                            </form:form>
                        </c:if>
                    </c:otherwise>
                </c:choose>
            </article>
        </aside>
    </section>
</div>
<div id="cancelBookingModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeCancelBookingModal()">&times;</span>
        <h3 style="margin-top:8px;"><spring:message code="confirm.cancelBooking"/></h3>
        <form id="cancelBookingForm" method="post" action="<c:url value='/bookings/${booking.bookingId}/cancel' />">
            <input type="hidden" name="bookingId" id="cancelBookingId" value=""/>
            <div class="modal-actions" style="margin-top:18px; display:flex; justify-content:space-between;">
                <button type="button" class="btn btn-secondary" onclick="closeCancelBookingModal()">
                    <spring:message code="button.cancel"/>
                </button>
                <button type="submit" class="btn btn-danger">
                    <spring:message code="booking.action.confirm"/>
                </button>
            </div>
        </form>
    </div>
</div>
<script>
    window.openCancelBookingModal = function(bookingId) {
        const input = document.getElementById('cancelBookingId');
        const modal = document.getElementById('cancelBookingModal');
        if (!modal) return;
        if (input) input.value = bookingId;
        modal.style.display = 'flex';
        setTimeout(() => {
            modal.style.opacity = '1';
            modal.style.visibility = 'visible';
        }, 10);
    };

    window.closeCancelBookingModal = function() {
        const input = document.getElementById('cancelBookingId');
        const modal = document.getElementById('cancelBookingModal');
        if (!modal) return;
        if (input) input.value = '';
        modal.style.display = 'none';
        modal.style.opacity = '0';
        modal.style.visibility = 'hidden';
    };

    document.addEventListener('DOMContentLoaded', function () {
        const modal = document.getElementById('cancelBookingModal');
        const btn = document.getElementById('userCancelBtn');

        if (btn) {
            btn.addEventListener('click', function (ev) {
                ev.preventDefault();
                openCancelBookingModal('${booking.bookingId}');
            });
        }

        window.addEventListener('click', function (e) {
            const modalNow = document.getElementById('cancelBookingModal');
            if (modalNow && e.target === modalNow) {
                closeCancelBookingModal();
            }
        });

        const closeX = document.querySelector('#cancelBookingModal .modal-close');
        if (closeX) {
            closeX.addEventListener('click', closeCancelBookingModal);
        }
    });
</script>
<c:if test="${not empty bookingAction}">
    <script>
        document.addEventListener("DOMContentLoaded", function() {
            var toast = document.getElementById("bookingActionToast");
            var msg = "";
            <c:choose>
            <c:when test="${bookingAction eq 'cancelled'}">
            msg = "<spring:message code='booking.cancelled.toast' text='Booking cancelled successfully.'/>";
            </c:when>
            <c:when test="${bookingAction eq 'confirmed'}">
            msg = "<spring:message code='booking.confirmed.toast' text='Booking confirmed successfully.'/>";
            </c:when>
            </c:choose>
            if (toast) {
                document.getElementById("bookingActionToastMsg").innerHTML = msg;
                toast.style.display = "flex";
                toast.classList.add("show");
                setTimeout(() => { toast.classList.remove("show"); toast.style.display = "none"; }, 3000);
            }
        });
    </script>
</c:if>
<div id="bookingActionToast" class="toast-popup" style="display:none;">
    <i class="fa-solid fa-circle-check" style="color:#4caf50; font-size:24px;"></i>
    <span id="bookingActionToastMsg"></span>
</div>

<div id="reservationToast" class="toast-popup" style="display:none;">
    <i class="fa-solid fa-circle-check" style="color:#4caf50; font-size:24px;"></i>
    <span><spring:message code="reservation.sent"/></span>
</div>

<c:if test="${not empty reservationSent}">
    <script>
        document.addEventListener("DOMContentLoaded", function() {
            const toast = document.getElementById("reservationToast");
            toast.style.display = "flex";
            toast.classList.add("show");
            setTimeout(() => { toast.classList.remove("show"); toast.style.display = "none"; }, 3000);
        });
    </script>
</c:if>
</body>
</html>
