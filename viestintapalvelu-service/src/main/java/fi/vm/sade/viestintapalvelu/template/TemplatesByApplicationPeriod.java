/**
 * Copyright (c) 2014 The Finnish National Board of Education - Opetushallitus
 *
 * This program is free software: Licensed under the EUPL, Version 1.1 or - as
 * soon as they will be approved by the European Commission - subsequent versions
 * of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * European Union Public Licence for more details.
 */
package fi.vm.sade.viestintapalvelu.template;

import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

/**
 * @author risal1
 *
 */
@ApiModel("Hakutunnisteeseen liittyvät kirjepohjat")
public class TemplatesByApplicationPeriod implements Serializable {

    private static final long serialVersionUID = -6852022531900885586L;

    @NotNull
    @ApiModelProperty("Hakutunniste millä kirjepohjat on haettu")
    public final String hakuOid;

    @ApiModelProperty("Viimeisimmät versiot julkaistuista kirjepohjista")
    public final List<TemplateInfo> publishedTemplates;

    @ApiModelProperty("Viimeisimmät versiot luonnostilaisista kirjepohjista")
    public final List<TemplateInfo> draftTemplates;

    @ApiModelProperty("Viimeisimmät suljetut versiot kirjepohjista, sisältää vain suljetut versiot kirjepohjista mistä ei löydy julkaistua- tai luonnostilaista versiota")
    public final List<TemplateInfo> closedTemplates;

    public TemplatesByApplicationPeriod(String hakuOid, List<TemplateInfo> publishedTemplates, List<TemplateInfo> draftTemplates,
            List<TemplateInfo> closedTemplates) {
        this.hakuOid = hakuOid;
        this.publishedTemplates = publishedTemplates;
        this.draftTemplates = draftTemplates;
        this.closedTemplates = closedTemplates;
    }

}
