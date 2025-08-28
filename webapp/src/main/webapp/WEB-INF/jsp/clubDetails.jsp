<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/details.css' />" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <meta charset="UTF-8">
    <title>${club.title} - Detalles</title>
</head>
<body>
<div class="top-bar">
    <a href="<c:url value='/' />" class="logo-link">
        <div class="app-title">
            Bling <br>
            &nbsp;&nbsp;Bling
        </div>
    </a>
</div>

<div class="detail-page">
    <div class="detail-header">
        <h1 class="detail-title">${club.title}</h1>
        <t:button text="Contactarse" cssClass="btn-success" icon="fa-solid fa-envelope" />
    </div>

    <div class="detail-content">
        <img src="<c:url value='${club.image}' />" alt="Imagen del boliche" class="detail-image">
        <div class="detail-info">
            <div class="detail-details">
                <div>
                    <i class="fa-solid fa-location-dot"></i>
                    <span>${club.location}</span>
                </div>
                <div>
                    <i class="fa-solid fa-music"></i>
                    <span>${club.musicType}</span>
                </div>
                <div>
                    <i class="fa-solid fa-clock"></i>
                    <span>${club.hours}</span>
                </div>
                <div>
                    <i class="fa-solid fa-star"></i>
                    <span>${club.rating}</span>
                </div>
            </div>
            <div class="detail-details">
                <div>
                    <i class="fa-solid fa-money-bill"></i>
                    <span>${club.price}</span>
                </div>
                <div>
                    <i class="fa-solid fa-users"></i>
                    <span>${club.audience}</span>
                </div>
                <div>
                    <i class="fa-solid fa-globe"></i>
                    <a href="https://${club.website}" target="_blank">${club.website}</a>
                </div>
                <div>
                    <i class="fa-solid fa-phone"></i>
                    <span>${club.phone}</span>
                </div>
            </div>
        </div>

        <div class="detail-description">
            <p>${club.description}</p>
        </div>

        <div class="detail-buttons">
            <div class="dropdown">
                <button class="dropdown-btn btn btn-success">
                    Carta <i class="fa-solid fa-chevron-down"></i>
                </button>
                <div class="dropdown-content">
                    <div class="dropdown-content-grid">
                        <div class="drink-category">
                            <h3>Cervezas</h3>
                            <ul>
                                <li><span class="drink-name">Corona</span> <span class="drink-price">$1200</span></li>
                                <li><span class="drink-name">Stella Artois</span> <span class="drink-price">$1100</span></li>
                                <li><span class="drink-name">Heineken</span> <span class="drink-price">$1300</span></li>
                            </ul>
                        </div>
                        <div class="drink-category">
                            <h3>Tragos</h3>
                            <ul>
                                <li><span class="drink-name">Fernet con Coca</span> <span class="drink-price">$1500</span></li>
                                <li><span class="drink-name">Cuba Libre</span> <span class="drink-price">$1400</span></li>
                                <li><span class="drink-name">Gin Tonic</span> <span class="drink-price">$1600</span></li>
                                <li><span class="drink-name">Mojito</span> <span class="drink-price">$1700</span></li>
                            </ul>
                        </div>
                        <div class="drink-category">
                            <h3>Champagne</h3>
                            <ul>
                                <li><span class="drink-name">Chandon</span> <span class="drink-price">$8000</span></li>
                                <li><span class="drink-name">Baron B</span> <span class="drink-price">$12000</span></li>
                            </ul>
                        </div>
                        <div class="drink-category">
                            <h3>Sin Alcohol</h3>
                            <ul>
                                <li><span class="drink-name">Agua Mineral</span> <span class="drink-price">$800</span></li>
                                <li><span class="drink-name">Gaseosa</span> <span class="drink-price">$900</span></li>
                                <li><span class="drink-name">Red Bull</span> <span class="drink-price">$1100</span></li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <script>
            document.addEventListener('DOMContentLoaded', function() {
                const dropdownBtn = document.querySelector('.dropdown-btn');
                const dropdownContent = document.querySelector('.dropdown-content');

                dropdownBtn.addEventListener('click', function() {
                    dropdownContent.classList.toggle('show');
                    this.classList.toggle('active');
                });

                // Cerrar el dropdown si se hace clic fuera de él
                window.addEventListener('click', function(event) {
                    if (!event.target.matches('.dropdown-btn') &&
                        !event.target.matches('.dropdown-btn *') &&
                        !event.target.matches('.dropdown-content') &&
                        !event.target.matches('.dropdown-content *')) {

                        if (dropdownContent.classList.contains('show')) {
                            dropdownContent.classList.remove('show');
                            dropdownBtn.classList.remove('active');
                        }
                    }
                });
            });
        </script>
    </div>
</div>
</body>
</html>