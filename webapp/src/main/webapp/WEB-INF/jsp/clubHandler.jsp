<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="es">
<head><title><spring:message code="title.myActivity"/></title>
    <link rel="icon" type="image/png" href="<c:url value='/resources/img/favicon.png'/>"/>
    <link rel="stylesheet" href="<c:url value='/resources/css/buttons.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/text.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/index.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/sidebar.css' />">
    <link rel="stylesheet" href="<c:url value='/resources/css/activity.css' />">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <meta charset="UTF-8">
</head>
<body>
<div class="top-bar"><a href="<c:url value='/clubs' />" class="logo-link">
    <div class="app-title"><spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/></div>
</a></div>
<div class="panel-content">
    <button type="button" class="btn btn-success"
            onclick="window.location.href='<c:url value="/clubs/activity?userId=${club.contactInfo.id}"/>'"><i
            class="fa-solid fa-arrow-left"></i></button> <!-- Botón editar imagen -->
    <div class="club-image-container" style="position:relative;"><c:choose> <c:when test="${not empty coverPhotoUrl}">
        <img src="<c:url value='${coverPhotoUrl}' />" alt="<c:out value='${club.clubName}'/>" class="card-image"/>
    </c:when> <c:otherwise>
        <div class="card-no-image"><spring:message code="app.title"/> <br>&nbsp;&nbsp;<spring:message code="app.title"/>
        </div>
    </c:otherwise> </c:choose>
        <button type="button" class="btn btn-success btn-edit-small" style="position:absolute; top:12px; right:12px;"
                onclick="document.getElementById('editImageModal').style.display='flex'"><i
                class="fa-solid fa-image"></i></button>
    </div>
    <div class="club-info">
        <div><strong><spring:message code="form.name"/>:&nbsp;</strong> <c:out value="${club.clubName}"/>
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('clubName', '<c:out value="${club.clubName}"/>')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
        <div><strong><spring:message code="form.location"/>:&nbsp;</strong> <c:out value="${club.location}"/>
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('location', '<c:out value="${club.location}"/>')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
        <div><strong><spring:message code="label.zone"/>:&nbsp;</strong> <spring:message code="zone.${club.zone}"/>
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('zone', '<c:out value="${club.zone}"/>')"><i class="fa-solid fa-pen"></i>
            </button>
        </div>
        <div><strong><spring:message code="label.musicGenre"/>:&nbsp;</strong> <spring:message
                code="genre.${club.musicGenre}"/>
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('musicGenre', '<c:out value="${club.musicGenre}"/>')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
        <div><strong><spring:message code="label.price"/>:&nbsp;</strong> <spring:message
                code="price.${club.priceRange}"/>
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('priceRange', '<c:out value="${club.priceRange}"/>')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
        <div><strong><spring:message code="form.description"/>:&nbsp;</strong> <c:choose> <c:when
                test="${not empty club.description and fn:length(club.description) > 100}"> <c:out
                value="${fn:substring(club.description, 0, 100)}"/>... </c:when> <c:otherwise> <c:out
                value="${club.description}"/> </c:otherwise> </c:choose>
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('description', '<c:out value="${club.description}"/>')"><i
                    class="fa-solid fa-pen"></i></button>
        </div>
    </div>
    <h2><spring:message code="menu.menu"/></h2>
    <table class="styled-table">
        <thead>
        <tr>
            <th><spring:message code="label.product"/></th>
            <th><spring:message code="form.description"/></th>
            <th><spring:message code="label.price"/></th>
            <th><spring:message code="label.type"/></th>
            <th></th>
        </tr>
        </thead>
        <tbody><c:if test="${empty menu}">
            <tr>
                <td colspan="5" class="styled-table-th"
                    style="text-align:center; background:#3c3c3c; color:#fff; font-weight:bold;"><spring:message
                        code="error.noproducts"/></td>
            </tr>
        </c:if> <c:forEach var="product" items="${menu}">
            <tr>
                <td><c:out value="${product.productName}"/></td>
                <td><c:out value="${product.description}"/></td>
                <td><c:out value="${product.basePrice}"/></td>
                <td><spring:message code="productType.${product.productType}"/></td>
                <td>
                    <div style="display: flex; justify-content: right; margin-right: 12px">
                        <button type="button" class="btn btn-success btn-edit-small"
                                data-product-id="${product.productId}"
                                data-product-name="<c:out value='${product.productName}'/>"
                                data-product-description="<c:out value='${product.description}'/>"
                                data-product-price="${product.basePrice}" data-product-type="${product.productType}"
                                onclick="openProductEditModal(this)" title="Editar producto"><i
                                class="fa-solid fa-pen"></i></button>
                        <button type="button" class="btn btn-danger btn-edit-small"
                                onclick="openDeleteModal('${product.productId}')"><i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        </c:forEach></tbody>
    </table>
    <spring:message code="placeholder.name" var="phProductName"/>
    <spring:message code="placeholder.description" var="phDescription"/>
    <spring:message code="placeholder.price" var="phPrice"/>
    <form:form method="post" modelAttribute="productForm" action="${pageContext.request.contextPath}/clubs/addProduct" class="add-product-bar">
        <form:hidden path="clubId" value="${club.clubId}" />
        <form:input path="productName" cssClass="add-product-name" placeholder="${phProductName}" />
        <form:errors path="productName" cssClass="formError"/>

        <form:input path="description" cssClass="add-product-input" placeholder="${phDescription}" />
        <form:errors path="description" cssClass="formError"/>

        <form:input path="basePrice" type="number" cssClass="add-product-input" placeholder="${phPrice}" step="0.01" />
        <form:errors path="basePrice" cssClass="formError"/>

        <form:select path="productType" cssClass="input-text">
            <form:option value="ALCOHOLICA" label="Alcohólica"/>
            <form:option value="NO_ALCOHOLICA" label="No alcohólica"/>
        </form:select>
        <form:errors path="productType" cssClass="formError"/>

        <button type="submit" class="btn btn-success btn-edit-small"><i class="fa-solid fa-plus"></i></button>
    </form:form>

    <h2><spring:message code="button.tables"/></h2>
    <table class="styled-table">
        <thead>
        <tr>
            <th><spring:message code="label.number"/></th>
            <th><spring:message code="label.capacity"/></th>
            <th><spring:message code="label.price"/></th>
            <th><spring:message code="label.discount"/> (%)</th>
            <th></th>
        </tr>
        </thead>
        <tbody><c:if test="${empty tables}">
            <tr>
                <td colspan="5" class="styled-table-th"
                    style="text-align:center; background:#3c3c3c; color:#fff; font-weight:bold;"><spring:message
                        code="error.notables"/></td>
            </tr>
        </c:if> <c:forEach var="table" items="${tables}">
            <tr>
                <td><c:out value="${table.tableNumber}"/></td>
                <td><c:out value="${table.capacity}"/></td>
                <td><c:out value="${table.price}"/></td>
                <td><c:out value="${table.discount}"/></td>
                <td>
                    <div style="display: flex; justify-content: right; margin-right: 12px">
                        <button type="button" class="btn btn-success btn-edit-small"
                                onclick="openTableEditModal('${table.tableId}', '${table.tableNumber}', '${table.capacity}', '${table.price}', '${table.discount}')">
                            <i class="fa-solid fa-pen"></i></button>
                        <button type="button" class="btn btn-danger btn-edit-small"
                                onclick="openTableDeleteModal('${table.tableId}')"
                                title="<spring:message code='button.delete'/>"><i class="fa-solid fa-trash"></i>
                        </button>
                    </div>
                </td>
            </tr>
        </c:forEach></tbody>
    </table>
    <spring:message code="placeholder.tableNumber" var="phTableNumber"/> <spring:message code="placeholder.capacity"
                                                                                         var="phCapacity"/>
    <spring:message code="placeholder.discount" var="phDiscount"/>
    <form:form method="post" modelAttribute="tableForm" action="${pageContext.request.contextPath}/clubs/addTable" class="add-product-bar">
        <form:hidden path="clubId" value="${club.clubId}" />

        <form:input path="tableNumber" type="number" cssClass="add-product-name" placeholder="${phTableNumber}" />
        <form:errors path="tableNumber" cssClass="formError"/>

        <form:input path="capacity" type="number" cssClass="add-product-input" placeholder="${phCapacity}" />
        <form:errors path="capacity" cssClass="formError"/>

        <form:input path="price" type="number" cssClass="add-product-input" placeholder="${phPrice}" step="0.01"/>
        <form:errors path="price" cssClass="formError"/>

        <form:input path="discount" type="number" cssClass="add-product-input" placeholder="${phDiscount}" step="0.01"/>
        <form:errors path="discount" cssClass="formError"/>

        <button type="submit" class="btn btn-success btn-edit-small"><i class="fa-solid fa-plus"></i></button>
    </form:form>

    <c:if test="${param.err == 'dup'}">
        <div style="color:#e53935; font-size:0.9rem;"><spring:message code="error.table.duplicate" text="Ya existe una mesa con ese número."/></div>
    </c:if>
