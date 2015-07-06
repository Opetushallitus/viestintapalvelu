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

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Singleton;

import com.google.common.base.Supplier;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;

import fi.vm.sade.viestintapalvelu.FlyingSaucerReplaceElementFactory;
import fi.vm.sade.viestintapalvelu.OPHUserAgent;
import fi.vm.sade.viestintapalvelu.SLF4JLogChute;

import static fi.vm.sade.viestintapalvelu.Constants.UTF_8;

@Service
@Singleton
public class DocumentBuilder {
    private VelocityEngine templateEngine = new VelocityEngine();

    public DocumentBuilder() {
        // velocity.log -> slf4j loggerille
        templateEngine.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new SLF4JLogChute());
        templateEngine.init();
    }

    public byte[] xhtmlToPDF(byte[] xhtml) throws DocumentException, IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        newITextRenderer(xhtml).createPDF(output);
        return output.toByteArray();
    }

    public byte[] applyTextTemplate(byte[] template, Map<String, Object> data) throws IOException {
        StringWriter writer = new StringWriter();
        templateEngine.evaluate(new VelocityContext(data), writer, "LOG", new InputStreamReader(new ByteArrayInputStream(template)));
        return writer.toString().getBytes();
    }

    public byte[] applyTextTemplate(String templateName, Map<String, Object> data) throws IOException {
        byte[] template = readTemplate(templateName);
        return applyTextTemplate(template, data);
    }

    public MergedPdfDocument merge(PdfDocument pdfDocument) throws DocumentException, IOException {
        MergedPdfDocument mergedPDFDocument = new MergedPdfDocument();
        mergedPDFDocument.write(pdfDocument);
        mergedPDFDocument.flush();
        return mergedPDFDocument;
    }

    public MergedPdfDocument merge(List<PdfDocument> input) throws DocumentException, IOException {
        MergedPdfDocument mergedPDFDocument = new MergedPdfDocument();
        for (PdfDocument pdfDocument : input) {
            mergedPDFDocument.write(pdfDocument);
        }
        mergedPDFDocument.flush();
        return mergedPDFDocument;
    }

    /**
     * Merge xhtml file contents
     * 
     * @param inputs
     * @return
     * @throws DocumentException
     * @throws IOException
     */
    public byte[] mergeByte(List<byte[]> inputs) throws DocumentException, IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (byte[] input : inputs)
            outputStream.write(input);
        return outputStream.toByteArray();
    }

    public byte[] zip(Map<String, Supplier<byte[]>> documents) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zipStream = new ZipOutputStream(out);
        for (String documentName : documents.keySet()) {
            zipStream.putNextEntry(new ZipEntry(documentName));
            zipStream.write(documents.get(documentName).get());
            zipStream.closeEntry();
        }
        zipStream.close();
        return out.toByteArray();
    }

    private ITextRenderer newITextRenderer(byte[] input) {
        ITextRenderer renderer = new ITextRenderer();
        OPHUserAgent uac = new OPHUserAgent(renderer.getOutputDevice());
        FlyingSaucerReplaceElementFactory mref = new FlyingSaucerReplaceElementFactory(renderer.getSharedContext().getReplacedElementFactory());
        uac.setSharedContext(renderer.getSharedContext());
        renderer.getSharedContext().setUserAgentCallback(uac);
        renderer.getSharedContext().setReplacedElementFactory(mref);
        renderer.setDocumentFromString(new String(input, UTF_8));
        renderer.layout();
        return renderer;
    }

    private byte[] readTemplate(String templateName) throws FileNotFoundException, IOException {
        InputStream in = getClass().getResourceAsStream(templateName);
        if (in == null) {
            throw new FileNotFoundException("Template " + templateName + " not found");
        }
        return IOUtils.toByteArray(in);
    }
}
