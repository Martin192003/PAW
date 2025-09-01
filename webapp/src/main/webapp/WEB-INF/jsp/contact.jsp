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

<!DOCTYPE html>
<html>
<body>
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title">Bling <br>&nbsp;&nbsp;Bling</div>
    </a>
</div>

<div class="contact-page">
    <t:card size="card-lg" cssClass="contact-card">
        <div class="contact-title">
            Contactate con ${clubName}
            <span class="contact-icon"><i class="fa-solid fa-headphones"></i></span>
        </div>

        <form id="contactForm" action="<c:url value='/contact'/>" method="post">
            <input type="hidden" name="clubId" value="${clubId}">
            <div class="form-group">
                <input type="text" name="name" class="form-control" placeholder="Nombre completo" required>
            </div>
            <div class="form-group">
                <input type="email" name="email" class="form-control" placeholder="Correo electrónico" required>
            </div>
            <div class="form-group">
                <input type="tel" name="phone" class="form-control" placeholder="Teléfono">
            </div>
            <div class="form-group">
                <textarea name="message" class="form-control" placeholder="Mensaje" required></textarea>
            </div>
            <div class="contact-buttons">
                <button type="button" class="btn btn-secondary" onclick="window.history.back()">Cancelar</button>
                <button type="submit" class="btn btn-success">Enviar mensaje</button>
            </div>
        </form>
    </t:card>
</div>
<div id="sentModal" style="display:none; position:fixed; top:0; left:0; width:100vw; height:100vh; background:rgba(0,0,0,0.5); z-index:9999; justify-content:center; align-items:center;">
    <div style="background:#222; color:#fff; padding:32px; border-radius:12px; box-shadow:0 4px 24px rgba(0,0,0,0.6); width:400px; text-align:center;">
        <div style="font-size:22px; margin-bottom:18px;">Tu mensaje fue enviado. ¡Gracias!</div>
        <button class="btn btn-success" onclick="volverADetalles()">Volver</button>
    </div>
</div>
<script>
    var sent = ${sent ? 'true' : 'false'};
    if (sent) {
        document.addEventListener("DOMContentLoaded", function() {
            document.getElementById("sentModal").style.display = "flex";
        });
    }
    function volverADetalles() {
        window.location.href = "<c:url value='/clubs/${clubId}' />";
    }
</script>
</body>
</html>
