<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<link rel="stylesheet" href="<c:url value='/css/buttons.css' />" />
<link rel="stylesheet" href="<c:url value='/css/input.css' />" />
<link rel="stylesheet" href="<c:url value='/css/card.css' />" />
<link rel="stylesheet" href="<c:url value='/css/text.css' />" />
<link rel="stylesheet" href="<c:url value='/css/index.css' />" />
<meta charset="UTF-8">

<!DOCTYPE html>
<html>
<head>
    <title>Componentes Personalizados</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body>
<div class="header">
    <h1>Biblioteca de Componentes Personalizados</h1>
</div>

<div class="section">
    <h2>Entrada</h2>

    <div class="form-demo">
        <form id="demoForm" onsubmit="return validateForm()" novalidate>
            <t:input name="email" label="Correo Electronico" type="email"
                     placeholder="usuario@ejemplo.com" required="true" />

            <t:input name="username" label="Nombre de Usuario"
                     placeholder="Ingrese nombre de usuario" required="true" />

            <t:input name="password" label="Contrasena" type="password"
                     placeholder="Ingrese contrasena" required="true" />

            <t:input name="phone" label="Numero de Telefono" type="tel"
                     placeholder="1234-5678" pattern="[0-9]{3}-[0-9]{3}-[0-9]{4}" />

            <div id="formError"></div>

            <div class="component-group">
                <t:button text="Enviar" cssClass="btn-success" type="submit" />
                <t:button text="Restablecer" cssClass="btn-secondary" type="reset" />
            </div>

        </form>
    </div>
</div>

<div class="section">
    <h2>Botones</h2>

    <h3>Tipos de botones</h3>
    <div class="component-group">
        <t:button text="Primario" cssClass="btn-primary" />
        <t:button text="Secundario" cssClass="btn-secondary" />
        <t:button text="Exito" cssClass="btn-success" />
        <t:button text="Peligro" cssClass="btn-danger" />
        <t:button text="Advertencia" cssClass="btn-warning" />
        <t:button text="Informacion" cssClass="btn-info" />
    </div>

    <h3>Tamano de botones</h3>
    <div class="component-group">
        <t:button text="Pequeno" size="btn-sm" cssClass="btn-primary" />
        <t:button text="Mediano" size="btn-md" cssClass="btn-primary" />
        <t:button text="Grande" size="btn-lg" cssClass="btn-primary" />
    </div>

    <h3>Botones deshabilitados</h3>
    <div class="component-group">
        <t:button text="Primario Deshabilitado" cssClass="btn-primary" disabled="true" />
        <t:button text="Exito Deshabilitado" cssClass="btn-success" disabled="true" />
        <t:button text="Peligro Deshabilitado" cssClass="btn-danger" disabled="true" />
    </div>
</div>

<div class="section">
    <h2>Texto</h2>

    <div class="text-demo">
        <h3>Tamano de texto</h3>
        <t:text text="Texto Pequeno" size="text-sm" />
        <t:text text="Texto Mediano" size="text-md" />
        <t:text text="Texto Grande" size="text-lg" />

        <h3>Tipos de texto</h3>
        <t:text text="Texto Primario" cssClass="text-primary" />
        <t:text text="Texto Secundario" cssClass="text-secondary" />
        <t:text text="Texto Deshabilitado" disabled="true" />
    </div>
</div>

<div class="section">
    <h2>Tarjetas</h2>

    <div class="card-demo">
        <h3>Tamano de tarjetas</h3>
        <t:card text="Contenido de Tarjeta Pequena" size="card-sm" />
        <t:card text="Contenido de Tarjeta Mediana" size="card-md" />
        <t:card text="Contenido de Tarjeta Grande" size="card-lg" />

        <h3>Tipo de tarjetas</h3>
        <t:card text="Tarjeta Predeterminada" cssClass="card-default" />
        <t:card text="Tarjeta Primaria" cssClass="card-primary" />
        <t:card text="Tarjeta Secundaria" cssClass="card-secondary" />

        <h3>Tarjetas deshabilitadas</h3>
        <t:card text="Esta tarjeta esta deshabilitada" disabled="true" />
    </div>
</div>

<script>
    function clearFieldError(input) {
        input.classList.remove('is-invalid');
        const msg = input.parentNode.querySelector('.error');
        if (msg) msg.remove();
    }

    function showFieldError(input, message) {
        input.classList.add('is-invalid');
        const span = document.createElement('span');
        span.className = 'error';
        span.textContent = message;
        input.parentNode.appendChild(span);
    }


    function validateForm() {
        const form = document.getElementById('demoForm');
        form.classList.add('was-validated');

        document.querySelectorAll('.form-control').forEach(clearFieldError);

        let valid = true;

        const email = document.querySelector('[name="email"]');
        const username = document.querySelector('[name="username"]');
        const password = document.querySelector('[name="password"]');
        const phone = document.querySelector('[name="phone"]');

        const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
        const phonePattern = /^[0-9]{4}-[0-9]{4}$/;

        if (!username.value.trim()) {
            showFieldError(username, 'Por favor, ingrese un nombre de usuario.');
            valid = false;
        }
        if (!password.value.trim()) {
            showFieldError(password, 'Por favor, ingrese una contrasena.');
            valid = false;
        }
        if (!emailPattern.test(email.value)) {
            showFieldError(email, 'Por favor, ingrese un correo electronico valido.');
            valid = false;
        }
        if (phone.value.trim() && !phonePattern.test(phone.value.trim())) {
            showFieldError(phone, 'Formato invalido. Use: 123-456-7890');
            valid = false;
        }

        if (valid) {
            alert('Formulario validado correctamente!');
        }
        return false;
    }
</script>
</body>
</html>