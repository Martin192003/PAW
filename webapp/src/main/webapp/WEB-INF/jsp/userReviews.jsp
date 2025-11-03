<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8"/>
    <title>
        <c:out value="${user != null ? user.username : 'Mis'}"/> Reseñas
    </title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/userReviews.css' />" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

<c:set var="showRightButtons" value="true" />
<c:set var="showProfileButton" value="true" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>

<c:set var="activePage" value="reviews" />
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>

<%-- CONTENEDOR PRINCIPAL --%>
<div class="app-reviews">

    <div class="detail-back">
        <button type="button" class="btn btn-success" onclick="window.history.back()">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
    </div>

    <%-- HEADER FIJO: Volver + Título + Orden --%>
    <div class="reviews-header">

        <h1 class="reviews-title"><i class="fa-solid fa-star"></i> RESEÑAS </h1>

        <form method="get" class="reviews-sort-form">
            <c:if test="${not empty user}">
                <input type="hidden" name="userId" value="${user.id}"/>
                <label>
                    <select name="sort" class="filter-item filter-sort" onchange="this.form.submit()">
                        <option value="created_desc" <c:if test="${sort == 'created_desc'}">selected</c:if>>Más recientes</option>
                        <option value="rating_desc" <c:if test="${sort == 'rating_desc'}">selected</c:if>>Mejor puntaje</option>
                        <option value="created_asc" <c:if test="${sort == 'created_asc'}">selected</c:if>>Más antiguas</option>
                        <option value="rating_asc" <c:if test="${sort == 'rating_asc'}">selected</c:if>>Peor puntaje</option>
                    </select>
                </label>
            </c:if>
        </form>



    </div>





    <%-- LISTA DE RESEÑAS --%>
    <div class="reviews-list">
        <c:choose> <c:when test="${not empty reviews}">
            <c:forEach var="r" items="${reviews}">
            <div class="card-md review-card">
                    <%-- Header de la reseña --%>
                <div class="review-header">
                    <div class="review-user-info"><strong class="review-username"><c:out
                            value="${r.user.username}"/></strong> <c:if test="${r.fromTableGuest}"> <span
                            class="badge-guest"> <i class="fa-solid fa-ticket"></i> <spring:message
                            code="reviews.badge.guest" text="Reservó mesa"/> </span> </c:if></div>
                    <span class="review-rating"> ${r.rating} <i class="fa-solid fa-star"></i> </span></div>
                    <%-- Cuerpo de la reseña --%>
                <div class="review-body">
                    <div class="review-content"><strong class="review-club-name"><c:out
                            value="${r.club.clubName}"/></strong>
                        <p class="review-text"><c:out value="${r.description}"/></p> <small class="review-date">
                            <spring:message code="reviews.date" text="Fecha"/>: <fmt:formatDate value="${r.createdAt}"
                                                                                                pattern="dd/MM/yyyy HH:mm"/> </small>
                    </div>
                        <%-- Thumbnail de la foto --%>
                    <c:if test="${not empty r.photoId}"> <img class="review-thumb"
                                                              src="<c:url value='/photos/${r.photoId}'/>"
                                                              alt="Foto de reseña"
                                                              loading="lazy"
                                                              onclick="openLightbox(this.src)"/>
                    </c:if>
                </div>
            </div>
        </c:forEach>

                <!-- PAGINACIÓN -->
                <div class="pagination">
                    <c:if test="${currentPage > 1}">
                        <a href="?sort=${sort}&offset=${offset- limit}&limit=${limit}" class="page-btn">&laquo;</a>
                    </c:if>
                    <span class="page-info">${currentPage} de ${totalPages}</span>
                    <c:if test="${currentPage < totalPages}">
                        <a href="?sort=${sort}&offset=${offset + limit}&limit=${limit}" class="page-btn">&raquo;</a>
                    </c:if>
                </div>
            </c:when>
            <c:otherwise>
                <div class="empty-state">No hay reseñas aún.</div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<script>
    function openLightbox(src) {
        const overlay = document.createElement('div');
        overlay.style.cssText = 'position:fixed;top:0;left:0;width:100vw;height:100vh;background:rgba(0,0,0,0.95);z-index:9999;display:flex;align-items:center;justify-content:center;cursor:zoom-out;';
        overlay.onclick = () => overlay.remove();
        const img = new Image();
        img.src = src;
        img.style.cssText = 'max-width:90%;max-height:90%;border-radius:16px;border:4px solid #9c27b0;box-shadow:0 0 30px rgba(156,39,176,0.8);';
        overlay.appendChild(img);
        document.body.appendChild(overlay);
    }
</script>
</body>

</html>