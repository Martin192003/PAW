<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ attribute name="text" required="true" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="textSize" value="${empty size ? 'text-md' : size}" />
<c:set var="textClass" value="${empty cssClass ? '' : cssClass}" />
<c:set var="isDisabled" value="${disabled == true ? 'text-disabled' : ''}" />
<c:set var="classes" value="text ${textSize} ${textClass} ${isDisabled}" />

<span class="${classes}" <c:if test="${disabled}">style="opacity:0.6;pointer-events:none;"</c:if>>
    ${text}
</span>