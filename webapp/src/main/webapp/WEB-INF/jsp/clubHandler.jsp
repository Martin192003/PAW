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
    <link rel="stylesheet" href="<c:url value='/resources/css/carousel.css' />" />
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
    <h2><spring:message code="layout.title" text="Layout de mesas"/></h2>
    <div class="form-group">
        <c:choose>
            <c:when test="${not empty tableLayoutUrl}">
                <div class="table-layout-container" style="text-align:center; margin-bottom:12px;">
                    <img src="<c:url value='${tableLayoutUrl}' />" alt="Layout de mesas"
                         style="max-width:100%; border-radius:8px; border:1px solid #333;"/>
                </div>
            </c:when>
            <c:otherwise>
                <p><spring:message code="layout.available" text="No hay layout por el momento"/></p>
            </c:otherwise>
        </c:choose>

        <button type="button" class="btn btn-success btn-edit-small"
                onclick="document.getElementById('editLayoutModal').style.display='flex'">
            <i class="fa-solid fa-pen"></i>
        </button>
    </div>
    <!-- Sección de Galería de Fotos -->
    <div class="photo-section">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
            <h2><i class="fa-solid fa-images"></i> <spring:message code="photos.gallery" text="Galería de Fotos"/></h2>
            <div>
                <button type="button" class="btn btn-info" onclick="openAddCarouselPhotoModal()">
                    <i class="fa-solid fa-plus"></i> Add Carousel Photo
                </button>
                <button type="button" class="btn btn-success" onclick="openAddPhotosModal()">
                    <i class="fa-solid fa-plus"></i> <spring:message code="button.addPhotos" text="Agregar Fotos"/>
                </button>
            </div>
        </div>
        <h2><i class="fa-solid fa-images"></i> <spring:message code="photos.gallery" text="Galería de Fotos"/></h2>


        <c:choose>
            <c:when test="${not empty carouselPhotos}">
                <div class="photo-carousel">
                    <div class="carousel-container">
                        <div class="carousel-track" id="carouselTrack">
                            <c:forEach var="photo" items="${carouselPhotos}" varStatus="status">
                                <div class="carousel-slide">
                                    <img src="<c:url value='${photo.url}'/>"
                                         alt="<c:out value='${club.clubName}'/> - Foto ${status.index + 1}"
                                         loading="lazy">
                                </div>
                            </c:forEach>
                        </div>

                        <c:if test="${fn:length(carouselPhotos) > 1}">
                            <button class="carousel-btn carousel-prev" id="prevBtn">
                                <i class="fa-solid fa-chevron-left"></i>
                            </button>
                            <button class="carousel-btn carousel-next" id="nextBtn">
                                <i class="fa-solid fa-chevron-right"></i>
                            </button>

                            <div class="carousel-counter" id="carouselCounter">
                                1 / ${fn:length(carouselPhotos)}
                            </div>
                        </c:if>
                    </div>


                    <c:if test="${fn:length(carouselPhotos) > 1}">
                        <div class="carousel-indicators" id="carouselIndicators">
                            <c:forEach begin="0" end="${fn:length(carouselPhotos) - 1}" var="i">
                                <button class="carousel-dot ${i == 0 ? 'active' : ''}"></button>
                            </c:forEach>
                        </div>
                    </c:if>
                </div>
            </c:when>
            <c:otherwise>
                <div class="no-photos">
                    <i class="fa-solid fa-camera"></i>
                    <p><spring:message code="photos.noPhotos" text="No hay fotos disponibles para la galería"/></p>
                </div>
            </c:otherwise>
        </c:choose>
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
        <div style="color:#e53935; font-size:0.9rem;"><spring:message code="error.table.duplicate"/></div>
    </c:if>

    <h2><spring:message code="label.events" text="Eventos"/></h2>

    <table class="styled-table">
        <thead>
        <tr>
            <th><spring:message code="form.name"/></th>
            <th><spring:message code="form.date"/></th>
            <th><spring:message code="form.description"/></th>
            <th><spring:message code="form.eventImage"/></th>
            <th></th>
        </tr>
        </thead>
        <tbody>
        <c:if test="${empty events}">
            <tr>
                <td colspan="5" class="styled-table-th" style="text-align:center; background:#3c3c3c; color:#fff; font-weight:bold;">
                    <spring:message code="error.noevents"/>
                </td>
            </tr>
        </c:if>
        <c:forEach var="e" items="${events}">
            <tr>
                <td><c:out value="${e.eventName}"/></td>
                <td><c:out value="${e.date}"/></td>
                <td>
                    <c:choose>
                        <c:when test="${not empty e.description and fn:length(e.description) > 80}">
                            <c:out value="${fn:substring(e.description, 0, 80)}"/>...
                        </c:when>
                        <c:otherwise>
                            <c:out value="${e.description}"/>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td style="text-align:center;">
                    <c:choose>
                        <c:when test="${not empty e.photoId}">
                            <img src="<c:url value='/photos/${e.photoId}'/>"
                                 alt="Imagen del evento"
                                 class="event-thumbnail"/>
                        </c:when>
                        <c:otherwise>
                            <span style="color: #aaa; font-size: 0.85rem;"><spring:message code="label.noImage" text="Sin imagen"/></span>
                        </c:otherwise>
                    </c:choose>
                </td>
                <td>
                    <div style="display: flex; justify-content: right; gap: 8px; margin-right: 12px">
                        <button type="button" class="btn btn-success btn-edit-small"
                                onclick="openEventEditModal('${e.eventId}', '${fn:escapeXml(e.eventName)}', '${e.date}', '${fn:escapeXml(e.description)}', '${e.photoId}')">
                            <i class="fa-solid fa-pen"></i>
                        </button>
                        <form method="post" action="<c:url value='/clubs/events/${e.eventId}/delete'/>" style="display:inline;">
                            <input type="hidden" name="clubId" value="${club.clubId}"/>
                            <button type="submit" class="btn btn-danger btn-edit-small">
                                <i class="fa-solid fa-trash"></i>
                            </button>
                        </form>
                    </div>
                </td>
            </tr>
        </c:forEach>
        </tbody>
    </table>

    <spring:message code="placeholder.event.name" var="phEventName"/>
    <spring:message code="placeholder.event.date" var="phEventDate"/>
    <spring:message code="placeholder.event.description" var="phEventDesc"/>

    <form:form method="post"
               modelAttribute="eventForm"
               action="${pageContext.request.contextPath}/clubs/events/add"
               enctype="multipart/form-data"
               cssClass="add-product-bar">

        <form:hidden path="clubId" value="${club.clubId}"/>

        <div class="form-group">
            <form:input path="eventName" cssClass="add-product-name" placeholder="${phEventName}" />
            <form:errors path="eventName" cssClass="formError"/>
        </div>

        <div class="form-group">
            <form:input path="date" type="date" cssClass="add-product-input" placeholder="${phEventDate}" />
            <form:errors path="date" cssClass="formError"/>
        </div>

        <div class="form-group">
            <form:input path="description" cssClass="add-product-input" placeholder="${phEventDesc}"/>
            <form:errors path="description" cssClass="formError"/>
        </div>

        <div class="form-group">
            <input type="file" id="editEventPhoto" name="photoId" accept="image/jpeg,image/png,image/webp"/>
        </div>

        <button type="submit" class="btn btn-success btn-edit-small">
            <i class="fa-solid fa-plus"></i>
        </button>
    </form:form>


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

