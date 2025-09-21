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
    <link rel="stylesheet" href="<c:url value='/resources/css/activity.css' />">
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

<div class="panel-content">
    <button type="button" class="btn btn-success"
            onclick="window.location.href='<c:url value="/clubs/activity?userId=${club.contactInfo.id}"/>'">
        <i class="fa-solid fa-arrow-left"></i>
    </button>

    <!-- Botón editar imagen -->
    <div class="club-image-container" style="position:relative;">
        <c:choose>
            <c:when test="${not empty coverPhotoUrl}">
                <img src="<c:url value='${coverPhotoUrl}' />"
                     alt="${club.clubName}" class="club-image" />
            </c:when>
            <c:otherwise>
                <div class="card-no-image">
                    Bling <br>&nbsp;&nbsp;Bling
                </div>
            </c:otherwise>
        </c:choose>
        <button type="button" class="btn btn-success btn-edit-small"
                style="position:absolute; top:12px; right:12px;"
                onclick="document.getElementById('editImageModal').style.display='flex'">
            <i class="fa-solid fa-image"></i>
        </button>
    </div>

    <div class="club-info">
        <div><strong>Nombre:</strong> <c:out value="${club.clubName}" />
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('clubName', '<c:out value="${club.clubName}" />')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
        <div><strong>Ubicación:</strong> <c:out value="${club.location}" />
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('location', '<c:out value="${club.location}" />')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
        <div><strong>Zona:</strong> <c:out value="${club.zone}" />
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('zone', '<c:out value="${club.zone}" />')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
        <div><strong>Género musical:</strong> <c:out value="${club.musicGenre}" />
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('musicGenre', '<c:out value="${club.musicGenre}" />')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
        <div><strong>Precio:</strong> <c:out value="${club.priceRange}" />
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('priceRange', '<c:out value="${club.priceRange}" />')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
        <div><strong>Descripción:</strong> <c:out value="${club.description}" />
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('description', '<c:out value="${club.description}" />')">
                <i class="fa-solid fa-pen"></i>
            </button>
        </div>
    </div>
    <h2>Menú</h2>
    <table class="styled-table">
        <thead>
        <tr>
            <th>Producto</th>
            <th>Descripción</th>
            <th>Precio</th>
            <th>Tipo</th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <c:if test="${empty menu}">
            <tr>
                <td colspan="5" class="styled-table-th" style="text-align:center; background:#3c3c3c; color:#fff; font-weight:bold;">
                    No tiene productos en este momento
                </td>
            </tr>
        </c:if>
        <c:forEach var="product" items="${menu}">
            <tr>
                <td><c:out value="${product.productName}" /></td>
                <td><c:out value="${product.description}" /></td>
                <td>
                    <form method="post" action="<c:url value='/clubs/editProductPrice' />" style="display:flex; gap:2px; align-items:center;">
                        <input type="hidden" name="productId" value="${product.productId}" />
                        <input type="number" name="basePrice" value="${product.basePrice}" step="0.01" required class="input-price"/>
                        <button type="submit" class="btn btn-success btn-sm" title="Actualizar">
                            <i class="fa-solid fa-arrows-rotate"></i>
                        </button>
                    </form>
                </td>
                <td><c:out value="${product.productType}" /></td>
                <td>
                    <div style="display: flex; justify-content: right; margin-right: 12px">
                        <button type="button" class="btn btn-danger btn-sm" onclick="openDeleteModal('${product.productId}')">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
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
        <button type="submit" class="btn btn-success">Agregar</button>
    </form>
</div>

<!--Mesas-->
<div class="club-section" style="margin-top:40px;">
    <h2 class="club-title">Mesas</h2>
    <table class="styled-table">
        <thead>
        <tr>
            <th>Número</th>
            <th>Capacidad</th>
            <th>Precio</th>
            <th>Descuento (%)</th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <c:if test="${empty tables}">
            <tr>
                <td colspan="5" class="styled-table-th-empty">
                    No tiene mesas en este momento
                </td>
            </tr>
        </c:if>
        <c:forEach var="table" items="${tables}">
            <tr>
                <td><c:out value="${table.tableNumber}" /></td>
                <td><c:out value="${table.capacity}" /></td>
                <td><c:out value="${table.price}" /></td>
                <td><c:out value="${table.discount}" /></td>
                <td>
                    <div style="display: flex; gap: 8px; justify-content: right;">
                        <button type="button" class="btn btn-success btn-sm"
                                onclick="openTableEditModal('${table.tableId}', '${table.tableNumber}', '${table.capacity}', '${table.price}', '${table.discount}')"
                                title="Editar">
                            <i class="fa-solid fa-pen"></i>
                        </button>
                        <button type="button" class="btn btn-danger btn-sm"
                                onclick="openTableDeleteModal('${table.tableId}')"
                                title="Eliminar">
                            <i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <h3 style="margin-top:32px;">Agregar mesa</h3>
    <form method="post" action="<c:url value='/clubs/addTable' />" class="add-product-form" style="margin-bottom:24px;">
        <input type="hidden" name="clubId" value="${club.clubId}" />
        <input type="number" name="tableNumber" placeholder="Número de mesa" required min="1" />
        <input type="number" name="capacity" placeholder="Capacidad" required min="1" />
        <input type="number" name="price" placeholder="Precio" step="0.01" required min="0" />
        <input type="number" name="discount" placeholder="Descuento (%)" step="0.01" min="0" max="100" value="0" />
        <button type="submit" class="btn btn-success">Agregar</button>
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

