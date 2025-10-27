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
    <link rel="stylesheet" href="<c:url value='/resources/css/carousel.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/reviews.css' />">
    <link rel="stylesheet" href="https://unpkg.com/leaflet/dist/leaflet.css" />
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
            <c:if test="${not isOwner}">
                <button type="button" class="btn btn-success" onclick="window.location.href='<c:url value="/contact"><c:param name="id" value="${club.clubId}"/></c:url>'">
                    <i class="fa-solid fa-envelope"></i>
                    <spring:message code="button.contact"/>
                </button>
            </c:if>
        </div>
    </div>

    <div class="detail-content">
        <div id="club-carousel" class="photo-carousel">
            <c:choose>
                <c:when test="${not empty clubPhotos}">
                    <div class="carousel-container">
                        <div class="carousel-track" id="carouselTrack">
                            <c:forEach var="photo" items="${clubPhotos}" varStatus="status">
                                <div class="carousel-slide${status.index == 0 ? ' active' : ''}">
                                    <img src="<c:url value='${photo.url}'/>"
                                         alt="<c:out value='${club.clubName}'/> - Foto ${status.index + 1}"
                                         loading="lazy" draggable="false">
                                </div>
                            </c:forEach>
                        </div>

                        <c:if test="${fn:length(clubPhotos) > 1}">
                            <button class="carousel-btn carousel-prev" id="prevBtn" type="button">
                                <i class="fa-solid fa-chevron-left"></i>
                            </button>
                            <button class="carousel-btn carousel-next" id="nextBtn" type="button">
                                <i class="fa-solid fa-chevron-right"></i>
                            </button>

                            <div class="carousel-counter" id="carouselCounter">—</div>
                        </c:if>
                    </div>

                    <c:if test="${fn:length(clubPhotos) > 1}">
                        <div class="carousel-indicators" id="carouselIndicators">
                            <c:forEach begin="0" end="${fn:length(clubPhotos) - 1}" var="i">
                                <button class="carousel-dot${i == 0 ? ' active' : ''}" type="button"></button>
                            </c:forEach>
                        </div>
                    </c:if>
                </c:when>
                <c:otherwise>
                    <div class="no-photos">
                        <i class="fa-solid fa-camera"></i>
                        <p><spring:message code="photos.noPhotos" text="No hay fotos disponibles para la galería"/></p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>


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
                    <c:if test="${not empty tableLayoutUrl}">
                        <div class="table-layout-container" style="text-align:center; margin-bottom:12px;">
                            <img src="<c:url value='${tableLayoutUrl}' />" alt="Layout de mesas"
                                 style="max-width:100%; border-radius:8px; border:1px solid #333;"/>
                        </div>
                    </c:if>
                    <c:url var="reserveUrl" value="/clubs/${club.clubId}/reserve"/>
                    <c:if test="${not empty error}">
                        <div class="bb-error" style="background:#5a1a1a;color:#fff;padding:10px;border-radius:6px;margin-bottom:12px;">
                            <spring:message code="${error}"/>
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
        <div class="map-container">
            <h3><i class="fa-solid fa-map-location-dot"></i> <spring:message code="form.location"/></h3>
            <div id="clubMap"></div>
        </div>

        <c:if test="${not empty reviewPhotos}">
            <!-- Usa el mismo ID que el CSS espera -->
            <div id="club-carousel" class="photo-carousel review-carousel">
                <h3><i class="fa-solid fa-images"></i>
                    <spring:message code="reviews.photos" text="Fotos de reseñas"/>
                </h3>

                <div class="carousel-container">
                    <div class="carousel-track">
                        <c:forEach var="ph" items="${reviewPhotos}" varStatus="st">
                            <div class="carousel-slide${st.index == 0 ? ' active' : ''}">
                                <img src="<c:url value='/photos/${ph.photoId}'/>"
                                     alt="Foto de reseña ${st.index + 1}"
                                     loading="lazy" draggable="false">
                            </div>
                        </c:forEach>
                    </div>

                    <c:if test="${fn:length(reviewPhotos) > 1}">
                        <button class="carousel-btn carousel-prev" type="button">
                            <i class="fa-solid fa-chevron-left"></i>
                        </button>
                        <button class="carousel-btn carousel-next" type="button">
                            <i class="fa-solid fa-chevron-right"></i>
                        </button>
                        <div class="carousel-counter">—</div>
                    </c:if>
                </div>

                <c:if test="${fn:length(reviewPhotos) > 1}">
                    <div class="carousel-indicators">
                        <c:forEach begin="0" end="${fn:length(reviewPhotos) - 1}" var="i">
                            <button class="carousel-dot${i == 0 ? ' active' : ''}" type="button"></button>
                        </c:forEach>
                    </div>
                </c:if>
            </div>
        </c:if>



        <script src="https://unpkg.com/leaflet/dist/leaflet.js"></script>


        <!-- Reviews: listado -->
        <section id="reviews" class="reviews-list-section">
            <div class="contact-card reviews-card">
                <h3 class="contact-title">
                    <span><spring:message code="reviews.title" text="Reseñas"/></span>
                </h3>

                <c:choose>
                    <c:when test="${not empty reviews}">
                        <ul class="reviews-list">
                            <c:forEach var="review" items="${reviews}">
                                <li class="review-item">
                                    <div class="review-item-header">
                                        <strong class="review-user"><c:out value="${review.user.username}"/></strong>
                                        <span class="review-rating" aria-label="rating">
                                            ${review.rating}
                                            <i class="fa-solid fa-star" style="color:#FFD700;"></i>
                                        </span>
                                    </div>
                                    <p class="review-text">
                                        <c:out value="${review.description}"/>
                                    </p>
                                    <small class="review-date">
                                        <spring:message code="reviews.date" text="Fecha"/>:
                                        <fmt:formatDate value="${review.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                                    </small>
                                </li>
                            </c:forEach>
                        </ul>
                    </c:when>
                    <c:otherwise>
                        <p class="no-reviews">
                            <spring:message code="reviews.empty" text="Todavía no hay reseñas."/>
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>
        </section>

        <c:if test="${isLoggedIn}">
            <section class="review-section">
                <div class="contact-card review-card">
                    <div class="review-header">
                        <h3 class="contact-title">
                            <span><spring:message code="reviews.add" text="Dejar una reseña"/></span>
                        </h3>
                        <span class="form-text-required"><spring:message code="form.required" text="Campos obligatorios"/></span>
                    </div>

                    <form method="post"
                          action="<c:url value='/clubs/${club.clubId}/reviews'/>"
                          class="review-form"
                          enctype="multipart/form-data">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

                        <div class="form-group">
                            <label for="rating"><spring:message code="reviews.rating" text="Puntuación"/> *</label>
                            <select name="rating" id="rating" class="form-control" required>
                                <c:forEach var="i" begin="1" end="5">
                                    <option value="${i}">${i}</option>
                                </c:forEach>
                            </select>

                            <label for="photo"><spring:message code="reviews.photo" text="Foto (opcional)"/></label>
                            <input type="file" id="photo" name="photo" accept="image/*" class="form-control"/>
                            <small class="bb-help"><spring:message code="reviews.photo.help"
                                                                   text="Formatos: JPG/PNG/WebP. Máx. 5MB."/></small>
                        </div>

                        <div class="form-group">
                            <label for="description"><spring:message code="reviews.comment" text="Mensaje"/> *</label>
                            <textarea name="description" id="description" class="form-control" rows="5"
                                      placeholder="<spring:message code='reviews.placeholder' text='Contanos tu experiencia...'/>"
                                      required></textarea>
                        </div>

                        <div class="contact-buttons">
                            <button type="button" class="btn btn-secondary"
                                    onclick="window.location.hash='#reviews';">
                                <spring:message code="button.cancel" text="Cancelar"/>
                            </button>

                            <button type="submit" class="btn btn-primary">
                                <i class="fa-solid fa-paper-plane"></i>
                                <spring:message code="reviews.submit" text="Enviar reseña"/>
                            </button>
                        </div>
                    </form>
                </div>
            </section>
        </c:if>


    </div>
    <script>
        document.addEventListener('DOMContentLoaded', async () => {
            const address = "${club.location}"; // dirección guardada en la DB

            let lat = -34.5891429; // coordenadas por defecto
            let lng = -58.3961641;

            try {
                const response = await fetch("https://nominatim.openstreetmap.org/search?format=json&q=" + encodeURIComponent(address));
                const results = await response.json();
                if (results && results.length > 0) {
                    lat = results[0].lat;
                    lng = results[0].lon;
                } else {
                    console.warn("No se encontró la ubicación, usando coordenadas por defecto");
                }
            } catch (error) {
                console.error("Error al geocodificar la dirección:", error);
            }

            const map = L.map('clubMap').setView([lat, lng], 16);

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(map);

            L.marker([lat, lng]).addTo(map)
                .bindPopup(`<b>${"${club.clubName}"}</b><br>${address}`)
                .openPopup();
        });
    </script>
    <c:if test="${not empty reservationSent}">
        <script>
            document.addEventListener("DOMContentLoaded", function() {
                const toast = document.getElementById("reservationToast");
                toast.classList.add("show");
                setTimeout(() => { toast.classList.remove("show"); }, 3000);
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
    <script src="https://unpkg.com/leaflet/dist/leaflet.js"></script>
</div>
</div>
<!-- Pop-up de confirmación de reserva -->
<div id="reservationToast" class="toast-popup" style="display:none;">
    <i class="fa-solid fa-circle-check" style="color:#4caf50; font-size:24px;"></i>
    <span><spring:message code="reservation.sent"/></span>
</div>

<!-- Modal: disponibilidad de mesas para el evento (con estilos mínimos) -->
<style>
    .modal{position:fixed;inset:0;display:none;align-items:center;justify-content:center;background:rgba(0,0,0,.55);z-index:9999}
    .modal.show{display:flex}
    .modal-content{background:#222;color:#fff;border-radius:10px;min-width:280px;max-width:520px;width:90%;padding:18px;box-shadow:0 10px 30px rgba(0,0,0,.45)}
    .modal-content .modal-close{float:right;font-size:24px;cursor:pointer}
    .modal-actions{margin-top:14px;display:flex;gap:10px;justify-content:flex-end}
</style>

<div id="eventAvailabilityModal" class="modal" aria-hidden="true" role="dialog">
    <div class="modal-content">
        <span class="modal-close" onclick="closeEventAvailabilityModal()">&times;</span>
        <h3 style="margin:0 0 10px;">Disponibilidad para el evento</h3>
        <p id="eventAvailabilityText" style="margin:0 0 14px;"></p>
        <div class="modal-actions">
            <button type="button" class="btn btn-secondary" onclick="closeEventAvailabilityModal()">Cerrar</button>
            <button type="button" class="btn btn-success" onclick="closeEventAvailabilityModal()">OK</button>
        </div>
    </div>
</div>

<script>
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('.photo-carousel').forEach((root) => {
            const track    = root.querySelector('.carousel-track');
            if (!track) return;

            let   slides   = Array.from(track.querySelectorAll('.carousel-slide'));
            const prevBtn  = root.querySelector('.carousel-prev');
            const nextBtn  = root.querySelector('.carousel-next');
            const counter  = root.querySelector('.carousel-counter');
            const dots     = Array.from(root.querySelectorAll('.carousel-indicators .carousel-dot'));

            let current = Math.max(0, slides.findIndex(s => s.classList.contains('active')));
            if (current === -1) current = 0;

            const refresh = () => { slides = Array.from(track.querySelectorAll('.carousel-slide')); };

            function paint() {
                refresh();
                const total = slides.length;
                if (!total) { if (counter) counter.textContent = ''; return; }

                if (current < 0) current = 0;
                if (current > total - 1) current = total - 1;

                slides.forEach((s, i) => s.classList.toggle('active', i === current));
                dots.forEach((d, i)   => d.classList.toggle('active', i === current));

                if (counter) counter.textContent = (current + 1) + ' / ' + total;
                if (prevBtn) prevBtn.disabled = (current === 0);
                if (nextBtn) nextBtn.disabled = (current === total - 1);
            }

            const goTo  = (i) => { if (i < 0 || i >= slides.length) return; current = i; paint(); };
            const next  = ()   => goTo(current + 1);
            const prev  = ()   => goTo(current - 1);

            prevBtn && prevBtn.addEventListener('click', prev);
            nextBtn && nextBtn.addEventListener('click', next);
            dots.forEach((d, i) => d.addEventListener('click', () => goTo(i)));

            // init tras cargar imágenes
            const imgs = track.querySelectorAll('img');
            let loaded = 0;
            const done = () => { loaded++; if (loaded === imgs.length) requestAnimationFrame(paint); };
            imgs.forEach(img => (img.complete ? done() : (img.onload = img.onerror = done)));
            requestAnimationFrame(paint);
        });
    });

    (function(){
        function openEventAvailabilityModal(message){
            var modal = document.getElementById('eventAvailabilityModal');
            var textEl = document.getElementById('eventAvailabilityText');
            if(!modal || !textEl) return;
            textEl.textContent = message;
            modal.classList.add('show');
            modal.setAttribute('aria-hidden','false');
        }
        window.closeEventAvailabilityModal = function(){
            var modal = document.getElementById('eventAvailabilityModal');
            if(!modal) return;
            modal.classList.remove('show');
            modal.setAttribute('aria-hidden','true');
        };

        document.addEventListener('DOMContentLoaded', function(){
            var params = new URLSearchParams(window.location.search);
            var fromEvent = params.get('fromEvent') === 'true';
            var dateParam = params.get('date');
            if(!fromEvent || !dateParam) return;

            // 1) Conteo del servidor (si availableTables está en el modelo)
            var serverCount = Number('${empty availableTables ? 0 : fn:length(availableTables)}'.replace(/[^0-9]/g,''));
            if(isNaN(serverCount)) serverCount = 0;

            // 2) Fallback: contar ítems renderizados en la lista de mesas
            var domCount = document.querySelectorAll('.table-list li').length;

            var availableCount = Math.max(serverCount, domCount);

            var msg = (availableCount > 0)
                ? ('Quedan ' + availableCount + ' mesas disponibles para el evento (' + dateParam + ').')
                : ('Se agotaron las mesas para el evento (' + dateParam + ').');

            openEventAvailabilityModal(msg);
        });
    })();
</script>
</body>
</html>
