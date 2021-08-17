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
package fi.vm.sade.viestintapalvelu.asiointitili;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.BadRequestException;

import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import fi.vm.sade.viestintapalvelu.model.types.ContentTypes;
import org.apache.commons.io.IOUtils;
import org.apache.geronimo.mail.util.StringBufferOutputStream;
import org.bouncycastle.util.encoders.Base64Encoder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;


import fi.suomi.asiointitili.*;
import fi.vm.sade.viestintapalvelu.common.util.BeanValidator;
import fi.vm.sade.viestintapalvelu.common.util.ValidHetu;
import fi.vm.sade.viestintapalvelu.common.util.XMLTypeHelper;
import fi.vm.sade.viestintapalvelu.common.util.impl.BeanValidatorImpl;
import fi.vm.sade.viestintapalvelu.externalinterface.asiointitili.dto.*;
import fi.vm.sade.viestintapalvelu.externalinterface.asiointitili.dto.converter.AsiointitiliDtoConverter;
import fi.vm.sade.viestintapalvelu.externalinterface.asiointitili.impl.AsiointitiliCommunicationServiceImpl;
import fi.vm.sade.viestintapalvelu.util.CatchParametersAnswers;

import static fi.vm.sade.viestintapalvelu.util.AnswerChain.atFirstReturn;
import static fi.vm.sade.viestintapalvelu.util.CatchParametersAnswers.catchAllParameters;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
@ContextConfiguration("/test-application-context.xml")
@TestExecutionListeners(listeners = {DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class})
@Transactional(readOnly=true)
public class AsiointitiliCommunicationServiceImplTest {
    @Mock
    private Viranomaispalvelut asiointitiliViranomaispalvelutClient;
    @InjectMocks
    private AsiointitiliCommunicationServiceImpl asiointitiliService;
    private AsiointitiliDtoConverter asiointitiliDtoConverter = new AsiointitiliDtoConverter();
    private BeanValidator validator = new BeanValidatorImpl();

    @Before
    public void setup() {
        asiointitiliService.setAsiointitiliDtoConverter(asiointitiliDtoConverter);
        asiointitiliService.setViranomainenKayttajatunnus("KT");
        asiointitiliService.setViranomainenTunnus("VT");
        asiointitiliService.setVirnaomainenPalvelutunnus("PT");
        asiointitiliService.setVirnaomainenSanomaVarmenne("SV");
    }

    @Test
    public void testLisaaKohteitaAsiointitilille() throws Exception {
        KohdeLisaysDto kysely = new KohdeLisaysDto();
        KohdeDto kohde = new KohdeDto();
        kohde.getAsiakkaat().add(asiakas());
        kohde.setViranomaisTunniste("13f22ca5-b8222-48e3-712c-333ac5223i4x_TESTI");
        kohde.setVahvistusVaatimus(false);
        kohde.setAsiaNumero("AsiaNumero K17 (TESTI)");
        kohde.setLahetysPvm(new DateTime(2013, 4, 2, 7, 11, 31, DateTimeZone.UTC));
        kohde.setLahettajaNimi("LahettajaNimi K20");
        kohde.setNimeke("Nimike K18");
        kohde.setKuvausTeksti("Klikkaa opintopolkuun osoittavaa linkkiä siirtyäksesi hyväksymään opiskelupaikka. KuvausTeksti K21");
        kohde.setMaksullisuus(false);

        TiedostoDto tiedosto = tiedostoFromFile("/testfiles/test.pdf");
        tiedosto.setKuvaus("Hello world päätös");
        kohde.getTiedostot().add(tiedosto);

        tiedosto = new TiedostoDto();
        tiedosto.setKuvaus("Siirry Opintopolku palveluun");
        tiedosto.setNimi("SiirryOpintopolkuPalveluun");
        tiedosto.setUrl("https://testi.opintopolku.fi/omatsivut/login");
        tiedosto.setMuoto("text/url");
        tiedosto.setKoko(10);
        kohde.getTiedostot().add(tiedosto);
        kysely.getKohteet().add(kohde);

        VastausWS2 vastausWs = new VastausWS2();
        vastausWs.setKohdeMaara(1);
        ArrayOfKohdeJaAsiakasTilaWS2V kohteet = new ArrayOfKohdeJaAsiakasTilaWS2V();
        KohdeJaAsiakasTilaWS2V vastausKohde = new KohdeJaAsiakasTilaWS2V();
        vastausKohde.setViranomaisTunniste("13f22ca5-b8222-48e3-712c-333ac5223i4x_TESTI");
        AsiakasJaKohteenTila vastausAsiakas = new AsiakasJaKohteenTila();
        vastausAsiakas.setAsiakasTunnus("010101-123N");
        vastausAsiakas.setTunnusTyyppi("SSN");
        vastausAsiakas.setAsiointitiliTunniste("12345");
        vastausAsiakas.setKohteenTila(200);
        vastausAsiakas.setKohteenTilaKuvaus("OK");
        vastausKohde.getAsiakas().add(vastausAsiakas);
        kohteet.getKohde().add(vastausKohde);
        vastausWs.setKohteet(kohteet);
        vastausWs.setTilaKoodi(tilakoodi());
        CatchParametersAnswers<VastausWS2> answer = catchAllParameters(atFirstReturn(vastausWs));
        when(asiointitiliViranomaispalvelutClient.lisaaKohteita(any(Viranomainen.class), any(KyselyWS2.class))).then(answer);

        // Ensure this tests a valid case (assumed to be valid in service level):
        validator.validate(kysely);

        // To get test material for Swagger try it out, uncomment:
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JodaModule());
//        System.out.println(mapper.writeValueAsString(kysely));

