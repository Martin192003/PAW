<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
<meta charset="UTF-8">

<!DOCTYPE html>
<html>
<body>
<div class="top-bar">
    <a href="<c:url value='/' />" class="logo-link">
        <div class="app-title">
            Bling <br>
            &nbsp;&nbsp;Bling
        </div>
    </a>
</div>
<div class="app-boliches">
  <c:forEach var="club" items="${clubs}">
    <t:card size="card-lg" cssClass="boliche-card" dataId="${club.clubId}">
        <div class="card-content">
            <div class="card-left">
                <img src="<c:url value='/resources/img/Bali-Imagen.webp' />" alt="${club.clubName}" class="card-image">
            </div>
            <div class="card-right">
                <div class="card-text-title">
                    ${club.clubName}<br>
                </div>
                <span class="card-text-body">
                        <i class="fa-solid fa-location-dot"></i>
                        ${club.location} <br>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star-half-alt" style="color: #FFD700;"></i>
                        <i class="fa-regular fa-star" style="color: #bab8b8;"></i>
                        ${club.rating}<br>
                        ${club.description}<br>
                </span>
            </div>
        </div>
    </t:card>
  </c:forEach>
</div>


<script>
    document.addEventListener('DOMContentLoaded', function() {
        const bolichesCards = document.querySelectorAll('.boliche-card');

        bolichesCards.forEach(card => {
            card.addEventListener('click', function() {
                const bolichId = this.getAttribute('data-id');
                if (bolichId) {
                    // Redirección a la pantalla
                    window.location.href = 'boliche?id=' + bolichId;
                } else {
                    console.error("No se encontró data-id en la tarjeta");
                }
            });
        });
    });
</script>
</body>
</html>