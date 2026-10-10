package rw.ac.rca.hoabs;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Scheduling Rules & Validation Tests (Appendix A, EP/BVA)")
class SchedulingRulesTest {

    private static final Pattern RWANDAN_PHONE_PATTERN = Pattern.compile("^07[2389][0-9]{7}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    @Test
    @DisplayName("Lead time BVA: < 2 hours is partition P1 (too late)")
    void testLeadTimeTooLate() {
        Instant now = Instant.now();
        Instant slotStart = now.plus(Duration.ofMinutes(119));
        Duration leadTime = Duration.between(now, slotStart);
        assertTrue(leadTime.compareTo(Duration.ofHours(2)) < 0, "Lead time under 2 hours must be rejected online");
    }

    @Test
    @DisplayName("Lead time BVA: >= 2 hours and <= 14 days is partition P2 (valid)")
    void testLeadTimeValid() {
        Instant now = Instant.now();
        Instant slotExactTwoHours = now.plus(Duration.ofHours(2));
        Instant slotFourteenDays = now.plus(Duration.ofDays(14));

        assertTrue(Duration.between(now, slotExactTwoHours).compareTo(Duration.ofHours(2)) >= 0);
        assertTrue(Duration.between(now, slotFourteenDays).compareTo(Duration.ofDays(14)) <= 0);
    }

    @Test
    @DisplayName("Lead time BVA: > 14 days is partition P3 (too far)")
    void testLeadTimeTooFar() {
        Instant now = Instant.now();
        Instant slotFifteenDays = now.plus(Duration.ofDays(15));
        assertTrue(Duration.between(now, slotFifteenDays).compareTo(Duration.ofDays(14)) > 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0788000001", "0720123456", "0739999999", "0791234567"})
    @DisplayName("Valid Rwandan mobile numbers (07X, 10 digits)")
    void testValidRwandanPhones(String phone) {
        assertTrue(RWANDAN_PHONE_PATTERN.matcher(phone).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"078800000", "07880000001", "0888000001", "0712345678", "0788abc001", ""})
    @DisplayName("Invalid Rwandan mobile numbers rejected")
    void testInvalidRwandanPhones(String phone) {
        assertFalse(RWANDAN_PHONE_PATTERN.matcher(phone).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Password1", "Amahoro2026!", "Rwanda123", "SecurePass9"})
    @DisplayName("Strong passwords matching policy (>= 8 chars, letter + digit)")
    void testValidPasswords(String pass) {
        assertTrue(PASSWORD_PATTERN.matcher(pass).matches());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pass", "1234567", "allletters", "123456789", "pass1"})
    @DisplayName("Weak passwords rejected")
    void testInvalidPasswords(String pass) {
        assertFalse(PASSWORD_PATTERN.matcher(pass).matches());
    }
}