<!-- Modal editar imagen -->
<div id="editImageModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="document.getElementById('editImageModal').style.display='none'">&times;</span>
        <h3>Modificar imagen del club</h3>
        <form method="post" action="<c:url value='/clubs/club/${club.clubId}/photos' />" enctype="multipart/form-data">
            <input type="file" name="file" accept="image/*" required />
            <div class="modal-actions">
                <button type="submit" class="edit-field-btn">Guardar</button>
                <button type="button" class="edit-field-btn btn-secondary" onclick="document.getElementById('editImageModal').style.display='none'">Cancelar</button>
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

    <!-- Modal editar mesa  -->
    <div id="tableEditModal" class="modal">
        <div class="modal-content" style="background:#232323; color:#fff; border-radius:14px; padding:32px 28px; width:440px;">
            <span class="modal-close" onclick="closeTableEditModal()" style="color:#fff;">&times;</span>
            <h2 style="margin-bottom:18px; text-align:center; color:#9c27b0;">Editar mesa</h2>
            <form method="post" id="tableEditForm" style="display:flex; flex-direction:column; gap:18px;">
                <input type="hidden" name="clubId" value="${club.clubId}" />
                <div style="display:flex; flex-direction:column; gap:6px;">
                    <label for="editTableNumber" style="font-weight:500;">Número</label>
                    <input type="number" name="tableNumber" id="editTableNumber" required class="input-price" style="background:#1a1a1a; color:#fff;"/>
                </div>
                <div style="display:flex; flex-direction:column; gap:6px;">
                    <label for="editTableCapacity" style="font-weight:500;">Capacidad</label>
                    <input type="number" name="capacity" id="editTableCapacity" required class="input-price" style="background:#1a1a1a; color:#fff;"/>
                </div>
                <div style="display:flex; flex-direction:column; gap:6px;">
                    <label for="editTablePrice" style="font-weight:500;">Precio</label>
                    <input type="number" name="price" id="editTablePrice" step="0.01" required class="input-price" style="background:#1a1a1a; color:#fff;"/>
                </div>
                <div style="display:flex; flex-direction:column; gap:6px;">
                    <label for="editTableDiscount" style="font-weight:500;">Descuento (%)</label>
                    <input type="number" name="discount" id="editTableDiscount" step="0.01" min="0" max="100" required class="input-price" style="background:#1a1a1a; color:#fff;"/>
                </div>
                <div class="modal-actions" style="margin-top:24px; justify-content:center;">
                    <button type="submit" class="edit-field-btn" style="background:#9c27b0; color:#fff;">Guardar</button>
                    <button type="button" class="edit-field-btn btn-secondary" onclick="closeTableEditModal()">Cancelar</button>
                </div>
            </form>
        </div>
    </div>


<!-- Modal eliminar mesa -->
<div id="tableDeleteModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeTableDeleteModal()">&times;</span>
        <h3>¿Seguro que querés eliminar esta mesa?</h3>
        <form method="post" id="tableDeleteForm">
            <input type="hidden" name="clubId" value="${club.clubId}" />
            <div class="modal-actions">
                <button type="submit" class="edit-field-btn btn-danger">Eliminar</button>
                <button type="button" class="edit-field-btn btn-secondary" onclick="closeTableDeleteModal()">Cancelar</button>
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
            el.style.display = 'none'; // Oculta por style también
        });

        if (field === 'zone') {
            const select = document.getElementById('zoneSelect');
            select.classList.remove("hidden");
            select.style.display = 'block';
            select.name = 'value';
            select.required = true;
            select.value = value;
        } else if (field === 'musicGenre') {
            const select = document.getElementById('musicGenreSelect');
            select.classList.remove("hidden");
            select.style.display = 'block';
            select.name = 'value';
            select.required = true;
            select.value = value;
        } else if (field === 'priceRange') {
            const select = document.getElementById('priceSelect');
            select.classList.remove("hidden");
            select.style.display = 'block';
            select.name = 'value';
            select.required = true;
            select.value = value;
        } else {
            const input = document.getElementById('editFieldValue');
            input.classList.remove("hidden");
            input.style.display = 'block';
            input.name = 'value';
            input.required = true;
            input.value = value;
        }
    }


    function closeEditModal() {
        document.getElementById('editModal').style.display = 'none';
    }
    function openTableEditModal(tableId, tableNumber, capacity, price, discount) {
        document.getElementById('tableEditModal').style.display = 'flex';
        document.getElementById('editTableNumber').value = tableNumber;
        document.getElementById('editTableCapacity').value = capacity;
        document.getElementById('editTablePrice').value = price;
        document.getElementById('editTableDiscount').value = discount;
        document.getElementById('tableEditForm').action =
            '${pageContext.request.contextPath}/clubs/tables/' + tableId + '/edit';    }
    function closeTableEditModal() {
        document.getElementById('tableEditModal').style.display = 'none';
    }
    function openTableDeleteModal(tableId) {
        document.getElementById('tableDeleteModal').style.display = 'flex';
        document.getElementById('tableDeleteForm').action =
            '${pageContext.request.contextPath}/clubs/tables/' + tableId + '/delete';    }
    function closeTableDeleteModal() {
        document.getElementById('tableDeleteModal').style.display = 'none';
    }

</script>

</body>
</html>


