package com.ikn.ums.googlemeet.service;

public interface EmailTemplateService {

    String getSubject(String templateKey);

    String getBody(String templateKey, Object... args);

    String[] getToEmails(String templateKey);

    String[] getCcEmails(String templateKey);

    String[] getBccEmails(String templateKey);
}

