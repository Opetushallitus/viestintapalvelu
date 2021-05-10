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

import fi.vm.sade.dto.HenkiloDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import fi.vm.sade.dto.PagingAndSortingDTO;
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

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.viestintapalvelu.dao.IPostiDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterBatchDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterReceiverLetterDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterReceiversDAO;
import fi.vm.sade.viestintapalvelu.dto.letter.LetterBatchReportDTO;
import fi.vm.sade.viestintapalvelu.dto.letter.LetterBatchesReportDTO;
import fi.vm.sade.viestintapalvelu.dto.query.LetterReportQueryDTO;
import fi.vm.sade.viestintapalvelu.externalinterface.component.CurrentUserComponent;
import fi.vm.sade.viestintapalvelu.externalinterface.component.HenkiloComponent;
import fi.vm.sade.viestintapalvelu.externalinterface.component.OrganizationComponent;
import fi.vm.sade.viestintapalvelu.externalinterface.organisaatio.OrganisaatioService;
import fi.vm.sade.viestintapalvelu.letter.LetterReportService;
import fi.vm.sade.viestintapalvelu.letter.impl.LetterReportServiceImpl;
import fi.vm.sade.viestintapalvelu.model.IPosti;
import fi.vm.sade.viestintapalvelu.model.LetterBatch;
import fi.vm.sade.viestintapalvelu.model.LetterReceivers;
import fi.vm.sade.viestintapalvelu.template.TemplateService;
import fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration("/test-application-context.xml")
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class, 
    DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class})
@Transactional(readOnly=true)
public class LetterReportServiceTest {
    @Mock
    private LetterBatchDAO mockedLetterBatchDAO;
    @Mock
    private LetterReceiversDAO mockedLetterReceiversDAO;
    @Mock
    private LetterReceiverLetterDAO mockedLetterReceiverLetterDAO;
    @Mock
    private IPostiDAO mockedIPostiDAO;
    @Mock
    private TemplateService mockedTemplateService;
    @Mock
    private CurrentUserComponent mockedCurrentUserComponent;
    @Mock
    private OrganizationComponent mockedOrganizationComponent;
    @Mock
    private HenkiloComponent mockedHenkiloComponent;
    @Mock
    private OrganisaatioService organisaatioService;

    private LetterReportService letterReportService;
    
    @Before
    public void setup() {
        this.letterReportService = new LetterReportServiceImpl(mockedLetterBatchDAO, mockedLetterReceiversDAO, 
            mockedLetterReceiverLetterDAO, mockedIPostiDAO, mockedTemplateService, mockedCurrentUserComponent, 
            mockedOrganizationComponent, mockedHenkiloComponent, organisaatioService);
    }
    
    @Test
    public void testGetLetterBatchReport() {
        //         List<LetterReceivers> letterReceiverList = letterReceiversDAO.findLetterReceiversByLetterBatchID(letterBatchID, pagingAndSorting, query);
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(new Long(1));
        Set<LetterReceivers> letterReceivers = DocumentProviderTestData.getLetterReceivers(new Long(2), letterBatch);
        List<LetterReceivers> mockedLetterReceivers = new ArrayList<LetterReceivers>(letterReceivers);
        
        when(mockedLetterReceiversDAO.findLetterReceiversByLetterBatchID(
            any(Long.class), any(PagingAndSortingDTO.class))).thenReturn(mockedLetterReceivers);
        when(mockedLetterReceiversDAO.findLetterReceiversByLetterBatchID(
                any(Long.class), any(PagingAndSortingDTO.class), eq(null))).thenReturn(mockedLetterReceivers);
        when(mockedLetterReceiversDAO.findNumberOfReciversByLetterBatchID(any(Long.class), eq(null))).thenReturn(1L);
        
        OrganisaatioRDTO organisaatio = DocumentProviderTestData.getOrganisaatioRDTO();
        when(mockedOrganizationComponent.getOrganization(any(String.class))).thenReturn(organisaatio);
        when(mockedOrganizationComponent.getNameOfOrganisation(any(OrganisaatioRDTO.class))).thenReturn("oppilaitos");
        
        List<IPosti> mockedIPostis = DocumentProviderTestData.getIPosti(new Long(3), letterBatch);
        when(mockedIPostiDAO.findMailById(any(Long.class))).thenReturn(mockedIPostis);
        when(mockedIPostiDAO.findByLetterBatchId(any(Long.class))).thenReturn(mockedIPostis);
        
        when(mockedHenkiloComponent.getHenkilo(any(String.class))).thenReturn(new HenkiloDto());
        PagingAndSortingDTO pagingAndSorting = DocumentProviderTestData.getPagingAndSortingDTO();
        
        LetterBatchReportDTO letterBatchReport = letterReportService.getLetterBatchReport(new Long(1), pagingAndSorting, null);
        assertNotNull(letterBatchReport);
        assertEquals(letterBatchReport.getApplicationPeriod(), letterBatch.getApplicationPeriod());
        assertEquals(letterBatchReport.getFetchTarget(), "fetchTarget");
        assertEquals(letterBatchReport.getLetterBatchID(), new Long(1));
        assertEquals(1, letterBatchReport.getLetterReceivers().size());
        assertTrue(letterBatchReport.getiPostis().size() > 0);
    }
    
