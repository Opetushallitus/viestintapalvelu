package fi.vm.sade.viestintapalvelu.letter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectEntity;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectMetadata;
import fi.vm.sade.viestintapalvelu.model.types.ContentTypes;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;
import com.lowagie.text.DocumentException;
import com.wordnik.swagger.annotations.*;

import fi.vm.sade.viestintapalvelu.Constants;
import fi.vm.sade.viestintapalvelu.Urls;
import fi.vm.sade.viestintapalvelu.dao.dto.LetterBatchStatusDto;
import fi.vm.sade.viestintapalvelu.letter.dto.AsyncLetterBatchDto;

import static org.joda.time.DateTime.now;

@Component("LetterResource")
@Path(Urls.LETTER_PATH)
@PreAuthorize("isAuthenticated()")
// Use HTML-entities instead of scandinavian letters in @Api-description, since
// swagger-ui.js treats model's description as HTML and does not escape it
// properly
@Api(value = "/" + Urls.API_PATH + "/" + Urls.LETTER_PATH, description = "Kirjeiden muodostusrajapinnat")
public class LetterResource extends AbstractLetterResource {
    private final Logger LOG = LoggerFactory.getLogger(LetterResource.class);

    private final static String ApiEmailPreview = "Tuottaa esikatselun yhdestä kirjelähetykseen liittyvästä sähköpostiviestistä";
    private final static String ApiLetterLanguageOptions = "Kertoo, mitä kieliä annetun kirjeen vastaanottajissa on";
    private final static String ApiEmail = "Lähettää sähköpostiviestin annetun kirjelähetyksen vastaanottajille.";

    private final static String ApiPDFSync = "Palauttaa URLin, josta voi ladata kirjeen/kirjeet PDF-muodossa; synkroninen";
    private final static String ApiPDFAsync = "Palauttaa URLin, josta voi ladata kirjeen/kirjeet PDF-muodossa; asynkroninen";
    private final static String PDFResponse400 = "BAD_REQUEST; PDF-tiedoston luonti epäonnistui eikä tiedostoa voi noutaa download-linkin avulla.";

    private final static String ApiZIPSync = "Palauttaa URLin, josta voi ladata kirjeen/kirjeet Itellan ZIP-muodossa; synkroninen.";
    private final static String ApiZIPAsync = "Palauttaa URLin, josta voi ladata kirjeen/kirjeet Itellan ZIP-muodossa; asynkroninen";
    private final static String ZIPResponse400 = "BAD_REQUEST; ZIP-tiedoston luonti epäonnistui eikä tiedostoa voi noutaa download-linkin avulla.";

    private final static String TemplateByID = "Palauttaa letter pohjan id:n perusteella.";
    private final static String TemplateByID400 = "Pohjan palautus id:n perusteella epäonnistui.";

    private final static String TemplateByNameOrgTag = "Palauttaa letter pohjan nimen, organisaation ja tunnisteen perusteella.";
    private final static String TemplateByNameOrgTag400 = "Pohjan palautus pohjan nimen, organisaation ja tunnisteen perusteella epäonnistui.";

    private final static String GetLetter = "Palauttaa generoidun/lähetetyn/tallennetun kirjeen Id:n perusteella. Id taulusta 'vastaanottajakirje'";

    private final static String ApiEMAILSync = "Palauttaa URLin, josta voi ladata kirjeen/kirjeet Email-muodossa; synkroninen.";

    @GET
    @Produces("text/plain")
    @Path("/isAlive")
    public String isAlive() {
        return "alive";
    }

    @GET
    @Produces("application/json")
    @Path("/list/person/{personOid}")
    @ApiOperation(value="Tuottaa listan hakijan kirjeistä")
    public Response listByPerson( @PathParam("personOid") @ApiParam(name="Henkilön OID", required = true) String personOid) {
        return super.listByPerson(personOid);
    }

