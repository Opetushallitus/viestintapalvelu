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

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import fi.vm.sade.viestintapalvelu.dao.DAOUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.OrderSpecifier;

import fi.vm.sade.generic.dao.AbstractJpaDAOImpl;
import fi.vm.sade.dto.PagingAndSortingDTO;
import fi.vm.sade.ryhmasahkoposti.api.dto.query.ReportedMessageQueryDTO;
import fi.vm.sade.ryhmasahkoposti.api.dto.query.ReportedRecipientQueryDTO;
import fi.vm.sade.ryhmasahkoposti.dao.ReportedMessageDAO;
import fi.vm.sade.ryhmasahkoposti.model.QReportedMessage;
import fi.vm.sade.ryhmasahkoposti.model.QReportedRecipient;
import fi.vm.sade.ryhmasahkoposti.model.ReportedMessage;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import static com.mysema.query.types.expr.BooleanExpression.anyOf;

@Repository
public class ReportedMessageDAOImpl extends AbstractJpaDAOImpl<ReportedMessage, Long> implements ReportedMessageDAO {

    private QReportedMessage reportedMessage = QReportedMessage.reportedMessage;
    private QReportedRecipient reportedRecipient = QReportedRecipient.reportedRecipient;

    @Value("${ryhmasahkoposti.reportedmessage.fetch.maxage.days:365}")
    private int reportedMessageFetchMaxAgeDays;

    @Override
    public List<ReportedMessage> findByOrganizationOids(List<String> organizationOids, PagingAndSortingDTO pagingAndSorting) {
        if (organizationOids != null && organizationOids.isEmpty()) {
            return new ArrayList<>();
        }

        Date dateLimit = DateTime.now().minusDays(reportedMessageFetchMaxAgeDays).toDate();

        JPAQuery findAllReportedMessagesQuery = from(reportedMessage).where(reportedMessage.timestamp.gt(dateLimit)).orderBy(orderBy(pagingAndSorting));
        if (organizationOids != null) {
            findAllReportedMessagesQuery = findAllReportedMessagesQuery.where(anyOf(DAOUtil.splittedInExpression(organizationOids,
                    reportedMessage.senderOrganizationOid)));
        }

        if (pagingAndSorting.getNumberOfRows() != 0) {
            findAllReportedMessagesQuery.limit(pagingAndSorting.getNumberOfRows()).offset(pagingAndSorting.getFromIndex());
        }

        return findAllReportedMessagesQuery.list(reportedMessage);
    }

    @Override
    public List<ReportedMessage> findBySenderOid(String senderOid, PagingAndSortingDTO pagingAndSortingDTO) {
        JPAQuery query = new JPAQuery(getEntityManager());
        return query.from(reportedMessage).where(reportedMessage.senderOid.eq(senderOid)).orderBy(orderBy(pagingAndSortingDTO)).list(reportedMessage);
    }

    @Override
    public Optional<ReportedMessage> findByLetter(Long letterID) {
        JPAQuery query = new JPAQuery(getEntityManager());
        List<ReportedMessage> messageList = query.from(reportedMessage).where(reportedMessage.letterId.eq(letterID)).list(reportedMessage);
        return messageList.size() == 0 ? Optional.<ReportedMessage>empty() : Optional.of(messageList.get(0));
    }

    @Override
    public List<ReportedMessage> findBySenderOidAndProcess(String senderOid, String process, PagingAndSortingDTO pagingAndSorting) {
        JPAQuery query = new JPAQuery(getEntityManager());
        return query.from(reportedMessage).where(reportedMessage.senderOid.eq(senderOid).and(reportedMessage.process.equalsIgnoreCase(process)))
                .orderBy(orderBy(pagingAndSorting)).list(reportedMessage);
    }

