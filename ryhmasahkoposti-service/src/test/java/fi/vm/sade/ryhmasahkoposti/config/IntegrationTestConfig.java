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
package fi.vm.sade.ryhmasahkoposti.config;

import fi.vm.sade.dto.HenkiloDto;
import fi.vm.sade.dto.OrganisaatioHenkiloDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;

import fi.vm.sade.externalinterface.KayttooikeusRestClient;
import fi.vm.sade.externalinterface.OppijanumeroRekisteriRestClient;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailBounces;
import fi.vm.sade.ryhmasahkoposti.externalinterface.component.BounceComponent;
import mockit.Mock;
import mockit.MockClass;
import mockit.Mockit;
import org.apache.http.HttpResponse;
import org.dom4j.DocumentException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailAttachment;
import fi.vm.sade.ryhmasahkoposti.api.dto.TemplateDTO;
import fi.vm.sade.ryhmasahkoposti.externalinterface.api.*;
import fi.vm.sade.ryhmasahkoposti.externalinterface.api.dto.OrganisaatioHierarchyResultsDto;
import fi.vm.sade.ryhmasahkoposti.service.DailyTaskRunner;

/**
 * User: ratamaa
 * Date: 25.9.2014
 * Time: 12:38
 */
@Configuration
@ImportResource(value = {"classpath:test-bundle-context.xml"})
public class IntegrationTestConfig {

    public static class MailerStatus {
        private List<Message> messages = new ArrayList<Message>();

        public MailerStatus() {
        }

        public void addSentMessage(Message message) {
            this.messages.add(message);
        }

        public List<Message> getMessages() {
            return messages;
        }
    }

    private static MailerStatus mailerStatus = new MailerStatus();

    @Bean
    DailyTaskRunner systemStartupTasksService() {
        return new DailyTaskRunner() {
            @Override
            public void runDailyTasks() {

            }
        };
    }

    @Bean
    KayttooikeusRestClient kayttooikeusMock() {
        return new KayttooikeusRestClient("baseurl", "webCasUrl",
                "casService", "username", "password") {
            @Override
            public List<OrganisaatioHenkiloDto> getOrganisaatioHenkilo(String henkiloOid) throws IOException {
                return Collections.singletonList(new OrganisaatioHenkiloDto());
            }
        };
    }

    @Bean
    OppijanumeroRekisteriRestClient onrMock() {
        return new OppijanumeroRekisteriRestClient("baseurl", "casService", "webCasUrl",
                "username", "password") {
            @Override
            public HenkiloDto getHenkilo(String henkiloOid) throws IOException {
                HenkiloDto h = new HenkiloDto();
                h.setOidHenkilo(henkiloOid);
                return h;
            }
        };
    }

    @Bean
    BounceResource bounceMock() {
        return new BounceResource() {
            @Override
            public EmailBounces getBounces(String bouncesUrl) {
                return new EmailBounces();
            }
        };
    }

    @Bean
    BounceComponent bounceComponentMock() {
        BounceComponent bc = new BounceComponent();
        return bc;
    }

    @Bean
    MailerStatus mailerStatus() {
        // TODO: Find a way to do this or similar thing (overriding static method) with newer
        // JMockito wihtout it's runner or PwoerMockito without it's runner (seems not possible)
        // (using SpringJUnit4ClassRunner runner)
        Mockit.setUpMock(TransportMock.class);
        return mailerStatus;
    }

    @MockClass(realClass = Transport.class)
    public static class TransportMock {
        @Mock
        public static void send(Message msg) throws MessagingException {
            mailerStatus.addSentMessage(msg);
        }

        @Mock
        public static void send(Message msg, Address[] addresses)
                throws MessagingException {
            mailerStatus.addSentMessage(msg);
        }
    }

    @Bean
    OrganisaatioResource organisaatioResourceStub() {
        return new OrganisaatioResource() {
            @Override
            public OrganisaatioRDTO getOrganisaatioByOID(String oid) {
                throw new IllegalStateException("Please mock me when needed!");
            }

            @Override
            public OrganisaatioHierarchyResultsDto hierarchy(boolean vainAktiiviset) throws Exception {
                throw new IllegalStateException("Please mock me when needed!");
            }
        };
    }

    @Bean
    AttachmentResource attachmentResourceStub() {
        return new AttachmentResource() {
            @Override
            public EmailAttachment downloadByUri(String uri) {
                throw new IllegalStateException("Please mock me when needed!");
            }

            @Override
            public HttpResponse deleteByUris(UrisContainerDto urisContainerDto) {
                throw new IllegalStateException("Please mock me when needed!");
            }
        };
    }

    @Bean
    TemplateResource templateResourceStub() {
        return new TemplateResource() {
            @Override
            public TemplateDTO getTemplateContent(String templateName, String languageCode, String type, String applicationPeriod) throws IOException, DocumentException {
                throw new IllegalStateException("Please mock me when needed!");
            }

            @Override
            public TemplateDTO getTemplateContent(String templateName, String languageCode, String type) throws IOException, DocumentException {
                throw new IllegalStateException("Please mock me when needed!");
            }
            
            @Override
            public TemplateDTO getTemplateByID(String templateId, String type) {
                throw new IllegalStateException("Please mock me when needed!");
            }
        };
    }

    @Bean
    ThreadPoolTaskExecutor emailExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}
