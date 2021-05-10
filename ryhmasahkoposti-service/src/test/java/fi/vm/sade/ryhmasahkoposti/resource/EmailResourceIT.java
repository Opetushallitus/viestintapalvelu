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
package fi.vm.sade.ryhmasahkoposti.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.core.Response;

import fi.vm.sade.ryhmasahkoposti.externalinterface.component.BounceComponent;
import org.apache.cxf.helpers.IOUtils;
import org.dom4j.DocumentException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import fi.vm.sade.ryhmasahkoposti.api.dto.*;
import fi.vm.sade.ryhmasahkoposti.config.IntegrationTestConfig;
import fi.vm.sade.ryhmasahkoposti.externalinterface.api.AttachmentResource;
import fi.vm.sade.ryhmasahkoposti.externalinterface.api.TemplateResource;
import fi.vm.sade.ryhmasahkoposti.externalinterface.api.UrisContainerDto;
import fi.vm.sade.ryhmasahkoposti.externalinterface.component.AttachmentComponent;
import fi.vm.sade.ryhmasahkoposti.externalinterface.component.TemplateComponent;
import fi.vm.sade.ryhmasahkoposti.testdata.RaportointipalveluTestData;
import fi.vm.sade.ryhmasahkoposti.util.AnswerChain;
import org.springframework.test.util.ReflectionTestUtils;

import static fi.vm.sade.ryhmasahkoposti.testdata.RaportointipalveluTestData.*;
import static fi.vm.sade.ryhmasahkoposti.util.AnswerChain.atFirst;
import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * User: ratamaa Date: 25.9.2014 Time: 10:30
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class)
@TestExecutionListeners(listeners = { DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class })
@TestPropertySource(properties = {
        "ryhmasahkoposti.bounces.path=testValue",
        "ryhmasahkoposti.smtp.use_tls=false",
        "ryhmasahkoposti.smtp.authenticate=false",
        "ryhmasahkoposti.smtp.username=testUsername",
        "ryhmasahkoposti.smtp.password=testPassword",
        "ryhmasahkoposti.smtp.return_path=testReturnPath",

})

public class EmailResourceIT {
    private static final Logger logger = LoggerFactory.getLogger(EmailResourceIT.class);

    @Autowired
    private EmailResourceImpl emailResource;

    @Autowired
    private TemplateComponent templateComponent;

    @Autowired
    private AttachmentComponent attachmentComponent;

    @Autowired
    private IntegrationTestConfig.MailerStatus mailerStatus;

    @Autowired
    private ApplicationContext applicationContext;

    private TemplateResource templateClient;
    private AttachmentResource attachmentResource;

    @Before
    public void setup() {
        templateClient = mock(TemplateResource.class);
        templateComponent.setTemplateResourceClient(templateClient);

        attachmentResource = mock(AttachmentResource.class);
        attachmentComponent.setAttachmentResource(attachmentResource);
    }

