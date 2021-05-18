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
package fi.vm.sade.ryhmasahkoposti.testdata;

import fi.vm.sade.dto.HenkiloDto;
import fi.vm.sade.dto.OrganisaatioHenkiloDto;
import fi.vm.sade.dto.PagingAndSortingDTO;
import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.ryhmasahkoposti.api.dto.*;
import fi.vm.sade.ryhmasahkoposti.api.dto.query.ReportedMessageQueryDTO;
import fi.vm.sade.ryhmasahkoposti.model.*;
import org.apache.commons.fileupload.FileItem;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.*;

public class RaportointipalveluTestData {

    public static AttachmentResponse getAttachmentResponse(Long id, FileItem fileItem) {
        AttachmentResponse attachmentResponse = new AttachmentResponse();

        attachmentResponse.setUuid(id.toString());
        attachmentResponse.setFileName(fileItem.getName());
        attachmentResponse.setContentType(fileItem.getContentType());

        return attachmentResponse;
    }

    public static EmailData getEmailData() {
        EmailData emailData = new EmailData();
        emailData.setSenderOid("lahettajan oid");

        EmailMessage emailMessage = new EmailMessage();
        emailMessage.setBody("s-postiviestin sisalto");
        emailMessage.setCallingProcess("kutsuvaprosessi esim. valinta");
        emailMessage.setCharset("koodisto");
        emailMessage.setFrom("lahettajan s-postiosoite");
        emailMessage.setHtml(false);
        emailMessage.setOrganizationOid("lahettajan organisaation oid-tunnus");
        emailMessage.setReplyTo("vastaus s-postiosoite");
        emailMessage.setSenderOid("lahettajan oid-tunnus");
        emailMessage.setSubject("s-postin aihe");

        EmailRecipient emailRecipient = new EmailRecipient();
        emailRecipient.setEmail("vastaanottajan s-postiosoite");
        emailRecipient.setLanguageCode("vastaanottajan kielikoodi");
        emailRecipient.setOid("vastaanottajan oid-tunnus");
        emailRecipient.setOidType("arvoksi tyhja");
        emailData.setRecipient(new ArrayList<EmailRecipient>(Arrays.asList(emailRecipient)));

        return emailData;
    }

    public static EmailMessage getEmailMessage() {
        EmailMessage emailMessage = new EmailMessage();

        emailMessage.setBody("Tämä on koekutsu");
        emailMessage.setCallingProcess("Hakuprosessi");
        emailMessage.setCharset("utf-8");
        emailMessage.setHtml(false);
        emailMessage.setReplyTo("vastaus.oppilaitos@sposti.fi");
        emailMessage.setFrom("lahettaja.oppilaitos@sposti.fi");
        emailMessage.setSenderOid("1.2.246.562.24.42645159413");
        emailMessage.setSubject("Koekutsu");

        List<SourceRegister> sourceRegisters = new ArrayList<SourceRegister>();
        SourceRegister sourceRegister = new SourceRegister();
        sourceRegister.setName("opintopolku");
        sourceRegisters.add(sourceRegister);

        emailMessage.setSourceRegister(sourceRegisters);

        return emailMessage;
    }

    public static EmailMessageDTO getEmailMessageDTO() {
        EmailMessageDTO emailMessageDTO = new EmailMessageDTO();

        emailMessageDTO.setMessageID(new Long(1));
        emailMessageDTO.setBody("Tämä on koekutsu");
        emailMessageDTO.setCallingProcess("Hakuprosessi");
        emailMessageDTO.setCharset("utf-8");
        emailMessageDTO.setHtml(false);
        emailMessageDTO.setReplyTo("vastaus.oppilaitos@sposti.fi");
        emailMessageDTO.setFrom("lahettaja.oppilaitos@sposti.fi");
        emailMessageDTO.setSenderOid("1.2.246.562.24.42645159413");
        emailMessageDTO.setSubject("Koekutsu");

        return emailMessageDTO;
    }

    public static EmailAttachment getEmailAttachment() {
        EmailAttachment emailAttachment = new EmailAttachment();

        byte[] attachment = { 'k', 'o', 'e', 'k', 'u', 't', 's', 'u' };

        emailAttachment.setData(attachment);
        emailAttachment.setName("koekutsu.doc");
        emailAttachment.setContentType("application/pdf");

        return emailAttachment;
    }

    public static EmailRecipient getEmailRecipient() {
        EmailRecipient emailRecipient = new EmailRecipient();

        emailRecipient.setOid("1.2.246.562.24.34397748041");
        emailRecipient.setOidType("oppilas");
        emailRecipient.setEmail("vastaan.ottaja@sposti.fi");
        emailRecipient.setLanguageCode("FI");

        return emailRecipient;
    }

