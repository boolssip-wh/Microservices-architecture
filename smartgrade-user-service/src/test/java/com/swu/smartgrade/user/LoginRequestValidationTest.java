package com.swu.smartgrade.user;

import com.swu.smartgrade.user.api.dto.LoginRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsBlankUsernameAndPassword() {
        var violations = validator.validate(new LoginRequest(" ", ""));

        assertThat(violations).extracting(item -> item.getPropertyPath().toString())
                .contains("username", "password");
    }

    @Test
    void enforcesDocumentedCredentialRulesAndTrimsUsername() {
        assertThat(validator.validate(new LoginRequest(" teacher1 ", "Teach2026"))).isEmpty();
        assertThat(validator.validate(new LoginRequest("teacher1", "123456"))).isNotEmpty();
        assertThat(validator.validate(new LoginRequest("teacher1", "abcdefgh"))).isNotEmpty();
        assertThat(validator.validate(new LoginRequest("bad-name", "Teach2026"))).isNotEmpty();
        assertThat(validator.validate(new LoginRequest("teacher1", "Teach 2026"))).isNotEmpty();
    }
}
