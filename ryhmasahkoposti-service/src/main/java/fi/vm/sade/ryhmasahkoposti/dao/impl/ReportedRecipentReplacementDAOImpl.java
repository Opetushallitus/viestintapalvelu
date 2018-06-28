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
package fi.vm.sade.ryhmasahkoposti.dao.impl;

import java.util.List;

import org.springframework.stereotype.Repository;

import fi.vm.sade.generic.dao.AbstractJpaDAOImpl;
import fi.vm.sade.ryhmasahkoposti.dao.ReportedRecipientReplacementDAO;
import fi.vm.sade.ryhmasahkoposti.model.ReportedRecipient;
import fi.vm.sade.ryhmasahkoposti.model.ReportedRecipientReplacement;

@Repository
public class ReportedRecipentReplacementDAOImpl extends AbstractJpaDAOImpl<ReportedRecipientReplacement, Long> implements ReportedRecipientReplacementDAO {

    /**
     * Find reported recipient replacements
     * 
     * @param reportedRecipient
     * @return List of reported recipient replacements
     */
    public List<ReportedRecipientReplacement> findReportedRecipientReplacements(ReportedRecipient reportedRecipient) {
        return findBy("reportedRecipient", reportedRecipient);
    }

    @Override
    public
    ReportedRecipientReplacement insertAndFlush(ReportedRecipientReplacement reportedRecipientReplacement) {
        getEntityManager().persist(reportedRecipientReplacement);
        getEntityManager().flush();
        return reportedRecipientReplacement;
    }

    @Override
    public ReportedRecipientReplacement insert(ReportedRecipientReplacement reportedRecipientReplacement) {
        getEntityManager().persist(reportedRecipientReplacement);
        return reportedRecipientReplacement;
    }
}
