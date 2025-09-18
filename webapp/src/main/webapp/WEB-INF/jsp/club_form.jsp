<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <title><spring:message code="title.addClub"/></title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/clubForm.css' />" />
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <meta charset="UTF-8">
</head>
<body>
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title"><spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/></div>
    </a>
</div>
<div class="club-form-page">
    <t:card size="card-lg" cssClass="club-form-card">
        <div class="club-form-title">
            <spring:message code="title.addClub"/>
            <span class="club-form-icon"><i class="fa-solid fa-martini-glass-citrus"></i></span>
        </div>
        <spring:message code="placeholder.name" var="phName"/>
        <spring:message code="placeholder.location" var="phLocation"/>
        <spring:message code="placeholder.description" var="phDescription"/>
        <spring:message code="form.priceRange" var="phPriceRange"/>
        <spring:message code="form.musicGenre" var="phMusicGenre"/>
        <spring:message code="form.zone" var="phZone"/>

        <form:form modelAttribute="clubForm" method="post" action="${pageContext.request.contextPath}${postPath}" enctype="multipart/form-data">
            <span class="form-text-required"><spring:message code="form.required"/></span>
            <div class="form-group">
                <form:input path="name" class="form-control" placeholder="${phName}"/>
                <form:errors path="name" cssClass="error" element="div"/>
            </div>
            <div class="form-group">
                <form:input path="location" class="form-control" placeholder="${phLocation}"/>
                <form:errors path="location" cssClass="error" element="div"/>
            </div>
            <div class="form-group">
                <form:select path="priceRange" class="form-control">
                    <form:option value="" label="${phPriceRange}"/>
                    <form:options items="${priceValues}" itemValue="name" itemLabel="label"/>
                </form:select>
                <form:errors path="priceRange" cssClass="error"/>
            </div>
            <div class="form-group">
                <form:textarea path="description" class="form-control" placeholder="${phDescription}"/>
                <form:errors path="description" cssClass="error" element="div"/>
            </div>
            <div class="form-group">
                <form:select path="musicGenre" class="form-control">
                    <form:option value="" label="${phMusicGenre}"/>
                    <form:options items="${musicGenres}" itemValue="name" itemLabel="label"/>
                </form:select>
                <form:errors path="musicGenre" cssClass="error" element="div"/>
            </div>
            <div class="form-group">
                <form:select path="zone" class="form-control">
                    <form:option value="" label="${phZone}"/>
                    <form:options items="${zones}" itemValue="dbValue" itemLabel="displayName"/>
                </form:select>
                <form:errors path="zone" cssClass="error" element="div"/>
            </div>
            <div class="form-group">
                <form:input path="image" type="file" class="form-control"/>
                <form:errors path="image" cssClass="error" element="div"/>
            </div>
            <div class="club-form-buttons">
                <a href="<c:url value='/clubs' />" class="btn btn-secondary">
                    <spring:message code="button.cancel"/>
                </a>
                <button type="submit" class="btn btn-success">
                    <spring:message code="button.save"/>
                </button>
            </div>
        </form:form>
    </t:card>
</div>
</body>
</html>
