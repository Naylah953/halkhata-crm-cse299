package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor      // <-- Required for Jackson JSON parsing
@AllArgsConstructor     // <-- Required for Lombok @Builder
public class AiAnalyticsResponse {
    private String aiSummary;
    private boolean isTable;
    private TableData tableData;

    @Data
    @Builder
    @NoArgsConstructor  // <-- Required for Jackson JSON parsing
    @AllArgsConstructor // <-- Required for Lombok @Builder
    public static class TableData {
        private List<String> columns;
        private List<Map<String, Object>> rows;
    }
}