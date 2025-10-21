<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

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
            onclick="window.location.href='<c:url value="/admin/clubs/activity?userId=${club.contactInfo.id}"/>'"><i
            class="fa-solid fa-arrow-left"></i></button>
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
        <div>
            <strong><spring:message code="placeholder.cbu"/>:&nbsp;</strong>
            <c:out value="${club.cbu}"/>
            <button type="button" class="btn btn-success btn-edit-small"
                    onclick="openEditModal('cbu', '<c:out value="${club.cbu}"/>')">
                <i class="fa-solid fa-pen"></i>
            </button>
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
                <td colspan="5" class="styled-table-th" style="text-align:center; background:#3c3c3c; color:#fff; font-weight:bold;"><spring:message code="error.noproducts"/></td>
            </tr>
        </c:if> <c:forEach var="product" items="${menu}">
            <tr>
                <td><c:out value="${product.productName}"/></td>
                <td><c:out value="${product.description}"/></td>
                <td>$<fmt:formatNumber value="${product.basePrice}" type="number" groupingUsed="true" maxFractionDigits="0"/></td>
                <td><spring:message code="productType.${product.productType}"/></td>
                <td>
                    <div style="display: flex; justify-content: right; margin-right: 12px">
                        <button type="button" class="btn btn-success btn-edit-small"
                                data-product-id="${product.productId}"
                                data-product-name="<c:out value='${product.productName}'/>"
                                data-product-description="<c:out value='${product.description}'/>"
                                data-product-price="<fmt:formatNumber value='${product.basePrice}' type='number' groupingUsed='false' maxFractionDigits='0'/>"
                                data-product-type="${product.productType}"
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
    <spring:message code="placeholder.menu.name" var="phProductName"/>
    <spring:message code="placeholder.menu.description" var="phDescription"/>
    <spring:message code="placeholder.price" var="phPrice"/>
    <form:form method="post" modelAttribute="productForm" action="${pageContext.request.contextPath}/clubs/products/addProduct" class="add-product-bar">
        <form:hidden path="clubId" value="${club.clubId}" />

        <div class="form-group">
        <form:input path="productName" cssClass="add-product-name" placeholder="${phProductName}" />
        <form:errors path="productName" cssClass="formError"/>
        </div>

    <div class="form-group">

    <form:input path="description" cssClass="add-product-input" placeholder="${phDescription}" />
        <form:errors path="description" cssClass="formError"/>
    </div>

        <div class="form-group">
        <form:input path="basePrice" type="number" cssClass="add-product-input" placeholder="${phPrice}" />
        <form:errors path="basePrice" cssClass="formError"/>
        </div>

        <div class="form-group">
            <form:select path="productType" cssClass="input-text">
                <form:option value="ALCOHOLICA" label="Alcohólica"/>
                <form:option value="NO_ALCOHOLICA" label="No alcohólica"/>
            </form:select>
            <form:errors path="productType" cssClass="formError"/>
        </div>

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
                <td colspan="5" class="styled-table-th" style="text-align:center; background:#3c3c3c; color:#fff; font-weight:bold;"><spring:message
                        code="error.notables"/></td>
            </tr>
        </c:if> <c:forEach var="table" items="${tables}">
            <tr>
                <td><c:out value="${table.tableNumber}"/></td>
                <td><c:out value="${table.capacity}"/></td>
                <td>$<fmt:formatNumber value="${table.price}" type="number" groupingUsed="true" maxFractionDigits="0"/></td>
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
    <spring:message code="placeholder.tableNumber" var="phTableNumber"/> <spring:message code="placeholder.capacity" var="phCapacity"/>
    <spring:message code="placeholder.discount" var="phDiscount"/>
    <form:form method="post" modelAttribute="tableForm" action="${pageContext.request.contextPath}/clubs/tables/addTable" class="add-product-bar">
        <form:hidden path="clubId" value="${club.clubId}" />
        <div class="form-group">
            <form:input path="tableNumber" type="number" cssClass="add-product-name" placeholder="${phTableNumber}" />
            <form:errors path="tableNumber" cssClass="formError"/>
        </div>
        <div class="form-group">
            <form:input path="capacity" type="number" cssClass="add-product-input" placeholder="${phCapacity}" />
            <form:errors path="capacity" cssClass="formError"/>
        </div>
        <div class="form-group">
            <form:input path="price" type="number" cssClass="add-product-input" placeholder="${phPrice}"/>
            <form:errors path="price" cssClass="formError"/>
        </div>
        <div class="form-group">
            <form:input path="discount" type="number" cssClass="add-product-input" placeholder="${phDiscount}"/>
            <form:errors path="discount" cssClass="formError"/>
        </div>
        <button type="submit" class="btn btn-success btn-edit-small"><i class="fa-solid fa-plus"></i></button>
    </form:form>
    <c:if test="${param.err == 'dup'}">
        <div style="color:#e53935; font-size:0.9rem;"><spring:message code="error.table.duplicate" text="Ya existe una mesa con ese número."/></div>
    </c:if>
