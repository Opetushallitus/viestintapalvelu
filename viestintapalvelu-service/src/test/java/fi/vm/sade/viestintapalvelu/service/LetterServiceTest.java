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

import static fi.vm.sade.viestintapalvelu.util.CatchParametersAnswers.catchAllParameters;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

import fi.vm.sade.externalinterface.common.ObjectMapperProvider;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.viestintapalvelu.dao.IPostiDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterBatchDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterReceiverLetterDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterReceiversDAO;
import fi.vm.sade.viestintapalvelu.dao.TemplateDAO;
import fi.vm.sade.viestintapalvelu.dao.criteria.TemplateCriteria;
import fi.vm.sade.viestintapalvelu.dao.dto.LetterBatchStatusDto;
import fi.vm.sade.viestintapalvelu.dao.dto.LetterBatchStatusErrorDto;
import fi.vm.sade.viestintapalvelu.document.DocumentBuilder;
import fi.vm.sade.viestintapalvelu.dto.letter.LetterReceiverLetterDTO;
import fi.vm.sade.viestintapalvelu.externalinterface.component.CurrentUserComponent;
import fi.vm.sade.viestintapalvelu.letter.LetterBatchStatusLegalityChecker;
import fi.vm.sade.viestintapalvelu.letter.LetterBuilder;
import fi.vm.sade.viestintapalvelu.letter.LetterContent;
import fi.vm.sade.viestintapalvelu.letter.LetterListItem;
import fi.vm.sade.viestintapalvelu.letter.LetterListResponse;
import fi.vm.sade.viestintapalvelu.letter.LetterPublisher;
import fi.vm.sade.viestintapalvelu.letter.LetterService.LetterBatchProcess;
import fi.vm.sade.viestintapalvelu.letter.dto.AsyncLetterBatchDto;
import fi.vm.sade.viestintapalvelu.letter.dto.converter.LetterBatchDtoConverter;
import fi.vm.sade.viestintapalvelu.letter.impl.LetterServiceImpl;
import fi.vm.sade.viestintapalvelu.letter.processing.IPostiProcessable;
import fi.vm.sade.viestintapalvelu.model.IPosti;
import fi.vm.sade.viestintapalvelu.model.LetterBatch;
import fi.vm.sade.viestintapalvelu.model.LetterBatchGeneralProcessingError;
import fi.vm.sade.viestintapalvelu.model.LetterBatchIPostProcessingError;
import fi.vm.sade.viestintapalvelu.model.LetterBatchLetterProcessingError;
import fi.vm.sade.viestintapalvelu.model.LetterReceiverLetter;
import fi.vm.sade.viestintapalvelu.model.LetterReceivers;
import fi.vm.sade.viestintapalvelu.model.types.ContentTypes;
import fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData;
import fi.vm.sade.viestintapalvelu.util.CatchParametersAnswers;
import fi.vm.sade.viestintapalvelu.util.DummyDokumenttiIdProvder;
import org.apache.cxf.helpers.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration("/test-application-context.xml")
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class,
    DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class})
@Transactional(readOnly=true)
public class LetterServiceTest {
    @Mock
    private LetterBatchDAO mockedLetterBatchDAO;
    @Mock
    private LetterReceiverLetterDAO mockedLetterReceiverLetterDAO;
    @Mock
    private LetterReceiversDAO mockedLetterReceiversDao;
    @Mock
    private CurrentUserComponent mockedCurrentUserComponent;
    @Mock
    private TemplateDAO templateDAO;
    private LetterServiceImpl letterService;
    @Mock
    private LetterBuilder letterBuilder;
    @Mock
    private IPostiDAO iPostiDAO;
    @Mock
    private DokumenttiResource dokumenttipalveluRestClient;
    @Mock
    private LetterPublisher letterPublisher;

