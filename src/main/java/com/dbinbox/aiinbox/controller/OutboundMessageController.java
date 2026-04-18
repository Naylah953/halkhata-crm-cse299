package com.dbinbox.aiinbox.controller;


import com.dbinbox.aiinbox.dto.CRMResponse;
import com.dbinbox.aiinbox.service.OutboundMessageService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//This controller is responsible for acting as a "client" to Meta's servers or Graph API
//It sends data
@RestController
@RequestMapping("api/outbound")
public class OutboundMessageController
{
    private OutboundMessageService messageService;

    @Autowired
    public OutboundMessageController(OutboundMessageService messageService)
    {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<String> sendMessage(@RequestBody CRMResponse response)
    {
        messageService.sendReplyToUser(response.recipient().id(), response.message().text());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send")
    public ResponseEntity<Void> handleFormSend(@RequestParam String psid, @RequestParam String text) {
        messageService.sendReplyToUser(psid, text);

        // Redirects the browser back to the debug page so you see the new bubble immediately
        return ResponseEntity.status(302).header("Location", "/debug-inbox").build();
    }



}

