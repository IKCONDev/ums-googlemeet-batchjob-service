package com.ikn.ums.googlemeet.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ikn.ums.googlemeet.entity.EmailTemplate;



@Repository
public interface EmailTemplateRepository
        extends JpaRepository<EmailTemplate, String> {

    Optional<EmailTemplate> findByTemplateKeyAndActiveTrue(String templateKey);
}

