package com.ikn.ums.googlemeet.service.impl;

import java.text.MessageFormat;

import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.entity.EmailTemplate;
import com.ikn.ums.googlemeet.repo.EmailTemplateRepository;
import com.ikn.ums.googlemeet.service.EmailTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private final EmailTemplateRepository repository;

    @Override
    public String getSubject(String templateKey) {
        return getTemplate(templateKey).getSubject();
    }

    @Override
    public String getBody(String templateKey, Object... args) {
        EmailTemplate template = getTemplate(templateKey);
        return MessageFormat.format(template.getBody(), args);
    }

    @Override
    public String[] getToEmails(String templateKey) {
        return splitEmails(getTemplate(templateKey).getToEmails());
    }

    @Override
    public String[] getCcEmails(String templateKey) {
        return splitEmails(getTemplate(templateKey).getCcEmails());
    }

    @Override
    public String[] getBccEmails(String templateKey) {
        return splitEmails(getTemplate(templateKey).getBccEmails());
    }

    private EmailTemplate getTemplate(String key) {
        return repository.findByTemplateKeyAndActiveTrue(key)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Email template not found: " + key
                        ));
    }

    private String[] splitEmails(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.split("\\s*,\\s*");
    }
}

