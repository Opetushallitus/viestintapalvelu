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
package fi.vm.sade.viestintapalvelu.service;

import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;

import fi.vm.sade.ryhmasahkoposti.api.dto.EmailData;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailRecipient;
import fi.vm.sade.ryhmasahkoposti.api.dto.ReportedRecipientReplacementDTO;
import fi.vm.sade.viestintapalvelu.dao.LetterBatchDAO;
import fi.vm.sade.viestintapalvelu.dao.criteria.TemplateCriteriaImpl;
import fi.vm.sade.externalinterface.common.ObjectMapperProvider;
import fi.vm.sade.viestintapalvelu.externalinterface.component.EmailComponent;
import fi.vm.sade.viestintapalvelu.letter.DokumenttiIdProvider;
import fi.vm.sade.viestintapalvelu.letter.LetterBatchStatusLegalityChecker;
import fi.vm.sade.viestintapalvelu.letter.LetterBuilder;
import fi.vm.sade.viestintapalvelu.letter.LetterService;
import fi.vm.sade.viestintapalvelu.letter.dto.LanguageCodeOptionsDto;
import fi.vm.sade.viestintapalvelu.letter.impl.LetterEmailServiceImpl;
import fi.vm.sade.viestintapalvelu.letter.impl.LetterServiceImpl;
import fi.vm.sade.viestintapalvelu.model.LetterBatch;
import fi.vm.sade.viestintapalvelu.model.LetterReceiverReplacement;
import fi.vm.sade.viestintapalvelu.model.LetterReceivers;
import fi.vm.sade.viestintapalvelu.model.types.ContentStructureType;
import fi.vm.sade.viestintapalvelu.template.Template;
import fi.vm.sade.viestintapalvelu.template.TemplateService;
import fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData;
import fi.vm.sade.viestintapalvelu.util.CatchSingleParameterAnswer;
import fi.vm.sade.viestintapalvelu.util.DummyDokumenttiIdProvder;

import static fi.vm.sade.viestintapalvelu.util.AnswerChain.atFirstReturn;
import static fi.vm.sade.viestintapalvelu.util.CatchSingleParameterAnswer.catchParameters;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * User: ratamaa
 * Date: 1.10.2014
 * Time: 16:32
 */
@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration("/test-application-context.xml")
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class})
@Transactional(readOnly=true)
public class LetterEmailServiceImplTest {
    @Mock
    private LetterBatchDAO letterBatchDAO;
    @Mock
    private EmailComponent emailComponent;
    @Mock
    private TemplateService templateService;
    @Mock
    private LetterServiceImpl letterService;
    @Mock
    private LetterBuilder letterBuilder;
    @InjectMocks
    private LetterEmailServiceImpl letterEmailService;

    @Before
    public void setup() {
        letterEmailService.setObjectMapperProvider(new ObjectMapperProvider());
        doCallRealMethod().when(letterService).setLetterBatchDAO(any(LetterBatchDAO.class));
        doCallRealMethod().when(letterService).setLetterBatchStatusLegalityChecker(any(LetterBatchStatusLegalityChecker.class));
        doCallRealMethod().when(letterService).setDokumenttiIdProvider(any(DokumenttiIdProvider.class));
        letterService.setLetterBatchDAO(letterBatchDAO);
        letterService.setLetterBatchStatusLegalityChecker(new LetterBatchStatusLegalityChecker());
        letterService.setDokumenttiIdProvider(new DummyDokumenttiIdProvder());
    }

    @Test(expected = IllegalStateException.class)
    public void testSendEmailInvalidStatus() throws Exception {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(1l);
        letterBatch.setBatchStatus(LetterBatch.Status.processing);
        when(letterBatchDAO.read(eq(Long.valueOf(1l)))).thenReturn(letterBatch);
        letterEmailService.sendEmail(1l);
    }

    @Test(expected = IllegalStateException.class)
    public void testSendEmailInvalidEmailHandlingStatus() throws Exception {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(1l);
        letterBatch.setBatchStatus(LetterBatch.Status.ready);
        letterBatch.setEmailHandlingStarted(new Date());
        when(letterBatchDAO.read(eq(Long.valueOf(1l)))).thenReturn(letterBatch);
        letterEmailService.sendEmail(1l);
    }

    @Test(expected = IllegalStateException.class)
    public void testSendEmailNoTemplate() throws Exception {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(1l);
        letterBatch.setBatchStatus(LetterBatch.Status.ready);
        letterBatch.setTemplateId(null);
        when(letterBatchDAO.read(eq(Long.valueOf(1l)))).thenReturn(letterBatch);
        letterEmailService.sendEmail(1l);
    }

    @Test(expected = NotFoundException.class)
    public void testSendEmailTemplateNotFound() throws Exception {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(1l);
        letterBatch.setBatchStatus(LetterBatch.Status.ready);
        when(letterBatchDAO.read(eq(Long.valueOf(1l)))).thenReturn(letterBatch);
        letterEmailService.sendEmail(1l);
    }

