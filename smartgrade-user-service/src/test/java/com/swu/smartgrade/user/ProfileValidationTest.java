package com.swu.smartgrade.user;

import com.swu.smartgrade.user.api.dto.ProfileRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class ProfileValidationTest {
    @Test
    void validatesEnumsAndDeduplicatesOptionalPhrases() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        var request = new ProfileRequest("NEUTRAL", "MEDIUM", "DIRECT", List.of("论点清晰", "论点清晰"), null);
        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.commonPhrases()).containsExactly("论点清晰");
        assertThat(request.bannedPhrases()).isEmpty();
        assertThat(validator.validate(new ProfileRequest("温和", "适中", "直接", null, null))).isNotEmpty();
    }
}
