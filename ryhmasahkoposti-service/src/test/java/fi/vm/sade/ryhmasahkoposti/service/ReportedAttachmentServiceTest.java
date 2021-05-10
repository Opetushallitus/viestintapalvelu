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
package fi.vm.sade.ryhmasahkoposti.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.sade.ryhmasahkoposti.api.dto.AttachmentResponse;
import fi.vm.sade.ryhmasahkoposti.dao.ReportedAttachmentDAO;
import fi.vm.sade.ryhmasahkoposti.model.ReportedAttachment;
import fi.vm.sade.ryhmasahkoposti.service.impl.ReportedAttachmentServiceImpl;
import fi.vm.sade.ryhmasahkoposti.testdata.RaportointipalveluTestData;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration("/test-bundle-context.xml")
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class })
@Transactional(readOnly = true)
public class ReportedAttachmentServiceTest {
    @Mock
    ReportedAttachmentDAO mockedReportedAttachmentDAO;
    private ReportedAttachmentService reportedAttachmentService;

    @Before
    public void setup() {
        reportedAttachmentService = new ReportedAttachmentServiceImpl(mockedReportedAttachmentDAO);
    }

    @Test
    public void testSaveReportedAttachment() {
        ReportedAttachment reportedAttachment = RaportointipalveluTestData.getReportedAttachment();
        ReportedAttachment savedReportedAttachment = RaportointipalveluTestData.getReportedAttachment();
        savedReportedAttachment.setId(new Long(1));

        when(mockedReportedAttachmentDAO.insert(reportedAttachment)).thenReturn(savedReportedAttachment);

        Long liitteenID = reportedAttachmentService.saveReportedAttachment(reportedAttachment);

        assertNotNull(liitteenID);
        assertTrue(liitteenID.longValue() > 0);
    }

    @Test
    public void testGetReportedAttachmentsByattachmentResponses() {
        ReportedAttachment reportedAttachment = RaportointipalveluTestData.getReportedAttachment();
        reportedAttachment.setId(new Long(1));

        FileItem mockedFileItem = mock(FileItem.class);
        byte[] sisalto = { 'k', 'o', 'e', 'k', 'u', 't', 's', 'u' };

        when(mockedFileItem.getName()).thenReturn("Koekutsu");
        when(mockedFileItem.getContentType()).thenReturn("application/pdf");
        lenient().when(mockedFileItem.get()).thenReturn(sisalto);

        List<AttachmentResponse> attachmentResponses = new ArrayList<AttachmentResponse>();
        attachmentResponses.add(RaportointipalveluTestData.getAttachmentResponse(reportedAttachment.getId(), mockedFileItem));

        when(mockedReportedAttachmentDAO.read(any(Long.class))).thenReturn(reportedAttachment);

        List<ReportedAttachment> reportedAttachments = reportedAttachmentService.getReportedAttachments(attachmentResponses);

        assertNotNull(reportedAttachments);
        assertEquals(reportedAttachments.get(0).getId(), new Long(1));
    }
}
