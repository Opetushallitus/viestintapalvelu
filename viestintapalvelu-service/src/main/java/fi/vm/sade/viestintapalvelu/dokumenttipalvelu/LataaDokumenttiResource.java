package fi.vm.sade.viestintapalvelu.dokumenttipalvelu;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiParam;
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.ObjectEntity;
import fi.vm.sade.viestintapalvelu.Urls;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Service("LataaDokumenttiResource")
@Singleton
@Path(Urls.LATAA_RESOURCE_PATH)
@Api(value = "/" + Urls.API_PATH + "/" + Urls.LATAA_RESOURCE_PATH, description = "Dokumenttipalveluun (S3) tallennetun dokumentin lataaminen")
public class LataaDokumenttiResource {
    private static Logger LOGGER = LoggerFactory.getLogger(LataaDokumenttiResource.class);
    private final Dokumenttipalvelu dokumenttipalvelu;
    @Inject
    public LataaDokumenttiResource(final Dokumenttipalvelu dokumenttipalvelu) {
        this.dokumenttipalvelu = dokumenttipalvelu;
    }

    @GET
    @Path("/{key}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response lataa(@ApiParam(value = "Ladattavan dokumentin key", required = true) @PathParam("key") String key,
                          @Context HttpServletResponse response) {
        try {
            final ObjectEntity objectEntity = dokumenttipalvelu.get(key);
            response.setHeader("Content-Type", objectEntity.contentType);
            response.setHeader("Content-Disposition", "attachment; filename=\"" + objectEntity.fileName + "\"");
            response.setHeader("Content-Length", String.valueOf(objectEntity.contentLength));
            response.setHeader("Cache-Control", "private");
            return Response.ok(IOUtils.toByteArray(objectEntity.entity)).build();
        } catch (final Exception e) {
            LOGGER.warn("Error fetching document with key {}", key, e);
            if (e.getCause() != null && e.getCause() instanceof NoSuchKeyException) {
                return Response.status(Response.Status.NOT_FOUND).entity("Document not found with given key").build();
            } else {
                return Response.serverError().build();
            }
        }
    }
}