    @POST
    @Produces("text/plain")
    @Path("/emailLetterBatch/{letterBatchId}")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_SEND_LETTER_EMAIL)
    @ApiOperation(value = ApiEmail, notes = ApiEmail)
    public Response emailLetterBatch( @PathParam("letterBatchId") @ApiParam(name="Kirjelähetyksen ID", required = true)
                                          String letterBatchId ) throws Exception {
        letterEmailService.sendEmail(getLetterBatchId(letterBatchId));
        return Response.ok().build();
    }

    @GET
    @Produces("message/rfc822")
    @Path("/previewLetterBatchEmail/{letterBatchId}")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_SEND_LETTER_EMAIL)
    @ApiOperation(value = ApiEmailPreview, notes = ApiEmailPreview)
    public Response previewLetterBatchEmail(
            @PathParam("letterBatchId") @ApiParam(value="Kirjelähetyksen ID", required = true) String letterBatchId,
            @QueryParam("language") @ApiParam(value="Muodostuskieli (valinnainen)", required = false) String languageCode ) {
        return Response.ok(
                letterEmailService.getPreview(getLetterBatchId(letterBatchId), Optional.fromNullable(languageCode))
            ).header("Content-Disposition", "attachment; filename=\"preview.eml\"")
            .build();
    }

    @GET
    @Produces("application/json")
    @Path("/languageOptions/{letterBatchId}")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_SEND_LETTER_EMAIL)
    @ApiOperation(value = ApiLetterLanguageOptions, notes = ApiLetterLanguageOptions)
    public Response getLanguageOptions(@PathParam("letterBatchId") @ApiParam(value="Kirjelähetyksen ID", required = true)
                                           String letterBatchId) {
        return Response.ok(
                letterEmailService.getLanguageCodeOptions(getLetterBatchId(letterBatchId))
        ).build();
    }

    @GET
    @Transactional
    @Produces("application/json")
    @Path("/getById")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_READ)
    @ApiOperation(value = TemplateByID, notes = TemplateByID, response = LetterBatch.class)
    // SWAGGER
    @ApiResponses(@ApiResponse(code = 400, message = TemplateByID400))
    // SWAGGER
    public Response templateByID(@Context HttpServletRequest request) throws IOException, DocumentException {
        String letterBatchId = request.getParameter("letterBatchId");
        Long id = Long.parseLong(letterBatchId);

        LetterBatch lb = letterService.findById(id);
        if (lb == null || lb.getTemplateId() == null || lb.getTemplateId() == 0) {
            return Response.status(Status.NOT_FOUND).entity("Template by id " + id + " not found.").build();
        } else {
            return Response.ok(lb).build();
        }
    }

    @GET
    @Transactional
    @Produces("application/json")
    @Path("/getByNameOrgTag")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_READ)
    @ApiOperation(value = TemplateByNameOrgTag, notes = TemplateByNameOrgTag, response = LetterBatch.class)
    // SWAGGER
    @ApiResponses(@ApiResponse(code = 400, message = TemplateByNameOrgTag400))
    // SWAGGER
    public Response templateByNameOidTag(@Context HttpServletRequest request) throws IOException, DocumentException {

        // Pick up the organization oid from request and check user's rights to
        // organization
        String oid = request.getParameter("oid");
        Response response = userRightsValidator.checkUserRightsToOrganization(oid);

        // User isn't authorized to the organization
        if (response.getStatus() != 200) {
            return response;
        }

        String name = request.getParameter("name");
        String language = request.getParameter("language");

        String tag = request.getParameter("tag");

        String applicationPeriod = request.getParameter("applicationPeriod");

        if ((tag == null) || ("".equals(tag))) {
            tag = "%%";
        }

        return Response.ok(letterService.findLetterBatchByNameOrgTag(name, language, oid,
                Optional.of(tag),
                Optional.fromNullable(applicationPeriod))).build();
    }

    @GET
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_CREATE_LETTER)
    @Transactional
    @Produces("application/json")
    @Path("/getLetter")
    @ApiOperation(value = GetLetter, notes = GetLetter, response = LetterContent.class)
    // SWAGGER
    public fi.vm.sade.viestintapalvelu.letter.LetterContent getLetter(@Context HttpServletRequest request)
        throws IOException, DocumentException {
        String letterId = request.getParameter("id");
        Long id = Long.parseLong(letterId);

        return letterService.getLetter(id);
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/async/pdf")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_CREATE_LETTER)
    @ApiOperation(value = "Tallentaa kirjeet asynkronisesti. Palauttaa kirjelähetyksen id:n.",
        notes = "")
    public Response asyncPdf(@ApiParam(value = "Muodostettavien kirjeiden tiedot (1-n)", required = true)
                                     final AsyncLetterBatchDto input) {

        input.setIposti(false);
        return asyncLetter(input);
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/async/zip")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_CREATE_LETTER)
    @ApiOperation(value = "Tallentaa kirjeet asynkronisesti. Palauttaa kirjelähetyksen id:n.",
        notes = "")
    public Response asyncZip(@ApiParam(value = "Muodostettavien kirjeiden tiedot (1-n)", required = true)
                                     final AsyncLetterBatchDto input) {
        input.setIposti(true);
        return asyncLetter(input);
    }

    @POST
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/async/letter")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_CREATE_LETTER)
    @ApiOperation(value = "Tallentaa kirjeet asynkronisesti. Palauttaa kirjelähetyksen id:n.",
        notes = "")
    public Response asyncLetter(@ApiParam(value = "Muodostettavien kirjeiden tiedot (1-n)", required = true)
                                     final AsyncLetterBatchDto input) {
        if(input != null) {
            LOG.info("Aloitetaan kirjeiden luonti: " + input.toStringForLogging());
        } else {
            LOG.info("Aloitetaan kirjeiden luonti (input on null)");
        }

        try {
            return createAsyncLetter(input, false);
        } catch (Exception e) {
            LOG.error("Kirjeiden muodostus epäonnistui: " + e.getMessage(), e);
            throw e;
        }

    }

    // /aync/pdf -> /async/letter
    // /async/zip -> iposti=true

    @GET
    @Consumes("application/json")
    @Produces("application/json")
    @Path("/async/letter/status/{letterBatchId}")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_CREATE_LETTER)
    @ApiOperation(value = "Palauttaa kirjelähetykseen kuuluvien käsiteltyjen kirjeiden määrän ja kokonaismäärän")
    public Response letterBatchStatus(@PathParam("letterBatchId") @ApiParam(value = "Kirjelähetyksen id")  String prefixedLetterBatchId) {
        return getLetterBatchStatus(prefixedLetterBatchId);
    }

    @GET
    @Consumes("application/json")
    @Produces(ContentTypes.CONTENT_TYPE_PDF)
    @Path("/async/letter/pdf/{letterBatchId}")
    @PreAuthorize(Constants.ASIAKIRJAPALVELU_CREATE_LETTER)
    @ApiOperation(value = "Palauttaa kirjelähetyksestä generoidun PDF-dokumentin")
    public Response getLetterBatchPDF(
            @PathParam("letterBatchId")
            @ApiParam(value = "Kirjelähetyksen id")
            final String prefixedLetterBatchId) {
        final long letterBatchId = getLetterBatchId(prefixedLetterBatchId);
        try {
            final LetterBatchStatusDto batchStatus = letterService.getBatchStatus(letterBatchId);
            if (batchStatus == null || ! fi.vm.sade.viestintapalvelu.model.LetterBatch.Status.ready.equals(batchStatus.getStatus())) {
                return Response.status(Status.BAD_REQUEST).build();
            }
            final LetterBatch letterBatch = letterService.findById(letterBatchId);
            final String documentId = "mergedLetterBatch" + letterBatchId;
            final List<String> tags = Stream.of("viestintapalvelu", letterBatch.getFetchTarget(), documentId)
                    .filter(Objects::nonNull).collect(Collectors.toList());
            final Collection<ObjectMetadata> documents = dokumenttipalvelu.find(tags);
            ObjectEntity objectEntity;
            if (!documents.isEmpty()) {
                objectEntity = dokumenttipalvelu.get(documents.stream().findFirst().get().key);
            } else {
                final byte[] bytes = letterService.getLetterContentsByLetterBatchID(letterBatchId);
                final ObjectMetadata saved = dokumenttipalvelu.save(
                        documentId,
                        "mergedletters.pdf",
                        now().plusDays(2).toDate(),
                        tags,
                        ContentTypes.CONTENT_TYPE_PDF,
                        new ByteArrayInputStream(bytes));
                objectEntity = dokumenttipalvelu.get(saved.key);
            }
            return Response.ok(IOUtils.toByteArray(objectEntity.entity))
                    .header("Content-Disposition", "attachment; filename=\"" + objectEntity.fileName + "\"")
                    .header("Content-Type", objectEntity.contentType)
                    .build();

        } catch (final Exception e) {
            LOG.error("Error getting merged pdf for batch " + letterBatchId, e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
