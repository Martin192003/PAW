<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"    uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt"  uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8"/>
    <spring:message code="reviews.title" arguments="${user != null ? user.username : 'Mis'}" var="pageTitle"/>
    <title><c:out value="${pageTitle}"/></title>

    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/userReviews.css'/>"/>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css"/>

    <style>
        @font-face {
            font-family: "Darklinos";
            src: url("../fonts/DARKLINOS.ttf") format("truetype");
            font-weight: normal;
            font-style: normal;
        }

        body {
            font-family: Arial, sans-serif;
            background-color: #1b1a1a;
            color: #fff;
            margin: 0;
            padding: 0;
            overflow-x: hidden;
        }

        .app-reviews {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            justify-content: flex-start;
            width: 100vw;
            min-height: 60vh;
            padding-top: 100px;
            padding-left: 240px;
            box-sizing: border-box;
        }

        .app-reviews .detail-back {
            position: fixed;
            top: 106px;
            left: 190px;
            z-index: 120;
            margin: 0;
        }

        .detail-back .btn-success {
            height: 40px;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .reviews-header {
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            justify-content: flex-start;
            width: calc(100% - 240px);
            max-width: none;
            margin-bottom: 24px;
            gap: 12px;
        }

        .reviews-title {
            font-size: 2.2rem;
            color: #9c27b0;
            margin: 0;
            font-family: "Darklinos", Arial, sans-serif;
            letter-spacing: 1px;
            text-shadow: 2px 2px 4px rgba(216, 0, 183, 0.2);
            display: flex;
            align-items: center;
            gap: 12px;
            align-self: center;
            text-align: center;
        }

        .reviews-title i {
            color: #ffd700;
            font-size: 2rem;
        }

        .reviews-sort-form {
            order: 0;
            display: flex;
            align-items: center;
            justify-content: flex-start;
            width: auto;
            margin: 0;
        }

        .filter-item.filter-sort {
            background: #3c3c3c;
            color: #fff;
            border: 2px solid #9c27b0;
            padding: 10px 16px;
            border-radius: 8px;
            font-size: 14px;
            cursor: pointer;
            min-width: 160px;
            height: 45px;
            outline: none;
            transition: all 0.2s;
        }

        .filter-item.filter-sort:hover {
            background: #9c27b0;
            border-color: #d800b7;
        }

        .filter-item.filter-sort option {
            background: #3c3c3c;
            color: #fff;
        }

        .reviews-list {
            width: calc(100% - 240px);
            max-width: none;
            display: flex;
            flex-direction: column;
            align-items: flex-start;
            gap: 16px;
        }

        .card-md.review-card {
            background: #3c3c3c;
            border-radius: 12px;
            box-shadow: 0 2px 8px rgba(156, 39, 176, 0.15);
            margin-bottom: 24px;
            padding: 20px 28px;
            width: 100%;
            max-width: none;
            display: flex;
            flex-direction: column;
            transition: box-shadow 0.2s, transform 0.2s;
            position: relative;
        }

        .card-md.review-card:hover {
            box-shadow: 0 4px 16px rgba(156, 39, 176, 0.25);
            transform: scale(1.01);
        }

        .review-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
            padding-bottom: 12px;
            border-bottom: 1px solid #555;
            flex-wrap: wrap;
            gap: 12px;
        }

        .review-user-info {
            display: flex;
            align-items: center;
            gap: 12px;
            flex-wrap: wrap;
        }

        .review-username {
            color: #d800b7;
            font-size: 1.1rem;
            font-weight: 700;
        }

        .badge-guest {
            background: linear-gradient(135deg, #9c27b0, #d800b7);
            color: white;
            padding: 6px 14px;
            border-radius: 999px;
            font-size: 0.85rem;
            font-weight: 700;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            box-shadow: 0 2px 6px rgba(156, 39, 176, 0.4);
        }

        .review-rating {
            color: #ffd700;
            font-weight: 700;
            font-size: 1.2rem;
            display: flex;
            align-items: center;
            gap: 6px;
        }

        .review-rating i {
            color: #ffd700;
        }

        .review-body {
            display: flex;
            gap: 20px;
            align-items: flex-start;
        }

        .review-content {
            flex: 1;
        }

        .review-club-name {
            color: #fff;
            font-size: 1.05rem;
            display: block;
            margin-bottom: 8px;
        }

        .review-text {
            color: #eee;
            line-height: 1.6;
            font-size: 1rem;
            margin: 8px 0;
        }

        .review-date {
            color: #aaa;
            font-size: 0.9rem;
        }

        .review-thumb {
            width: 72px;
            height: 72px;
            object-fit: cover;
            border-radius: 12px;
            border: 3px solid #9c27b0;
            box-shadow: 0 4px 12px rgba(156, 39, 176, 0.3);
            cursor: pointer;
            transition: transform 0.2s;
        }

        .review-thumb:hover {
            transform: scale(1.05);
        }

        .pagination {
            display: flex;
            justify-content: center;
            align-items: center;
            margin: 20px 0;
            flex-wrap: wrap;
            width: 100%;
            box-sizing: border-box;
        }

        .page-btn {
            display: inline-block;
            padding: 8px 12px;
            margin: 2px;
            color: #ffffff;
            text-decoration: none;
            border-radius: 4px;
            transition: all 0.2s;
        }

        .page-btn:hover {
            color: #b1b1b1;
        }

        .page-info {
            color: #ffffff;
            margin: 0 10px;
            font-size: 0.95rem;
        }

        /* MODAL DE RESEÑAS - FUNCIONA PERFECTO */
        .reviews-modal {
            position: fixed !important;
            inset: 0 !important;
            display: none;
            align-items: center;
            justify-content: center;
            background: rgba(0,0,0,.6);
            z-index: 100000;
            padding: 20px;
        }

        .reviews-modal.open {
            display: flex !important;
        }

        .review-modal-content {
            background: linear-gradient(180deg, #2b2b2b 0%, #232323 100%);
            border-radius: 12px;
            padding: 24px;
            width: min(92vw, 480px);
            max-height: 80vh;
            overflow: auto;
            box-shadow: 0 8px 30px rgba(0,0,0,0.6), 0 2px 8px rgba(156,39,176,0.2);
            color: #fff;
            position: relative;
        }

        .review-modal-close {
            position: absolute;
            top: 12px;
            right: 16px;
            font-size: 1.8rem;
            cursor: pointer;
            color: #fff;
            width: 32px;
            height: 32px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            transition: background 0.2s;
        }

        .review-modal-close:hover {
            background: rgba(255,255,255,0.1);
        }

        body.reviews-modal-open {
            overflow: hidden !important;
            height: 100%;
            touch-action: none;
        }

        @media (max-width: 900px) {
            .app-reviews {
                padding-left: 20px;
                padding-right: 20px;
            }
            .app-reviews .detail-back { left: 20px; }
            .reviews-header, .reviews-list { width: 100%; padding: 0 20px; }
            .filter-item.filter-sort { width: 100%; }
            .review-body { flex-direction: column; }
            .review-thumb { width: 100%; height: 200px; }
        }

        .btn-delete-review {
            background: #d32f2f !important;           /* Rojo oscuro */
            color: white !important;                  /* Ícono blanco */
            border: none !important;
            width: 36px !important;
            height: 36px !important;
            border-radius: 15% !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
            cursor: pointer !important;
            box-shadow: 0 2px 6px rgba(211, 47, 47, 0.3) !important;
            transition: all 0.2s ease !important;
            font-size: 0.9rem !important;
        }

        .modal-actions.centered .btn-danger {
            background-color: #e53935;
            color: #fff;
            border: none;
            padding: 10px 18px;
            border-radius: 6px;
            box-shadow: 0 4px 12px rgba(229,57,53,0.25);
        }

        .modal-actions.centered {
            justify-content: center; /* center contents horizontally */
        }

    </style>
</head>
<body>

<c:set var="showRightButtons" value="true"/>
<c:set var="showProfileButton" value="true"/>
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>

<c:set var="activePage" value="reviews"/>
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>

<spring:message code="reviews.date.pattern" var="datePattern" text="dd/MM/yyyy HH:mm"/>

<div class="app-reviews">

    <div class="detail-back">
        <button type="button" class="btn btn-success" onclick="window.history.back()">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
    </div>

    <div class="reviews-header">
        <h1 class="reviews-title">
            <i class="fa-solid fa-star"></i>
            <spring:message code="reviews.header" text="RESEÑAS"/>
        </h1>

        <form method="get" class="reviews-sort-form">
            <c:if test="${not empty user}">
                <input type="hidden" name="userId" value="${user.id}"/>
            </c:if>
            <label>
                <select name="sort" class="filter-item filter-sort" onchange="this.form.submit()">
                    <option value="created_desc" <c:if test="${sort == 'created_desc'}">selected</c:if>>
                        <spring:message code="reviews.sort.created_desc" text="Más recientes"/>
                    </option>
                    <option value="rating_desc" <c:if test="${sort == 'rating_desc'}">selected</c:if>>
                        <spring:message code="reviews.sort.rating_desc" text="Mejor puntaje"/>
                    </option>
                    <option value="created_asc" <c:if test="${sort == 'created_asc'}">selected</c:if>>
                        <spring:message code="reviews.sort.created_asc" text="Más antiguas"/>
                    </option>
                    <option value="rating_asc" <c:if test="${sort == 'rating_asc'}">selected</c:if>>
                        <spring:message code="reviews.sort.rating_asc" text="Peor puntaje"/>
                    </option>
                </select>
            </label>
        </form>
    </div>

    <div class="reviews-list">
        <c:choose>
            <c:when test="${not empty reviews}">
                <c:forEach var="r" items="${reviews}">
                    <div class="card-md review-card">
                        <div class="review-header">
                            <div class="review-user-info">
                                <strong class="review-username"><c:out value="${r.user.username}"/></strong>
                                <c:if test="${r.fromTableGuest}">
                                    <span class="badge-guest">
                                        <i class="fa-solid fa-ticket"></i>
                                        <spring:message code="reviews.badge.guest" text="Reservó mesa"/>
                                    </span>
                                </c:if>
                            </div>
                            <span class="review-rating">
                                <c:out value="${r.rating}"/> <i class="fa-solid fa-star"></i>
                            </span>
                            <c:if test="${isAuthenticated and (pageContext.request.userPrincipal.name == r.user.username or pageContext.request.userPrincipal.name == r.user.email)}">
                                <button
                                        type="button"
                                        class="btn-delete-review js-open-delete"
                                        data-review-id="${r.reviewId}"
                                        onclick="if(window.openReviewDeleteModal) openReviewDeleteModal('${r.reviewId}');"
                                        title="<spring:message code='button.delete'/>">
                                    <i class="fa-solid fa-trash"></i>
                                </button>
                            </c:if>
                        </div>

                        <div class="review-body">
                            <div class="review-content">
                                <strong class="review-club-name"><c:out value="${r.club.clubName}"/></strong>
                                <p class="review-text"><c:out value="${r.description}"/></p>
                                <small class="review-date">
                                    <spring:message code="reviews.date" text="Fecha"/>:
                                    <fmt:formatDate value="${r.createdAt}" pattern="${datePattern}"/>
                                </small>
                            </div>
                            <c:if test="${not empty r.photoId}">
                                <img class="review-thumb" src="<c:url value='/photos/${r.photoId}'/>"
                                     alt="<spring:message code='reviews.photo.alt' text='Foto de reseña'/>"
                                     loading="lazy" onclick="openLightbox(this.src)"/>
                            </c:if>
                        </div>
                    </div>
                </c:forEach>

                <div class="pagination">
                    <c:if test="${currentPage > 1}">
                        <a href="?sort=${sort}&offset=${offset - limit}&limit=${limit}" class="page-btn">«</a>
                    </c:if>
                    <span class="page-info">${currentPage} <spring:message code="pagination.of.word" text="de"/> ${totalPages}</span>
                    <c:if test="${currentPage < totalPages}">
                        <a href="?sort=${sort}&offset=${offset + limit}&limit=${limit}" class="page-btn">»</a>
                    </c:if>
                </div>
            </c:when>
            <c:otherwise>
                <div class="empty-state">
                    <spring:message code="reviews.empty" text="No hay reseñas aún."/>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<!-- MODAL DE ELIMINAR RESEÑA -->
<div id="reviewDeleteModal" class="reviews-modal">
    <div class="review-modal-content">
        <span class="review-modal-close" onclick="closeReviewDeleteModal()">×</span>
        <h3><spring:message code="confirm.deleteReview" text="¿Seguro que quieres eliminar esto?"/></h3>
        <form id="reviewDeleteForm" method="post" action="">
            <input type="hidden" name="reviewId" id="deleteReviewId" value="" />
            <c:if test="${not empty requestScope._csrf}">
                <input type="hidden" name="${requestScope._csrf.parameterName}" value="${requestScope._csrf.token}" />
            </c:if>
            <div class="modal-actions centered">
                <button type="submit" class="modal-delete-btn">
                    <spring:message code="button.delete" />
                </button>
            </div>
        </form>
    </div>
</div>

<script>
    (function() {
        const ctx   = '<c:out value="${pageContext.request.contextPath}"/>';
        const modal = document.getElementById('reviewDeleteModal');
        const form  = document.getElementById('reviewDeleteForm');
        const hid   = document.getElementById('deleteReviewId');

        window.openReviewDeleteModal = function(reviewId) {
            if (!modal) return;
            modal.classList.add('open');
            document.body.classList.add('reviews-modal-open');
            try {
                if (hid) hid.value = reviewId || '';
                if (form) form.action = ctx + '/reviews/' + reviewId + '/delete';
            } catch(e) {
                console.error('Error:', e);
            }
        };

        window.closeReviewDeleteModal = function() {
            if (!modal) return;
            modal.classList.remove('open');
            document.body.classList.remove('reviews-modal-open');
            if (hid) hid.value = '';
            if (form) form.action = '';
        };

        document.addEventListener('click', function(ev) {
            const btn = ev.target.closest('.js-open-delete');
            if (btn) {
                ev.preventDefault();
                openReviewDeleteModal(btn.getAttribute('data-review-id'));
                return;
            }
            if (ev.target === modal) closeReviewDeleteModal();
        });

        document.addEventListener('keydown', function(ev) {
            if (ev.key === 'Escape' && modal && modal.classList.contains('open')) {
                closeReviewDeleteModal();
            }
        });
    })();
</script>

</body>
</html>

