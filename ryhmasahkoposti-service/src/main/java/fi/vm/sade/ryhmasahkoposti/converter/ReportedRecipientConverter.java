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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import com.google.common.base.Optional;

import fi.vm.sade.ryhmasahkoposti.api.dto.EmailRecipient;
import fi.vm.sade.ryhmasahkoposti.model.ReportedMessage;
import fi.vm.sade.ryhmasahkoposti.model.ReportedRecipient;

@Component
public class ReportedRecipientConverter {

    public ReportedRecipient convert(EmailRecipient emailRecipient) {
        ReportedRecipient reportedRecipient = new ReportedRecipient();

        reportedRecipient.setRecipientOid(emailRecipient.getOid());
        reportedRecipient.setRecipientOidType(emailRecipient.getOidType());
        reportedRecipient.setRecipientEmail(emailRecipient.getEmail());
        reportedRecipient.setLanguageCode(emailRecipient.getLanguageCode());
        reportedRecipient.setSearchName(Optional.fromNullable(emailRecipient.getName()).or(""));
        reportedRecipient.setDetailsRetrieved(false);
        reportedRecipient.setSendingStarted(null);
        reportedRecipient.setSendingEnded(null);
        reportedRecipient.setSendingSuccessful(null);
        reportedRecipient.setFailureReason(null);
        reportedRecipient.setTimestamp(new Date());

        return reportedRecipient;
    }

    public Set<ReportedRecipient> convert(ReportedMessage reportedMessage, List<EmailRecipient> emailRecipients) {
        Set<ReportedRecipient> reportedRecipients = new HashSet<>();

        for (EmailRecipient emailRecipient : emailRecipients) {
            reportedRecipients.add(getReportedRecipient(reportedMessage, emailRecipient));
        }
        return reportedRecipients;
    }

    private ReportedRecipient getReportedRecipient(ReportedMessage reportedMessage, EmailRecipient emailRecipient) {
        ReportedRecipient reportedRecipient = convert(emailRecipient);
        reportedRecipient.setReportedMessage(reportedMessage);
        reportedRecipient.setLetterHash(createLetterHash(reportedMessage.getTimestamp(), emailRecipient.getEmail()));
        return reportedRecipient;
    }

    private String createLetterHash(Date timestamp, String email) {
        return DigestUtils.md5Hex(timestamp.toString() + ":" +  email);
    }

    public ReportedRecipient convert(ReportedMessage reportedMessage, EmailRecipient emailRecipient) {
        return getReportedRecipient(reportedMessage, emailRecipient);
    }
}
