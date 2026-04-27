package com.dbinbox.aiinbox.ai.tools;

import com.dbinbox.aiinbox.service.ProductService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomerTools {

    @Autowired
    private ProductService productService;

    //@Autowired
    //private OrderService orderService;

    // We use @Tool (or just define them as standard functions)
    // The description tells Gemini WHEN to use this.

    //@Tool(description = "Creates a draft order. Call this when the user is ready to buy.")
    //public String draftOrder(String itemsSummary, String conversationId) {
        //return orderService.createDraftOrder(conversationId, itemsSummary);
    //}
}