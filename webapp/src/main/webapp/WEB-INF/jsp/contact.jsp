<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
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
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title"><spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/> </div>
    </a>
</div>

<div class="contact-page">
    <t:card size="card-lg" cssClass="contact-card">
        <div class="contact-title">
            <spring:message code="contact.title" arguments="${clubName}"/>
            <span class="contact-icon"><i class="fa-solid fa-headphones"></i></span>
        </div>
        <spring:message code="form.fullname" var="phName"/>
        <spring:message code="placeholder.email" var="phEmail"/>
        <spring:message code="placeholder.phone" var="phPhone"/>
        <spring:message code="placeholder.message" var="phMessage"/>
        <form:form modelAttribute="clubForm" method="post" id="contactForm">
            <div class="form-group">
                <form:input path="name" class="form-control" placeholder="${phName}" />
            </div>
            <form:errors path="name" cssClass="error" element="div"/>
            <div class="form-group">
                <form:input path="email" class="form-control" placeholder="${phEmail}" />
            </div>
            <form:errors path="email" cssClass="error" element="div"/>
            <div class="form-group">
                <form:input path="phone" class="form-control" placeholder="${phPhone}"/>
            </div>
            <form:errors path="phone" cssClass="error" element="div"/>
            <div class="form-group">
                <form:textarea path="message" class="form-control" placeholder="${phMessage}" />
            </div>
            <form:errors path="message" cssClass="error" element="div"/>
            <div class="contact-buttons">
                <button type="button" class="btn btn-secondary" onclick="window.history.back()"><spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.send"/></button>
            </div>
        </form:form>
    </t:card>
</div>
<div id="sentModal" style="display:none; position:fixed; top:0; left:0; width:100vw; height:100vh; background:rgba(0,0,0,0.5); z-index:9999; justify-content:center; align-items:center;">
    <div style="background:#222; color:#fff; padding:32px; border-radius:12px; box-shadow:0 4px 24px rgba(0,0,0,0.6); width:400px; text-align:center;">
        <div style="font-size:22px; margin-bottom:18px;"><spring:message code="contact.sent"/></div>
        <button class="btn btn-success" onclick="volverADetalles()"><spring:message code="menu.back"/></button>
    </div>
</div>
<script>
    var sent = ${sent ? 'true' : 'false'};
    if (sent) {
        document.addEventListener("DOMContentLoaded", function() {
            document.getElementById("sentModal").style.display = "flex";
        });
    }
    function volverADetalles() {
        window.location.href = "<c:url value='/clubs/${clubId}' />";
    }
</script>
</body>
</html>