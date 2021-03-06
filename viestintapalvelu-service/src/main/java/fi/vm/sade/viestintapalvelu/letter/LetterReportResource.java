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

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fi.vm.sade.auditlog.Audit;
import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.auditlog.User;
import fi.vm.sade.converter.PagingAndSortingDTOConverter;
import fi.vm.sade.dto.PagingAndSortingDTO;
import fi.vm.sade.viestintapalvelu.Utils;
import fi.vm.sade.viestintapalvelu.auditlog.AuditLog;
import fi.vm.sade.viestintapalvelu.auditlog.Target;
import fi.vm.sade.viestintapalvelu.auditlog.ViestintapalveluOperation;
import fi.vm.sade.viestintapalvelu.model.types.ContentTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.istack.Nullable;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import fi.vm.sade.viestintapalvelu.AsynchronousResource;
import fi.vm.sade.viestintapalvelu.Constants;
import fi.vm.sade.viestintapalvelu.Urls;
import fi.vm.sade.viestintapalvelu.download.cache.DownloadCache;
import fi.vm.sade.viestintapalvelu.dto.OrganizationDTO;
import fi.vm.sade.viestintapalvelu.dto.letter.LetterBatchReportDTO;
import fi.vm.sade.viestintapalvelu.dto.letter.LetterBatchesReportDTO;
import fi.vm.sade.viestintapalvelu.dto.letter.LetterReceiverLetterDTO;
import fi.vm.sade.viestintapalvelu.dto.query.LetterReportQueryDTO;
import fi.vm.sade.viestintapalvelu.externalinterface.organisaatio.OrganisaatioService;

/**
 * Kirjelähetysten raportoinnin REST-toteutus
 *
 * @author vehei1
 *
 */
@Component("LetterReportResource")
@ComponentScan(value = { "fi.vm.sade.converter" })
@PreAuthorize("isAuthenticated()")
@Path(Urls.REPORTING_PATH)
@Api(value = "/reporting", description = "Kirjel&auml;hetysten raportointi")
public class LetterReportResource extends AsynchronousResource {
    private static Logger logger = LoggerFactory.getLogger(LetterReportResource.class);
    @Autowired
    private LetterReportService letterReportService;
    @Autowired
    private LetterService letterService;
    @Autowired
    private PagingAndSortingDTOConverter pagingAndSortingDTOConverter;
    @Autowired
    private DownloadCache downloadCache;
    @Autowired
    private OrganisaatioService organisaatioService;

    public static final Audit AUDIT = Utils.ViestintaPalveluAudit;

    @Value("${viestintapalvelu.rekisterinpitajaOID}")
    private String rekisterinpitajaOID;
    private LoadingCache<String, List<OrganizationDTO>> currentUserOrganisaatiosCache = CacheBuilder.newBuilder().maximumSize(100)
            .expireAfterAccess(3L, TimeUnit.MINUTES).build(new CacheLoader<String, List<OrganizationDTO>>() {
                public List<OrganizationDTO> load(String oid) throws Exception {
                    return letterReportService.getUserOrganizations();
                }
            });

