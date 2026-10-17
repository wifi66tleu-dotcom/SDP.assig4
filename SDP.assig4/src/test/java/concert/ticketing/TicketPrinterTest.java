package concert.ticketing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class TicketPrinterTest {

    private Path dir;
    private TicketPrinter printer;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dir = tempDir;
        printer = new TicketPrinter(new TicketRepository(dir), new FraudChecker());
    }

    @Test
    void printedTicketHasStatusPrinted() {
        Ticket t = printer.printTicket("MUSIC-NIGHT", "A1", "Anna", "ORD-1");
        assertEquals(Ticket.Status.PRINTED, t.getStatus());
        assertEquals("T-1", t.getId());
    }

    @Test
    void blockedBuyerIsRejectedAndNoTicketIsSaved() {
        printer.blockBuyer("Boris");
        assertThrows(IllegalStateException.class, () -> printer.printTicket("MUSIC-NIGHT", "A1", "boris", "ORD-1"));
        assertNull(printer.findTicketByOrder("ORD-1"));
    }

    @Test
    void buyerCannotHoldMoreThanFourTickets() {
        for (int i = 1; i <= 4; i++) {
            printer.printTicket("MUSIC-NIGHT", "A" + i, "Eva", "ORD-" + i);
        }
        assertThrows(IllegalStateException.class, () -> printer.printTicket("MUSIC-NIGHT", "A5", "Eva", "ORD-5"));
    }

    @Test
    void voidedTicketsDoNotCountForTheLimit() {
        for (int i = 1; i <= 4; i++) {
            printer.printTicket("MUSIC-NIGHT", "A" + i, "Eva", "ORD-" + i);
        }
        printer.voidTicket("T-1");
        assertDoesNotThrow(() -> printer.printTicket("MUSIC-NIGHT", "A5", "Eva", "ORD-5"));
    }

    @Test
    void badBuyerNameIsRejected() {
        assertThrows(IllegalStateException.class, () -> printer.printTicket("MUSIC-NIGHT", "A1", "", "ORD-1"));
        assertThrows(IllegalStateException.class, () -> printer.printTicket("MUSIC-NIGHT", "A1", null, "ORD-1"));
        assertThrows(IllegalStateException.class, () -> printer.printTicket("MUSIC-NIGHT", "A1", "a,b", "ORD-1"));
    }

    @Test
    void voidTwiceIsIllegal() {
        Ticket t = printer.printTicket("MUSIC-NIGHT", "A1", "Anna", "ORD-1");
        printer.voidTicket(t.getId());
        assertThrows(IllegalStateException.class, () -> printer.voidTicket(t.getId()));
        assertThrows(IllegalStateException.class, () -> printer.voidTicket("T-99"));
    }

    @Test
    void printingAnAlreadyPrintedTicketIsIllegal() {
        Ticket t = printer.printTicket("MUSIC-NIGHT", "A1", "Anna", "ORD-1");
        assertThrows(IllegalStateException.class, t::print);
    }

    @Test
    void ticketsAndBlocklistAreReadBackAfterRestart() {
        printer.printTicket("MUSIC-NIGHT", "A1", "Anna", "ORD-1");
        printer.blockBuyer("Boris");
        printer.blockBuyer("Boris");

        TicketPrinter restarted = new TicketPrinter(new TicketRepository(dir), new FraudChecker());

        assertEquals("Anna", restarted.findTicket("T-1").getBuyer());
        assertThrows(IllegalStateException.class, () -> restarted.printTicket("MUSIC-NIGHT", "A2", "Boris", "ORD-2"));
    }
}
