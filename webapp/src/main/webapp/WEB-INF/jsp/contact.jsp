<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/contactForm.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <meta charset="UTF-8">
</head>
<body>
<c:set var="showAddClubButton" value="false" />
<c:set var="showProfileButton" value="false" />
<c:set var="showRightButtons" value="true" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>

<div class="contact-page">
    <t:card size="card-lg" cssClass="contact-card">
        <div class="contact-title">
            <c:set var="clubNameEsc"><c:out value='${clubName}'/></c:set>
            <spring:message code="contact.title" arguments="${clubNameEsc}"/>
            <span class="contact-icon"><i class="fa-solid fa-headphones"></i></span>
        </div>

        <spring:message code="form.fullname" var="phName"/>
        <spring:message code="placeholder.email" var="phEmail"/>
        <spring:message code="placeholder.phone" var="phPhone"/>
        <spring:message code="placeholder.message" var="phMessage"/>
        <spring:message code="button.sending" var="msgSending"/>

        <!-- RUTAS/ACCION: NO CAMBIADAS -->
        <form:form modelAttribute="clubForm" method="post" id="contactForm" autocomplete="off">
            <span class="form-text-required"><spring:message code="form.required"/></span>

            <div class="form-group">
                <form:input path="name" class="form-control" placeholder="${phName}" />
                <form:errors path="name" cssClass="error" element="div"/>
            </div>

            <div class="form-group">
                <form:input path="email" class="form-control" placeholder="${phEmail}" />
                <form:errors path="email" cssClass="error" element="div"/>
            </div>

            <div class="form-group">
                <form:input path="phone" class="form-control" placeholder="${phPhone}"/>
                <form:errors path="phone" cssClass="error" element="div"/>
            </div>

            <div class="form-group">
                <form:textarea path="message" class="form-control" placeholder="${phMessage}" />
                <form:errors path="message" cssClass="error" element="div"/>
            </div>

            <div class="contact-buttons">
                <button type="button" class="btn btn-secondary" onclick="window.history.back()">
                    <spring:message code="button.cancel"/>
                </button>
                <button id="sendBtn" type="submit" class="btn btn-success">
                    <i class="fa-solid fa-paper-plane"></i> <spring:message code="button.send"/>
                </button>
            </div>
        </form:form>
    </t:card>
</div>

<!-- Toast de envío -->
<div id="sentToast" class="toast-bottom-right">
    <div class="toast-content">
        <i class="fa-solid fa-circle-check"></i>
        <span><spring:message code="contact.sent"/></span>
    </div>
</div>
<script>
    document.addEventListener('DOMContentLoaded', () => {
        // --- estética del título (sin cambios de rutas) ---
        function breakEveryN(str, n) {
            if (!str) return '';
            const s = String(str).trim();
            if (s.length <= n) return s;
            const parts = [];
            for (let i = 0; i < s.length; i += n) parts.push(s.slice(i, i + n));
            return parts.join('\n');
        }
        const titleEl = document.querySelector('.contact-title');
        if (titleEl && !titleEl.dataset.broken) {
            titleEl.innerText = breakEveryN(titleEl.textContent, 30);
            titleEl.dataset.broken = '1';
        }

        // ========= BLOQUEO DE ENVÍO (mismo estilo que el JSP de reservas) =========
        const form = document.getElementById('contactForm');
        const sendBtn = document.getElementById('sendBtn');
        const SENDING = "<c:out value='${msgSending}'/>";

        function cloneHidden(name, value) {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = name;
            input.value = value != null ? value : '';
            form.appendChild(input);
            return input;
        }

        if (form && sendBtn) {
            let submitting = false;

            form.addEventListener('submit', (e) => {
                if (submitting) { e.preventDefault(); return; }

                if (!form.checkValidity()) return;

                const nameEl    = form.querySelector('[name="name"]');
                const emailEl   = form.querySelector('[name="email"]');
                const phoneEl   = form.querySelector('[name="phone"]');
                const messageEl = form.querySelector('[name="message"]');

                const hName    = cloneHidden('name',    nameEl ? nameEl.value    : '');
                const hEmail   = cloneHidden('email',   emailEl ? emailEl.value   : '');
                const hPhone   = cloneHidden('phone',   phoneEl ? phoneEl.value   : '');
                const hMessage = cloneHidden('message', messageEl ? messageEl.value : '');

                submitting = true;
                form.dataset.submitting = '1';

                sendBtn.disabled = true;
                sendBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> ' + (SENDING || '');

                form.querySelectorAll('input, textarea, select, button').forEach(el => {
                    const keep = (el === hName || el === hEmail || el === hPhone || el === hMessage);
                    if (!keep) el.disabled = true;
                });
            });

            window.addEventListener('pageshow', (evt) => {
                if (evt.persisted) {
                    submitting = false;
                    form.dataset.submitting = '0';
                    form.querySelectorAll('input, textarea, select, button').forEach(el => {
                        // no tocar hiddens extra antiguos; el server ya los ignorará en GET
                        if (el.type !== 'hidden') el.disabled = false;
                    });
                    sendBtn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> ' + "<spring:message code='button.send'/>";
                }
            });
        }
    });

    var sent = ${sent ? 'true' : 'false'};
    if (sent) {
        document.addEventListener("DOMContentLoaded", function() {
            const toast = document.getElementById("sentToast");
            toast.classList.add("show");

            // Ocultar después de 2.5 segundos
            setTimeout(() => {
                toast.classList.remove("show");
            }, 2500);
        });
    }

    function volverADetalles() {
        window.location.href = "<c:url value='/clubs/${clubId}' />";
    }

    // Bloquea el símbolo "-" en el input de teléfono del formulario de contacto
    document.addEventListener('DOMContentLoaded', function() {
        var phoneInput = document.querySelector('#contactForm input[name="phone"]');
        if (phoneInput) {
            phoneInput.addEventListener('keydown', function(e) {
                if (e.key === '-' || e.key === 'Subtract') {
                    e.preventDefault();
                }
            });
            phoneInput.addEventListener('input', function(e) {
                if (this.value.includes('-')) {
                    this.value = this.value.replace(/-/g, '');
                }
            });
        }
    });

</script>
</body>
</html>
