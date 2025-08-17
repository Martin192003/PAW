<%@ tag language="java" pageEncoding="UTF-8" %>
<%@ attribute name="name" required="true" %>
<%@ attribute name="label" required="false" %>
<%@ attribute name="type" required="false" %>
<%@ attribute name="value" required="false" %>
<%@ attribute name="placeholder" required="false" %>
<%@ attribute name="required" required="false" type="java.lang.Boolean" %>
<%@ attribute name="pattern" required="false" %>
<%@ attribute name="error" required="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="inputType" value="${not empty type ? type : 'text'}" />
<c:set var="inputRequired" value="${required ne null ? required : false}" />

<div class="form-group">
    <c:if test="${not empty label}">
        <label for="${name}"><c:out value="${label}" /></label>
    </c:if>

    <input
            type="${inputType}"
            id="${name}"
            name="${name}"
            value="<c:out value='${value}'/>"
            placeholder="<c:out value='${placeholder}'/>"
            class="form-control"
            <c:if test="${inputRequired}">required</c:if>
            <c:if test="${not empty pattern}">pattern="${pattern}"</c:if>
    />

    <c:if test="${not empty error}">
        <span class="error"><c:out value="${error}" /></span>
    </c:if>
</div>
