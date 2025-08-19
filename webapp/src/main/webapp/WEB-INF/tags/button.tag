<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ attribute name="text" required="true" %>
<%@ attribute name="size" required="false" %>
<%@ attribute name="cssClass" required="false" %>
<%@ attribute name="disabled" required="false" type="java.lang.Boolean" %>
<%@ attribute name="type" required="false" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="btnSize" value="${empty size ? 'btn-md' : size}" />
<c:set var="btnClass" value="${empty cssClass ? 'btn-primary' : cssClass}" />
<c:set var="isDisabled" value="${disabled == true ? 'disabled' : ''}" />
<c:set var="classes" value="btn ${btnSize} ${btnClass} ${isDisabled}" />
<c:set var="btnType" value="${empty type ? 'button' : type}" />

<button type="${btnType}" class="${classes}" <c:if test="${isDisabled}"> disabled</c:if>>
    ${text}
</button>