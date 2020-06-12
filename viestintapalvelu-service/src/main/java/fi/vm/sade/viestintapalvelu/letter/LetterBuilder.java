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
 */
package fi.vm.sade.viestintapalvelu.letter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.lowagie.text.DocumentException;
import fi.vm.sade.externalinterface.common.ObjectMapperProvider;
import fi.vm.sade.viestintapalvelu.Constants;
import fi.vm.sade.viestintapalvelu.address.HtmlAddressLabelDecorator;
import fi.vm.sade.viestintapalvelu.address.XmlAddressLabelDecorator;
import fi.vm.sade.viestintapalvelu.api.address.AddressLabel;
import fi.vm.sade.viestintapalvelu.attachment.AttachmentService;
import fi.vm.sade.viestintapalvelu.attachment.dto.LetterReceiverLEtterAttachmentSaveDto;
import fi.vm.sade.viestintapalvelu.conversion.AddressLabelConverter;
import fi.vm.sade.viestintapalvelu.dao.criteria.TemplateCriteriaImpl;
import fi.vm.sade.viestintapalvelu.document.DocumentBuilder;
import fi.vm.sade.viestintapalvelu.document.DocumentMetadata;
import fi.vm.sade.viestintapalvelu.document.MergedPdfDocument;
import fi.vm.sade.viestintapalvelu.document.PdfDocument;
import fi.vm.sade.viestintapalvelu.letter.dto.AsyncLetterBatchDto;
import fi.vm.sade.viestintapalvelu.letter.html.Cleaner;
import fi.vm.sade.viestintapalvelu.letter.html.XhtmlCleaner;
import fi.vm.sade.viestintapalvelu.model.LetterReceiverLetter;
import fi.vm.sade.viestintapalvelu.model.LetterReceiverReplacement;
import fi.vm.sade.viestintapalvelu.model.LetterReceivers;
import fi.vm.sade.viestintapalvelu.model.UsedTemplate;
import fi.vm.sade.viestintapalvelu.model.types.ContentStructureType;
import fi.vm.sade.viestintapalvelu.model.types.ContentTypes;
import fi.vm.sade.viestintapalvelu.template.Contents;
import fi.vm.sade.viestintapalvelu.template.Replacement;
import fi.vm.sade.viestintapalvelu.template.Template;
import fi.vm.sade.viestintapalvelu.template.TemplateContent;
import fi.vm.sade.viestintapalvelu.template.TemplateService;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fi.vm.sade.viestintapalvelu.util.DateUtil.palautusTimestampEn;
import static fi.vm.sade.viestintapalvelu.util.DateUtil.palautusTimestampFi;
import static fi.vm.sade.viestintapalvelu.util.DateUtil.palautusTimestampSv;

@Service
@Singleton
@ComponentScan(value = { "fi.vm.sade.externalinterface" })
public class LetterBuilder {
    private final Logger LOG = LoggerFactory.getLogger(LetterBuilder.class);

    private DocumentBuilder documentBuilder;
    private TemplateService templateService;
    private AttachmentService attachmentService;
    private ObjectMapperProvider objectMapperProvider;

    @Autowired(required = false)
    public LetterBuilder(DocumentBuilder documentBuilder, TemplateService templateService, AttachmentService attachmentService, ObjectMapperProvider objectMapperProvider) {
        this.documentBuilder = documentBuilder;
        this.templateService = templateService;
        this.attachmentService = attachmentService;
        this.objectMapperProvider = objectMapperProvider;
    }
    public LetterBuilder() {
    }

    public byte[] printZIP(List<LetterReceiverLetter> receivers, String templateName, String zipName) {
        MergedPdfDocument pdf = getMergedPDFDocument(receivers);
        try {
            return getIpostiZip(pdf, templateName, zipName);
        } catch (IOException e) {
            LOG.error("Iposti generation failed ", e);
        }

        return null;
    }

