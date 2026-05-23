package za.ac.iie.quickchat;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import za.ac.iie.quickchat.Message.Action;
import za.ac.iie.quickchat.Message.MessageRecord;

public class QuickChatApp {

    private static final String STORED_JSON         = "stored-messages.json";
    private static final String REGISTRATION_SUCCESS = "User registered successfully.";
    private static final String VALID_RECIPIENT      = "Cell phone number successfully captured.";
    private static final String MESSAGE_READY        = "Message ready to send.";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        try {
            Login   login          = new Login();
            Message messageService = new Message();

            // ── Welcome ──────────────────────────────────────────────────────
            System.out.println("Welcome to QuickChat");
            System.out.println();

            // ── Registration ─────────────────────────────────────────────────
            System.out.println("Register your account");

            String firstName = prompt(scanner, "First name: ").trim();
            String lastName  = prompt(scanner, "Last name: ").trim();
            String username  = promptUntilValid(scanner, "Username: ",
                    login::getUsernameValidationMessage, login::checkUserName);
            String password  = promptUntilValid(scanner, "Password: ",
                    login::getPasswordValidationMessage, login::checkPasswordComplexity);
            String cell      = promptUntilValid(scanner, "Cell number (+27...): ",
                    login::getCellValidationMessage, login::checkCellPhoneNumber);

            String registration = login.registerUser(username, password, cell, firstName, lastName);
            System.out.println(registration);
            if (!REGISTRATION_SUCCESS.equals(registration)) {
                return;
            }

            // ── Login ────────────────────────────────────────────────────────
            System.out.println();
            System.out.println("Login");
            boolean loggedIn = false;
            int     attempts = 0;
            while (!loggedIn && attempts < 3) {
                String enteredUser = prompt(scanner, "Username: ").trim();
                String enteredPass = prompt(scanner, "Password: ").trim();
                loggedIn = login.loginUser(enteredUser, enteredPass);
                System.out.println(login.returnLoginStatus(loggedIn));
                attempts++;
            }

            if (!loggedIn) {
                System.out.println("Too many failed attempts. Exiting QuickChat.");
                return;
            }

            // ── How many messages this session ───────────────────────────────
            int count          = promptForInt(scanner, "How many messages do you want to enter? ");
            int messagesSent   = 0;   // tracks how many have been processed (sent/stored/disregarded)
            int numberCounter  = 0;   // auto-incremented message number

            // ── Main menu loop (runs until user quits) ───────────────────────
            while (true) {
                System.out.println();
                System.out.println("Menu");
                System.out.println("1) Send Messages");
                System.out.println("2) Show recently sent messages");
                System.out.println("3) Quit");
                System.out.println("4) Stored Messages");
                int option = promptForInt(scanner, "Choose option: ");

                switch (option) {

                    case 3:
                        // Display total before quitting
                        System.out.println("Total messages sent: " + messageService.returnTotalMessages());
                        System.out.println("Session complete.");
                        return;

                    case 2:
                        System.out.println("Coming Soon.");
                        break;

                    case 4:
                        handleStoredMenu(scanner, messageService);
                        break;

                    case 1:
                        // Allow the user to compose one message per menu visit,
                        // up to the total they declared at the start.
                        if (messagesSent >= count) {
                            System.out.println("You have already entered all " + count + " messages.");
                            System.out.println("Total messages sent: " + messageService.returnTotalMessages());
                            break;
                        }

                        numberCounter++;
                        processMessage(scanner, messageService, numberCounter);
                        messagesSent++;

                        // Persist stored messages to JSON after every entry
                        try {
                            messageService.storeMessage(STORED_JSON);
                        } catch (IOException e) {
                            System.out.println("Could not store JSON: " + e.getMessage());
                        }

                        System.out.println("Messages entered: " + messagesSent + " / " + count);
                        break;

                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            }

        } catch (IllegalStateException e) {
            System.out.println("Input stream closed. Exiting QuickChat.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } finally {
            scanner.close();
        }
    }

    // ── Process a single message ───────────────────────────────────────────────

    private static void processMessage(Scanner scanner, Message messageService, int messageNumber) {

        // Validate recipient
        String recipient;
        while (true) {
            recipient = prompt(scanner, "Recipient (+27...): ").trim();
            String recipientStatus = messageService.checkRecipientCell(recipient);
            System.out.println(recipientStatus);
            if (VALID_RECIPIENT.equals(recipientStatus)) {
                break;
            }
        }

        // Validate message text
        String text;
        while (true) {
            text = prompt(scanner, "Message text: ").trim();
            String messageStatus = messageService.checkMessageLength(text);
            System.out.println(messageStatus);
            if (MESSAGE_READY.equals(messageStatus)) {
                break;
            }
        }

        // Choose action
        System.out.println("Select action:");
        System.out.println("1) Send Message");
        System.out.println("2) Disregard Message");
        System.out.println("3) Store Message to send later");
        int    actionOption = promptForInt(scanner, "Choose option: ");
        Action action       = mapAction(actionOption);

        // Create, classify, and display result
        MessageRecord record = messageService.createAndClassifyMessage(
                messageNumber, recipient, text, action);
        System.out.println(messageService.sentMessage(action));

        if (action == Action.DISREGARD) {
            return;
        }

        // Display full message details as required by the spec
        System.out.println("Message ID: "   + record.getMessageId());
        System.out.println("Message Hash: " + record.getMessageHash());
        System.out.println("Recipient: "    + record.getRecipient());
        System.out.println("Message: "      + record.getMessage());
    }

    // ── Stored messages sub-menu ───────────────────────────────────────────────

    private static void handleStoredMenu(Scanner scanner, Message messageService) {
        try {
            messageService.readStoredMessages(STORED_JSON);
        } catch (IOException ignored) {
            // Fine if the file doesn't exist yet
        }

        System.out.println();
        System.out.println("Stored Messages Menu");
        System.out.println("1) Display sender and recipient of stored messages");
        System.out.println("2) Display longest stored message");
        System.out.println("3) Search by message ID");
        System.out.println("4) Search by recipient");
        System.out.println("5) Delete by message hash");
        System.out.println("6) Display report");
        int option = promptForInt(scanner, "Choose option: ");

        switch (option) {
            case 1:
                List<MessageRecord> stored = messageService.getStoredMessages();
                if (stored.isEmpty()) {
                    System.out.println("No stored messages.");
                } else {
                    for (MessageRecord record : stored) {
                        System.out.println("Sender: You, Recipient: " + record.getRecipient());
                    }
                }
                break;

            case 2:
                System.out.println(messageService.getLongestStoredMessage());
                break;

            case 3:
                String id    = prompt(scanner, "Enter message ID: ").trim();
                MessageRecord found = messageService.findByMessageId(id);
                if (found != null) {
                    System.out.println("Recipient: " + found.getRecipient());
                    System.out.println("Message: "   + found.getMessage());
                } else {
                    System.out.println("No message found.");
                }
                break;

            case 4:
                String recipient = prompt(scanner, "Enter recipient: ").trim();
                List<String> messages = messageService.findMessagesByRecipient(recipient);
                if (messages.isEmpty()) {
                    System.out.println("No messages found.");
                } else {
                    for (String msg : messages) {
                        System.out.println(msg);
                    }
                }
                break;

            case 5:
                String hash = prompt(scanner, "Enter hash: ").trim();
                System.out.println(messageService.deleteByHash(hash));
                break;

            case 6:
                System.out.println(messageService.displayReport());
                break;

            default:
                System.out.println("Invalid option.");
                break;
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private interface ValidationMessage { String apply(String value); }
    private interface ValidationCheck   { boolean test(String value); }

    private static String promptUntilValid(Scanner scanner, String promptText,
                                           ValidationMessage message,
                                           ValidationCheck check) {
        while (true) {
            String value = prompt(scanner, promptText).trim();
            System.out.println(message.apply(value));
            if (check.test(value)) {
                return value;
            }
        }
    }

    private static Action mapAction(int actionOption) {
        switch (actionOption) {
            case 1: return Action.SEND;
            case 2: return Action.DISREGARD;
            case 3: return Action.STORE;
            default: throw new IllegalArgumentException("Invalid option selected.");
        }
    }

    private static String prompt(Scanner scanner, String promptText) {
        if (!promptText.isEmpty()) {
            System.out.print(promptText);
        }
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("No console input available.");
        }
        return scanner.nextLine();
    }

    private static int promptForInt(Scanner scanner, String promptText) {
        String value = prompt(scanner, promptText);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric input: " + value);
        }
    }
}