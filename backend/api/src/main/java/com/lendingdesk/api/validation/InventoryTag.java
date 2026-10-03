package com.lendingdesk.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** An inventory tag: {@code NTL-} followed by at least one character. {@code null} is valid. */
@Documented
@Constraint(validatedBy = InventoryTagValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface InventoryTag {

  String message() default "must start with NTL-";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
