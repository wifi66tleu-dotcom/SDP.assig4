package concert.ticketing;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FraudCheckerTest {

    private final FraudChecker checker = new FraudChecker();

    @Test
    void normalBuyerPasses() {
        assertDoesNotThrow(() -> checker.check("Anna", false, 3));
    }

    @Test
    void blockedBuyerFails() {
        assertThrows(IllegalStateException.class, () -> checker.check("Boris", true, 0));
    }

    @Test
    void fourTicketsAlreadyBoughtFails() {
        assertThrows(IllegalStateException.class, () -> checker.check("Eva", false, 4));
    }
}
