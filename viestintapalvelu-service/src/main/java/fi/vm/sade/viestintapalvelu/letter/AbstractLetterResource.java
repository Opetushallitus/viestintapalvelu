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

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.gson.GsonBuilder;
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.wordnik.swagger.annotations.ApiParam;

import fi.vm.sade.viestintapalvelu.AsynchronousResource;
import fi.vm.sade.viestintapalvelu.common.util.FilenameHelper;
import fi.vm.sade.viestintapalvelu.dao.dto.LetterBatchStatusDto;
import fi.vm.sade.viestintapalvelu.letter.dto.AsyncLetterBatchDto;
import fi.vm.sade.viestintapalvelu.validator.LetterBatchValidator;
import fi.vm.sade.viestintapalvelu.validator.UserRightsValidator;

public abstract class AbstractLetterResource extends AsynchronousResource {
    private final Logger LOG = LoggerFactory.getLogger(LetterResource.class);

    @Autowired
    protected LetterBuilder letterBuilder;

    @Autowired
    protected Dokumenttipalvelu dokumenttipalvelu;

    @Resource(name = "otherAsyncResourceJobsExecutorService")
    protected ExecutorService executor;

    @Autowired
    protected LetterService letterService;

    @Autowired
    protected UserRightsValidator userRightsValidator;

    @Autowired
    protected LetterBatchProcessor letterPDFProcessor;

    @Autowired
    protected LetterEmailService letterEmailService;

    @Autowired
    protected DokumenttiIdProvider dokumenttiIdProvider;

    protected Response listByPerson(String personOid) {
        return Response.ok(letterService.listLettersByUser(personOid)).build();
    }

    protected Response countLetterStatuses(String hakuOid, String type, String language) {
        return Response.ok(letterService.countLetterStatuses(hakuOid,type,language)).build();
    }

    protected Response createAsyncLetter(@ApiParam(value = "Muodostettavien kirjeiden tiedot (1-n)", required = true) final AsyncLetterBatchDto input,
            boolean anonymousRequest) {

        LOG.info("New letter batch received. Starting validation.");
        LetterResponse response = new LetterResponse();
        try {
            Map<String, String> errors = LetterBatchValidator.validate(input);
            if (errors != null) {
                LOG.error("Validaatiovirheitä! \r\n{}", new GsonBuilder().setPrettyPrinting().create().toJson(errors.keySet()));
                response.setStatus(LetterResponse.STATUS_ERROR);
                response.setErrors(errors);
                return Response.ok(response).build();
            }
        } catch (Exception e) {
            LOG.error("Validation error.", e);
            return Response.status(Status.BAD_REQUEST).build();
        }

        try {
            LOG.info("Letter batch is valid. Starting to fetch template. {}", input.toStringForLogging());
            letterBuilder.initTemplateId(input);
            LOG.info("Fetching template is ready. Saving letter batch of types {} for further processing. {}", input.getContentStructureTypes(), input.toStringForLogging());
            fi.vm.sade.viestintapalvelu.model.LetterBatch letter = letterService.createLetter(input, anonymousRequest);
            LOG.info("Letter batch is saved with batchID={}. Scheduling async processing for it. {}", letter.getId(), input.toStringForLogging());
            Long id = letter.getId();
            letterPDFProcessor.processLetterBatch(id);
            LOG.info("Letter batch with batchID={} is scheduled.", letter.getId());
            response.setStatus(LetterResponse.STATUS_SUCCESS);
            response.setBatchId(getPrefixedLetterBatchID(id, (input.isIposti() && input.isSkipDokumenttipalvelu())));
            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.error("Letter async failed. " + input.toStringForLogging(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Response getLetterBatchStatus(@PathParam("letterBatchId") @ApiParam(value = "Kirjelähetyksen id") String prefixedLetterBatchId) {
        long letterBatchId = getLetterBatchId(prefixedLetterBatchId);
        LetterBatchStatusDto status = letterService.getBatchStatus(letterBatchId);
        if (status == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }

        return Response.status(Status.OK).entity(status).build();
    }

    protected long getLetterBatchId(String id) {
        id = FilenameHelper.withoutExtension(id);
        // Expect format:
        // VIES-<tyyppi>-id-<HASH(suola+"VIES-"+tyyppi+id+tallentaja-oid)>
        if (id.startsWith(LetterService.DOKUMENTTI_ID_PREFIX_PDF)) {
            return dokumenttiIdProvider.parseLetterBatchIdByDokumenttiId(id, LetterService.DOKUMENTTI_ID_PREFIX_PDF);
        } else if (id.startsWith(LetterService.DOKUMENTTI_ID_PREFIX_ZIP)) {
            return dokumenttiIdProvider.parseLetterBatchIdByDokumenttiId(id, LetterService.DOKUMENTTI_ID_PREFIX_ZIP);
        } else {
            return Long.parseLong(id);
        }
    }

    protected String getPrefixedLetterBatchID(long id, boolean isZip) {
        if (isZip) {
            return dokumenttiIdProvider.generateDocumentIdForLetterBatchId(id, LetterService.DOKUMENTTI_ID_PREFIX_ZIP);
        } else {
            return dokumenttiIdProvider.generateDocumentIdForLetterBatchId(id, LetterService.DOKUMENTTI_ID_PREFIX_PDF);
        }
    }

    protected boolean isLetterBatchStatusReady(LetterBatchStatusDto status) {
        return fi.vm.sade.viestintapalvelu.model.LetterBatch.Status.ready.equals(status.getStatus());
    }
}