    @Before
    public void setup() {
        this.letterService = new LetterServiceImpl(mockedLetterBatchDAO, mockedLetterReceiverLetterDAO,
            mockedCurrentUserComponent, templateDAO, new LetterBatchDtoConverter(),
            mockedLetterReceiversDao, new ObjectMapperProvider(), iPostiDAO, new LetterBatchStatusLegalityChecker(),
            new DocumentBuilder(), new DummyDokumenttiIdProvder(), letterPublisher);
        this.letterService.setLetterBuilder(letterBuilder);
        this.letterService.setDokumenttipalveluRestClient(dokumenttipalveluRestClient);
    }

    @Test
    public void testCreateLetter() {
        AsyncLetterBatchDto letterBatch = DocumentProviderTestData.getAsyncLetterBatch();

        when(mockedCurrentUserComponent.getCurrentUser()).thenReturn(DocumentProviderTestData.getHenkilo().getOidHenkilo());
        when(mockedLetterBatchDAO.insert(any(LetterBatch.class))).thenAnswer(new Answer<LetterBatch>() {
            @Override
            public LetterBatch answer(InvocationOnMock invocation) throws Throwable {
              return (LetterBatch) invocation.getArguments()[0];
            }
        });
        when(templateDAO.findTemplate(any(TemplateCriteria.class)))
            .thenReturn(DocumentProviderTestData.getTemplate(1l));
        LetterBatch createdLetterBatch = letterService.createLetter(letterBatch, false);

        assertNotNull(createdLetterBatch);
        assertNotNull(createdLetterBatch.getLetterReceivers());
        assertTrue(createdLetterBatch.getLetterReceivers().size() > 0);
        assertNotNull(createdLetterBatch.getLetterReplacements());
        assertTrue(createdLetterBatch.getLetterReplacements().size() > 0);
        assertNotNull(createdLetterBatch.getTemplateId());
        assertEquals(1, createdLetterBatch.getUsedTemplates().size());
    }

    @Test
    public void testFindById() {
        List<LetterBatch> mockedLetterBatchList = new ArrayList<LetterBatch>();
        mockedLetterBatchList.add(DocumentProviderTestData.getLetterBatch(1l));
        when(mockedLetterBatchDAO.findBy(any(String.class), any(Object.class))).thenReturn(mockedLetterBatchList);

        fi.vm.sade.viestintapalvelu.letter.LetterBatch letterBatchFindById = letterService.findById(new Long(1));

        assertNotNull(letterBatchFindById);
        assertNotNull(letterBatchFindById.getTemplateId());
    }

    @Test
    @SuppressWarnings({"unchecked"})
    public void testFindLetterBatchByNameOrgTag() {
        when(mockedLetterBatchDAO.findLetterBatchByNameOrgTag(any(String.class), eq("FI"), any(String.class),
                any(Optional.class), any(Optional.class))).thenReturn(DocumentProviderTestData.getLetterBatch(1l));

        fi.vm.sade.viestintapalvelu.letter.LetterBatch foundLetterBatch =
            letterService.findLetterBatchByNameOrgTag("test-template", "FI", "1.2.246.562.10.00000000001",
                    Optional.of("test-tag"),
                    Optional.<String>fromNullable(null));

        assertNotNull(foundLetterBatch);
        assertTrue(foundLetterBatch.getTemplateId() == 1);
        assertTrue(foundLetterBatch.getTemplateName().equals("test-templateName"));
        assertTrue(foundLetterBatch.getLetters() == null);
        assertNotNull(foundLetterBatch.getTemplateReplacements());
        assertTrue(foundLetterBatch.getTemplateReplacements().size() > 0);
    }

    @Test
    public void testFindReplacementByNameOrgTag() {
        when(mockedLetterBatchDAO.findLetterBatchByNameOrg(any(String.class), eq("FI"),
            any(String.class))).thenReturn(DocumentProviderTestData.getLetterBatch(1l));

        List<fi.vm.sade.viestintapalvelu.template.Replacement> replacements =
            letterService.findReplacementByNameOrgTag("test-templateName", "FI", "1.2.246.562.10.00000000001",
                    Optional.<String>absent(),
                    Optional.<String>absent());

        assertNotNull(replacements);
        assertTrue(replacements.size() > 0);
        assertTrue(replacements.get(0).getName().equals("test-replacement-name"));
        assertTrue(!replacements.get(0).isMandatory());
        assertTrue(replacements.get(0).getDefaultValue().isEmpty());
    }