    @Override
    public List<ReportedMessage> findBySearchCriteria(ReportedMessageQueryDTO query, PagingAndSortingDTO pagingAndSorting) {
        String nativeSqlQuery = createFindBySearchCriteriaQuery(query);
        nativeSqlQuery += " LIMIT " + pagingAndSorting.getNumberOfRows() +
                          " OFFSET " + pagingAndSorting.getFromIndex();
        Query q = getEntityManager().createNativeQuery(nativeSqlQuery, ReportedMessage.class);

        return q.getResultList();
    }

    @Override
    public Long findNumberOfReportedMessages(List<String> organizationOids) {
        if (organizationOids != null && organizationOids.isEmpty()) {
            return 0L;
        }

        String dateString = getDateLimitString();

        Map<String, Object> params = new HashMap<>();
        String findNumberOfReportedMessages = "SELECT COUNT(*) FROM ReportedMessage a WHERE a.timestamp > '" + dateString + "'";
        if (organizationOids != null) {
            findNumberOfReportedMessages += " AND " + DAOUtil.splittedInExpression(organizationOids, "a.senderOrganizationOid", params, "_oids");
        }
        return DAOUtil.querySingleLong(getEntityManager(), params, findNumberOfReportedMessages);
    }

    @Override
    public Long findNumberOfUserMessages(String userOid) {
        JPAQuery query = new JPAQuery(getEntityManager());
        return query.from(reportedMessage).where(reportedMessage.senderOid.eq(userOid)).count();
    }

    @Override
    public Long findNumberOfReportedMessage(ReportedMessageQueryDTO query) {
        String nativeSqlQuery = "SELECT COUNT(*) FROM (" + createFindBySearchCriteriaQuery(query) + ") AS c";

        Query q = getEntityManager().createNativeQuery(nativeSqlQuery);

        return Long.valueOf(q.getSingleResult().toString());
    }

    protected JPAQuery from(EntityPath<?>... o) {
        return new JPAQuery(getEntityManager()).from(o);
    }

    protected OrderSpecifier<?> orderBy(PagingAndSortingDTO pagingAndSorting) {
        if (pagingAndSorting.getSortedBy() == null || pagingAndSorting.getSortedBy().isEmpty()) {
            return reportedMessage.sendingStarted.asc();
        }

        if (pagingAndSorting.getSortedBy().equalsIgnoreCase("sendingStarted")) {
            if (pagingAndSorting.getSortOrder().equalsIgnoreCase("asc")) {
                return reportedMessage.sendingStarted.asc();
            }

            return reportedMessage.sendingStarted.desc();
        }

        if (pagingAndSorting.getSortedBy().equalsIgnoreCase("senderName")) {
            if (pagingAndSorting.getSortOrder().equalsIgnoreCase("asc")) {
                return reportedMessage.senderName.asc();
            }

            return reportedMessage.sendingStarted.desc();
        }

        if (pagingAndSorting.getSortedBy().equalsIgnoreCase("process") || pagingAndSorting.getSortedBy().equalsIgnoreCase("subject")) {
            if (pagingAndSorting.getSortOrder().equalsIgnoreCase("asc")) {
                return reportedMessage.process.asc();
            }

            return reportedMessage.process.desc();
        }

        return reportedMessage.sendingStarted.asc();
    }

    private String getDateLimitString() {
        DateTime dateLimit = DateTime.now().minusDays(reportedMessageFetchMaxAgeDays);
        DateTimeFormatter dft = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        return dft.print(dateLimit);
    }

    private String safelyQuote(String s) {
        return "'" + s.replace("'", "") + "'";
    }

    private String createReportedMessagesSenderOrganizationOidWhereItem(List<String> organizationOids, String organizationOid) {
        if (organizationOids != null) {
            if (organizationOids.isEmpty()) {
                return "true = false";
            } else {
                String quotedOrganizationOidsList = organizationOids.stream().
                        map(oid -> safelyQuote(oid)).
                        collect(Collectors.joining(", "));
                return "m.lahettajan_organisaatio_oid in (" + quotedOrganizationOidsList + ")";
            }
        } else if (organizationOid != null) {
            return "m.lahettajan_organisaatio_oid = " + safelyQuote(organizationOid);
        }
        return "";
    }