<!-- Modal agregar fotos al carrusel -->
<div id="addPhotosModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeAddPhotosModal()">&times;</span>
        <h3><spring:message code="title.addPhotos" text="Agregar Fotos a la Galería"/></h3>
        <form method="post" action="<c:url value='/clubs/club/${club.clubId}/gallery' />" enctype="multipart/form-data">
            <div style="margin-bottom: 20px;">
                <label for="galleryFiles" style="display: block; margin-bottom: 8px;">
                    <spring:message code="label.selectPhotos" text="Seleccionar fotos (máximo 5):"/>
                </label>
                <input type="file" id="galleryFiles" name="files" accept="image/*" multiple style="width: 100%;"/>
                <small style="color: #ccc; font-size: 0.85em;">
                    <spring:message code="help.multiplePhotos" text="Puedes seleccionar hasta 5 fotos a la vez"/>
                </small>
            </div>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeAddPhotosModal()">
                    <spring:message code="button.cancel"/>
                </button>
                <button type="submit" class="btn btn-success">
                    <i class="fa-solid fa-upload"></i> <spring:message code="button.upload" text="Subir Fotos"/>
                </button>
            </div>
        </form>
    </div>
</div>

<!-- Modal for adding a single carousel photo -->
<div id="addCarouselPhotoModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeAddCarouselPhotoModal()">&times;</span>
        <h3>Add Photo to Carousel</h3>
        <form method="post" action="<c:url value='/clubs/club/${club.clubId}/carousel' />" enctype="multipart/form-data">
            <input type="file" name="file" accept="image/*" required />
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeAddCarouselPhotoModal()">Cancel</button>
                <button type="submit" class="btn btn-success">
                    <i class="fa-solid fa-upload"></i> Upload Photo
                </button>
            </div>
        </form>
    </div>