    @Test
    public void testEmailSendingWithReceiverSepcificDownloadedAttachments() throws Exception {
        TemplateDTO template = with(with(assumeTestTemplate("Testitemplate", "FI"), content("email_body",
                "<html><head><title>$title</title><style type=\"text/css\">$tyylit</style></head>"
                        + "<body><p>Hei $etunimi,</p>$sisalto <ul>#foreach ($v in $lista)<li>$v</li>#end</ul> $loppuosa</body></html>")),
                replacement("loppuosa", "<p>Terveisin Lähettäjä</p>"), replacement("title", "Otsikko"), replacement("etunimi", ""),
                replacement("sender-from", "overwritten-from@example.com"));
        String attachmentUri = "viestinta://letterReceiverLetterAttachment/1", attachment2Uri = "viestinta://letterReceiverLetterAttachment/2", attachment3Uri = "viestinta://letterReceiverLetterAttachment/3", attachment4Uri = "viestinta://letterReceiverLetterAttachment/4";
        EmailAttachment attachment = assumeAttachment(attachmentUri, "Liite.pdf", "test.pdf"), attachment2 = assumeAttachment(attachment2Uri, "Liite2.pdf",
                "test2.pdf"), attachment3 = assumeAttachment(attachment3Uri, "Liite3.pdf", "test.pdf"), attachment4 = assumeAttachment(attachment4Uri,
                "Vastaanottajan2Liite.pdf", "test2.pdf"), personalAttachment = produceAttachment("HenkKohtLiite.pdf", "test.pdf"), commonAttachment = produceAttachment(
                "Perusliite.pdf", "test2.pdf");

        EmailData emailData = new EmailData();
        emailData.getEmail().setSubject("Varsinainen otsikko");
        emailData.getEmail().setBody("<p>Varsinainen viestin ssisältö</p>");
        emailData.getEmail().setCharset("UTF-8");
        emailData.getEmail().setHtml(true);
        emailData.getEmail().setTemplateName("Testitemplate");
        emailData.getEmail().setLanguageCode("FI");
        emailData.getEmail().setFrom("varsinainen_from@example.com");
        emailData.getEmail().setReplyTo("varsinainen_replyto@example.com");
        emailData.getEmail().setCallingProcess("prosessi");
        emailData.getEmail().setSenderOid("senderUserOid.123456.78990");
        emailData.getEmail().setOrganizationOid("senderOrganizationOid.123456.7890");
        emailData.getEmail().setSourceRegister(Arrays.asList(new SourceRegister("opintopolku")));
        emailData.getEmail().addEmailAttachement(commonAttachment);
        emailData.setSenderOid("senderUserOid.123456.78990");

        EmailRecipient recipient = new EmailRecipient();
        recipient.setEmail("varsinainen_vastaanottaja@example.com");
        recipient.setLanguageCode("FI");
        recipient.setName("Milla Mallikas");
        recipient.setOid("vastaanottajaHenkiloOid.123456.7890");
        recipient.setOidType("virkailija");
        recipient.setRecipientReplacements(new ArrayList<ReportedRecipientReplacementDTO>());
        recipient.getAttachments().add(personalAttachment);
        recipient.getRecipientReplacements().add(new ReportedRecipientReplacementDTO("etunimi", "Milla"));
        recipient.getRecipientReplacements().add(new ReportedRecipientReplacementDTO("lista", Arrays.asList(1, 2, 3)));
        recipient.getRecipientReplacements().add(new ReportedRecipientReplacementDTO("additionalAttachmentUris", Arrays.asList(attachmentUri, attachment2Uri)));
        recipient.getRecipientReplacements().add(new ReportedRecipientReplacementDTO("additionalAttachmentUri", attachment3Uri));
        emailData.getRecipient().add(recipient);

        EmailRecipient recipient2 = new EmailRecipient();
        recipient2.setEmail("varsinainen_vastaanottaja2@example.com");
        recipient2.setLanguageCode("FI");
        recipient2.setName("Matti Mallikas");
        recipient2.setOid("vastaanottajaHenkiloOid.123456.7891");
        recipient2.setOidType("virkailija");
        recipient2.setRecipientReplacements(new ArrayList<ReportedRecipientReplacementDTO>());
        recipient2.getRecipientReplacements().add(new ReportedRecipientReplacementDTO("etunimi", "Matti"));
        recipient2.getRecipientReplacements().add(new ReportedRecipientReplacementDTO("additionalAttachmentUri", attachment4Uri));
        recipient2.getRecipientReplacements().add(new ReportedRecipientReplacementDTO("sender-from", "special-from@example.com"));
        emailData.getRecipient().add(recipient2);

        final List<String> urlsReportedForDeletion = new ArrayList<String>();
        AnswerChain<Void> answersForAttachmentDeletions = atFirst(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) throws Throwable {
                urlsReportedForDeletion.addAll(((UrisContainerDto) invocation.getArguments()[0]).getUris());
                return null;
            }
        });
        doAnswer(answersForAttachmentDeletions).when(attachmentResource).deleteByUris(any(UrisContainerDto.class));

        long start = System.currentTimeMillis();
        Response response = emailResource.sendEmail(emailData, true);
        assertNotNull(response);
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof EmailSendId);
        EmailSendId id = (EmailSendId) response.getEntity();
        assertNotNull(id.getId());
        // Wait for the queue to be handled:
        while (isSending(id.getId())) {
            Thread.sleep(10l);
        }

        long duration = System.currentTimeMillis() - start;
        logger.info("Duration: " + duration + " ms.");
        assertTrue(duration < 1000l);

        // actually outgoing message at the javax.mail.Transport static send
        // method:
        assertEquals(2, mailerStatus.getMessages().size());
        Message message = mailerStatus.getMessages().get(0);
        assertEquals(1, message.getFrom().length);
        assertEquals(new InternetAddress("overwritten-from@example.com"), message.getFrom()[0]);
        assertEquals("Varsinainen otsikko", message.getSubject());
        assertTrue(message.getContent() instanceof MimeMultipart);
        MimeMultipart multipart = (MimeMultipart) message.getContent();
        assertEquals(6, multipart.getCount());

        MimeBodyPart bodyPart = (MimeBodyPart) multipart.getBodyPart(0);
        String content = bodyPart.getContent().toString();
        assertEquals("<html><head><title>Otsikko</title>" + "<style type=\"text/css\">body {padding:10px;}</style></head>"
                + "<body><p>Hei Milla,</p><p>Varsinainen viestin ssis&auml;lt&ouml;</p> " + "<ul><li>1</li><li>2</li><li>3</li></ul> "
                + "<p>Terveisin L&auml;hett&auml;j&auml;</p></body></html>", content);

        verifyAttachment(commonAttachment, (MimeBodyPart) multipart.getBodyPart(1));
        verifyAttachment(personalAttachment, (MimeBodyPart) multipart.getBodyPart(2));
        verifyAttachment(attachment, (MimeBodyPart) multipart.getBodyPart(3));
        verifyAttachment(attachment2, (MimeBodyPart) multipart.getBodyPart(4));
        verifyAttachment(attachment3, (MimeBodyPart) multipart.getBodyPart(5));

        assertEquals(1, answersForAttachmentDeletions.getTotalCallCount());
        assertEquals(4, urlsReportedForDeletion.size());
        assertTrue(urlsReportedForDeletion.containsAll(Arrays.asList(attachmentUri, attachment2Uri, attachment3Uri, attachment4Uri)));

        // Verify message 2
        Message message2 = mailerStatus.getMessages().get(1);
        multipart = (MimeMultipart) message2.getContent();
        assertEquals(3, multipart.getCount());
        assertEquals(1, message2.getFrom().length);
        assertEquals(new InternetAddress("special-from@example.com"), message2.getFrom()[0]);

        verifyAttachment(commonAttachment, (MimeBodyPart) multipart.getBodyPart(1));
        verifyAttachment(attachment4, (MimeBodyPart) multipart.getBodyPart(2));
        bodyPart = (MimeBodyPart) multipart.getBodyPart(0);
        assertEquals("<html><head><title>Otsikko</title>" + "<style type=\"text/css\">body {padding:10px;}</style></head>"
                + "<body><p>Hei Matti,</p><p>Varsinainen viestin ssis&auml;lt&ouml;</p> " + "<ul></ul> "
                + "<p>Terveisin L&auml;hett&auml;j&auml;</p></body></html>", bodyPart.getContent().toString());
    }

    private void verifyAttachment(EmailAttachment attachment, MimeBodyPart mimePart) throws MessagingException, IOException {
        assertEquals(attachment.getContentType(), mimePart.getContentType());
        assertEquals(attachment.getName(), mimePart.getFileName());
        byte[] attachmentBytes = IOUtils.readBytesFromStream(mimePart.getInputStream());
        assertEquals(new String(attachment.getData()), new String(attachmentBytes));
    }

    private boolean isSending(String sendId) {
        Response response = emailResource.getStatus(sendId);
        assertNotNull(response);
        assertNotNull(response.getEntity());
        assertTrue(response.getEntity() instanceof SendingStatusDTO);
        SendingStatusDTO status = (SendingStatusDTO) response.getEntity();
        return status.getSendingEnded() == null;
    }

    private TemplateDTO assumeTestTemplate(String templateName, String languageCode) throws IOException, DocumentException {
        TemplateDTO template = RaportointipalveluTestData.template(templateName, languageCode);
        when(templateClient.getTemplateContent(eq(templateName), eq(languageCode), eq(TemplateDTO.TYPE_EMAIL), any(String.class))).thenReturn(template);
        when(templateClient.getTemplateByID(eq("" + template.getId()), eq("email"))).thenReturn(template);

        return template;
    }

    private EmailAttachment assumeAttachment(String uri, String name, String testFile) throws IOException {
        EmailAttachment attachment = produceAttachment(name, testFile);
        when(attachmentResource.downloadByUri(eq(uri))).thenReturn(attachment);
        return attachment;
    }

    private EmailAttachment produceAttachment(String name, String testFile) throws IOException {
        Resource resource = applicationContext.getResource("classpath:testfiles/" + testFile);
        EmailAttachment attachment = new EmailAttachment();
        String[] prts = resource.getFilename().split("\\.");
        String extension = prts[prts.length - 1];
        attachment.setContentType("application/" + extension);
        attachment.setName(name);
        attachment.setData(IOUtils.readBytesFromStream(resource.getInputStream()));
        return attachment;
    }
}
