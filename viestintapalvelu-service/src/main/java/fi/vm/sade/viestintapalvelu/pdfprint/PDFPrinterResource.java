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
package fi.vm.sade.viestintapalvelu.pdfprint;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fi.vm.sade.viestintapalvelu.model.types.ContentTypes;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Entities.EscapeMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.lowagie.text.DocumentException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiParam;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.viestintapalvelu.AsynchronousResource;
import fi.vm.sade.viestintapalvelu.Urls;
import fi.vm.sade.viestintapalvelu.document.DocumentBuilder;
import fi.vm.sade.viestintapalvelu.document.MergedPdfDocument;
import fi.vm.sade.viestintapalvelu.document.PdfDocument;
import fi.vm.sade.viestintapalvelu.download.Download;
import fi.vm.sade.viestintapalvelu.download.cache.DownloadCache;
import fi.vm.sade.viestintapalvelu.letter.LetterResource;
import org.springframework.stereotype.Component;

import static fi.vm.sade.viestintapalvelu.Utils.filenamePrefixWithUsernameAndTimestamp;
import static fi.vm.sade.viestintapalvelu.Utils.globalRandomId;
import static org.joda.time.DateTime.now;

@Component("PDFPrinterResource")
@Path(Urls.PRINTER_PATH)
@PreAuthorize("isAuthenticated()")
@Api(value = "/" + Urls.API_PATH + "/" + Urls.PRINTER_PATH, description = "Pdf tulosteiden muodostus rajapinta")
public class PDFPrinterResource extends AsynchronousResource {
    private final Logger LOG = LoggerFactory.getLogger(LetterResource.class);

    @Autowired(required = false)
    private DokumenttiResource dokumenttiResource;
    @Autowired
    private DownloadCache downloadCache;
    @Autowired
    @Qualifier("batchJobExecutorService")
    private ExecutorService executor;
    @Autowired
    private DocumentBuilder documentBuilder;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/isAlive")
    public String isAlive() {
        return "alive";
    }

    /**
     * PDF sync
     *
     * @param input
     * @param request
     * @return
     * @throws IOException
     * @throws DocumentException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/pdf")
    // @ApiOperation(value = ApiPDFSync, notes = ApiPDFSync)
    // @ApiResponses(@ApiResponse(code = 400, message = PDFResponse400))
    public Response pdf(
            @ApiParam(value = "Muodostettavien koekutsukirjeiden tiedot (1-n)", required = true) DocumentSource input,
            @Context HttpServletRequest request) throws IOException,
            DocumentException {
        String documentId = null;
        try {
            byte[] pdf = buildDocument(input);
            String documentName = input.getDocumentName() == null ? "document.pdf" : input.getDocumentName() + ".pdf";
            documentId = downloadCache.addDocument(new Download(
                    "application/pdf;charset=utf-8", documentName, pdf)).getDocumentId();
        } catch (Exception e) {
            LOG.error("Sync PDF failed: " + e.getMessage(), e);
            return createFailureResponse(request);
        }
        return createResponse(request, documentId+".pdf");
    }

    /**
     * Get PDF content
     *
     * @param input
     * @param request
     * @return
     * @throws IOException
     * @throws DocumentException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(ContentTypes.CONTENT_TYPE_PDF)
    @Path("/pdf/content")
    // @ApiOperation(value = "Muodostaa HTML-pohjaisesta asiakirja PDF-dokumentin ja palauttaa sen", notes = "")
    // @ApiResponses(@ApiResponse(code = 400, message = "Document creation failed"))
    public Response pdfContent(
        @ApiParam(value = "Muodostettavien asiakirjojen tiedot (1-n)", required = true) DocumentSource input,
        @Context HttpServletRequest request) throws IOException, DocumentException {
        try {
            byte[] pdf = buildDocument(input);
            return Response.ok(pdf).build();
        } catch (Exception e) {
            LOG.error("Getting PDF content failed: " + e.getMessage(), e);
            return createFailureResponse(request);
        }
    }

    /**
     * PDF async
     *
     * @param input
     * @param request
     * @return
     * @throws IOException
     * @throws DocumentException
     */
    @POST
    @Consumes("application/json")
    @Produces("text/plain")
    @Path("/async/pdf")
    // @ApiOperation(value = ApiPDFAsync, notes = ApiPDFAsync
    // + AsyncResponseLogicDocumentation)
    public Response asyncPdf(
            @ApiParam(value = "PDF source", required = true) final DocumentSource input,
            @Context final HttpServletRequest request) throws IOException,
            DocumentException {
        if (input == null || input.getSources().isEmpty()) {
            LOG.error("Nothing to do {}", input);
            return Response.serverError().entity("Batch was empty!").build();
        }
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        try {
            LOG.info("Authentication {\r\n\tName: {}\r\n\tPrincipal: {}\r\n}", new Object[] { auth.getName(), auth.getPrincipal() });
        } catch (Exception e) {
            LOG.error("No authentication!!!", e);
        }

        final String documentId = globalRandomId();
        executor.execute(new Runnable() {
            public void run() {
                SecurityContextHolder.getContext().setAuthentication(auth);
                try {
                    byte[] pdf = buildDocument(input);
                    String documentName = input.getDocumentName() == null ? "document.pdf"
                            : input.getDocumentName() + ".pdf";
                    dokumenttiResource
                            .tallenna(
                                    null,
                                    filenamePrefixWithUsernameAndTimestamp(documentName),
                                    now().plusDays(2).toDate().getTime(),
                                    Arrays.asList("viestintapalvelu",
                                            documentName, "pdf"),
                                    "application/pdf;charset=utf-8",
                                    new ByteArrayInputStream(pdf));
                } catch (Exception e) {
                    LOG.error("PDF async failed: " + e.getMessage(), e);
                }
            }
        });
        return createResponse(request, documentId+".pdf");
    }

    @GET
    @Path("/getDocumentSource")
    @Produces("application/json")
    public Response getDocumentSource() {
        DocumentSource ds = new DocumentSource();

        List<String> sources = new ArrayList<>();
        sources.add("documentsource text");
        ds.setDocumentName("documentName");
        ds.setSources(sources);

        return Response.ok(ds).build();
    }

    private byte[] buildDocument(DocumentSource input)
            throws DocumentException, IOException, COSVisitorException {
        List<PdfDocument> pdfs = new ArrayList<>();

        for (String source : input.getSources()) {
            Document jsoupDoc = Jsoup.parse(source);
            jsoupDoc.outputSettings(new OutputSettings().escapeMode(EscapeMode.xhtml));
            String parsedSource = jsoupDoc.toString();
            byte[] pdf = documentBuilder.xhtmlToPDF(parsedSource.getBytes());
            PdfDocument doc = new PdfDocument(
                    null,
                    getLanguage(pdf),
                    new PdfDocument.FrontPageData(pdf),
                    null,
                    new PdfDocument.ContentData(pdf)
            );
            pdfs.add(doc);
        }
        MergedPdfDocument mPdf = documentBuilder.merge(pdfs);
        return mPdf.buildDocument();
    }

    private String getLanguage(byte[] pdf) throws IOException {
        InputStream inputStream = null;
        PDDocument document = null;
        try {
            inputStream = new ByteArrayInputStream(pdf);
            document = PDDocument.load(inputStream);
            return document.getDocumentCatalog().getLanguage();
        } finally {
            close(inputStream);
            close(document);
        }
    }

    private void close(PDDocument document) throws IOException {
        if (document != null) {
            document.close();
        }
    }

    private void close(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