    public static EmailRecipientDTO getEmailRecipientDTO() {
        EmailRecipientDTO emailRecipientDTO = new EmailRecipientDTO();

        emailRecipientDTO.setRecipientID(-1L);
        emailRecipientDTO.setOid("1.2.246.562.24.34397748041");
        emailRecipientDTO.setOidType("oppilas");
        emailRecipientDTO.setEmail("vastaan.ottaja@sposti.fi");
        emailRecipientDTO.setLanguageCode("FI");

        return emailRecipientDTO;
    }

    public static EmailRecipientDTO getEmailRecipientDTO(EmailMessageDTO message) {
        EmailRecipientDTO recipient = getEmailRecipientDTO();
        recipient.setEmailMessageID(message.getMessageID());
        recipient.setLetterHash("hasneededfortestingwithnewmockito");
        return recipient;
    }

    public static HenkiloDto getHenkilo() {
        HenkiloDto henkilo = new HenkiloDto();

        henkilo.setOidHenkilo("1.2.246.562.24.34397748041");
        henkilo.setHetu("081181-9984");
        henkilo.setEtunimet("Etunimi");
        henkilo.setSukunimi("Sukunimi");
        henkilo.setKutsumanimi("Kutsumanimi");

        return henkilo;
    }

    public static HenkiloDto getSender() {
        HenkiloDto sender = new HenkiloDto();

        sender.setOidHenkilo("1.2.246.562.24.42645159413"); // matches sender
                                                            // oid
        sender.setHetu("081181-9984");
        sender.setEtunimet("Etunimi");
        sender.setSukunimi("Sukunimi");
        sender.setKutsumanimi("Kutsumanimi");

        return sender;
    }

    public static OrganisaatioRDTO getOrganisaatioRDTO() {
        OrganisaatioRDTO organisaatio = new OrganisaatioRDTO();

        organisaatio.setOid("1.2.246.562.10.00000000001");
        Map<String, String> nimet = new HashMap<String, String>();
        nimet.put("fi", "Oppilaitos");
        organisaatio.setNimi(nimet);

        return organisaatio;
    }

    public static OrganizationDTO getOrganizationDTO() {
        OrganizationDTO organizationDTO = new OrganizationDTO();

        organizationDTO.setName("OPH");
        organizationDTO.setOid("1.2.246.562.10.00000000001");

        return organizationDTO;
    }

    public static PagingAndSortingDTO getPagingAndSortingDTO() {
        PagingAndSortingDTO pagingAndSorting = new PagingAndSortingDTO();

        pagingAndSorting.setFromIndex(0);
        pagingAndSorting.setNumberOfRows(10);
        pagingAndSorting.setSortedBy("");
        pagingAndSorting.setSortOrder("asc");

        return pagingAndSorting;
    }

    public static ReportedMessage getReportedMessage() {
        ReportedMessage reportedMessage = new ReportedMessage();

        reportedMessage.setSubject("Koekutsu");
        reportedMessage.setProcess("Hakuprosessi");
        reportedMessage.setSenderOid("1.2.246.562.24.42645159413");
        reportedMessage.setSenderOrganizationOid("1.2.246.562.10.00000000001");
        reportedMessage.setSenderEmail("testi.virkailija@sposti.fi");
        reportedMessage.setReplyToEmail("testi.virkailija@sposti.fi");
        reportedMessage.setMessage("Tämä on koekutsu");
        reportedMessage.setHtmlMessage("");
        reportedMessage.setCharacterSet("utf-8");
        reportedMessage.setSendingStarted(new Date());
        reportedMessage.setTimestamp(new Date());

        return reportedMessage;
    }

    public static ReportedMessageDTO getReportedMessageDTO() {
        ReportedMessageDTO reportedMessage = new ReportedMessageDTO();

        reportedMessage.setMessageID(new Long(1));
        reportedMessage.setSubject("Koekutsu");
        reportedMessage.setSenderOid("1.2.246.562.24.42645159413");
        reportedMessage.setOrganizationOid("1.2.246.562.10.00000000001");
        reportedMessage.setSendingStatus(getSendingStatusDTO());
        reportedMessage.setSendingReport("");
        reportedMessage.setStatusReport("Lahetyksiä epäonnistui");

        return reportedMessage;
    }

    public static ReportedMessagesDTO getReportedMessagesDTO() {
        ReportedMessagesDTO reportedMessagesDTO = new ReportedMessagesDTO();

        reportedMessagesDTO.setNumberOfReportedMessages(new Long(1));
        List<ReportedMessageDTO> reportedMessageDTOs = new ArrayList<ReportedMessageDTO>();
        reportedMessageDTOs.add(getReportedMessageDTO());
        reportedMessagesDTO.setReportedMessages(reportedMessageDTOs);

        return reportedMessagesDTO;
    }