    @Test
    public void testGetLetter() {
        List<LetterBatch> mockedLetterBatchList = new ArrayList<LetterBatch>();
        mockedLetterBatchList.add(DocumentProviderTestData.getLetterBatch(1l));

        List<LetterReceivers> mockedLetterReceiversList =
            new ArrayList<LetterReceivers>(mockedLetterBatchList.get(0).getLetterReceivers());

        List<LetterReceiverLetter> mockedLetterReceiverLetterList = new ArrayList<LetterReceiverLetter>();
        mockedLetterReceiverLetterList.add(mockedLetterReceiversList.get(0).getLetterReceiverLetter());

        when(mockedLetterReceiverLetterDAO.findBy(any(String.class), any(Object.class))).thenReturn(
            mockedLetterReceiverLetterList);

        LetterContent letterContent = letterService.getLetter(new Long(1));

        assertNotNull(letterContent);
        assertTrue(letterContent.getContentType().equalsIgnoreCase(ContentTypes.CONTENT_TYPE_PDF));
        assertNotNull(letterContent.getContent());
        assertTrue(new String(letterContent.getContent()).equalsIgnoreCase("letter"));
    }

    @Test
    public void setsHandlingStartedWhenStartingProcessIsCalled() {
        LetterBatch batch = DocumentProviderTestData.getLetterBatch(1l);
        when(mockedLetterBatchDAO.read(any(Long.class))).thenReturn(batch);
        letterService.updateBatchProcessingStarted(1l, LetterBatchProcess.LETTER);
        assertNotNull(batch.getHandlingStarted());
    }

    @Test
    public void testHandleLetterProcessesFinished() throws Exception {
        LetterBatch batch = DocumentProviderTestData.getLetterBatch(1l);
        InputStream pdfIs = this.getClass().getResourceAsStream("/testfiles/test.pdf"),
                pdf2Is = this.getClass().getResourceAsStream("/testfiles/test2.pdf");
        batch.getLetterReceivers().iterator().next()
                .getLetterReceiverLetter().setLetter((IOUtils.readBytesFromStream(pdfIs)));
        LetterReceivers anotherReceiver = DocumentProviderTestData.getLetterReceivers(20l, batch).iterator().next();
        anotherReceiver.getLetterReceiverLetter().setLetter(IOUtils.readBytesFromStream(pdf2Is));
        batch.getLetterReceivers().add(anotherReceiver);

        when(mockedLetterBatchDAO.read(any(Long.class))).thenReturn(batch);
        CatchParametersAnswers<Void> catchParameter = catchAllParameters();
        doAnswer(catchParameter).when(dokumenttipalveluRestClient)
                .tallenna(any(String.class), any(String.class), any(Long.class),
                        any(List.class), any(String.class), any(InputStream.class));
        letterService.updateBatchProcessingFinished(1l, LetterBatchProcess.LETTER);

        assertEquals(LetterBatch.Status.ready, batch.getBatchStatus());
        assertNotNull(batch.getHandlingFinished());
    }

