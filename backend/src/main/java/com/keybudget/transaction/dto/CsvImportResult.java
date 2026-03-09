package com.keybudget.transaction.dto;

import java.util.List;

public record CsvImportResult(
        int totalRows,
        int importedCount,
        int skippedCount,
        List<String> errors
) {}
