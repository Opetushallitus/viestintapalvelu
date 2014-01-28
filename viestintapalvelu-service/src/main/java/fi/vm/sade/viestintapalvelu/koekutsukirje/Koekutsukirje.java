package fi.vm.sade.viestintapalvelu.koekutsukirje;

import java.util.List;
import java.util.Map;

import fi.vm.sade.viestintapalvelu.address.AddressLabel;

public class Koekutsukirje {
    public Koekutsukirje() {
    }

    public Koekutsukirje(AddressLabel addressLabel, String languageCode,
                           String hakukohde, String letterBodyText, List<Map<String, String>> customLetterContents) {
        this.addressLabel = addressLabel;
        this.languageCode = languageCode;
        this.hakukohde = hakukohde;
        this.letterBodyText = letterBodyText;
        this.customLetterContents = customLetterContents;
    }
    

    /**
     * Osoitetiedot.
     */
    private AddressLabel addressLabel;
    /**
     * Kielikoodi ISO 639-1.
     */
    private String languageCode;
    /**
     * Koekutsukirjeen leipäteksti
     */
    private String letterBodyText;
    
    /**
     * Valinnoista tuleva tieto siitä mihin (kouluun, koulutusohjelmaan jne. ) kokeella haetaan.
     */
    private String hakukohde; 

	/**
     * Placeholder toistaiseksi. Voidaan välittää avain-arvo-pareina mahdollisia lisätietoja, 
     * joita voidaan käyttää mahdollisesti jatkossa esimerkiksi _hakukohteen_ tai _kokeen_ tietojen
     * täyttämiseksi koekutsukirjeeseen. Tässä vaiheessa (20140124, versio 8?), kun virkailijat 
     * saavat kirjoittaa koko kirjeen editorilla, ei ole vielä tiedossa mitä mahdollisia lisäkenttiä
     * seuraaviin versioihin tulee / mitä mahdollisesti halutaan tähänkin versioon kuitenkin mukaan. 
     * 
     * Tällä ratkaisulla ei tarvita uusia muuttujia tähän luokkaan toistaiseksi, vaan voidaan pikaisesti
     * toteuttaa muutokset hakemallla tästä Mapista halutut arvot, jos sellaisia tarvitaan. Jatkossa on ehkä 
     * syytä lisätä tähän luokkaan omat muuttujansa selvyyden vuoksi (ainakin) niille kentille, jotka ovat
     * pakollisia / Petri Mikkelä 20140124
     */
    private List<Map<String, String>> customLetterContents;

    public AddressLabel getAddressLabel() {
        return addressLabel;
    }

    public String getLetterBodyText() {
        return letterBodyText;
    }

    public String getHakukohde() {
    	return hakukohde;
    }

    public List<Map<String, String>> getCustomLetterContents() {
        return customLetterContents;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    @Override
    public String toString() {
        return "Koekutsukirje ["
        		+ "addressLabel=" + addressLabel
                + ", languageCode=" + languageCode
                + ", hakukohde=" + hakukohde
                + ", letterBodyText=" + letterBodyText 
                + "]";
    }
}