    private byte[] getIpostiZip(final MergedPdfDocument pdf, String templateName, String zipName) throws IOException {
        Map<String, Object> context = createIPostDataContext(pdf.getDocumentMetadata());
        context.put("filename", templateName + ".pdf");
        final byte[] ipostXml = documentBuilder.applyTextTemplate(Constants.LETTER_IPOST_TEMPLATE, context);
        Map<String, Supplier<byte[]>> documents = new HashMap<>();
        documents.put(templateName + ".pdf", new Supplier<byte[]>() {
            @Override
            public byte[] get() {
                return pdf.buildDocument();
            }
        });
        documents.put(templateName + ".xml", new Supplier<byte[]>() {
            @Override
            public byte[] get() {
                return ipostXml;
            }
        });
        return documentBuilder.zip(documents);
    }

    private MergedPdfDocument getMergedPDFDocument(List<LetterReceiverLetter> receivers) {
        MergedPdfDocument result = null;
        try {
            result = new MergedPdfDocument();
            for (LetterReceiverLetter l : receivers) {
                LetterReceivers letterReceiver = l.getLetterReceivers();
                AddressLabel address = AddressLabelConverter.convert(letterReceiver.getLetterReceiverAddress());
                PdfDocument document = new PdfDocument(
                        address,
                        null,
                        null,
                        null,
                        new PdfDocument.ContentData(l.getLetter())
                );
                result.write(document);
            }
        } catch (Exception e) {
            LOG.error("PDF building failed ", e);
        }
        return result;
    }

    void initTemplateId(AsyncLetterBatchDto batch) {
        if (batch.getTemplateId() == null
                && batch.getTemplateName() != null
                && batch.getLanguageCode() != null) {
            batch
                    .getContentStructureTypes()
                    .stream()
                    .map(contentStructureType ->
                            new TemplateCriteriaImpl(
                                    batch.getTemplateName(),
                                    batch.getLanguageCode(),
                                    contentStructureType
                            ).withApplicationPeriod(batch.getApplicationPeriod())
                    )
                    .map(templateCriteria -> templateService.getTemplateByName(templateCriteria, true))
                    .findFirst()
                    .map(Template::getId)
                    .ifPresent(batch::setTemplateId);
        }
    }

    public Map<String, Object> getTemplateReplacements(Template template) {
        Map<String, Object> replacements = new HashMap<>();
        for (Replacement r : template.getReplacements()) {
            replacements.put(r.getName(), r.getDefaultValue());
        }
        return replacements;
    }

    private byte[] createPagePdf(Template template, byte[] pageContent, AddressLabel addressLabel, Map<String, Object> templReplacements,
                                 Map<String, Object> letterBatchReplacements, Map<String, Object> letterReplacements) throws IOException, DocumentException {
        return documentBuilder.xhtmlToPDF(createPageHtml(template, pageContent, addressLabel, templReplacements, letterBatchReplacements, letterReplacements));
    }

    private byte[] createPageHtml(Template template, byte[] pageContent, AddressLabel addressLabel, Map<String, Object> templReplacements,
                                  Map<String, Object> letterBatchReplacements, Map<String, Object> letterReplacements) throws IOException, DocumentException {
        Map<String, Object> dataContext = createDataContext(XhtmlCleaner.INSTANCE,
                template, addressLabel, templReplacements, letterBatchReplacements, letterReplacements);
        byte[] xhtml = documentBuilder.applyTextTemplate(pageContent, dataContext);
        if (xhtml != null && LOG.isDebugEnabled()) {
            LOG.debug("CreatePageHtml using xhtml:\n" + new String(xhtml));
        }
        return xhtml;
    }


