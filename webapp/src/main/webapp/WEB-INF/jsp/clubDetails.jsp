<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title><c:out value="${club.clubName}"/> - Detalles</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/details.css' />" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<c:url var="clubUrl" value="/clubs/${club.clubId}"/>

<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title">
            Bling <br>&nbsp;&nbsp;Bling
        </div>
    </a>
</div>

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
            <i class="fa-solid fa-envelope"></i> Contactarse
        </button>
    </div>


    <div class="detail-content">
        <img src="<c:url value='${coverPhotoUrl}' />"
             alt="<c:out value='${club.clubName}'/>" class="detail-image">

        <div class="detail-info">
            <div class="detail-details">
                <span>📍<c:out value="${locationText}"/></span><br>
                <span>🎸${genreText}</span>
                <div>
                    <i class="fa-solid fa-star" style="color:#FFD700;"></i>
                    <c:choose>
                        <c:when test="${club.rating != null}">
                            <span>
                              <fmt:formatNumber value="${club.rating}" minFractionDigits="1" maxFractionDigits="1"/>
                              (<c:out value="${club.quantityRatings}"/> reseñas)
                            </span>
                        </c:when>
                        <c:otherwise><span>No calificado</span></c:otherwise>
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
                    <span>Carta</span>
                    <i class="fa-solid fa-chevron-down"></i>
                </button>
                <div class="bb-panel">
                    <c:choose>
                        <c:when test="${empty productsByCategory}">
                            <p class="bb-empty">No hay productos disponibles</p>
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
                    <span>Mesas</span>
                    <i class="fa-solid fa-chevron-down"></i>
                </button>

                <div class="bb-panel">
                    <c:url var="reserveUrl" value="/clubs/${club.clubId}/reserve"/>
                        <form id="reserveForm" method="post" action="${reserveUrl}">

                        <!-- Fila: Fecha + Botón -->
                        <div class="bb-actions" style="display:flex; gap:12px; align-items:flex-end; flex-wrap:wrap;">
                            <div class="bb-date" style="display:flex; flex-direction:column;">
                                <label for="bookingDate" class="bb-label" style="margin-bottom:6px;">Fecha de reserva</label>
                                <input id="bookingDate" name="date" type="date" class="input"
                                       value="${selectedDate != null ? selectedDate : ''}"
                                       min="<fmt:formatDate value='${now}' pattern='yyyy-MM-dd'/>" />
                                <p class="bb-help" id="dateHelp" style="margin-top:6px; max-width:380px;">
                                    Solo se aceptan <strong>jueves, viernes y sábado</strong>. La franja es
                                    <strong>23:59 a 06:00</strong> (del día siguiente).
                                </p>
                            </div>

                            <button id="reserveBtn" type="submit" class="btn btn-success" disabled
                                    style="height:40px; align-self:end;">
                                <i class="fa-solid fa-calendar-check"></i> Reservar
                            </button>
                        </div>

                        <c:choose>
                            <c:when test="${empty availableTables}">
                                <p class="bb-empty">No hay mesas disponibles</p>
                            </c:when>
                            <c:otherwise>
                                <ul class="table-list">
                                    <c:forEach var="t" items="${availableTables}">
                                        <li class="table-item">
                                            <label class="table-row">
                                                <input type="radio" name="tableId" value="${t.tableId}" />
                                                <span class="table-row-text">
                                                    <strong>Mesa #${t.tableNumber}</strong> · Cap: ${t.capacity}
                                                </span>
                                                <span class="table-row-price">
                                                    $<fmt:formatNumber value="${t.price}" minFractionDigits="0" maxFractionDigits="0"/>
                                                    <c:if test="${t.discount != null && t.discount > 0}">
                                                        <small>(desc: $<fmt:formatNumber value="${t.discount}" minFractionDigits="0" maxFractionDigits="0"/>)</small>
                                                    </c:if>
                                                </span>
                                            </label>
                                        </li>
                                    </c:forEach>
                                </ul>
                            </c:otherwise>
                        </c:choose>
                    </form>
                </div>
            </div>


        </div>

        <script>
            document.addEventListener('DOMContentLoaded', () => {
                // Dropdowns (igual que tu script)
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

                // --- Lógica de fecha permitida (Jue=4, Vie=5, Sáb=6) ---
                const ALLOWED_DAYS = [4,5,6];
                const reserveBtn = document.getElementById('reserveBtn');
                const form = document.getElementById('reserveForm');
                const bookingDate = document.getElementById('bookingDate');
                const dateHelp = document.getElementById('dateHelp');

                const today = new Date();
                const yyyy = today.getFullYear();
                const mm = String(today.getMonth()+1).padStart(2,'0');
                const dd = String(today.getDate()).padStart(2,'0');
                if (bookingDate && !bookingDate.getAttribute('min')) {
                    bookingDate.setAttribute('min', `${yyyy}-${mm}-${dd}`);
                }

                function isAllowedDay(isoDate){
                    if(!isoDate) return false;
                    // usar mediodía para evitar problemas de TZ
                    const d = new Date(isoDate + 'T12:00:00');
                    const dow = d.getDay(); // 0=Dom..6=Sáb
                    return ALLOWED_DAYS.includes(dow);
                }

                function updateReserveButtonState(){
                    const hasTable = !!(form && form.querySelector('input[name="tableId"]:checked'));
                    const hasValidDate = bookingDate && isAllowedDay(bookingDate.value);
                    reserveBtn.disabled = !(hasTable && hasValidDate);

                    if (!bookingDate.value) {
                        dateHelp.innerHTML = 'Elegí una fecha. Solo Jue/Vie/Sáb. Franja: <strong>23:59 a 06:00</strong>.';
                        bookingDate.classList.remove('invalid');
                    } else if (!hasValidDate) {
                        dateHelp.innerHTML = 'La fecha seleccionada no es jueves, viernes ni sábado.';
                        bookingDate.classList.add('invalid');
                    } else {
                        dateHelp.innerHTML = 'Franja fija: <strong>23:59 a 06:00</strong> (del día siguiente).';
                        bookingDate.classList.remove('invalid');
                    }
                }

                updateReserveButtonState();

                bookingDate.addEventListener('change', updateReserveButtonState);

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

                    // Guardia final en submit (defensa extra)
                    form.addEventListener('submit', (e) => {
                        if (!isAllowedDay(bookingDate.value)) {
                            e.preventDefault();
                            alert('Solo se puede reservar jueves, viernes o sábado.');
                        }
                    });
                }

                // recargar al cambiar la fecha
                bookingDate.addEventListener('change', () => {
                    if (!bookingDate.value) return;
                    // si querés forzar solo jue/vie/sáb, podés chequear antes; si no, que el servidor decida
                    window.location.href = "${clubUrl}?date=" + bookingDate.value + "#dd-tables";
                });

                if (location.hash === "#dd-tables") {
                    const dd = document.getElementById('dd-tables');
                    if (dd) {
                        dd.querySelector('.bb-btn')?.classList.add('active');
                        dd.querySelector('.bb-panel')?.classList.add('show');
                    }
                }
            });
        </script>

    </div>
</div>
</body>
</html>
