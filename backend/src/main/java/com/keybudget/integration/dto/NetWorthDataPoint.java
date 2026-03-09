package com.keybudget.integration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single date/value entry in the net-worth history time series.
 */
public record NetWorthDataPoint(
    LocalDate date,
    BigDecimal totalUsd
) {}
