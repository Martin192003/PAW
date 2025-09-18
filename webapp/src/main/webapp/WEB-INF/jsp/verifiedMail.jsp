<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />" />
    <link rel="stylesheet" href="<c:url value='/resources/css/details.css' />" />
    <title><spring:message code="title.verifiedMail"/></title>
</head>
<body>
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title"><spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/></div>
    </a>
</div>

<div class="detail-page">
    <div class="detail-header">
        <h1 class="detail-title"><spring:message code="contact.verified"/></h1>
    </div>

    <div class="detail-content">
        <p><spring:message code="contact.processing"/></p>
        <div class="detail-buttons">
            <a href="<c:url value='/clubs' />" class="btn btn-success"><spring:message code="button.return.main"/></a>
        </div>
    </div>
</div>
</body>
</html>