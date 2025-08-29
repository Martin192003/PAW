<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/contactForm.css' />" />
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
    <div class="contact-page">
        <t:card size="card-lg" cssClass="contact-card">
            <div class="contact-title">
                    Contactate con ${clubName}
                    <span class="contact-icon">
                        <i class="fa-solid fa-headphones"></i>
                    </span>
            </div>
            <form id="contactForm" action="#" method="post">
                <input type="hidden" name="clubId" value="${clubId}">

                <div class="form-group">
                    <input type="text" id="name" name="name" class="form-control" placeholder="Nombre completo" required>
                </div>

                <div class="form-group">
                    <input type="email" id="email" name="email" class="form-control" placeholder="Correo electrónico" required>
                </div>

                <div class="form-group">
                    <input type="tel" id="phone" name="phone" class="form-control" placeholder="Teléfono">
                </div>

                <div class="form-group">
                    <textarea id="message" name="message" class="form-control" placeholder="Mensaje" required></textarea>
                </div>

                <div class="contact-buttons">
                    <button type="button" class="btn btn-secondary" onclick="window.history.back()">Cancelar</button>
                    <button type="submit" class="btn btn-success">Enviar mensaje</button>
                </div>
            </form>
        </t:card>
    </div>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const contactForm = document.getElementById('contactForm');

            contactForm.addEventListener('submit', function(e) {
                e.preventDefault();

                alert('Mensaje enviado correctamente. (Simulación)');
                window.history.back();
            });
        });
    </script>
</body>
</html>