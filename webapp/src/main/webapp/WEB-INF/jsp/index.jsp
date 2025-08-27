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
    <div class="app-title">
        Bling <br>
        &nbsp;&nbsp;Bling
    </div>
</div>
<div class="app-boliches">
    <t:card size="card-lg" cssClass="boliche-card" dataId="1">
        <div class="card-content">
            <div class="card-left">
                <img src="<c:url value='/resources/img/Bali-Imagen.webp' />" alt="Bali Boliche" class="card-image">
            </div>
            <div class="card-right">
                <div class="card-text-title">
                    BALI<br>
                </div>
                <span class="card-text-body">
                        <i class="fa-solid fa-location-dot"></i>
                        CABA-Costanera <br>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star-half-alt" style="color: #FFD700;"></i>
                        <i class="fa-regular fa-star" style="color: #bab8b8;"></i>
                        3.7 (258 reseñas)<br>
                        Descubre Bali Costanera, el mejor boliche en Buenos Aires para disfrutar de música y diversión nocturna con tus amigos.<br>
                    </span>
            </div>
        </div>
    </t:card>
    <t:card size="card-lg" cssClass="boliche-card" dataId="2">
        <div class="card-content">
            <div class="card-left">
                <img src="<c:url value='/resources/img/Bali-Imagen.webp' />" alt="Club One" class="card-image">
            </div>
            <div class="card-right">
                <div class="card-text-title">
                    CLUB ONE<br>
                </div>
                <span class="card-text-body">
                        <i class="fa-solid fa-location-dot"></i>
                        CABA-Palermo <br>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-regular fa-star-half-alt" style="color: #FFD700;"></i>
                        4.2 (176 reseñas)<br>
                        Club One es el destino perfecto para los amantes de la música electrónica en Buenos Aires.<br>
                    </span>
            </div>
        </div>
    </t:card>
    <t:card size="card-lg" cssClass="boliche-card" dataId="3">
        <div class="card-content">
            <div class="card-left">
                <img src="<c:url value='/resources/img/Bali-Imagen.webp' />" alt="Groove" class="card-image">
            </div>
            <div class="card-right">
                <div class="card-text-title">
                    GROOVE<br>
                </div>
                <span class="card-text-body">
                        <i class="fa-solid fa-location-dot"></i>
                        CABA-Palermo <br>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-regular fa-star" style="color: #bab8b8;"></i>
                        4.0 (320 reseñas)<br>
                        Groove ofrece la mejor experiencia para los amantes de la música en vivo y el buen ambiente.<br>
                    </span>
            </div>
        </div>
    </t:card>
    <t:card size="card-lg" cssClass="boliche-card" dataId="4">
        <div class="card-content">
            <div class="card-left">
                <img src="<c:url value='/resources/img/Bali-Imagen.webp' />" alt="Bahrein" class="card-image">
            </div>
            <div class="card-right">
                <div class="card-text-title">
                    BAHREIN<br>
                </div>
                <span class="card-text-body">
                        <i class="fa-solid fa-location-dot"></i>
                        CABA-Centro <br>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-regular fa-star" style="color: #bab8b8;"></i>
                        4.1 (215 reseñas)<br>
                        Bahrein es un clásico de la noche porteña, con los mejores DJ de música electrónica.<br>
                    </span>
            </div>
        </div>
    </t:card>
    <t:card size="card-lg" cssClass="boliche-card" dataId="5">
        <div class="card-content">
            <div class="card-left">
                <img src="<c:url value='/resources/img/Bali-Imagen.webp' />" alt="Rose in Rio" class="card-image">
            </div>
            <div class="card-right">
                <div class="card-text-title">
                    ROSE IN RIO<br>
                </div>
                <span class="card-text-body">
                        <i class="fa-solid fa-location-dot"></i>
                        CABA-Costanera <br>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-solid fa-star" style="color: #FFD700;"></i>
                        <i class="fa-regular fa-star-half-alt" style="color: #FFD700;"></i>
                        4.3 (280 reseñas)<br>
                        Rose in Rio ofrece una experiencia única con vistas al río y la mejor selección musical.<br>
                    </span>
            </div>
        </div>
    </t:card>
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