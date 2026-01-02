package com.ikn.ums.googlemeet.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailTemplateDto {

    private String templateKey;
    private String subject;
    private String body;
    private String toEmails;
    private String ccEmails;
    private String bccEmails;
    private Boolean active;
    private LocalDateTime updatedAt;
    private String updatedBy;
}


