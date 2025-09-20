<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <title>Añadir Boliche</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/contactForm.css' />" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <meta charset="UTF-8">
</head>
<body>
<c:set var="showRightButtons" value="false" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<div class="contact-page">
    <t:card size="card-lg" cssClass="contact-card">
        <div class="contact-title">
            Añadir Boliche
            <span class="contact-icon"><i class="fa-solid fa-martini-glass-citrus"></i></span>
        </div>
        <form:form modelAttribute="clubForm" method="post" action="${pageContext.request.contextPath}${postPath}" enctype="multipart/form-data">
            <div class="form-group">
                <form:input path="name" class="form-control" placeholder="Nombre"/>
            </div>
            <form:errors path="name" cssClass="error" element="div"/>
            <div class="form-group">
                <form:input path="location" class="form-control" placeholder="Ubicación"/>
            </div>
            <form:errors path="location" cssClass="error" element="div"/>
            <div class="form-group">
                <form:select path="priceRange" class="form-control">
                    <form:option value="" label="Seleccione un rango de precio"/>
                    <form:options items="${priceValues}" itemValue="name" itemLabel="label"/>
                </form:select>
            </div>
            <form:errors path="priceRange" cssClass="error" element="div"/>
            <div class="form-group">
                <form:textarea path="description" class="form-control" placeholder="Descripción"/>
            </div>
            <form:errors path="description" cssClass="error" element="div"/>
            <div class="form-group">
                <form:select path="musicGenre" class="form-control">
                    <form:option value="" label="Seleccione un genero musical"/>
                    <form:options items="${genreValues}" itemValue="name" itemLabel="label"/>
                </form:select>
            </div>
            <form:errors path="musicGenre" cssClass="error" element="div"/>
            <div class="form-group">
                <form:select path="zone" class="form-control" id="zone">
                    <form:option value="" label="Seleccione una zona"/>
                    <form:options items="${zoneValues}" itemValue="dbValue" itemLabel="displayName"/>
                </form:select>
            </div>
            <form:errors path="zone" cssClass="error" element="div"/>
            <div class="form-group">
                <input type="file" name="image" class="form-control" accept="image/*"/>
            </div>
            <form:errors path="image" cssClass="error" element="div"/>
            <div class="contact-buttons">
                <button type="button" class="btn btn-secondary" onclick="window.history.back()">Cancelar</button>
                <button type="submit" class="btn btn-success">Guardar</button>
            </div>
        </form:form>
    </t:card>
</div>
</body>
</html>