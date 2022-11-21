/*
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
 */
package fi.vm.sade.viestintapalvelu.letter.impl;

import java.io.IOException;
import java.util.*;

import javax.ws.rs.NotFoundException;

import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;

import fi.vm.sade.ryhmasahkoposti.api.dto.*;
import fi.vm.sade.viestintapalvelu.attachment.impl.AttachmentUri;
import fi.vm.sade.viestintapalvelu.common.util.OptionalHelper;
import fi.vm.sade.viestintapalvelu.dao.LetterBatchDAO;
import fi.vm.sade.viestintapalvelu.dao.criteria.TemplateCriteriaImpl;
import fi.vm.sade.viestintapalvelu.email.TemplateEmailField;
import fi.vm.sade.externalinterface.common.ObjectMapperProvider;
import fi.vm.sade.viestintapalvelu.externalinterface.component.EmailComponent;
import fi.vm.sade.viestintapalvelu.letter.LetterEmailService;
import fi.vm.sade.viestintapalvelu.letter.LetterService;
import fi.vm.sade.viestintapalvelu.letter.dto.EmailSendDataDto;
import fi.vm.sade.viestintapalvelu.letter.dto.LanguageCodeOptionsDto;
import fi.vm.sade.viestintapalvelu.model.*;
import fi.vm.sade.viestintapalvelu.model.types.ContentStructureType;
import fi.vm.sade.viestintapalvelu.template.ReadableReplacement;
import fi.vm.sade.viestintapalvelu.template.Template;
import fi.vm.sade.viestintapalvelu.template.TemplateService;

/**
 * User: ratamaa
 * Date: 30.9.2014
 * Time: 12:24
 */
@Service
@ComponentScan(value = { "fi.vm.sade.externalinterface" })
public class LetterEmailServiceImpl implements LetterEmailService {
    public static final String ADDITIONAL_ATTACHMENT_URIS_EMAIL_RECEIVER_PARAMETER = "additionalAttachmentUris";
    public static final String DEFAULT_LANGUAGE = "FI";

    private static final Logger logger = LoggerFactory.getLogger(LetterEmailServiceImpl.class);

    @Autowired
    private LetterBatchDAO letterBatchDAO;

    @Autowired
    private EmailComponent emailComponent;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private LetterService letterService;

    @Autowired
    private ObjectMapperProvider objectMapperProvider;

    @Override
    @Transactional
    public void sendEmail(long letterBatchId) throws Exception {
        LetterBatch letterBatch = foundLetterBatch(letterBatchId);
        if (letterBatch.getBatchStatus() == null || letterBatch.getBatchStatus() != LetterBatch.Status.ready) {
            throw new IllegalStateException("Can not send email to LetterBatch="+letterBatch.getTemplateId()
                    +" in status="+ letterBatch.getBatchStatus()+". Expecting ready status.");
        }
        if (letterBatch.getEmailHandlingStarted() != null) {
            throw new IllegalStateException("Email handling already stated at "
                    +letterBatch.getEmailHandlingStarted()+" for LetterBatch="+letterBatch.getTemplateId());
        }
        Template template = getTemplate(letterBatch);

        letterService.updateBatchProcessingStarted(letterBatchId, LetterService.LetterBatchProcess.EMAIL);

        EmailSendDataDto emailSendData = buildEmails(letterBatch, letterBatch.getLetterReceivers(), template);
        sendEmails(emailSendData);

        letterService.updateBatchProcessingFinished(letterBatchId, LetterService.LetterBatchProcess.EMAIL);
    }