</div>
<!-- Modales -->
<div id="editModal" class="modal">
    <div class="modal-content"><span class="modal-close" onclick="closeEditModal()">&times;</span>
        <form method="post" action="<c:url value='/clubs/editField' />"><input type="hidden" name="clubId"
                                                                               value="${club.clubId}"/> <input
                type="hidden" name="field" id="editFieldName" value=""/> <label><spring:message
                code="label.newValue"/>:</label> <input type="text" name="value" id="editFieldValue" required/> <select
                name="value" id="zoneSelect" class="hidden"> <c:forEach var="z" items="${zones}">
            <option value="${z.dbValue}" ${zone == z.dbValue ? 'selected' : ''}><spring:message
                    code="zone.${z.dbValue}"/></option>
        </c:forEach> </select> <select name="value" id="musicGenreSelect" class="hidden"> <c:forEach var="g"
                                                                                                     items="${genres}">
            <option value="${g}" ${genre == g ? 'selected' : ''}><spring:message code="genre.${g}"/></option>
        </c:forEach> </select> <select name="value" id="priceSelect" class="hidden"> <c:forEach var="p"
                                                                                                items="${prices}">
            <option value="${p}" ${price == p ? 'selected' : ''}><spring:message code="price.${p}"/></option>
        </c:forEach> </select>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeEditModal()"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form>
    </div>
