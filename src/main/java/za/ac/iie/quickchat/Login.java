package za.ac.iie.quickchat;

import java.util.Objects;
import java.util.regex.Pattern;

public class Login {
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    private static final Pattern SA_CELL_PATTERN = Pattern.compile("^\\+27\\d{9}$");

    private String username;
    private String password;
    private String cellPhone;
    private String firstName;
    private String lastName;

    public boolean checkUserName(String value) {
        return value != null && value.contains("_") && value.length() <= 5;
    }

    public boolean checkPasswordComplexity(String value) {
        return value != null && PASSWORD_PATTERN.matcher(value).matches();
    }

    public boolean checkCellPhoneNumber(String value) {
        return value != null && SA_CELL_PATTERN.matcher(value).matches();
    }

    public String registerUser(String username, String password, String cellPhone, String firstName, String lastName) {
        if (!checkUserName(username)) {
            return "Username is not correctly formatted; please ensure that your username contains an underscore and is no more than five characters in length.";
        }
        if (!checkPasswordComplexity(password)) {
            return "Password is not correctly formatted; please ensure that the password contains at least eight characters, a capital letter, a number, and a special character.";
        }
        if (!checkCellPhoneNumber(cellPhone)) {
            return "Cell number is incorrectly formatted or does not contain an international code; please correct the number and try again.";
        }

        this.username = username;
        this.password = password;
        this.cellPhone = cellPhone;
        this.firstName = firstName;
        this.lastName = lastName;

        return "User registered successfully.";
    }

    public boolean loginUser(String enteredUsername, String enteredPassword) {
        return Objects.equals(this.username, enteredUsername)
                && Objects.equals(this.password, enteredPassword);
    }

    public String returnLoginStatus(boolean success) {
        if (success) {
            return "Welcome " + firstName + ", " + lastName + " it is great to see you again.";
        }
        return "Username or password incorrect, please try again.";
    }

    public String getUsernameValidationMessage(String username) {
        if (checkUserName(username)) {
            return "Username successfully captured.";
        }
        return "Username is not correctly formatted; please ensure that your username contains an underscore and is no more than five characters in length.";
    }

    public String getPasswordValidationMessage(String password) {
        if (checkPasswordComplexity(password)) {
            return "Password successfully captured.";
        }
        return "Password is not correctly formatted; please ensure that the password contains at least eight characters, a capital letter, a number, and a special character.";
    }

    public String getCellValidationMessage(String cellPhone) {
        if (checkCellPhoneNumber(cellPhone)) {
            return "Cell number successfully captured.";
        }
        return "Cell number is incorrectly formatted or does not contain an international code; please correct the number and try again.";
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public static void main(String[] args) {
        Login login = new Login();

        String username = "kyl_1";
        String password = "Ch&&sec@ke99!";
        String cellPhone = "+27838968976";
        String firstName = "Kyle";
        String lastName = "Smith";

        System.out.println("QuickChat Login Demo");
        System.out.println("Username check: " + login.getUsernameValidationMessage(username));
        System.out.println("Password check: " + login.getPasswordValidationMessage(password));
        System.out.println("Cell check: " + login.getCellValidationMessage(cellPhone));

        String registration = login.registerUser(username, password, cellPhone, firstName, lastName);
        System.out.println("Registration: " + registration);

        boolean loginSuccess = login.loginUser(username, password);
        System.out.println(login.returnLoginStatus(loginSuccess));
    }
}
