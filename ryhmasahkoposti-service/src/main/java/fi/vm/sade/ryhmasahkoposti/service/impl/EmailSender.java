/**
 * Copyright (c) 2014 The Finnish Board of Education - Opetushallitus
 *
 * This program is free software:  Licensed under the EUPL, Version 1.1 or - as
 * soon as they will be approved by the European Commission - subsequent versions
 * of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 **/
package fi.vm.sade.ryhmasahkoposti.service.impl;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;

import fi.vm.sade.ryhmasahkoposti.api.dto.AttachmentContainer;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailAttachment;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailConstants;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailMessage;

@Service
public class EmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(fi.vm.sade.ryhmasahkoposti.service.impl.EmailSender.class);

    private static final String FIVE_MINUTES = "300000";

    @Value("${ryhmasahkoposti.smtp.host}")
    private String smtpHost;
    @Value("${ryhmasahkoposti.smtp.port}")
    private String smtpPort;
    @Value("${ryhmasahkoposti.smtp.use_tls}")
    private boolean smtpUseTLS;
    @Value("${ryhmasahkoposti.smtp.authenticate}")
    private boolean smtpAuthenticate;
    @Value("${ryhmasahkoposti.smtp.username}")
    private String smtpUsername;
    @Value("${ryhmasahkoposti.smtp.password}")
    private String smtpPassword;
    @Value("${ryhmasahkoposti.smtp.return_path}")
    private String smtpReturnPath;

    public void handleMail(EmailMessage emailMessage, String emailAddress,
                           String letterHash, Optional<? extends AttachmentContainer> additionalAttachments) throws Exception {
        try {
            Session session = createSession();
            MimeMessage message = createMail(session, emailMessage, emailAddress, letterHash, additionalAttachments);
            if (EmailConstants.TEST_MODE.equals("NO")) {
                LOGGER.debug("Sending message: " + message.toString());
                long start = System.currentTimeMillis();
                Transport.send(message);
                long took = System.currentTimeMillis() -start;
                LOGGER.debug("Message sent took: " + took);
            } else {
                LOGGER.info("Sending mock message to {}, hash {}", emailAddress, letterHash);
                mockSendMail(emailMessage, emailAddress, additionalAttachments); //just log the message
            }
        } catch (Exception e) {
            LOGGER.error("Failed to send message to " + emailAddress + ": " + emailMessage.getBody(), e);
            throw e;
        }
    }

    public MimeMessage createMail(EmailMessage emailMessage, String emailAddress,
                                  String letterHash, Optional<? extends AttachmentContainer> additionalAttachments)
            throws MessagingException, UnsupportedEncodingException {
        return createMail(createSession(), emailMessage, emailAddress, letterHash, additionalAttachments);
    }

    public MimeMessage createMail(Session session, EmailMessage emailMessage, String emailAddress,
                                  String letterHash, Optional<? extends AttachmentContainer> additionalAttachments)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage msg = new MimeMessage(session);

        msg.setRecipients(MimeMessage.RecipientType.TO, InternetAddress.parse(emailAddress, false));
        msg.setFrom(new InternetAddress(emailMessage.getFrom(), emailMessage.getSender()));
        msg.setSubject(emailMessage.getSubject(), emailMessage.getCharset());
        if (emailMessage.getReplyTo() != null) {
            InternetAddress[] replyToAddrs = InternetAddress.parse(emailMessage.getReplyTo(), false);
            msg.setReplyTo(replyToAddrs);
        }
        msg.addHeader("X-Batch-ID", "Opetushallitus");
        msg.addHeader("X-Message-ID", letterHash + ".posti@hard.ware.fi");
        if (smtpReturnPath != null && smtpReturnPath.length() > 0) {
            msg.addHeader("Return-Path", smtpReturnPath);
        }

        MimeMultipart msgContent = new MimeMultipart("mixed");
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(getContent(emailMessage), getContentType(emailMessage) + ";charset=" + emailMessage.getCharset());
        bodyPart.setHeader("Content-Transfer-Encoding", getContentTransferEncoding());
        msgContent.addBodyPart(bodyPart);

        boolean valid = emailMessage.isValid();
        if (valid && emailMessage.getAttachments() != null) {
            valid = insertAttachments(emailMessage, msgContent);
        }
        if (valid && additionalAttachments.isPresent() && additionalAttachments.get().getAttachments() != null) {
            valid = insertAttachments(additionalAttachments.get(), msgContent);
        }

        if (valid) {
            msg.setContent(msgContent);
            msg.setHeader("Content-Transfer-Encoding", getContentTransferEncoding());
            msg.saveChanges(); //updates changes to headers
            return msg;
        }
        return null;
    }

    private String getContent(EmailMessage emailMessage) {
        if(!emailMessage.isHtml()) {
            //Change LF to CRLF (SMTP standard)
            //The extra space at the end is a "temporary" bug fix to plaintext LF issue
            return emailMessage.getBody().replaceAll("\n", "\r\n") + " ";
        }
        return emailMessage.getBody();
    }

    /*
     * 8BITMIME SMTP Extension should be supported by 99% of SMTP servers.
     */
    private String getContentTransferEncoding() {
        return "8bit";
    }

    private String getContentType(EmailMessage emailMessage) {
        return emailMessage.isHtml() ? "text/html" : "text/plain";
    }

    private boolean insertAttachments(AttachmentContainer container, MimeMultipart multipart) throws MessagingException {
        for (EmailAttachment attachment : container.getAttachments()) {
            if ((attachment.getData() != null) && (attachment.getName() != null)) {
                ByteArrayDataSource ds = new ByteArrayDataSource(attachment.getData(), attachment.getContentType());

                MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.setDataHandler(new DataHandler(ds));
                attachmentPart.setFileName(attachment.getName());
                attachmentPart.setHeader("Content-Type", attachment.getContentType());
                multipart.addBodyPart(attachmentPart);

            } else {
                LOGGER.error("Failed to insert attachment - it is not valid " + attachment.getName());
                return false;
            }
        }
        return true;
    }

    private Session createSession() {
        Properties mailProps = new Properties();
        mailProps.put("mail.smtp.host", smtpHost);
        mailProps.put("mail.smtp.port", smtpPort);
        mailProps.put("mail.smtp.connectiontimeout", FIVE_MINUTES);
        mailProps.put("mail.smtp.timeout", FIVE_MINUTES);
        mailProps.put("mail.smtp.writetimeout", FIVE_MINUTES);
        mailProps.put("mail.smtp.auth", smtpAuthenticate);
        mailProps.put("mail.starttls.enable", smtpUseTLS);
        mailProps.put("mail.transport.protocol", "smtp");

        if (smtpAuthenticate) {
            return Session.getInstance(mailProps, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUsername, smtpPassword);
                }
            });
        } else {
            return Session.getInstance(mailProps);
        }
    }

    private void mockSendMail(EmailMessage emailMessage, String emailAddress,
                              Optional<? extends AttachmentContainer> additionalAttachments) {
        // Log the content of the message
        StringBuffer sb = new StringBuffer("Email dummysender:");
        sb.append("\nFROM:    ");
        sb.append(emailMessage.getFrom());
        sb.append("\nTO:      ");
        sb.append(emailAddress);
        sb.append("\nSUBJECT: ");
        sb.append(emailMessage.getSubject());
        if (emailMessage.getAttachments() != null && emailMessage.getAttachments().size() > 0) {
            sb.append("\nATTACHMENTS:");
            createAttachmentNameAndContentType(emailMessage, sb);
        }
        if (additionalAttachments.isPresent()) {
            sb.append("\nADDITIONAL ATTACHMENTS:");
            createAttachmentNameAndContentType(emailMessage, sb);
        }
        sb.append("\n");
        sb.append(emailMessage.getBody());
        LOGGER.debug("MockSendMail:\n" + sb.toString());
    }

    private void createAttachmentNameAndContentType(EmailMessage emailMessage, StringBuffer sb) {
        for (EmailAttachment attachment : emailMessage.getAttachments()) {
            sb.append(" ");
            sb.append(attachment.getName());
            sb.append("(");
            sb.append(attachment.getContentType());
            sb.append(")");
        }
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    @Override
    public String toString() {
        return "EmailSender: [smtp host " + smtpHost + " port " + smtpPort + "]";
    }

}
