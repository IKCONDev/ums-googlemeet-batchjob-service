package com.ikn.ums.googlemeet.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EmailUtility {

    @Autowired
    private JavaMailSender sender;

    public boolean sendMail(String to, String subject, String textBody,
                            String[] cc, String[] bcc,
                            MultipartFile file, boolean isHtml) {
        return sendEmail(new String[]{to}, subject, textBody, cc, bcc,
                file != null ? new MultipartFile[]{file} : null, isHtml);
    }

    public boolean sendMail(String to, String subject, String textBody, boolean isHtml) {
        return sendEmail(new String[]{to}, subject, textBody, null, null, null, isHtml);
    }

    public boolean sendMail(String[] to, String subject, String textBody,
                            String[] cc, String[] bcc,
                            MultipartFile file, boolean isHtml) {
        return sendEmail(to, subject, textBody, cc, bcc,
                file != null ? new MultipartFile[]{file} : null, isHtml);
    }

    public boolean sendMail(String[] to, String subject, String textBody, boolean isHtml) {
        return sendEmail(to, subject, textBody, null, null, null, isHtml);
    }

    private boolean sendEmail(String[] to, String subject, String textBody,
                              String[] cc, String[] bcc,
                              MultipartFile[] files, boolean isHtml) {

    	if (to == null || to.length == 0
    	        || subject == null || subject.isEmpty()
    	        || textBody == null || textBody.isEmpty()) {

    	    log.warn("Email validation failed: invalid recipient, subject, or body");
    	}

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, files != null && files.length > 0, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(textBody, isHtml);

            if (cc != null && cc.length > 0) {
                helper.setCc(cc);
            }
            if (bcc != null && bcc.length > 0) {
                helper.setBcc(bcc);
            }

            if (files != null) {
                for (MultipartFile file : files) {
                    if (file != null && file.getOriginalFilename() != null) {
                        helper.addAttachment(file.getOriginalFilename(), file);
                    }
                }
            }

            sender.send(message);
            log.info("Email sent successfully to -> {}", String.join(", ", to));
            return true;

        } catch (Exception e) {
            log.error("Error while sending email -> {}", e.getMessage(), e);
            return false;
        }
    }
}
