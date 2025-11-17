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
                                <span class="review-rating">
                                    <c:out value="${r.rating}"/> <i class="fa-solid fa-star"></i>
                                </span>
                            </div>
                                <button
                                        type="button"
                                        class="btn-delete-review js-open-delete"
                                        data-review-id="${r.reviewId}"
                                        onclick="if(window.openReviewDeleteModal) openReviewDeleteModal('${r.reviewId}');"
                                        title="<spring:message code='button.delete'/>">
                                    <i class="fa-solid fa-trash"></i>
                                </button>
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
                        <a href="?sort=${sort}&offset=${offset - limit}&limit=${limit}" class="page-btn"><i class="fa-solid fa-chevron-left"></i></a>
                    </c:if>
                    <span class="page-info">${currentPage} <spring:message code="pagination.of.word" text="de"/> ${totalPages}</span>
                    <c:if test="${currentPage < totalPages}">
                        <a href="?sort=${sort}&offset=${offset + limit}&limit=${limit}" class="page-btn"><i class="fa-solid fa-chevron-right"></i></a>
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
