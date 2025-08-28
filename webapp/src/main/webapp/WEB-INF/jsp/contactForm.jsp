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
    <t:card size="card-xl" cssClass="contact-card">
        <div class="contact-title">Contacto con ${bolicheName}</div>
        <form id="contactForm" action="#" method="post">
            <input type="hidden" name="bolichId" value="${bolichId}">

            <div class="form-group">
                <label for="name">Nombre completo:</label>
                <input type="text" id="name" name="name" class="form-control" required>
            </div>

            <div class="form-group">
                <label for="email">Correo electrónico:</label>
                <input type="email" id="email" name="email" class="form-control" required>
            </div>

            <div class="form-group">
                <label for="phone">Teléfono:</label>
                <input type="tel" id="phone" name="phone" class="form-control">
            </div>

            <div class="form-group">
                <label for="subject">Asunto:</label>
                <input type="text" id="subject" name="subject" class="form-control" required>
            </div>

            <div class="form-group">
                <label for="message">Mensaje:</label>
                <textarea id="message" name="message" class="form-control" required></textarea>
            </div>

            <div class="contact-buttons">
                <button type="button" class="btn-secondary" onclick="window.history.back()">Cancelar</button>
                <button type="submit" class="btn-primary">Enviar mensaje</button>
            </div>
        </form>
    </t:card>

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