    @Test
    public void testHandleIpostiProcessesFinished() throws Exception {
        LetterBatch batch = DocumentProviderTestData.getLetterBatch(1l);
        batch.setIposti(true);
        batch.setBatchStatus(LetterBatch.Status.processing_ipost);
        InputStream pdfIs = this.getClass().getResourceAsStream("/testfiles/test.pdf"),
                pdf2Is = this.getClass().getResourceAsStream("/testfiles/test2.pdf");
        batch.getLetterReceivers().iterator().next()
                .getLetterReceiverLetter().setLetter((IOUtils.readBytesFromStream(pdfIs)));
        LetterReceivers anotherReceiver = DocumentProviderTestData.getLetterReceivers(20l, batch).iterator().next();
        anotherReceiver.getLetterReceiverLetter().setLetter(IOUtils.readBytesFromStream(pdf2Is));
        batch.getLetterReceivers().add(anotherReceiver);

        Map<String, Supplier<byte[]>> zipMap = new HashMap<>();
        DocumentBuilder builder = new DocumentBuilder();
        zipMap.put("testpdf.pdf", new Supplier<byte[]>() {
            @Override
            public byte[] get() {
                try {
                    return IOUtils.readBytesFromStream(this.getClass().getResourceAsStream("/testfiles/test.pdf"));
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
        byte[] zipContent = new DocumentBuilder().zip(zipMap);
        assertNotNull(zipContent);
        IPosti ipost = new IPosti();
        ipost.setContent(zipContent);
        ipost.setContentName("zip1.zip");
        ipost.setContentType("application/zip");
        ipost.setLetterBatch(batch);
        ipost.setOrderNumber(1);
        batch.addIPosti(ipost);

        when(mockedLetterBatchDAO.read(eq(batch.getId()))).thenReturn(batch);
        CatchParametersAnswers<Void> catchParameter = catchAllParameters();
        lenient().doAnswer(catchParameter).when(dokumenttipalveluRestClient)
                .tallenna(any(String.class), any(String.class), any(Long.class),
                        any(List.class), any(String.class), any(InputStream.class));
        letterService.updateBatchProcessingFinished(1l, LetterBatchProcess.IPOSTI);

        assertEquals(LetterBatch.Status.ready, batch.getBatchStatus());
        assertNotNull(batch.getIpostHandlingFinished());
    }

    @Test
    public void setsEmailHandlingStartedWhenStartingProcessIsCalled() {
        LetterBatch batch = DocumentProviderTestData.getLetterBatch(1l);
        when(mockedLetterBatchDAO.read(any(Long.class))).thenReturn(batch);
        letterService.updateBatchProcessingStarted(1l, LetterBatchProcess.EMAIL);
        assertNotNull(batch.getEmailHandlingStarted());
    }

    @Test
    public void setsEmailHandlingFinishedWhenFinishingProcessIsCalled() throws Exception {
        LetterBatch batch = DocumentProviderTestData.getLetterBatch(1l);
        when(mockedLetterBatchDAO.read(any(Long.class))).thenReturn(batch);
        letterService.updateBatchProcessingFinished(1l, LetterBatchProcess.EMAIL);
        assertNotNull(batch.getEmailHandlingFinished());
    }

    @Test
    public void usesDAOWhenFetchingLetterBatch() {
        letterService.fetchById(1l);
        verify(mockedLetterBatchDAO).read(1l);
    }

    @Test
    public void usesDAOWhenUpdatingLetterReceiverLetter() {
        LetterReceiverLetter letter = new LetterReceiverLetter();
        letterService.updateLetter(letter);
        verify(mockedLetterReceiverLetterDAO).update(letter);
    }

    @Test
    public void getBatchStatusSuccess() {

        System.out.println("getBatchStatusFailure");

        LetterBatchStatusDto statusDto = new LetterBatchStatusDto(123l, 0, 0, LetterBatch.Status.ready, 0);
        when(mockedLetterBatchDAO.getLetterBatchStatus(eq(123l))).thenReturn(statusDto);

        LetterBatchStatusDto batchStatus = letterService.getBatchStatus(123l);

        assertEquals("Batch status should be ready when sent and total match", LetterBatch.Status.ready, batchStatus.getStatus());
        assertNull("Batch errors should be null when successful", batchStatus.getErrors());

        statusDto = new LetterBatchStatusDto(1234l, 456, 456, LetterBatch.Status.ready, 456);
        when(mockedLetterBatchDAO.getLetterBatchStatus(eq(1234l))).thenReturn(statusDto);
        batchStatus = letterService.getBatchStatus(1234l);

        System.out.println("batchStatus = " + batchStatus);
        assertEquals("Batch status should be ready when sent and total match", LetterBatch.Status.ready, batchStatus.getStatus());
        assertNull("Batch errors should be null when successful", batchStatus.getErrors());
    }

    @Test
    public void getBatchStatusSending() {

        LetterBatchStatusDto statusDto = new LetterBatchStatusDto(1238l, 45, 123, LetterBatch.Status.processing, 40);
        when(mockedLetterBatchDAO.getLetterBatchStatus(eq(1238l))).thenReturn(statusDto);
        LetterBatchStatusDto batchStatus = letterService.getBatchStatus(1238l);

        assertEquals("Batch status should be sending when the process is still going", LetterBatch.Status.processing, batchStatus.getStatus());
    }

    @Test
    public void getBatchStatusBatchNotFound() {
        final long batchId = 123;

        LetterBatchLetterProcessingError expectedError = new LetterBatchLetterProcessingError();
        expectedError.setErrorCause("Batch not found for id " + batchId);
        expectedError.setErrorTime(new Date()); //can't check real time as the service just uses the current time in this case

        when(mockedLetterBatchDAO.getLetterBatchStatus(eq(batchId))).thenReturn(null);

        LetterBatchStatusDto batchStatus = letterService.getBatchStatus(batchId);
        assertEquals(LetterBatch.Status.error, batchStatus.getStatus());
        assertEquals("Error message didn't match", expectedError.getErrorCause(), batchStatus.getErrors().get(0).getErrorCause());
    }


    @Test
    public void getBatchStatusFailure() {
        final Date failDate = new Date();

        LetterBatch mockBatch = DocumentProviderTestData.getLetterBatch(1235l);
        assertNotNull(mockBatch.getLetterReceivers());
        LetterReceivers first = mockBatch.getLetterReceivers().iterator().next();

        LetterBatchLetterProcessingError error = new LetterBatchLetterProcessingError();
        error.setLetterBatch(mockBatch);
        error.setErrorCause("Testing error");
        error.setErrorTime(failDate);
        error.setLetterReceivers(first);
        mockBatch.addProcessingErrors(error);

        LetterBatchStatusDto mockDto = new LetterBatchStatusDto(1235l, 235, 456, LetterBatch.Status.error, 235);

        when(mockedLetterBatchDAO.getLetterBatchStatus(eq(1235l))).thenReturn(mockDto);
        when(mockedLetterBatchDAO.read(eq(1235l))).thenReturn(mockBatch);

        LetterBatchStatusDto statusDto = letterService.getBatchStatus(1235l);
        assertEquals("Batch processing should be indicated with an 'error' status", LetterBatch.Status.error, statusDto.getStatus());
        assertEquals(1, statusDto.getErrors().size());
        assertEquals("Testing error", statusDto.getErrors().get(0).getErrorCause());
        assertTrue(statusDto.getErrors().get(0) != null);
        assertNotNull("Letter receivers must not be null", ((LetterBatchStatusErrorDto)statusDto.getErrors().get(0))
                .getRecipientId());
    }

    @Test
    public void saveBatchProcessingError() {
        long batchId = 101l;
        long letterReceiverId = 999l;
        LetterBatch batch = DocumentProviderTestData.getLetterBatch(batchId);
        Set<LetterReceivers> letterReceivers = DocumentProviderTestData.getLetterReceivers(letterReceiverId, batch);
        LetterReceivers receivers = letterReceivers.iterator().next();
        batch.setLetterReceivers(letterReceivers);
        when(mockedLetterReceiversDao.read(eq(letterReceiverId))).thenReturn(receivers);

        String msg = "test message";
        letterService.saveBatchErrorForReceiver(999l, msg);
        assertNotNull(batch.getProcessingErrors());
        assertEquals(1,batch.getProcessingErrors().size());
    }

    @Test
    public void usesLetterBatchDaoForFetchingUnfinishedLetterBatches() {
        letterService.findUnfinishedLetterBatches();
        verify(mockedLetterBatchDAO).findUnfinishedLetterBatches();
    }

    @Test
    public void testGeneralError() {
        long batchId = 101l;
        LetterBatch batch = DocumentProviderTestData.getLetterBatch(batchId);
        when(mockedLetterBatchDAO.read(eq(batchId))).thenReturn(batch);

        letterService.errorProcessingBatch(batchId, new Exception("test_msg"));
        assertEquals(LetterBatch.Status.error, batch.getBatchStatus());
        assertEquals(1, batch.getProcessingErrors().size());
        assertTrue(batch.getProcessingErrors().iterator().next() instanceof LetterBatchGeneralProcessingError);
        assertEquals("test_msg", batch.getProcessingErrors().iterator().next().getErrorCause());
    }

    @Test
    public void testIpostiError() {
        long batchId = 101l;
        LetterBatch batch = DocumentProviderTestData.getLetterBatch(batchId);
        batch.setIposti(true);
        batch.setBatchStatus(LetterBatch.Status.processing_ipost);
        when(mockedLetterBatchDAO.read(eq(batchId))).thenReturn(batch);

        letterService.handleIpostError(new IPostiProcessable(batchId, 1), new Exception("test_msg"));
        assertEquals(LetterBatch.Status.error, batch.getBatchStatus());
        assertEquals(1, batch.getProcessingErrors().size());
        assertTrue(batch.getProcessingErrors().iterator().next() instanceof LetterBatchIPostProcessingError);
        assertEquals("test_msg", batch.getProcessingErrors().iterator().next().getErrorCause());
    }

    @Test
    public void testReceiverError() {
        long batchId = 101l,
            receiverId = 23l;
        LetterBatch batch = DocumentProviderTestData.getLetterBatch(batchId);
        batch.setBatchStatus(LetterBatch.Status.processing);
        LetterReceivers receiver = DocumentProviderTestData.getLetterReceivers(receiverId, batch).iterator().next();
        when(mockedLetterReceiversDao.read(eq(receiverId))).thenReturn(receiver);

        letterService.saveBatchErrorForReceiver(receiverId, "test_msg");
        assertEquals(LetterBatch.Status.error, batch.getBatchStatus());
        assertEquals(1, batch.getProcessingErrors().size());
        assertTrue(batch.getProcessingErrors().iterator().next() instanceof LetterBatchLetterProcessingError);
        assertEquals("test_msg", batch.getProcessingErrors().iterator().next().getErrorCause());
        LetterBatchLetterProcessingError error = ((LetterBatchLetterProcessingError)batch.getProcessingErrors().iterator().next());
        assertNotNull(error.getLetterReceivers());
        assertEquals(Long.valueOf(receiverId), error.getLetterReceivers().getId());
    }

    @Test
    public void testListLettersByUser() {
        List<LetterListItem> list = Arrays.asList(new LetterListItem(123l, "test-haku-oid", "jalkiohjauskirje", ContentTypes.CONTENT_TYPE_PDF, new Date()));

        when(mockedLetterBatchDAO.findLettersReadyForPublishByPersonOid("test-person-oid")).thenReturn(list);

        LetterListResponse response = letterService.listLettersByUser("test-person-oid");
        assertEquals(1, response.getLetters().size());
        assertEquals(123l, response.getLetters().get(0).getId());
    }

    @Test
    public void testListLettersByUserMissingOid() {
        LetterListResponse response = letterService.listLettersByUser(null);
        assertEquals(null, response.getLetters());
    }

    @Test
    public void testGetLetterReceiverLetter() throws IOException, DataFormatException {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(new Long(1));
        Set<LetterReceivers> letterReceiversSet = DocumentProviderTestData.getLetterReceivers(new Long(2), letterBatch);
        LetterReceivers letterReceivers = letterReceiversSet.iterator().next();
        LetterReceiverLetter mockedLetterReceiverLetter =
                DocumentProviderTestData.getLetterReceiverLetter(new Long(3), letterReceivers);
        when(mockedLetterReceiverLetterDAO.read(any(Long.class))).thenReturn(mockedLetterReceiverLetter);

        LetterReceiverLetterDTO letterReceiverLetterDTO = letterService.getLetterReceiverLetter(new Long(3));

        assertNotNull(letterReceiverLetterDTO);
        assertTrue(new String(letterReceiverLetterDTO.getLetter()).equals("letter"));
    }
}
