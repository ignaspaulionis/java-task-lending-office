package com.lendingdesk.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class InventoryTagValidator implements ConstraintValidator<InventoryTag, String> {

  static final String PREFIX = "NTL-";

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null || (value.startsWith(PREFIX) && value.length() > PREFIX.length());
  }
}
