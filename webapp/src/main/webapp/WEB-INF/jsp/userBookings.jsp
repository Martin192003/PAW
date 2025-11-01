<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="jfn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<html>
<head>
    <title><spring:message code="bookings.list"/></title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookings.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>

<c:set var="showRightButtons" value="true" />
<c:set var="showProfileButton" value="true" />
<c:set var="showAddClubButton" value="false" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<c:set var="activePage" value="bookings" />
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>

<div class="app-bookings">
    <div class="detail-back">
        <button type="button" class="btn btn-success" onclick="window.location.href='<c:url value="/clubs"/>'">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
    </div>
    <h1><spring:message code="menu.reservations"/></h1>

    <div class="filter-container">
        <spring:message code="filter.status" var="phStatus" text="Estado"/>
        <spring:message code="filter.club"   var="phClub"   text="Club"/>
        <spring:message code="booking.id" var="phBookingId" text="ID de la Reserva"/>
        <form:form id="filtersForm" method="get" action="${pageContext.request.contextPath}/bookings" modelAttribute="bookingSearchForm" cssClass="filter-bar">

            <form:hidden path="userId"/>

            <div class="filter-bar">
                <c:if test="${not empty clubs}">
                    <form:select path="clubId" cssClass="filter-item filter-club">
                        <form:option value=""><spring:message code="filter.club"/></form:option>
                        <c:forEach var="c" items="${clubs}">
                            <form:option value="${c.clubId}">
                                <c:out value="${c.clubName}"/>
                            </form:option>
                        </c:forEach>
                    </form:select>
                </c:if>
                <div class="filter-item" style="display:flex; flex-direction:column;">
                    <form:input path="bookingId" placeholder="${phBookingId}" cssClass="filter-item"/>
                </div>

                <form:select path="status" cssClass="filter-item filter-status">
                    <form:option value=""><spring:message code="filter.status"/></form:option>
                    <form:option value="PENDIENTE"><spring:message code="booking.status.pending"/></form:option>
                    <form:option value="CONFIRMADA"><spring:message code="booking.status.confirmed"/></form:option>
                    <form:option value="CANCELADA"><spring:message code="booking.status.cancelled"/></form:option>
                </form:select>

                <form:select path="sort" cssClass="filter-item filter-sort">
                    <form:option value="created_desc"><spring:message code="filter.sort.createdDesc"/></form:option>
                    <form:option value="created_asc"><spring:message code="filter.sort.createdAsc"/></form:option>
                    <form:option value="start_desc"><spring:message code="filter.sort.startDesc"/></form:option>
                    <form:option value="start_asc"><spring:message code="filter.sort.startAsc"/></form:option>
                </form:select>

                <form:hidden path="page"/>
            </div>

            <button type="submit" class="btn btn-filter">
                <i class="fa-solid fa-filter"></i><spring:message code="button.filter"/>
            </button>
        </form:form>
        <button type="button" class="btn-reset" onclick="window.location.href='<c:url value="/bookings"><c:param name="userId" value="${bookingSearchForm.userId}"/></c:url>'">
            <i class="fa-solid fa-rotate-right"></i>
            <spring:message code="button.reset"/>
        </button>
    </div>

    <!-- ========= LISTA ========= -->
    <c:choose>
        <c:when test="${empty bookings}">
            <div style="color:#ffffff; font-size:1.1rem; margin-top:24px;">
                <spring:message code="bookings.empty" text="No hay reservas."/>
            </div>
        </c:when>
        <c:otherwise>
            <c:forEach var="booking" items="${bookings}">
                <a href="<c:url value='/booking'><c:param name='bookingId' value='${booking.bookingId}'/></c:url>"
                   style="text-decoration:none; color:inherit;">
                    <div class="card-md user-booking-card" style="position:relative; margin-bottom:24px;">
                        <span><b><spring:message code='booking.id' text='ID de la Reserva'/>: </b> <c:out value="${booking.bookingId}"/></span><br/>
                        <span><b><spring:message code='filter.club' text='Boliche'/>: </b> <c:out value="${clubNames[booking.bookingId]}"/></span>
                        <div class="booking-status-container">
                            <c:choose>
                                <c:when test="${jfn:trim(jfn:toUpperCase(booking.status)) == 'PENDIENTE'}">
                                    <p class="status-badge is-pending">
                                        <spring:message code="booking.status.pending" text="Pendiente"/>
                                    </p>
                                </c:when>
                                <c:when test="${jfn:trim(jfn:toUpperCase(booking.status)) == 'CONFIRMADA'}">
                                    <p class="status-badge is-confirmed">
                                        <spring:message code="booking.status.confirmed" text="Confirmada"/>
                                    </p>
                                </c:when>
                                <c:when test="${jfn:trim(jfn:toUpperCase(booking.status)) == 'CANCELADA'}">
                                    <p class="status-badge is-cancelled">
                                        <spring:message code="booking.status.cancelled" text="Cancelada"/>
                                    </p>
                                </c:when>
                                <c:otherwise>
                                    <p class="status-badge"><c:out value="${booking.status}"/></p>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </a>
            </c:forEach>
        </c:otherwise>
    </c:choose>

    <c:if test="${totalPages > 1}">
        <div class="pagination">
            <c:if test="${currentPage > 1}">
                <a href="?${queryParams}&page=${currentPage - 1}" class="page-btn" title="Anterior">
                    <i class="fa-solid fa-chevron-left"></i>
                </a>
            </c:if>
            <span class="page-info">
                    ${currentPage} <spring:message code="pagination.of"/> ${totalPages}
            </span>
            <c:if test="${currentPage < totalPages}">
                <a href="?${queryParams}&page=${currentPage + 1}" class="page-btn" title="Siguiente">
                    <i class="fa-solid fa-chevron-right"></i>
                </a>
            </c:if>
        </div>
    </c:if>
</div>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const form = document.getElementById('filtersForm');
        const resetBtn = document.querySelector('.btn-reset');
        const clubSelect = form.querySelector('[name="clubId"]');
        const bookingIdInput = form.querySelector('[name="bookingId"]');
        const statusSelect = form.querySelector('[name="status"]');
        const sortSelect = form.querySelector('[name="sort"]');

        function hasActiveFilters() {
            return (clubSelect && clubSelect.value !== '') ||
                (bookingIdInput && bookingIdInput.value.trim() !== '') ||
                (statusSelect && statusSelect.value !== '') ||
                (sortSelect && sortSelect.value !== 'created_desc');
        }

        function updateResetButton() {
            if (hasActiveFilters()) {
                resetBtn.style.display = 'inline-flex';
            } else {
                resetBtn.style.display = 'none';
            }
        }

        // Verificar al cargar la página
        updateResetButton();

        // Escuchar cambios en los campos
        [clubSelect, bookingIdInput, statusSelect, sortSelect].forEach(field => {
            if (field) {
                field.addEventListener('change', updateResetButton);
                field.addEventListener('input', updateResetButton);
            }
        });
    });
</script>

</body>
</html>