    public static ReportedMessageQueryDTO getReportedMessageQueryDTO() {
        ReportedMessageQueryDTO query = new ReportedMessageQueryDTO();

        query.setOrganizationOid("1.2.246.562.10.00000000001");
        query.setSearchArgument("testi.vastaanottaja@sposti.fi");

        return query;
    }

    public static ReportedMessageAttachment getReportedMessageAttachment() {
        ReportedMessageAttachment reportedMessageAttachment = new ReportedMessageAttachment();

        reportedMessageAttachment.setId(new Long(400));
        reportedMessageAttachment.setReportedAttachmentID(new Long(100));
        reportedMessageAttachment.setVersion(new Long(0));
        reportedMessageAttachment.setTimestamp(new Date());

        return reportedMessageAttachment;
    }

    public static ReportedRecipient getReportedRecipient(ReportedMessage reportedMessage) {
        ReportedRecipient raportoitavaVastaanottaja = new ReportedRecipient();

        raportoitavaVastaanottaja.setReportedMessage(reportedMessage);
        raportoitavaVastaanottaja.setRecipientOid("1.2.246.562.24.34397748041");
        raportoitavaVastaanottaja.setRecipientOidType("oppilas");
        raportoitavaVastaanottaja.setRecipientEmail("testi.vastaanottaja@sposti.fi");
        raportoitavaVastaanottaja.setLanguageCode("FI");
        raportoitavaVastaanottaja.setSearchName("Testi Oppilas");
        raportoitavaVastaanottaja.setDetailsRetrieved(true);
        raportoitavaVastaanottaja.setSendingStarted(new Date());
        raportoitavaVastaanottaja.setSendingEnded(new Date());
        raportoitavaVastaanottaja.setFailureReason("");
        raportoitavaVastaanottaja.setTimestamp(new Date());

        return raportoitavaVastaanottaja;
    }

    public static ReportedRecipient getReportedRecipient() {
        ReportedRecipient reportedRecipient = new ReportedRecipient();

        reportedRecipient.setRecipientOid("1.2.246.562.24.34397748041");
        reportedRecipient.setRecipientOidType("oppilas");
        reportedRecipient.setRecipientEmail("testi.vastaanottaja@sposti.fi");
        reportedRecipient.setLanguageCode("FI");
        reportedRecipient.setSearchName("Testi,Oppilas");
        reportedRecipient.setDetailsRetrieved(true);
        reportedRecipient.setSendingStarted(null);
        reportedRecipient.setSendingEnded(null);
        reportedRecipient.setFailureReason("");
        reportedRecipient.setTimestamp(new Date());

        return reportedRecipient;
    }

    public static ReportedAttachment getReportedAttachment() {
        ReportedAttachment raportoitavaLiite = new ReportedAttachment();

        byte[] sisalto = { 'k', 'o', 'e', 'k', 'u', 't', 's', 'u' };

        raportoitavaLiite.setAttachmentName("koekutsu.doc");
        raportoitavaLiite.setContentType("application/pdf");
        raportoitavaLiite.setAttachment(sisalto);
        raportoitavaLiite.setTimestamp(new Date());

        return raportoitavaLiite;
    }

    public static SendingStatusDTO getSendingStatusDTO() {
        SendingStatusDTO sendingStatusDTO = new SendingStatusDTO();
        sendingStatusDTO.setMessageID(new Long(1));
        sendingStatusDTO.setNumberOfRecipients(new Long(10));
        sendingStatusDTO.setNumberOfFailedSendings(new Long(2));
        sendingStatusDTO.setNumberOfSuccessfulSendings(new Long(5));
        sendingStatusDTO.setNumberOfBouncedSendings(new Long(6));
        sendingStatusDTO.setSendingStarted(new Date());

        return sendingStatusDTO;
    }

    public static List<OrganisaatioHenkiloDto> getHenkilonOrganisaatiot() {
        List<OrganisaatioHenkiloDto> henkilonOrganisaatiot = new ArrayList<OrganisaatioHenkiloDto>();

        OrganisaatioHenkiloDto organisaatioHenkilo = new OrganisaatioHenkiloDto();
        organisaatioHenkilo.setOrganisaatioOid("1.2.246.562.10.00000000001");
        henkilonOrganisaatiot.add(organisaatioHenkilo);

        return henkilonOrganisaatiot;
    }

