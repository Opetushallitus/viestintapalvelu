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
package fi.vm.sade.ryhmasahkoposti.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.test.context.ContextConfiguration;

import fi.vm.sade.ryhmasahkoposti.api.dto.query.ReportedMessageQueryDTO;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ReportedMessageQueryDTOConverterTest.class)
@ContextConfiguration("/test-bundle-context.xml")
public class ReportedMessageQueryDTOConverterTest {
    private ReportedMessageQueryDTOConverter reportedMessageQueryDTOConverter;

    @Before
    public void setup() {
        this.reportedMessageQueryDTOConverter = new ReportedMessageQueryDTOConverter();
    }

    @Test
    public void testEmailInSearchArgument() {
        String oid = "1.2.246.562.10.00000000001";
        String searchArgument = "testi.osoite@sposti.fi";

        ReportedMessageQueryDTO query = reportedMessageQueryDTOConverter.convert(oid, searchArgument);

        assertNotNull(query);
        assertNotNull(query.getReportedRecipientQueryDTO());
        assertEquals(searchArgument, query.getReportedRecipientQueryDTO().getRecipientEmail());
    }

    @Test
    public void testOidInSearchArgument() {
        String oid = "1.2.246.562.10.00000000001";
        String searchArgument = "1.2.246.562.24.42645159413";

        ReportedMessageQueryDTO query = reportedMessageQueryDTOConverter.convert(oid, searchArgument);

        assertNotNull(query);
        assertNotNull(query.getReportedRecipientQueryDTO());
        assertEquals(searchArgument, query.getReportedRecipientQueryDTO().getRecipientOid());
    }
}
