<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>

<html>
<body>
<c:url value="/login" var="loginUrl" />
<form action="<c:url value='/login' />"method="post" enctype="application/x-www-form-urlencoded">
    <div>
        <label for="username">Username: </label>
        <input id="username" name="j_username" type="text"/>
    </div>
    <div>
        <label for="password">Password: </label>
        <input id="password" name="j_password" type="password"/>
    </div>
    <div>
        <label><input name="j_rememberme" type="checkbox"/> <spring:message
                code="remember_me"/></label>
    </div>
    <div>
        <input type="submit" value="Login!"/>
    </div>
</form>

<div>
    <p>Don't have an account?</p>
    <a href="<c:url value='/register' />">
        <button type="button">Register</button>
    </a>
</div>
</body>
</html>