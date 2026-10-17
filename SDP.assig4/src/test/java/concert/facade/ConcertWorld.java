package concert.facade;

import concert.access.AccessControlGate;
import concert.access.AccessPass;
import concert.access.AccessPassRepository;
import concert.access.RefundService;
import concert.seating.DynamicPricingEngine;
import concert.seating.Reservation;
import concert.seating.ReservationRepository;
import concert.seating.SeatingChart;
import concert.ticketing.FraudChecker;
import concert.ticketing.Ticket;
import concert.ticketing.TicketPrinter;
import concert.ticketing.TicketRepository;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class ConcertWorld {

    public static final String CONCERT = "MUSIC-NIGHT";

    private final Path dataDir;
    public SeatingChart seating;
    public TicketPrinter printer;
    public AccessControlGate gate;
    public OrderRepository orders;
    public TicketBookingFacade booking;
    public BoxOfficeAdminFacade admin;

    public ConcertWorld(Path dataDir) {
        this.dataDir = dataDir;
        wire();
    }

    private void wire() {
        seating = new SeatingChart(new ReservationRepository(dataDir), new DynamicPricingEngine());
        printer = new TicketPrinter(new TicketRepository(dataDir), new FraudChecker());
        gate = new AccessControlGate(new AccessPassRepository(dataDir), new RefundService());
        orders = new OrderRepository(dataDir);
        booking = new TicketBookingFacade(seating, printer, gate, orders);
        admin = new BoxOfficeAdminFacade(seating, printer, gate, orders);
    }

    public ConcertWorld restartApplication() {
        wire();
        return this;
    }

    public ConcertWorld givenBuyerIsBlocked(String buyer) {
        admin.blockBuyer(buyer);
        return this;
    }

    public ConcertWorld givenGateIsClosed() {
        admin.closeGate(CONCERT);
        return this;
    }

    public Order whenBuying(String buyer, String seat) {

        return booking.buyTicket(CONCERT, seat, buyer);
    }

    public PurchaseFailedException whenBuyingFails(String buyer, String seat) {
        return assertThrows(PurchaseFailedException.class, () -> booking.buyTicket(CONCERT, seat, buyer));
    }

    public void thenSeatIsFree(String seat) {
        assertTrue(seating.getFreeSeats(CONCERT).contains(seat), "seat " + seat + " should be free");
    }

    public void thenSeatIsTaken(String seat) {
        assertFalse(seating.getFreeSeats(CONCERT).contains(seat), "seat " + seat + " should be taken");
    }

    public void thenOrderIs(String orderId, Order.Status expected) {
        assertEquals(expected, orders.findById(orderId).getStatus());
    }

    public void thenReservationIs(String orderId, Reservation.Status expected) {
        assertEquals(expected, seating.findReservationByOrder(orderId).getStatus());
    }

    public void thenTicketIs(String orderId, Ticket.Status expected) {
        assertEquals(expected, printer.findTicketByOrder(orderId).getStatus());
    }

    public void thenNoTicketFor(String orderId) {

        assertNull(printer.findTicketByOrder(orderId));
    }

    public void thenPassIs(String orderId, AccessPass.Status expected) {
        assertEquals(expected, gate.findPassByOrder(orderId).getStatus());
    }

    public void thenNoPassFor(String orderId) {

        assertNull(gate.findPassByOrder(orderId));
    }
}