    private String createWhereItemIfExists(String clausePrefix, String value) {
        if (value != null) {
            return clausePrefix + " " + safelyQuote(value);
        } else {
            return "";
        }
    }

    private String joinWhereItemsWithAnd(List<String> items) {
        return items.stream().filter(t -> !t.isEmpty()).collect(Collectors.joining(" AND "));
    }

    String createFindBySearchCriteriaQuery(ReportedMessageQueryDTO query) {
        ReportedRecipientQueryDTO reportedRecipientQuery = query.getReportedRecipientQueryDTO();

        String selectClause = "SELECT DISTINCT * FROM raportoitavaviesti m";
        String nativeSqlQuery = selectClause;

        String aikaleimaItem = createWhereItemIfExists("m.aikaleima > ", getDateLimitString());
        boolean isThereSearchArgument = query.getSearchArgument() != null && !query.getSearchArgument().isEmpty();
        String reportedMessagesWhereClause = joinWhereItemsWithAnd(Arrays.asList(
                createReportedMessagesSenderOrganizationOidWhereItem(query.getOrganizationOids(), query.getOrganizationOid()),
                aikaleimaItem
        ));
        String reportedRecipientsSubqueryWhereClause = joinWhereItemsWithAnd(Arrays.asList(
                createWhereItemIfExists("r.vastaanottajan_oid = ", reportedRecipientQuery.getRecipientOid()),
                createWhereItemIfExists("r.vastaanottajan_sahkopostiosoite = ", reportedRecipientQuery.getRecipientEmail())
        ));

        if (isThereSearchArgument) {
            String fullTextSearchReportedItem = "to_tsvector('simple', m.viesti || ' ' || m.prosessi || ' ' || m.aihe) @@ " +
                                                safelyQuote(query.getSearchArgument().toLowerCase());
            String fullTextSearchRecipientsItem = "to_tsvector('simple', r.hakunimi) @@ " +
                                                  safelyQuote(query.getSearchArgument().toLowerCase());
            String reportRecipientsSubqueryForFullTextSearchRecipeints =
                    "SELECT 1 FROM raportoitavavastaanottaja r " +
                            " WHERE m.id = r.lahetettyviesti_id AND " + fullTextSearchRecipientsItem;
            if (reportedRecipientsSubqueryWhereClause.isEmpty()) {
                nativeSqlQuery += " WHERE " + fullTextSearchReportedItem +
                        " AND " + reportedMessagesWhereClause +
                        " UNION DISTINCT " +
                        selectClause + " WHERE " +
                        "EXISTS (" + reportRecipientsSubqueryForFullTextSearchRecipeints + ") AND " +
                        reportedMessagesWhereClause;
            } else {
                nativeSqlQuery += " WHERE " +
                        "EXISTS (" + reportRecipientsSubqueryForFullTextSearchRecipeints +
                        reportedRecipientsSubqueryWhereClause + ") AND " +
                        reportedMessagesWhereClause;
            }
        } else {
            if (reportedRecipientsSubqueryWhereClause.isEmpty()) {
                nativeSqlQuery += " WHERE " + reportedMessagesWhereClause;
            } else {
                String reportRecipientsSubquery =
                        "SELECT 1 FROM raportoitavavastaanottaja r " +
                                " WHERE m.id = r.lahetettyviesti_id AND " + reportedRecipientsSubqueryWhereClause;
                nativeSqlQuery += " WHERE " +
                        "EXISTS (" + reportRecipientsSubquery + ") AND " + reportedMessagesWhereClause;
            }
        }

        nativeSqlQuery += " ORDER BY lahetysalkoi DESC ";

        return nativeSqlQuery;
    }
}
