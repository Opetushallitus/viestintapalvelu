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
package fi.vm.sade.viestintapalvelu.letter.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import fi.vm.sade.externalinterface.common.ObjectMapperProvider;
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.viestintapalvelu.LetterZipUtil;
import fi.vm.sade.viestintapalvelu.common.util.CollectionHelper;
import fi.vm.sade.viestintapalvelu.dao.IPostiDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterBatchDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterReceiverLetterDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterReceiversDAO;
import fi.vm.sade.viestintapalvelu.dao.TemplateDAO;
import fi.vm.sade.viestintapalvelu.dao.criteria.TemplateCriteriaImpl;
import fi.vm.sade.viestintapalvelu.dao.dto.LetterBatchCountDto;
import fi.vm.sade.viestintapalvelu.dao.dto.LetterBatchStatusDto;
import fi.vm.sade.viestintapalvelu.dao.dto.LetterBatchStatusErrorDto;
import fi.vm.sade.viestintapalvelu.document.DocumentBuilder;
import fi.vm.sade.viestintapalvelu.dto.letter.LetterReceiverLetterDTO;
import fi.vm.sade.viestintapalvelu.externalinterface.component.CurrentUserComponent;
import fi.vm.sade.viestintapalvelu.letter.DokumenttiIdProvider;
import fi.vm.sade.viestintapalvelu.letter.LetterBatchStatusLegalityChecker;
import fi.vm.sade.viestintapalvelu.letter.LetterBuilder;
import fi.vm.sade.viestintapalvelu.letter.LetterListResponse;
import fi.vm.sade.viestintapalvelu.letter.LetterPublisher;
import fi.vm.sade.viestintapalvelu.letter.LetterService;
import fi.vm.sade.viestintapalvelu.letter.dto.AsyncLetterBatchDto;
import fi.vm.sade.viestintapalvelu.letter.dto.AsyncLetterBatchLetterDto;
import fi.vm.sade.viestintapalvelu.letter.dto.LetterBatchDetails;
import fi.vm.sade.viestintapalvelu.letter.dto.LetterBatchSplitedIpostDto;
import fi.vm.sade.viestintapalvelu.letter.dto.LetterDetails;
import fi.vm.sade.viestintapalvelu.letter.dto.converter.LetterBatchDtoConverter;
import fi.vm.sade.viestintapalvelu.letter.processing.IPostiProcessable;
import fi.vm.sade.viestintapalvelu.model.IPosti;
import fi.vm.sade.viestintapalvelu.model.LetterBatch;
import fi.vm.sade.viestintapalvelu.model.LetterBatchGeneralProcessingError;
import fi.vm.sade.viestintapalvelu.model.LetterBatchIPostProcessingError;
import fi.vm.sade.viestintapalvelu.model.LetterBatchLetterProcessingError;
import fi.vm.sade.viestintapalvelu.model.LetterBatchProcessingError;
import fi.vm.sade.viestintapalvelu.model.LetterReceiverLetter;
import fi.vm.sade.viestintapalvelu.model.LetterReceiverReplacement;
import fi.vm.sade.viestintapalvelu.model.LetterReceivers;
import fi.vm.sade.viestintapalvelu.model.LetterReplacement;
import fi.vm.sade.viestintapalvelu.model.Template.State;
import fi.vm.sade.viestintapalvelu.model.UsedTemplate;
import fi.vm.sade.viestintapalvelu.model.types.ContentStructureType;
import fi.vm.sade.viestintapalvelu.model.types.ContentTypes;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.ws.rs.NotFoundException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

import static org.joda.time.DateTime.now;

/**
 * @author migar1
 *
 */
@Service
@Transactional
@ComponentScan(value = { "fi.vm.sade.externalinterface" })
public class LetterServiceImpl implements LetterService {
    public static final String DOCUMENT_TYPE_APPLICATION_ZIP = "application/zip";
    private static final String DOCUMENT_TYPE_HTML = "text/html";

    private static final Map<ContentStructureType, String> CONTENT_STRUCTURE_TYPE_STRING_MAPPING = new HashMap<>();
    static {
        CONTENT_STRUCTURE_TYPE_STRING_MAPPING.put(ContentStructureType.letter, ContentTypes.CONTENT_TYPE_PDF);
        CONTENT_STRUCTURE_TYPE_STRING_MAPPING.put(ContentStructureType.accessibleHtml, DOCUMENT_TYPE_HTML);
    }

