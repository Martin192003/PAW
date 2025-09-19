<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Mi actividad</title>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
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
        <button type="button" class="btn btn-success" style="margin-top: 80px"
                onclick="window.location.href='<c:url value="/clubs/activity?userId=${club.contactInfo.id}"/>'">
            <i class="fa-solid fa-arrow-left"></i>
        </button>
        <h1>
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
        <p>
            <strong>Ubicación:</strong> <c:out value="${club.location}" />
            <button type="button" class="edit-field-btn"
                    onclick="openEditModal('location', '<c:out value="${club.location}" />')">Editar</button>
        </p>
        <p>
            <strong>Zona:</strong> <c:out value="${club.zone}" />
            <button type="button" class="edit-field-btn"
                    onclick="openEditModal('zone', '<c:out value="${club.zone}" />')">Editar</button>
        </p>
        <p>
            <strong>Género musical:</strong> <c:out value="${club.musicGenre}" />
            <button type="button" class="edit-field-btn"
                    onclick="openEditModal('musicGenre', '<c:out value="${club.musicGenre}" />')">Editar</button>
        </p>
        <p>
            <strong>Precio:</strong> <c:out value="${club.priceRange}" />
            <button type="button" class="edit-field-btn"
                    onclick="openEditModal('priceRange', '<c:out value="${club.priceRange}" />')">Editar</button>
        </p>
        <p>
            <strong>Descripción:</strong> <c:out value="${club.description}" />
            <button type="button" class="edit-field-btn"
                    onclick="openEditModal('description', '<c:out value="${club.description}" />')">Editar</button>
        </p>
    </div>
</div>

<div class="menu" style="text-align: center">
    <h2>Menú</h2>
    <table>
        <tr>
            <th>Producto</th>
            <th>Descripción</th>
            <th>Precio</th>
            <th>Tipo</th>
            <th>Acciones</th>
        </tr>
        <c:forEach var="product" items="${menu}">
            <tr>
                <td><c:out value="${product.productName}" /></td>
                <td><c:out value="${product.description}" /></td>
                <td>
                    <form method="post" action="<c:url value='/clubs/editProductPrice' />" style="display:inline;">
                        <input type="hidden" name="productId" value="${product.productId}" />
                        <input type="number" name="basePrice" value="${product.basePrice}" step="0.01" required />
                        <button type="submit" class="edit-field-btn">Actualizar</button>
                    </form>
                </td>
                <td><c:out value="${product.productType}" /></td>
                <td>
                    <form method="post" action="<c:url value='/clubs/deleteProduct' />" style="display:inline;">
                        <input type="hidden" name="productId" value="${product.productId}" />
                        <button type="button" class="edit-field-btn" onclick="openDeleteModal('${product.productId}')">Eliminar</button>
                    </form>
                </td>
            </tr>
        </c:forEach>
    </table>


    <!-- Formulario para agregar producto -->
    <h3>Agregar producto</h3>
    <form method="post" action="<c:url value='/clubs/addProduct' />">
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

<!-- Modal -->
<div id="editModal" style="display:none; position:fixed; top:0; left:0; width:100vw; height:100vh; background:rgba(0,0,0,0.5); z-index:999;">
    <div style="background:#fff; margin:10% auto; padding:20px; width:400px; border-radius:8px; position:relative;">
        <span style="position:absolute; top:10px; right:20px; cursor:pointer;" onclick="closeEditModal()">&times;</span>
        <form method="post" action="<c:url value='/clubs/editField' />">
            <input type="hidden" name="clubId" value="${club.clubId}" />
            <input type="hidden" name="field" id="editFieldName" value="" />
            <label style="color: #1b1a1a">Nuevo valor:</label>

            <!-- input por defecto -->
            <input type="text" name="value" id="editFieldValue" required />

            <!-- select para Zone -->
            <select name="value" id="zoneSelect" style="display:none;">
                <c:forEach var="z" items="${zones}">
                    <option value="${z}">${z.displayName}</option>
                </c:forEach>
            </select>

            <!-- select para MusicalGenre -->
            <select name="value" id="musicGenreSelect" style="display:none;">
                <c:forEach var="g" items="${genres}">
                    <option value="${g}">${g.label}</option>
                </c:forEach>
            </select>

            <!-- select para Price -->
            <select name="value" id="priceSelect" style="display:none;">
                <c:forEach var="p" items="${prices}">
                    <option value="${p}">${p.label}</option>
                </c:forEach>
            </select>

            <br><br>
            <button type="submit" class="edit-field-btn">Guardar</button>
            <button type="button" class="edit-field-btn" onclick="closeEditModal()">Cancelar</button>
        </form>
    </div>
</div>

<!-- Modal de confirmación de eliminación -->
<div id="deleteModal" style="display:none; position:fixed; top:0; left:0; width:100vw; height:100vh; background:rgba(0,0,0,0.5); z-index:999;">
    <div style="background:#fff; margin:10% auto; padding:20px; width:400px; border-radius:8px; position:relative;">
        <span style="position:absolute; top:10px; right:20px; cursor:pointer;" onclick="closeDeleteModal()">&times;</span>
        <h3 style="color: black">¿Seguro que querés eliminar este producto?</h3>
        <form id="deleteForm" method="post" action="<c:url value='/clubs/deleteProduct' />">
            <input type="hidden" name="productId" id="deleteProductId" value="" />
            <button type="submit" class="edit-field-btn">Eliminar</button>
            <button type="button" class="edit-field-btn" onclick="closeDeleteModal()">Cancelar</button>
        </form>
    </div>
</div>

<script>
    function openDeleteModal(productId) {
        document.getElementById('deleteProductId').value = productId;
        document.getElementById('deleteModal').style.display = 'block';
    }
    function closeDeleteModal() {
        document.getElementById('deleteModal').style.display = 'none';
    }
</script>

<script>
    function openEditModal(field, value) {
        document.getElementById('editModal').style.display = 'block';
        document.getElementById('editFieldName').value = field;

        const inputs = ['editFieldValue', 'zoneSelect', 'musicGenreSelect', 'priceSelect'];
        inputs.forEach(id => {
            const el = document.getElementById(id);
            el.style.display = 'none';
            el.removeAttribute('name');
            el.removeAttribute('required'); // <-- quitar required
        });

        if (field === 'zone') {
            const select = document.getElementById('zoneSelect');
            select.style.display = 'block';
            select.name = 'value';
            select.required = true;
            select.value = value;
        } else if (field === 'musicGenre') {
            const select = document.getElementById('musicGenreSelect');
            select.style.display = 'block';
            select.name = 'value';
            select.required = true;
            select.value = value;
        } else if (field === 'priceRange') {
            const select = document.getElementById('priceSelect');
            select.style.display = 'block';
            select.name = 'value';
            select.required = true;
            select.value = value;
        } else {
            const input = document.getElementById('editFieldValue');
            input.style.display = 'block';
            input.name = 'value';
            input.required = true;
            input.value = value;
        }
    }

    function closeEditModal() {
        document.getElementById('editModal').style.display = 'none';
    }
</script>

</body>
</html>


