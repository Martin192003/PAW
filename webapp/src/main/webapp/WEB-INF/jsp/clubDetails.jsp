<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title><c:out value="${club.clubName}"/> - <spring:message code="title.details"/></title>
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
        <button type="button" class="btn btn-success"
                onclick="window.location.href='<c:url value="/contact"><c:param name="id" value="${club.clubId}"/></c:url>'">
            <i class="fa-solid fa-envelope"></i> <spring:message code="button.contact"/>
        </button>
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
                                    <h3><c:out value="${entry.key}"/></h3>
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
                    <form id="reserveForm" method="post" action="${reserveUrl}">

                        <!-- Fila: Fecha + Botón -->
                        <div class="bb-actions" style="display:flex; gap:12px; align-items:flex-end; flex-wrap:wrap;">
                            <div class="bb-date" style="display:flex; flex-direction:column;">
                                <label for="bookingDate" class="bb-label" style="margin-bottom:6px;"><spring:message code="title.reservation"/></label>
                                <input id="bookingDate" name="date" type="date" class="input"
                                       value="${selectedDate != null ? selectedDate : ''}"
                                       min="<fmt:formatDate value='${now}' pattern='yyyy-MM-dd'/>" />
                                <p class="bb-help" id="dateHelp" style="margin-top:6px; max-width:380px;">
                                    <spring:message code="tables.accept"/> <strong><spring:message code="tables.days"/></strong>. <spring:message code="tables.time"/>
                                    <strong>23:59 <spring:message code="tables.to"/> 06:00</strong> (<spring:message code="tables.nextday"/>).
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
                                                    <strong><spring:message code="tables.table"/> #${t.tableNumber}</strong> · <spring:message code="tables.capacity"/>: ${t.capacity}
                                                </span>
                                                <span class="table-row-price">
                                                    $<fmt:formatNumber value="${t.price}" minFractionDigits="0" maxFractionDigits="0"/>
                                                    <c:if test="${t.discount != null && t.discount > 0}">
                                                        <small>(<spring:message code="tables.discount"/>: $<fmt:formatNumber value="${t.discount}" minFractionDigits="0" maxFractionDigits="0"/>)</small>
                                                    </c:if>
                                                </span>
                                            </label>
                                        </li>
                                    </c:forEach>
                                </ul>

                            </c:otherwise>
                        </c:choose>
                        <button id="reserveBtn" type="submit" class="btn btn-success" disabled
                                style="height:40px; align-self:end;">
                            <i class="fa-solid fa-calendar-check"></i> <spring:message code="button.reservation"/>
                        </button>
                    </form>
                </div>
            </div>


        </div>

        <spring:message code="tables.selectdate" var="msgSelectDate"/>
        <spring:message code="tables.invaliddate" var="msgInvalidDate"/>
        <spring:message code="tables.fixedrange" var="msgFixedRange"/>
        <spring:message code="tables.pastdate" var="msgPastDate"/>

        <script>
            document.addEventListener('DOMContentLoaded', () => {
                function breakEveryN(str, n) {
                    if (!str) return '';
                    const s = String(str).trim();
                    if (s.length <= n) return s;
                    const parts = [];
                    for (let i = 0; i < s.length; i += n) {
                        parts.push(s.slice(i, i + n));
                    }
                    // Usamos <br> para forzar nueva línea, no solo wrap suave
                    return parts.join('\n');
                }

                // Aplica al título (cada 40)
                const titleEl = document.querySelector('.detail-title');
                if (titleEl && !titleEl.dataset.broken) {
                    titleEl.innerText = breakEveryN(titleEl.textContent, 30);
                    titleEl.dataset.broken = '1';
                }

                // Aplica a la descripción (cada 150)
                const descEl = document.querySelector('.detail-description p');
                if (descEl && !descEl.dataset.broken) {
                    descEl.innerText = breakEveryN(descEl.textContent, 100);
                    descEl.dataset.broken = '1';
                }

                // Dropdowns
                const dropdowns = Array.from(document.querySelectorAll('.bb-dropdown'));
                function closeAll() {
                    dropdowns.forEach(dd => {
                        dd.querySelector('.bb-btn').classList.remove('active');
                        dd.querySelector('.bb-panel').classList.remove('show');
                    });
                }
                dropdowns.forEach(dd => {
                    const btn = dd.querySelector('.bb-btn');
                    const panel = dd.querySelector('.bb-panel');
                    btn.addEventListener('click', (e) => {
                        const isOpen = panel.classList.contains('show');
                        closeAll();
                        if (!isOpen) { btn.classList.add('active'); panel.classList.add('show'); }
                        e.stopPropagation();
                    });
                });
                window.addEventListener('click', (e) => {
                    if (!e.target.closest('.bb-dropdown')) closeAll();
                });

                // Date rules (Thu=4, Fri=5, Sat=6)
                const ALLOWED_DAYS = [4,5,6];
                const reserveBtn = document.getElementById('reserveBtn');
                const form = document.getElementById('reserveForm');
                const bookingDate = document.getElementById('bookingDate');
                const dateHelp = document.getElementById('dateHelp');

                // Ensure min=today (browser UI) but still validate manually
                const today = new Date();
                const yyyy = today.getFullYear();
                const mm = String(today.getMonth()+1).padStart(2,'0');
                const dd = String(today.getDate()).padStart(2,'0');
                if (bookingDate && !bookingDate.getAttribute('min')) {
                    bookingDate.setAttribute('min', `${yyyy}-${mm}-${dd}`);
                }

                function isAllowedDay(isoDate){
                    if(!isoDate) return false;
                    const d = new Date(isoDate + 'T12:00:00'); // avoid TZ issues
                    const dow = d.getDay(); // 0..6
                    return ALLOWED_DAYS.includes(dow);
                }
                function isPastDate(isoDate){
                    if(!isoDate) return false;
                    const d = new Date(isoDate + 'T12:00:00');
                    const now = new Date();
                    now.setHours(12,0,0,0);
                    return d < now;
                }

                function updateReserveButtonState(){
                    const hasTable = !!(form && form.querySelector('input[name="tableId"]:checked'));
                    const past = bookingDate && bookingDate.value && isPastDate(bookingDate.value);
                    const allowed = bookingDate && bookingDate.value && isAllowedDay(bookingDate.value);
                    const hasValidDate = bookingDate && bookingDate.value && !past && allowed;

                    reserveBtn.disabled = !(hasTable && hasValidDate);

                    if (!bookingDate.value) {
                        dateHelp.innerHTML = '${msgSelectDate}';
                        bookingDate.classList.remove('invalid');
                    } else if (past) {
                        dateHelp.innerHTML = '${msgPastDate}';
                        bookingDate.classList.add('invalid');
                    } else if (!allowed) {
                        dateHelp.innerHTML = '${msgInvalidDate}';
                        bookingDate.classList.add('invalid');
                    } else {
                        dateHelp.innerHTML = '${msgFixedRange}';
                        bookingDate.classList.remove('invalid');
                    }
                }

                updateReserveButtonState();

                bookingDate.addEventListener('change', () => {
                    updateReserveButtonState();
                    // Only reload tables when the chosen date is valid; otherwise keep the user's date and show error
                    const past = bookingDate && bookingDate.value && isPastDate(bookingDate.value);
                    const allowed = bookingDate && bookingDate.value && isAllowedDay(bookingDate.value);
                    if (bookingDate.value && !past && allowed) {
                        window.location.href = "${clubUrl}?date=" + bookingDate.value + "#dd-tables";
                    }
                });

                if (form && reserveBtn){
                    form.addEventListener('change', (e) => {
                        if (e.target.name === 'tableId') updateReserveButtonState();
                    });
                    form.addEventListener('click', (e) => {
                        const li = e.target.closest('li.table-item');
                        if (!li) return;
                        const radio = li.querySelector('input[type=radio]');
                        if (radio){ radio.checked = true; updateReserveButtonState(); }
                    });
                    // Final guard: keep user's date and block submit if invalid
                    form.addEventListener('submit', (e) => {
                        const value = bookingDate.value;
                        if (!value || isPastDate(value) || !isAllowedDay(value)) {
                            e.preventDefault();
                            // Keep the date as-is and surface the error inline
                            updateReserveButtonState();
                        }
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
        </script>
    </div>
</div>
</body>
</html>