</div>
<div id="productEditModal" class="modal">
    <div class="modal-content"><span class="modal-close" onclick="closeProductEditModal()">&times;</span>
        <h2>Editar producto</h2>
        <form method="post" id="productEditForm" action="<c:url value='/clubs/editProduct' />"
              style="display:flex; flex-direction:column; gap:18px;"><input type="hidden" name="productId"
                                                                            id="editProductId"/>
            <div style="display:flex; flex-direction:column; gap:6px;"><label for="editProductName">Nombre:</label>
                <input type="text" name="productName" id="editProductName" required/></div>
            <div style="display:flex; flex-direction:column; gap:6px;"><label
                    for="editProductDescription">Descripción:</label> <input type="text" name="description"
                                                                             id="editProductDescription"/></div>
            <div style="display:flex; flex-direction:column; gap:6px;"><label for="editProductPrice">Precio:</label>
                <input type="number" name="basePrice" id="editProductPrice" step="0.01" required/></div>
            <div style="display:flex; flex-direction:column; gap:6px;"><label for="editProductType">Tipo:</label>
                <select name="productType" id="editProductType" required>
                    <option value="alcoholicas">Alcoholicas</option>
                    <option value="noAlcoholicas">No alcoholicas</option>
                </select></div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeProductEditModal()">Cancelar</button>
                <button type="submit" class="btn btn-success">Actualizar</button>
            </div>
        </form>
    </div>
</div>
<!-- Modal editar imagen -->
<div id="editImageModal" class="modal">
    <div class="modal-content"><span class="modal-close"
                                     onclick="document.getElementById('editImageModal').style.display='none'">&times;</span>
        <h3><spring:message code="title.editImage"/></h3>
        <form method="post" action="<c:url value='/clubs/club/${club.clubId}/photos' />" enctype="multipart/form-data">
            <input type="file" name="file" accept="image/*" required/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary"
                        onclick="document.getElementById('editImageModal').style.display='none'"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
            </div>
        </form>
    </div>
</div>
<div id="deleteModal" class="modal">
    <div class="modal-content"><span class="modal-close" onclick="closeDeleteModal()">&times;</span>
        <h3><spring:message code="confirm.deleteProduct"/></h3>
        <form id="deleteForm" method="post" action="<c:url value='/clubs/deleteProduct' />"><input type="hidden"
                                                                                                   name="productId"
                                                                                                   id="deleteProductId"
                                                                                                   value=""/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeDeleteModal()"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-danger"><spring:message code="button.delete"/></button>
            </div>
        </form>
    </div>
