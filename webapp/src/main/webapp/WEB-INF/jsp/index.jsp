
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<link rel="stylesheet" href="<c:url value='/css/buttons.css' />" />
<link rel="stylesheet" href="<c:url value='/css/input.css' />" />

<!DOCTYPE html>
<html>
<body>
<h2>"Hello World!"</h2>
<form onsubmit="return validateForm()">
    <t:input name="email" label="Correo electronico" type="email" placeholder="usuario@ejemplo.com" required="true" error="${requestScope.errorEmail}" />
    <t:button text="Click me" size="btn-lg" cssClass="btn-success" disabled="false" />
    <t:button text="Primary" size="btn-md" cssClass="btn-primary" disabled="false" />
    <t:button text="Success Large" size="btn-lg" cssClass="btn-success" disabled="false" />
    <t:button text="Danger Small" size="btn-sm" cssClass="btn-danger" disabled="false" />
    <t:button text="Warning Disabled" size="btn-md" cssClass="btn-warning" disabled="true" />
    <t:button text="Info" size="btn-md" cssClass="btn-info" disabled="false" />
    <t:button text="Default" disabled="false" /></form>

<script>
    function validateForm() {
        const email = document.querySelector('[name="email"]').value;
        const emailPattern = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;
        const errorDiv = document.getElementById('formError');
        errorDiv.textContent = '';
        if (!emailPattern.test(email)) {
            errorDiv.textContent = "Ingrese un correo electrónico válido.";
            return false;
        }
        return true;
    }
</script>

</body>
</html>
