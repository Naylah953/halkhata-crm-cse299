package com.dbinbox.aiinbox.repository;


import com.dbinbox.aiinbox.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepo extends JpaRepository<Contact, String>
{
}
