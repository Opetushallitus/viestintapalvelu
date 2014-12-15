package fi.vm.sade.viestintapalvelu.dao.impl;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.internal.util.StringHelper;
import org.springframework.stereotype.Repository;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.ConstructorExpression;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.expr.ComparableExpressionBase;
import com.mysema.query.types.path.PathBuilder;
import com.mysema.query.types.path.StringPath;
import com.mysema.query.types.template.BooleanTemplate;

import fi.vm.sade.generic.dao.AbstractJpaDAOImpl;
import fi.vm.sade.viestintapalvelu.common.util.CollectionHelper;
import fi.vm.sade.viestintapalvelu.dao.LetterBatchDAO;
import fi.vm.sade.viestintapalvelu.dao.dto.LetterBatchStatusDto;
import fi.vm.sade.viestintapalvelu.dto.PagingAndSortingDTO;
import fi.vm.sade.viestintapalvelu.dto.letter.LetterBatchReportDTO;
import fi.vm.sade.viestintapalvelu.dto.query.LetterReportQueryDTO;
import fi.vm.sade.viestintapalvelu.model.*;

import static com.mysema.query.types.expr.BooleanExpression.anyOf;

@Repository
public class LetterBatchDAOImpl extends AbstractJpaDAOImpl<LetterBatch, Long> implements LetterBatchDAO {

    public static final int MAX_CHUNK_SIZE_FOR_IN_EXPRESSION = 1000;

    public LetterBatch findLetterBatchByNameOrgTag(String templateName, String language, String organizationOid,
                                                   Optional<String> tag, Optional<String> applicationPeriod) {
        EntityManager em = getEntityManager();

        String findTemplate = "SELECT a FROM LetterBatch a WHERE "
            + "a.templateName=:templateName AND "
            + "a.organizationOid=:organizationOid AND "
            + "a.language=:language  ";
        if (tag.isPresent()) {
            findTemplate += " AND a.tag LIKE :tag ";
        }
        if (applicationPeriod.isPresent()) {
            findTemplate += " AND a.applicationPeriod = :applicationPeriod ";
        }
        findTemplate +=  "ORDER BY a.timestamp DESC";

        TypedQuery<LetterBatch> query = em.createQuery(findTemplate, LetterBatch.class);
        query.setParameter("templateName", templateName);
        query.setParameter("language", language);
        query.setParameter("organizationOid", organizationOid);
        if (tag.isPresent()) {
            query.setParameter("tag", tag.get());
        }
        if (applicationPeriod.isPresent()) {
            query.setParameter("applicationPeriod", applicationPeriod.get());
        }
        query.setFirstResult(0);	// LIMIT 1
        query.setMaxResults(1);  	//

        LetterBatch letterBatch = new LetterBatch();
        try {
            letterBatch = query.getSingleResult();
        } catch (Exception e) {
            letterBatch = null;
        }

        return letterBatch;
    }

    public LetterBatch findLetterBatchByNameOrg(String templateName, String language, String organizationOid) {
        EntityManager em = getEntityManager();

        String findTemplate = "SELECT a FROM LetterBatch a WHERE "
                + "a.templateName=:templateName AND "
                + "a.language=:language AND "
                + "a.organizationOid=:organizationOid "
                + "ORDER BY a.timestamp DESC";

        TypedQuery<LetterBatch> query = em.createQuery(findTemplate, LetterBatch.class);
        query.setParameter("templateName", templateName);
        query.setParameter("language", language);
        query.setParameter("organizationOid", organizationOid);
        query.setFirstResult(0);    // LIMIT 1
        query.setMaxResults(1);     //

        LetterBatch letterBatch = new LetterBatch();
        try {
            letterBatch = query.getSingleResult();
        } catch (Exception e) {
            letterBatch = null;
        }

        return letterBatch;
    }

