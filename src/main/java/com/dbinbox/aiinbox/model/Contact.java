package com.dbinbox.aiinbox.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Contact
{
    @Id
    private String id;
    private String name;

    public Contact(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    @JsonIgnore
    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL)
    private List<Message> messageList = new ArrayList<>();// Initialize to avoid nulls
}