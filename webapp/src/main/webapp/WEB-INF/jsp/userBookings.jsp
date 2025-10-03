<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="jfn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<html>
<head>
    <title>Mis reservas</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/input.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/card.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/clubActivity.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/topbar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookings.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/bookingUserHandler.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/filters.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>.field-error { color:#e53935; font-size:.9rem; margin-top:4px; }</style>
</head>
<body>

<c:set var="showRightButtons" value="true" />
<c:set var="showProfileButton" value="true" />
<c:set var="showAddClubButton" value="false" />
<%@ include file="/WEB-INF/jspf/topbar.jspf" %>
<c:set var="activePage" value="bookings" />
<%@ include file="/WEB-INF/jspf/sidebar.jspf" %>

<div class="app-bookings">
    <h1>Mis reservas</h1>

    <!-- ========= FILTROS ========= -->
    <div class="filter-container">
        <spring:message code="booking.id" var="phBookingId" text="ID de la Reserva"/>
        <form:form method="get" action="${pageContext.request.contextPath}/bookings"
                   modelAttribute="bookingSearchForm" cssClass="filter-bar">

            <form:hidden path="userId"/>

            <!-- Club -->
            <c:if test="${not empty clubs}">
                <form:select path="clubId" cssClass="filter-item">
                    <form:option value=""><spring:message code="filter.club" text="Club"/></form:option>
                    <c:forEach var="c" items="${clubs}">
                        <form:option value="${c.clubId}">
                            <c:out value="${c.clubName}"/>
                        </form:option>
                    </c:forEach>
                </form:select>
            </c:if>

            <!-- Booking ID -->
            <div class="filter-item" style="display:flex; flex-direction:column;">
                <form:input path="bookingId"
                            placeholder="${phBookingId}" cssClass="filter-item"/>
                <form:errors path="bookingId" cssClass="field-error" element="div"/>
            </div>

            <!-- Estado -->
            <form:select path="status" cssClass="filter-item">
                <form:option value=""><spring:message code="filter.status"/></form:option>
                <form:option value="PENDIENTE"><spring:message code="booking.status.pending"/></form:option>
                <form:option value="CONFIRMADA"><spring:message code="booking.status.confirmed"/></form:option>
                <form:option value="CANCELADA"><spring:message code="booking.status.cancelled"/></form:option>
            </form:select>

            <!-- Orden -->
            <form:select path="sort" cssClass="filter-item filter-sort">
                <form:option value="created_desc"><spring:message code="filter.sort.createdDesc"/></form:option>
                <form:option value="created_asc"><spring:message code="filter.sort.createdAsc"/></form:option>
                <form:option value="start_desc"><spring:message code="filter.sort.startDesc"/></form:option>
                <form:option value="start_asc"><spring:message code="filter.sort.startAsc"/></form:option>
            </form:select>

            <button type="submit" class="btn btn-filter">
                <i class="fa-solid fa-filter"></i>&nbsp;<spring:message code="button.filter"/>
            </button>
        </form:form>

        <!-- Reset limpio -->
        <button type="button" class="btn-reset"
                onclick="window.location.href='<c:url value="/bookings"><c:param name="userId" value="${bookingSearchForm.userId}"/></c:url>'">
            <i class="fa-solid fa-rotate-right"></i>
            <spring:message code="button.reset"/>
        </button>
    </div>

    <!-- ========= LISTA ========= -->
    <c:choose>
        <c:when test="${empty bookings}">
            <div style="color:#e53935; font-size:1.1rem; margin-top:24px;">
                <spring:message code="bookings.empty" text="No hay reservas."/>
            </div>
        </c:when>
        <c:otherwise>
            <c:forEach var="booking" items="${bookings}">
                <a href="<c:url value='/booking'><c:param name='bookingId' value='${booking.bookingId}'/></c:url>"
                   style="text-decoration:none; color:inherit;">
                    <div class="card-lg booking-card" style="position:relative; margin-bottom:24px;">
                        <span><b><spring:message code='booking.id' text='ID de la Reserva'/>: </b> <c:out value="${booking.bookingId}"/></span><br/>
                        <span><b><spring:message code='filter.club' text='Boliche'/>: </b> <c:out value="${clubNames[booking.bookingId]}"/></span><br/>
                        <div class="booking-status-container">
                            <c:choose>
                                <c:when test="${jfn:trim(jfn:toUpperCase(booking.status)) == 'PENDIENTE'}">
                                    <span class="booking-status booking-status-pending">
                                        <spring:message code="booking.status.pending" text="Pendiente"/>
                                    </span>
                                </c:when>
                                <c:when test="${jfn:trim(jfn:toUpperCase(booking.status)) == 'CONFIRMADA'}">
                                    <span class="booking-status booking-status-confirmed">
                                        <spring:message code="booking.status.confirmed" text="Confirmada"/>
                                    </span>
                                </c:when>
                                <c:when test="${jfn:trim(jfn:toUpperCase(booking.status)) == 'CANCELADA'}">
                                    <span class="booking-status booking-status-cancelled">
                                        <spring:message code="booking.status.cancelled" text="Cancelada"/>
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span class="booking-status"><c:out value="${booking.status}"/></span>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </a>
            </c:forEach>
        </c:otherwise>
    </c:choose>

    <!-- ========= PAGINACIÓN ========= -->
    <c:if test="${totalPages > 1}">
        <div class="pagination">
            <c:if test="${currentPage > 1}">
                <a href="?${queryParams}&page=${currentPage - 1}" class="page-btn" title="Anterior">
                    <i class="fa-solid fa-chevron-left"></i>
                </a>
            </c:if>
            <span class="page-info">${currentPage} de ${totalPages}</span>
            <c:if test="${currentPage < totalPages}">
                <a href="?${queryParams}&page=${currentPage + 1}" class="page-btn" title="Siguiente">
                    <i class="fa-solid fa-chevron-right"></i>
                </a>
            </c:if>
        </div>
    </c:if>
</div>

</body>
</html>