    @Override
    public List<LetterBatch> findAll(PagingAndSortingDTO pagingAndSorting) {
        QLetterBatch letterBatch = QLetterBatch.letterBatch;

        OrderSpecifier<?> orderBy = orderBy(pagingAndSorting, null);
        JPAQuery findLetterBatches = from(letterBatch).orderBy(orderBy);

        if (pagingAndSorting.getNumberOfRows() != 0) {
            findLetterBatches.limit(pagingAndSorting.getNumberOfRows()).offset(pagingAndSorting.getFromIndex());
        }

        return findLetterBatches.list(letterBatch);
    }

    @Override
    public List<LetterBatch> findLetterBatchesByOrganizationOid(List<String> organizationOIDs,
                PagingAndSortingDTO pagingAndSorting) {
        if (organizationOIDs.isEmpty()) {
            return new ArrayList<LetterBatch>();
        }

        QLetterBatch letterBatch = QLetterBatch.letterBatch;
        OrderSpecifier<?> orderBy = orderBy(pagingAndSorting, null);
        BooleanExpression whereExpression = anyOf(splittedInExpression(organizationOIDs,
                letterBatch.organizationOid));
        JPAQuery findLetterBatches = from(letterBatch).where(whereExpression).orderBy(orderBy);
        
        if (pagingAndSorting.getNumberOfRows() != 0) {
            findLetterBatches.limit(pagingAndSorting.getNumberOfRows()).offset(pagingAndSorting.getFromIndex());
        }
        
        return findLetterBatches.list(letterBatch);
    }

    @Override
    public List<LetterBatchReportDTO> findLetterBatchesBySearchArgument(LetterReportQueryDTO query,
                                                               PagingAndSortingDTO pagingAndSorting) {
        QLetterBatch letterBatch = QLetterBatch.letterBatch;
        QLetterReceivers receiver = QLetterReceivers.letterReceivers;
        QLetterReceiverAddress letterReceiverAddress = QLetterReceiverAddress.letterReceiverAddress;
        JPAQuery q = fromSearchTarget(query.getTarget(), letterBatch, receiver, letterReceiverAddress)
            .where(whereExpressionForSearchCriteria(query, letterBatch, receiver, letterReceiverAddress))
            .orderBy(orderBy(pagingAndSorting, letterReceiverAddress))
            .limit(pagingAndSorting.getNumberOfRows()).offset(pagingAndSorting.getFromIndex());
        if (query.getTarget() == LetterReportQueryDTO.SearchTarget.receiver) {
            return q.list(ConstructorExpression.create(LetterBatchReportDTO.class,
                    letterBatch.id,
                    letterBatch.templateId,
                    letterBatch.templateName,
                    letterBatch.applicationPeriod,
                    letterBatch.fetchTarget,
                    letterBatch.tag,
                    letterBatch.iposti,
                    letterBatch.timestamp,
                    letterBatch.organizationOid,
                    letterBatch.batchStatus,
                    receiver.letterReceiverLetter.id,
                    letterReceiverAddress.lastName.concat(" ").concat(letterReceiverAddress.firstName)
            ));
        }
        return q.list(ConstructorExpression.create(LetterBatchReportDTO.class,
                    letterBatch.id,
                    letterBatch.templateId,
                    letterBatch.templateName,
                    letterBatch.applicationPeriod,
                    letterBatch.fetchTarget,
                    letterBatch.tag,
                    letterBatch.iposti,
                    letterBatch.timestamp,
                    letterBatch.organizationOid,
                    letterBatch.batchStatus
            ));
    }

    @Override
    public Long findNumberOfLetterBatches() {
        EntityManager em = getEntityManager();

        String findNumberOfLetterBatches =
                "SELECT COUNT(*) FROM LetterBatch";
        TypedQuery<Long> query = em.createQuery(findNumberOfLetterBatches, Long.class);
        return query.getSingleResult();
    }

