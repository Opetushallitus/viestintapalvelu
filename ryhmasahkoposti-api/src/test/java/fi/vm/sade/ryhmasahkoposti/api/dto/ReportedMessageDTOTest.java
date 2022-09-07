package fi.vm.sade.ryhmasahkoposti.api.dto;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

public class ReportedMessageDTOTest {

    @Test
    public void testGetBodyReturnsRetractedHakuperusteetToken() {
        ReportedMessageDTO reportedMessageDTO = new ReportedMessageDTO();
        String token = "a02f12b22a21ce3";
        String body = "Header\n" +
                "Url https://itest-oppija.oph.ware.fi/hakuperusteet/app/1.2.246.562.11.00000001342#/token/" + token + " and stuff after URL\n" +
                "Footer";
        assertTrue(body.contains(token));
        reportedMessageDTO.setBody(body);
        assertThat(reportedMessageDTO.getBody(), not(containsString(token)));
        assertThat(reportedMessageDTO.getBody(), containsString("Url https://itest-oppija.oph.ware.fi/hakuperusteet/app/1.2.246.562.11.00000001342#/token/[RETRACTED] and stuff after URL"));
    }

    @Test
    public void testGetBodyReturnsRetractedAtaruToken() {
        ReportedMessageDTO reportedMessageDTO = new ReportedMessageDTO();
        String token = "OkJ6I3Xe8qHbRBLYxX2u7t_pV_dSGS7CWrw_CzmAwLHUKA";
        String body = "Header\n" +
                "Url https://testiopintopolku.fi/hakemus?modify=" + token + " and stuff after URL\n" +
                "Link <a href=\"https://testiopintopolku.fi/hakemus?modify=" + token + "\" target=\"_blank\"> and stuff after link\n" +
                "Footer";
        assertTrue(body.contains(token));
        reportedMessageDTO.setBody(body);
        assertThat(reportedMessageDTO.getBody(), not(containsString(token)));
        assertThat(reportedMessageDTO.getBody(), containsString("Url https://testiopintopolku.fi/hakemus?modify=[RETRACTED] and stuff after URL\n"));
        assertThat(reportedMessageDTO.getBody(), containsString("Link <a href=\"https://testiopintopolku.fi/hakemus?modify=[RETRACTED]\" target=\"_blank\"> and stuff after link\n"));
    }

    @Test
    public void testGetBodyReturnsRetractedMaksutToken() {
        ReportedMessageDTO reportedMessageDTO = new ReportedMessageDTO();
        String token = "a02f12b22a21ce3";
        String body = "Header\n" +
                "Url https://opintopolku.fi/maksut/?secret=" + token + " and stuff after URL\n" +
                "Footer";
        assertTrue(body.contains(token));
        reportedMessageDTO.setBody(body);
        assertThat(reportedMessageDTO.getBody(), not(containsString(token)));
        assertThat(reportedMessageDTO.getBody(), containsString("Url https://opintopolku.fi/maksut/?secret=[RETRACTED] and stuff after URL"));
    }
}