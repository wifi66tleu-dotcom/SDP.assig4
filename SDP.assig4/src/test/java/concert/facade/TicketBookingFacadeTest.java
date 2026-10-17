package concert.facade;
import concert.access.AccessPass;
import concert.seating.DynamicPricingEngine;
import concert.seating.Reservation;
import concert.seating.ReservationRepository;
import concert.seating.SeatingChart;
import concert.ticketing.FraudChecker;
import concert.ticketing.Ticket;
import concert.ticketing.TicketPrinter;
import concert.ticketing.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TicketBookingFacadeTest {

    private Path dir;
    private ConcertWorld world;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dir = tempDir;
        world = new ConcertWorld(dir);
    }

    @Test
    void happyPathCompletesAllThreeSteps() {
        Order order = world.whenBuying("Anna", "A1");

        world.thenOrderIs(order.getId(), Order.Status.COMPLETED);
        world.thenReservationIs(order.getId(), Reservation.Status.CONFIRMED);
        world.thenTicketIs(order.getId(), Ticket.Status.PRINTED);
        world.thenPassIs(order.getId(), AccessPass.Status.ACTIVE);
        world.thenSeatIsTaken("A1");
        assertEquals(80, order.getPrice());
    }

    @Test
    void whenBFailsAfterA_onlyAIsCompensated() {
        world.givenBuyerIsBlocked("Boris");

        PurchaseFailedException e = world.whenBuyingFails("Boris", "B2");

        String orderId = e.getOrderId();
        world.thenOrderIs(orderId, Order.Status.COMPENSATED);
        world.thenReservationIs(orderId, Reservation.Status.RELEASED);
        world.thenNoTicketFor(orderId);
        world.thenNoPassFor(orderId);
        world.thenSeatIsFree("B2");
        assertTrue(world.orders.findById(orderId).getNote().contains("Step B failed"));
    }

    @Test
    void whenCFailsAfterAAndB_bThenAAreCompensated() {
        world.givenGateIsClosed();

        PurchaseFailedException e = world.whenBuyingFails("Clara", "C3");

        String orderId = e.getOrderId();
        world.thenOrderIs(orderId, Order.Status.COMPENSATED);
        world.thenTicketIs(orderId, Ticket.Status.VOIDED);
        world.thenReservationIs(orderId, Reservation.Status.RELEASED);
        world.thenNoPassFor(orderId);
        world.thenSeatIsFree("C3");
        assertTrue(world.orders.findById(orderId).getNote().contains("Step C failed"));
    }

    @Test
    void whenCFailsAfterAAndB_ticketIsVoidedBeforeReservationIsReleased() {
        List<String> calls = new ArrayList<>();
        SeatingChart recordingSeating = new SeatingChart(new ReservationRepository(dir), new DynamicPricingEngine()) {
            @Override
            public void release(String reservationId) {
                calls.add("release reservation");
                super.release(reservationId);
            }
        };
        TicketPrinter recordingPrinter = new TicketPrinter(new TicketRepository(dir), new FraudChecker()) {
            @Override
            public void voidTicket(String ticketId) {
                calls.add("void ticket");
                super.voidTicket(ticketId);
            }
        };
        TicketBookingFacade facade = new TicketBookingFacade(recordingSeating, recordingPrinter, world.gate, world.orders);
        world.givenGateIsClosed();

        assertThrows(PurchaseFailedException.class, () -> facade.buyTicket(ConcertWorld.CONCERT, "C3", "Clara"));

        assertEquals(List.of("void ticket", "release reservation"), calls);
    }

    @Test
    void whenBFailsAfterA_ticketIsNeverVoidedBecauseNoneWasPrinted() {
        List<String> calls = new ArrayList<>();
        TicketPrinter recordingPrinter = new TicketPrinter(new TicketRepository(dir), new FraudChecker()) {
            @Override
            public void voidTicket(String ticketId) {
                calls.add("void ticket");
                super.voidTicket(ticketId);
            }
        };
        TicketBookingFacade facade = new TicketBookingFacade(world.seating, recordingPrinter, world.gate, world.orders);
        world.givenBuyerIsBlocked("Boris");

        assertThrows(PurchaseFailedException.class, () -> facade.buyTicket(ConcertWorld.CONCERT, "B2", "Boris"));

        assertTrue(calls.isEmpty(), "only A must be compensated when B fails");
    }

    @Test
    void seatTakenAtStepA_rejectsOrderWithNothingToUndo() {
        world.whenBuying("Anna", "A1");

        PurchaseFailedException e = world.whenBuyingFails("Dina", "A1");

        world.thenOrderIs(e.getOrderId(), Order.Status.REJECTED);
        world.thenNoTicketFor(e.getOrderId());
        world.thenNoPassFor(e.getOrderId());
    }

    @Test
    void unknownConcertOrSeatIsRejectedAtStepA() {
        PurchaseFailedException unknownConcert = assertThrows(PurchaseFailedException.class,
                () -> world.booking.buyTicket("NO-SUCH-CONCERT", "A1", "Anna"));
        PurchaseFailedException unknownSeat = assertThrows(PurchaseFailedException.class,
                () -> world.booking.buyTicket(ConcertWorld.CONCERT, "Z9", "Anna"));

        world.thenOrderIs(unknownConcert.getOrderId(), Order.Status.REJECTED);
        world.thenOrderIs(unknownSeat.getOrderId(), Order.Status.REJECTED);
        world.thenNoTicketFor(unknownSeat.getOrderId());
        world.thenNoPassFor(unknownSeat.getOrderId());
    }

    @Test
    void invalidInputIsRejectedBeforeAnythingIsSaved() {
        assertThrows(InvalidRequestException.class, () -> world.booking.buyTicket(ConcertWorld.CONCERT, "A1", "x"));
        assertThrows(InvalidRequestException.class, () -> world.booking.buyTicket(ConcertWorld.CONCERT, "A1", null));
        assertThrows(InvalidRequestException.class, () -> world.booking.buyTicket(ConcertWorld.CONCERT, "", "Anna"));
        assertThrows(InvalidRequestException.class, () -> world.booking.buyTicket("", "A1", "Anna"));
        assertTrue(world.orders.findAll().isEmpty());
    }

    @Test
    void fifthTicketOfSameBuyerFailsAtStepB() {
        world.whenBuying("Eva", "D1");
        world.whenBuying("Eva", "D2");
        world.whenBuying("Eva", "D3");
        world.whenBuying("Eva", "D4");

        PurchaseFailedException e = world.whenBuyingFails("Eva", "D5");

        world.thenOrderIs(e.getOrderId(), Order.Status.COMPENSATED);
        world.thenSeatIsFree("D5");
    }

    @Test
    void freeSeatsShrinkAfterPurchase() {
        int before = world.booking.showFreeSeats(ConcertWorld.CONCERT).size();
        world.whenBuying("Anna", "A1");
        assertEquals(before - 1, world.booking.showFreeSeats(ConcertWorld.CONCERT).size());
    }

    @Test
    void commaInConcertOrSeatIsRejectedAndDoesNotCorruptStorage() {
        assertThrows(InvalidRequestException.class, () -> world.booking.buyTicket("FOO,BAR", "A1", "Anna"));
        assertThrows(InvalidRequestException.class, () -> world.booking.buyTicket(ConcertWorld.CONCERT, "A1,x", "Anna"));
        assertThrows(InvalidRequestException.class, () -> world.booking.buyTicket(ConcertWorld.CONCERT, "A1", "An,na"));

        world.restartApplication();
        assertTrue(world.orders.findAll().isEmpty());
        assertEquals(0, world.admin.recoverUnfinishedOrders());
    }

    @Test
    void whenCFailsAndRollbackFailsToo_orderStaysPendingAndRecoveryFinishesIt() {
        TicketPrinter brokenPrinter = new TicketPrinter(new TicketRepository(dir), new FraudChecker()) {
            @Override
            public void voidTicket(String ticketId) {
                throw new IllegalStateException("printer offline");
            }
        };
        TicketBookingFacade facade = new TicketBookingFacade(world.seating, brokenPrinter, world.gate, world.orders);
        world.givenGateIsClosed();

        PurchaseFailedException e = assertThrows(PurchaseFailedException.class,
                () -> facade.buyTicket(ConcertWorld.CONCERT, "C3", "Clara"));

        String orderId = e.getOrderId();
        world.thenOrderIs(orderId, Order.Status.PENDING_C);
        assertTrue(world.orders.findById(orderId).getNote().contains("Compensation failed"));
        world.thenSeatIsTaken("C3");

        world.restartApplication();
        assertEquals(1, world.admin.recoverUnfinishedOrders());

        world.thenOrderIs(orderId, Order.Status.COMPENSATED);
        world.thenTicketIs(orderId, Ticket.Status.VOIDED);
        world.thenReservationIs(orderId, Reservation.Status.RELEASED);
        world.thenSeatIsFree("C3");
    }

    @Test
    void subsystemsReportFailuresWithTheirOwnExceptionTypes() {
        world.whenBuying("Anna", "A1");
        world.givenBuyerIsBlocked("Boris").givenGateIsClosed();

        assertThrows(concert.seating.SeatUnavailableException.class,
                () -> world.seating.holdSeat(ConcertWorld.CONCERT, "A1", "X"));
        assertThrows(concert.ticketing.BuyerRejectedException.class,
                () -> world.printer.printTicket(ConcertWorld.CONCERT, "B1", "Boris", "X"));
        assertThrows(concert.access.GateClosedException.class,
                () -> world.gate.registerPass(ConcertWorld.CONCERT, "B1", "X"));
    }
}
