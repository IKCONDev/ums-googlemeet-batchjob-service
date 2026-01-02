package com.ikn.ums.googlemeet.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "email_template")
@Getter
@Setter
public class EmailTemplate {

    @Id
    @Column(name = "template_key")
    private String templateKey;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "to_emails", columnDefinition = "TEXT")
    private String toEmails;  

    @Column(name = "cc_emails", columnDefinition = "TEXT")
    private String ccEmails;

    @Column(name = "bcc_emails", columnDefinition = "TEXT")
    private String bccEmails;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;
}
