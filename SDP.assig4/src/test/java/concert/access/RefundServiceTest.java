package concert.access;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefundServiceTest {
    private final RefundService service = new RefundService();
    @Test
    void refundIsPriceMinusTenPercent() {
        assertEquals(72, service.calculateRefund(80));
        assertEquals(36, service.calculateRefund(40));
    }

    @Test
    void feeIsAtLeastOne() {
        assertEquals(4, service.calculateRefund(5));
    }

    @Test
    void zeroPriceGivesZeroRefund() {
        assertEquals(0, service.calculateRefund(0));
    }
}
