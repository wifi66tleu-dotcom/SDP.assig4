package concert.seating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SeatingChartTest {

    private Path dir;
    private SeatingChart chart;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dir = tempDir;
        chart = new SeatingChart(new ReservationRepository(dir), new DynamicPricingEngine());
    }

    @Test
    void holdSeatMakesItUnavailable() {
        Reservation r = chart.holdSeat("MUSIC-NIGHT", "A1", "ORD-1");

        assertEquals(Reservation.Status.HELD, r.getStatus());
        assertFalse(chart.getFreeSeats("MUSIC-NIGHT").contains("A1"));
        assertTrue(chart.getFreeSeats("SUNSET-CONCERT").contains("A1"));
    }

    @Test
    void takenSeatCannotBeHeldTwice() {
        chart.holdSeat("MUSIC-NIGHT", "A1", "ORD-1");
        assertThrows(IllegalStateException.class, () -> chart.holdSeat("MUSIC-NIGHT", "A1", "ORD-2"));
    }

    @Test
    void unknownSeatAndUnknownConcertAreRejected() {
        assertThrows(IllegalStateException.class, () -> chart.holdSeat("MUSIC-NIGHT", "Z9", "ORD-1"));
        assertThrows(IllegalStateException.class, () -> chart.holdSeat("NO-SUCH-CONCERT", "A1", "ORD-1"));
    }

    @Test
    void releasedSeatIsFreeAgain() {
        Reservation r = chart.holdSeat("MUSIC-NIGHT", "A1", "ORD-1");
        chart.release(r.getId());
        assertTrue(chart.getFreeSeats("MUSIC-NIGHT").contains("A1"));
    }

    @Test
    void confirmedReservationCanBeReleased() {
        Reservation r = chart.holdSeat("MUSIC-NIGHT", "A1", "ORD-1");
        chart.confirm(r.getId());
        chart.release(r.getId());
        assertEquals(Reservation.Status.RELEASED, chart.findReservation(r.getId()).getStatus());
    }

    @Test
    void illegalStatusChangesAreRejected() {
        Reservation r = chart.holdSeat("MUSIC-NIGHT", "A1", "ORD-1");
        chart.confirm(r.getId());
        assertThrows(IllegalStateException.class, () -> chart.confirm(r.getId()));
        chart.release(r.getId());
        assertThrows(IllegalStateException.class, () -> chart.release(r.getId()));
        assertThrows(IllegalStateException.class, () -> chart.confirm(r.getId()));
    }

    @Test
    void unknownReservationIsRejected() {
        assertThrows(IllegalStateException.class, () -> chart.confirm("R-99"));
        assertThrows(IllegalStateException.class, () -> chart.release("R-99"));
    }

    @Test
    void reservationIsReadBackFromDiskAfterRestart() {
        chart.holdSeat("MUSIC-NIGHT", "B2", "ORD-7");

        SeatingChart restarted = new SeatingChart(new ReservationRepository(dir), new DynamicPricingEngine());

        Reservation r = restarted.findReservationByOrder("ORD-7");
        assertEquals("B2", r.getSeatId());
        assertEquals(60, r.getPrice());
        assertFalse(restarted.getFreeSeats("MUSIC-NIGHT").contains("B2"));
    }

    @Test
    void findingMissingReservationGivesNull() {
        assertNull(chart.findReservation("R-1"));
        assertNull(chart.findReservationByOrder("ORD-1"));
    }
}
