<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
                                         loading="lazy" draggable="false" style="object-fit: contain;">
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
                        <div style="display:flex; justify-content:center; margin-top:12px; align-items:center;">
                            <button id="reserveBtn" type="submit" class="btn btn-success"
                                    <c:if test="${empty availableTables}">disabled</c:if>
                                    style="height:40px; align-self:end; width: 160px">
                                <i class="fa-solid fa-calendar-check"></i> <spring:message code="button.reservation"/>
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
        <div class="map-container">
            <h3><i class="fa-solid fa-map-location-dot"></i> <spring:message code="form.location"/></h3>
            <div id="clubMap"></div>
        </div>

        <c:if test="${not empty reviewPhotos}">
            <div class="review-photos-section">
                <h3><i class="fa-solid fa-images"></i> <spring:message code="reviews.photos" text="Fotos de reseñas"/></h3>

                <div class="review-photos-carousel-wrapper">
                    <button class="carousel-btn left" onclick="scrollReviewPhotos(-1)">
                        <i class="fa-solid fa-chevron-left"></i>
                    </button>

                    <div class="review-photos-carousel" id="reviewPhotosCarousel">
                        <c:forEach var="ph" items="${reviewPhotos}">
                            <div class="review-photo-item">
                                <img src="<c:url value='/photos/${ph.photoId}'/>"
                                     alt="Foto de reseña"
                                     loading="lazy"
                                     onclick="openLightbox(this.src)"
                                     style="cursor: pointer;" />
                            </div>
                        </c:forEach>
                    </div>

                    <button class="carousel-btn right" onclick="scrollReviewPhotos(1)">
                        <i class="fa-solid fa-chevron-right"></i>
                    </button>
                </div>
            </div>
        </c:if>



        <script src="https://unpkg.com/leaflet/dist/leaflet.js"></script>


        <!-- Reviews: listado -->
        <section id="reviews" class="reviews-list-section">
            <div class="contact-card reviews-card">
                <div class="review-header">
                    <h3 class="contact-title">
                        <span><spring:message code="reviews.title" text="Reseñas"/></span>
                    </h3>

                    <c:if test="${isLoggedIn}">
                        <button type="button" class="btn btn-primary" onclick="openReviewModal()" style="margin-bottom: 15px">
                            <i class="fa-solid fa-star"></i> <spring:message code="reviews.add" text="Dejar una reseña"/>
                        </button>
                    </c:if>
                </div>

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
    </div>
    <script>
        document.addEventListener('DOMContentLoaded', async () => {
            const address = "${club.location}";
            let lat = -34.5891429, lng = -58.3961641;

            try {
                const response = await fetch("https://nominatim.openstreetmap.org/search?format=json&q=" + encodeURIComponent(address));
                const results = await response.json();
                if (results && results.length > 0) {
                    lat = results[0].lat;
                    lng = results[0].lon;
                }
            } catch (e) {
                console.error("Error al geocodificar:", e);
            }

            const map = L.map('clubMap', {
                center: [lat, lng],
                zoom: 16,
                zoomControl: false,
                dragging: false,
                scrollWheelZoom: false,
                doubleClickZoom: false,
                boxZoom: false,
                keyboard: false
            });

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; OpenStreetMap contributors'
            }).addTo(map);

            L.marker([lat, lng]).addTo(map)
                .bindPopup(`<b>${"${club.clubName}"}</b><br>${address}`)
                .openPopup();

            // Estado de interacción
            let interactive = false;

            // Escuchamos el click en el contenedor del mapa
            const mapContainer = document.getElementById('clubMap');
            mapContainer.style.cursor = "pointer";

            mapContainer.addEventListener('click', () => {
                if (!interactive) {
                    // Activar interacción en el siguiente ciclo de event loop
                    setTimeout(() => {
                        map.dragging.enable();
                        map.scrollWheelZoom.enable();
                        map.doubleClickZoom.enable();
                        map.boxZoom.enable();
                        map.keyboard.enable();
                        L.control.zoom({ position: 'topright' }).addTo(map);
                        mapContainer.style.cursor = "grab";
                        interactive = true;
                        console.log("Mapa ahora es interactivo");
                    }, 50);
                }
            });
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

        document.addEventListener('DOMContentLoaded', () => {
            const stars = document.querySelectorAll('#starRating i');
            const hiddenInput = document.getElementById('rating');
            let currentRating = 5; // valor por defecto

            const updateStars = (value) => {
                stars.forEach(star => {
                    star.classList.toggle('inactive', star.dataset.value > value);
                });
            };

            stars.forEach(star => {
                star.addEventListener('click', () => {
                    const value = parseInt(star.dataset.value);
                    currentRating = value;
                    hiddenInput.value = value;
                    updateStars(value);
                });

                // efecto hover (opcional)
                star.addEventListener('mouseenter', () => {
                    updateStars(star.dataset.value);
                });

                star.addEventListener('mouseleave', () => {
                    updateStars(currentRating);
                });
            });

            // inicializa con todas activas
            updateStars(currentRating);
        });
    </script>
    <script src="https://unpkg.com/leaflet/dist/leaflet.js"></script>
