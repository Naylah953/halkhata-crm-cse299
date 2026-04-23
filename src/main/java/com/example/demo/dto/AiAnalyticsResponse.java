package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AiAnalyticsResponse {
    private String aiSummary;   // The conversational text (e.g., "Here are your top 5 products!")
    private boolean isTable;    // Tells your JS whether to build a <table> or not
    private TableData tableData;

    @Data
    @Builder
    public static class TableData {
        private List<String> columns; // e.g., ["Product Name", "Quantity Sold", "Revenue"]
        private List<Map<String, Object>> rows; // The actual database results
    }
}