<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<body>
<h3>Usuario</h3>
<p>ID: <c:out value="${user.id}" /></p>
<p>Nombre: <c:out value="${user.username}" /></p>
<p>Numero: <c:out value="${user.cellphone}" /></p>
<p>Apellido: <c:out value="${user.surname}"/></p>
<p>Email: <c:out value="${user.email}"/></p>

<p>
    Probar:
    <a href="${pageContext.request.contextPath}/user/1">/user/1</a> |
    <a href="${pageContext.request.contextPath}/user/2">/user/2</a> |
    <a href="${pageContext.request.contextPath}/user/3">/user/3</a>
</p>
<p><a href="${pageContext.request.contextPath}/">Volver</a></p>
</body>
</html>