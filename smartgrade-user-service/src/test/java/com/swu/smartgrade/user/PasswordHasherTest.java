package com.swu.smartgrade.user;

import com.swu.smartgrade.user.application.PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {
    @Test
    void usesBcryptWithFreshSaltAndRejectsWrongPassword() {
        String first = PasswordHasher.hash("Teach2026", PasswordHasher.newSalt());
        String second = PasswordHasher.hash("Teach2026", PasswordHasher.newSalt());
        assertThat(first).startsWith("$2").isNotEqualTo(second);
        assertThat(PasswordHasher.matches("Teach2026", "", first)).isTrue();
        assertThat(PasswordHasher.matches("Wrong2026", "", first)).isFalse();
    }
}
