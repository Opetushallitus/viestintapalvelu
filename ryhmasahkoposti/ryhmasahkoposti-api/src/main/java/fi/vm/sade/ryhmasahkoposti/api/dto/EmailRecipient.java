package fi.vm.sade.ryhmasahkoposti.api.dto;

import java.util.List;

/**
 *
 * @author migar1
 */
public class EmailRecipient {
    private String oid = "";
    private String oidType = "";
    private String email = "";
    private String languageCode = "FI";
    
    /**
     * List of recipient replacements
     */
    private List<ReportedRecipientReplacementDTO> recipientReplacements;
    
    public void setOid(String oid) {
	this.oid = oid;
    }

    public void setOidType(String oidType) {
	this.oidType = oidType;
    }

    public void setLanguageCode(String languageCode) {
	this.languageCode = languageCode;
    }

    public EmailRecipient() {
	super();
    }

    public EmailRecipient(String oid) {
	super();
	this.oid = oid;
    }

    public EmailRecipient(String oid, String email) {
	super();
	this.oid = oid;
	this.email = email;
    }

    public EmailRecipient(String oid, String oidType, String email, String languageCode) {
	super();
	this.oid = oid;
	this.oidType = oidType;
	this.email = email;
	this.languageCode = languageCode;
    }
    
        public EmailRecipient(String oid, String oidType, String email,
	    String languageCode,
	    List<ReportedRecipientReplacementDTO> recipientReplacements) {
	super();
	this.oid = oid;
	this.oidType = oidType;
	this.email = email;
	this.languageCode = languageCode;
	this.recipientReplacements = recipientReplacements;
    }

    public String getOid() {
	return oid;
    }

    public String getOidType() {
	return oidType;
    }

    public String getLanguageCode() {
	return languageCode;
    }

    public String getEmail() {
	return email;
    }

    public void setEmail(String email) {
	this.email = email;
    }

    
    /**
     * @return the recipientReplacements
     */
    public List<ReportedRecipientReplacementDTO> getRecipientReplacements() {
        return recipientReplacements;
    }

    /**
     * @param recipientReplacements the recipientReplacements to set
     */
    public void setRecipientReplacements(
    	List<ReportedRecipientReplacementDTO> recipientReplacements) {
        this.recipientReplacements = recipientReplacements;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
	return "EmailRecipient [oid=" + oid + ", oidType=" + oidType
		+ ", email=" + email + ", languageCode=" + languageCode
		+ ", recipientReplacements=" + recipientReplacements + "]";
    }
}
