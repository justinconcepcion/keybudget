package com.keybudget.transaction.dto;

import java.util.List;

/**
 * Result of a CSV import operation.
 *
 * @param totalRows        total data rows read from the file (excluding the header)
 * @param importedCount    rows successfully saved as new transactions
 * @param skippedCount     rows skipped due to parse/validation errors
 * @param skippedDuplicates rows skipped because an identical transaction was already imported
 * @param errors           human-readable error messages, one per skipped/invalid row
 */
public record CsvImportResult(
        int totalRows,
        int importedCount,
        int skippedCount,
        int skippedDuplicates,
        List<String> errors
) {}
