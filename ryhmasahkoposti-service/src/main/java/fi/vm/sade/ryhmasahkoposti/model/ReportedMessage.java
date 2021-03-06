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
package fi.vm.sade.ryhmasahkoposti.model;

import fi.vm.sade.generic.model.BaseEntity;

import javax.persistence.*;

import java.util.Date;
import java.util.Set;

@Table(name = "raportoitavaviesti")
@Entity
public class ReportedMessage extends BaseEntity {
    private static final long serialVersionUID = 7511140604535983187L;

    @Column(name = "prosessi", nullable = false)
    private String process;

    @Column(name = "lahettajan_oid", nullable = true)
    private String senderOid;

    @Column(name = "lahettajan_nimi", nullable = true)
    private String senderName;

    @Column(name = "lahettajan_sahkopostiosoite", nullable = false)
    private String senderEmail;

    @Column(name = "lahettajan_näyttöteksti")
    private String senderDisplayText;

    @Column(name = "viestipohja_id")
    private Long templateId;

    @Column(name = "kirjelahetys_id")
    private Long letterId;

    @Column(name = "lahettajan_organisaatio_oid", nullable = true)
    private String senderOrganizationOid;

    @Column(name = "vastaus_sahkopostiosoite", nullable = true)
    private String replyToEmail;

    @Column(name = "aihe", nullable = false)
    private String subject;

    @Column(name = "viesti", nullable = false)
    private String message;

    @Column(name = "htmlviesti", nullable = false)
    private String htmlMessage;

    @Column(name = "merkisto", nullable = false)
    private String characterSet;

    @OneToMany(mappedBy = "reportedMessage", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ReportedRecipient> reportedRecipients;

    @OneToMany(mappedBy = "reportedMessage", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ReportedMessageAttachment> reportedMessageAttachments;

    @Column(name = "lahetysalkoi", nullable = false)
    private Date sendingStarted;

    @Column(name = "lahetyspaattyi", nullable = true)
    private Date sendingEnded;

    @Column(name = "aikaleima", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column(name = "tyyppi", nullable = true)
    private String type;

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String getSenderOid() {
        return senderOid;
    }

    public void setSenderOid(String senderOid) {
        this.senderOid = senderOid;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSenderDisplayText() {
        return senderDisplayText;
    }

    public void setSenderDisplayText(String senderDisplayText) {
        this.senderDisplayText = senderDisplayText;
    }

    public String getSenderOrganizationOid() {
        return senderOrganizationOid;
    }

    public void setSenderOrganizationOid(String senderOrganizationOid) {
        this.senderOrganizationOid = senderOrganizationOid;
    }

    public String getReplyToEmail() {
        return replyToEmail;
    }

    public void setReplyToEmail(String replyToEmail) {
        this.replyToEmail = replyToEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getHtmlMessage() {
        return htmlMessage;
    }

    public void setHtmlMessage(String htmlMessage) {
        this.htmlMessage = htmlMessage;
    }

    public String getCharacterSet() {
        return characterSet;
    }

    public void setCharacterSet(String characterSet) {
        this.characterSet = characterSet;
    }

    public Set<ReportedRecipient> getReportedRecipients() {
        return reportedRecipients;
    }

    public void setReportedRecipients(Set<ReportedRecipient> reportedRecipients) {
        this.reportedRecipients = reportedRecipients;
    }

    public Set<ReportedMessageAttachment> getReportedMessageAttachments() {
        return reportedMessageAttachments;
    }

    public void setReportedMessageAttachments(Set<ReportedMessageAttachment> reportedMessageAttachments) {
        this.reportedMessageAttachments = reportedMessageAttachments;
    }

    public Date getSendingStarted() {
        return sendingStarted;
    }

    public void setSendingStarted(Date sendingStarted) {
        this.sendingStarted = sendingStarted;
    }

    public Date getSendingEnded() {
        return sendingEnded;
    }

    public void setSendingEnded(Date sendingEnded) {
        this.sendingEnded = sendingEnded;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getLetterId() {
        return letterId;
    }

    public void setLetterId(Long letterId) {
        this.letterId = letterId;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Email content is created using template
     */
    public static final String TYPE_TEMPLATE = "T";

    /**
     * Email content is in email body
     */
    public static final String TYPE_EMAIL = "E";

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ReportedMessage{" + "process='" + process + '\'' + ", senderOid='" + senderOid + '\'' + ", templateId='" + templateId + '\'' + ", senderName='"
                + senderName + '\'' + ", senderEmail='" + senderEmail + '\'' + ", senderDisplayText='" + senderDisplayText + '\'' + ", senderOrganizationOid='"
                + senderOrganizationOid + '\'' + ", replyToEmail='" + replyToEmail + '\'' + ", subject='" + subject + '\'' + ", message='" + message + '\''
                + ", htmlMessage='" + htmlMessage + '\'' + ", characterSet='" + characterSet + '\'' + ", reportedRecipients=" + reportedRecipients
                + ", reportedMessageAttachments=" + reportedMessageAttachments + ", sendingStarted=" + sendingStarted + ", sendingEnded=" + sendingEnded
                + ", timestamp=" + timestamp + ", type='" + type + '\'' + '}';
    }
}