        KohdeLisaysVastausDto vastaus = asiointitiliService.lisaaKohteitaAsiointitilille(kysely);

        assertNotNull(vastaus);
        assertEquals(1, vastaus.getKohdeMaara());
        assertEquals(10, vastaus.getTilaKoodi());
        assertEquals("Koodi10", vastaus.getKuvaus());
        assertEquals(1, vastaus.getKohteet().size());
        assertEquals("13f22ca5-b8222-48e3-712c-333ac5223i4x_TESTI", vastaus.getKohteet().get(0).getViranomaisTunniste());
        assertEquals(200, vastaus.getKohteet().get(0).getAsiakas().get(0).getKohteenTila());

        assertEquals(1, answer.getInvocationCount());
        assertNotNull(answer.getArguments().get(0).get(0));
        assertValidViranomainen(answer);

        KyselyWS2 kyselyWs = (KyselyWS2) answer.getArguments().get(0).get(1);
        assertEquals(1, kyselyWs.getKohdeMaara());
        assertNotNull(kyselyWs.getKohteet());
        assertEquals(1, kyselyWs.getKohteet().getKohde().size());
        KohdeWS2 kohdeWs = kyselyWs.getKohteet().getKohde().get(0);
        assertEquals("13f22ca5-b8222-48e3-712c-333ac5223i4x_TESTI", kohdeWs.getViranomaisTunniste());
        assertEquals("LahettajaNimi K20", kohdeWs.getLahettajaNimi());
        assertEquals(XMLGregorianCalendarImpl.parse("2013-04-02T07:11:31.000+00:00"), kohdeWs.getLahetysPvm());
        assertEquals("0", kohdeWs.getVahvistusVaatimus());
        assertEquals("0", kohdeWs.getMaksullisuus());
        assertEquals("0", kohdeWs.getVaadiLukukuittaus());
        assertEquals("Nimike K18", kohdeWs.getNimeke());
        assertEquals("Klikkaa opintopolkuun osoittavaa linkkiä siirtyäksesi hyväksymään opiskelupaikka. KuvausTeksti K21",
                kohdeWs.getKuvausTeksti());
        assertNull(kohdeWs.getMaksamisKuvausTeksti());
        assertNotNull(kohdeWs.getAsiakas());
        assertEquals(1, kohdeWs.getAsiakas().size());
        Asiakas asiakas = kohdeWs.getAsiakas().get(0);
        assertEquals("010101-123N", asiakas.getAsiakasTunnus());
        assertEquals("SSN", asiakas.getTunnusTyyppi());

        assertNotNull(kohdeWs.getTiedostot());
        assertEquals(2, kohdeWs.getTiedostot().getTiedosto().size());
        Tiedosto tiedostoWithSisalto = kohdeWs.getTiedostot().getTiedosto().get(0),
                tiedostoWithUrl = kohdeWs.getTiedostot().getTiedosto().get(1);
        assertEquals("test.pdf", tiedostoWithSisalto.getTiedostoNimi());
        assertEquals("Hello world päätös", tiedostoWithSisalto.getTiedostonKuvaus());
        assertEquals(ContentTypes.CONTENT_TYPE_PDF, tiedostoWithSisalto.getTiedostoMuoto());
        assertTrue(Integer.parseInt(tiedostoWithSisalto.getTiedostoKoko()) > 0);
        assertNotNull(tiedostoWithSisalto.getTiedostoSisalto());
        assertNull(tiedostoWithSisalto.getTiedostoURL());

