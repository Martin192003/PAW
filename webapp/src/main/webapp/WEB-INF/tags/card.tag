<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ attribute name="text" required="false" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="cardSize" value="${empty size ? 'card-md' : size}" />
<c:set var="cardClass" value="${empty cssClass ? 'card-default' : cssClass}" />
<c:set var="isDisabled" value="${disabled == true ? 'card-disabled' : ''}" />
<c:set var="classes" value="card ${cardSize} ${cardClass} ${isDisabled}" />

<div class="${classes}" <c:if test="${disabled}">style="pointer-events:none;opacity:0.6;"</c:if>>
    <div class="card-body">
        <jsp:doBody/>
    </div>
</div>