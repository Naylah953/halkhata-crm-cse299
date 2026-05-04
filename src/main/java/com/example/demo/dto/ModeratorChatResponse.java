package com.example.demo.ai.dto;

import com.example.demo.dto.AiAnalyticsResponse.TableData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModeratorChatResponse {
    private String text;
    private boolean isTable;
    private TableData tableData;
}