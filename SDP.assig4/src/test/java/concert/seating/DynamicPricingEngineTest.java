package concert.seating;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DynamicPricingEngineTest {

    private final DynamicPricingEngine engine = new DynamicPricingEngine();

    @Test
    void frontRowsCostMore() {
        assertEquals(80, engine.calculatePrice("A1", 0, 20));
        assertEquals(60, engine.calculatePrice("B1", 0, 20));
        assertEquals(40, engine.calculatePrice("C1", 0, 20));
        assertEquals(40, engine.calculatePrice("D5", 0, 20));
    }

    @Test
    void priceGrowsWhenHallFillsUp() {
        assertEquals(40, engine.calculatePrice("C1", 9, 20));   // 45% taken
        assertEquals(46, engine.calculatePrice("C1", 10, 20));  // 50% taken
        assertEquals(52, engine.calculatePrice("C1", 15, 20));  // 75% taken
    }
}
