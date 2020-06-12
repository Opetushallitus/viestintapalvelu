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
package fi.vm.sade.viestintapalvelu.document;

import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.viewerpreferences.PDViewerPreferences;
import org.apache.pdfbox.util.PDFMergerUtility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

public class MergedPdfDocument {
    public static final String FALLBACK_PDF_LANGUAGE = "fi";

    private List<DocumentMetadata> documentMetadata;
    private ByteArrayOutputStream intermediaryOutput;
    private ByteArrayOutputStream output;

    private int currentPageNumber;
    private PDFMergerUtility pdfMerger;
    private String language;

    public MergedPdfDocument() {
        this.documentMetadata = new ArrayList<>();
        this.output = new ByteArrayOutputStream();
        this.intermediaryOutput = new ByteArrayOutputStream();
        this.currentPageNumber = 0;
        this.pdfMerger = new PDFMergerUtility();
        pdfMerger.setDestinationStream(intermediaryOutput);
    }

    public void write(PdfDocument pdfDocument) throws IOException, COSVisitorException {
        int startPage = currentPageNumber + 1;
        int pages = 0;
        language = pdfDocument.getLanguage();

        if (pdfDocument.getFrontPage() != null) {
            pages += writePage(pdfMerger, pdfDocument.getFrontPage());
        }

        if (pdfDocument.getAttachment() != null) {
            pages += writePage(pdfMerger, pdfDocument.getAttachment());
        }

        if (pdfDocument.getContentSize() > 0) {
            for (int i = 0; i < pdfDocument.getContentSize(); i++) {
                pages += writePage(pdfMerger, pdfDocument.getContentStream(i));
            }
        }

        documentMetadata.add(new DocumentMetadata(pdfDocument.getAddressLabel(), startPage, pages));
    }

    /**
     * Write current page to merged PDF and return number of pages the written document has.
     *
     * @param pdfMerger Merger to use.
     * @param page BAIS of page contents. BAIS is enforced because this method relies on mark/reset
     * @return Number of written pages, 0 or more.
     */
    private int writePage(PDFMergerUtility pdfMerger, ByteArrayInputStream page) {
        PDDocument doc = null;
        try {
            page.mark(-1);
            // load single doc to extract needed metadata
            doc = PDDocument.load(page);
            currentPageNumber += doc.getNumberOfPages();
            // reset stream back to generate merged output
            page.reset();
            pdfMerger.addSource(page);
            return doc.getNumberOfPages();
        } catch (IOException e) {
            // TODO: logging?
            return 0;
        } finally {
            close(doc);
        }
    }

    private void close(PDDocument pdDocument) {
        if (pdDocument != null) {
            try {
                pdDocument.close();
            } catch (IOException e) {}
        }
    }

    private void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {}
        }
    }

    public List<DocumentMetadata> getDocumentMetadata() {
        return documentMetadata;
    }

    public byte[] buildDocument() {

        // produce final PDF document
        InputStream is = null;
        PDDocument document = null;
        try {
            intermediaryOutput.flush();
            pdfMerger.mergeDocuments();

            is = new ByteArrayInputStream(intermediaryOutput.toByteArray());
            document = PDDocument.load(is);

            PDDocumentCatalog documentCatalog = document.getDocumentCatalog();
            documentCatalog.setLanguage(
                    Optional.ofNullable(language)
                            .orElse(FALLBACK_PDF_LANGUAGE)
                            .toLowerCase());
            documentCatalog.setViewerPreferences(new PDViewerPreferences(new COSDictionary()));
            documentCatalog.getViewerPreferences().setDisplayDocTitle(true);

            document.save(output);
        } catch (COSVisitorException | IOException e) {
        } finally {
            close(is);
            close(document);
        }
        return output.toByteArray();
    }
}