    @Override
    public Long findNumberOfLetterBatches(List<String> oids) {
        if (oids.isEmpty()) {
            return 0l;
        }
    	EntityManager em = getEntityManager();

        Map<String,Object> params = new HashMap<String, Object>();
    	String findNumberOfLetterBatches = 
    		"SELECT COUNT(*) FROM LetterBatch a WHERE "
            + splittedInExpression(oids, "a.organizationOid", params, "_oids");
    	TypedQuery<Long> query = em.createQuery(findNumberOfLetterBatches, Long.class);
    	for (Map.Entry<String,Object> kv : params.entrySet()) {
            query.setParameter(kv.getKey(), kv.getValue());
        }

    	return query.getSingleResult();
    }

    @Override
    public Long findNumberOfLetterBatchesBySearchArgument(LetterReportQueryDTO letterReportQuery, Long maxCount) {
    	QLetterBatch letterBatch = QLetterBatch.letterBatch;
        QLetterReceivers receiver = QLetterReceivers.letterReceivers;
        QLetterReceiverAddress letterReceiverAddress = QLetterReceiverAddress.letterReceiverAddress;
        JPAQuery q = fromSearchTarget(letterReportQuery.getTarget(), letterBatch, receiver, letterReceiverAddress)
                    .where(whereExpressionForSearchCriteria(letterReportQuery, letterBatch, receiver, letterReceiverAddress));
        if (maxCount != null) {
            // can not get count effectively (runtime of receiver join explodes), just tell if we got more than maxCount
            return (long) q.limit(maxCount + 1l).orderBy(letterBatch.timestamp.desc())
                    .list(letterReportQuery.getTarget() == LetterReportQueryDTO.SearchTarget.receiver ?
                            new Expression<?>[]{receiver.letterReceiverLetter.id, letterBatch.timestamp}
                          : new Expression<?>[]{letterBatch.timestamp}).size();
        }
        return q.count();
    }

