package fi.vm.sade.viestintapalvelu.dao;

import java.util.List;

import fi.vm.sade.generic.dao.JpaDAO;
import fi.vm.sade.viestintapalvelu.dto.PagingAndSortingDTO;
import fi.vm.sade.viestintapalvelu.model.LetterReceivers;

/**
 * Rajapinta kirjelähetysten vastaanottajien tietokantakäsittelyä varten
 * 
 * @author vehei1
 *
 */
public interface LetterReceiversDAO extends JpaDAO<LetterReceivers, Long> {
    /**
     * Hakee kirjelähetysten vastaanottajat ja vastaanottajien kirjeet
     * 
     * @param letterBatchID Kirjelähetyksen tunnus
     * @param pagingAndSorting Sivutus- ja lajittelutiedot
     * @return Lista kirjelähetyksen vastaanottajatietoja
     */
    public List<LetterReceivers> findLetterReceiversByLetterBatchID(Long letterBatchID, PagingAndSortingDTO pagingAndSorting);
}