    /**
     * Get test {@link ReportedRecipientReplacement} object
     *
     * @param reportedRecipient
     * @return Test object
     */
    public static ReportedRecipientReplacement getReportedRecipientReplacement(ReportedRecipient reportedRecipient) {

        ReportedRecipientReplacement reportedRecipientReplacement = new ReportedRecipientReplacement();

        reportedRecipientReplacement.setReportedRecipient(reportedRecipient);
        reportedRecipientReplacement.setName("test-replacement-key");
        reportedRecipientReplacement.setValue("default-value");
        reportedRecipientReplacement.setTimestamp(new Date());

        return reportedRecipientReplacement;
    }

    /**
     * Get test {@link ReportedMessageReplacement}
     *
     * @param reportedMessage
     * @return Test object
     */
    public static ReportedMessageReplacement getReportedMessageReplacement(ReportedMessage reportedMessage) {

        ReportedMessageReplacement reportedMessageReplacement = new ReportedMessageReplacement();

        reportedMessageReplacement.setReportedMessage(reportedMessage);
        reportedMessageReplacement.setName("test-replacement-key");
        reportedMessageReplacement.setDefaultValue("default-value");
        reportedMessageReplacement.setTimestamp(new Date());

        return reportedMessageReplacement;
    }

    /**
     * Generate replacements data.
     *
     * @param ids
     * @return Test object
     */
    public static List<ReplacementDTO> getEmailReplacements(int... ids) {

        List<ReplacementDTO> reportedReplacements = new ArrayList<ReplacementDTO>();

        for (int id : ids) {
            ReplacementDTO replacement = new ReplacementDTO();
            replacement.setName("key-" + id);
            replacement.setDefaultValue("email-" + id);
            reportedReplacements.add(replacement);
        }
        return reportedReplacements;
    }

    /**
     * Generate replacements data.
     *
     * @param ids
     * @return Test object
     */
    public static Set<ReplacementDTO> getTemplateReplacements(int... ids) {

        Set<ReplacementDTO> reportedReplacements = new HashSet<ReplacementDTO>();

        for (int id : ids) {
            ReplacementDTO replacement = new ReplacementDTO();
            replacement.setName("key-" + id);
            replacement.setDefaultValue("template-" + id);
            reportedReplacements.add(replacement);
        }
        return reportedReplacements;
    }

    /**
     * Generate reported recipient replacements data.
     *
     * @param ids
     * @return Test object
     */
    public static List<ReportedRecipientReplacementDTO> getReportedReceientReplacements(int... ids) {

        List<ReportedRecipientReplacementDTO> reportedRecipientReplacements = new ArrayList<ReportedRecipientReplacementDTO>();

        for (int id : ids) {
            ReportedRecipientReplacementDTO replacement = new ReportedRecipientReplacementDTO();
            replacement.setName("key-" + id);
            replacement.setDefaultValue("recipient-" + id);
            reportedRecipientReplacements.add(replacement);
        }
        return reportedRecipientReplacements;
    }

    public static SendQueue sendQueue(Long id, SendQueueState state) {
        SendQueue queue = new SendQueue();
        queue.setId(id);
        queue.setVersion(1l);
        queue.setState(state);
        return queue;
    }

    public static TemplateDTO template(String templateName, String languageCode) throws IOException, DocumentException {
        TemplateDTO template = new TemplateDTO();
        template.setId(123l);
        template.setName(templateName);
        template.setLanguage(languageCode);
        template.setVersionro("0");
        template.setOrganizationOid("123.456.789");
        template.setStoringOid("123");
        template.setType("email");
        template.setStyles("body {padding:10px;}");
        return template;
    }

    public static TemplateDTO with(TemplateDTO template, TemplateContentDTO... contents) {
        Set<TemplateContentDTO> templateContents = new HashSet<TemplateContentDTO>();
        for (TemplateContentDTO content : contents) {
            templateContents.add(content);
        }
        template.setContents(templateContents);
        return template;
    }

    public static TemplateDTO with(TemplateDTO template, ReplacementDTO... replacements) {
        Set<ReplacementDTO> replacementsSet = new HashSet<ReplacementDTO>();
        for (ReplacementDTO replacement : replacements) {
            replacementsSet.add(replacement);
        }
        template.setReplacements(replacementsSet);
        return template;
    }

    public static TemplateContentDTO content(String name, String contentStr) {
        TemplateContentDTO content = new TemplateContentDTO();
        content.setName(name);
        content.setContentType("text/html");
        content.setOrder(1);
        content.setTimestamp(new Date());
        content.setContent(contentStr);
        return content;
    }

    public static ReplacementDTO replacement(String name, String defaultValue) {
        ReplacementDTO loppuOsa = new ReplacementDTO();
        loppuOsa.setName(name);
        loppuOsa.setDefaultValue(defaultValue);
        loppuOsa.setMandatory(true);
        return loppuOsa;
    }
}
