package com.dbinbox.aiinbox.controller;

import com.dbinbox.aiinbox.model.Message;
import com.dbinbox.aiinbox.repository.MessageRepo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DebugViewController
{
    private final MessageRepo messageRepository;

    public DebugViewController(MessageRepo messageRepository) {
        this.messageRepository = messageRepository;
    }

    @GetMapping("/debug-inbox")
    public String showInbox(Model model) {
        List<Message> messages = messageRepository.findAllByOrderByCreatedAtDesc();

        // Debug print to see if the first message has a name
        if(!messages.isEmpty() && messages.get(0).getContact() != null) {
            System.out.println("UI fetch - Contact name: " + messages.get(0).getContact().getName());
        }

        model.addAttribute("messages", messages);
        return "debug-inbox";
    }
}