    public Map<String, Object> createDataContext(Cleaner cleaner,
                     Template template, AddressLabel addressLabel, Map<String, Object>... replacementsList) {
        Map<String, Object> data = cleanValues(cleaner, replacementsList);

        String styles = template.getStyles();
        if (styles == null) {
            styles = "";
        }

        String palautusPvm = (String)data.get("palautusPvm");
        String somePalautusAika = (String)data.get("palautusAika");
        String palautusAika = null;
        if(somePalautusAika != null) {
            palautusAika = somePalautusAika.replaceAll(":", ".");
        }
        String palautusTimestampFi = palautusTimestampFi(palautusPvm, palautusAika);
        if(palautusTimestampFi != null) {
            data.put("palautusTimestampFi", palautusTimestampFi);
        }
        String palautusTimestampEn = palautusTimestampEn(palautusPvm, palautusAika);
        if(palautusTimestampEn != null) {
            data.put("palautusTimestampEn", palautusTimestampEn);
        }
        String palautusTimestampSv = palautusTimestampSv(palautusPvm, palautusAika);
        if(palautusTimestampSv != null) {
            data.put("palautusTimestampSv", palautusTimestampSv);
        }
        data.put("letterDate", new SimpleDateFormat("d.M.yyyy").format(new Date()));
        if(data.get("syntymaaika") != null) {
            data.put("syntymaaika", formatDate((String) data.get("syntymaaika")));
        }
        data.put("osoite", new HtmlAddressLabelDecorator(addressLabel));
        data.put("addressLabel", new XmlAddressLabelDecorator(addressLabel));

        if (data.containsKey("muut_hakukohteet")) {
            List<String> muidenHakukohteidenNimet = (List<String>) data.get("muut_hakukohteet");
            data.put("muut_hakukohteet", muidenHakukohteidenNimet);
        }

        data.put("tyylit", styles);
        return data;
    }

    private String formatDate(final String dateStr) {
        final SimpleDateFormat sourceDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        final SimpleDateFormat targetDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        try {
            return targetDateFormat.format(sourceDateFormat.parse(dateStr));
        } catch (ParseException ignore) {
            return dateStr;
        }
    }

