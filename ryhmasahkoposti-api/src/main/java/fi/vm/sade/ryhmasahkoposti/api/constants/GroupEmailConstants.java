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
package fi.vm.sade.ryhmasahkoposti.api.constants;

public interface GroupEmailConstants {
    String HTML_MESSAGE = "html";
    String NOT_HTML_MESSAGE = "";
    String SENDING_SUCCESSFUL = "1";
    String SENDING_FAILED = "0";
    String SENDING_BOUNCED = "2";

    // OID-vakiot
    String OID_OPH_TREE = "1.2.246.562";
    String OID_OPH_PERSON_TREE = "1.2.246.562.24";
    String OID_OPH_ORGANISATION_TREE = "1.2.246.562.10";
}
