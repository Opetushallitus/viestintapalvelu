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
package fi.vm.sade.viestintapalvelu.koekutsukirje;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import fi.vm.sade.viestintapalvelu.document.MergedPdfDocument;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.lowagie.text.DocumentException;

import fi.vm.sade.viestintapalvelu.Constants;
import fi.vm.sade.viestintapalvelu.Utils;
import fi.vm.sade.viestintapalvelu.address.AddressLabelDecorator;
import fi.vm.sade.viestintapalvelu.address.HtmlAddressLabelDecorator;
import fi.vm.sade.viestintapalvelu.api.address.AddressLabel;
import fi.vm.sade.viestintapalvelu.document.DocumentBuilder;
import fi.vm.sade.viestintapalvelu.document.PdfDocument;

/**
 * @author pemik1
 *
 */
/**
 * @author pemik1
 * 
 */
@Service
@Singleton
public class KoekutsukirjeBuilder {

    private DocumentBuilder documentBuilder;

    @Inject
    public KoekutsukirjeBuilder(DocumentBuilder documentBuilder) {
        this.documentBuilder = documentBuilder;
    }

    public byte[] printPDF(KoekutsukirjeBatch batch) throws IOException, DocumentException, COSVisitorException {
        List<PdfDocument> source = new ArrayList<>();
        for (Koekutsukirje kirje : batch.getLetters()) {
            String kirjeTemplateName = Utils.resolveTemplateName(Constants.KOEKUTSUKIRJE_TEMPLATE, kirje.getLanguageCode());
            String tarjoaja = Strings.nullToEmpty(kirje.getTarjoaja());
            byte[] frontPage = createFirstPagePDF(kirjeTemplateName, kirje.getAddressLabel(), kirje.getHakukohde(), tarjoaja, kirje.getLetterBodyText());
            final Optional<String> language = batch.getLetters()
                    .stream()
                    .findAny()
                    .map(Koekutsukirje::getLanguageCode);
            source.add(new PdfDocument(
                    kirje.getAddressLabel(),
                    language.orElse(MergedPdfDocument.FALLBACK_PDF_LANGUAGE),
                    new PdfDocument.FrontPageData(frontPage),
                    null,
                    new PdfDocument.ContentData(frontPage))
            );
        }
        return documentBuilder.merge(source).buildDocument();
    }

    private byte[] createFirstPagePDF(String templateName, AddressLabel addressLabel, String hakukohde, String tarjoaja, String letterBodyText)
            throws IOException, DocumentException {
        Map<String, Object> dataContext = createDataContext(new HtmlAddressLabelDecorator(addressLabel), hakukohde, tarjoaja, letterBodyText);
        byte[] xhtml = documentBuilder.applyTextTemplate(templateName, dataContext);
        return documentBuilder.xhtmlToPDF(xhtml);
    }

    private Map<String, Object> createDataContext(AddressLabelDecorator decorator, String hakukohde, String tarjoaja, String letterBodyText) {
        Map<String, Object> data = new HashMap<>();
        data.put("osoite", decorator);
        data.put("hakukohde", StringEscapeUtils.escapeHtml(hakukohde));
        data.put("tarjoaja", StringEscapeUtils.escapeHtml(tarjoaja));
        data.put("letterDate", new SimpleDateFormat("d.M.yyyy").format(new Date()));
        data.put("letterBodyText", cleanHtmlFromApi(letterBodyText));
        return data;
    }

    private String cleanHtmlFromApi(String letterBody) {
        return Jsoup.clean(letterBody, Whitelist.relaxed());
    }
}