</div>


<div id="productEditModal" class="modal">
    <div class="modal-content"><span class="modal-close" onclick="closeProductEditModal()">&times;</span>
        <h2><spring:message code="title.editProduct"/></h2>
        <form:form method="post" modelAttribute="editProductForm" id="productEditForm"
                   action="${pageContext.request.contextPath}/clubs/products/editProduct"
                   cssStyle="display:flex; flex-direction:column; gap:18px">

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
        <form method="post" action="<c:url value='/clubs/club/${club.clubId}/cover' />" enctype="multipart/form-data">
            <input type="file" name="file" accept="image/jpeg,image/png,image/webp"/>
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
<!-- Modal editar mesa -->
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
<div id="eventEditModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeEventEditModal()" style="color:#fff;">&times;</span>
        <h2><spring:message code="title.editEvent"/></h2>

        <form:form method="post"
                   modelAttribute="editEventForm"
                   id="eventEditForm"
                   enctype="multipart/form-data"
                   cssStyle="display:flex; flex-direction:column; gap:18px;">

            <form:hidden path="clubId" value="${club.clubId}"/>
            <form:hidden path="eventId" id="editEventId"/>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editEventName"><spring:message code="form.name"/>:</label>
                <form:input path="eventName" id="editEventName" cssClass="input-modal"/>
                <form:errors path="eventName" cssClass="formError"/>
            </div>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editEventDate"><spring:message code="form.date"/>:</label>
                <form:input path="date" type="date" id="editEventDate" cssClass="input-modal"/>
                <form:errors path="date" cssClass="formError"/>
            </div>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editEventDescription"><spring:message code="form.description"/>:</label>
                <form:input path="description" id="editEventDescription" rows="4" cssClass="input-modal"/>
                <form:errors path="description" cssClass="formError"/>
            </div>

            <div style="display:flex; flex-direction:column; gap:6px;">
                <label for="editEventPhoto"><spring:message code="form.eventImage" text="Imagen del evento"/>:</label>
                <input type="file" id="editEventPhoto" name="photo" accept="image/jpeg,image/png,image/webp"/>
                <span id="editEventFileInfo" style="font-size: 0.85rem; color: #fff; margin-top: 4px;"></span>
            </div>

            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeEventEditModal()">
                    <spring:message code="button.cancel"/>
                </button>
                <button type="submit" class="btn btn-success">
                    <spring:message code="button.update"/>
                </button>
            </div>
        </form:form>
    </div>
</div>

<!-- Modal eliminar evento -->
<div id="eventDeleteModal" class="modal">
    <div class="modal-content">
        <span class="modal-close" onclick="closeEventDeleteModal()">&times;</span>
<%--        <h3><spring:message code="confirm.deleteEvent" text="¿Eliminar evento?"/></h3>--%>
        <form id="eventDeleteForm" method="post">
            <input type="hidden" name="clubId" value="${club.clubId}"/>
            <div class="modal-actions">
                <button type="button" class="btn btn-secondary" onclick="closeEventDeleteModal()"><spring:message code="button.cancel"/></button>
                <button type="submit" class="btn btn-danger"><spring:message code="button.delete"/></button>
            </div>
        </form>
    </div>
</div>

