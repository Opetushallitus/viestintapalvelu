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
package fi.vm.sade.ryhmasahkoposti.util;

import fi.vm.sade.ryhmasahkoposti.common.util.InputCleaner;
import org.junit.Test;
import static org.assertj.core.api.Assertions.*;

public class InputCleanerTest {

    @Test
    public void parseHtmlFragment() {
        String h = "<h1>Otsikko</h1><p style=\"color:red\">Leipätekstiä</p>";
        String cleanHtml = InputCleaner.cleanHtmlFragment(h);
        assertThat(cleanHtml).isEqualTo("<h1>Otsikko</h1><p style=\"color:red\">Leip&auml;teksti&auml;</p>");
    }

}