    private static final int STORE_DOKUMENTTIS_DAYS = 2;
    private static final int MAX_IPOST_CHUNK_SIZE = 500;
    private static final Logger logger = LoggerFactory.getLogger(LetterServiceImpl.class);
    private LetterBatchDAO letterBatchDAO;
    private LetterReceiverLetterDAO letterReceiverLetterDAO;
    private LetterReceiversDAO letterReceiversDAO;
    private CurrentUserComponent currentUserComponent;
    private TemplateDAO templateDAO;
    private LetterBatchDtoConverter letterBatchDtoConverter;
    private IPostiDAO iPostiDAO;
    private ObjectMapperProvider objectMapperProvider;
    // Causes circular reference if autowired directly, through
    // applicationContext laxily:
    private LetterBuilder letterBuilder;
    private LetterBatchStatusLegalityChecker letterBatchStatusLegalityChecker;
    private DocumentBuilder documentBuilder;
    private DokumenttiIdProvider dokumenttiIdProvider;
    private final LetterPublisher letterPublisher;

    @Autowired
    private Dokumenttipalvelu dokumenttipalvelu;
    @Autowired
    private ApplicationContext applicationContext;

    @Value("${viestintapalvelu.letter.publish.dir}")
    private File letterPublishDir;


    @Autowired
    public LetterServiceImpl(LetterBatchDAO letterBatchDAO, LetterReceiverLetterDAO letterReceiverLetterDAO, CurrentUserComponent currentUserComponent,
                             TemplateDAO templateDAO, LetterBatchDtoConverter letterBatchDtoConverter, LetterReceiversDAO letterReceiversDAO,
                             ObjectMapperProvider objectMapperProvider, IPostiDAO iPostiDAO, LetterBatchStatusLegalityChecker letterBatchStatusLegalityChecker,
                             DocumentBuilder documentBuilder, DokumenttiIdProvider dokumenttiIdProvider, LetterPublisher letterPublisher) {
        this.letterBatchDAO = letterBatchDAO;
        this.currentUserComponent = currentUserComponent;
        this.templateDAO = templateDAO;
        this.letterReceiversDAO = letterReceiversDAO;
        this.letterReceiverLetterDAO = letterReceiverLetterDAO;
        this.letterBatchDtoConverter = letterBatchDtoConverter;
        this.objectMapperProvider = objectMapperProvider;
        this.iPostiDAO = iPostiDAO;
        this.letterBatchStatusLegalityChecker = letterBatchStatusLegalityChecker;
        this.documentBuilder = documentBuilder;
        this.dokumenttiIdProvider = dokumenttiIdProvider;
        this.applicationContext = applicationContext;
        this.letterPublisher = letterPublisher;
    }

    @Override
    public int publishLetterBatch(long letterBatchId) throws Exception {
        return letterPublisher.publishLetterBatch(letterBatchId);
    }

    /* ---------------------- */
    /* - Create LetterBatch - */
    /* ---------------------- */
    @Override
    @Transactional
    public LetterBatch createLetter(AsyncLetterBatchDto letterBatch, boolean anonymous) {
        // kirjeet.kirjelahetys
        ObjectMapper mapper = objectMapperProvider.getContext(getClass());
        LetterBatch model = new LetterBatch();
        try {

            letterBatchDtoConverter.convert(letterBatch, model, mapper);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON parsing of letter receiver replacement failed: " + e.getMessage(), e);
        }

        model.setTimestamp(new Date());
        if (!anonymous) {
            model.setStoringOid(getCurrentHenkilo());
        }
        model.setBatchStatus(LetterBatch.Status.created);

        // kirjeet.vastaanottaja
        try {
            model.setLetterReceivers(parseLetterReceiversModels(letterBatch, model, mapper));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("JSON parsing of letter receiver replacement failed: " + e.getMessage(), e);
        }
        model.setUsedTemplates(parseUsedTemplates(letterBatch, model, letterBatch.getContentStructureTypes()));

        if (letterBatch.isIposti()) {
            addIpostData(letterBatch, model);
        } else {
            logger.info("Jätetään IPost-luonti väliin kirjeiden muodostuksessa haulle " + letterBatch.getApplicationPeriod());
        }
        return storeLetterBatch(model);
    }

    private String getCurrentHenkilo() {
        logger.debug("getting current user!!! ");
        String henkiloOid = currentUserComponent.getCurrentUser();
        logger.debug("getting current user!!!  got " + henkiloOid);
        return henkiloOid;
    }

    private void addIpostData(LetterBatchDetails letterBatch, LetterBatch model) {
        Map<String, byte[]> ipostiData = letterBatch.getIPostiData();
        if (ipostiData != null) {
            for (Map.Entry<String, byte[]> data : ipostiData.entrySet()) {
                model.addIPosti(createIPosti(model, data));
            }
        }
    }

