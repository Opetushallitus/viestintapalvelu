package fi.vm.sade.ryhmasahkoposti.converter;

import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.ryhmasahkoposti.api.constants.GroupEmailConstants;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailMessage;
import fi.vm.sade.ryhmasahkoposti.externalinterface.route.OmattiedotRoute;
import fi.vm.sade.ryhmasahkoposti.model.ReportedMessage;

@Component
public class ReportedMessageConverter {
    private static OmattiedotRoute omattiedotRoute;
    
    @Autowired
    public ReportedMessageConverter(OmattiedotRoute omattiedotRoute) {
        ReportedMessageConverter.omattiedotRoute = omattiedotRoute;
    }
    
	public static ReportedMessage convert(EmailMessage emailMessage) throws IOException {
		ReportedMessage reportedMessage = new ReportedMessage();
		
		reportedMessage.setSubject(emailMessage.getSubject());
		reportedMessage.setProcess(emailMessage.getCallingProcess());
		reportedMessage.setSenderOid(getCurrentUserOid());
		reportedMessage.setSenderEmail(emailMessage.getFrom());
		reportedMessage.setReplyToEmail(emailMessage.getReplyTo());
		reportedMessage.setSubject(emailMessage.getSubject());
		reportedMessage.setMessage(emailMessage.getBody());
		if (emailMessage.isHtml()) {
			reportedMessage.setHtmlMessage(GroupEmailConstants.HTML_MESSAGE);
		} else {
			reportedMessage.setHtmlMessage(GroupEmailConstants.NOT_HTML_MESSAGE);
		}
		reportedMessage.setCharacterSet(emailMessage.getCharset());
		reportedMessage.setSendingStarted(new Date());
		reportedMessage.setTimestamp(new Date());
		
		return reportedMessage;
	}
	
	private static String getCurrentUserOid() {
	    Henkilo henkilo = omattiedotRoute.getCurrenUser();
	    return henkilo.getOidHenkilo();
	}
}
