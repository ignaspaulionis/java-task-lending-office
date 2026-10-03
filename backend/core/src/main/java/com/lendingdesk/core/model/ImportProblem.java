package com.lendingdesk.core.model;

/** Why a CSV row was not imported. */
public enum ImportProblem {
  MISSING_FIELD,
  INVALID_TAG,
  FIELD_TOO_LONG,
  DUPLICATE_IN_FILE,
  TAG_EXISTS
}
