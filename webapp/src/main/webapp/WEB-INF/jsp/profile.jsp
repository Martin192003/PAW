<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
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
        <button type="button" class="btn btn-success"
                onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
    </div>
    <h1 class="profile-title"><spring:message code="title.myProfile"/></h1>
    <div class="profile-card">
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="form.username"/>:</span>
            <span class="profile-value">${user.username}</span>
            <button type="button" class="btn btn-success btn-edit-small" onclick="openEditProfileModal('username', '${user.username}')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="form.name"/>:</span>
            <span class="profile-value">${user.name}</span>
            <button type="button" class="btn btn-success btn-edit-small" onclick="openEditProfileModal('name', '${user.name}')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="form.surname"/>:</span>
            <span class="profile-value">${user.surname}</span>
            <button type="button" class="btn btn-success btn-edit-small" onclick="openEditProfileModal('surname', '${user.surname}')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="form.email"/>:</span>
            <span class="profile-value">${user.email}</span>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="form.cellphone"/>:</span>
            <span class="profile-value">${user.cellphone}</span>
            <button type="button" class="btn btn-success btn-edit-small" onclick="openEditProfileModal('cellphone', '${user.cellphone}')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
    </div>

</div>
<div id="editProfileModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeEditProfileModal()">&times;</span>
        <form:form modelAttribute="userForm" method="post" action="<c:url value='/profile/edit'/>" style="display:flex; flex-direction:column; gap:18px;">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <input type="hidden" id="editFieldName" name="fieldName" />
            <div style="display:flex; flex-direction:column; gap:6px;">
                <form:label id="editFieldLabel" path=""></form:label>
                <form:input id="editFieldValue" path="" cssClass="form-control" />
                <form:errors id="fieldErrorSpan" path="" cssClass="error" />
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeEditProfileModal()">
                    <spring:message code="modal.edit.cancel"/>
                </button>
                <button type="submit" class="btn btn-success">
                    <spring:message code="modal.edit.save"/>
                </button>
            </div>
        </form:form>
    </div>
</div>

<script>
    function openEditProfileModal(field, value) {
        document.getElementById('editProfileModal').style.display = 'flex';
        document.getElementById('editFieldName').value = field;
        document.getElementById('editFieldValue').value = value;
        document.getElementById('editFieldLabel').innerText = field.charAt(0).toUpperCase() + field.slice(1);
    }

    function closeEditProfileModal() {
        document.getElementById('editProfileModal').style.display = 'none';
    }
</script>
</body>
</html>
