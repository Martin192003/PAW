<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
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
        <h2>Editar perfil</h2>
        <form method="post" action="${pageContext.request.contextPath}/profile/edit" style="display:flex; flex-direction:column; gap:18px;">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
            <input type="hidden" id="editFieldName" name="fieldName"/>
            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editFieldValue" id="editFieldLabel" style="font-weight:500;"></label>
                <input type="text" id="editFieldValue" name="fieldValue" class="form-control"/>
                <c:if test="${not empty usernameError and editField == 'username'}">
                    <div style="color: #e53935; font-size: 0.95em; margin-top: 4px;">
                            ${usernameError}
                    </div>
                </c:if>
                <div id="modalErrorContainer"></div>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeEditProfileModal()">Cancelar</button>
                <button type="submit" class="btn btn-success">Guardar</button>
            </div>
        </form>
    </div>
</div>


<script>
    function openEditProfileModal(field, value) {
        document.getElementById('editProfileModal').style.display = 'flex';
        document.getElementById('editFieldName').value = field;
        document.getElementById('editFieldLabel').innerText =
            field.charAt(0).toUpperCase() + field.slice(1);
        document.getElementById('editFieldValue').value = value;

        if (field !== 'username') {
            var errorDiv = document.querySelector('div[style*="color: #e53935"]');
            if (errorDiv) errorDiv.innerHTML = '';
        }
    }

    function closeEditProfileModal() {
        document.getElementById('editProfileModal').style.display = 'none';
    }

    function capitalize(str) {
        return str.charAt(0).toUpperCase() + str.slice(1);
    }
</script>
<script>
    window.addEventListener('DOMContentLoaded', function() {
        <c:if test="${openModal and editField == 'username'}">
        openEditProfileModal('username', '${user.username}');
        </c:if>
    });
</script>
</body>
</html>
