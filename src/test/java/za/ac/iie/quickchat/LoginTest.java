package za.ac.iie.quickchat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginTest {

    @Test
    void checkUserName_valid() {
        Login login = new Login();
        assertTrue(login.checkUserName("kyl_1"));
    }

    @Test
    void checkUserName_invalid() {
        Login login = new Login();
        assertFalse(login.checkUserName("kyle!!!!!!!"));
        assertEquals(
                "Username is not correctly formatted; please ensure that your username contains an underscore and is no more than five characters in length.",
                login.getUsernameValidationMessage("kyle!!!!!!!")
        );
    }

    @Test
    void password_valid() {
        Login login = new Login();
        assertTrue(login.checkPasswordComplexity("Ch&&sec@ke99!"));
        assertEquals("Password successfully captured.", login.getPasswordValidationMessage("Ch&&sec@ke99!"));
    }

    @Test
    void password_invalid() {
        Login login = new Login();
        assertFalse(login.checkPasswordComplexity("password"));
        assertEquals(
                "Password is not correctly formatted; please ensure that the password contains at least eight characters, a capital letter, a number, and a special character.",
                login.getPasswordValidationMessage("password")
        );
    }

    @Test
    void cell_valid() {
        Login login = new Login();
        assertTrue(login.checkCellPhoneNumber("+27838968976"));
        assertEquals("Cell number successfully captured.", login.getCellValidationMessage("+27838968976"));
    }

    @Test
    void cell_invalid() {
        Login login = new Login();
        assertFalse(login.checkCellPhoneNumber("08966553"));
        assertEquals(
                "Cell number is incorrectly formatted or does not contain an international code; please correct the number and try again.",
                login.getCellValidationMessage("08966553")
        );
    }

    @Test
    void login_success_and_failure() {
        Login login = new Login();
        assertEquals("User registered successfully.", login.registerUser("kyl_1", "Ch&&sec@ke99!", "+27838968976", "Kyle", "Smith"));

        assertTrue(login.loginUser("kyl_1", "Ch&&sec@ke99!"));
        assertEquals("Welcome Kyle, Smith it is great to see you again.", login.returnLoginStatus(true));

        assertFalse(login.loginUser("kyl_1", "wrong"));
        assertEquals("Username or password incorrect, please try again.", login.returnLoginStatus(false));
    }
}
