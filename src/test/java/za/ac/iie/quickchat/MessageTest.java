package za.ac.iie.quickchat;

import org.junit.jupiter.api.Test;
import za.ac.iie.quickchat.Message.Action;
import za.ac.iie.quickchat.Message.MessageRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void messageLengthValidation_success_and_failure() {
        Message service = new Message();
        assertEquals("Message ready to send.", service.checkMessageLength("Hi Mike, can you join us for dinner tonight?"));

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 260; i++) {
            builder.append('x');
        }
        String longMessage = builder.toString();
        assertEquals("Message exceeds 250 characters by 10; please reduce the size.", service.checkMessageLength(longMessage));
    }

    @Test
    void recipientValidation_success_and_failure() {
        Message service = new Message();
        assertEquals("Cell phone number successfully captured.", service.checkRecipientCell("+27718693002"));
        assertEquals(
                "Cell phone number is incorrectly formatted or does not contain an international code. Please correct the number and try again.",
                service.checkRecipientCell("08575975889")
        );
    }

    @Test
    void hashAndMessageIdGeneration() {
        Message service = new Message();
        String hash = service.createMessageHash("0012345678", 0, "Hi Mike, can you join us for dinner tonight?");
        assertEquals("00:0:HITONIGHT", hash);

        String id = service.generateMessageId();
        assertTrue(service.checkMessageID(id));
    }

    @Test
    void sentMessageActionResponses() {
        Message service = new Message();
        assertEquals("Message successfully sent.", service.sentMessage(Action.SEND));
        assertEquals("Press 0 to delete the message.", service.sentMessage(Action.DISREGARD));
        assertEquals("Message successfully stored.", service.sentMessage(Action.STORE));
    }

    @Test
    void part2FlowAndTotalMessages() {
        Message service = new Message();

        service.createAndClassifyMessage(0, "+27718693002", "Hi Mike, can you join us for dinner tonight?", Action.SEND);
        service.createAndClassifyMessage(1, "08575975889", "Hi Keegan, did you receive the payment?", Action.DISREGARD);

        assertEquals(1, service.returnTotalMessages());
        assertFalse(service.printMessages().isBlank());
    }

    @Test
    void part3ArraysAndQueries() throws IOException {
        Message service = new Message();

        service.createAndClassifyMessage(0, "+27834557896", "Did you get the cake?", Action.SEND);
        MessageRecord m2 = service.createAndClassifyMessage(1, "+27838884567", "Where are you? You are late! I have asked you to be on time.", Action.STORE);
        service.createAndClassifyMessage(2, "+27834484567", "Yohoooo, I am at your gate.", Action.DISREGARD);
        MessageRecord m4 = service.createAndClassifyMessage(3, "0838884567", "It is dinner time !", Action.SEND);
        service.createAndClassifyMessage(4, "+27838884567", "Ok, I am leaving without you.", Action.STORE);

        assertEquals(Arrays.asList("Did you get the cake?", "It is dinner time !"), service.getSentMessageTexts());
        assertEquals("Where are you? You are late! I have asked you to be on time.", service.getLongestStoredMessage());

        MessageRecord found = service.findByMessageId(m4.getMessageId());
        assertNotNull(found);
        assertEquals("It is dinner time !", found.getMessage());

        List<String> byRecipient = service.findMessagesByRecipient("+27838884567");
        assertEquals(Arrays.asList(
                "Where are you? You are late! I have asked you to be on time.",
                "Ok, I am leaving without you."
        ), byRecipient);

        String deleteResult = service.deleteByHash(m2.getMessageHash());
        assertEquals("Message: \"Where are you? You are late! I have asked you to be on time.\" successfully deleted.", deleteResult);

        String report = service.displayReport();
        assertTrue(report.contains("Message Hash:"));
        assertTrue(report.contains("Recipient:"));
        assertTrue(report.contains("Message:"));

        Path tmp = Files.createTempFile("stored", ".json");
        service.storeMessage(tmp.toString());

        Message reader = new Message();
        reader.readStoredMessages(tmp.toString());
        assertFalse(reader.getStoredMessages().isEmpty());
    }
}
