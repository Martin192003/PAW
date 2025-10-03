<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title><spring:message code="title.myProfile"/></title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/profile.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title"><spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/></div>
    </a>
</div>
<c:set var="activePage" value="profile" />
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>

<div class="profile-page">
    <div class="detail-back">
        <button type="button" class="btn btn-success" onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
    </div>
    <h1 class="profile-title"><spring:message code="profile.title"/></h1>

    <div class="profile-card">
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="profile.username"/>:<br> ${user.username}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editUsernameModal')"><i class="fa-solid fa-pen"></i></button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="profile.name"/>:<br> ${user.name}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editNameModal')"><i class="fa-solid fa-pen"></i></button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="profile.surname"/>:<br> ${user.surname}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editSurnameModal')"><i class="fa-solid fa-pen"></i></button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="profile.email"/>:<br> ${user.email}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editEmailModal')"><i class="fa-solid fa-pen"></i></button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="profile.cellphone"/>:<br> ${user.cellphone}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editCellphoneModal')"><i class="fa-solid fa-pen"></i></button>
        </div>
    </div>
</div>


<!-- MODAL USERNAME -->
<div id="editUsernameModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editUsernameModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/username" method="post" modelAttribute="editUserForm">
            <label for="username"><spring:message code="profile.username"/></label>
            <form:input path="username" id="username"/>
            <form:errors path="username" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editUsernameModal')"><spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<!-- MODAL NAME -->
<div id="editNameModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editNameModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/name" method="post" modelAttribute="editUserForm">
            <label for="name"><spring:message code="profile.name"/></label>
            <form:input path="name" id="name"/>
            <form:errors path="name" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editNameModal')"><spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<!-- MODAL SURNAME -->
<div id="editSurnameModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editSurnameModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/surname" method="post" modelAttribute="editUserForm">
            <label for="surname"><spring:message code="profile.surname"/></label>
            <form:input path="surname" id="surname"/>
            <form:errors path="surname" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editSurnameModal')"><spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<!-- MODAL EMAIL -->
<div id="editEmailModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editEmailModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/email" method="post" modelAttribute="editUserForm">
            <label for="email"><spring:message code="profile.email"/></label>
            <form:input path="email" id="email"/>
            <form:errors path="email" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editEmailModal')"><spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<!-- MODAL CELLPHONE -->
<div id="editCellphoneModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editCellphoneModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/cellphone" method="post" modelAttribute="editUserForm">
            <label for="cellphone"><spring:message code="profile.cellphone"/></label>
            <form:input path="cellphone" id="cellphone"/>
            <form:errors path="cellphone" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editCellphoneModal')"><spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<script>
    function openModal(id) {
        document.getElementById(id).style.display = "flex";
    }
    function closeModal(id) {
        document.getElementById(id).style.display = "none";
    }

    // Abrir modal automáticamente si hay error en ese campo
    window.onload = function() {
        <c:if test="${not empty editField}">
        openModal('edit${editField.substring(0,1).toUpperCase()}${editField.substring(1)}Modal');
        </c:if>
    }

    // Cerrar modal haciendo click fuera del contenido
    window.onclick = function(event) {
        if (event.target.classList.contains('modal')) {
            event.target.style.display = "none";
        }
    }
</script>
</body>
</html>
