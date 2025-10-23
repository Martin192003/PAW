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
<c:set var="showRightButtons" value="true"/>
<c:set var="showProfileButton" value="false"/>
<c:set var="showAddClubButton" value="false"/>
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<c:set var="activePage" value="profile"/>
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
            <span class="profile-label"><spring:message code="profile.username"/>:</span>
            <span class="profile-value">${user.username}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editUsernameModal')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="profile.name"/>:</span>
            <span class="profile-value">${user.name}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editNameModal')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="profile.surname"/>:</span>
            <span class="profile-value">${user.surname}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editSurnameModal')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="profile.email"/>:</span>
            <span class="profile-value">${user.email}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editEmailModal')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="profile.cellphone"/>:</span>
            <span class="profile-value">${user.cellphone}</span>
            <button class="btn btn-success btn-edit-small" onclick="openModal('editCellphoneModal')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
        <div class="profile-info-row">
            <span class="profile-label"><spring:message code="form.password"/>:</span>
            <span class="profile-value">••••••••</span>
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openModal('editPasswordModal')"><i class="fa-solid fa-key"></i>
            </button>
        </div>
    </div>
</div>


<!-- MODAL USERNAME -->
<div id="editUsernameModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editUsernameModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/username" method="post"
                   modelAttribute="editUserForm">
            <label for="username"><spring:message code="profile.username"/></label>
            <form:input path="username" id="username" data-original="${user.username}"/>
            <form:errors path="username" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editUsernameModal')">
                    <spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<!-- MODAL NAME -->
<div id="editNameModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editNameModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/name" method="post"
                   modelAttribute="editUserForm">
            <label for="name"><spring:message code="profile.name"/></label>
            <form:input path="username" id="username" data-original="${user.name}"/>
            <form:errors path="name" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editNameModal')"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<!-- MODAL SURNAME -->
<div id="editSurnameModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editSurnameModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/surname" method="post"
                   modelAttribute="editUserForm">
            <label for="surname"><spring:message code="profile.surname"/></label>
            <form:input path="username" id="username" data-original="${user.surname}"/>
            <form:errors path="surname" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editSurnameModal')"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<!-- MODAL EMAIL -->
<div id="editEmailModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editEmailModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/email" method="post"
                   modelAttribute="editUserForm">
            <label for="email"><spring:message code="profile.email"/></label>
            <form:input path="username" id="username" data-original="${user.email}"/>
            <form:errors path="email" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editEmailModal')"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<!-- MODAL CELLPHONE -->
<div id="editCellphoneModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editCellphoneModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/cellphone" method="post"
                   modelAttribute="editUserForm">
            <label for="cellphone"><spring:message code="profile.cellphone"/></label>
            <form:input path="username" id="username" data-original="${user.cellphone}"/>
            <form:errors path="cellphone" cssClass="error"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editCellphoneModal')">
                    <spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<!-- MODAL PASSWORD -->
<div id="editPasswordModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeModal('editPasswordModal')">&times;</span>
        <form:form action="${pageContext.request.contextPath}/profile/edit/password" method="post"
                   modelAttribute="editUserForm">
            <label for="password"><spring:message code="form.password"/></label>
            <form:password path="password" id="password"/>
            <form:errors path="password" cssClass="error"/>

            <label for="confirmPassword"><spring:message code="form.confirmPassword"/></label>
            <form:password path="confirmPassword" id="confirmPassword"/>
            <form:errors path="confirmPassword" cssClass="error"/>

            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeModal('editPasswordModal')">
                    <spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form:form>
    </div>
</div>

<script>
    function hasValidationErrors(modal) {
        if (!modal) return false;
        return modal.querySelector(
            'span.error, .error, .formError, .error-message, .field-error, .validation-error, .help-block, .invalid-feedback, .form-errors, .errors'
        ) !== null;
    }

    function setOriginalValues(modal) {
        if (!modal) return;
        modal.querySelectorAll('input[data-original], select[data-original], textarea[data-original]')
            .forEach(el => {
                el.value = el.getAttribute('data-original') ?? '';
            });
    }

    function clearFormErrors(scopeId) {
        const root = scopeId ? document.getElementById(scopeId) : document;
        if (!root) return;

        root.querySelectorAll(
            'span.error, .error, .formError, .error-message, .field-error, .validation-error, .help-block, .invalid-feedback, .form-errors, .errors'
        ).forEach(el => el.remove());

        root.querySelectorAll('input, select, textarea').forEach(el => {
            el.classList.remove('input-error', 'is-invalid', 'has-error', 'is-danger');
            el.removeAttribute('aria-invalid');
        });

        root.querySelectorAll('.form-group, .has-error, .is-invalid').forEach(el => {
            el.classList.remove('has-error', 'is-invalid');
        });
    }

    function clearErrorsExcept(exceptId) {
        document.querySelectorAll('.modal').forEach(m => {
            if (m.id !== (exceptId || '')) {
                clearFormErrors(m.id);
                setOriginalValues(m);
            }
        });
    }

    function openModal(id) {
        const modal = document.getElementById(id);
        if (!modal) return;
        clearErrorsExcept(id);
        if (!hasValidationErrors(modal)) {
            setOriginalValues(modal);
        }
        modal.style.display = "flex";
    }

    function closeModal(id) {
        const modal = document.getElementById(id);
        if (!modal) return;
        modal.style.display = "none";
        clearFormErrors(id);
        setOriginalValues(modal);
    }

    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('.modal').forEach(m => {
            m.addEventListener('click', (e) => {
                if (e.target === e.currentTarget) closeModal(m.id);
            });
        });
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal').forEach(m => {
                if (getComputedStyle(m).display !== 'none') closeModal(m.id);
            });
        }
    });

    window.onload = function() {
        var openedId = null;
        <c:if test="${not empty editField}">
        openedId = 'edit${editField.substring(0,1).toUpperCase()}${editField.substring(1)}Modal';
        openModal(openedId);
        </c:if>
        clearErrorsExcept(openedId);
    }

    document.addEventListener('DOMContentLoaded', function() {
        var cellphoneInput = document.querySelector('#editCellphoneModal input');
        if (cellphoneInput) {
            cellphoneInput.addEventListener('keydown', function(e) {
                if (e.key === '-' || e.key === 'Subtract') {
                    e.preventDefault();
                }
            });
            cellphoneInput.addEventListener('input', function(e) {
                if (this.value.includes('-')) {
                    this.value = this.value.replace(/-/g, '');
                }
            });
        }
    });
</script>


</body>
</html>
