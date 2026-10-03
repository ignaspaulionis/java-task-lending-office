package com.lendingdesk.core.model;

/**
 * Device search parameters. {@code null} filters match everything.
 *
 * @param sortField field to sort by, as given by the client
 * @param ascending sort direction
 */
public record DeviceSearch(
    String q,
    String category,
    Boolean available,
    int page,
    int size,
    String sortField,
    boolean ascending) {}
