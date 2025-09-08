<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<!DOCTYPE html>
<html>
<head>
    <title>Register</title>
</head>
<body>
<h1>Register</h1>

<c:url var="registerUrl" value="/register" />

<form:form modelAttribute="userForm" method="post" action="${registerUrl}">
    <!-- CSRF (Spring Security) -->
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

    <div>
        <label>Username:</label>
        <form:input path="username" />
        <form:errors path="username" cssClass="error" />
    </div>
    <div>
        <label>Surname:</label>
        <form:input path="surname" />
        <form:errors path="surname" cssClass="error" />
    </div>
    <div>
        <label>Password:</label>
        <form:password path="password" />
        <form:errors path="password" cssClass="error" />
    </div>
    <div>
        <label>Email:</label>
        <form:input path="email" />
        <form:errors path="email" cssClass="error" />
    </div>
    <div>
        <label>Cellphone:</label>
        <form:input path="cellphone" />
        <form:errors path="cellphone" cssClass="error" />
    </div>

    <div>
    <label>Role:</label>
    <form:input path="role" />
    <form:errors path="role" cssClass="error" />
    </div>


    <div>
        <button type="submit">Register</button>
    </div>
</form:form>
</body>
</html>