<!-- Modal subir layout -->
<div id="editLayoutModal" class="modal" style="display:none;">
    <div class="modal-content">
        <span class="modal-close" onclick="closeLayoutModal()">&times;</span>
        <h3><spring:message code="title.editTableLayout" text="Subir layout de mesas"/></h3>
        <form method="post" action="<c:url value='/clubs/club/${club.clubId}/layout' />" enctype="multipart/form-data">
            <input type="file" name="file" accept="image/*" required/>
            <div class="modal-actions" style="margin-top:12px;">
                <button type="button" class="btn btn-secondary" onclick="closeLayoutModal()">
                    <spring:message code="button.cancel"/>
                </button>
                <button type="submit" class="btn btn-success"><spring:message code="button.save"/></button>
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
        <c:if test="${not empty editField and editField ne 'Event'}">
        const field = '${editField}';
        if (field === 'Product') {
            openModal('productEditModal');
        } else if (field === 'Table') {
            openModal('tableEditModal');
        } else {
            openEditModal(field, '${fn:escapeXml(editFieldValue)}');
        }
        </c:if>
    });

    function clearFormErrors(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) return;
        modal.querySelectorAll('.formError').forEach(el => {
            el.innerHTML = '';
        });
    }

    document.getElementById('eventPhotoInput')?.addEventListener('change', function(e) {
        const fileInfo = document.getElementById('eventFileInfo');
        if (this.files && this.files[0]) {
            const file = this.files[0];
            fileInfo.textContent = file.name;
        } else {
            fileInfo.textContent = '';
        }
    });
    function openEventEditModal(eventId, eventName, eventDate, eventDesc, photoId) {
        // Open modal
        document.getElementById('eventEditModal').style.display = 'flex';

        // Set values
        document.getElementById('editEventId').value = eventId;
        document.getElementById('editEventName').value = eventName || '';
        document.getElementById('editEventDate').value = eventDate || ''; // Already in YYYY-MM-DD
        document.getElementById('editEventDescription').value = eventDesc || '';
        document.getElementById('eventEditForm').action = '${pageContext.request.contextPath}/clubs/events/' + eventId + '/edit';

        // Handle existing image
        const fileInfo = document.getElementById('editEventFileInfo');
        if (photoId) {
            // Placeholder: Derive filename (replace with actual filename if available)
            fileInfo.textContent = 'Current image: event_' + photoId + '.jpg';
        } else {
            fileInfo.textContent = 'No image';
        }
    }

    function closeEventEditModal() {
        document.getElementById('eventEditModal').style.display = 'none';
        clearFormErrors('eventEditModal');
        // limpiar inputs
        document.getElementById('editEventId').value = '';
        document.getElementById('editEventName').value = '';
        document.getElementById('editEventDate').value = '';
        document.getElementById('editEventDescription').value = '';
        document.getElementById('editEventPhotoInput').value = '';
        document.getElementById('currentEventImagePreview').innerHTML = '';
        document.getElementById('editEventFileInfo').textContent = '';
    }

    function openEventDeleteModal(eventId) {
        document.getElementById('eventDeleteModal').style.display = 'flex';
        document.getElementById('eventDeleteForm').action = '${pageContext.request.contextPath}/clubs/events/' + eventId + '/delete';
    }
    function closeEventDeleteModal() { document.getElementById('eventDeleteModal').style.display = 'none'; }


    // file input preview & info for add-event (ya tenías) y para editar evento
    document.getElementById('eventPhotoInput')?.addEventListener('change', function(e) {
        const fileInfo = document.getElementById('eventFileInfo');
        if (this.files && this.files[0]) {
            const file = this.files[0];
            fileInfo.textContent = file.name;
        } else {
            fileInfo.textContent = '';
        }
    });

    document.getElementById('editEventPhoto')?.addEventListener('change', function(e) {
        const info = document.getElementById('editEventFileInfo');
        const preview = document.getElementById('currentEventImagePreview');
        if (this.files && this.files[0]) {
            const file = this.files[0];
            info.textContent = file.name;
            // show small preview
            const reader = new FileReader();
            reader.onload = function(ev) {
                preview.innerHTML = '<img src="' + ev.target.result + '" style="max-width:80px; max-height:60px; object-fit:cover; border-radius:4px;"/>';
            };
            reader.readAsDataURL(file);
        } else {
            info.textContent = '';
            preview.innerHTML = '';
        }
    });
    function closeLayoutModal() {
        const modal = document.getElementById('editLayoutModal');
        if (!modal) return;
        modal.style.display = 'none';
        const fileInput = modal.querySelector('input[type="file"]');
        if (fileInput) fileInput.value = '';
        // reutiliza la función existente que limpia errores si está definida
        try { clearFormErrors('editLayoutModal'); } catch(e){}
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

    function openAddCarouselPhotoModal() {
        document.getElementById('addCarouselPhotoModal').style.display = 'flex';
    }
    function closeAddCarouselPhotoModal() {
        document.getElementById('addCarouselPhotoModal').style.display = 'none';
    }

    function openAddPhotosModal() {
        document.getElementById('addPhotosModal').style.display = 'flex';
    }

    function closeAddPhotosModal() {
        document.getElementById('addPhotosModal').style.display = 'none';
    }

    // Optional: Add file validation
    document.addEventListener('DOMContentLoaded', function() {
        const fileInput = document.getElementById('galleryFiles');
        if (fileInput) {
            fileInput.addEventListener('change', function(e) {
                const files = e.target.files;
                if (files.length > 5) {
                    alert('Máximo 5 fotos permitidas');
                    e.target.value = '';
                }
            });
        }
    });
    document.addEventListener('DOMContentLoaded', () => {
        const track = document.getElementById('carouselTrack');
        const prevBtn = document.getElementById('prevBtn');
        const nextBtn = document.getElementById('nextBtn');
        const counter = document.getElementById('carouselCounter');
        const indicatorsContainer = document.getElementById('carouselIndicators');

        let indicators = [];
        if (indicatorsContainer) {
            indicators = Array.from(indicatorsContainer.querySelectorAll('.carousel-dot'));
        }

        if (!track || track.children.length === 0) return;

        const slides = Array.from(track.children);
        const totalSlides = slides.length;
        let currentSlide = 0;

        // Asegurar que cada slide tenga el ancho del contenedor para un carrusel que solo muestra una foto a la vez
        function setSlideWidths(containerWidth) {
            slides.forEach(slide => {
                // * CAMBIO 1: Añadir backticks (`) alrededor de toda la expresión para concatenar 'px' *
                slide.style.width = `${containerWidth}px`;
            });
        }

        function updateCarousel() {
            const container = track.parentElement;
            const containerWidth = container.offsetWidth;

            if (containerWidth === 0) {
                requestAnimationFrame(updateCarousel);
                return;
            }

            // Ajustar el ancho de los slides en cada actualización (importante para el resize)
            setSlideWidths(containerWidth);

            // * CAMBIO 2: Añadir backticks (`) alrededor de toda la expresión para 'translateX' *
            track.style.transform = `translateX(-${currentSlide * containerWidth}px)`;

            // * CAMBIO 3: Añadir backticks (`) alrededor de toda la expresión para el contador *
            if (counter) {
                counter.textContent = `${currentSlide + 1} / ${totalSlides}`;
            }

            // Indicadores
            indicators.forEach((dot, i) => {
                dot.classList.toggle('active', i === currentSlide);
            });

            // Botones
            if (prevBtn) prevBtn.disabled = currentSlide === 0;
            if (nextBtn) nextBtn.disabled = currentSlide === totalSlides - 1;
        }

        function nextSlide() {
            if (currentSlide < totalSlides - 1) {
                currentSlide++;
                updateCarousel();
            }
        }

        function prevSlide() {
            if (currentSlide > 0) {
                currentSlide--;
                updateCarousel();
            }
        }

        function goToSlide(index) {
            if (index >= 0 && index < totalSlides) {
                currentSlide = index;
                updateCarousel();
            }
        }

        // Eventos
        indicators.forEach((dot, i) => dot.addEventListener('click', () => goToSlide(i)));
        if (nextBtn) nextBtn.addEventListener('click', nextSlide);
        if (prevBtn) prevBtn.addEventListener('click', prevSlide);

        // Teclas y Resize
        document.addEventListener('keydown', e => {
            if (e.key === 'ArrowLeft') prevSlide();
            if (e.key === 'ArrowRight') nextSlide();
        });

        window.addEventListener('resize', () => {
            requestAnimationFrame(updateCarousel);
        });

        // Carga de Imágenes
        const images = track.querySelectorAll('img');
        let loaded = 0;
        const check = () => {
            loaded++;
            if (loaded === images.length) {
                requestAnimationFrame(updateCarousel);
            }
        };

        images.forEach(img => {
            if (img.complete) check();
            else img.onload = img.onerror = check;
        });

        // Inicializar
        requestAnimationFrame(updateCarousel);
    });
</script>
</body>
</html>