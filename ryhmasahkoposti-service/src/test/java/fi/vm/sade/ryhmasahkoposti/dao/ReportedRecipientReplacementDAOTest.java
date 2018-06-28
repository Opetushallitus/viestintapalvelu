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
package fi.vm.sade.ryhmasahkoposti.dao;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import fi.vm.sade.externalinterface.common.ObjectMapperProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.sade.ryhmasahkoposti.api.dto.ReportedRecipientReplacementDTO;
import fi.vm.sade.ryhmasahkoposti.converter.ReportedRecipientReplacementConverter;
import fi.vm.sade.ryhmasahkoposti.model.ReportedMessage;
import fi.vm.sade.ryhmasahkoposti.model.ReportedRecipient;
import fi.vm.sade.ryhmasahkoposti.model.ReportedRecipientReplacement;
import fi.vm.sade.ryhmasahkoposti.testdata.RaportointipalveluTestData;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-dao-context.xml")
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
        TransactionalTestExecutionListener.class })
@Transactional(readOnly = true)
public class ReportedRecipientReplacementDAOTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Autowired
    private ReportedMessageDAO reportedMessageDAO;

    @Autowired
    private ReportedRecipientDAO reportedRecipientDAO;

    @Autowired
    private ReportedRecipientReplacementDAO reportedRecipientReplacementDAO;

    private ReportedRecipientReplacementConverter replacementConverter;

    @Before
    public void setup() {
        replacementConverter = new ReportedRecipientReplacementConverter();
        replacementConverter.setObjectMapperProvider(new ObjectMapperProvider());
    }

    @Test
    public void testReportedRecipientReplacementInsertWasSuccessful() {

        ReportedMessage reportedMessage = RaportointipalveluTestData.getReportedMessage();
        ReportedMessage savedReportedMessage = reportedMessageDAO.insert(reportedMessage);

        ReportedRecipient reportedRecipient = RaportointipalveluTestData.getReportedRecipient(savedReportedMessage);
        ReportedRecipient savedReportedRecipient = reportedRecipientDAO.insert(reportedRecipient);

        ReportedRecipientReplacement reportedRecipientReplacement = RaportointipalveluTestData.getReportedRecipientReplacement(savedReportedRecipient);

        ReportedRecipientReplacement savedReportedRecipientReplacement = reportedRecipientReplacementDAO.insertAndFlush(reportedRecipientReplacement);

        assertNotNull(savedReportedRecipientReplacement);
        assertNotNull(savedReportedRecipientReplacement.getId());
        assertNotNull(savedReportedRecipientReplacement.getVersion());
    }

    @Test
    public void testAllReportedRecipientReplacementFound() {

        ReportedMessage reportedMessage = RaportointipalveluTestData.getReportedMessage();
        ReportedMessage savedReportedMessage = reportedMessageDAO.insert(reportedMessage);

        ReportedRecipient reportedRecipient = RaportointipalveluTestData.getReportedRecipient(savedReportedMessage);
        ReportedRecipient savedReportedRecipient = reportedRecipientDAO.insert(reportedRecipient);

        ReportedRecipientReplacement reportedRecipientReplacement = RaportointipalveluTestData.getReportedRecipientReplacement(savedReportedRecipient);
        ReportedRecipientReplacement savedReportedRecipientReplacement = reportedRecipientReplacementDAO.insertAndFlush(reportedRecipientReplacement);

        List<ReportedRecipientReplacement> reportedRecipientReplacements = reportedRecipientReplacementDAO.findAll();

        assertNotNull(reportedRecipientReplacements);
        assertNotEquals(0, reportedRecipientReplacements.size());
        assertEquals(savedReportedRecipientReplacement, reportedRecipientReplacements.get(0));
    }

    @Test
    public void testReportedRecipientReplacementsFoundByID() {

        ReportedMessage reportedMessage = RaportointipalveluTestData.getReportedMessage();
        ReportedMessage savedReportedMessage = reportedMessageDAO.insert(reportedMessage);

        ReportedRecipient reportedRecipient = RaportointipalveluTestData.getReportedRecipient(savedReportedMessage);
        ReportedRecipient savedReportedRecipient = reportedRecipientDAO.insert(reportedRecipient);

        ReportedRecipientReplacement reportedRecipientReplacement = RaportointipalveluTestData.getReportedRecipientReplacement(savedReportedRecipient);
        ReportedRecipientReplacement savedReportedRecipientReplacement = reportedRecipientReplacementDAO.insertAndFlush(reportedRecipientReplacement);

        Long id = savedReportedRecipientReplacement.getId();
        ReportedRecipientReplacement searchedReportedRecipientReplacement = reportedRecipientReplacementDAO.read(id);

        assertNotNull(searchedReportedRecipientReplacement);
        assertEquals(savedReportedRecipientReplacement.getId(), searchedReportedRecipientReplacement.getId());
        assertEquals(savedReportedRecipientReplacement, searchedReportedRecipientReplacement);
    }

    @Test
    public void testReportedRecipientReplacementFoundByRecipientId() {

        ReportedMessage reportedMessage = RaportointipalveluTestData.getReportedMessage();
        ReportedMessage savedReportedMessage = reportedMessageDAO.insert(reportedMessage);

        ReportedRecipient reportedRecipient = RaportointipalveluTestData.getReportedRecipient(savedReportedMessage);
        ReportedRecipient savedReportedRecipient = reportedRecipientDAO.insert(reportedRecipient);

        ReportedRecipientReplacement reportedRecipientReplacement = RaportointipalveluTestData.getReportedRecipientReplacement(savedReportedRecipient);
        ReportedRecipientReplacement savedReportedRecipientReplacement = reportedRecipientReplacementDAO.insertAndFlush(reportedRecipientReplacement);

        List<ReportedRecipientReplacement> reportedRecipientReplacements = reportedRecipientReplacementDAO
                .findReportedRecipientReplacements(savedReportedRecipient);

        assertNotNull(reportedRecipientReplacements);
        assertNotEquals(0, reportedRecipientReplacements.size());
        assertEquals(savedReportedRecipientReplacement, reportedRecipientReplacements.get(0));
    }

    @Test
    public void testReportedMessageUpdateIsSuccessful() {

        ReportedMessage reportedMessage = RaportointipalveluTestData.getReportedMessage();
        ReportedMessage savedReportedMessage = reportedMessageDAO.insert(reportedMessage);

        ReportedRecipient reportedRecipient = RaportointipalveluTestData.getReportedRecipient(savedReportedMessage);
        ReportedRecipient savedReportedRecipient = reportedRecipientDAO.insert(reportedRecipient);

        ReportedRecipientReplacement reportedRecipientReplacement = RaportointipalveluTestData.getReportedRecipientReplacement(savedReportedRecipient);
        ReportedRecipientReplacement savedReportedRecipientReplacement = reportedRecipientReplacementDAO.insertAndFlush(reportedRecipientReplacement);

        assertEquals(new Long(0), savedReportedRecipientReplacement.getVersion());

        savedReportedRecipientReplacement.setName("new-name");
        reportedRecipientReplacementDAO.update(savedReportedRecipientReplacement);

        assertEquals(new Long(1), savedReportedRecipientReplacement.getVersion());
        assertEquals("new-name", savedReportedRecipientReplacement.getName());
    }

    @Test
    public void testReportedMessageDeletedIsSuccessful() {

        ReportedMessage reportedMessage = RaportointipalveluTestData.getReportedMessage();
        ReportedMessage savedReportedMessage = reportedMessageDAO.insert(reportedMessage);

        ReportedRecipient reportedRecipient = RaportointipalveluTestData.getReportedRecipient(savedReportedMessage);
        ReportedRecipient savedReportedRecipient = reportedRecipientDAO.insert(reportedRecipient);

        ReportedRecipientReplacement reportedRecipientReplacement = RaportointipalveluTestData.getReportedRecipientReplacement(savedReportedRecipient);
        ReportedRecipientReplacement savedReportedRecipientReplacement = reportedRecipientReplacementDAO.insertAndFlush(reportedRecipientReplacement);

        long id = savedReportedRecipientReplacement.getId();
        reportedRecipientReplacementDAO.remove(savedReportedRecipientReplacement);

        ReportedRecipientReplacement notFoundReportedRecipientReplacement = reportedRecipientReplacementDAO.read(id);
        assertNull(notFoundReportedRecipientReplacement);
    }

    @Test
    public void testReportedMessageNotFoundByID() {
        Long id = new Long(2013121452);
        ReportedRecipientReplacement reportedRecipientReplacement = reportedRecipientReplacementDAO.read(id);

        assertNull(reportedRecipientReplacement);
    }

    @Test
    public void testJsonListValue() throws IOException {
        ReportedMessage reportedMessage = RaportointipalveluTestData.getReportedMessage();
        ReportedMessage savedReportedMessage = reportedMessageDAO.insert(reportedMessage);
        ReportedRecipient recipient = RaportointipalveluTestData.getReportedRecipient(savedReportedMessage);
        reportedRecipientDAO.insertAndFlush(recipient);

        ReportedRecipientReplacementDTO replacementDTO = new ReportedRecipientReplacementDTO();
        replacementDTO.setName("testi");
        replacementDTO.setValue(Arrays.asList("a", "b", "c"));
        List<ReportedRecipientReplacement> replacements = replacementConverter.convert(recipient, Arrays.asList(replacementDTO));
        assertEquals(1, replacements.size());
        ReportedRecipientReplacement replacement = replacements.get(0);
        reportedRecipientReplacementDAO.insertAndFlush(replacement);

        assertNull(replacement.getValue());
        assertNotNull(replacement.getJsonValue());
        assertEquals("[\"a\",\"b\",\"c\"]", replacement.getJsonValue());

        List<ReportedRecipientReplacementDTO> dtos = replacementConverter.convertDTO(replacements);
        assertEquals(1, dtos.size());
        ReportedRecipientReplacementDTO dto = dtos.get(0);
        assertNull(dto.getDefaultValue());
        assertNotNull(dto.getValue());
        assertNotNull(dto.getEffectiveValue());
        assertTrue(dto.getEffectiveValue() instanceof List);
        List<?> list = (List<?>) dto.getEffectiveValue();
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    public void testJsonStringValue() throws IOException {
        ReportedMessage reportedMessage = RaportointipalveluTestData.getReportedMessage();
        ReportedMessage savedReportedMessage = reportedMessageDAO.insert(reportedMessage);
        ReportedRecipient recipient = RaportointipalveluTestData.getReportedRecipient(savedReportedMessage);
        reportedRecipientDAO.insertAndFlush(recipient);

        ReportedRecipientReplacementDTO replacementDTO = new ReportedRecipientReplacementDTO();
        replacementDTO.setName("testi");

        replacementDTO.setDefaultValue("foo");
        assertEquals("foo", replacementDTO.getEffectiveValue());
        replacementDTO.setValue("TEST \"with\" some \\ data");
        assertEquals("TEST \"with\" some \\ data", replacementDTO.getEffectiveValue());
        assertNull(replacementDTO.getDefaultValue());

        List<ReportedRecipientReplacement> replacements = replacementConverter.convert(recipient, Arrays.asList(replacementDTO));
        assertEquals(1, replacements.size());
        ReportedRecipientReplacement replacement = replacements.get(0);
        reportedRecipientReplacementDAO.insertAndFlush(replacement);
        assertNull(replacement.getValue());
        assertNotNull(replacement.getJsonValue());
        assertEquals("\"TEST \\\"with\\\" some \\\\ data\"", replacement.getJsonValue());

        List<ReportedRecipientReplacementDTO> dtos = replacementConverter.convertDTO(replacements);
        assertEquals(1, dtos.size());
        ReportedRecipientReplacementDTO dto = dtos.get(0);
        assertNull(dto.getDefaultValue());
        assertNotNull(dto.getValue());
        assertNotNull(dto.getEffectiveValue());
        assertTrue(dto.getEffectiveValue() instanceof String);
        assertEquals("TEST \"with\" some \\ data", dto.getEffectiveValue());
    }

    @Test
    public void testDefaultValueDtoConversion() throws IOException {
        ReportedMessage reportedMessage = RaportointipalveluTestData.getReportedMessage();
        ReportedMessage savedReportedMessage = reportedMessageDAO.insert(reportedMessage);
        ReportedRecipient recipient = RaportointipalveluTestData.getReportedRecipient(savedReportedMessage);
        reportedRecipientDAO.insertAndFlush(recipient);

        ReportedRecipientReplacementDTO replacementDTO = new ReportedRecipientReplacementDTO();
        replacementDTO.setName("test");
        replacementDTO.setDefaultValue("plain old string");

        List<ReportedRecipientReplacement> replacements = replacementConverter.convert(recipient, Arrays.asList(replacementDTO));
        assertEquals(1, replacements.size());
        ReportedRecipientReplacement replacement = replacements.get(0);
        reportedRecipientReplacementDAO.insertAndFlush(replacement);

        assertNull(replacement.getJsonValue());
        assertNotNull(replacement.getValue());

        List<ReportedRecipientReplacementDTO> dtos = replacementConverter.convertDTO(replacements);
        assertEquals(1, dtos.size());
        ReportedRecipientReplacementDTO dto = dtos.get(0);
        assertNull(dto.getValue());
        assertNotNull(dto.getDefaultValue());
        assertNotNull(dto.getEffectiveValue());
        assertEquals("plain old string", dto.getEffectiveValue());
    }
}
