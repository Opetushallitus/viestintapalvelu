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
package fi.vm.sade.viestintapalvelu.address;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.viestintapalvelu.download.cache.DocumentId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.lowagie.text.DocumentException;
import com.wordnik.swagger.annotations.*;

import fi.vm.sade.viestintapalvelu.AsynchronousResource;
import fi.vm.sade.viestintapalvelu.Urls;
import fi.vm.sade.viestintapalvelu.download.Download;
import fi.vm.sade.viestintapalvelu.download.cache.DownloadCache;

import static fi.vm.sade.viestintapalvelu.Utils.filenamePrefixWithUsernameAndTimestamp;
import static fi.vm.sade.viestintapalvelu.Utils.globalRandomId;
import static org.joda.time.DateTime.now;

@Service("AddressLabelResource")
@Singleton
@Path(Urls.ADDRESS_LABEL_RESOURCE_PATH)
@Api(value = "/" + Urls.API_PATH + "/" + Urls.ADDRESS_LABEL_RESOURCE_PATH, description = "Osoitetarrojen k&auml;sittelyn rajapinnat")
public class AddressLabelResource extends AsynchronousResource {
    private final Logger LOG = LoggerFactory.getLogger(AddressLabelResource.class);
    @Autowired
    private DownloadCache downloadCache;
    @Autowired
    private AddressLabelBuilder labelBuilder;
    @Autowired
    private Dokumenttipalvelu dokumenttipalvelu;
    @Resource(name = "otherAsyncResourceJobsExecutorService")
    private ExecutorService executor;

    private final static String FixedTemplateNote = "Tarrapohjan malli on kiinteästi tiedostona jakelupaketissa. ";
    private final static String ApiPDFSync = "Palauttaa tarroiksi tulostettavat osoitteet PDF-muodossa synkronisesti. " + FixedTemplateNote;
    private final static String ApiPDFAsync = "Palauttaa tarroiksi tulostettavat osoitteet PDF-muodossa asynkronisesti. " + FixedTemplateNote;
    private final static String ApiXLSSync = "Palauttaa tarroiksi tulostettavat osoitteet Excel-muodossa synkronisesti. " + FixedTemplateNote;
    private final static String ApiXLSAsync = "Palauttaa tarroiksi tulostettavat osoitteet Excel-muodossa asynkronisesti. " + FixedTemplateNote;
    private final static String PDFResponse400 = "BAD_REQUEST; PDF-tiedoston luonti epäonnistui eikä tiedostoa voi noutaa download-linkin avulla.";
    private final static String XLSResponse400 = "BAD_REQUEST; Excel-tiedoston luonti epäonnistui eikä tiedostoa voi noutaa download-linkin avulla.";

    // Sync routes

    @POST
    @Consumes("application/json")
    @Produces("text/plain")
    @Path("/pdf")
    @ApiOperation(value = ApiPDFSync, notes = ApiPDFSync)
    @ApiResponses(@ApiResponse(code = 400, message = PDFResponse400))
    public Response pdf(@ApiParam(value = "Osoitetiedot", required = true) final AddressLabelBatch input, @Context HttpServletRequest request) {
        try {
            byte[] pdf = labelBuilder.printPDF(input); // TODO: add validation?
            DocumentId docId = downloadCache.addDocument(new Download("application/pdf;charset=utf-8", "addresslabels.pdf", pdf));
            return createResponse(request, docId.getDocumentId() + ".pdf");
        } catch (Exception e) {
            LOG.error("AddressLabel PDF failed: {}", e);
            return createFailureResponse(request);
        }
    }

    @POST
    @Consumes("application/json")
    @Produces("text/plain")
    @Path("/xls")
    @ApiOperation(value = ApiXLSSync, notes = ApiXLSSync)
    @ApiResponses(@ApiResponse(code = 400, message = XLSResponse400))
    public Response xls(@ApiParam(value = "Osoitetiedot", required = true) AddressLabelBatch input, @Context HttpServletRequest request) {
        String documentId;
        try {
            byte[] csv = labelBuilder.printCSV(input);
            documentId = downloadCache
                    .addDocument(new Download("application/vnd.ms-excel", "addresslabels.xls", csv))
                    .getDocumentId();
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("AddressLabel Excel failed: {}", e.getMessage());
            return createFailureResponse(request);
        }
        return createResponse(request, documentId + ".xls");
    }

    @POST
    @Consumes("application/json")
    @Produces({"application/octet-stream", "text/plain"})
    @Path("/sync/pdf")
    @ApiOperation(value = ApiPDFSync, notes = ApiPDFSync)
    @ApiResponses(@ApiResponse(code = 400, message = PDFResponse400))
    public Response syncPdf(@ApiParam(value = "Osoitetiedot", required = true) final AddressLabelBatch input) throws DocumentException, IOException {
        try {
            return Response.ok(new ByteArrayInputStream(labelBuilder.printPDF(input))).build();
        } catch (final Exception e) {
            LOG.error("AddressLabel PDF creation failed: {}", e.getMessage(), e);
            return createFailureResponse(null);
        }
    }

    // Async routes
    @PreAuthorize("isAuthenticated()")
    @POST
    @Consumes("application/json")
    @Produces("text/plain")
    @Path("/async/pdf")
    @ApiOperation(value = ApiPDFAsync, notes = ApiPDFAsync + AsyncResponseLogicDocumentation)
    public Response asyncPdf(@ApiParam(value = "Osoitetiedot", required = true) final AddressLabelBatch input, @Context HttpServletRequest request)
            throws IOException, DocumentException {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final String documentId = globalRandomId();
        executor.execute(new Runnable() {
            public void run() {
                SecurityContextHolder.getContext().setAuthentication(auth);
                try {
                    byte[] pdf = labelBuilder.printPDF(input);
                    dokumenttipalvelu.save(null, filenamePrefixWithUsernameAndTimestamp("addresslabels.pdf"), now().plusDays(2).toDate(),
                            Arrays.asList("viestintapalvelu", "addresslabels"), "application/pdf", new ByteArrayInputStream(pdf));
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error("AddressLabel PDF failed: {}", e.getMessage());
                }
            }
        });
        return createResponse(request, documentId + ".pdf");
    }

    @POST
    @Consumes("application/json")
    @Produces("text/plain")
    @Path("/async/xls")
    @ApiOperation(value = ApiXLSAsync, notes = ApiXLSAsync + AsyncResponseLogicDocumentation)
    public Response asyncXls(@ApiParam(value = "Osoitetiedot", required = true) final AddressLabelBatch input, @Context HttpServletRequest request)
            throws IOException, DocumentException {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final String documentId = globalRandomId();
        executor.execute(new Runnable() {
            public void run() {
                SecurityContextHolder.getContext().setAuthentication(auth);
                try {
                    byte[] csv = labelBuilder.printCSV(input);
                    dokumenttipalvelu.save(null, filenamePrefixWithUsernameAndTimestamp("addresslabels.xls"), now().plusDays(2).toDate(),
                            Arrays.asList("viestintapalvelu", "addresslabels"), "application/vnd.ms-excel", new ByteArrayInputStream(csv));
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.error("AddressLabel PDF failed: {}", e.getMessage());
                }
            }
        });
        return createResponse(request, documentId + ".xls");
    }
}