    private Set<UsedTemplate> parseUsedTemplates(
            LetterBatchDetails letterBatch,
            LetterBatch letterB,
            final List<ContentStructureType> contentStructureTypes) {
        final Set<String> languageCodes = Stream.concat(
                letterBatch
                        .getLetters()
                        .stream()
                        .map(LetterDetails::getLanguageCode),
                Stream.of(letterBatch.getLanguageCode())
        )
                .collect(Collectors.toSet());
        return contentStructureTypes
                .stream()
                .flatMap(contentStructureType -> {
                    final String templateName = getTemplateNameFromBatch(letterBatch, contentStructureType);
                    return languageCodes
                            .stream()
                            .map(languageCode -> new TemplateCriteriaImpl(
                                    templateName,
                                    languageCode,
                                    contentStructureType
                            ))
                            .map(templateCriteria -> templateDAO.findTemplate(templateCriteria))
                            .filter(Objects::nonNull)
                            .map(template -> {
                                final UsedTemplate usedTemplate = new UsedTemplate();
                                usedTemplate.setLetterBatch(letterB);
                                usedTemplate.setTemplate(template);
                                return usedTemplate;
                            });
                })
                .collect(Collectors.toSet());
    }

    private String getTemplateNameFromBatch(LetterBatchDetails letterBatch, final ContentStructureType contentStructureType) {
        return java.util.Optional.of(
                templateDAO.findTemplate(
                        new TemplateCriteriaImpl(
                                letterBatch.getTemplateName(),
                                letterBatch.getLanguageCode(),
                                contentStructureType
                        )
                )
        )
                .orElseGet(() -> templateDAO.findByIdAndState(letterBatch.getTemplateId(), State.julkaistu))
                .getName();
    }

    /* ------------ */
    /* - findById - */
    /* ------------ */
    @Transactional(readOnly = true)
    public fi.vm.sade.viestintapalvelu.letter.LetterBatch findById(long id) {
        LetterBatch searchResult = null;
        List<LetterBatch> letterBatch = letterBatchDAO.findBy("id", id);
        if (letterBatch != null && !letterBatch.isEmpty()) {
            searchResult = letterBatch.get(0);
        } else {
            return null;
        }
        fi.vm.sade.viestintapalvelu.letter.LetterBatch result = new fi.vm.sade.viestintapalvelu.letter.LetterBatch();
        result.setApplicationPeriod(searchResult.getApplicationPeriod());
        setCommonLetterBatchFields(searchResult, result);
        // kirjeet.vastaanottaja
        // result.setLetters(parseLetterDTOs(searchResult.getLetterReceivers()));
        return result;
    }

    private void setCommonLetterBatchFields(LetterBatch searchResult, fi.vm.sade.viestintapalvelu.letter.LetterBatch result) {
        result.setTemplateId(searchResult.getTemplateId());
        result.setTemplateName(searchResult.getTemplateName());
        result.setFetchTarget(searchResult.getFetchTarget());
        result.setLanguageCode(searchResult.getLanguage());
        result.setStoringOid(searchResult.getStoringOid());
        result.setOrganizationOid(searchResult.getOrganizationOid());
        result.setTag(searchResult.getTag());
        // kirjeet.lahetyskorvauskentat
        result.setTemplateReplacements(parseReplDTOs(searchResult.getLetterReplacements()));
    }

    /* ------------------------------- */
    /* - findLetterBatchByNameOrgTag - */
    /* ------------------------------- */
    @Transactional(readOnly = true)
    public fi.vm.sade.viestintapalvelu.letter.LetterBatch findLetterBatchByNameOrgTag(String templateName, String languageCode, String organizationOid,
            Optional<String> tag, Optional<String> applicationPeriod) {
        fi.vm.sade.viestintapalvelu.letter.LetterBatch result = new fi.vm.sade.viestintapalvelu.letter.LetterBatch();

        LetterBatch letterBatch = letterBatchDAO.findLetterBatchByNameOrgTag(templateName, languageCode, organizationOid, tag, applicationPeriod);
        if (letterBatch != null) {
            result.setApplicationPeriod(letterBatch.getApplicationPeriod());
            result.setTag(letterBatch.getTag());
            setCommonLetterBatchFields(letterBatch, result);
        }
        return result;
    }

    /* ------------------------------- */
    /* - findReplacementByNameOrgTag - */
    /* ------------------------------- */
    @Transactional(readOnly = true)
    public List<fi.vm.sade.viestintapalvelu.template.Replacement> findReplacementByNameOrgTag(String templateName, String languageCode, String organizationOid,
            Optional<String> tag, Optional<String> applicationPeriod) {

        List<fi.vm.sade.viestintapalvelu.template.Replacement> replacements = new LinkedList<>();
        LetterBatch letterBatch = null;
        if (!tag.isPresent() && !applicationPeriod.isPresent()) {
            letterBatch = letterBatchDAO.findLetterBatchByNameOrg(templateName, languageCode, organizationOid);
        } else {
            letterBatch = letterBatchDAO.findLetterBatchByNameOrgTag(templateName, languageCode, organizationOid, tag, applicationPeriod);
        }
        if (letterBatch != null) {

            // kirjeet.lahetyskorvauskentat
            for (LetterReplacement letterRepl : letterBatch.getLetterReplacements()) {
                fi.vm.sade.viestintapalvelu.template.Replacement repl = new fi.vm.sade.viestintapalvelu.template.Replacement();
                repl.setId(letterRepl.getId());
                repl.setName(letterRepl.getName());
                repl.setDefaultValue(letterRepl.getDefaultValue());
                repl.setMandatory(letterRepl.isMandatory());
                repl.setTimestamp(letterRepl.getTimestamp());

                replacements.add(repl);
            }
        }
        return replacements;
    }