</div>
<div id="editModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeEditModal()">&times;</span>
        <form:form modelAttribute="clubHandlerForm" method="post" action="${pageContext.request.contextPath}/clubs/editClub"
                   cssStyle="display:flex; flex-direction:column; gap:18px;">
            <form:hidden path="clubId" value="${club.clubId}" />
            <form:hidden path="editedField" id="editedField" />

            <div id="editField_clubName" style="display:none;">
                <label><spring:message code="form.name"/>:</label>
                <form:input path="name" id="editFieldValue_clubName" cssClass="input-modal"/>
                <form:errors path="name" cssClass="formError"/>
            </div>

            <div id="editField_location" style="display:none;">
                <label><spring:message code="form.location"/>:</label>
                <form:input path="location" id="editFieldValue_location" cssClass="input-modal"/>
                <form:errors path="location" cssClass="formError"/>
            </div>

            <div id="editField_zone" style="display:none;">
                <label><spring:message code="label.zone"/>:</label>
                <form:select path="zone" id="zoneSelectModal" cssClass="input-modal">
                    <c:forEach var="z" items="${zones}">
                        <form:option value="${z.dbValue}">
                            <spring:message code="zone.${z.dbValue}"/>
                        </form:option>
                    </c:forEach>
                </form:select>
                <form:errors path="zone" cssClass="formError"/>
            </div>

            <div id="editField_musicGenre" style="display:none;">
                <label><spring:message code="label.musicGenre"/>:</label>
                <form:select path="musicGenre" id="musicGenreSelectModal" cssClass="input-modal">
                    <c:forEach var="g" items="${genres}">
                        <form:option value="${g}">
                            <spring:message code="genre.${g}"/>
                        </form:option>
                    </c:forEach>
                </form:select>
                <form:errors path="musicGenre" cssClass="formError"/>
            </div>

            <div id="editField_priceRange" style="display:none;">
                <label><spring:message code="label.price"/>:</label>
                <form:select path="priceRange" id="priceSelectModal" cssClass="input-modal">
                    <c:forEach var="p" items="${prices}">
                        <form:option value="${p}">
                            <spring:message code="price.${p}"/>
                        </form:option>
                    </c:forEach>
                </form:select>
                <form:errors path="priceRange" cssClass="formError"/>
            </div>

            <div id="editField_description" style="display:none;">
                <label><spring:message code="form.description"/>:</label>
                <form:textarea path="description" id="editFieldValue_description" cssClass="input-modal textarea-modal" rows="6"/>
                <form:errors path="description" cssClass="formError"/>
            </div>

            <div id="editField_cbu" style="display:none;">
                <label><spring:message code="placeholder.cbu"/>:</label>
                <form:input path="cbu" id="editFieldValue_cbu" cssClass="input-modal"/>
                <form:errors path="cbu" cssClass="formError"/>
            </div>

            <div class="modal-actions" style="margin-top:12px;">
                <button type="button" class="btn btn-secondary" onclick="closeEditModal()">
                    <spring:message code="button.cancel"/>
                </button>
                <button type="submit" class="btn btn-success">
                    <spring:message code="button.save"/>
                </button>
            </div>
        </form:form>
    </div>
</div>


