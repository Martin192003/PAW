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
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<div class="club-form-page">
    <div class="detail-back">
        <button type="button" class="btn btn-success"
                onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
    </div>
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
        <spring:message code="form.venueType" var="phvenueType"/>
        <spring:message code="label.bar" var="phbar"/>
        <spring:message code="label.notBar" var="phclub"/>

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
                <div class="select-with-tooltip">
                    <form:select path="priceRange" id="priceRange" class="form-control" aria-describedby="tt-price">
                        <option value=""><spring:message code="form.priceRange"/></option>
                        <option value="BAJO" ${priceRange == 'BAJO' ? 'selected' : ''}>
                            <spring:message code="price.BAJO"/>
                        </option>
                        <option value="MEDIO" ${priceRange == 'MEDIO' ? 'selected' : ''}>
                            <spring:message code="price.MEDIO"/>
                        </option>
                        <option value="ALTO" ${priceRange == 'ALTO' ? 'selected' : ''}>
                            <spring:message code="price.ALTO"/>
                        </option>
                    </form:select>

                    <!-- Botón/info dentro de la "caja" -->
                    <button type="button"
                            class="info-btn"
                            aria-label="<spring:message code='form.priceRange'/>"
                            aria-haspopup="true"
                            aria-controls="tt-price"
                            aria-expanded="false">
                        <i class="fa-solid fa-circle-info" aria-hidden="true"></i>
                    </button>

                    <!-- Tooltip -->
                    <div id="tt-price" class="tooltip-content" role="tooltip">
                        <div class="tip"></div>
                        <strong><spring:message code="form.priceRange"/></strong><br/>
                        <span>
                            <spring:message code="price.BAJO"/> ($) – <spring:message code="price.drinks.low"/>.<br/>
                            <spring:message code="price.MEDIO"/> ($$) – <spring:message code="price.drinks.medium"/>.<br/>
                            <spring:message code="price.ALTO"/> ($$$) – <spring:message code="price.drinks.high"/>.
                        </span>
                    </div>
                </div>

                <form:errors path="priceRange" cssClass="error"/>
            </div>

            <div class="form-group">
                <form:textarea path="description" class="form-control" placeholder="${phDescription}"/>
                <form:errors path="description" cssClass="error" element="div"/>
            </div>
            <div class="form-group">
                <form:select path="musicGenre" class="form-control">
                    <option value=""><spring:message code="form.musicGenre"/></option>
                    <option value="ROCK" ${musicGenre == 'ROCK' ? 'selected' : ''}>
                        <spring:message code="genre.ROCK"/>
                    </option>
                    <option value="POP" ${musicGenre == 'POP' ? 'selected' : ''}>
                        <spring:message code="genre.POP"/>
                    </option>
                    <option value="CACHENGUE" ${musicGenre == 'CACHENGUE' ? 'selected' : ''}>
                        <spring:message code="genre.CACHENGUE"/>
                    </option>
                    <option value="TECHNO" ${musicGenre == 'TECHNO' ? 'selected' : ''}>
                        <spring:message code="genre.TECHNO"/>
                    </option>
                    <option value="REGGAETON" ${musicGenre == 'REGGAETON' ? 'selected' : ''}>
                        <spring:message code="genre.REGGAETON"/>
                    </option>
                    <option value="CUMBIA" ${musicGenre == 'CUMBIA' ? 'selected' : ''}>
                        <spring:message code="genre.CUMBIA"/>
                    </option>
                    <option value="TRAP" ${musicGenre == 'TRAP' ? 'selected' : ''}>
                        <spring:message code="genre.TRAP"/>
                    </option>
                    <option value="JAZZ" ${musicGenre == 'JAZZ' ? 'selected' : ''}>
                        <spring:message code="genre.JAZZ"/>
                    </option>
                    <option value="ELECTRONICA" ${musicGenre == 'ELECTRONICA' ? 'selected' : ''}>
                        <spring:message code="genre.ELECTRONICA"/>
                    </option>
                    <option value="AFROBEAT" ${musicGenre == 'AFROBEAT' ? 'selected' : ''}>
                        <spring:message code="genre.AFROBEAT"/>
                    <option value="OTRO" ${musicGenre == 'OTRO' ? 'selected' : ''}>
                        <spring:message code="genre.OTRO"/>
                    </option>
                </form:select>
                <form:errors path="musicGenre" cssClass="error" element="div"/>
            </div>
            <div class="form-group">
                <select name="zone" class="form-control">
                    <option value=""><spring:message code="form.zone"/></option>
                    <option value="ZONA_NORTE" ${zone == 'ZONA_NORTE' ? 'selected' : ''}>
                        <spring:message code="zone.ZONA_NORTE"/>
                    </option>
                    <option value="ZONA_OESTE" ${zone == 'ZONA_OESTE' ? 'selected' : ''}>
                        <spring:message code="zone.ZONA_OESTE"/>
                    </option>
                    <option value="ZONA_SUR" ${zone == 'ZONA_SUR' ? 'selected' : ''}>
                        <spring:message code="zone.ZONA_SUR"/>
                    </option>
                    <option value="ZONA_CABA" ${zone == 'ZONA_CABA' ? 'selected' : ''}>
                        <spring:message code="zone.ZONA_CABA"/>
                    </option>
                </select>
                <form:errors path="zone" cssClass="error" element="div"/>
            </div>
            <div class="form-group">
                <form:select path="isBar" class="form-control">
                    <form:option value="" label="${phvenueType}"/>
                    <form:option value="true" label="${phbar}"/>
                    <form:option value="false" label="${phclub}"/>
                </form:select>
                <form:errors path="isBar" cssClass="error" element="div"/>
            </div>
            <div class="form-group">
                <form:input path="image" type="file" class="form-control"/>
                <form:errors path="image" cssClass="error" element="div"/>
            </div>
            <div class="club-form-buttons">
                <a href="<c:url value='/clubs' />" class="btn btn-secondary">
                    <spring:message code="button.cancel"/>
                </a>
                <button id="saveClubBtn" type="submit" class="btn btn-success">
                    <spring:message code="button.save"/>
                </button>
            </div>
        </form:form>
    </t:card>
</div>
</body>
</html>
<script>
    (function () {
        const form = document.getElementById('clubForm');
        const btn = document.getElementById('saveClubBtn');
        if (!form || !btn) return;

        form.addEventListener('submit', function (e) {
            if (form.dataset.submitted === 'true') {
                e.preventDefault();
                return false;
            }
            form.dataset.submitted = 'true';
            btn.disabled = true;
            btn.setAttribute('aria-busy', 'true');
        });
    })();
    (function () {
        document.querySelectorAll('.select-with-tooltip .info-btn').forEach(function(btn){
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                var tip = btn.parentElement.querySelector('.tooltip-content');
                var isOpen = tip.classList.toggle('show');
                btn.setAttribute('aria-expanded', String(isOpen));
            });
        });

        document.addEventListener('click', function (e) {
            document.querySelectorAll('.select-with-tooltip .tooltip-content.show').forEach(function(tip){
                if (!tip.parentElement.contains(e.target)) {
                    tip.classList.remove('show');
                    var btn = tip.parentElement.querySelector('.info-btn');
                    if (btn) btn.setAttribute('aria-expanded', 'false');
                }
            });
        });
    })();
</script>
