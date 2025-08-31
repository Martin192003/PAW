<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/details.css' />" />
    <title>Contacto Verificado</title>
</head>
<body>
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title">Bling <br>&nbsp;&nbsp;Bling</div>
    </a>
</div>

<div class="detail-page">
    <div class="detail-header">
        <h1 class="detail-title">¡Gracias por validar tu contacto!</h1>
    </div>

    <div class="detail-content">
        <p>Tu reserva está siendo procesada por el boliche. Te contactaremos pronto.</p>
        <div class="detail-buttons">
            <a href="<c:url value='/clubs' />" class="btn btn-success">Volver a la página principal</a>
        </div>
    </div>
</div>
</body>
</html>