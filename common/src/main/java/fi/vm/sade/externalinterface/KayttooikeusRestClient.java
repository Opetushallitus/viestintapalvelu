package fi.vm.sade.externalinterface;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import fi.vm.sade.dto.OrganisaatioHenkiloDto;
import fi.vm.sade.javautils.legacy_caching_rest_client.CachingRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class KayttooikeusRestClient {
    protected static Logger logger = LoggerFactory.getLogger(KayttooikeusRestClient.class);
    private final CachingRestClient restClient;

    private final String baseUrl;

    public KayttooikeusRestClient(@Value("${kayttooikeus-service.base}") String baseUrl,
                                  @Value("${web.url.cas}") String webCasUrl,
                                  @Value("${kayttooikeus-service.base}/j_spring_cas_security_check") String casService,
                                  @Value("${ryhmasahkoposti.app.username.to.viestintapalvelu}") String username,
                                  @Value("${ryhmasahkoposti.app.password.to.viestintapalvelu}") String password) {
        String callerId = "1.2.246.562.10.00000000001.viestintapalvelu.common";
        this.baseUrl = baseUrl;
        this.restClient = new CachingRestClient(callerId);
        this.restClient.setWebCasUrl(webCasUrl);
        this.restClient.setUsername(username);
        this.restClient.setPassword(password);
        this.restClient.setCasService(casService);
    }

    public List<OrganisaatioHenkiloDto> getOrganisaatioHenkilo(String henkiloOid) throws IOException {
        Type listType = new TypeToken<ArrayList<OrganisaatioHenkiloDto>>(){}.getType();
        String url = baseUrl + "/henkilo/" + henkiloOid + "/organisaatiohenkilo";
        String resultJsonString = this.restClient.getAsString(url);
        return new Gson().fromJson(resultJsonString, listType);
    }
}
