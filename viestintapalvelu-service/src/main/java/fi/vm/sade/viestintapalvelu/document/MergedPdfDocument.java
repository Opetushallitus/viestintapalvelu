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
    private ByteArrayOutputStream output;

    private int currentPageNumber;

    public MergedPdfDocument() {
        this.documentMetadata = new ArrayList<>();
        this.output = new ByteArrayOutputStream();
        this.currentPageNumber = 0;
    }

    public void write(PdfDocument pdfDocument) throws IOException, COSVisitorException {
        final ByteArrayOutputStream intermediaryOutput = new ByteArrayOutputStream();

        PDFMergerUtility pdfMerger = new PDFMergerUtility();

        int startPage = currentPageNumber + 1;
        int pages = 0;
        try {
            pdfMerger.setDestinationStream(intermediaryOutput);

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
            pdfMerger.mergeDocuments();
        } finally {
            intermediaryOutput.flush();
        }

        // produce final PDF document
        InputStream is = null;
        PDDocument document = null;
        try {
            is = new ByteArrayInputStream(intermediaryOutput.toByteArray());
            document = PDDocument.load(is);

            PDDocumentCatalog documentCatalog = document.getDocumentCatalog();
            documentCatalog.setLanguage(
                    Optional.ofNullable(pdfDocument.getLanguage())
                            .orElse(FALLBACK_PDF_LANGUAGE)
                            .toLowerCase());
            documentCatalog.setViewerPreferences(new PDViewerPreferences(new COSDictionary()));
            documentCatalog.getViewerPreferences().setDisplayDocTitle(true);

            document.save(output);
        } finally {
            close(is);
            close(document);
        }
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
            try {
                close(doc);
            } catch (IOException e) {}
        }
    }

    private void close(PDDocument pdDocument) throws IOException {
        if (pdDocument != null) {
            pdDocument.close();
        }
    }

    private void close(InputStream is) throws IOException {
        if (is != null) {
            is.close();
        }
    }

    public List<DocumentMetadata> getDocumentMetadata() {
        return documentMetadata;
    }

    public byte[] toByteArray() {
        return output.toByteArray();
    }
}
