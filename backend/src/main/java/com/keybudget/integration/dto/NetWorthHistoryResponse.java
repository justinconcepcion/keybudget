package com.keybudget.integration.dto;

import java.util.List;

/**
 * Time-series of the user's aggregated net worth in USD for charting purposes.
 */
public record NetWorthHistoryResponse(
    List<NetWorthDataPoint> dataPoints
) {}
