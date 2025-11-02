<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"    uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt"  uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn"   uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8"/>
    <title>
        <c:choose>
            <c:when test="${not empty targetUser}">
                <c:out value="${targetUser.username}"/> - <spring:message code="reviews.title" text="Reseñas"/>
            </c:when>
            <c:otherwise>
                <spring:message code="reviews.title" text="Reseñas"/>
            </c:otherwise>
        </c:choose>
    </title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/reviews.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/userReviews.css' />" />

</head>
<body>
<div class="page-wrap" id="reviews">
    <div class="header-row">
        <h1 style="margin:0">
            <c:choose>
                <c:when test="${not empty targetUser}">
                    <spring:message code="reviews.user.title" text="Reseñas de"/> <c:out value="${targetUser.username}"/>
                </c:when>
                <c:otherwise>
                    <spring:message code="reviews.title" text="Reseñas"/>
                </c:otherwise>
            </c:choose>
        </h1>

        <div>
            <!-- Orden (opcional) -->
            <form method="get" style="display:flex; gap:8px; align-items:center;">
                <input type="hidden" name="offset" value="0"/>
                <label class="sr-only" for="sortSel">Orden</label>
                <select id="sortSel" name="sort" class="btn btn-secondary" onchange="this.form.submit()">
                    <option value="created_desc" <c:if test="${sort == 'created_desc'}">selected</c:if>>
                        <spring:message code="filter.sort.newest" text="Más recientes"/>
                    </option>
                    <option value="created_asc"  <c:if test="${sort == 'created_asc'}">selected</c:if>>
                        <spring:message code="filter.sort.oldest" text="Más antiguas"/>
                    </option>
                    <option value="rating_desc"  <c:if test="${sort == 'rating_desc'}">selected</c:if>>
                        <spring:message code="filter.sort.rating.desc" text="Mejor puntaje"/>
                    </option>
                    <option value="rating_asc"   <c:if test="${sort == 'rating_asc'}">selected</c:if>>
                        <spring:message code="filter.sort.rating.asc" text="Peor puntaje"/>
                    </option>
                </select>
            </form>
        </div>
    </div>

    <c:choose>
        <c:when test="${not empty reviews}">
            <ul class="reviews-list">
                <c:forEach var="r" items="${reviews}">
                    <li class="review-item">
                        <div class="review-item-header">
                            <div style="display:flex;align-items:center;gap:10px;flex-wrap:wrap">
                                <strong class="review-user">
                                    <c:out value="${r.club.clubName}"/>
                                </strong>

                                <c:if test="${r.fromTableGuest}">
                  <span class="badge-guest">
                    <i class="fa-solid fa-ticket"></i>
                    <spring:message code="reviews.badge.guest" text="Reservó mesa"/>
                  </span>
                                </c:if>
                            </div>

                            <span class="review-rating" aria-label="rating" style="font-weight:700;">
                ${r.rating} <i class="fa-solid fa-star" style="color:#FFD700;"></i>
              </span>
                        </div>

                        <div class="review-row">
                            <div class="review-col">
                                <p class="review-text"><c:out value="${r.description}"/></p>
                                <small class="review-date">
                                    <spring:message code="reviews.date" text="Fecha"/>:
                                    <fmt:formatDate value="${r.createdAt}" pattern="dd/MM/yyyy HH:mm"/>
                                </small>
                            </div>

                            <c:if test="${not empty r.photoId}">
                                <img class="review-thumb"
                                     src="<c:url value='/photos/${r.photoId}'/>"
                                     alt="Foto de reseña"
                                     loading="lazy"
                                     onclick="openLightbox(this.src)"/>
                            </c:if>
                        </div>
                    </li>
                </c:forEach>
            </ul>

            <!-- Resumen -->
            <c:set var="pLimit"  value="${limit != null ? limit : 10}" />
            <c:set var="pOffset" value="${offset != null ? offset : 0}" />
            <c:set var="pTotal"  value="${total != null ? total : 0}" />
            <c:set var="currentPage" value="${(pOffset / pLimit) + 1}" />
            <c:set var="totalPages"  value="${(pTotal + pLimit - 1) / pLimit}" />

            <div class="summary">
                <c:set var="fromIdx" value="${pOffset + 1}" />
                <c:set var="toIdx"   value="${pOffset + reviews.size()}" />
            </div>

            <!-- Paginación -->
            <c:if test="${totalPages > 1}">
                <nav class="pagination" aria-label="Reviews pagination">
                    <!-- Prev -->
                    <c:set var="prevOffset" value="${pOffset - pLimit}" />
                    <c:choose>
                        <c:when test="${prevOffset >= 0}">
                            <c:url var="prevUrl" value="/users/${targetUser.id}/reviews">
                                <c:param name="sort"   value="${sort}"/>
                                <c:param name="limit"  value="${pLimit}"/>
                                <c:param name="offset" value="${prevOffset}"/>
                            </c:url>
                            <a class="btn btn-secondary" href="${prevUrl}#reviews" rel="prev" aria-label="Anterior">&laquo;</a>
                        </c:when>
                        <c:otherwise>
                            <span class="btn btn-secondary" aria-disabled="true">&laquo;</span>
                        </c:otherwise>
                    </c:choose>

                    <!-- Ventana de páginas (máx 7) -->
                    <c:set var="startPage" value="${currentPage - 3}" />
                    <c:if test="${startPage < 1}"><c:set var="startPage" value="1" /></c:if>
                    <c:set var="endPage" value="${startPage + 6}" />
                    <c:if test="${endPage > totalPages}"><c:set var="endPage" value="${totalPages}" /></c:if>
                    <c:if test="${endPage - startPage < 6}">
                        <c:set var="startPage" value="${endPage - 6}" />
                        <c:if test="${startPage < 1}"><c:set var="startPage" value="1" /></c:if>
                    </c:if>

                    <c:forEach var="pg" begin="${startPage}" end="${endPage}">
                        <c:set var="pageOffset" value="${(pg - 1) * pLimit}" />
                        <c:choose>
                            <c:when test="${pg == currentPage}">
                                <span class="btn btn-primary" aria-current="page">${pg}</span>
                            </c:when>
                            <c:otherwise>
                                <c:url var="pageUrl" value="/users/${targetUser.id}/reviews">
                                    <c:param name="sort"   value="${sort}"/>
                                    <c:param name="limit"  value="${pLimit}"/>
                                    <c:param name="offset" value="${pageOffset}"/>
                                </c:url>
                                <a class="btn btn-secondary" href="${pageUrl}#reviews">${pg}</a>
                            </c:otherwise>
                        </c:choose>
                    </c:forEach>

                    <!-- Next -->
                    <c:set var="nextOffset" value="${pOffset + pLimit}" />
                    <c:choose>
                        <c:when test="${nextOffset < pTotal}">
                            <c:url var="nextUrl" value="/users/${targetUser.id}/reviews">
                                <c:param name="sort"   value="${sort}"/>
                                <c:param name="limit"  value="${pLimit}"/>
                                <c:param name="offset" value="${nextOffset}"/>
                            </c:url>
                            <a class="btn btn-secondary" href="${nextUrl}#reviews" rel="next" aria-label="Siguiente">&raquo;</a>
                        </c:when>
                        <c:otherwise>
                            <span class="btn btn-secondary" aria-disabled="true">&raquo;</span>
                        </c:otherwise>
                    </c:choose>
                </nav>
            </c:if>

        </c:when>
        <c:otherwise>
            <div class="review-item" style="text-align:center;">
                <spring:message code="reviews.empty" text="Todavía no hay reseñas."/>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<!-- Lightbox mínimo -->
<script>
    function openLightbox(src){
        const lb = document.createElement('div');
        lb.style.position='fixed';lb.style.inset='0';lb.style.background='rgba(0,0,0,.9)';
        lb.style.display='flex';lb.style.alignItems='center';lb.style.justifyContent='center';
        lb.style.zIndex='10000';lb.style.cursor='pointer';
        lb.onclick=()=>lb.remove();
        const img = document.createElement('img');
        img.src = src; img.style.maxWidth='90%'; img.style.maxHeight='90%'; img.style.borderRadius='12px';
        lb.appendChild(img);
        document.body.appendChild(lb);
    }
</script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/js/all.min.js" defer></script>
</body>
</html>