    protected JPAQuery fromSearchTarget(LetterReportQueryDTO.SearchTarget target,
                                        QLetterBatch letterBatch,
                                        QLetterReceivers receiver,
                                        QLetterReceiverAddress letterReceiverAddress) {
        switch (target) {
        case batch:
            return from(letterBatch)
                    .innerJoin(letterBatch.letterReceivers, receiver)
                    .leftJoin(receiver.letterReceiverAddress, letterReceiverAddress);
        case receiver:
            return from(letterBatch).distinct()
                    .leftJoin(letterBatch.letterReceivers, receiver)
                    .leftJoin(receiver.letterReceiverAddress, letterReceiverAddress);
        default: throw new IllegalArgumentException("Unknown SearchTarget" + target);
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public LetterBatchStatusDto getLetterBatchStatus(long letterBatchId) {
        List<LetterBatchStatusDto> results = (List<LetterBatchStatusDto>) getEntityManager()
                .createNamedQuery("letterBatchStatus")
                    .setParameter("batchId", letterBatchId)
                .setMaxResults(1)
                .getResultList();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    @Override
    public List<Long> findUnprocessedLetterReceiverIdsByBatch(long batchId) {
        return getEntityManager().createQuery(
                    "select lr.id from LetterReceivers lr"
                    + " inner join lr.letterBatch batch with batch.id = :batchId"
                    + " inner join lr.letterReceiverLetter letter"
                    + " where letter.letter = null"
                    + " order by lr.id", Long.class)
            .setParameter("batchId", batchId).getResultList();
    }
    
    @Override
    public List<Long> findAllLetterReceiverIdsByBatch(long batchId) {
        return getEntityManager().createQuery(
                "select lr.id from LetterReceivers lr"
                        + " inner join lr.letterBatch batch with batch.id = :batchId"
                        + " order by lr.id", Long.class)
            .setParameter("batchId", batchId).getResultList();
    }
    
    @Override
    public List<Long> findUnfinishedLetterBatches() {
        return getEntityManager().createQuery("SELECT lb.id FROM LetterBatch lb"
                + " WHERE lb.batchStatus != 'ready' AND lb.batchStatus != 'error'"
                + " ORDER BY lb.timestamp ASC", Long.class).getResultList();
    }



    protected JPAQuery from(EntityPath<?>... o) {
        return new JPAQuery(getEntityManager()).from(o);
    }

    protected OrderSpecifier<?> orderBy(PagingAndSortingDTO pagingAndSorting, QLetterReceiverAddress receiverAddress) {
        PathBuilder<LetterBatch> pb = new PathBuilder<LetterBatch>(LetterBatch.class, "letterBatch");
        
        if (pagingAndSorting.getSortedBy() != null && !pagingAndSorting.getSortedBy().isEmpty()) {
            if (receiverAddress != null && "receiverName".equals(pagingAndSorting.getSortedBy())) {
                return direction(receiverAddress.lastName.concat(" ").concat(receiverAddress.firstName),
                        pagingAndSorting.getSortOrder());
            }
            return direction(pb.getString(pagingAndSorting.getSortedBy()),
                    pagingAndSorting.getSortOrder());
        }
        return pb.getString("timestamp").asc();
    }

    protected OrderSpecifier<?> direction(ComparableExpressionBase<?> exp, String sortOrder) {
        if (sortOrder == null || sortOrder.isEmpty()) {
            return exp.asc();
        }
        if (sortOrder.equalsIgnoreCase("asc")) {
            return exp.asc();
        }
        return exp.desc();
    }

    protected BooleanBuilder whereExpressionForSearchCriteria(LetterReportQueryDTO query,
                                                              final QLetterBatch letterBatch,
                                                              final QLetterReceivers letterReceivers,
                                                              final QLetterReceiverAddress letterReceiverAddress) {
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (query.getBeginDate() != null) {
            // gte missing from QueryDSL
            booleanBuilder.andAnyOf(
                    letterBatch.timestamp.after(query.getBeginDate().toDateTimeAtStartOfDay().toDate()),
                    letterBatch.timestamp.eq(query.getBeginDate().toDateTimeAtStartOfDay().toDate())
            );
        }
        if (query.getEndDate() != null) {
            // as well as lte
            booleanBuilder.andAnyOf(
                    letterBatch.timestamp.before(query.getEndDate().toDateTimeAtStartOfDay().toDate()),
                    letterBatch.timestamp.eq(query.getEndDate().toDateTimeAtStartOfDay().toDate())
            );
        }

        if (query.getOrganizationOids() != null) {
            if (query.getOrganizationOids().isEmpty()) {
                // no organisaatios should yield no results, thus:
                booleanBuilder.and(BooleanTemplate.TRUE.eq(BooleanTemplate.FALSE));
            } else {
                booleanBuilder.andAnyOf(splittedInExpression(query.getOrganizationOids(),
                        letterBatch.organizationOid));
            }
        }

        if (query.getApplicationPeriod() != null && !query.getApplicationPeriod().isEmpty()) {
            booleanBuilder.and(letterBatch.applicationPeriod.eq(query.getApplicationPeriod()));
        }

        int subQueryNum = 0;
        if (query.getLetterBatchSearchArgument() != null && !query.getLetterBatchSearchArgument().isEmpty()) {
            for (String word : words(query.getLetterBatchSearchArgument())) {
                booleanBuilder.andAnyOf(
                    anyOfLetterBatchRelatedConditions(letterBatch, word),
                    letterBatchReplacementsContain(letterBatch, word, ++subQueryNum)
                );
            }
        }
        if (query.getReceiverSearchArgument() != null && !query.getReceiverSearchArgument().isEmpty()) {
            for (String word : words(query.getReceiverSearchArgument())) {
                booleanBuilder.andAnyOf(
                    anyOfLetterBatchRelatedConditions(letterBatch, word),
                    anyOfReceiverAddressFieldsContains(letterReceiverAddress, word),
                    letterBatchReplacementsContain(letterBatch, word, ++subQueryNum),
                    receiverReplacementsContain(letterReceivers, word, ++subQueryNum)
                );
            }
        }

        return booleanBuilder;
    }

    private BooleanExpression anyOfReceiverAddressFieldsContains(QLetterReceiverAddress letterReceiverAddress, String word) {
        return anyOf(
                letterReceiverAddress.firstName.trim().containsIgnoreCase(word),
                letterReceiverAddress.lastName.trim().containsIgnoreCase(word),
                letterReceiverAddress.addressline.trim().containsIgnoreCase(word),
                letterReceiverAddress.addressline2.trim().containsIgnoreCase(word),
                letterReceiverAddress.addressline3.trim().containsIgnoreCase(word),
                letterReceiverAddress.city.trim().containsIgnoreCase(word),
                letterReceiverAddress.postalCode.trim().contains(word)
        );
    }

    private BooleanExpression letterBatchReplacementsContain(QLetterBatch letterBatch, String word, int subQueryNumber) {
        QLetterBatch subQueryLetterBatch = new QLetterBatch("sqLetterBatch_"+subQueryNumber);
        QLetterReplacement replacement = new QLetterReplacement("sqLetterReplacement_"+subQueryNumber);
        return new JPASubQuery().from(subQueryLetterBatch)
                .innerJoin(subQueryLetterBatch.letterReplacements, replacement)
                .where(subQueryLetterBatch.id.eq(letterBatch.id)
                .andAnyOf(
                        replacement.defaultValue.containsIgnoreCase(word),
                        replacement.jsonValue.containsIgnoreCase(word)
                )
                ).exists();
    }

    private BooleanExpression receiverReplacementsContain(QLetterReceivers letterReceivers, String word, int subQueryNumber) {
        QLetterReceivers subQueryLetterReceiver = new QLetterReceivers("sqLetterReceiver_"+subQueryNumber);
        QLetterReceiverReplacement replacement = new QLetterReceiverReplacement("sqLetterReceiverReplacement_"+subQueryNumber);
        return new JPASubQuery().from(subQueryLetterReceiver)
                .innerJoin(subQueryLetterReceiver.letterReceiverReplacement, replacement)
                .where(subQueryLetterReceiver.id.eq(letterReceivers.id)
                        .andAnyOf(
                                replacement.defaultValue.containsIgnoreCase(word),
                                replacement.jsonValue.containsIgnoreCase(word)
                        )
                ).exists();
    }

    private BooleanExpression anyOfLetterBatchRelatedConditions(QLetterBatch letterBatch, String word) {
        return letterBatch.templateName.containsIgnoreCase(word)
                .or(
                    letterBatch.fetchTarget.containsIgnoreCase(word)
                    .or(
                            letterBatch.applicationPeriod.containsIgnoreCase(word)
                    )
                );
    }

    private String[] words(String searchArgument) {
        return searchArgument.trim().split("\\s+");
    }

    private BooleanExpression[] splittedInExpression(List<String> values, final StringPath column) {
        List<List<String>> oidChunks = CollectionHelper.split(values, MAX_CHUNK_SIZE_FOR_IN_EXPRESSION);
        Collection<BooleanExpression> inExcepssionsCollection = Collections2.transform(oidChunks,
                new Function<List<String>, BooleanExpression>() {
                    public BooleanExpression apply(@Nullable List<String> oidsChunk) {
                        return column.in(oidsChunk);
                    }
                });
        return inExcepssionsCollection.toArray(new BooleanExpression[
                inExcepssionsCollection.size()]);
    }

    private String splittedInExpression(List<String> values, final String hqlColumn,
                                        final Map<String, Object> params,
                                        final String valPrefix) {
        final List<List<String>> oidChunks = CollectionHelper.split(values, MAX_CHUNK_SIZE_FOR_IN_EXPRESSION);
        final AtomicInteger n = new AtomicInteger(0);
        Collection<String> inExcepssionsCollection = Collections2.transform(oidChunks,
                new Function<List<String>, String>() {
                    public String apply(@Nullable List<String> oidsChunk) {
                        int pNum = n.incrementAndGet();
                        String paramName = valPrefix+"_"+pNum;
                        params.put(paramName, oidsChunk);
                        return hqlColumn+" in (:"+paramName+")";
                    }
                });
        return StringHelper.join(" OR ", inExcepssionsCollection.toArray(new String[inExcepssionsCollection.size()]));
    }
}
