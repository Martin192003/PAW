<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title><c:out value="${club.clubName}"/> - <spring:message code="title.details"/></title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/details.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<c:url var="clubUrl" value="/clubs/${club.clubId}"/>
<c:set var="showRightButtons" value="true" />
<c:set var="showProfileButton" value="true" />
<c:set var="showAddClubButton" value="false" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<div class="detail-page">
    <div class="detail-back">
        <button type="button" class="btn btn-success"
                onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
    </div>
    <div class="detail-header" style="display: flex; flex-direction: row; justify-content: space-between; align-items: center; width: 100%;">
        <h1 class="detail-title"><c:out value="${club.clubName}"/></h1>
        <div class="detail-actions" style="display:flex; flex-direction:column; gap:8px; align-items:flex-end;">
            <c:if test="${isOwner}">
                <button type="button" class="btn btn-success" onclick="window.location.href='<c:url value='/clubs/handle'><c:param name='clubId' value='${club.clubId}'/></c:url>'">
                    <i class="fa-solid fa-pen"></i>
                    <spring:message code="button.edit" text="Editar"/>
                </button>
            </c:if>
            <button type="button" class="btn btn-success" onclick="window.location.href='<c:url value="/contact"><c:param name="id" value="${club.clubId}"/></c:url>'">
                <i class="fa-solid fa-envelope"></i>
                <spring:message code="button.contact"/>
            </button>
        </div>
    </div>

    <div class="detail-content">
        <c:if test="${not empty coverPhotoUrl}">
            <img src="<c:url value='${coverPhotoUrl}' />"
                 alt="<c:out value='${club.clubName}'/>" class="detail-image">
        </c:if>

        <div class="detail-info">
            <div class="detail-details">
                <span><i class="fa-solid fa-location-dot"></i><c:out value="${locationText}"/></span><br>
                <span><i class="fa-solid fa-music"></i><spring:message code="genre.${club.musicGenre}"/></span><br>
                <i class="fa-solid fa-dollar-sign"></i> <spring:message code="price.${club.priceRange}"/><br>
                <div>
                    <i class="fa-solid fa-star" style="color:#FFD700;"></i>
                    <c:choose>
                        <c:when test="${club.rating != null}">
                            <span>
                              <fmt:formatNumber value="${club.rating}" minFractionDigits="1" maxFractionDigits="1"/>
                              (<c:out value="${club.quantityRatings}"/> <spring:message code="rating.reviews"/>)
                            </span>
                        </c:when>
                        <c:otherwise><span><spring:message code="error.norated"/></span></c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <div class="detail-description">
            <p><c:out value="${club.description}" /></p>
        </div>

        <!-- Fila con 2 dropdowns independientes -->
        <div class="bb-row">
            <!-- Dropdown: Carta -->
            <div class="bb-dropdown" id="dd-menu">
                <button type="button" class="bb-btn">
                    <span><spring:message code="menu.menu"/></span>
                    <i class="fa-solid fa-chevron-down"></i>
                </button>
                <div class="bb-panel">
                    <c:choose>
                        <c:when test="${empty productsByCategory}">
                            <p class="bb-empty"><spring:message code="error.noproductsavailable"/></p>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="entry" items="${productsByCategory}">
                                <div class="drink-category">
                                    <c:catch var="messageError">
                                        <spring:message code="productType.${entry.key}" var="catName" htmlEscape="true"/>
                                    </c:catch>
                                    <h3>
                                        <c:choose>
                                            <c:when test="${empty messageError and not empty catName and not fn:startsWith(catName, 'productType.')}">
                                                <c:out value="${catName}"/>
                                            </c:when>
                                            <c:otherwise>
                                                <c:out value="${entry.key}"/>
                                            </c:otherwise>
                                        </c:choose>
                                    </h3>
                                    <ul>
                                        <c:forEach var="p" items="${entry.value}">
                                            <li>
                                                <span class="drink-name"><c:out value="${p.productName}"/></span>
                                                <span class="drink-price">
                                                    $<fmt:formatNumber value="${p.basePrice}" type="number" minFractionDigits="0" maxFractionDigits="0"/>
                                                </span>
                                            </li>
                                        </c:forEach>
                                    </ul>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <!-- Dropdown: Mesas -->
            <div class="bb-dropdown" id="dd-tables">
                <button type="button" class="bb-btn">
                    <span><spring:message code="button.tables"/></span>
                    <i class="fa-solid fa-chevron-down"></i>
                </button>

                <div class="bb-panel">
                    <c:url var="reserveUrl" value="/clubs/${club.clubId}/reserve"/>
                    <c:if test="${not empty error}">
                        <div class="bb-error" style="background:#5a1a1a;color:#fff;padding:10px;border-radius:6px;margin-bottom:12px;">
                            <spring:message code="invalid.date"/>
                        </div>
                    </c:if>
                    <form id="reserveForm" method="post" action="${reserveUrl}">
                        <div class="bb-actions" style="display:flex; gap:12px; align-items:flex-end; flex-wrap:wrap;">
                            <div class="bb-date" style="display:flex; flex-direction:column;">
                                <label for="bookingDate" class="bb-label" style="margin-bottom:6px;">
                                    <spring:message code="title.reservation"/>
                                </label>
                                <input id="bookingDate"
                                       name="date"
                                       type="date"
                                       class="input"
                                       value="${selectedDate != null ? selectedDate : ''}" />
                                <p class="bb-help" id="dateHelp" style="margin-top:6px; max-width:380px;">
                                    <spring:message code="tables.accept"/> <strong><spring:message code="tables.days"/></strong>.
                                    <spring:message code="tables.time"/> <strong>23:59 <spring:message code="tables.to"/> 06:00</strong>
                                    (<spring:message code="tables.nextday"/>).
                                </p>
                            </div>
                        </div>

                        <c:choose>
                            <c:when test="${empty availableTables}">
                                <p class="bb-empty"><spring:message code="tables.empty"/></p>
                            </c:when>
                            <c:otherwise>
                                <ul class="table-list">
                                    <c:forEach var="t" items="${availableTables}">
                                        <li class="table-item">
                                            <label class="table-row">
                                                <input type="radio" name="tableId" value="${t.tableId}" />
                                                <span class="table-row-text">
                                                    <strong><spring:message code="tables.table"/> #${t.tableNumber}</strong>
                                                    · <spring:message code="tables.capacity"/>: ${t.capacity}
                                                </span>
                                                <span class="table-row-price">
                                                    <c:choose>
                                                        <c:when test="${t.discount != null && t.discount > 0}">
                                                            <c:set var="discountedPrice" value="${t.price * (1 - t.discount / 100)}" />
                                                            $<fmt:formatNumber value="${discountedPrice}" minFractionDigits="0" maxFractionDigits="0"/>
                                                            <small>(<spring:message code="tables.discount"/>:
                                                                <fmt:formatNumber value="${t.discount}" minFractionDigits="0" maxFractionDigits="0"/>%
                                                            )</small>
                                                        </c:when>
                                                        <c:otherwise>
                                                            $<fmt:formatNumber value="${t.price}" minFractionDigits="0" maxFractionDigits="0"/>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </span>
                                            </label>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </c:otherwise>
                        </c:choose>

                        <button id="reserveBtn" type="submit" class="btn btn-success"
                                <c:if test="${empty availableTables}">disabled</c:if>
                                style="height:40px; align-self:end;">
                            <i class="fa-solid fa-calendar-check"></i> <spring:message code="button.reservation"/>
                        </button>
                    </form>
                </div>
            </div>
        </div>

        <div id="reservationModal" style="display:none; position:fixed; top:0; left:0; width:100vw; height:100vh; background:rgba(0,0,0,0.5); z-index:9999; justify-content:center; align-items:center;">
            <div style="background:#222; color:#fff; padding:32px; border-radius:12px; box-shadow:0 4px 24px rgba(0,0,0,0.6); width:400px; text-align:center;">
                <div style="font-size:22px; margin-bottom:18px;">
                    <spring:message code="reservation.sent"/>
                </div>
                <button class="btn btn-success" onclick="cerrarModal()"><spring:message code="button.back"/></button>
            </div>
        </div>

        <c:if test="${not empty reservationSent}">
            <script>
                document.addEventListener("DOMContentLoaded", function() {
                    document.getElementById("reservationModal").style.display = "flex";
                });
            </script>
        </c:if>

        <script>
            document.addEventListener('DOMContentLoaded', () => {
                function breakEveryN(str, n) {
                    if (!str) return '';
                    const s = String(str).trim();
                    if (s.length <= n) return s;
                    const parts = [];
                    for (let i = 0; i < s.length; i += n) parts.push(s.slice(i, i + n));
                    return parts.join('\n');
                }

                // Aplica al título (cada 30)
                const titleEl = document.querySelector('.detail-title');
                if (titleEl && !titleEl.dataset.broken) {
                    titleEl.innerText = breakEveryN(titleEl.textContent, 30);
                    titleEl.dataset.broken = '1';
                }

                // Aplica a la descripción (cada 100)
                const descEl = document.querySelector('.detail-description p');
                if (descEl && !descEl.dataset.broken) {
                    descEl.innerText = breakEveryN(descEl.textContent, 100);
                    descEl.dataset.broken = '1';
                }

                const dropdowns = Array.from(document.querySelectorAll('.bb-dropdown'));
                function closeAll() {
                    dropdowns.forEach(dd => {
                        dd.querySelector('.bb-btn')?.classList.remove('active');
                        dd.querySelector('.bb-panel')?.classList.remove('show');
                    });
                }
                dropdowns.forEach(dd => {
                    const btn = dd.querySelector('.bb-btn');
                    const panel = dd.querySelector('.bb-panel');
                    btn?.addEventListener('click', (e) => {
                        const isOpen = panel?.classList.contains('show');
                        closeAll();
                        if (!isOpen) { btn.classList.add('active'); panel.classList.add('show'); }
                        e.stopPropagation();
                    });
                });
                window.addEventListener('click', (e) => {
                    if (!e.target.closest('.bb-dropdown')) closeAll();
                });

                const reserveBtn   = document.getElementById('reserveBtn');
                const form         = document.getElementById('reserveForm');
                const bookingDate  = document.getElementById('bookingDate');

                const today = new Date();
                const yyyy  = today.getFullYear();
                const mm    = String(today.getMonth()+1).padStart(2,'0');
                const dd    = String(today.getDate()).padStart(2,'0');
                if (bookingDate && !bookingDate.getAttribute('min')) {
                    bookingDate.setAttribute('min', `${yyyy}-${mm}-${dd}`);
                }

                function updateReserveButtonState(){
                    if (!reserveBtn || !form) return;
                    const selected = form.querySelector('input[type="radio"][name="tableId"]:checked');
                    reserveBtn.disabled = !selected;
                }

                (function autoPickFirstIfAny(){
                    if (!form) return;
                    const radios = form.querySelectorAll('input[type="radio"][name="tableId"]');
                    if (radios.length > 0) {
                        const anyChecked = Array.from(radios).some(r => r.checked);
                        if (!anyChecked) radios[0].checked = true;
                    }
                })();

                updateReserveButtonState();

                if (bookingDate) {
                    bookingDate.addEventListener('change', () => {
                        if (bookingDate.value) {
                            window.location.href = "${clubUrl}?date=" + bookingDate.value + "#dd-tables";
                        }
                    });
                }

                if (form && reserveBtn){
                    form.addEventListener('submit', (e) => {
                        if (form.dataset.submitting === '1') { e.preventDefault(); return; }

                        const selected = form.querySelector('input[type="radio"][name="tableId"]:checked');
                        if (!selected) {
                            e.preventDefault();
                            updateReserveButtonState();
                            return;
                        }

                        let hidTable = form.querySelector('input[type="hidden"][name="tableId"]');
                        if (!hidTable) {
                            hidTable = document.createElement('input');
                            hidTable.type = 'hidden';
                            hidTable.name = 'tableId';
                            form.appendChild(hidTable);
                        }
                        hidTable.value = selected.value;

                        let hidDate = form.querySelector('input[type="hidden"][name="date"]');
                        if (!hidDate) {
                            hidDate = document.createElement('input');
                            hidDate.type = 'hidden';
                            hidDate.name = 'date';
                            form.appendChild(hidDate);
                        }
                        if (!bookingDate || !bookingDate.value) {
                            e.preventDefault();
                            return;
                        }
                        hidDate.value = bookingDate.value;

                        form.dataset.submitting = '1';
                        reserveBtn.disabled = true;
                        reserveBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> <spring:message code="button.reservation"/>';

                        form.querySelectorAll('input, button, select').forEach(el => {
                            const keepEnabled = (el === hidTable) || (el === hidDate) || (el === selected);
                            if (!keepEnabled) el.disabled = true;
                        });
                    });
                }

                if (location.hash === "#dd-tables") {
                    const ddEl = document.getElementById('dd-tables');
                    if (ddEl) {
                        ddEl.querySelector('.bb-btn')?.classList.add('active');
                        ddEl.querySelector('.bb-panel')?.classList.add('show');
                    }
                }
            });

            function cerrarModal() {
                window.location.href = "${clubUrl}";
            }
        </script>

    </div>
</div>
</body>
</html>
