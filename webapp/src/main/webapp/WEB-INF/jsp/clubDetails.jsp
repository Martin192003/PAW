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

<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title">
            Bling <br>&nbsp;&nbsp;Bling
        </div>
    </a>
</div>

<div class="detail-page">
    <div style="margin-bottom: 0px; margin-left: 18px;">
        <button type="button" class="btn btn-success"
                onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
    </div>
    <div class="detail-header" style="display: flex; flex-direction: row; justify-content: space-between; align-items: center; width: 100%;">
        <h1 class="detail-title">${club.clubName}</h1>
        <button type="button" class="btn btn-success"
                onclick="window.location.href='<c:url value="/contact"><c:param name="id" value="${club.clubId}"/></c:url>'">
            <i class="fa-solid fa-envelope"></i> Contactarse
        </button>
    </div>


    <div class="detail-content">
        <img src="<c:url value='/resources/img/Bali-Imagen.webp' />"
             alt="<c:out value='${club.clubName}'/>" class="detail-image">

        <div class="detail-info">
            <div class="detail-details">
                <span>${locationText}</span>
                <span>${genreText}</span>
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

        <div class="detail-buttons">
            <div class="dropdown">
                <button class="dropdown-btn btn btn-success">
                    Carta <i class="fa-solid fa-chevron-down"></i>
                </button>
                <div class="dropdown-content">
                    <div class="dropdown-content-grid">
                        <c:choose>
                            <c:when test="${empty productsByCategory}">
                                <p style="padding:12px;">No hay productos.</p>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="entry" items="${productsByCategory}">
                                    <div class="drink-category">
                                        <h3><c:out value="${entry.key}"/></h3>
                                        <ul>
                                            <c:forEach var="p" items="${entry.value}">
                                                <li>
                                                    <span class="drink-name">
                                                        <c:out value="${p.productName}"/>
                                                    </span>
                                                    <span class="drink-price">
                                                          <c:choose>
                                                              <c:when test="${not empty p.basePrice}">
                                                                  $<fmt:formatNumber value="${p.basePrice}" type="number" minFractionDigits="0" maxFractionDigits="0"/>
                                                              </c:when>
                                                              <c:otherwise>—</c:otherwise>
                                                          </c:choose>
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
            </div>
        </div>

        <script>
            document.addEventListener('DOMContentLoaded', function() {
                const btn = document.querySelector('.dropdown-btn');
                const content = document.querySelector('.dropdown-content');
                btn.addEventListener('click', function() {
                    content.classList.toggle('show');
                    this.classList.toggle('active');
                });
                window.addEventListener('click', function(e) {
                    if (!e.target.closest('.dropdown')) {
                        content.classList.remove('show');
                        btn.classList.remove('active');
                    }
                });
            });
        </script>
    </div>
</div>
</body>
</html>
