package fi.vm.sade.ryhmasahkoposti.raportointi.dao.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.expr.BooleanExpression;

import fi.vm.sade.generic.dao.AbstractJpaDAOImpl;
import fi.vm.sade.ryhmasahkoposti.raportointi.dao.RaportoitavaViestiDAO;
import fi.vm.sade.ryhmasahkoposti.raportointi.dto.query.RyhmasahkopostiViestiQueryDTO;
import fi.vm.sade.ryhmasahkoposti.raportointi.model.QRaportoitavaViesti;
import fi.vm.sade.ryhmasahkoposti.raportointi.model.RaportoitavaViesti;

@Repository
public class RaportoitavaViestiDAOImpl extends AbstractJpaDAOImpl<RaportoitavaViesti, Long> 
	implements RaportoitavaViestiDAO {
	
	@Override
	public List<RaportoitavaViesti> findBySearchCriteria(RyhmasahkopostiViestiQueryDTO query) {
		return null;
	}

	@Override
	public List<RaportoitavaViesti> findLahettajanRaportoitavatViestit(List<String> lahettajanOidList) {
		QRaportoitavaViesti raportoitavaViesti = QRaportoitavaViesti.raportoitavaViesti;
		
		BooleanExpression whereExpression = raportoitavaViesti.lahettajanOid.in(lahettajanOidList);
		
		List<RaportoitavaViesti> raportoitavatViestit = 
			from(raportoitavaViesti).where(whereExpression).list(raportoitavaViesti);
			
		return raportoitavatViestit;
	}

	@Override
	public Long findRaportoitavienViestienLukumaara() {
		EntityManager em = getEntityManager();
		
		String findRaportoitavienViestienLukumaara = "SELECT COUNT(*) FROM RaportoitavaViesti a";
		TypedQuery<Long> query = em.createQuery(findRaportoitavienViestienLukumaara, Long.class);
		
		return query.getSingleResult();
	}
	
	protected JPAQuery from(EntityPath<?>... o) {
        return new JPAQuery(getEntityManager()).from(o);
    }
}