    /**
     * Hakee käyttäjän organisaation kirjelähetysten tiedot
     *
     * @param organizationOid
     *            Organisaation OID
     * @param nbrOfRows
     *            Haettavien rivien lukumäärä
     * @param page
     *            Sivu, mistä kohdasta haluttu määrä rivejä haetaan
     * @param sortedBy
     *            Taulun sarake, minkä mukaan tiedot lajitellaan
     * @param order
     *            Lajittelujärjestys
     * @return Lista kirjelähetysten raporttirivejä
     */
    @GET
    @Path(Urls.REPORTING_LIST_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Hakee käyttäjän organisaation kirjelähetysten tiedot", notes = "Hakee halutun määrän käyttäjän "
            + "ja hänen organisaantionsa kirjelähetyksiä. Haku voidaan aloittaa tietystä kohtaa ja ne voidaan hakea lajiteltuna "
            + "nousevasti tai laskevasti tietyn sarakkeen mukaan. Palauttaa tietojen mukana käyttäjän kaikki organisaatiot. "
            + "Jos käyttäjä on rekisterinylläpitäjä, haetaan kaikkien kirjelähetysten tiedot organisaatiosta riippumatta.", response = LetterBatchesReportDTO.class, responseContainer = "List")
    public Response getLetterBatchReports(
            @ApiParam(value = "Organisaation oid-tunnus", required = false) @QueryParam(Constants.PARAM_ORGANIZATION_OID) String organizationOid,
            @ApiParam(value = "Haettavien rivien lukumäärä", required = true) @QueryParam(Constants.PARAM_NUMBER_OF_ROWS) Integer nbrOfRows,
            @ApiParam(value = "Sivu, mistä kohdasta haluttu määrä rivejä haetaan", required = true) @QueryParam(Constants.PARAM_PAGE) Integer page,
            @ApiParam(value = "Taulun sarake, minkä mukaan tiedot lajitellaan", required = false) @QueryParam(Constants.PARAM_SORTED_BY) String sortedBy,
            @ApiParam(value = "Lajittelujärjestys", allowableValues = "asc, desc", required = false) @QueryParam(Constants.PARAM_ORDER) String order) {
        List<OrganizationDTO> organizations = currentUserOrganisaatiosCache.getUnchecked(getLoggedInUserOid());
        organizationOid = resolveAllowedOrganizationOid(organizationOid, organizations);

        PagingAndSortingDTO pagingAndSorting = pagingAndSortingDTOConverter.convert(nbrOfRows, page, sortedBy, order);
        LetterBatchesReportDTO letterBatchesReport = letterReportService.getLetterBatchesReport(organizationOid, pagingAndSorting);

        setOrganizations(organizationOid, organizations, letterBatchesReport);

        return Response.ok(letterBatchesReport).build();
    }

    /**
     * Hakee yksittäisen kirjelähetyksen tiedot
     *
     * @param id
     *            Kirjelähetyksen ID
     * @param nbrOfRows
     *            Haettavien rivien lukumäärä
     * @param page
     *            Sivu, mistä kohdasta haluttu määrä rivejä haetaan
     * @param sortedBy
     *            Taulun sarake, minkä mukaan tiedot lajitellaan
     * @param order
     *            Lajittelujärjestys
     * @return Näytettävän kirjelähetyksen tiedot
     */
    @GET
    @Path(Urls.REPORTING_VIEW_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Hakee yksittäisen kirjelähetyksen tiedot", notes = "Haettavat tiedot sisältävät "
            + "vastaanottajien ja kirjeiden tiedot sekä iPosti-lähetyksen tiedot", response = LetterBatchReportDTO.class)
    public Response getLetterBatchReport(@ApiParam(value = "Kirjelähetyksen avain", required = true) @QueryParam(Constants.PARAM_ID) Long id,
            @ApiParam(value = "Haettavien rivien lukumäärä", required = true) @QueryParam(Constants.PARAM_NUMBER_OF_ROWS) Integer nbrOfRows,
            @ApiParam(value = "Sivu, mistä kohdasta haluttu määrä rivejä haetaan", required = true) @QueryParam(Constants.PARAM_PAGE) Integer page,
            @ApiParam(value = "Taulun sarake, minkä mukaan tiedot lajitellaan", required = false) @QueryParam(Constants.PARAM_SORTED_BY) String sortedBy,
            @ApiParam(value = "Lajittelujärjestys", allowableValues = "asc, desc", required = false) @QueryParam(Constants.PARAM_ORDER) String order,
            @ApiParam(value = "Hakusanat", required = false) @QueryParam(Constants.PARAM_QUERY) String query,
            @Context HttpServletRequest request) {
        logger.info("Audit logging getLetterBatchReport (kirjeen lähetysraportin katselu)");
        User user = AuditLog.getUser(request);
        AuditLog.log(AUDIT, user, ViestintapalveluOperation.KIRJELAHETYS_LUKU, Target.KIRJELAHETYS, id.toString(), Changes.EMPTY);

        PagingAndSortingDTO pagingAndSorting = pagingAndSortingDTOConverter.convert(nbrOfRows, page, sortedBy, order);
        LetterBatchReportDTO letterBatchReport = letterReportService.getLetterBatchReport(id, pagingAndSorting, query);
        return Response.ok(letterBatchReport).build();
    }