        assertEquals("SiirryOpintopolkuPalveluun", tiedostoWithUrl.getTiedostoNimi());
        assertEquals("Siirry Opintopolku palveluun", tiedostoWithUrl.getTiedostonKuvaus());
        assertEquals("text/url", tiedostoWithUrl.getTiedostoMuoto());
        assertEquals("https://testi.opintopolku.fi/omatsivut/login", tiedostoWithUrl.getTiedostoURL());
        assertNull(tiedostoWithUrl.getTiedostoSisalto());
        assertEquals("10", tiedostoWithUrl.getTiedostoKoko());
    }

    private AsiakasDto asiakas() {
        AsiakasDto asiakas = new AsiakasDto();
        asiakas.setAsiakasTunnus("010101-123N");
        asiakas.setTunnusTyyppi("SSN");
        return asiakas;
    }

    private TiedostoDto tiedostoFromFile(String resourceFile) throws IOException {
        TiedostoDto tiedostoDto = new TiedostoDto();
        byte[] bytes =  IOUtils.toByteArray(getClass().getResourceAsStream(resourceFile));
        Base64Encoder encoder = new Base64Encoder();
        StringBuffer b = new StringBuffer();
        OutputStream out = new StringBufferOutputStream(b);
        encoder.encode(bytes, 0, bytes.length, out);
        tiedostoDto.setSisalto(b.toString());
        tiedostoDto.setKoko(bytes.length / 1024);
        String[] prts = resourceFile.split("/");
        String filename = prts[prts.length-1];
        prts = filename.split("\\.");
        tiedostoDto.setNimi(filename);
        tiedostoDto.setMuoto("application/"+prts[prts.length-1]);
        return tiedostoDto;
    }

    private TilaKoodiWS tilakoodi() {
        TilaKoodiWS vastausTilakoodi = new TilaKoodiWS();
        vastausTilakoodi.setSanomaTunniste("Tunniste");
        vastausTilakoodi.setTilaKoodi(10);
        vastausTilakoodi.setTilaKoodiKuvaus("Koodi10");
        return vastausTilakoodi;
    }

    private void assertValidViranomainen(CatchParametersAnswers<?> answer) {
        Viranomainen viranomainen = (Viranomainen) answer.getArguments().get(0).get(0);
        assertEquals("KT", viranomainen.getKayttajaTunnus());
        assertEquals("PT", viranomainen.getPalveluTunnus());
        assertEquals("VT", viranomainen.getViranomaisTunnus());
        assertNotNull(viranomainen.getSanomaTunniste());
        assertEquals("SV", viranomainen.getSanomaVarmenneNimi());
        assertEquals("1.0", viranomainen.getSanomaVersio());
    }

    @Test
    public void testTarkistaAsiointitilinTila() throws Exception {
        AsiakasTilaTarkastusKyselyDto kysely = new AsiakasTilaTarkastusKyselyDto();
        kysely.getAsiakkaat().add(asiakas());

        // Ensure this tests a valid case (assumed to be valid in service level):
        validator.validate(kysely);

        VastausWS1 vastausWs = new VastausWS1();
        vastausWs.setTilaKoodi(tilakoodi());
        ArrayOfAsiakasJaTilaWS1 asiakkaat = new ArrayOfAsiakasJaTilaWS1();
        AsiakasJaTilaWS1 asiakas = new AsiakasJaTilaWS1();
        asiakas.setAsiakasTunnus("AT");
        asiakas.setTunnusTyyppi("TT");
        asiakas.setTila(123);
        asiakas.setTilaPvm(XMLGregorianCalendarImpl.parse("2013-10-15T10:05:00.000+03:00"));
        asiakkaat.getAsiakas().add(asiakas);
        vastausWs.setAsiakkaat(asiakkaat);
        CatchParametersAnswers<VastausWS1> answer = catchAllParameters(atFirstReturn(vastausWs));
        when(asiointitiliViranomaispalvelutClient.haeAsiakkaita(any(Viranomainen.class), any(KyselyWS1.class))).then(answer);

        AsiakasTilaKyselyVastausDto vastaus = asiointitiliService.tarkistaAsiointitilinTila(kysely);
        assertNotNull(vastaus);
        assertEquals(10, vastaus.getTilaKoodi());
        assertEquals("Koodi10", vastaus.getKuvaus());
        assertEquals(1, vastaus.getAsiakkaat().size());
        AsiakasTilaDto asiakasTila = vastaus.getAsiakkaat().get(0);
        assertEquals("AT", asiakasTila.getAsiakasTunnus());
        assertEquals("TT", asiakasTila.getTunnusTyyppi());
        assertEquals(123, asiakasTila.getTila());
        assertEquals(new DateTime(2013,10,15, 10,5,0, DateTimeZone.forOffsetHours(3)).toDate(), asiakasTila.getTilaPvm().toDate());

        assertEquals(1, answer.getInvocationCount());
        assertNotNull(answer.getArguments().get(0).get(0));
        assertValidViranomainen(answer);

        KyselyWS1 kyselyWs = (KyselyWS1) answer.getArguments().get(0).get(1);
        assertNotNull(kyselyWs);
        assertNotNull(kyselyWs.getAsiakkaat());
        assertEquals(1, kyselyWs.getAsiakkaat().getAsiakas().size());
        assertNull(kyselyWs.getKyselyAlku());
        assertNull(kyselyWs.getKyselyLoppu());
        assertEquals("Asiakkaat", kyselyWs.getKyselyLaji());
    }

    @Test
    public void testHaeAsiakasTiloja() throws Exception {
        HaeAsiakasTilojaKyselyDto kysely = new HaeAsiakasTilojaKyselyDto();
        DateTime begin = now().withTimeAtStartOfDay().minusMonths(10),
                end = now().withTimeAtStartOfDay();
        kysely.setKyselyAlku(begin);
        kysely.setKyselyLoppu(end);

        // Ensure this tests a valid case (assumed to be valid in service level):
        validator.validate(kysely);

        VastausWS1 vastausWs = new VastausWS1();
        vastausWs.setTilaKoodi(tilakoodi());
        ArrayOfAsiakasJaTilaWS1 asiakkaat = new ArrayOfAsiakasJaTilaWS1();
        AsiakasJaTilaWS1 asiakas = new AsiakasJaTilaWS1();
        asiakas.setAsiakasTunnus("AT");
        asiakas.setTunnusTyyppi("TT");
        asiakas.setTila(123);
        asiakas.setTilaPvm(XMLGregorianCalendarImpl.parse("2013-10-15T10:05:00.000+03:00"));
        asiakkaat.getAsiakas().add(asiakas);
        vastausWs.setAsiakkaat(asiakkaat);
        CatchParametersAnswers<VastausWS1> answer = catchAllParameters(atFirstReturn(vastausWs));
        when(asiointitiliViranomaispalvelutClient.haeAsiakkaita(any(Viranomainen.class), any(KyselyWS1.class))).then(answer);

        AsiakasTilaKyselyVastausDto vastaus = asiointitiliService.haeAsiakasTiloja(kysely);
        assertNotNull(vastaus);
        assertEquals(10, vastaus.getTilaKoodi());
        assertEquals("Koodi10", vastaus.getKuvaus());
        assertEquals(1, vastaus.getAsiakkaat().size());
        AsiakasTilaDto asiakasTila = vastaus.getAsiakkaat().get(0);
        assertEquals("AT", asiakasTila.getAsiakasTunnus());
        assertEquals("TT", asiakasTila.getTunnusTyyppi());
        assertEquals(123, asiakasTila.getTila());
        assertEquals(new DateTime(2013,10,15, 10,5,0, DateTimeZone.forOffsetHours(3)).toDate(), asiakasTila.getTilaPvm().toDate());

        assertEquals(1, answer.getInvocationCount());
        assertNotNull(answer.getArguments().get(0).get(0));
        assertValidViranomainen(answer);

        KyselyWS1 kyselyWs = (KyselyWS1) answer.getArguments().get(0).get(1);
        assertNotNull(kyselyWs);
        assertNull(kyselyWs.getAsiakkaat());
        assertEquals(XMLTypeHelper.dateTime(begin), kyselyWs.getKyselyAlku());
        assertEquals(XMLTypeHelper.dateTime(end), kyselyWs.getKyselyLoppu());
        assertEquals("Kaikki", kyselyWs.getKyselyLaji());
    }

    @Test(expected = BadRequestException.class)
    public void testValidatorThrowsBadRequestWithInvalidWS2Request() {
        KohdeLisaysDto kysely = new KohdeLisaysDto();
        validator.validate(kysely);
    }

    @Test(expected = BadRequestException.class)
    public void testValidatorThrowsBadRequestWithInvalidWS1TarkistusRequest() {
        AsiakasTilaTarkastusKyselyDto kysely = new AsiakasTilaTarkastusKyselyDto();
        validator.validate(kysely);
    }

    @Test(expected = BadRequestException.class)
    public void testValidatorThrowsBadRequestWithInvalidWS1HaeAsiakasRequest() {
        HaeAsiakasTilojaKyselyDto kysely = new HaeAsiakasTilojaKyselyDto();
        validator.validate(kysely);
    }

    @Test(expected = BadRequestException.class)
    public void testHetuValidationInvalid() {
        validator.validate(new ClassWithHetu("010165-123A"));
    }

    @Test
    public void testHetuValidation() {
        validator.validate(new ClassWithHetu("010101-123N"));
    }

    public static class ClassWithHetu {
        @ValidHetu
        public String hetu;

        public ClassWithHetu(String hetu) {
            this.hetu = hetu;
        }
    }
}