<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!DOCTYPE html>
<html>
<head>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/verify.css' />" />
    <meta charset="UTF-8">
    <title><spring:message code="title.verifyResult"/></title>
</head>
<body>
<div class="verify-page">
    <div class="app-title">
        <spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
    </div>

    <div class="verify-card">
        <div class="verify-content">
            <p class="verify-title"><spring:message code="register.valid.token"/></p>
            <div class="verify-buttons">
                <button type="button" class="btn btn-success"
                        onclick="window.location.href='<c:url value='/login' />'">
                    <spring:message code="return.login"/>
                </button>
            </div>
        </div>
    </div>
</div>
</body>
</html>
