package ar.edu.itba.paw.model.dto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents the direction in which a DTO property belongs to the public API.
 * Controllers only consume READ_WRITE/WRITE_ONLY properties and assemblers are
 * responsible for READ_ONLY response data.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ApiFieldAccess {
    Mode value();

    enum Mode {
        READ_ONLY,
        WRITE_ONLY
    }
}
