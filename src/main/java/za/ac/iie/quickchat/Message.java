package za.ac.iie.quickchat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Message {
    public enum Action {
        SEND,
        DISREGARD,
        STORE
    }

    public static class MessageRecord {
        private final String messageId;
        private final int messageNumber;
        private final String recipient;
        private final String message;
        private final String messageHash;
        private final Action action;

        public MessageRecord(String messageId, int messageNumber, String recipient, String message, String messageHash, Action action) {
            this.messageId = messageId;
            this.messageNumber = messageNumber;
            this.recipient = recipient;
            this.message = message;
            this.messageHash = messageHash;
            this.action = action;
        }

        public String getMessageId() {
            return messageId;
        }

        public int getMessageNumber() {
            return messageNumber;
        }

        public String getRecipient() {
            return recipient;
        }

        public String getMessage() {
            return message;
        }

        public String getMessageHash() {
            return messageHash;
        }

        public Action getAction() {
            return action;
        }
    }

    private static final int MAX_MESSAGE_LENGTH = 250;
    private static final Pattern RECIPIENT_PATTERN = Pattern.compile("^\\+27\\d{9}$");
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
            "\\{\\s*\"messageId\":\"(.*?)\",\\s*\"messageNumber\":(\\d+),\\s*\"recipient\":\"(.*?)\",\\s*\"message\":\"(.*?)\",\\s*\"messageHash\":\"(.*?)\",\\s*\"action\":\"(.*?)\"\\s*\\}"
    );

    private final SecureRandom random = new SecureRandom();
    private final List<MessageRecord> allMessages = new ArrayList<MessageRecord>();
    private final List<MessageRecord> sentMessages = new ArrayList<MessageRecord>();
    private final List<MessageRecord> disregardedMessages = new ArrayList<MessageRecord>();
    private final List<MessageRecord> storedMessages = new ArrayList<MessageRecord>();

    public String generateMessageId() {
        long value = 1_000_000_000L + (Math.abs(random.nextLong()) % 9_000_000_000L);
        return Long.toString(value);
    }

    public boolean checkMessageID(String messageId) {
        return messageId != null && messageId.length() == 10 && messageId.matches("\\d{10}");
    }

    public String checkRecipientCell(String recipient) {
        if (recipient != null && RECIPIENT_PATTERN.matcher(recipient).matches()) {
            return "Cell phone number successfully captured.";
        }
        return "Cell phone number is incorrectly formatted or does not contain an international code. Please correct the number and try again.";
    }

    public String checkMessageLength(String message) {
        if (message == null) {
            return "Please enter a message of less than 250 characters.";
        }
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return "Message ready to send.";
        }
        return "Please enter a message of less than 250 characters.";
    }

    public String createMessageHash(String messageId, int messageNumber, String message) {
        String idPrefix = messageId.substring(0, Math.min(2, messageId.length()));
        String[] words = message.trim().split("\\s+");
        String firstWord = words.length > 0 ? words[0] : "";
        String lastWord = words.length > 0 ? words[words.length - 1] : "";
        String token = (firstWord + lastWord).replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return idPrefix + ":" + messageNumber + ":" + token;
    }

    public String sentMessage(Action action) {
        switch (action) {
            case SEND:
                return "Message successfully sent.";
            case DISREGARD:
                return "Press 0 to delete the message.";
            case STORE:
                return "Message successfully stored.";
            default:
                return "Invalid action.";
        }
    }

    public String SentMessage(Action action) {
        return sentMessage(action);
    }

    public MessageRecord createAndClassifyMessage(int messageNumber, String recipient, String message, Action action) {
        String id = generateMessageId();
        String hash = createMessageHash(id, messageNumber, message);
        MessageRecord record = new MessageRecord(id, messageNumber, recipient, message, hash, action);

        if (action != Action.DISREGARD) {
            allMessages.add(record);
        }
        if (action == Action.SEND) {
            sentMessages.add(record);
        } else if (action == Action.DISREGARD) {
            disregardedMessages.add(record);
        } else if (action == Action.STORE) {
            storedMessages.add(record);
        }
        return record;
    }

    public String printMessages() {
        if (allMessages.isEmpty()) {
            return "No messages to display.";
        }
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < allMessages.size(); i++) {
            MessageRecord m = allMessages.get(i);
            if (i > 0) {
                output.append(System.lineSeparator()).append("---").append(System.lineSeparator());
            }
            output.append("Message ID: ").append(m.getMessageId()).append(System.lineSeparator());
            output.append("Message Hash: ").append(m.getMessageHash()).append(System.lineSeparator());
            output.append("Recipient: ").append(m.getRecipient()).append(System.lineSeparator());
            output.append("Message: ").append(m.getMessage());
        }
        return output.toString();
    }

    public int returnTotalMessages() {
        return sentMessages.size();
    }

    public int returnTotalMessagess() {
        return returnTotalMessages();
    }

    public void storeMessage(String jsonPath) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("[");
        for (int i = 0; i < storedMessages.size(); i++) {
            MessageRecord record = storedMessages.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append(System.lineSeparator()).append("  {");
            json.append("\"messageId\":\"").append(escapeJson(record.getMessageId())).append("\",");
            json.append("\"messageNumber\":").append(record.getMessageNumber()).append(",");
            json.append("\"recipient\":\"").append(escapeJson(record.getRecipient())).append("\",");
            json.append("\"message\":\"").append(escapeJson(record.getMessage())).append("\",");
            json.append("\"messageHash\":\"").append(escapeJson(record.getMessageHash())).append("\",");
            json.append("\"action\":\"").append(record.getAction().name()).append("\"");
            json.append("}");
        }
        if (!storedMessages.isEmpty()) {
            json.append(System.lineSeparator());
        }
        json.append("]");

        Files.write(Path.of(jsonPath), json.toString().getBytes(StandardCharsets.UTF_8));
    }

    public List<MessageRecord> readStoredMessages(String jsonPath) throws IOException {
        storedMessages.clear();
        String content = Files.readString(Path.of(jsonPath), StandardCharsets.UTF_8);
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(content);

        while (matcher.find()) {
            MessageRecord record = new MessageRecord(
                    unescapeJson(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    unescapeJson(matcher.group(3)),
                    unescapeJson(matcher.group(4)),
                    unescapeJson(matcher.group(5)),
                    Action.valueOf(matcher.group(6))
            );
            storedMessages.add(record);
        }
        return new ArrayList<MessageRecord>(storedMessages);
    }

    public List<String> getSentMessageTexts() {
        List<String> results = new ArrayList<String>();
        for (MessageRecord record : sentMessages) {
            results.add(record.getMessage());
        }
        return results;
    }

    public String getLongestStoredMessage() {
        String longest = "";
        for (MessageRecord record : storedMessages) {
            if (record.getMessage().length() > longest.length()) {
                longest = record.getMessage();
            }
        }
        return longest;
    }

    public MessageRecord findByMessageId(String messageId) {
        for (MessageRecord record : allMessages) {
            if (record.getMessageId().equals(messageId)) {
                return record;
            }
        }
        return null;
    }

    public List<String> findMessagesByRecipient(String recipient) {
        List<String> results = new ArrayList<String>();
        for (MessageRecord record : allMessages) {
            if (record.getRecipient().equals(recipient)
                    && (record.getAction() == Action.SEND || record.getAction() == Action.STORE)) {
                results.add(record.getMessage());
            }
        }
        return results;
    }

    public String deleteByHash(String hash) {
        MessageRecord target = null;
        for (MessageRecord record : allMessages) {
            if (record.getMessageHash().equals(hash)) {
                target = record;
                break;
            }
        }

        if (target == null) {
            return "Message hash not found.";
        }

        allMessages.remove(target);
        sentMessages.remove(target);
        storedMessages.remove(target);
        disregardedMessages.remove(target);
        return "Message: \"" + target.getMessage() + "\" successfully deleted.";
    }

    public String displayReport() {
        StringBuilder report = new StringBuilder();
        for (MessageRecord message : allMessages) {
            if (message.getAction() == Action.SEND || message.getAction() == Action.STORE) {
                report.append("Message Hash: ").append(message.getMessageHash()).append(System.lineSeparator());
                report.append("Recipient: ").append(message.getRecipient()).append(System.lineSeparator());
                report.append("Message: ").append(message.getMessage()).append(System.lineSeparator()).append(System.lineSeparator());
            }
        }
        return report.toString().trim();
    }

    public List<MessageRecord> getStoredMessages() {
        return new ArrayList<MessageRecord>(storedMessages);
    }

    public List<MessageRecord> getAllMessages() {
        return new ArrayList<MessageRecord>(allMessages);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String unescapeJson(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public static void main(String[] args) {
        Message service = new Message();

        System.out.println("QuickChat Message Demo");
        System.out.println("Recipient check: " + service.checkRecipientCell("+27718693002"));
        System.out.println("Length check: " + service.checkMessageLength("Hi Mike, can you join us for dinner tonight?"));

        MessageRecord record = service.createAndClassifyMessage(
                0,
                "+27718693002",
                "Hi Mike, can you join us for dinner tonight?",
                Action.SEND
        );

        System.out.println("Action result: " + service.sentMessage(Action.SEND));
        System.out.println("Message ID: " + record.getMessageId());
        System.out.println("Message Hash: " + record.getMessageHash());
        System.out.println("Messages sent: " + service.returnTotalMessages());
        System.out.println();
        System.out.println(service.printMessages());
    }
}
