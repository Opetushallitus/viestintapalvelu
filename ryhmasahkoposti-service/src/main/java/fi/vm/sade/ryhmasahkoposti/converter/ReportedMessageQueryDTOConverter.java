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
package fi.vm.sade.ryhmasahkoposti.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.generic.common.HetuUtils;
import fi.vm.sade.ryhmasahkoposti.api.dto.query.ReportedMessageQueryDTO;
import fi.vm.sade.ryhmasahkoposti.api.dto.query.ReportedRecipientQueryDTO;
import fi.vm.sade.ryhmasahkoposti.externalinterface.component.CurrentUserComponent;
import fi.vm.sade.ryhmasahkoposti.validation.EmailAddressValidator;
import fi.vm.sade.ryhmasahkoposti.validation.OidValidator;

@Component
public class ReportedMessageQueryDTOConverter {
    private CurrentUserComponent currentUserComponent;

    @Autowired
    public ReportedMessageQueryDTOConverter(CurrentUserComponent currentUserComponent) {
        this.currentUserComponent = currentUserComponent;
    }

    public ReportedMessageQueryDTO convert() {
        ReportedMessageQueryDTO reportedMessageQueryDTO = new ReportedMessageQueryDTO();
        getSenderOidList();
        return reportedMessageQueryDTO;
    }

    public ReportedMessageQueryDTO convert(String organizationOid, String searchArgument) {
        ReportedMessageQueryDTO reportedMessageQueryDTO = new ReportedMessageQueryDTO();
        ReportedRecipientQueryDTO reportedRecipientQueryDTO = new ReportedRecipientQueryDTO();

        reportedMessageQueryDTO.setOrganizationOid(organizationOid);

        if (HetuUtils.isHetuValid(searchArgument)) {
            reportedRecipientQueryDTO.setRecipientSocialSecurityID(searchArgument.trim());
            reportedMessageQueryDTO.setReportedRecipientQueryDTO(reportedRecipientQueryDTO);

            return reportedMessageQueryDTO;
        }

        if (OidValidator.isOID(searchArgument)) {
            reportedRecipientQueryDTO.setRecipientOid(searchArgument.trim());
            reportedMessageQueryDTO.setReportedRecipientQueryDTO(reportedRecipientQueryDTO);

            return reportedMessageQueryDTO;
        }

        if (EmailAddressValidator.validate(searchArgument)) {
            reportedRecipientQueryDTO.setRecipientEmail(searchArgument.trim());
            reportedMessageQueryDTO.setReportedRecipientQueryDTO(reportedRecipientQueryDTO);

            return reportedMessageQueryDTO;
        }

        reportedRecipientQueryDTO.setRecipientName(searchArgument);
        reportedMessageQueryDTO.setReportedRecipientQueryDTO(reportedRecipientQueryDTO);
        reportedMessageQueryDTO.setSearchArgument(searchArgument);

        return reportedMessageQueryDTO;
    }
    private boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
    }
    private List<String> getSenderOidList() {
        List<String> senderOidList = new ArrayList<String>();
        if(isAuthenticated()) {
            Henkilo henkilo = currentUserComponent.getCurrentUser();
            senderOidList.add(henkilo.getOidHenkilo());
        }
        return senderOidList;
    }
}