    @Override
    @Transactional(readOnly = true)
    public LanguageCodeOptionsDto getLanguageCodeOptions(long letterBatchId) {
        LetterBatch letterBatch = foundLetterBatch(letterBatchId);
        Template template = getTemplate(letterBatch);

        Set<String> options = new TreeSet<>();
        String templateLanguage = Optional.fromNullable(template.getLanguage()).or(DEFAULT_LANGUAGE);
        // Add all recipient's wanted languages to has set:
        for (LetterReceivers receiver : letterBatch.getLetterReceivers()) {
            if (receiver.getWantedLanguage() != null) {
                options.add(receiver.getWantedLanguage().toUpperCase());
            } else {
                options.add(templateLanguage.toUpperCase());
            }
        }
        // Ensure template exists for each language:
        for (String language : new HashSet<>(options)) {
            if (templateService.getTemplateByName(
                        new TemplateCriteriaImpl(template.getName(), language.toUpperCase())
                            .withApplicationPeriod(letterBatch.getApplicationPeriod()), false) == null) {
                options.remove(language);
            }
        }

        LanguageCodeOptionsDto dto = new LanguageCodeOptionsDto();
        dto.setOptions(new ArrayList<>(options));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public String getPreview(LetterBatch letterBatch, Template template, Optional<String> languageCode) {
        assertBatchStatusIsReady(letterBatch);
        String templateLanguage = Optional.fromNullable(template.getLanguage()).or(DEFAULT_LANGUAGE);
        final Optional<LetterReceivers> letterReceiversOptional = firstWithEmail(letterBatch.getLetterReceivers(), languageCode, templateLanguage);
        final List<LetterReceivers> letterReceiverses = Collections.singletonList(letterReceiversOptional
                .or(OptionalHelper.<LetterReceivers>notFound("LetterBatch=" + letterBatch.getId()
                        + " does not have any handled recipients with email address")));
        EmailSendDataDto emailSendData = buildEmails(letterBatch, letterReceiverses, template);
        if (emailSendData.getEmails().isEmpty()) {
            throw new NotFoundException("No emails to preview.");
        }
        if (languageCode.isPresent()) {
            return emailComponent.getPreview(emailSendData.getEmailByLanguageCode(languageCode.get()));
        }
        return emailComponent.getPreview(emailSendData.getEmails().get(0));
    }

    private void assertBatchStatusIsReady(LetterBatch letterBatch) {
        if (letterBatch.getBatchStatus() == null || letterBatch.getBatchStatus() == LetterBatch.Status.created) {
            throw new IllegalStateException("Can not send email to LetterBatch="+letterBatch.getTemplateId() + " in status="+ letterBatch.getBatchStatus()+". Expecting ready status.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String getPreview(LetterBatch letterBatch, Optional<String> languageCode) {
        assertBatchStatusIsReady(letterBatch);
        Template template = getTemplate(letterBatch);
        return getPreview(letterBatch, template, languageCode);
    }

    @Override
    @Transactional(readOnly = true)
    public String getPreview(Long letterBatchId, Optional<String> languageCode) {
        LetterBatch letterBatch = foundLetterBatch(letterBatchId);
        return getPreview(letterBatch, languageCode);
    }

    private Optional<LetterReceivers> firstWithEmail(Collection<LetterReceivers> receivers,
                                         Optional<String> languageCode, String templateLanguage) {
        // Ensure selecting the same to preview for each time:
        List<LetterReceivers> receiversList = new ArrayList<>(receivers);
        Collections.sort(receiversList, new Comparator<LetterReceivers>() {
            public int compare(LetterReceivers o1, LetterReceivers o2) {
                return o1.getId().compareTo(o2.getId());
            }
        });
        for (LetterReceivers receiver : receiversList) {
            if (shouldReceiveEmail(receiver)
                    && receiver.getLetterReceiverLetter() != null
                    && receiver.getLetterReceiverLetter().getLetter() != null // ready
                    && ((!languageCode.isPresent() && (receiver.getWantedLanguage() == null
                            // do not select if no languageCode and receiver has other wantedLanguage than that of the template
                            || receiver.getWantedLanguage().equalsIgnoreCase(templateLanguage) ))
                        || (languageCode.isPresent() && (
                                ( receiver.getWantedLanguage() != null
                                && receiver.getWantedLanguage().equalsIgnoreCase(languageCode.get()))
                            || (
                                receiver.getWantedLanguage() == null
                                && languageCode.get().equalsIgnoreCase(templateLanguage) )))) ) {
                return Optional.of(receiver);
            }
        }
        return Optional.absent();
    }

    private LetterBatch foundLetterBatch(Long letterBatchId) {
        LetterBatch letterBatch = letterBatchDAO.read(letterBatchId);
        if (letterBatch == null) {
            throw new NotFoundException("LetterBatch not found by id="+letterBatchId);
        }
        return letterBatch;
    }

    private Template getTemplate(LetterBatch letterBatch) {
        if (letterBatch.getTemplateId() == null) {
            throw new IllegalStateException("LetterBatch="+letterBatch.getTemplateId()+" does not have a templateId!");
        }
        Template template = templateService.findById(letterBatch.getTemplateId(), ContentStructureType.email);
        if (template == null) {
            throw new NotFoundException("Template not found by id="+letterBatch.getTemplateId());
        }
        return template;
    }

    private EmailSendDataDto buildEmails(LetterBatch letterBatch, Collection<LetterReceivers> letterReceivers,
                             Template baseTemplate) {
        EmailSendDataDto emailSendData = new EmailSendDataDto();
        ObjectMapper mapper = objectMapperProvider.getContext(getClass());
        for (LetterReceivers letterReceiver : letterReceivers) {
            LetterReceiverLetter letter = letterReceiver.getLetterReceiverLetter();
            if (letter == null || letter.getLetter() == null) {
                logger.error("Logical error: trying to send email to non-processed LetterReceivers={}!", letterReceiver.getId());
                continue;
            }

            Template template = baseTemplate;
            String templateLanguage = Optional.fromNullable(template.getLanguage()).or("FI");
            String languageCode = Optional.fromNullable(letterReceiver.getWantedLanguage()).or(templateLanguage);

            if (!languageCode.equals(templateLanguage)) {
                // Get the template in user specific language
                Template languageTemplate = templateService.getTemplateByName(
                        new TemplateCriteriaImpl(template.getName(), languageCode)
                            .withApplicationPeriod(letterBatch.getApplicationPeriod()), true);
                if (languageTemplate != null) {
                    template = languageTemplate;
                } else {
                    // else using the default template
                    languageCode = templateLanguage;
                }
            }

            EmailData emailData = emailSendData.getEmailByLanguageCode(languageCode);
            if (emailData == null) {
                Set<LetterReplacement> batchReplacementsToUse;
                if (template == baseTemplate) {
                    batchReplacementsToUse = letterBatch.getLetterReplacements();
                } else {
                    batchReplacementsToUse = new HashSet<>();
                }
                emailData = buildEmailData(template, batchReplacementsToUse, new EmailData(),
                        languageCode, letterBatch.getApplicationPeriod());
                emailSendData.setEmailByLanguageCode(languageCode, emailData);
            }

            EmailRecipient recipient = null;
            if (shouldReceiveEmail(letterReceiver)) {
                try {
                    recipient = buildRecipient(letterReceiver, mapper);
                    emailData.getRecipient().add(recipient);
                } catch (Exception e) {
                    logger.info("Could not handle email sending for LetterReceivers="
                            + letterReceiver.getId() + " reason {}", e);
                    continue;
                }
            } else {
                logger.debug("Skipped email sending to LetterReceivers={}", letterReceiver.getId());
                continue;
            }

            List<AttachmentUri> attachmentUris = new ArrayList<>();
            for (LetterReceiverLetterAttachment attachment : letter.getAttachments()) {
                attachmentUris.add(AttachmentUri.getLetterReceiverLetterAttachment(attachment.getId()));
            }
            if (!attachmentUris.isEmpty()) {
                recipient.getRecipientReplacements().add(new ReportedRecipientReplacementDTO(
                        ADDITIONAL_ATTACHMENT_URIS_EMAIL_RECEIVER_PARAMETER,
                        AttachmentUri.uriStringsOfList(attachmentUris)));
            }
        }
        return emailSendData;
    }

    private EmailData buildEmailData(Template letterTemplate, Set<LetterReplacement> batchReplacements,
                                     EmailData emailData, String languageCode, String applicationPeriod) {
        EmailMessage message = emailData.getEmail();
        message.setLanguageCode(languageCode);
        message.setTemplateName(letterTemplate.getName());
        message.setHakuOid(applicationPeriod);
        message.setTemplateId("" + letterTemplate.getId());
        message.setHtml(true);

        List<ReadableReplacement> replacements = new ArrayList<>();
        replacements.addAll(letterTemplate.getReplacements());
        replacements.addAll(batchReplacements);
        for (ReadableReplacement replacement : replacements) {
            if (TemplateEmailField.BODY.getFieldName().equals(replacement.getName())) {
                message.setBody(replacement.getDefaultValue());
            } else if (TemplateEmailField.SUBJECT.getFieldName().equals(replacement.getName()) 
                    && replacement.getDefaultValue() != null) {
                message.setSubject(StringEscapeUtils.unescapeHtml(replacement.getDefaultValue()));
            }
            ReplacementDTO replacementDto = new ReplacementDTO();
            replacementDto.setDefaultValue(replacement.getDefaultValue());
            replacementDto.setName(replacement.getName());
            replacementDto.setMandatory(replacement.isMandatory());
            replacementDto.setTimestamp(replacement.getTimestamp());
            emailData.getReplacements().add(replacementDto);
        }
        return emailData;
    }

    private boolean shouldReceiveEmail(LetterReceivers receiver) {
        return (receiver.getLetterReceiverEmail() == null
                && receiver.getEmailAddress() != null && !receiver.getEmailAddress().isEmpty());
    }

    private EmailRecipient buildRecipient(LetterReceivers letter, ObjectMapper mapper) throws IOException {
        String emailAddress = letter.getEmailAddress();
        if (emailAddress == null || emailAddress.isEmpty()) {
            throw new IllegalArgumentException("Vastaanottajat puuttuu");
        }
        EmailRecipient recipient = new EmailRecipient();
        logger.info("letter: ");
        logger.info(letter.toString());
        logger.info("letter: end ");
        recipient.setEmail(letter.getEmailAddress());
        recipient.setLanguageCode(letter.getWantedLanguage());
        if (letter.getLetterReceiverAddress() != null
                && letter.getLetterReceiverAddress().getFirstName() != null
                && letter.getLetterReceiverAddress().getLastName() != null) {
            recipient.setName(letter.getLetterReceiverAddress().getFirstName() + " "
                    + letter.getLetterReceiverAddress().getLastName());
        }
        // TODO: oidType, oid?

        recipient.setRecipientReplacements(new ArrayList<ReportedRecipientReplacementDTO>());
        for (LetterReceiverReplacement replacement :  letter.getLetterReceiverReplacement()) {
            recipient.getRecipientReplacements().add(new ReportedRecipientReplacementDTO(replacement.getName(),
                    replacement.getEffectiveValue(mapper)));
        }
        return recipient;
    }

    private void sendEmails(EmailSendDataDto emailSendData) {
        for (EmailData email : emailSendData.getEmails()) {
            if (!email.getRecipient().isEmpty()) {
                emailComponent.sendEmail(email);
            }
        }
    }

    public void setObjectMapperProvider(ObjectMapperProvider objectMapperProvider) {
        this.objectMapperProvider = objectMapperProvider;
    }
}
