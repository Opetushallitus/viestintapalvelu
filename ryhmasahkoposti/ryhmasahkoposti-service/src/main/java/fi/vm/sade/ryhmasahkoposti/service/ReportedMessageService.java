package fi.vm.sade.ryhmasahkoposti.service;

import java.util.List;

import fi.vm.sade.ryhmasahkoposti.api.dto.PagingAndSortingDTO;
import fi.vm.sade.ryhmasahkoposti.api.dto.query.ReportedMessageQueryDTO;
import fi.vm.sade.ryhmasahkoposti.model.ReportedMessage;

public interface ReportedMessageService {  
    /**
     * Hakee raportoitavien ryhmäsähköpostien lukumäärän
     * 
     * @return Raportoitavien ryhmäsähköpostien lukumäärän
     */
    public Long getNumberOfReportedMessages();
    
    /**
     * Hakee käyttäjän ja käyttäjän käyttöoikeusryhmien kaikki raportoitavat viestit
     * 
     * @param pagingAndSorting Lajittelutekijät 
     * @return Lista raportoitavia viestejä {@link ReportedMessage} 
     */ 
    public List<ReportedMessage> getReportedMessages(PagingAndSortingDTO pagingAndSorting);

	/**
	 * Hakee raportoitavan viestin viestin avaimella
	 * 
	 * @param id Raportoitavan viestin avain
	 * @return Raportoitavan viestin tiedot {@link ReportedMessage}
	 */
	public ReportedMessage getReportedMessage(Long id);

	/**
	 * Hakee annettujen parametrien mukaiset ryhmäsähköpostin raportoitavat viestit
	 * 
	 * @param query Hakuparametrit
	 * @return Lists raportoitavia viestejä {@link ReportedMessage}
	 */
	public List<ReportedMessage> getReportedMessages(ReportedMessageQueryDTO query, 
	    PagingAndSortingDTO pagingAndSorting);
	
	/**
	 * Päivittä raportoitavan viestin tietoja tietokantaan
	 * @param reportedMessage Päivittetävä raportoitava viesti
	 */
	public void updateReportedMessage(ReportedMessage reportedMessage);
	
	/**
	 * Tallentaa raportoitvan ryhmäsähköpostin viestin
	 * 
	 * @param reportedMessage Ryhmäsähköpostin raportoitavaviesti
	 * @return Tallennetu raportoitava viesti
	 */
	public ReportedMessage saveReportedMessage(ReportedMessage reportedMessage);
}
