<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Bling Bling</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title">Bling <br>&nbsp;&nbsp;Bling</div>
    </a>
</div>

<div class="boliches-wrapper">
    <div class="app-boliches">
        <c:forEach var="club" items="${clubs}">
            <a class="boliche-link" href="<c:url value='/clubs/${club.clubId}' />">
                <t:card size="card-lg" cssClass="boliche-card">
                    <div class="card-content">
                        <div class="card-left">
                            <img src="<c:url value='/resources/img/Bali-Imagen.webp' />"
                                 alt="${club.clubName}" class="card-image">
                        </div>
                        <div class="card-right">
                            <div class="card-text-title">${club.clubName}</div>
                            <span class="card-text-body">
                                <i class="fa-solid fa-location-dot"></i> ${club.location}<br>
                                <i class="fa-solid fa-star" style="color:#FFD700;"></i>
                                <i class="fa-solid fa-star" style="color:#FFD700;"></i>
                                <i class="fa-solid fa-star" style="color:#FFD700;"></i>
                                <i class="fa-solid fa-star-half-alt" style="color:#FFD700;"></i>
                                <i class="fa-regular fa-star" style="color:#bab8b8;"></i>
                                    ${club.rating}<br>
                                ${club.description}
                            </span>
                        </div>
                    </div>
                </t:card>
            </a>
        </c:forEach>
    </div>
</div>
</body>
</html>