<div id="productEditModal" class="modal">
    <div class="modal-content"><span class="modal-close" onclick="closeProductEditModal()">&times;</span>
        <h2><spring:message code="title.editProduct"/></h2>
        <form:form method="post" modelAttribute="editProductForm" id="productEditForm"
                   action="${pageContext.request.contextPath}/clubs/products/editProduct"
                   cssStyle="display:flex; flex-direction:column; gap:18px;">

            <form:hidden path="clubId" value="${club.clubId}"/>
            <form:hidden path="productId" id="editProductId"/>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editProductName"><spring:message code="form.name"/>:</label>
                <form:input path="productName" id="editProductName"/>
                <form:errors path="productName" cssClass="formError"/>
            </div>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editProductDescription"><spring:message code="form.description"/>:</label>
                <form:input path="description" id="editProductDescription"/>
                <form:errors path="description" cssClass="formError"/>
            </div>
            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editProductPrice"><spring:message code="label.price"/>:</label>
                <form:input path="basePrice" type="number" id="editProductPrice"/>
                <form:errors path="basePrice" cssClass="formError"/>
            </div>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editProductType"><spring:message code="label.type"/>:</label>
                <form:select path="productType" id="editProductType">
                    <form:option value="ALCOHOLICA" label="Alcohólica"/>
                    <form:option value="NO_ALCOHOLICA" label="No alcohólica"/>
                </form:select>
                <form:errors path="productType" cssClass="formError"/>
            </div>

            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeProductEditModal()">
                    <spring:message code="button.cancel"/>
                </button>
                <button type="submit" class="btn btn-success"><spring:message code="button.update"/></button>
            </div>
        </form:form>
    </div>
</div>
<!-- Modal editar imagen -->
<div id="editImageModal" class="modal">
    <div class="modal-content"><span class="modal-close"
                                     onclick="document.getElementById('editImageModal').style.display='none'">&times;</span>
        <h3><spring:message code="title.editImage"/></h3>
        <form method="post" action="<c:url value='/clubs/club/${club.clubId}/photos' />" enctype="multipart/form-data">
            <input type="file" name="file" accept="image/*"/> <!-- IMAGEN ACEPTA GIF -->
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
        <form id="deleteForm" method="post" action="<c:url value='/clubs/products/deleteProduct' />"><input type="hidden" name="productId" id="deleteProductId" value=""/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeDeleteModal()"><spring:message
                        code="button.cancel"/></button>
                <button type="submit" class="btn btn-danger"><spring:message code="button.delete"/></button>
            </div>
        </form>
    </div>
</div>
<div id="tableEditModal" class="modal">
    <div class="modal-content"><span class="modal-close" onclick="closeTableEditModal()" style="color:#fff;">&times;</span>
        <h2><spring:message code="title.editTable"/></h2>
        <form:form method="post" modelAttribute="editTableForm" id="tableEditForm"
                   cssStyle="display:flex; flex-direction:column; gap:18px;">
            <form:hidden path="clubId" value="${club.clubId}"/>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editTableNumber"><spring:message code="label.number"/>:</label>
                <form:input path="tableNumber" type="number" id="editTableNumber"/>
                <form:errors path="tableNumber" cssClass="formError"/>
            </div>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editTableCapacity"><spring:message code="label.capacity"/>:</label>
                <form:input path="capacity" type="number" id="editTableCapacity"/>
                <form:errors path="capacity" cssClass="formError"/>
            </div>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editTablePrice"><spring:message code="label.price"/>:</label>
                <form:input path="price" type="number" id="editTablePrice"/>
                <form:errors path="price" cssClass="formError"/>
            </div>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editTableDiscount"><spring:message code="label.discount"/>:</label>
                <form:input path="discount" type="number" id="editTableDiscount"/>
                <form:errors path="discount" cssClass="formError"/>
            </div>

            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeTableEditModal()">
                    <spring:message code="button.cancel"/>
                </button>
                <button type="submit" class="btn btn-success"><spring:message code="button.update"/></button>
            </div>
        </form:form>
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
<div id="deleteModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeDeleteModal()">&times;</span>
        <h3><spring:message code="confirm.deleteProduct"/></h3>
        <p style="margin-top:8px;">¿Estás seguro que querés eliminar este producto? Esta acción no se puede deshacer.</p>

        <form id="deleteForm" method="post" action="<c:url value='/clubs/products/deleteProduct' />">
            <input type="hidden" name="productId" id="deleteProductId" value=""/>
            <div class="modal-actions" style="margin-top:18px; display:flex; justify-content:space-between;">
                <button type="button" class="btn btn-secondary" onclick="closeDeleteModal()">
                    <spring:message code="button.cancel"/>
                </button>
                <button type="submit" class="btn btn-danger">
                    <spring:message code="button.delete"/>
                </button>
            </div>
        </form>
    </div>
