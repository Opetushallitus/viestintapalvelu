package fi.vm.sade.viestintapalvelu.message.conversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import fi.vm.sade.viestintapalvelu.api.address.AddressLabel;
import fi.vm.sade.viestintapalvelu.api.message.MessageData;
import fi.vm.sade.viestintapalvelu.api.message.Receiver;
import fi.vm.sade.viestintapalvelu.asiontitili.api.dto.AsiointitiliMessageDto;
import fi.vm.sade.viestintapalvelu.asiontitili.api.dto.AsiointitiliSendBatchDto;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageToAsiointiTiliConverterTest {
    
    private MessageToAsiointiTiliConverter converter = new MessageToAsiointiTiliConverter();
    
    @Test
    public void convertsToAsiointiTiliSendBatchDto() {
        MessageData message = givenMessageData();
        assertBatch(message, converter.convert(message).wrapped);
    }
    
    @Test
    public void addsReceiversWithoutHetuToIncompatibles() {
        Receiver receiver = givenReceiver("");
        ConvertedMessageWrapper<AsiointitiliSendBatchDto> wrapper = converter.convert(givenMessageData(Arrays.asList(receiver)));
        assertTrue(wrapper.incompatibleReceivers.contains(receiver));
        assertTrue(wrapper.wrapped.getMessages().isEmpty());
    }

    private void assertBatch(MessageData message, AsiointitiliSendBatchDto batch) {
        assertEquals(message.templateName, batch.getTemplateName());
        assertEquals(message.language, batch.getLanguageCode());
        assertEquals(message.commonReplacements, batch.getTemplateReplacements());
        assertMessages(message.receivers, batch.getMessages());
    }
    
    private void assertMessages(final List<Receiver> receivers, List<AsiointitiliMessageDto> messages) {
        for (final Receiver receiver : receivers) {
            assertTrue(Iterables.tryFind(messages, new Predicate<AsiointitiliMessageDto>() {

                @Override
                public boolean apply(AsiointitiliMessageDto input) {
                    return receiver.addressLabel.equals(input.getAddressLabel()) && receiver.ssn.equals(input.getReceiverHetu()) && receiver.replacements.equals(input.getTemplateReplacements())
                            && receiver.language.equals(input.getLanguageCode());
                }
                
            }).isPresent());
        }
        
    }

    private MessageData givenMessageData() {
        return givenMessageData(Arrays.asList(givenReceiver("010101-123N")));
    }
    
    private MessageData givenMessageData(List<Receiver> receivers) {
        return new MessageData("templateName", "FI", receivers, new HashMap<String, Object>());
    }

    private Receiver givenReceiver(String ssn) {
        return new Receiver("1.9.2.345", "test@test.com", new AddressLabel(), new HashMap<String, Object>(), ssn);
    }
}