    /* ------------- */
    /* - getLetter - */
    /* ------------- */
    @Transactional(readOnly = true)
    public fi.vm.sade.viestintapalvelu.letter.LetterContent getLetter(long id) {

        List<LetterReceiverLetter> letterReceiverLetter = letterReceiverLetterDAO.findBy("id", id);

        fi.vm.sade.viestintapalvelu.letter.LetterContent content = new fi.vm.sade.viestintapalvelu.letter.LetterContent();

        if (letterReceiverLetter != null && !letterReceiverLetter.isEmpty()) {
            LetterReceiverLetter lb = letterReceiverLetter.get(0);

            content.setContentType(lb.getOriginalContentType());
            content.setTimestamp(lb.getTimestamp());

            if (DOCUMENT_TYPE_APPLICATION_ZIP.equals(lb.getContentType())) {
                try {
                    content.setContent(LetterZipUtil.unZip(lb.getLetter()));
                } catch (IOException | DataFormatException e) {
                    content.setContent(lb.getLetter());
                    content.setContentType(lb.getContentType());
                }
            } else {
                content.setContent(lb.getLetter());
            }

        }

        return content;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getLetterContentsByLetterBatchID(Long letterBatchID) throws Exception {
        ByteArrayOutputStream contentsOutputStream = new ByteArrayOutputStream();
        PDFMergerUtility merger = new PDFMergerUtility();

        LetterBatch letterBatch = letterBatchDAO.read(letterBatchID);

        Set<LetterReceivers> letterReceivers = letterBatch.getLetterReceivers();
        for (LetterReceivers letterReceiver : letterReceivers) {
            LetterReceiverLetter letter = letterReceiver.getLetterReceiverLetter();

            byte[] content = letter.getLetter();
            if (letter.getContentType().equals(DOCUMENT_TYPE_APPLICATION_ZIP)) {
                content = LetterZipUtil.unZip(content);
            }
            merger.addSource(new ByteArrayInputStream(content));
        }

        merger.setDestinationStream(contentsOutputStream);
        merger.mergeDocuments();

        return contentsOutputStream.toByteArray();
    }

    private IPosti createIPosti(LetterBatch letterB, Map.Entry<String, byte[]> data) {
        return createIPosti(letterB, data.getKey(), data.getValue());
    }

    private IPosti createIPosti(LetterBatch letterB, String name, byte[] data) {
        IPosti iPosti = new IPosti();
        iPosti.setLetterBatch(letterB);
        iPosti.setContent(data);
        iPosti.setContentName(name);
        iPosti.setContentType(DOCUMENT_TYPE_APPLICATION_ZIP);
        iPosti.setCreateDate(new Date());
        return iPosti;
    }

    /*
     * kirjeet.vastaanottaja
     */
    private Set<LetterReceivers> parseLetterReceiversModels(fi.vm.sade.viestintapalvelu.letter.LetterBatch letterBatch, LetterBatch letterB, ObjectMapper mapper)
            throws JsonProcessingException {
        Set<LetterReceivers> receivers = new HashSet<>();
        for (fi.vm.sade.viestintapalvelu.letter.Letter letter : letterBatch.getLetters()) {
            fi.vm.sade.viestintapalvelu.model.LetterReceivers rec = letterBatchDtoConverter.convert(letter,
                    new fi.vm.sade.viestintapalvelu.model.LetterReceivers(), mapper);
            rec.setLetterBatch(letterB);

            // kirjeet.vastaanottajakirje
            if (letter.getLetterContent() != null) {
                LetterReceiverLetter lrl = new LetterReceiverLetter();
                lrl.setTimestamp(new Date());
                try {
                    lrl.setLetter(LetterZipUtil.zip(letter.getLetterContent().getContent()));
                    lrl.setContentType(DOCUMENT_TYPE_APPLICATION_ZIP); // application/zip
                    lrl.setOriginalContentType(letter.getLetterContent().getContentType()); // application/pdf
                } catch (IOException e) {
                    lrl.setLetter(letter.getLetterContent().getContent());
                    lrl.setContentType(letter.getLetterContent().getContentType()); // application/pdf
                    lrl.setOriginalContentType(letter.getLetterContent().getContentType()); // application/pdf
                }
                lrl.setLetterReceivers(rec);
                rec.setLetterReceiverLetter(lrl);
            }

            receivers.add(rec);
        }
        return receivers;
    }

    private Set<LetterReceivers> parseLetterReceiversModels(
            AsyncLetterBatchDto letterBatch,
            LetterBatch letterB,
            ObjectMapper mapper
    ) throws JsonProcessingException {
        final List<String> contentTypes = letterBatch
                .getContentStructureTypes()
                .stream()
                .map(CONTENT_STRUCTURE_TYPE_STRING_MAPPING::get)
                .map(java.util.Optional::of)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
        Set<LetterReceivers> receivers = new HashSet<>();
        final Date now = new Date();
        for (AsyncLetterBatchLetterDto letter : letterBatch.getLetters()) {
            for (final String contentType : contentTypes) {
                fi.vm.sade.viestintapalvelu.model.LetterReceivers rec = letterBatchDtoConverter.convert(letter,
                        new fi.vm.sade.viestintapalvelu.model.LetterReceivers(), mapper);
                receivers.add(rec);
                rec.setLetterBatch(letterB);

                // kirjeet.vastaanottajakirje, luodaan aina tyhjänä:
                LetterReceiverLetter lrl = new LetterReceiverLetter();
                lrl.setTimestamp(now);
                lrl.setLetterReceivers(rec);
                lrl.setContentType(contentType);
                lrl.setOriginalContentType(contentType);
                lrl.setReadyForPublish(false);
                rec.setLetterReceiverLetter(lrl);
            }
        }
        return receivers;
    }

    private Map<String, Object> parseReplDTOs(Set<LetterReplacement> letterReplacements) {
        Map<String, Object> replacements = new HashMap<>();

        for (LetterReplacement letterRepl : letterReplacements) {
            replacements.put(letterRepl.getName(), letterRepl.getDefaultValue());
        }
        return replacements;
    }

    private LetterBatch storeLetterBatch(LetterBatch letterB) {
        return letterBatchDAO.insert(letterB);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateBatchProcessingStarted(long id, LetterBatchProcess process) {
        LetterBatch batch = letterBatchDAO.read(id);
        if (batch == null) {
            throw new NotFoundException("LetterBatch not found by id=" + id);
        }
        LetterBatch.Status status = batch.getBatchStatus(), newStatus = status;
        switch (process) {
        case EMAIL:
            if (batch.getEmailHandlingStarted() != null) {
                throw new IllegalStateException("Email handling already stated at " + batch.getEmailHandlingStarted() + " for LetterBatch="
                        + batch.getTemplateId());
            }
            batch.setEmailHandlingStarted(new Date());
            break;
        case LETTER:
            batch.setHandlingStarted(new Date());
            newStatus = LetterBatch.Status.processing;
            break;
        case IPOSTI:
            batch.setIpostHandlingFinished(new Date());
            newStatus = LetterBatch.Status.processing_ipost;
            break;
        }
        letterBatchStatusLegalityChecker.ensureLegalStateChange(id, status, newStatus);
        batch.setBatchStatus(newStatus);
        letterBatchDAO.update(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findUnprocessedLetterReceiverIdsByBatch(long batchId) {
        return letterBatchDAO.findUnprocessedLetterReceiverIdsByBatch(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAllReceiversIdsByBatch(long batchId) {
        return letterBatchDAO.findAllLetterReceiverIdsByBatch(batchId);
    }

    @Override
    @Transactional
    public void saveBatchErrorForReceiver(Long letterReceiverId, String message) {
        LetterReceivers receiver = letterReceiversDAO.read(letterReceiverId);

        LetterBatch batch = receiver.getLetterBatch();
        letterBatchStatusLegalityChecker.ensureLegalStateChange(batch.getId(), batch.getBatchStatus(), LetterBatch.Status.error);
        batch.setBatchStatus(LetterBatch.Status.error);

        LetterBatchLetterProcessingError error = new LetterBatchLetterProcessingError();
        error.setErrorTime(new Date());
        error.setLetterReceivers(receiver);
        error.setLetterBatch(batch);
        error.setErrorCause(Optional.fromNullable(message).or("Unknown (TODO)"));
        batch.addProcessingErrors(error);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processLetterReceiver(long receiverId) throws Exception {
        LetterReceivers receiver = letterReceiversDAO.read(receiverId);
        LetterBatch batch = receiver.getLetterBatch();
        ObjectMapper mapper = objectMapperProvider.getContext(getClass());
        getLetterBuilder().constructPagesForLetterReceiverLetter(receiver, batch, formReplacementMap(batch, mapper), formReplacementMap(receiver, mapper));
        letterReceiverLetterDAO.update(receiver.getLetterReceiverLetter());
    }

    protected void processIpostiBatchForLetterReceivers(long letterBatchId, List<Long> batchReceiversIds, int index) {
        logger.info("Processing IPosti {} for LetterBatch={} with {} receivers", index, letterBatchId, batchReceiversIds.size());
        LetterBatch batch = letterBatchDAO.read(letterBatchId);
        List<LetterReceiverLetter> receiverLetters = letterReceiverLetterDAO.getLetterReceiverLettersByLetterReceiverIds(batchReceiversIds);
        String templateName = batch.getTemplateName();
        String lang = batch.getLanguage();
        String zipName = templateName + "_" + lang + "_" + index + ".zip";
        byte[] iPostZipBytes = getLetterBuilder().printZIP(receiverLetters, templateName, zipName);
        IPosti iposti = createIPosti(batch, zipName, iPostZipBytes);
        iPostiDAO.insert(iposti);

        for (LetterReceiverLetter letter : receiverLetters) {
            LetterReceivers receiver = letter.getLetterReceivers();
            receiver.setContainedInIposti(iposti);
            letterReceiversDAO.update(receiver);
        }
    }

    private Map<String, Object> formReplacementMap(fi.vm.sade.viestintapalvelu.model.LetterBatch batch, ObjectMapper mapper) throws IOException {
        Map<String, Object> replacements = new HashMap<>();
        for (LetterReplacement repl : batch.getLetterReplacements()) {
            replacements.put(repl.getName(), repl.getEffectiveValue(mapper));
        }
        return replacements;
    }

    private Map<String, Object> formReplacementMap(LetterReceivers receiver, ObjectMapper mapper) throws IOException {
        Map<String, Object> replacements = new HashMap<>();
        for (LetterReceiverReplacement repl : receiver.getLetterReceiverReplacement()) {
            replacements.put(repl.getName(), repl.getEffectiveValue(mapper));
        }
        return replacements;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<LetterBatchProcess> updateBatchProcessingFinished(long id, LetterBatchProcess process) throws Exception {
        LetterBatchProcess nextProcess = null;
        LetterBatch batch = letterBatchDAO.read(id);
        if (batch == null) {
            throw new NotFoundException("LetterBatch not found by id=" + id);
        }
        LetterBatch.Status status = batch.getBatchStatus(), newStatus = status;
        switch (process) {
        case EMAIL:
            logger.info("EMAIL processing finished for  letter batch {}", id);
            batch.setEmailHandlingFinished(new Date());
            break;
        case LETTER:
            batch.setHandlingFinished(new Date());
            Boolean saveIpostiAsPdf = false;
            if (batch.isIposti() && !batch.getSkipDokumenttipalvelu()) {
                saveIpostiAsPdf = true;
            }
            if (saveIpostiAsPdf) {
                logger.info("LETTER processing and saving pdf to Dokumenttipalvelu finished for IPosti letter batch {}", id);
                savePdfDocument(batch);
                newStatus = LetterBatch.Status.waiting_for_ipost_processing;
                nextProcess = LetterBatchProcess.IPOSTI;
            } else if (batch.isIposti()) {
                logger.info("LETTER processing finished for IPosti letter batch {}", id);
                newStatus = LetterBatch.Status.waiting_for_ipost_processing;
                nextProcess = LetterBatchProcess.IPOSTI;
            } else {
                logger.info("LETTER processing finished for  letter batch {}", id);
                if(!batch.getSkipDokumenttipalvelu() ) {
                    savePdfDocument(batch);
                } else {
                    logger.info("NOT saving pdf document to Dokumenttipalvelu for LetterBatch={}...", batch.getId());
                }
                newStatus = LetterBatch.Status.ready;
            }
            break;
        case IPOSTI:
            logger.info("IPOSTI processing finished for  letter batch {}", id);
            batch.setIpostHandlingFinished(new Date());
            logger.warn("NOT saving zip document to Dokumenttipalvelu for LetterBatch={} or ANY letter batch temporarily (OY-224)...", batch.getId());
            newStatus = LetterBatch.Status.ready;
            break;
        }
        letterBatchStatusLegalityChecker.ensureLegalStateChange(id, status, newStatus);
        batch.setBatchStatus(newStatus);
        letterBatchDAO.update(batch);
        if (nextProcess != null) {
            logger.info("Current status {}, next process: {}", batch.getBatchStatus(), nextProcess);
        }
        return Optional.fromNullable(nextProcess);
    }

    private void savePdfDocument(LetterBatch batch) throws Exception {
        logger.info("Saving pdf document to Dokumenttipalvelu for LetterBatch={}...", batch.getId());
        String documentId = dokumenttiIdProvider.generateDocumentIdForLetterBatchId(batch.getId(), LetterService.DOKUMENTTI_ID_PREFIX_PDF);
        String fileName = Optional.fromNullable(batch.getTemplateName()).or("mergedletters") + "_" + Optional.fromNullable(batch.getLanguage()).or("FI")
                + ".pdf";
        List<String> tags = Stream.of("viestintapalvelu", batch.getFetchTarget(), documentId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        byte[] bytes = getLetterContentsByLetterBatchID(batch.getId());
        logger.info("Stroring PDF with documentId={}", documentId);
        dokumenttipalvelu.save(documentId, fileName, now().plusDays(STORE_DOKUMENTTIS_DAYS).toDate(), tags,
                ContentTypes.CONTENT_TYPE_PDF, new ByteArrayInputStream(bytes));
        logger.info("Done saving pdf document to Dokumenttipalvelu for LetterBatch={}", batch.getId());
        }

    @Override
    @Transactional(readOnly = true)
    public LetterBatch fetchById(long id) {
        return letterBatchDAO.read(id);
    }

    @Override
    @Transactional(readOnly = true)
    public LetterBatchStatusDto getBatchStatus(long batchId) {
        LetterBatchStatusDto batch = letterBatchDAO.getLetterBatchStatus(batchId);

        if (batch == null) {
            batch = new LetterBatchStatusDto(null, null, null, LetterBatch.Status.error, 0);
            List<LetterBatchStatusErrorDto> errors = new ArrayList<>();
            LetterBatchStatusErrorDto error = new LetterBatchStatusErrorDto();
            error.setErrorCause("Batch not found for id " + batchId);
            error.setErrorTime(new Date());
            errors.add(error);
            batch.setErrors(errors);
            return batch;
        }

        // This happens when an earlier batch process has encountered an error
        // when processing the batch
        // with the given batchId.
        // The error messages are stored in the actual model class, so we need
        // to fetch that
        // to get hold of the error messages and pass them to the DTO.
        if (LetterBatch.Status.error.equals(batch.getStatus())) {
            LetterBatch actualBatch = fetchById(batchId);
            List<LetterBatchStatusErrorDto> processingErrors = new ArrayList<>();
            for (LetterBatchProcessingError error : actualBatch.getProcessingErrors()) {
                LetterBatchStatusErrorDto errorDto = new LetterBatchStatusErrorDto();
                errorDto.setErrorCause(error.getErrorCause());
                errorDto.setErrorTime(error.getErrorTime());
                if (error instanceof LetterBatchLetterProcessingError && ((LetterBatchLetterProcessingError) error).getLetterReceivers() != null) {
                    errorDto.setRecipientId(((LetterBatchLetterProcessingError) error).getLetterReceivers().getId());
                }
                processingErrors.add(errorDto);
            }
            batch.setErrors(processingErrors);
            return batch;
        }

        return batch;
    }

    @Override
    @Transactional
    public void updateLetter(LetterReceiverLetter letter) {
        letterReceiverLetterDAO.update(letter);
    }

    @Override
    public List<Long> findUnfinishedLetterBatches() {
        return letterBatchDAO.findUnfinishedLetterBatches();
    }

    @Override
    @Transactional
    public LetterBatchSplitedIpostDto splitBatchForIpostProcessing(long letterBatchId) {
        logger.info("splitBatchForIpostProcessing {}", letterBatchId);

        LetterBatchSplitedIpostDto job = new LetterBatchSplitedIpostDto();
        List<Long> allReceiverIds = letterReceiversDAO.findLetterRecieverIdsByLetterBatchIdForIpostiProcessing(letterBatchId);
        List<List<Long>> splitted = CollectionHelper.split(allReceiverIds, MAX_IPOST_CHUNK_SIZE);
        int orderNumber = 1;
        for (List<Long> receiverIds : splitted) {
            logger.info("Total {} receivers in letterBatch's {} IPosti {}", receiverIds.size(), letterBatchId, orderNumber);
            IPostiProcessable processable = new IPostiProcessable(letterBatchId, orderNumber);
            processable.addLetterReceiverIds(receiverIds);
            job.add(processable);
            orderNumber++;
        }
        return job;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processIposti(IPostiProcessable processable) throws Exception {
        if (processable.getLetterReceiverIds().isEmpty()) {
            return;
        }
        processIpostiBatchForLetterReceivers(processable.getLetterBatchId(), processable.getLetterReceiverIds(), processable.getOrderNumber());
    }

    @Override
    @Transactional
    public void handleIpostError(IPostiProcessable iPostiProcessable, Exception e) {
        logger.error("Error processing IPostiProcessable: " + iPostiProcessable + ". Message: " + e.getMessage(), e);

        LetterBatch batch = letterBatchDAO.read(iPostiProcessable.getLetterBatchId());
        letterBatchStatusLegalityChecker.ensureLegalStateChange(iPostiProcessable.getLetterBatchId(), batch.getBatchStatus(), LetterBatch.Status.error);
        batch.setBatchStatus(LetterBatch.Status.error);

        LetterBatchIPostProcessingError error = new LetterBatchIPostProcessingError();
        error.setOrderNumber(iPostiProcessable.getOrderNumber());
        error.setLetterBatch(batch);
        error.setErrorTime(new Date());
        error.setErrorCause(Optional.fromNullable(e.getMessage()).or("Unknown"));
        batch.addProcessingErrors(error);
        letterBatchDAO.update(batch);
    }

    @Override
    @Transactional
    public void errorProcessingBatch(long letterBatchId, Exception e) {
        LetterBatch batch = letterBatchDAO.read(letterBatchId);
        logger.error("Error processing LetterBatch: " + letterBatchId + " in status " + batch.getBatchStatus() + ". Message: " + e.getMessage(), e);

        letterBatchStatusLegalityChecker.ensureLegalStateChange(letterBatchId, batch.getBatchStatus(), LetterBatch.Status.error);
        batch.setBatchStatus(LetterBatch.Status.error);

        LetterBatchGeneralProcessingError error = new LetterBatchGeneralProcessingError();
        error.setLetterBatch(batch);
        error.setErrorTime(new Date());
        error.setErrorCause(Optional.fromNullable(e.getMessage()).or("Unknown"));
        batch.addProcessingErrors(error);
        letterBatchDAO.update(batch);
    }

    public void setLetterBuilder(LetterBuilder letterBuilder) {
        this.letterBuilder = letterBuilder;
    }

    public LetterBuilder getLetterBuilder() {
        if (this.letterBuilder == null && this.applicationContext != null) {
            this.letterBuilder = applicationContext.getBean(LetterBuilder.class);
        }
        return this.letterBuilder;
    }

    public void setLetterBatchDAO(LetterBatchDAO letterBatchDAO) {
        this.letterBatchDAO = letterBatchDAO;
    }

    public void setLetterReceiverLetterDAO(LetterReceiverLetterDAO letterReceiverLetterDAO) {
        this.letterReceiverLetterDAO = letterReceiverLetterDAO;
    }

    public void setCurrentUserComponent(CurrentUserComponent currentUserComponent) {
        this.currentUserComponent = currentUserComponent;
    }

    public void setLetterReceiversDAO(LetterReceiversDAO letterReceiversDAO) {
        this.letterReceiversDAO = letterReceiversDAO;
    }

    public void setObjectMapperProvider(ObjectMapperProvider objectMapperProvider) {
        this.objectMapperProvider = objectMapperProvider;
    }

    public void setLetterBatchStatusLegalityChecker(LetterBatchStatusLegalityChecker letterBatchStatusLegalityChecker) {
        this.letterBatchStatusLegalityChecker = letterBatchStatusLegalityChecker;
    }

    public void setDokumenttipalvelu(Dokumenttipalvelu dokumenttipalvelu) {
        this.dokumenttipalvelu = dokumenttipalvelu;
    }

    public void setDokumenttiIdProvider(DokumenttiIdProvider dokumenttiIdProvider) {
        this.dokumenttiIdProvider = dokumenttiIdProvider;
    }

    @Override
    public String getLetterTypeByLetterBatchID(Long letterBatchID) {
        return letterBatchDAO.findTemplateNameForLetterBatch(letterBatchID);
    }

    @Override
    public LetterListResponse listLettersByUser(String persoinOid) {
        LetterListResponse response = new LetterListResponse();
        if(StringUtils.isNotBlank(persoinOid)) {
            response.setLetters(letterBatchDAO.findLettersReadyForPublishByPersonOid(persoinOid));
        }
        logger.debug("Found " + response.toString() + " for person " + persoinOid + " for publish.");
        return response;
    }

    @Override
    public LetterReceiverLetterDTO getLetterReceiverLetter(Long id) throws IOException, DataFormatException {
        LetterReceiverLetterDTO letterReceiverLetterDTO = new LetterReceiverLetterDTO();

        LetterReceiverLetter letterReceiverLetter = letterReceiverLetterDAO.read(id);
        LetterBatch letterBatch = letterReceiverLetter.getLetterReceivers().getLetterBatch();

        letterReceiverLetterDTO.setContentType(letterReceiverLetter.getOriginalContentType());
        letterReceiverLetterDTO.setId(letterReceiverLetter.getId());

        if (letterReceiverLetter.getContentType().equalsIgnoreCase("application/zip")) {
            letterReceiverLetterDTO.setLetter(LetterZipUtil.unZip(letterReceiverLetter.getLetter()));
        } else {
            letterReceiverLetterDTO.setLetter(letterReceiverLetter.getLetter());
        }

        letterReceiverLetterDTO.setTemplateName(letterBatch.getTemplateName());

        return letterReceiverLetterDTO;
    }

    @Override
    public Optional<Long> getLetterBatchIdReadyForPublish(String hakuOid, String type, String language) {
        return letterBatchDAO.getLetterBatchIdReadyForPublish(hakuOid, type, language);
    }

    @Override
    public Optional<Long> getLetterBatchIdReadyForEPosti(String hakuOid, String type, String language) {
        return letterBatchDAO.getLetterBatchIdReadyForEPosti(hakuOid, type, language);
    }

    public LetterBatchCountDto countLetterStatuses(String hakuOid, String type, String language) {
        LetterBatchCountDto count = letterBatchDAO.countBatchStatus(hakuOid, type, language);
        return count;
    }

    @Override
    public Map<String, String> getEPostiEmailAddresses(long letterBatchId) {
        return letterBatchDAO.getEPostiEmailAddressesByBatchId(letterBatchId);
    }


}
