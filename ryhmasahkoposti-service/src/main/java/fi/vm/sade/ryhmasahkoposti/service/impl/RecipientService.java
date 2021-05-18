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
package fi.vm.sade.ryhmasahkoposti.service.impl;

import fi.vm.sade.dto.HenkiloDto;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.ryhmasahkoposti.dao.ReportedRecipientDAO;
import fi.vm.sade.ryhmasahkoposti.externalinterface.component.OrganizationComponent;
import fi.vm.sade.ryhmasahkoposti.externalinterface.component.PersonComponent;
import fi.vm.sade.ryhmasahkoposti.model.ReportedRecipient;
import fi.vm.sade.ryhmasahkoposti.validation.OidValidator;
import org.springframework.util.StringUtils;

@Service
public class RecipientService {
    private static Logger log = LoggerFactory.getLogger(RecipientService.class);

    @Autowired
    private PersonComponent personComponent;
    @Autowired
    private OrganizationComponent organizationComponent;
    @Autowired
    private ReportedRecipientDAO recipientDAO;
    @Autowired
    private ApplicationContext applicationContext;

    // TODO: request the recipient entities as batch from Henkilöpalvelu and
    // Organisaatiopalvelu
    // e.g. give a list of oids and receive a list of Henkilö or organization
    // objects
    @Scheduled(cron = "0 0/1 * * * ?")
    public void retrieveMissingInformation() {
        log.debug("Started retrieving missing recipient information");
        RecipientService self = applicationContext.getBean(RecipientService.class);
        try {
            List<Long> recipientIds = recipientDAO.findRecipientIdsWithIncompleteInformation();
            for (Long recipientId : recipientIds) {
                self.updateRecipientInformation(recipientId);
            }
        } catch (Exception e) {
            log.error("Information retrieval failed", e);
        }
    }

    @Transactional
    public void updateRecipientInformation(Long recipientId) {
        ReportedRecipient recipient = recipientDAO.findByRecipientID(recipientId);
        if(recipient != null) {
            if (OidValidator.isHenkiloOID(recipient.getRecipientOid())) {
                updatePerson(recipient);
            } else if (OidValidator.isOrganisaatioOID(recipient.getRecipientOid())) {
                updateOrganization(recipient);
            } else {
                updateRecipientWithoutOid(recipient);
            }
        }
    }

    private void updateOrganization(ReportedRecipient recipient) {
        log.debug("Updating organization recipient with oid {} ", recipient.getRecipientOid());
        try {
            OrganisaatioRDTO organisaatio = organizationComponent.getOrganization(recipient.getRecipientOid());
            String nameOfOrganisation = organizationComponent.getNameOfOrganisation(organisaatio);
            recipient.setSearchName(nameOfOrganisation);
            recipient.setDetailsRetrieved(true);
            updateRecipient(recipient);
        } catch (Exception e) {
            log.warn("Updating organization recipient with oid {} failed: {}", recipient.getRecipientOid(), e.getMessage());
            log.debug("Updating organization recipient with oid {} failed", recipient.getRecipientOid(), e);
        }
    }

    private void updatePerson(ReportedRecipient recipient) {
        log.debug("Updating person recipient with oid {} ", recipient.getRecipientOid());
        try {
            HenkiloDto henkilo = personComponent.getPerson(recipient.getRecipientOid());
            recipient.setSearchName(henkilo.getSukunimi() + "," + henkilo.getEtunimet());
            recipient.setDetailsRetrieved(true);
            updateRecipient(recipient);
        } catch (Exception e) {
            log.warn("Updating person recipient with oid {} failed: {}", recipient.getRecipientOid(), e.getMessage());
            log.debug("Updating person recipient with oid {} failed", recipient.getRecipientOid(), e);
        }
    }

    private void updateRecipientWithoutOid(ReportedRecipient recipient) {
        try {
            if(!StringUtils.isEmpty(recipient.getRecipientOid())) {
                log.warn("Unrecognizable OID: {}, in recipient: {}. Can not retrieve details.", recipient.getRecipientOid(), recipient.getRecipientEmail());
            }
            recipient.setDetailsRetrieved(true);
            updateRecipient(recipient);
        } catch (Exception e) {
            log.warn("Updating recipient without oid failed. Recipient id is {}: {}", recipient.getId(), e.getMessage());
            log.debug("Updating recipient without oid failed. Recipient id is {}", recipient.getId(), e);
        }

    }

    private void updateRecipient(ReportedRecipient recipient) {
        try {
            log.debug("Updating recipient to db: {}", recipient);
            recipientDAO.update(recipient);
        } catch (Exception e) {
            log.error("Reciepient update failed", e);
        }
    }

}
