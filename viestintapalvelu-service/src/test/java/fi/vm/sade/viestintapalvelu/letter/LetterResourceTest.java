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
package fi.vm.sade.viestintapalvelu.letter;

import java.lang.reflect.Field;

import javax.ws.rs.core.Response.Status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;

import fi.vm.sade.viestintapalvelu.letter.dto.AsyncLetterBatchDto;
import fi.vm.sade.viestintapalvelu.letter.dto.LetterBatchDetails;
import fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ContextConfiguration(locations = "/test-application-context.xml")
@RunWith(MockitoJUnitRunner.class)
public class LetterResourceTest {
    
    private static final Long LETTERBATCH_ID = 1l;

    
    @Mock
    private LetterService service;
    
    @Mock
    private LetterBuilder builder;
    
    @Mock
    private LetterBatchProcessor processor;
    
    private LetterResource resource;
    
    @Before
    public void init() throws Exception {
        resource = new LetterResource();
        injectObject("letterService", service);
        injectObject("letterBuilder", builder);
        injectObject("letterPDFProcessor", processor);
        when(service.createLetter(any(AsyncLetterBatchDto.class), any(Boolean.class)))
                .thenReturn(DocumentProviderTestData.getLetterBatch(LETTERBATCH_ID));
    }
    
    @Test
    public void usesLetterService() {
        resource.asyncLetter(DocumentProviderTestData.getAsyncLetterBatch());
        verify(service, times(1)).createLetter(any(AsyncLetterBatchDto.class), any(Boolean.class));
    }
    
    @Test
    public void initializesTemplateId() {
        resource.asyncLetter(DocumentProviderTestData.getAsyncLetterBatch());
        verify(builder, times(1)).initTemplateId(any(AsyncLetterBatchDto.class));
    }
    
    @Test
    public void returnsBadRequestForInvalidLetterBatch() {
        assertEquals(Status.BAD_REQUEST.getStatusCode(), resource.asyncLetter(null).getStatus());
    }
    
    @Test
    public void startsProcessingLetters() {
        resource.asyncLetter(DocumentProviderTestData.getAsyncLetterBatch()).getEntity();
        verify(processor).processLetterBatch(any(Long.class));
    }
    
    @Test
    public void returnsServerErrorWhenExceptionIsThrownDuringAsyncLetter() {
        when(service.createLetter(any(AsyncLetterBatchDto.class), any(Boolean.class))).thenThrow(new NullPointerException());
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), resource.asyncLetter(DocumentProviderTestData.getAsyncLetterBatch()).getStatus());
    }

    private void injectObject(String field, Object object) throws NoSuchFieldException, IllegalAccessException {
        Field letterService = resource.getClass().getSuperclass().getDeclaredField(field);
        letterService.setAccessible(true);
        letterService.set(resource, object);
    }
}
