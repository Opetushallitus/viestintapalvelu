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
package fi.vm.sade.viestintapalvelu.attachment.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fi.vm.sade.ryhmasahkoposti.api.dto.EmailAttachment;
import fi.vm.sade.viestintapalvelu.attachment.AttachmentService;
import fi.vm.sade.viestintapalvelu.attachment.dto.LetterReceiverLEtterAttachmentSaveDto;
import fi.vm.sade.viestintapalvelu.dao.LetterReceiverLetterAttachmentDAO;
import fi.vm.sade.viestintapalvelu.dao.LetterReceiverLetterDAO;
import fi.vm.sade.viestintapalvelu.model.LetterReceiverLetter;
import fi.vm.sade.viestintapalvelu.model.LetterReceiverLetterAttachment;

/**
 * User: ratamaa
 * Date: 29.9.2014
 * Time: 17:40
 */
@Service
public class AttachmentServiceImpl implements AttachmentService {
    private static final Logger logger = LoggerFactory.getLogger(AttachmentServiceImpl.class);

    @Autowired
    private LetterReceiverLetterDAO letterReceiverLetterDAO;

    @Autowired
    private LetterReceiverLetterAttachmentDAO letterReceiverLetterAttachmentDAO;

    @Override
    @Transactional
    public long saveReceiverAttachment(LetterReceiverLEtterAttachmentSaveDto dto) {
        LetterReceiverLetter receiverLetter = dto.getLetterReceiverLetterId() == null ? null :
                letterReceiverLetterDAO.read(dto.getLetterReceiverLetterId());
        LetterReceiverLetterAttachment receiverAttachment = new LetterReceiverLetterAttachment();
        receiverAttachment.setName(dto.getName());
        receiverAttachment.setContents(dto.getContents());
        receiverAttachment.setContentType(dto.getContentType());
        receiverAttachment.setLetterReceiverLetter(receiverLetter);
        letterReceiverLetterAttachmentDAO.insert(receiverAttachment);
        return receiverAttachment.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public EmailAttachment getLetterAttachment(Long attachmentId) {
        LetterReceiverLetterAttachment letter = letterReceiverLetterAttachmentDAO.read(attachmentId);
        if (letter == null) {
            return null;
        }
        EmailAttachment attachment = new EmailAttachment();
        attachment.setName(letter.getName());
        attachment.setContentType(letter.getContentType());
        attachment.setData(letter.getContents());
        return attachment;
    }

    @Override
    @Transactional
    public void markLetterReceiverAttachmentDownloaded(Long letterReceiverLetterAttachmentId) {
        LetterReceiverLetterAttachment attachment = letterReceiverLetterAttachmentDAO.read(letterReceiverLetterAttachmentId);
        if (attachment != null) {
            logger.debug("Removing LetterReceiverLetterAttachment={}", letterReceiverLetterAttachmentId);
            letterReceiverLetterAttachmentDAO.remove(attachment);
        }
    }

    public void setLetterReceiverLetterDAO(LetterReceiverLetterDAO letterReceiverLetterDAO) {
        this.letterReceiverLetterDAO = letterReceiverLetterDAO;
    }

    public void setLetterReceiverLetterAttachmentDAO(LetterReceiverLetterAttachmentDAO letterReceiverLetterAttachmentDAO) {
        this.letterReceiverLetterAttachmentDAO = letterReceiverLetterAttachmentDAO;
    }
}
