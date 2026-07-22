package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.ProductType;
import ar.edu.itba.paw.model.TableState;
import ar.edu.itba.paw.model.dto.ApiFieldAccess;
import ar.edu.itba.paw.model.dto.BookingDto;
import ar.edu.itba.paw.model.dto.ProductDto;
import ar.edu.itba.paw.model.dto.ReviewDto;
import ar.edu.itba.paw.model.dto.TableDto;
import ar.edu.itba.paw.model.dto.UserDto;
import ar.edu.itba.paw.model.dto.ValidationGroups;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DtoValidationTest {

    @Test
    public void userCreateRequiresRegistrationFieldsAndValidFormats() {
        UserDto dto = validUser();
        assertTrue(DtoValidation.isValid(dto, ValidationGroups.Create.class));

        dto.setEmail("not-an-email");
        assertFalse(DtoValidation.isValid(dto, ValidationGroups.Create.class));
    }

    @Test
    public void userUpdateAllowsPartialPayloadButValidatesProvidedFields() {
        UserDto dto = new UserDto();
        assertTrue(DtoValidation.isValid(dto, ValidationGroups.Update.class));

        dto.setCellphone("letters");
        assertFalse(DtoValidation.isValid(dto, ValidationGroups.Update.class));
    }

    @Test
    public void bookingUsesDifferentCreateAndUpdateSubsets() {
        BookingDto dto = new BookingDto();
        dto.setTableId(UUID.randomUUID());
        dto.setDate(LocalDate.now().plusDays(1));
        assertTrue(DtoValidation.isValid(dto, ValidationGroups.Create.class));
        assertFalse(DtoValidation.isValid(dto, ValidationGroups.Update.class));

        dto.setStatus("CONFIRMADA");
        assertTrue(DtoValidation.isValid(dto, ValidationGroups.Update.class));
    }

    @Test
    public void productRejectsOutOfRangePriceAndRequiresAvailabilityOnUpdate() {
        ProductDto dto = new ProductDto();
        dto.setProductName("Agua");
        dto.setDescription("Botella");
        dto.setBasePrice(BigDecimal.TEN);
        dto.setProductType(ProductType.NO_ALCOHOLICA);
        assertTrue(DtoValidation.isValid(dto, ValidationGroups.Create.class));
        assertFalse(DtoValidation.isValid(dto, ValidationGroups.Update.class));

        dto.setIsAvailable(true);
        dto.setBasePrice(BigDecimal.valueOf(100_000_000));
        assertFalse(DtoValidation.isValid(dto, ValidationGroups.Update.class));
    }

    @Test
    public void tableAndReviewRangesAreEnforced() {
        TableDto table = new TableDto();
        table.setTableNumber(1);
        table.setCapacity(4);
        table.setTableState(TableState.DISPONIBLE);
        table.setPrice(BigDecimal.valueOf(1000));
        table.setDiscount(BigDecimal.valueOf(101));
        assertFalse(DtoValidation.isValid(table, ValidationGroups.Create.class));

        ReviewDto review = new ReviewDto();
        review.setRating(6);
        assertFalse(DtoValidation.isValid(review, ValidationGroups.Create.class));
        review.setRating(5);
        assertTrue(DtoValidation.isValid(review, ValidationGroups.Update.class));
    }

    @Test
    public void dtoPropertiesDeclareTheirApiDirection() throws Exception {
        ApiFieldAccess userId = UserDto.class.getMethod("getUserId").getAnnotation(ApiFieldAccess.class);
        ApiFieldAccess password = UserDto.class.getMethod("setPassword", String.class)
                .getAnnotation(ApiFieldAccess.class);

        assertEquals(ApiFieldAccess.Mode.READ_ONLY, userId.value());
        assertEquals(ApiFieldAccess.Mode.WRITE_ONLY, password.value());
        assertNull(UserDto.class.getMethod("getUsername").getAnnotation(ApiFieldAccess.class));
    }

    private UserDto validUser() {
        UserDto dto = new UserDto();
        dto.setUsername("martin");
        dto.setName("Martin");
        dto.setSurname("Perez");
        dto.setEmail("martin@example.com");
        dto.setPassword("secret1");
        dto.setCellphone("+54 11 1234-5678");
        return dto;
    }
}