    @Test
    public void testSendEmailNoRecipients() throws Exception {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(1l);
        letterBatch.setBatchStatus(LetterBatch.Status.ready);
        when(letterBatchDAO.read(eq(Long.valueOf(1l)))).thenReturn(letterBatch);
        when(templateService.findById(eq(1l), eq(ContentStructureType.email))).thenReturn(DocumentProviderTestData.getTemplate());
        doCallRealMethod().when(letterService).updateBatchProcessingStarted(eq(1l), eq(LetterService.LetterBatchProcess.EMAIL));
        doCallRealMethod().when(letterService).updateBatchProcessingFinished(eq(1l), eq(LetterService.LetterBatchProcess.EMAIL));

        letterEmailService.sendEmail(1l);

        verify(letterService).updateBatchProcessingStarted(eq(1l), eq(LetterService.LetterBatchProcess.EMAIL));
        verify(letterService).updateBatchProcessingFinished(eq(1l), eq(LetterService.LetterBatchProcess.EMAIL));
        assertNotNull(letterBatch.getEmailHandlingStarted());
        assertNotNull(letterBatch.getEmailHandlingFinished());
        verifyZeroInteractions(emailComponent);
    }

    @Test
    public void testSendEmail() throws Exception {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(1l);
        LetterReceivers receiver = letterBatch.getLetterReceivers().iterator().next();
        receiver.setEmailAddress("test@example.com");
        LetterReceiverReplacement replacement = new LetterReceiverReplacement();
        replacement.setName("test");
        replacement.setJsonValue("[1,2,3]");
        receiver.getLetterReceiverReplacement().clear();
        receiver.getLetterReceiverReplacement().add(replacement);

        letterBatch.setBatchStatus(LetterBatch.Status.ready);
        when(letterBatchDAO.read(eq(Long.valueOf(1l)))).thenReturn(letterBatch);
        Template template = DocumentProviderTestData.getTemplate();
        template.setId(1l);
        when(templateService.findById(eq(1l), eq(ContentStructureType.email))).thenReturn(template);
        CatchSingleParameterAnswer<Boolean,EmailData> catchEmailInvocations = catchParameters(atFirstReturn(true)
                .thenThrow(new IllegalStateException("Only one email expected.")));
        when(emailComponent.sendEmail(any(EmailData.class))).then(catchEmailInvocations);

        letterEmailService.sendEmail(1l);

        assertEquals(1, catchEmailInvocations.getInvocationCount());
        EmailData data = catchEmailInvocations.getArguments().get(0);
        assertEquals("1", data.getEmail().getTemplateId());
        assertEquals("test_template", data.getEmail().getTemplateName());
        assertEquals("FI", data.getEmail().getLanguageCode());

        assertEquals(1, data.getRecipient().size());
        assertEquals(2, data.getReplacements().size());
        EmailRecipient recipient = data.getRecipient().get(0);
        assertEquals("test@example.com", recipient.getEmail());
        assertEquals(1, recipient.getRecipientReplacements().size());
        ReportedRecipientReplacementDTO replacementDto = recipient.getRecipientReplacements().get(0);
        assertEquals(Arrays.asList(1,2,3), replacementDto.getEffectiveValue());
    }

    @Test(expected = NotFoundException.class)
    public void testGetPreviewNotFoundByNoEmailAddresses() throws Exception {
        DocumentProviderTestData.getLetterBatch(1l);
        letterEmailService.getPreview(1l, Optional.<String>absent());
    }

    @Test
    public void testGetPreview() throws Exception {
        CatchSingleParameterAnswer<String,EmailData> previewData = catchParameters(atFirstReturn("Test EML data"));
        when(emailComponent.getPreview(any(EmailData.class))).then(previewData);
        expectLetterBatch(1l, expectTemplate(1l, "test_template", "FI"), 3, "SV");

        String result = letterEmailService.getPreview(1l, Optional.<String>absent());
        assertEquals("Test EML data", result);
        assertEquals(1, previewData.getInvocationCount());
        EmailData dataToSendForPreview = previewData.getArguments().get(0);
        assertEquals(1, dataToSendForPreview.getRecipient().size());
    }

    @Test(expected = NotFoundException.class)
    public void testGetPreviewNoReceiverFoundByDefaultLanguage() throws Exception {
        expectLetterBatch(1l, Optional.of("SV"));
        letterEmailService.getPreview(1l, Optional.<String>absent());
    }

    @Test(expected = NotFoundException.class)
    public void testGetPreviewNoReceiverFoundByDesiredLanguage() throws Exception {
        expectLetterBatch(1l, Optional.of("SV"));
        letterEmailService.getPreview(1l, Optional.of("EN"));
    }

    @Test(expected = NotFoundException.class)
    public void testGetPreviewNoReceiverFoundByDesiredLanguageDefault() throws Exception {
        expectLetterBatch(1l, Optional.<String>absent());
        letterEmailService.getPreview(1l, Optional.of("EN"));
    }

    @Test
    public void testPreviewFoundByDesiredLanguage() throws Exception {
        expectLetterBatch(1l, Optional.of("Sv"));
        letterEmailService.getPreview(1l, Optional.of("sV"));
    }

