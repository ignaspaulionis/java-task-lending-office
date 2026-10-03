package com.lendingdesk.core.service;

import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.model.ImportProblem;
import com.lendingdesk.core.model.ImportReport;
import com.lendingdesk.core.port.DeviceRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Imports devices from CSV text with the header {@value #HEADER}. */
public class DeviceImportService {

  static final String HEADER = "inventoryTag,name,category";
  private static final String TAG_PREFIX = "NTL-";
  private static final int MAX_TAG_LENGTH = 50;
  private static final int MAX_NAME_LENGTH = 100;
  private static final int MAX_CATEGORY_LENGTH = 50;

  private final DeviceRepository devices;

  public DeviceImportService(DeviceRepository devices) {
    this.devices = devices;
  }

  /**
   * Imports every valid row and reports the others. Fields never contain commas.
   *
   * @throws LendingException {@code VALIDATION_FAILED} if the file is empty or the header is wrong
   */
  @Transactional
  public ImportReport importCsv(String content) {
    List<String> lines = content.replaceFirst("^﻿", "").lines().toList();
    if (lines.isEmpty() || !HEADER.equals(lines.getFirst().strip())) {
      throw new LendingException(ErrorCode.VALIDATION_FAILED, "The first line must be " + HEADER);
    }

    List<ImportReport.Error> errors = new ArrayList<>();
    List<Row> candidates = new ArrayList<>();
    Set<String> tagsInFile = new HashSet<>();
    for (int index = 1; index < lines.size(); index++) {
      String line = lines.get(index);
      if (line.isBlank()) {
        continue;
      }
      int lineNumber = index + 1;
      Row row = Row.parse(lineNumber, line);
      ImportProblem problem = row == null ? ImportProblem.MISSING_FIELD : validate(row, tagsInFile);
      if (problem != null) {
        errors.add(new ImportReport.Error(lineNumber, problem));
      } else {
        candidates.add(row);
      }
    }

    Set<String> existing = devices.findExistingTags(candidates.stream().map(Row::tag).toList());
    int imported = 0;
    for (Row row : candidates) {
      if (existing.contains(row.tag())) {
        errors.add(new ImportReport.Error(row.line(), ImportProblem.TAG_EXISTS));
      } else {
        devices.save(new Device(row.tag(), row.name(), row.category()));
        imported++;
      }
    }
    errors.sort(Comparator.comparingInt(ImportReport.Error::line));
    return new ImportReport(imported, errors);
  }

  private static ImportProblem validate(Row row, Set<String> tagsInFile) {
    if (!row.tag().startsWith(TAG_PREFIX) || row.tag().length() == TAG_PREFIX.length()) {
      return ImportProblem.INVALID_TAG;
    }
    if (row.tag().length() > MAX_TAG_LENGTH
        || row.name().length() > MAX_NAME_LENGTH
        || row.category().length() > MAX_CATEGORY_LENGTH) {
      return ImportProblem.FIELD_TOO_LONG;
    }
    if (!tagsInFile.add(row.tag())) {
      return ImportProblem.DUPLICATE_IN_FILE;
    }
    return null;
  }

  private record Row(int line, String tag, String name, String category) {

    /** Splits a line into three non-blank fields, or returns {@code null}. */
    static Row parse(int line, String text) {
      String[] fields = text.split(",", -1);
      if (fields.length != 3) {
        return null;
      }
      for (int i = 0; i < fields.length; i++) {
        fields[i] = fields[i].strip();
        if (fields[i].isEmpty()) {
          return null;
        }
      }
      return new Row(line, fields[0], fields[1], fields[2]);
    }
  }
}
