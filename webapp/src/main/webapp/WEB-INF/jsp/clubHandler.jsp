<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <title>Mi actividad</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/activity.css' />"><!-- nuevo -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <meta charset="UTF-8">
</head>

<body>
<div class="top-bar">
    <a href="<c:url value='/clubs' />" class="logo-link">
        <div class="app-title">
            Bling <br>&nbsp;&nbsp;Bling
        </div>
    </a>
</div>

<div class="panel-container">
    <div class="panel-content">
        <button type="button" class="btn btn-back"
                onclick="window.location.href='<c:url value="/clubs/activity?userId=${club.contactInfo.id}"/>'">
            <i class="fa-solid fa-arrow-left"></i>
        </button>

        <h1 class="club-title">
            <c:out value="${club.clubName}" />
            <button type="button" class="edit-field-btn"
                    onclick="openEditModal('clubName', '<c:out value="${club.clubName}" />')">Editar</button>
        </h1>

        <div class="club-image-container">
            <c:if test="${not empty coverPhotoUrl}">
                <img src="<c:url value='${coverPhotoUrl}' />"
                     alt="${club.clubName}" class="club-image" />
            </c:if>
        </div>

        <div class="club-info">
            <div><strong>Ubicación:</strong> <c:out value="${club.location}" />
                <button type="button" class="edit-field-btn"
                        onclick="openEditModal('location', '<c:out value="${club.location}" />')">Editar</button>
            </div>
            <div><strong>Zona:</strong> <c:out value="${club.zone}" />
                <button type="button" class="edit-field-btn"
                        onclick="openEditModal('zone', '<c:out value="${club.zone}" />')">Editar</button>
            </div>
            <div><strong>Género musical:</strong> <c:out value="${club.musicGenre}" />
                <button type="button" class="edit-field-btn"
                        onclick="openEditModal('musicGenre', '<c:out value="${club.musicGenre}" />')">Editar</button>
            </div>
            <div><strong>Precio:</strong> <c:out value="${club.priceRange}" />
                <button type="button" class="edit-field-btn"
                        onclick="openEditModal('priceRange', '<c:out value="${club.priceRange}" />')">Editar</button>
            </div>
            <div><strong>Descripción:</strong> <c:out value="${club.description}" />
                <button type="button" class="edit-field-btn"
                        onclick="openEditModal('description', '<c:out value="${club.description}" />')">Editar</button>
            </div>
        </div>
    </div>
</div>

<div class="menu">
    <h2>Menú</h2>
    <table class="styled-table">
        <thead>
        <tr>
            <th>Producto</th>
            <th>Descripción</th>
            <th>Precio</th>
            <th>Tipo</th>
            <th>Acciones</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="product" items="${menu}">
            <tr>
                <td><c:out value="${product.productName}" /></td>
                <td><c:out value="${product.description}" /></td>
                <td>
                    <form method="post" action="<c:url value='/clubs/editProductPrice' />">
                        <input type="hidden" name="productId" value="${product.productId}" />
                        <input type="number" name="basePrice" value="${product.basePrice}" step="0.01" required class="input-price"/>
                        <button type="submit" class="edit-field-btn">Actualizar</button>
                    </form>
                </td>
                <td><c:out value="${product.productType}" /></td>
                <td>
                    <button type="button" class="edit-field-btn btn-danger"
                            onclick="openDeleteModal('${product.productId}')">Eliminar</button>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <h3>Agregar producto</h3>
    <form method="post" action="<c:url value='/clubs/addProduct' />" class="add-product-form">
        <input type="hidden" name="clubId" value="${club.clubId}" />
        <input type="text" name="productName" placeholder="Nombre" required />
        <input type="text" name="description" placeholder="Descripción" />
        <input type="number" name="basePrice" placeholder="Precio" step="0.01" required />
        <select name="productType" required>
            <option value="alcoholicas">Bebida alcohólica</option>
            <option value="noAlcoholicas">Bebida sin alcohol</option>
            <option value="tragos">Trago</option>
        </select>
        <button type="submit" class="edit-field-btn">Agregar</button>
    </form>
</div>

<!-- Modales -->
<div id="editModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeEditModal()">&times;</span>
        <form method="post" action="<c:url value='/clubs/editField' />">
            <input type="hidden" name="clubId" value="${club.clubId}" />
            <input type="hidden" name="field" id="editFieldName" value="" />

            <label>Nuevo valor:</label>
            <input type="text" name="value" id="editFieldValue" required />

            <select name="value" id="zoneSelect" class="hidden">
                <c:forEach var="z" items="${zones}">
                    <option value="${z}">${z.displayName}</option>
                </c:forEach>
            </select>

            <select name="value" id="musicGenreSelect" class="hidden">
                <c:forEach var="g" items="${genres}">
                    <option value="${g}">${g.label}</option>
                </c:forEach>
            </select>

            <select name="value" id="priceSelect" class="hidden">
                <c:forEach var="p" items="${prices}">
                    <option value="${p}">${p.label}</option>
                </c:forEach>
            </select>

            <div class="modal-actions">
                <button type="submit" class="edit-field-btn">Guardar</button>
                <button type="button" class="edit-field-btn btn-secondary" onclick="closeEditModal()">Cancelar</button>
            </div>
        </form>
    </div>
</div>

<div id="deleteModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeDeleteModal()">&times;</span>
        <h3>¿Seguro que querés eliminar este producto?</h3>
        <form id="deleteForm" method="post" action="<c:url value='/clubs/deleteProduct' />">
            <input type="hidden" name="productId" id="deleteProductId" value="" />
            <div class="modal-actions">
                <button type="submit" class="edit-field-btn btn-danger">Eliminar</button>
                <button type="button" class="edit-field-btn btn-secondary" onclick="closeDeleteModal()">Cancelar</button>
            </div>
        </form>
    </div>
</div>

<script>
    function openDeleteModal(productId) {
        document.getElementById('deleteProductId').value = productId;
        document.getElementById('deleteModal').style.display = 'flex';
    }
    function closeDeleteModal() {
        document.getElementById('deleteModal').style.display = 'none';
    }
    function openEditModal(field, value) {
        document.getElementById('editModal').style.display = 'flex';
        document.getElementById('editFieldName').value = field;

        const inputs = ['editFieldValue', 'zoneSelect', 'musicGenreSelect', 'priceSelect'];
        inputs.forEach(id => {
            const el = document.getElementById(id);
            el.classList.add("hidden");
            el.removeAttribute('name');
            el.removeAttribute('required');
        });

        let active;
        if (field === 'zone') active = 'zoneSelect';
        else if (field === 'musicGenre') active = 'musicGenreSelect';
        else if (field === 'priceRange') active = 'priceSelect';
        else active = 'editFieldValue';

        const el = document.getElementById(active);
        el.classList.remove("hidden");
        el.name = "value";
        el.required = true;
        el.value = value;
    }
    function closeEditModal() {
        document.getElementById('editModal').style.display = 'none';
    }
</script>

</body>
</html>



