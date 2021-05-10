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

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import fi.vm.sade.viestintapalvelu.dao.DraftDAO;
import fi.vm.sade.viestintapalvelu.dao.StructureDAO;
import fi.vm.sade.viestintapalvelu.dao.TemplateDAO;
import fi.vm.sade.viestintapalvelu.dao.criteria.TemplateCriteria;
import fi.vm.sade.viestintapalvelu.dao.criteria.TemplateCriteriaImpl;
import fi.vm.sade.viestintapalvelu.externalinterface.component.CurrentUserComponent;
import fi.vm.sade.viestintapalvelu.model.Structure;
import fi.vm.sade.viestintapalvelu.model.Template;
import fi.vm.sade.viestintapalvelu.model.Template.State;
import fi.vm.sade.viestintapalvelu.model.TemplateApplicationPeriod;
import fi.vm.sade.viestintapalvelu.model.types.ContentRole;
import fi.vm.sade.viestintapalvelu.model.types.ContentStructureType;
import fi.vm.sade.viestintapalvelu.model.types.ContentType;
import fi.vm.sade.viestintapalvelu.structure.dto.StructureSaveDto;
import fi.vm.sade.viestintapalvelu.structure.dto.converter.StructureDtoConverter;
import fi.vm.sade.viestintapalvelu.structure.impl.StructureServiceImpl;
import fi.vm.sade.viestintapalvelu.template.StructureConverter;
import fi.vm.sade.viestintapalvelu.template.TemplateService;
import fi.vm.sade.viestintapalvelu.template.TemplatesByApplicationPeriod;
import fi.vm.sade.viestintapalvelu.template.TemplateInfo;
import fi.vm.sade.viestintapalvelu.template.TemplatesByApplicationPeriodConverter;
import fi.vm.sade.viestintapalvelu.template.impl.StructureConverterImpl;
import fi.vm.sade.viestintapalvelu.template.impl.TemplateServiceImpl;
import fi.vm.sade.viestintapalvelu.template.impl.TemplatesByApplicationPeriodConverterImpl;
import fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData;
import fi.vm.sade.viestintapalvelu.util.DaoVault;
import static fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData.content;
import static fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData.contentSaveDto;
import static fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData.contentStructure;
import static fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData.contentStructureSaveDto;
import static fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData.replacement;
import static fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData.structure;
import static fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData.structureSaveDto;
import static fi.vm.sade.viestintapalvelu.testdata.DocumentProviderTestData.with;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TemplateServiceTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private TemplateDAO mockedTemplateDAO;
    @Mock
    private CurrentUserComponent mockedCurrentUserComponent;
    @Mock
    private DraftDAO mockedDraftDAO;
    @Mock
    private StructureDAO structureDAO;
    @Mock
    private StructureServiceImpl structureService;
    private TemplateService templateService;
    private StructureConverter structureConverter = new StructureConverterImpl();
    private TemplatesByApplicationPeriodConverter applicationPeriodConverter = new TemplatesByApplicationPeriodConverterImpl();

    @Before
    public void setup() {
        this.templateService = new TemplateServiceImpl(mockedTemplateDAO, mockedCurrentUserComponent, mockedDraftDAO,
                structureDAO, structureService, structureConverter, applicationPeriodConverter);
    }

    @Test
    public void testStoreTemplateDTO() {
        when(mockedCurrentUserComponent.getCurrentUser()).thenReturn(DocumentProviderTestData.getHenkilo().getOidHenkilo());

        mockStructureCreation();
        mockTemplateDao();
        assertEquals(1l, templateService.storeTemplateDTO(DocumentProviderTestData.getTemplate()));
        
        verify(mockedTemplateDAO).insert(DocumentProviderTestData.getTemplate(1l));
    }

    private DaoVault<Structure> mockStructureCreation() {
        doCallRealMethod().when(structureService).setStructureDAO(any(StructureDAO.class));
        doCallRealMethod().when(structureService).setDtoConverter(any(StructureDtoConverter.class));
        structureService.setStructureDAO(structureDAO);
        structureService.setDtoConverter(new StructureDtoConverter());
        doCallRealMethod().when(structureService).storeStructure(any(StructureSaveDto.class));
        return new DaoVault<Structure>(structureDAO, Structure.class);
    }

    private DaoVault<Template> mockTemplateDao() {
        return new DaoVault<Template>(mockedTemplateDAO, Template.class);
    }

    @Test
    public void testFindById() {
        Template template = DocumentProviderTestData.getTemplate(1l);
        when(mockedTemplateDAO.findByIdAndState(template.getId(), State.julkaistu)).thenReturn(template);
        fi.vm.sade.viestintapalvelu.template.Template templateFindByID = templateService.findById(1, ContentStructureType.letter);
        assertNotNull(templateFindByID);
        assertEquals(template.getId().longValue(), templateFindByID.getId());
        assertNotNull(templateFindByID.getContents().size() == 1);
        assertNotNull(templateFindByID.getReplacements().size() == 1);
    }

    @Test
    public void testGetTemplateByCriteriaWithApplicationPeriod() {
        Template mockedTemplate = DocumentProviderTestData.getTemplate(1l);
        String applicationPeriodOid = "1234.5678910.12345";
        DocumentProviderTestData.getTemplateHaku(mockedTemplate, applicationPeriodOid);
        when(mockedTemplateDAO.findTemplate(any(TemplateCriteria.class))).thenReturn(mockedTemplate);

        fi.vm.sade.viestintapalvelu.template.Template templateFindByName =
                templateService.getTemplateByName(new TemplateCriteriaImpl()
                        .withName("test_template")
                        .withLanguage("FI")
                        .withType(ContentStructureType.letter)
                        .withApplicationPeriod(applicationPeriodOid), true);

        assertNotNull(templateFindByName);
        assertTrue(templateFindByName.getId() == 1);
        assertNotNull(templateFindByName.getContents().size() == 1);
        assertNotNull(templateFindByName.getReplacements().size() == 1);
        assertTrue(templateFindByName.getType() != null);
        assertTrue(templateFindByName.getType().equals("letter"));
        assertEquals(1, templateFindByName.getApplicationPeriods().size());
    }


    @Test
    public void testGetTemplateByCriteriaWithApplicationPeriodWithFallback() {
        Template mockedTemplate = DocumentProviderTestData.getTemplate(1l),
                defaultTemplate = DocumentProviderTestData.getTemplate(2l),
                lastTemplate = DocumentProviderTestData.getTemplate(3l);
        String applicationPeriodOid = "1234.5678910.12345";
        DocumentProviderTestData.getTemplateHaku(mockedTemplate, applicationPeriodOid + "OTHER");
        defaultTemplate.setUsedAsDefault(true);
        String name = "test_template",
                lang = "FI";
        ContentStructureType type = ContentStructureType.letter;

        // The default should not be found:
        when(mockedTemplateDAO.findTemplate(eq(
                new TemplateCriteriaImpl()
                        .withName(name)
                        .withLanguage(lang)
                        .withType(type)
                        .withApplicationPeriod(applicationPeriodOid)
        ))).thenReturn(null);

        // The default should be found with default requirement:
        when(mockedTemplateDAO.findTemplate(eq(
                new TemplateCriteriaImpl()
                    .withName(name)
                    .withLanguage(lang)
                    .withType(type)
                    .withDefaultRequired()
        ))).thenReturn(defaultTemplate);

        // Without the default requirement, the last should be found:
        when(mockedTemplateDAO.findTemplate(eq(
                new TemplateCriteriaImpl()
                        .withName(name)
                        .withLanguage(lang)
                        .withType(type)
                        .withoutDefaultRequired()
        ))).thenReturn(lastTemplate);

        fi.vm.sade.viestintapalvelu.template.Template templateFindByName =
                templateService.getTemplateByName(new TemplateCriteriaImpl()
                        .withName(name)
                        .withLanguage(lang)
                        .withType(type)
                        .withApplicationPeriod(applicationPeriodOid), true);

        assertNotNull(templateFindByName);
        assertTrue(templateFindByName.getId() == 2);
        assertNotNull(templateFindByName.getContents().size() == 1);
        assertNotNull(templateFindByName.getReplacements().size() == 1);
        assertTrue(templateFindByName.getType() != null);
        assertTrue(templateFindByName.getType().equals("letter"));
    }


    @Test
    public void testGetTemplateByCriteriaWithoutApplicationPeriodWithFallback() {
        Template defaultTemplate = DocumentProviderTestData.getTemplate(2l),
                lastTemplate = DocumentProviderTestData.getTemplate(3l);
        defaultTemplate.setUsedAsDefault(true);
        String name = "test_template",
                lang = "FI";
        ContentStructureType type = ContentStructureType.letter;

        // The default should be found with default requirement:
        when(mockedTemplateDAO.findTemplate(eq(
                new TemplateCriteriaImpl(name, lang, type)
                        .withDefaultRequired()
        ))).thenReturn(defaultTemplate);

        // Without the default requirement, the last should be found:
        lenient().when(mockedTemplateDAO.findTemplate(eq(
                new TemplateCriteriaImpl(name, lang, type)
                        .withoutDefaultRequired()
        ))).thenReturn(lastTemplate);

        fi.vm.sade.viestintapalvelu.template.Template templateFindByName =
                templateService.getTemplateByName(new TemplateCriteriaImpl(name, lang, type), true);

        assertNotNull(templateFindByName);
        assertTrue(templateFindByName.getId() == 2);
        assertNotNull(templateFindByName.getContents().size() == 1);
        assertNotNull(templateFindByName.getReplacements().size() == 1);
        assertTrue(templateFindByName.getType() != null);
        assertTrue(templateFindByName.getType().equals("letter"));
    }

    @Test
    public void testGetTemplateByCriteriaWithoutApplicationPeriodWithoutFallback() {
        Template lastTemplate = DocumentProviderTestData.getTemplate(3l);
        String name = "test_template",
                lang = "FI";
        ContentStructureType type = ContentStructureType.letter;

        // The default should be found with default requirement:
        when(mockedTemplateDAO.findTemplate(eq(
                new TemplateCriteriaImpl(name, lang, type).withDefaultRequired()
        ))).thenReturn(null);

        // Without the default requirement, the last should be found:
        when(mockedTemplateDAO.findTemplate(eq(
                new TemplateCriteriaImpl(name, lang, type).withoutDefaultRequired()
        ))).thenReturn(lastTemplate);

        fi.vm.sade.viestintapalvelu.template.Template templateFindByName =
                templateService.getTemplateByName(new TemplateCriteriaImpl(name, lang, type), true);

        assertNotNull(templateFindByName);
        assertTrue(templateFindByName.getId() == 3);
        assertNotNull(templateFindByName.getContents().size() == 1);
        assertNotNull(templateFindByName.getReplacements().size() == 1);
        assertTrue(templateFindByName.getType() != null);
        assertTrue(templateFindByName.getType().equals("letter"));
    }
    
    @Test
    public void fetchesOnlyPublishedTemplatesByDefault() {
        templateService.getTemplateNamesList();
        verify(mockedTemplateDAO).getAvailableTemplatesByType(State.julkaistu);
    }
    
    @Test
    public void fetchesOnlyClosedTemplates() {
        templateService.getTemplateNamesListByState(State.suljettu);
        verify(mockedTemplateDAO).getAvailableTemplatesByType(State.suljettu);
    }
    
    @Test
    public void fetchesOnlyDrafts() {
        templateService.getTemplateNamesListByState(State.julkaistu);
        verify(mockedTemplateDAO).getAvailableTemplatesByType(State.julkaistu);
    }
    
    @Test
    public void closesPublishedTemplate() {
        verifyTemplateStateChange(State.julkaistu, State.suljettu);
    }
    
    @Test
    public void closesTemplateThatIsInDraftState() {
        verifyTemplateStateChange(State.luonnos, State.suljettu);
    }
    
    @Test
    public void publishesClosedTemplate() {
        verifyTemplateStateChange(State.suljettu, State.julkaistu);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void closedTemplateCannotBeUpdatedToAnythingBesidesPublished() {
        verifyTemplateStateChange(State.suljettu, State.luonnos);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void cannotUpdatePublishedTemplateToAnythingBesidesClosed() {
        verifyTemplateStateChange(State.julkaistu, State.luonnos);
    }
    
    @Test
    public void publishesTemplateThatIsInDraftState() {
        verifyTemplateStateChange(State.luonnos, State.julkaistu);
    }
    
    @Test
    public void findsOnlyPublishedTemplateByDefautl() {
        Long id = 1l;
        when(mockedTemplateDAO.findByIdAndState(id, State.julkaistu)).thenReturn(givenTemplateWithStateAndId(id, State.julkaistu));
        assertEquals(State.julkaistu, templateService.findById(id, ContentStructureType.letter).getState());
    }
    
    @Test
    public void findsOnlyClosedTemplate() {
        Long id = 3l;
        when(mockedTemplateDAO.findByIdAndState(id, State.suljettu)).thenReturn(givenTemplateWithStateAndId(id, State.suljettu));
        assertEquals(State.suljettu, templateService.findByIdAndState(id, ContentStructureType.letter, State.suljettu).getState());
    }
    
    @Test
    public void findsOnlyDraftTemplate() {
        Long id = 5l;
        when(mockedTemplateDAO.findByIdAndState(id, State.luonnos)).thenReturn(givenTemplateWithStateAndId(id, State.luonnos));
        assertEquals(State.luonnos, templateService.findByIdAndState(id, ContentStructureType.letter, State.luonnos).getState());
    }

    @Test
    public void testUpdateTemplateCanUpdateApplicationPeriods() {
        Template template = givenTemplateWithStateAndId(1l, State.luonnos);
        template.setVersion(1l);
        template.setStructure(structure(contentStructure(ContentStructureType.letter,
                content(ContentRole.body, ContentType.html))));
        template.getApplicationPeriods().add(new TemplateApplicationPeriod(template, "ap"));
        mockTemplateDao().insert(template);

        fi.vm.sade.viestintapalvelu.template.Template dto = withoutStructure(DocumentProviderTestData.getTemplate());
        dto.setId(1l);
        dto.setApplicationPeriods(Arrays.asList("ap", "other"));
        templateService.updateTemplate(dto);

        assertEquals(2, template.getApplicationPeriods().size());
        // Update called twice:
        assertEquals(Long.valueOf(3l), template.getVersion());
    }

    private fi.vm.sade.viestintapalvelu.template.Template withoutStructure(fi.vm.sade.viestintapalvelu.template.Template dto) {
        dto.setStructure(null);
        dto.setStructureName(null);
        dto.setStructureId(null);
        return dto;
    }

    @Test(expected = BadRequestException.class)
    public void testValidateStructureOnUpdate() {
        Template template = givenTemplateWithStateAndId(1l, State.luonnos);
        template.setStructure(with(structure(
                        contentStructure(ContentStructureType.letter, content(ContentRole.body, ContentType.html))),
                replacement("required")));
        template.getApplicationPeriods().add(new TemplateApplicationPeriod(template, "ap"));
        mockTemplateDao().insert(template);

        fi.vm.sade.viestintapalvelu.template.Template dto = withoutStructure(DocumentProviderTestData.getTemplate());
        dto.setId(1l);
        templateService.updateTemplate(dto);
    }

    @Test
    public void testChangeStructureById() {
        Template template = givenTemplateWithStateAndId(1l, State.luonnos);
        template.setStructure(with(structure(
                        contentStructure(ContentStructureType.letter, content(ContentRole.body, ContentType.html))),
                replacement("required")));
        template.getApplicationPeriods().add(new TemplateApplicationPeriod(template, "ap"));

        mockStructureCreation().insert(123l, structure(
                contentStructure(ContentStructureType.letter, content(ContentRole.body, ContentType.html))));
        mockTemplateDao().insert(template);

        fi.vm.sade.viestintapalvelu.template.Template dto = withoutStructure(DocumentProviderTestData.getTemplate());
        dto.setStructureId(123l);
        dto.setId(1l);
        templateService.updateTemplate(dto);

        assertEquals(Long.valueOf(123l), template.getStructure().getId());
    }

    @Test
    public void testChangeStructureByNew() {
        Template template = givenTemplateWithStateAndId(1l, State.luonnos);
        template.setVersion(1l);
        Structure original = with(structure(
                        contentStructure(ContentStructureType.letter, content(ContentRole.body, ContentType.html))),
                replacement("required"));
        template.setStructure(original);
        template.getApplicationPeriods().add(new TemplateApplicationPeriod(template, "ap"));

        mockStructureCreation().insert(20l, original);
        mockTemplateDao().insert(template);

        fi.vm.sade.viestintapalvelu.template.Template dto = withoutStructure(DocumentProviderTestData.getTemplate());
        dto.setId(1l);
        dto.setStructure(structureSaveDto(contentStructureSaveDto(ContentStructureType.letter,
                contentSaveDto("name", ContentRole.body, ContentType.html))));
        templateService.updateTemplate(dto);

        assertEquals(Long.valueOf(21l), template.getStructure().getId());
    }

    @Test
    public void updatesTemplateThatIsInDraftState() {
        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        fi.vm.sade.viestintapalvelu.template.Template template = DocumentProviderTestData.getTemplate();
        template.setId(5l);
        template.setUsedAsDefault(true);
        verifyTemplateStateChange(State.luonnos, State.luonnos, captor, template);
        assertTrue(captor.getValue().isUsedAsDefault());
    }
    
    @Test
    public void returnsLatestTemplatesByApplicationPeriod() {
        String hakuOid = "1.2.3.4.5600";
        Template latestPublishedTemplate = givenTemplateWithApplicationPeriodTimeAndState(hakuOid, new Date(), State.julkaistu);
        Template publishedTemplate = givenTemplateWithApplicationPeriodTimeAndState(hakuOid, new Date(0), State.julkaistu);
        Template latestDraftTemplate = givenTemplateWithApplicationPeriodTimeAndState(hakuOid, new Date(), State.luonnos);
        Template draftTemplate = givenTemplateWithApplicationPeriodTimeAndState(hakuOid, new Date(0), State.luonnos);
        when(mockedTemplateDAO.findTemplates(eq(new TemplateCriteriaImpl().withApplicationPeriod(hakuOid).withState(State.julkaistu)))).thenReturn(Arrays.asList(latestPublishedTemplate, publishedTemplate));
        when(mockedTemplateDAO.findTemplates(eq(new TemplateCriteriaImpl().withApplicationPeriod(hakuOid).withState(State.luonnos)))).thenReturn(Arrays.asList(latestDraftTemplate, draftTemplate));
        TemplatesByApplicationPeriod dto = templateService.findByApplicationPeriod(hakuOid);
        assertEquals(hakuOid, dto.hakuOid);
        assertEquals(1, dto.draftTemplates.size());
        assertEquals(1, dto.publishedTemplates.size());
        assertTemplateInfo(latestPublishedTemplate, dto.publishedTemplates.get(0));
        assertTemplateInfo(latestDraftTemplate, dto.draftTemplates.get(0));
    }
    

    @Test
    public void returnsTemplatesByApplicationPeriodByFilteringOutClosedTemplates() {
        String hakuOid = "1.2.3.4.57700";
        Template publishedTemplate = givenTemplateWithApplicationPeriodTimeAndState(hakuOid, new Date(), State.julkaistu);
        Template closedTemplate = givenTemplateWithApplicationPeriodTimeAndState(hakuOid, new Date(), State.suljettu);
        Template closedTemplateEn = givenTemplateWithApplicationPeriodTimeAndState(hakuOid, new Date(), State.suljettu);
        closedTemplateEn.setLanguage("EN");
        when(mockedTemplateDAO.findTemplates(eq(new TemplateCriteriaImpl().withApplicationPeriod(hakuOid).withState(State.julkaistu)))).thenReturn(Arrays.asList(publishedTemplate));
        when(mockedTemplateDAO.findTemplates(eq(new TemplateCriteriaImpl().withApplicationPeriod(hakuOid).withState(State.suljettu)))).thenReturn(Arrays.asList(closedTemplate, closedTemplateEn));
        TemplatesByApplicationPeriod dto = templateService.findByApplicationPeriod(hakuOid);
        assertEquals(hakuOid, dto.hakuOid);
        assertTrue(dto.draftTemplates.isEmpty());
        assertEquals(1, dto.publishedTemplates.size());
        assertEquals(1, dto.closedTemplates.size());
        assertTemplateInfo(publishedTemplate, dto.publishedTemplates.get(0));
        assertTemplateInfo(closedTemplateEn, dto.closedTemplates.get(0));
    }
    
    private void assertTemplateInfo(Template expectedValues, TemplateInfo info) {
        assertEquals(expectedValues.getState(), info.state);
        assertEquals(expectedValues.getTimestamp(), info.timestamp);
        assertEquals(expectedValues.getLanguage(), info.language);
    }
    
    private Template givenTemplateWithApplicationPeriodTimeAndState(String hakuOid, Date date, State state) {
        Template template = DocumentProviderTestData.getTemplate(1l);
        template.addApplicationPeriod(DocumentProviderTestData.getTemplateApplicationPeriod(template, hakuOid));
        template.setTimestamp(date);
        template.setState(state);
        return template;
    }
    
    private void verifyTemplateStateChange(State oldState, State newState) {
        ArgumentCaptor<Template> captor = ArgumentCaptor.forClass(Template.class);
        fi.vm.sade.viestintapalvelu.template.Template template = DocumentProviderTestData.getTemplate();
        template.setId(7l);
        verifyTemplateStateChange(oldState, newState, captor, template);
    }

    private void verifyTemplateStateChange(State oldState, State newState, ArgumentCaptor<Template> captor, fi.vm.sade.viestintapalvelu.template.Template template) {
        template.setState(newState);
        when(mockedTemplateDAO.read(template.getId())).thenReturn(givenTemplateWithStateAndId(template.getId(), oldState));
        when(structureDAO.read(any(Long.TYPE))).thenReturn(structure(contentStructure(ContentStructureType.letter,
                content(ContentRole.body, ContentType.html))));
        templateService.updateTemplate(template);

        verify(mockedTemplateDAO).update(captor.capture());
        assertEquals(newState, captor.getValue().getState());
    }
    
    private Template givenTemplateWithStateAndId(Long id, State state) {
        Template template = DocumentProviderTestData.getTemplate(id);
        template.setState(state);
        return template;
    }

}