    /**
     * Hakee vastaanottajan kirjeen ja palauttaa linkin ko. kirjeeseen
     *
     * @param id
     *            Vastaanottajan kirjeen avain
     * @param request
     *            HTTP pyyntö
     * @return Linkin vastaanottajan kirjeen
     */
    @GET
    @Path(Urls.REPORTING_LETTER_PATH)
    @Produces({ ContentTypes.CONTENT_TYPE_PDF, "application/zip" })
    @ApiOperation(value = "Hakee vastaanottajan kirjeen ja palauttaa linkin ko. kirjeeseen", notes = "Hakee "
            + "vastaanottajan kirjeen. Kirjeelle tehdään unzipo ja sisältö laitetaan talteen cacheen. Palautetaan linkin" + " ko. kirjeeseen", response = String.class)
    public Response getReceiversLetter(@ApiParam(value = "Vastaanottajan kirjeen avain") @QueryParam(Constants.PARAM_ID) Long id,
                                       @Context HttpServletRequest request,
                                       @Context HttpServletResponse response) {
        try {
            logger.info("audit logging getReceiversLetter (muodostetun kirjeen katselu)");
            User user = AuditLog.getUser(request);
            AuditLog.log(AUDIT, user, ViestintapalveluOperation.KIRJE_LUKU, Target.KIRJE, id.toString(), Changes.EMPTY);


            LetterReceiverLetterDTO letterReceiverLetter = letterService.getLetterReceiverLetter(id);
            byte[] letterContents = letterReceiverLetter.getLetter();
            if (letterReceiverLetter.getContentType().equals(ContentTypes.CONTENT_TYPE_HTML)) {
                return LetterDownloadHelper.downloadInlineResponse(response, letterContents);
            }
            final String filename = letterReceiverLetter.getTemplateName() + LetterDownloadHelper.determineExtension(letterReceiverLetter.getContentType());
            return LetterDownloadHelper.downloadPdfResponse(filename, response, letterContents);
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Constants.INTERNAL_SERVICE_ERROR).build();
        }
    }

    /**
     * Hakee kirjelähetyksen kirjeiden sisällöt yhdessä PDF:ssä
     *
     * @param id
     *            Kirjelähetyksen avain
     * @param request
     *            HTTP pyyntö
     * @return Linkin kirjeiden sisältöihin
     */
    @GET
    @Path(Urls.REPORTING_CONTENTS_PATH)
    @Produces({ ContentTypes.CONTENT_TYPE_PDF, "application/zip" })
    @ApiOperation(value = "Hakee kirjelähetyksen kirjeiden sisällöt yhdessä PDF:ssä", notes = "Hakee kirjelähetyksen"
            + "vastaanottajien kirjeet. Kirjeille tehdään unzip ja ne yhdistetään yhdeksi PDF:ksi. "
            + "Sisältö laitetaan talteen cacheen. Palautetaan linkin ko. PDF-dokumenteihin", response = String.class)
    public Response getLetterContents(@ApiParam(value = "Kirjelähetyksen avain") @QueryParam(Constants.PARAM_ID) Long id, @Context HttpServletRequest request, @Context HttpServletResponse response) {
        try {
            logger.info("audit logging getLetterContents (kirjekoosteen katselu)");
            User user = AuditLog.getUser(request);
            AuditLog.log(AUDIT, user, ViestintapalveluOperation.KIRJELAHETYS_KOOSTE_LUKU, Target.KIRJELAHETYS, id.toString(), Changes.EMPTY);

            byte[] letterContents = letterService.getLetterContentsByLetterBatchID(id);
            String type = letterService.getLetterTypeByLetterBatchID(id);
            return LetterDownloadHelper.downloadPdfResponse(type + ".pdf", response, letterContents);
        } catch (Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(Constants.INTERNAL_SERVICE_ERROR).build();
        }
    }

    /**
     * Hakee hakuparametrien mukaiset kirjelähetysten tiedot
     *
     * @param organizationOid
     *            Organisaation OID
     * @param searchArgument
     *            Hakuparametri
     * @param nbrOfRows
     *            Haettavien rivien lukumäärä
     * @param page
     *            Sivu, mistä kohdasta haluttu määrä rivejä haetaan
     * @param sortedBy
     *            Taulun sarake, minkä mukaan tiedot lajitellaan
     * @param order
     *            Lajittelujärjestys
     * @return Lista kirjelähetysten raporttirivejä
     */
    @GET
    @Path(Urls.REPORTING_SEARCH_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Hakee hakuparametrien mukaiset kirjelähetysten tiedot", notes = "Hakee hakuparametrien "
            + "mukaisesti halutun määrän käyttäjän ja hänen organisaantionsa kirjelähetyksiä. Haku voidaan aloittaa "
            + "tietystä kohtaa ja ne voidaan hakea lajiteltuna nousevasti tai laskevasti tietyn sarakkeen mukaan. "
            + "Palauttaa tietojen mukana käyttäjän kaikki organisaatiot", response = LetterBatchesReportDTO.class, responseContainer = "List")
    public Response searchLetterBatchReports(
            @ApiParam(value = "Organisaation oid-tunnus", required = false) @QueryParam(Constants.PARAM_ORGANIZATION_OID) String organizationOid,
            @ApiParam(value = "Näytöllä annettu kirjelähetykseen liittyvä hakutekijä esim. kirjepohjan nimi", required = false) @QueryParam(Constants.PARAM_LETTER_BATCH_SEARCH_ARGUMENT) String searchArgument,
            @ApiParam(value = "Näytöllä annettu vastaanottajaan liittyvä hakutekijä esim. kirjeen saajan nimi", required = false) @QueryParam(Constants.PARAM_RECEIVER_SEARCH_ARGUMENT) String receiverSearchArgument,
            @ApiParam(value = "Haun kohde: kirjelähetys=batch, vastaanottajakirje=receiver", required = true) @QueryParam(Constants.PARAM_SEARCH_TARGET) String searchTarget,
            @ApiParam(value = "Haun oid, jolla rajata hakua", required = false) @QueryParam(Constants.PARAM_APPLICATION_PERIOD) String applicationPeriod,
            @ApiParam(value = "Haettavien rivien lukumäärä", required = true) @QueryParam(Constants.PARAM_NUMBER_OF_ROWS) Integer nbrOfRows,
            @ApiParam(value = "Sivu, mistä kohdasta haluttu määrä rivejä haetaan", required = true) @QueryParam(Constants.PARAM_PAGE) Integer page,
            @ApiParam(value = "Taulun sarake, minkä mukaan tiedot lajitellaan", required = false) @QueryParam(Constants.PARAM_SORTED_BY) String sortedBy,
            @ApiParam(value = "Lajittelujärjestys", allowableValues = "asc, desc", required = false) @QueryParam(Constants.PARAM_ORDER) String order) {
        List<OrganizationDTO> organizations = currentUserOrganisaatiosCache.getUnchecked(getLoggedInUserOid());
        organizationOid = resolveAllowedOrganizationOid(organizationOid, organizations);

        LetterReportQueryDTO query = new LetterReportQueryDTO();
        if (organizationOid.equals(rekisterinpitajaOID)) {
            query.setOrganizationOids(null);
        } else {
            query.setOrganizationOids(organisaatioService.findHierarchyOids(organizationOid));
        }
        query.setLetterBatchSearchArgument(searchArgument);
        query.setReceiverSearchArgument(receiverSearchArgument);
        query.setTarget(searchTarget == null ? LetterReportQueryDTO.SearchTarget.batch : LetterReportQueryDTO.SearchTarget.valueOf(searchTarget));
        query.setApplicationPeriod(applicationPeriod);

        PagingAndSortingDTO pagingAndSorting = pagingAndSortingDTOConverter.convert(nbrOfRows, page, sortedBy, order);

        LetterBatchesReportDTO letterBatchesReport = letterReportService.getLetterBatchesReport(query, pagingAndSorting);

        setOrganizations(organizationOid, organizations, letterBatchesReport);

        return Response.ok(letterBatchesReport).build();
    }

    private void setOrganizations(@ApiParam(value = "Organisaation oid-tunnus", required = false) @QueryParam(Constants.PARAM_ORGANIZATION_OID) String organizationOid, List<OrganizationDTO> organizations, LetterBatchesReportDTO letterBatchesReport) {
        letterBatchesReport.setOrganizations(organizations);
        for (int i = 0; i < organizations.size(); i++) {
            OrganizationDTO organization = organizations.get(i);
            if (organization.getOid().equals(organizationOid)) {
                letterBatchesReport.setSelectedOrganization(i);
                break;
            }
        }
    }

    private String getLoggedInUserOid() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    protected String resolveAllowedOrganizationOid(@Nullable String organizationOid, List<OrganizationDTO> allowedOrganizations) {
        if (organizationOid != null && !organizationOid.isEmpty()) {
            for (OrganizationDTO organization : allowedOrganizations) {
                if (organizationOid.equals(organization.getOid())) {
                    return organizationOid;
                }
            }
        }
        return allowedOrganizations.get(0).getOid();
    }
}