    @Test
    public void testPreviewFoundByTemplateLanguage() throws Exception {
        expectLetterBatch(1l, Optional.<String>absent());
        letterEmailService.getPreview(1l, Optional.of("fi"));
    }

    @Test
    public void testGetLanguageCodeOptions() throws Exception {
        expectLetterBatch(1l, Optional.<String>absent());
        LanguageCodeOptionsDto options = letterEmailService.getLanguageCodeOptions(1l);
        assertNotNull(options);
        assertNotNull(options.getOptions());
        assertEquals(1, options.getOptions().size());
        assertEquals("FI", options.getOptions().get(0));
    }

    @Test
    public void testGetLanguageCodeOptionsWithDifferingReceiver() throws Exception {
        expectLetterBatch(1l, Optional.of("SV"));
        LanguageCodeOptionsDto options = letterEmailService.getLanguageCodeOptions(1l);
        assertNotNull(options);
        assertNotNull(options.getOptions());
        assertEquals(0, options.getOptions().size());
    }

    @Test
    public void testGetLanguageCodeOptionsWithSpecifiedReceiver() throws Exception {
        expectLetterBatch(1l, Optional.of("FI"));
        LanguageCodeOptionsDto options = letterEmailService.getLanguageCodeOptions(1l);
        assertEquals(1, options.getOptions().size());
        assertEquals("FI", options.getOptions().get(0));
    }

    @Test
    public void testGetLanguageCodeOptionsWithOtherLanguageTemplate() throws Exception {
        expectLetterBatch(1l, expectTemplate(1l, "test_template", "SV"), 1, "SV");
        LanguageCodeOptionsDto options = letterEmailService.getLanguageCodeOptions(1l);
        assertEquals(1, options.getOptions().size());
        assertEquals("SV", options.getOptions().get(0));
    }

    @Test
    public void testGetLanguageCodeOptionsWithMultilangeRecipeints() throws Exception {
        expectLetterBatch(1l, expectTemplate(1l, "test_template", "FI"),
                5, null, "sV", "en", null, "Sv");
        expectTemplate(2l, "test_template", "SV");
        expectTemplate(3l, "test_template", "EN");

        LanguageCodeOptionsDto options = letterEmailService.getLanguageCodeOptions(1l);
        assertEquals(3, options.getOptions().size());
        assertTrue(options.getOptions().containsAll(Arrays.asList("FI", "EN", "SV")));
    }

    @Test
    public void testGetLanguageCodeOptionsWithMultilangeRecipeintsWithoutTemplateLanguage() throws Exception {
        expectLetterBatch(1l, expectTemplate(1l, "test_template", "FI"),
                5, null, "sV", "en", null, "Sv");
        expectTemplate(2l, "test_template", "EN");

        LanguageCodeOptionsDto options = letterEmailService.getLanguageCodeOptions(1l);
        assertEquals(2, options.getOptions().size());
        assertTrue(options.getOptions().containsAll(Arrays.asList("FI", "EN")));
    }

    @Test(expected = NotFoundException.class)
    public void getNonExistentLetterBatchLangOptions() {
        long id = 123l;
        when(letterBatchDAO.read(eq(id))).thenReturn(null);
        letterEmailService.getLanguageCodeOptions(123l);
    }


    private LetterBatch expectLetterBatch(Long id, Optional<String> receiverLanguage) {
        Template template = expectTemplate(1l, "test_template", "FI");
        if (receiverLanguage.isPresent()) {
            return expectLetterBatch(id, template, 1, receiverLanguage.get());
        } else {
            return expectLetterBatch(id, template, 1);
        }
    }

    private Template expectTemplate(Long id, String name, String language) {
        Template template = DocumentProviderTestData.getTemplate();
        template.setName(name);
        template.setLanguage(language);
        template.setId(id);
        when(templateService.findById(eq(id), eq(ContentStructureType.email))).thenReturn(template);
        when(templateService.getTemplateByName(eq(new TemplateCriteriaImpl()
                        .withName(template.getName())
                        .withLanguage(template.getLanguage().toUpperCase())
        ), any(boolean.class))).thenReturn(template);
        return template;
    }

    private LetterBatch expectLetterBatch(Long id, Template template,
                                          int numberOfReceivers, String... receiverLanguages) {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(id);
        letterBatch.setTemplateId(template.getId());
        letterBatch.setTemplateName(template.getName());
        letterBatch.setLanguage(template.getLanguage());
        letterBatch.setApplicationPeriod(null);
        letterBatch.getLetterReceivers().clear();
        Set<LetterReceivers> receivers = DocumentProviderTestData.getLetterReceivers(20l, letterBatch, numberOfReceivers);
        Iterator<LetterReceivers> rIt = receivers.iterator();
        for (String receiverLang : receiverLanguages) {
            rIt.next().setWantedLanguage(receiverLang);
        }
        for (LetterReceivers receiver : receivers) {
            receiver.setEmailAddress("test@example.com");
            letterBatch.getLetterReceivers().add(receiver);
        }
        when(letterBatchDAO.read(eq(Long.valueOf(1l)))).thenReturn(letterBatch);
        return letterBatch;
    }
}
