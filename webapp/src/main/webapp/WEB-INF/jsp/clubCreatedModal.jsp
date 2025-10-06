<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
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
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">    <title><spring:message code="modalTitle.clubCreatedSuccessfully"/></title>
    <link rel="stylesheet" href="<c:url value='/resources/css/clubCreatedModal.css'/>">
    <meta charset="UTF-8">
</head>
<body>
<div class="modal-page">
    <div class="modal" style="display:flex;">
        <div class="modal-content">
            <h2><spring:message code="modalTitle.clubCreatedSuccessfully"/></h2>
            <div class="modal-body">
                <p><spring:message code="modalTitle.clubConfirmation"/></p>
            </div>
            <div class="modal-actions" style="text-align:center; margin-top:20px;">
                <form action="<c:url value='/clubs/confirmClub'/>" method="post">
                    <button type="submit" class="btn btn-success"><spring:message code="modalTitle.continue"/></button>
                </form>
            </div>
        </div>
    </div>
</div>
</body>
</html>