</div>
<div id="reservationToast" class="toast-popup" style="display:none;">
    <i class="fa-solid fa-circle-check" style="color:#4caf50; font-size:24px;"></i>
    <span><spring:message code="reservation.sent"/></span>
</div>

<div id="eventAvailabilityModal" class="modal" aria-hidden="true" role="dialog">
    <div class="modal-content">
        <span class="modal-close" onclick="closeEventAvailabilityModal()">&times;</span>
        <h3 style="margin:0 0 10px;"><spring:message code="event.availability.title"/></h3>
        <p id="eventAvailabilityText" style="margin:0 0 14px;"></p>
        <div class="modal-actions">
            <button type="button" class="btn btn-secondary" onclick="closeEventAvailabilityModal()"><spring:message code="event.availability.close"/></button>
            <button type="button" class="btn btn-success" onclick="closeEventAvailabilityModal()"><spring:message code="event.availability.ok"/></button>
        </div>
    </div>
</div>
<div id="reviewModal" class="modal" aria-hidden="true" role="dialog" >
    <div class="modal-content" style="max-width: 520px; width: 90%;">
        <span class="modal-close" onclick="closeReviewModal()">&times;</span>

        <div class="review-modal-header" style="margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center;">
            <h3 style="margin: 0; font-size:26px"><spring:message code="reviews.add" text="Dejar una reseña"/></h3>
            <span class="form-text-required"><spring:message code="form.required" text="Campos obligatorios"/></span>
        </div>

        <c:url var="reviewAction" value="/clubs/${club.clubId}/reviews"/>

        <form:form method="post" modelAttribute="reviewForm"
                   action="${reviewAction}"
                   cssClass="review-form"
                   enctype="multipart/form-data"
                   id="reviewModalForm">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

            <div class="form-group">
                <label for="modalRating"><spring:message code="reviews.rating" text="Puntuación"/> *</label>
                <div id="modalStarRating" class="star-rating">
                    <i class="fa-solid fa-star" data-value="1"></i>
                    <i class="fa-solid fa-star" data-value="2"></i>
                    <i class="fa-solid fa-star" data-value="3"></i>
                    <i class="fa-solid fa-star" data-value="4"></i>
                    <i class="fa-solid fa-star" data-value="5"></i>
                </div>
                <input type="hidden" name="rating" id="modalRating" value="5" />
            </div>

            <div class="form-group" style="margin-top: 16px;">
                <label for="modalPhoto"><spring:message code="reviews.photo" text="Foto (opcional)"/></label>
                <input type="file" id="modalPhoto" name="photo" accept="image/*" class="form-control"/>
            </div>

            <div class="form-group" style="margin-top: 16px;">
                <label for="modalDescription"><spring:message code="reviews.comment" text="Mensaje"/> *</label>
                <form:textarea path="description" id="modalDescription" cssClass="form-control" rows="4"/>
                <form:errors path="description" cssClass="error"/>
            </div>

            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeReviewModal()">
                    <spring:message code="button.cancel" text="Cancelar"/>
                </button>
                <button type="submit" class="btn btn-primary">
                    <i class="fa-solid fa-paper-plane"></i>
                    <spring:message code="reviews.submit" text="Enviar reseña"/>
                </button>
            </div>
        </form:form>
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
            if (!fromEvent || !dateParam) return;

            var serverCount = Number('<c:out value="${empty availableTables ? 0 : fn:length(availableTables)}"/>'.replace(/[^0-9]/g,''));
            if (isNaN(serverCount)) serverCount = 0;

            var domCount = document.querySelectorAll('.table-list li').length;

            var availableCount = Math.max(serverCount, domCount);

            const availableTpl = `<spring:message code="event.availability.available"/>`;
            const soldoutTpl  = `<spring:message code="event.availability.soldout"/>`;

            function formatTpl(tpl, ...args) {
                return tpl.replace(/\{(\d+)\}/g, (m, idx) => {
                    const i = Number(idx);
                    return (typeof args[i] !== 'undefined') ? args[i] : m;
                });
            }

            const msg = (availableCount > 0)
                ? formatTpl(availableTpl, availableCount, dateParam)
                : formatTpl(soldoutTpl, dateParam);

            openEventAvailabilityModal(msg);
        });

    })();
    function scrollReviewPhotos(direction) {
        const carousel = document.getElementById('reviewPhotosCarousel');
        if (!carousel) return;

        const itemWidth = carousel.querySelector('.review-photo-item').offsetWidth;
        const gap = 12; // del CSS
        const scrollAmount = (itemWidth + gap) * 3; // 3 items visibles

        carousel.scrollBy({
            left: direction * scrollAmount,
            behavior: 'smooth'
        });
    }

    // Opcional: Lightbox simple al hacer click
    function openLightbox(src) {
        const lightbox = document.createElement('div');
        lightbox.style.position = 'fixed';
        lightbox.style.inset = '0';
        lightbox.style.background = 'rgba(0,0,0,0.9)';
        lightbox.style.display = 'flex';
        lightbox.style.alignItems = 'center';
        lightbox.style.justifyContent = 'center';
        lightbox.style.zIndex = '10000';
        lightbox.style.cursor = 'pointer';
        lightbox.onclick = () => lightbox.remove();

        const img = document.createElement('img');
        img.src = src;
        img.style.maxWidth = '90%';
        img.style.maxHeight = '90%';
        img.style.borderRadius = '12px';
        img.style.boxShadow = '0 0 20px rgba(0,0,0,0.5)';

        lightbox.appendChild(img);
        document.body.appendChild(lightbox);
    }

    // Mostrar/ocultar flechas según scroll
    document.addEventListener('DOMContentLoaded', () => {
        const carousel = document.getElementById('reviewPhotosCarousel');
        const leftBtn = document.querySelector('.review-photos-carousel-wrapper .left');
        const rightBtn = document.querySelector('.review-photos-carousel-wrapper .right');

        if (!carousel || !leftBtn || !rightBtn) return;

        function updateButtons() {
            leftBtn.style.opacity = carousel.scrollLeft <= 0 ? '0.3' : '1';
            leftBtn.style.pointerEvents = carousel.scrollLeft <= 0 ? 'none' : 'auto';

            const atEnd = carousel.scrollLeft + carousel.clientWidth >= carousel.scrollWidth - 10;
            rightBtn.style.opacity = atEnd ? '0.3' : '1';
            rightBtn.style.pointerEvents = atEnd ? 'none' : 'auto';
        }

        carousel.addEventListener('scroll', updateButtons);
        updateButtons(); // inicial
    });
    function openReviewModal() {
        const modal = document.getElementById('reviewModal');
        if (modal) {
            modal.classList.add('show');
            modal.setAttribute('aria-hidden', 'false');
            document.body.style.overflow = 'hidden'; // evita scroll de fondo
            initModalStarRating(); // reinicia estrellas
        }
    }

    function closeReviewModal() {
        const modal = document.getElementById('reviewModal');
        if (modal) {
            modal.classList.remove('show');
            modal.setAttribute('aria-hidden', 'true');
            document.body.style.overflow = '';
            // reset form
            document.getElementById('reviewModalForm').reset();
            document.getElementById('modalRating').value = '5';
            updateModalStars(5);
        }
    }

    // Inicializar estrellas en el modal
    function initModalStarRating() {
        const stars = document.querySelectorAll('#modalStarRating i');
        const hiddenInput = document.getElementById('modalRating');
        let currentRating = 5;

        const updateModalStars = (value) => {
            stars.forEach(star => {
                star.classList.toggle('inactive', star.dataset.value > value);
            });
        };

        stars.forEach(star => {
            star.onclick = () => {
                currentRating = parseInt(star.dataset.value);
                hiddenInput.value = currentRating;
                updateModalStars(currentRating);
            };
            star.onmouseenter = () => updateModalStars(star.dataset.value);
            star.onmouseleave = () => updateModalStars(currentRating);
        });

        updateModalStars(currentRating);
    }

    // Cerrar modal al hacer click fuera
    document.addEventListener('click', (e) => {
        const modal = document.getElementById('reviewModal');
        if (modal && modal.classList.contains('show') && !e.target.closest('.modal-content') && !e.target.closest('[onclick="openReviewModal()"]')) {
            closeReviewModal();
        }
    });

    // Cerrar con ESC
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeReviewModal();
    });
    document.addEventListener('DOMContentLoaded', function() {
        // Busca si hay algún elemento con clase "error" dentro del modal de reseña
        const hasErrors = document.querySelector('#reviewModal .error') !== null;

        if (hasErrors) {
            // Si hay errores, muestra el modal automáticamente
            const modal = document.getElementById('reviewModal');
            if (modal) {
                modal.classList.add('show');
                modal.setAttribute('aria-hidden', 'false');
                document.body.style.overflow = 'hidden'; // Evita que se pueda scrollear el fondo
            }
        }
    });
</script>
</body>
</html>
