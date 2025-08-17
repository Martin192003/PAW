
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<link rel="stylesheet" href="<c:url value='/css/buttons.css' />" />
<!DOCTYPE html>
<html>
<body>
<h2>"Hello World!"</h2>
<t:button text="Click me" size="btn-lg" cssClass="btn-success" disabled="false" />
<t:button text="Primary" size="btn-md" cssClass="btn-primary" disabled="false" />
<t:button text="Success Large" size="btn-lg" cssClass="btn-success" disabled="false" />
<t:button text="Danger Small" size="btn-sm" cssClass="btn-danger" disabled="false" />
<t:button text="Warning Disabled" size="btn-md" cssClass="btn-warning" disabled="true" />
<t:button text="Info" size="btn-md" cssClass="btn-info" disabled="false" />
<t:button text="Default" disabled="false" />
</body>
</html>
