package ar.edu.itba.paw.webapp.rest;

import ar.edu.itba.paw.model.dto.BookingDto;
import ar.edu.itba.paw.model.dto.ClubDto;
import ar.edu.itba.paw.model.dto.EventDto;
import ar.edu.itba.paw.model.dto.ProductDto;
import ar.edu.itba.paw.model.dto.ReviewDto;
import ar.edu.itba.paw.model.dto.TableDto;
import ar.edu.itba.paw.model.dto.UserDto;
import ar.edu.itba.paw.model.dto.ValidationGroups;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Applies the request validation subset that corresponds to each operation.
 *
 * The DTOs live in the dependency-free model module, while validation belongs
 * to the HTTP boundary in webapp. Keeping the groups here avoids introducing a
 * new model dependency and still preserves one representation per resource.
 */
public final class DtoValidation {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^(?=.*\\d)[0-9+()\\-\\s]+$");
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal PRODUCT_PRICE_LIMIT = BigDecimal.valueOf(100_000_000);

    private DtoValidation() {
    }

    public static boolean isValid(Object dto, Class<?> group) {
        if (dto == null) {
            return false;
        }
        if (dto instanceof UserDto value) return validUser(value, group);
        if (dto instanceof ClubDto value) return validClub(value, group);
        if (dto instanceof EventDto value) return validEvent(value);
        if (dto instanceof ProductDto value) return validProduct(value, group);
        if (dto instanceof TableDto value) return validTable(value);
        if (dto instanceof ReviewDto value) return validReview(value);
        if (dto instanceof BookingDto value) return validBooking(value, group);
        throw new IllegalArgumentException("No DTO validation rules for " + dto.getClass().getName());
    }

    private static boolean validUser(UserDto dto, Class<?> group) {
        final boolean create = isCreate(group);
        return requiredText(dto.getUsername(), create, 100)
                && requiredText(dto.getName(), create, 100)
                && requiredText(dto.getSurname(), create, 100)
                && validEmail(dto.getEmail(), create)
                && validPhone(dto.getCellphone(), create)
                && (!create || (!isBlank(dto.registrationPassword()) && dto.registrationPassword().length() >= 6));
    }

    private static boolean validClub(ClubDto dto, Class<?> group) {
        final boolean create = isCreate(group);
        return requiredText(dto.getClubName(), create, 100)
                && requiredText(dto.getDescription(), create, 1000)
                && requiredText(dto.getLocation(), create, Integer.MAX_VALUE)
                && requiredText(dto.getZone(), create, Integer.MAX_VALUE)
                && requiredText(dto.getMusicGenre(), create, Integer.MAX_VALUE)
                && requiredText(dto.getPriceRange(), create, Integer.MAX_VALUE)
                && requiredText(dto.getCbu(), create, 50);
    }

    private static boolean validEvent(EventDto dto) {
        return requiredText(dto.getEventName(), true, 120)
                && dto.getDate() != null
                && optionalText(dto.getDescription(), 500);
    }

    private static boolean validProduct(ProductDto dto, Class<?> group) {
        return requiredText(dto.getProductName(), true, 100)
                && requiredText(dto.getDescription(), true, 300)
                && dto.getBasePrice() != null
                && dto.getBasePrice().signum() >= 0
                && dto.getBasePrice().compareTo(PRODUCT_PRICE_LIMIT) < 0
                && dto.getProductType() != null
                && (!isUpdate(group) || dto.getIsAvailable() != null);
    }

    private static boolean validTable(TableDto dto) {
        return dto.getTableNumber() != null && dto.getTableNumber() >= 1
                && dto.getCapacity() != null && dto.getCapacity() >= 1 && dto.getCapacity() <= 9999
                && dto.getTableState() != null
                && nonNegative(dto.getPrice())
                && dto.getDiscount() != null
                && dto.getDiscount().signum() >= 0
                && dto.getDiscount().compareTo(ONE_HUNDRED) <= 0;
    }

    private static boolean validReview(ReviewDto dto) {
        return dto.getRating() != null && dto.getRating() >= 1 && dto.getRating() <= 5;
    }

    private static boolean validBooking(BookingDto dto, Class<?> group) {
        if (isCreate(group)) {
            return dto.getTableId() != null && dto.getDate() != null;
        }
        return isUpdate(group) && !isBlank(dto.getStatus());
    }

    private static boolean validEmail(String value, boolean required) {
        return value == null
                ? !required
                : !value.isBlank() && EMAIL.matcher(value).matches();
    }

    private static boolean validPhone(String value, boolean required) {
        return value == null
                ? !required
                : !value.isBlank() && value.length() <= 30 && PHONE.matcher(value).matches();
    }

    private static boolean requiredText(String value, boolean required, int maxLength) {
        return value == null ? !required : !value.isBlank() && value.length() <= maxLength;
    }

    private static boolean optionalText(String value, int maxLength) {
        return value == null || value.length() <= maxLength;
    }

    private static boolean nonNegative(BigDecimal value) {
        return value != null && value.signum() >= 0;
    }

    private static boolean isCreate(Class<?> group) {
        return ValidationGroups.Create.class.equals(group);
    }

    private static boolean isUpdate(Class<?> group) {
        return ValidationGroups.Update.class.equals(group);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
