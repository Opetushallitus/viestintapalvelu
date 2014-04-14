package fi.vm.sade.ryhmasahkoposti.externalinterface.route;

import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.ryhmasahkoposti.externalinterface.component.CurrentUserComponent;

/**
 * Luokka Camel- ja CXF-reitin luomiseksi autentikaatiopalveluun omien tietojen hakemiseksi
 * 
 * @author vehei1
 *
 */
@Component
public class OmattiedotRoute extends SpringRouteBuilder {
	private static Logger LOGGER = LoggerFactory.getLogger(OmattiedotRoute.class);
    private static String ROUTE_GET_CURRENT_USER = "direct:getCurrentUser";
    private static String ROUTE_GET_CURRENT_USER_ORGANIZATIONS = "direct:getCurrentUserOrganizations";
    private CurrentUserComponent currentUserComponent;
    
    /**
     * Muodostin omat tiedot komponentin asettamiseksi
     * 
     * @param currentUserComponent Hae omat tiedot komponentti
     */
    @Autowired
    public OmattiedotRoute(CurrentUserComponent currentUserComponent) {
        this.currentUserComponent = currentUserComponent;
    }
    
	/**
	 * Muodostaa omien tietojen hakuun liittyvät reitit autentikointipalveluun
	 * 
	 */
	@Override
	public void configure() throws Exception {
	    LOGGER.info("Configure route to OmatTiedotResource");
	    from(ROUTE_GET_CURRENT_USER).bean(currentUserComponent);
	    from(ROUTE_GET_CURRENT_USER_ORGANIZATIONS).bean(currentUserComponent);
	}
}
