<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
<head>
    <title><spring:message code="booking.page.title" text="Gestión de reserva"/></title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookingUserHandler.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
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
    <c:if test="${not empty errorKey}">
        <div class="alert alert-danger">
            <spring:message code="${errorKey}" text="${errorKey}"/>
        </div>
    </c:if>

    <c:if test="${not empty success}">
        <c:choose>
            <c:when test="${success == 'booking.cancelled'}">
                <div class="alert alert-danger">
                    <spring:message code="${success}" text="${success}"/>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alert alert-success">
                    <spring:message code="${success}" text="${success}"/>
                </div>
            </c:otherwise>
        </c:choose>
    </c:if>

    <c:if test="${not empty info}">
        <div class="alert alert-info">
            <spring:message code="${info}" text="${info}"/>
        </div>
    </c:if>
    <!-- Título + badge de estado -->
    <h1 class="clubActivity-title">
        <span><spring:message code="booking.title.prefix" text="Reserva"/> <br>#<c:out value="${booking.bookingId}"/></span>
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

    <section class="booking-grid">
        <!-- Detalles -->
        <article class="card-lg booking-card">
            <h2 class="panel-title"><spring:message code="booking.details.title" text="Detalles de la reserva"/></h2>

            <dl class="details">
                <div class="row">
                    <dt><spring:message code="user.username" text="Usuario"/></dt>
                    <dd><c:out value="${user.username}"/></dd>
                </div>
                <div class="row">
                    <dt><spring:message code="user.email" text="Email"/></dt>
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
                    <dt><spring:message code="booking.cbu" text="CBU"/></dt>
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
                                <span>$ <fmt:formatNumber value="${priceToPay}" minFractionDigits="0" maxFractionDigits="0"/></span>
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
            <c:set var="statusNormalized" value="${fn:trim(fn:toUpperCase(booking.status))}" />
            <c:set var="isConfirmed" value="${statusNormalized == 'CONFIRMADA'}" />
            <c:set var="isCancelled" value="${statusNormalized == 'CANCELADA'}"/>
            <c:set var="isPending" value="${statusNormalized == 'PENDIENTE'}"/>
            <c:if test="${not isCancelled}">
                <div class="booking-actions">
                    <form id="adminCancelForm" class="confirm-cancel" method="post" action="<c:url value='/admin/bookings/${booking.bookingId}/cancel' />">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <button id="adminCancelBtn" type="submit" class="btn btn-secondary">
                            <i class="fa fa-trash"></i> <spring:message code="booking.action.cancel" text="Cancelar"/>
                        </button>
                    </form>
                    <form id="adminConfirmForm" method="post" action="<c:url value='/admin/bookings/${booking.bookingId}/confirm' />">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <c:choose>
                            <c:when test="${isConfirmed}">
                                <button type="button" class="btn btn-success" disabled title="<spring:message code='booking.action.alreadyConfirmed' text='Reserva ya confirmada'/>">
                                    <i class="fa fa-check"></i> <spring:message code="booking.action.confirm" text="Confirmar"/>
                                </button>
                            </c:when>
                            <c:otherwise>
                                <button id="adminConfirmBtn" type="submit" class="btn btn-success">
                                    <i class="fa fa-check"></i> <spring:message code="booking.action.confirm" text="Confirmar"/>
                                </button>
                            </c:otherwise>
                        </c:choose>
                    </form>
                </div>
            </c:if>
        </article>

        <div class="right-col">
            <article class="card-lg booking-card">
                <h2 class="panel-title"><spring:message code="booking.receipt.title" text="Comprobante"/></h2>

                <c:choose>
                    <c:when test="${hasReceipt}">
                        <div class="receipt-thumb">
                            <img src="<c:url value='/bookings/${booking.bookingId}/receipt'/>"
                                 alt="<spring:message code='receipt.alt' text='Comprobante'/>"/>
                        </div>
                        <c:if test="${isPending or isConfirmed}">                            <div class="receipt-actions">
                                <form style="justify-content: center; display: flex; align-items: center">
                                    <button type="button" class="btn btn-success" onclick="window.open('<c:url value='/bookings/${booking.bookingId}/receipt'/>', '_blank')">
                                        <spring:message code="receipt.view.tab" text="Ver en pestaña"/>
                                    </button>
                                </form>
                            </div>
                        </c:if>
                    </c:when>
                    <c:otherwise>
                        <p class="muted"><spring:message code="receipt.missing" text="No hay comprobante cargado."/></p>
                    </c:otherwise>
                </c:choose>
            </article>
            </div>
    </section>
</div>
<div id="cancelConfirmModal" class="modal" style="display:none; align-items:center; justify-content:center;">
    <div class="modal-content">
        <span class="modal-close" onclick="closeCancelConfirmModal()">&times;</span>
        <h3 style="margin-top:8px;"><spring:message code="confirm.cancelBooking"/></h3>
        <div class="modal-actions" style="margin-top:18px; display:flex; justify-content:space-between;">
            <button type="button" class="btn btn-secondary" onclick="closeCancelConfirmModal()">
                <spring:message code="button.cancel" text="Cancelar"/>
            </button>
            <button id="confirmCancelBtn" type="button" class="btn btn-danger">
                <spring:message code="booking.action.confirm"/>
            </button>
        </div>
    </div>
</div>
<script>
    (function(){
        let pendingCancelForm = null;

        function openCancelConfirmModal(form){
            pendingCancelForm = form;
            const modal = document.getElementById('cancelConfirmModal');
            if(modal) {
                modal.style.display = 'flex';
                setTimeout(() => {
                    modal.style.opacity = '1';
                    modal.style.visibility = 'visible';
                }, 10);
            }
        }

        function closeCancelConfirmModal(){
            const modal = document.getElementById('cancelConfirmModal');
            if(modal) {
                modal.style.display = 'none';
                modal.style.opacity = '0';
                modal.style.visibility = 'hidden';
            }
            pendingCancelForm = null;
        }

        document.addEventListener('DOMContentLoaded', function(){
            // Interceptar submit de forms de cancelación
            document.querySelectorAll('form.confirm-cancel').forEach(f => {
                f.addEventListener('submit', function(e){
                    // si ya se confirmó, permitir submit real
                    if(f.dataset.confirmed === '1') return;
                    e.preventDefault();
                    openCancelConfirmModal(f);
                });
            });

            // Botón de confirmación dentro del modal
            const confirmBtn = document.getElementById('confirmCancelBtn');
            if(confirmBtn){
                confirmBtn.addEventListener('click', function(){
                    if(!pendingCancelForm) return;

                    // marcar confirmado para que se haga submit real
                    pendingCancelForm.dataset.confirmed = '1';

                    // activar spinner solo ahora, antes de enviar
                    const btn = pendingCancelForm.querySelector('button[type="submit"]');
                    if(btn){
                        btn.disabled = true;
                        try {
                            btn.dataset.orig = btn.innerHTML;
                            btn.innerHTML = '<i class="fa fa-spinner fa-spin"></i> ' + (btn.textContent || '');
                        } catch (err){ }
                    }

                    pendingCancelForm.submit();
                });
            }

            // Cerrar modal al click fuera del contenido
            window.addEventListener('click', function(e){
                const modal = document.getElementById('cancelConfirmModal');
                if(modal && e.target === modal) closeCancelConfirmModal();
            });

            // Cerrar modal con la X
            const closeX = document.querySelector('#cancelConfirmModal .modal-close');
            if(closeX) closeX.addEventListener('click', closeCancelConfirmModal);

            window.closeCancelConfirmModal = closeCancelConfirmModal;
        });
    })();
</script>
</body>
</html>