    @Test
    public void testGetLetterBatchesBySearchArgument() {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(1l);
        List<LetterBatchReportDTO> mockedLetterBatches = new ArrayList<LetterBatchReportDTO>();
        mockedLetterBatches.add(new LetterBatchReportDTO(
                letterBatch.getId(),
                letterBatch.getTemplateId(),
                letterBatch.getTemplateName(),
                letterBatch.getApplicationPeriod(),
                letterBatch.getFetchTarget(),
                letterBatch.getTag(),
                letterBatch.isIposti(),
                letterBatch.getTimestamp(),
                letterBatch.getOrganizationOid(),
                letterBatch.getBatchStatus()
        ));
        when(mockedLetterBatchDAO.findLetterBatchesBySearchArgument(
            any(LetterReportQueryDTO.class), any(PagingAndSortingDTO.class))).thenReturn(mockedLetterBatches);

        when(mockedLetterBatchDAO.findNumberOfLetterBatchesBySearchArgument(
            any(LetterReportQueryDTO.class), any(Long.class))).thenReturn(1l);
        
        OrganisaatioRDTO organisaatio = DocumentProviderTestData.getOrganisaatioRDTO();
        when(mockedOrganizationComponent.getOrganization(any(String.class))).thenReturn(organisaatio);
        lenient().when(mockedOrganizationComponent.getNameOfOrganisation(any(OrganisaatioRDTO.class))).thenReturn("oppilaitos");

        LetterReportQueryDTO query = new LetterReportQueryDTO();
        query.setOrganizationOids(Arrays.asList("1.2.246.562.10.00000000001"));
        query.setLetterBatchSearchArgument("hakutekija");
        PagingAndSortingDTO pagingAndSorting = DocumentProviderTestData.getPagingAndSortingDTO();
     
        LetterBatchesReportDTO letterBatchesReport = letterReportService.getLetterBatchesReport(query, pagingAndSorting);
        
        assertNotNull(letterBatchesReport);
        assertNotNull(letterBatchesReport.getLetterBatchReports());
        assertTrue(letterBatchesReport.getLetterBatchReports().size() > 0);
        assertTrue(letterBatchesReport.getLetterBatchReports().size() == 1);
        assertTrue(letterBatchesReport.getNumberOfLetterBatches().equals(new Long(1)));
        assertTrue(letterBatchesReport.getLetterBatchReports().get(0).getFetchTarget().equalsIgnoreCase("fetchTarget"));
    }

    @Test
    public void testGetLetterBatchesReportByOrganizationOID() {
        LetterBatch letterBatch = DocumentProviderTestData.getLetterBatch(1l);
        List<LetterBatch> mockedLetterBatches = new ArrayList<LetterBatch>();
        mockedLetterBatches.add(letterBatch);
        //noinspection unchecked
        when(mockedLetterBatchDAO.findLetterBatchesByOrganizationOid(
            any(List.class), any(PagingAndSortingDTO.class))).thenReturn(mockedLetterBatches);

        when(organisaatioService.findHierarchyOids(eq("1.2.246.562.10.00000000001")))
                .thenReturn(Arrays.asList("1.2.246.562.10.00000000001"));

        when(mockedLetterBatchDAO.findNumberOfLetterBatches(any(List.class))).thenReturn(1l);

        OrganisaatioRDTO organisaatio = DocumentProviderTestData.getOrganisaatioRDTO();
        when(mockedOrganizationComponent.getOrganization(any(String.class))).thenReturn(organisaatio);
        when(mockedOrganizationComponent.getNameOfOrganisation(any(OrganisaatioRDTO.class))).thenReturn("oppilaitos");

        PagingAndSortingDTO pagingAndSorting = DocumentProviderTestData.getPagingAndSortingDTO();
     
        LetterBatchesReportDTO letterBatchesReport = 
            letterReportService.getLetterBatchesReport("1.2.246.562.10.00000000001", pagingAndSorting);
        
        assertNotNull(letterBatchesReport);
        assertNotNull(letterBatchesReport.getLetterBatchReports());
        assertTrue(letterBatchesReport.getLetterBatchReports().size() > 0);
        assertTrue(letterBatchesReport.getLetterBatchReports().size() == 1);
        assertTrue(letterBatchesReport.getNumberOfLetterBatches().equals(new Long(1)));
        assertTrue(letterBatchesReport.getLetterBatchReports().get(0).getFetchTarget().equalsIgnoreCase("fetchTarget"));
    }
}
