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
import java.util.HashSet;
import java.util.Set;

@Table(name = "raportoitavavastaanottaja")
@Entity
public class ReportedRecipient extends BaseEntity {
    private static final long serialVersionUID = -4957288730521500299L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lahetettyviesti_id")
    private ReportedMessage reportedMessage;

    @Column(name = "vastaanottajan_oid", nullable = false)
    private String recipientOid;

    @Column(name = "vastaanottajan_oid_tyyppi", nullable = false)
    private String recipientOidType;

    @Column(name = "vastaanottajan_sahkopostiosoite", nullable = false)
    private String recipientEmail;

    @Column(name = "kielikoodi", nullable = false)
    private String languageCode;

    @Column(name = "hakunimi", nullable = false)
    private String searchName;

    @Column(name = "lahetysalkoi", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date sendingStarted;

    @Column(name = "lahetyspaattyi", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date sendingEnded;

    @Column(name = "lahetysonnistui", nullable = true)
    private String sendingSuccessful;

    @Column(name = "epaonnistumisensyy", nullable = true)
    private String failureReason;

    @Column(name = "aikaleima", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

    @Column(name = "henkilotiedot_haettu", nullable = false)
    private Boolean detailsRetrieved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jono", nullable = true)
    private SendQueue queue;

    @Column(name = "letter_hash")
    private String letterHash;

    @OneToMany(mappedBy = "recipient", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<ReportedMessageRecipientAttachment> attachments = new HashSet<>(0);

    public ReportedMessage getReportedMessage() {
        return reportedMessage;
    }

    public void setReportedMessage(ReportedMessage reportedMessage) {
        this.reportedMessage = reportedMessage;
    }

    public String getRecipientOid() {
        return recipientOid;
    }

    public void setRecipientOid(String recipientOid) {
        this.recipientOid = recipientOid;
    }

    public String getRecipientOidType() {
        return recipientOidType;
    }

    public void setRecipientOidType(String recipientOidType) {
        this.recipientOidType = recipientOidType;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getSearchName() {
        return searchName;
    }

    public void setSearchName(String searchName) {
        this.searchName = searchName;
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

    public String getSendingSuccessful() {
        return sendingSuccessful;
    }

    public void setSendingSuccessful(String sendingSuccessful) {
        this.sendingSuccessful = sendingSuccessful;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getDetailsRetrieved() {
        return detailsRetrieved;
    }

    public void setDetailsRetrieved(Boolean detailsRetrieved) {
        this.detailsRetrieved = detailsRetrieved;
    }

    public SendQueue getQueue() {
        return queue;
    }

    public void setQueue(SendQueue queue) {
        this.queue = queue;
    }

    public Set<ReportedMessageRecipientAttachment> getAttachments() {
        return attachments;
    }

    protected void setAttachments(Set<ReportedMessageRecipientAttachment> attachments) {
        this.attachments = attachments;
    }

    public String getLetterHash() {
        return letterHash;
    }

    public void setLetterHash(String letterHash) {
        this.letterHash = letterHash;
    }
}