    private Map<String, Object> cleanValues(Cleaner cleaner, Map<String, Object>... replacementsList) {
        Map<String, Object> data = new HashMap<>();
        for (Map<String, Object> replacements : replacementsList) {
            if (replacements != null) {
                for(Map.Entry<String, Object> entry : replacements.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        data.put(entry.getKey(), cleanString(cleaner, entry.getValue()));
                    } else if(entry.getValue() instanceof Map){
                        Map<String,Object> v = (Map<String,Object>)entry.getValue();
                        data.put(entry.getKey(), cleanValues(cleaner,v));
                    } else if(entry.getValue() instanceof List){
                        List<Object> values = (List<Object>)entry.getValue();
                        List<Object> nv = new ArrayList<>();
                        for(Object v : values) {
                            if(v instanceof String) {
                                nv.add(cleanString(cleaner, v));
                            } else if(v instanceof Map) {
                                Map<String, Object> adsf = (Map<String, Object>)v;
                                nv.add(cleanValues(cleaner, adsf));
                            } else {
                                nv.add(v);
                            }
                        }
                        data.put(entry.getKey(), nv);
                    } else {
                        data.put(entry.getKey(), entry.getValue());
                    }

                }
            }
        }
        return data;
    }

    private String cleanString(Cleaner cleaner, Object entry) {
        return StringEscapeUtils.unescapeHtml(((String) entry))
                .replaceAll("[&\u200B]", "").replaceAll("’","'").replaceAll("”","\"").replaceAll("–","-");
    }

    private List<Map<String, Object>> normalizeColumns(Cleaner cleaner,
                               Map<String, Boolean> columns, List<Map<String, Object>> tulokset) {
        if (tulokset == null) {
            return null;
        }
        for (Map<String, Object> row : tulokset) {
            for (String column : columns.keySet()) {
                if (!row.containsKey(column) || row.get(column) == null) {
                    row.put(column, "");
                }
                Object obj = row.get(column);
                if(obj instanceof String) {
                    row.put(column, cleaner.clean((String) obj));
                } else if(obj instanceof ArrayList<?> && (((ArrayList<?>)obj).size() > 0 && ((ArrayList<?>)obj).get(0) instanceof Map<?, ?>)) {
                    for (Map<String, Object> map: (ArrayList<Map<String, Object>>) obj) {
                        for(Map.Entry<String, Object> entry: map.entrySet()) {
                            if (entry.getValue() != null) {
                                entry.setValue(cleaner.clean(entry.getValue().toString()));
                            }
                        }
                    }
                    row.put(column, obj);
                } else {
                    row.put(column, "");
                }

            }
        }
        return tulokset;
    }

    private Map<String, Object> createIPostDataContext(final List<DocumentMetadata> documentMetadataList) {
        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> metadataList = new ArrayList<>();
        for (DocumentMetadata documentMetadata : documentMetadataList) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("startPage", documentMetadata.getStartPage());
            metadata.put("pages", documentMetadata.getPages());
            metadata.put("addressLabel", new XmlAddressLabelDecorator(documentMetadata.getAddressLabel()));
            metadataList.add(metadata);
        }
        data.put("metadataList", metadataList);
        data.put("ipostTest", Constants.IPOST_TEST);
        return data;
    }

    public void constructPagesForLetterReceiverLetter(
            LetterReceivers receiver,
            fi.vm.sade.viestintapalvelu.model.LetterBatch batch,
            Map<String, Object> batchReplacements,
            Map<String, Object> letterReplacements
    ) throws DocumentException, COSVisitorException, IOException {
        final Template template = determineTemplate(receiver, batch);
        constructPagesForLetterReceiverLetter(
                receiver,
                template,
                batchReplacements,
                letterReplacements
        );
    }


    public LetterReceiverLetter constructPagesForLetterReceiverLetter(
            LetterReceivers receiver,
            Template template,
            Map<String, Object> batchReplacements,
            Map<String, Object> letterReplacements
    ) throws IOException, DocumentException, COSVisitorException {
        LetterReceiverLetter letter = receiver.getLetterReceiverLetter();

        Map<String, Object> templReplacements = formReplacementMap(template.getReplacements());

        ObjectMapper mapper = objectMapperProvider.getContext(getClass());
        templReplacements.putAll(formReplacementMap(letter.getLetterReceivers().getLetterReceiverReplacement(), mapper));

        AddressLabel address = AddressLabelConverter.convert(letter.getLetterReceivers().getLetterReceiverAddress());

        switch (ContentStructureType.valueOf(template.getType())) {
            case letter:
                addFullPdfDataToLetterReceiverLetter(
                        letter,
                        receiver,
                        template,
                        address,
                        templReplacements,
                        batchReplacements,
                        letterReplacements
                );
                break;
            case accessibleHtml:
                addHtmlDataToLetterReceiverLetter(
                        letter,
                        template,
                        address,
                        templReplacements,
                        batchReplacements,
                        letterReplacements
                );
                break;
            default:
                throw new NullPointerException("Ei voitu generoida kirjedataa vastaanottajalle " + receiver.getId() + " ja vastaanottajakirjeelle " + letter.getId());
        }

        return letter;
    }

    private void addFullPdfDataToLetterReceiverLetter(
            final LetterReceiverLetter letterReceiverLetter,
            final LetterReceivers letterReceivers,
            final Template template,
            final AddressLabel addressLabel,
            final Map<String, Object> templateReplacements,
            final Map<String, Object> batchReplacements,
            final Map<String, Object> letterReplacements) throws IOException, DocumentException, COSVisitorException {
        final PdfDocument currentDocument = new PdfDocument(addressLabel);
        final List<TemplateContent> templateContents = template
                .getContents()
                .stream()
                .sorted()
                .collect(Collectors.toList());
        for (TemplateContent tc : Contents.pdfLetterContents().filter(templateContents)) {
            try {
                final byte[] page = createPagePdf(
                        template,
                        tc.getContent().getBytes(),
                        addressLabel,
                        templateReplacements,
                        batchReplacements,
                        letterReplacements
                );
                if (letterReceiverLetter.getLetterReceivers().getEmailAddress() != null
                        && !letterReceiverLetter.getLetterReceivers().getEmailAddress().isEmpty()
                        && Contents.ATTACHMENT.equals(tc.getName())
                        && letterReceivers.getLetterReceiverEmail() == null) {
                    saveLetterReceiverAttachment(tc.getName(), page, letterReceivers.getLetterReceiverLetter().getId());
                }
                currentDocument.addContent(new PdfDocument.ContentData(page));
            } catch (Exception e) {
                final String errorMessage = "Ei voitu luoda PDF-dokumenttia vastaanottajakirjeelle " + letterReceiverLetter.getId() + " kun käytettiin kirjepohjaa " + template + " sisällöllä " + tc;
                LOG.error(errorMessage, e);
                throw e;
            }
        }
        letterReceiverLetter.setLetter(documentBuilder.merge(currentDocument).buildDocument());
        letterReceiverLetter.setContentType(ContentTypes.CONTENT_TYPE_PDF);
        letterReceiverLetter.setOriginalContentType(ContentTypes.CONTENT_TYPE_PDF);
    }

    private void addHtmlDataToLetterReceiverLetter(
            final LetterReceiverLetter letterReceiverLetter,
            final Template template,
            final AddressLabel addressLabel,
            final Map<String, Object> templateReplacements,
            final Map<String, Object> batchReplacements,
            final Map<String, Object> letterReplacements) throws IOException, DocumentException {
        final List<TemplateContent> templateContents = template
                .getContents()
                .stream()
                .sorted()
                .collect(Collectors.toList());
        final TemplateContent templateContent = Contents
                .accessibleHtmlContents()
                .filter(templateContents)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Vastaanottajakirjeelle " + letterReceiverLetter.getId() + " ei voitu luoda saavutettavan HTML:n tarvitsemaa kirjepohjaa"));
        final byte[] currentDocument = createPageHtml(
                template,
                templateContent.getContent().getBytes(),
                addressLabel,
                templateReplacements,
                batchReplacements,
                letterReplacements
        );
        letterReceiverLetter.setLetter(currentDocument);
        letterReceiverLetter.setContentType(ContentTypes.CONTENT_TYPE_HTML);
        letterReceiverLetter.setOriginalContentType(ContentTypes.CONTENT_TYPE_HTML);
    }

    private void saveLetterReceiverAttachment(String name, byte[] page, long letterReceiverLetterId) {
        LetterReceiverLEtterAttachmentSaveDto attachment = new LetterReceiverLEtterAttachmentSaveDto();
        attachment.setName(Optional.fromNullable(name).or("liite") + ".pdf");
        attachment.setContentType(ContentTypes.CONTENT_TYPE_PDF);
        attachment.setContents(page);
        attachment.setLetterReceiverLetterId(letterReceiverLetterId);
        attachmentService.saveReceiverAttachment(attachment);
    }

    private Template determineTemplate(
            LetterReceivers receiver,
            fi.vm.sade.viestintapalvelu.model.LetterBatch batch
    ) {
        final LetterReceiverLetter letterReceiverLetter = receiver.getLetterReceiverLetter();
        final String contentType = letterReceiverLetter.getContentType();
        final ContentStructureType contentStructureType = ContentTypes
                .getContentStructureType(letterReceiverLetter)
                .orElseThrow(() -> new NullPointerException("Vastaanottajan " + receiver.getId() + " vastaanottajakirjeellä " + letterReceiverLetter.getId() + " on tuntematon kirjepohjan tyyppi " + contentType));
        if (receiver.getWantedLanguage() == null) {
            return templateService.findById(batch.getTemplateId(), contentStructureType);
        }
        return batch
                .getUsedTemplates()
                .stream()
                .map(UsedTemplate::getTemplate)
                .filter(template -> template.getLanguage().equals(receiver.getWantedLanguage()))
                .map(template -> templateService.findById(template.getId(), contentStructureType))
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Ei voitu konstruoida vastaanottajalle " + receiver.getId() + " kirjepohjaa sisältötyypillä " + contentType));
    }

    private Map<String, Object> formReplacementMap(Set<LetterReceiverReplacement> replacements, ObjectMapper mapper) throws IOException {
        Map<String, Object> templReplacements = new HashMap<>();
        for (LetterReceiverReplacement replacement : replacements) {
            templReplacements.put(replacement.getName(), replacement.getEffectiveValue(mapper));
        }
        return templReplacements;
    }

    private static Map<String, Object> formReplacementMap(List<Replacement> replacements) {
        Map<String, Object> templReplacements = new HashMap<>();
        for (Replacement templRepl : replacements) {
            templReplacements.put(templRepl.getName(), templRepl.getDefaultValue());
        }
        return templReplacements;
    }
}
