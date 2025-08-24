<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
<link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
<meta charset="UTF-8">

<!DOCTYPE html>
<html>
<body>
    <div class="app-title">
        Bling <br>
        &nbsp;&nbsp;Bling
    </div>
    <div class="app-boliches">
        <t:card size="card-lg">
            <div class="card-content">
                <div class="card-left">
                    <img src="<c:url value='/resources/img/Bali-Imagen.webp' />" alt="Bali Boliche" class="card-image">
                </div>
            </div>
        </t:card>
    </div>

</body>
</html>