</div>
<div id="tableEditModal" class="modal">
    <div class="modal-content"><span class="modal-close" onclick="closeTableEditModal()"
                                     style="color:#fff;">&times;</span>
        <h2><spring:message code="title.editTable"/></h2>
        <form method="post" id="tableEditForm" style="display:flex; flex-direction:column; gap:18px;"><input
                type="hidden" name="clubId" value="${club.clubId}"/>
            <div style="display:flex; flex-direction:column; gap:6px;"><label for="editTableNumber"
                                                                              style="font-weight:500;"><spring:message
                    code="label.number"/>:</label> <input type="number" name="tableNumber" id="editTableNumber" required
                                                          class="input-price"/></div>
            <div style="display:flex; flex-direction:column; gap:6px;"><label for="editTableCapacity"><spring:message
                    code="label.capacity"/>:</label> <input type="number" name="capacity" id="editTableCapacity"
                                                            required class="input-price"/></div>
            <div style="display:flex; flex-direction:column; gap:6px;"><label for="editTablePrice"><spring:message
                    code="label.price"/>:</label> <input type="number" name="price" id="editTablePrice" step="0.01"
                                                         required class="input-price"/></div>
            <div style="display:flex; flex-direction:column; gap:6px;"><label for="editTableDiscount"><spring:message
                    code="label.discount"/>:</label> <input type="number" name="discount" id="editTableDiscount"
                                                            step="0.01" min="0" max="100" required class="input-price"/>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeTableEditModal()"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.update"/></button>
            </div>
        </form>
    </div>
</div>
<div id="tableEditModal" class="modal">
    <div class="modal-content"><span class="modal-close" onclick="closeTableEditModal()">&times;</span>
        <h3><spring:message code="title.editTable"/></h3>
        <form id="tableEditForm" method="post">
            <div><label for="editTableNumber"><spring:message code="label.number"/>:</label> <input type="number"
                                                                                                    id="editTableNumber"
                                                                                                    name="tableNumber"
                                                                                                    required></div>
            <div><label for="editTableCapacity"><spring:message code="label.capacity"/>:</label> <input type="number"
                                                                                                        id="editTableCapacity"
                                                                                                        name="capacity"
                                                                                                        required></div>
            <div><label for="editTablePrice"><spring:message code="label.price"/>:</label> <input type="number"
                                                                                                  id="editTablePrice"
                                                                                                  name="price"
                                                                                                  step="0.01" required>
            </div>
            <div><label for="editTableDiscount"><spring:message code="label.discount"/>:</label> <input type="number"
                                                                                                        id="editTableDiscount"
                                                                                                        name="discount"
                                                                                                        min="0"
                                                                                                        max="100"></div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeTableEditModal()"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-success"><spring:message code="button.update"/></button>
            </div>
        </form>
    </div>
</div>
<!-- Modal eliminar mesa -->
<div id="tableDeleteModal" class="modal">
    <div class="modal-content"><span class="modal-close" onclick="closeTableDeleteModal()">&times;</span>
        <h3><spring:message code="confirm.deleteTable"/></h3>
        <form method="post" id="tableDeleteForm"><input type="hidden" name="clubId" value="${club.clubId}"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeTableDeleteModal()"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-danger"><spring:message code="button.delete"/></button>
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
            el.style.display = 'none';
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
            if (field === 'clubName' || field === 'location') {
                input.setAttribute('maxlength', '100');
            } else {
                input.removeAttribute('maxlength');
            }
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
        document.getElementById('tableEditForm').action = '${pageContext.request.contextPath}/clubs/tables/' + tableId + '/edit';
    }

    function closeTableEditModal() {
        document.getElementById('tableEditModal').style.display = 'none';
    }

    function openTableDeleteModal(tableId) {
        document.getElementById('tableDeleteModal').style.display = 'flex';
        document.getElementById('tableDeleteForm').action = '${pageContext.request.contextPath}/clubs/tables/' + tableId + '/delete';
    }

    function closeTableDeleteModal() {
        document.getElementById('tableDeleteModal').style.display = 'none';
    }

    function openProductEditModal(btn) {
        const d = btn.dataset;
        document.getElementById('productEditModal').style.display = 'flex';
        document.getElementById('editProductId').value = d.productId;
        document.getElementById('editProductName').value = d.productName;
        document.getElementById('editProductDescription').value = d.productDescription;
        document.getElementById('editProductPrice').value = d.productPrice;
        document.getElementById('editProductType').value = d.productType;
    }

    function closeProductEditModal() {
        document.getElementById('productEditModal').style.display = 'none';
    }
</script>
</body>
</html>