</div>
<div id="tableDeleteModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeTableDeleteModal()">&times;</span>
        <h3><spring:message code="confirm.deleteTable"/></h3>
        <form method="post" id="tableDeleteForm">
            <input type="hidden" name="clubId" value="${club.clubId}"/>
            <input type="hidden" name="tableId" id="deleteTableId" value=""/>
            <div class="modal-actions" style="margin-top:18px; display:flex; justify-content:space-between;">
                <button type="button" class="btn btn-secondary" onclick="closeTableDeleteModal()">
                    <spring:message code="button.cancel"/>
                </button>
                <button type="submit" class="btn btn-danger">
                    <spring:message code="button.delete"/>
                </button>
            </div>
        </form>
    </div>
</div>
<script>
    function openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (modal) {
            modal.style.display = 'flex';
        }
    }
    function openDeleteModal(productId) {
        document.getElementById('deleteProductId').value = productId;
        document.getElementById('deleteModal').style.display = 'flex';
    }

    function closeDeleteModal() {
        document.getElementById('deleteProductId').value = '';
        document.getElementById('deleteModal').style.display = 'none';
    }

    function openEditModal(field, value) {
        document.getElementById('editModal').style.display = 'flex';
        document.getElementById('editedField').value = field;

        // ocultar todos los bloques
        const fields = ['clubName','location','zone','musicGenre','priceRange','description', 'cbu'];
        fields.forEach(f => {
            const el = document.getElementById('editField_' + f);
            if (el) el.style.display = 'none';
        });

        // mostrar el bloque que corresponde
        const visible = document.getElementById('editField_' + field);
        if (visible) visible.style.display = 'block';

        // setear valor en el input/select visible
        // intentamos encontrar input/textarea/select correspondiente
        const inputIds = {
            'clubName': 'editFieldValue_clubName',
            'location': 'editFieldValue_location',
            'description': 'editFieldValue_description',
            'zone': 'zoneSelectModal',
            'musicGenre': 'musicGenreSelectModal',
            'priceRange': 'priceSelectModal',
            'cbu': 'editFieldValue_cbu'
        };
        const id = inputIds[field];
        if (id) {
            const el = document.getElementById(id);
            if (el) {
                el.value = (value === undefined || value === null) ? '' : value;
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
        clearFormErrors('tableEditModal');
    }

    function openTableDeleteModal(tableId) {
        document.getElementById('tableDeleteModal').style.display = 'flex';
        document.getElementById('deleteTableId').value = tableId;
        document.getElementById('tableDeleteForm').action = '${pageContext.request.contextPath}/clubs/tables/' + tableId + '/delete';
    }

    function closeTableDeleteModal() {
        document.getElementById('deleteTableId').value = '';
        document.getElementById('tableDeleteModal').style.display = 'none';
    }

    function openProductEditModal(btn) {
        const d = btn.dataset;
        document.getElementById('productEditModal').style.display = 'flex';
        document.getElementById('editProductId').value = d.productId;
        document.getElementById('editProductName').value = d.productName;
        document.getElementById('editProductDescription').value = d.productDescription;
        document.getElementById('editProductPrice').value =d.productPrice;
        document.getElementById('editProductType').value = d.productType;
    }

    function closeProductEditModal() {
        document.getElementById('productEditModal').style.display = 'none';
        clearFormErrors('productEditModal');
    }

    document.addEventListener('DOMContentLoaded', () => {
        <c:if test="${not empty editField}">
        const field = '${editField}';
        if (field === 'Product') {
            openModal('productEditModal');
        } else if (field === 'Table') {
            openModal('tableEditModal');
        } else {
            openEditModal(field, '${fn:escapeXml(editFieldValue)}');
        }
        </c:if>

        document.querySelectorAll("form.add-product-bar").forEach(form => {
            form.addEventListener("submit", (e) => {
                const submitBtn = form.querySelector("button[type='submit']");
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = "<i class='fa fa-spinner fa-spin'></i>"; // opcional
                }
            });
        });
    });

    function clearFormErrors(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        modal.querySelectorAll('.formError').forEach(el => {
            el.innerHTML = '';
        });
    }

    document.addEventListener('DOMContentLoaded', function() {
        document.querySelectorAll('input[type="number"]').forEach(function(input) {
            input.addEventListener('keydown', function(e) {
                if (e.key === '-' || e.key === 'Subtract') {
                    e.preventDefault();
                }
            });
            input.addEventListener('input', function(e) {
                if (this.value.includes('-')) {
                    this.value = this.value.replace(/-/g, '');
                }
            });
        });
    });

</script>
</body>
</html>