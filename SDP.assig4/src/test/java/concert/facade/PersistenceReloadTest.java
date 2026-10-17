package concert.facade;
import concert.access.AccessPass;
import concert.seating.Reservation;
import concert.ticketing.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PersistenceReloadTest {

    private ConcertWorld world;

    @BeforeEach
    void setUp(@TempDir Path dir) {

        world = new ConcertWorld(dir);
    }

    @Test
    void completedOrderSurvivesRestart() {
        Order order = world.whenBuying("Anna", "A1");

        world.restartApplication();

        world.thenOrderIs(order.getId(), Order.Status.COMPLETED);
        world.thenSeatIsTaken("A1");
        world.thenPassIs(order.getId(), AccessPass.Status.ACTIVE);
    }

    @Test
    void crashAfterA_restartCompensatesOnlyA() {
        Order order = new Order(world.orders.nextId(), ConcertWorld.CONCERT, "B1", "Boris");
        world.seating.holdSeat(ConcertWorld.CONCERT, "B1", order.getId());
        order.setStatus(Order.Status.PENDING_B);
        world.orders.save(order);

        world.restartApplication();
        int fixed = world.admin.recoverUnfinishedOrders();

        assertEquals(1, fixed);
        world.thenOrderIs(order.getId(), Order.Status.COMPENSATED);
        world.thenReservationIs(order.getId(), Reservation.Status.RELEASED);
        world.thenNoTicketFor(order.getId());
        world.thenSeatIsFree("B1");
    }

    @Test
    void crashAfterB_restartCompensatesBAndA() {
        Order order = new Order(world.orders.nextId(), ConcertWorld.CONCERT, "C1", "Clara");
        world.seating.holdSeat(ConcertWorld.CONCERT, "C1", order.getId());
        world.printer.printTicket(ConcertWorld.CONCERT, "C1", "Clara", order.getId());
        order.setStatus(Order.Status.PENDING_C);
        world.orders.save(order);

        world.restartApplication();
        world.admin.recoverUnfinishedOrders();

        world.thenOrderIs(order.getId(), Order.Status.COMPENSATED);
        world.thenTicketIs(order.getId(), Ticket.Status.VOIDED);
        world.thenReservationIs(order.getId(), Reservation.Status.RELEASED);
        world.thenSeatIsFree("C1");
    }

    @Test
    void crashAfterCBeforeConfirm_restartCompensatesCThenBThenA() {
        Order order = new Order(world.orders.nextId(), ConcertWorld.CONCERT, "A2", "Anna");
        world.seating.holdSeat(ConcertWorld.CONCERT, "A2", order.getId());
        world.printer.printTicket(ConcertWorld.CONCERT, "A2", "Anna", order.getId());
        world.gate.registerPass(ConcertWorld.CONCERT, "A2", order.getId());
        order.setStatus(Order.Status.PENDING_C); // the crash happened before COMPLETED was saved
        world.orders.save(order);

        world.restartApplication();
        world.admin.recoverUnfinishedOrders();

        world.thenOrderIs(order.getId(), Order.Status.COMPENSATED);
        world.thenPassIs(order.getId(), AccessPass.Status.REVOKED);
        world.thenTicketIs(order.getId(), Ticket.Status.VOIDED);
        world.thenReservationIs(order.getId(), Reservation.Status.RELEASED);
        world.thenSeatIsFree("A2");
    }

    @Test
    void recoveryUsesDiskNotOrderStatus() {
        Order order = new Order(world.orders.nextId(), ConcertWorld.CONCERT, "D1", "Dina");
        world.seating.holdSeat(ConcertWorld.CONCERT, "D1", order.getId());
        world.printer.printTicket(ConcertWorld.CONCERT, "D1", "Dina", order.getId());
        order.setStatus(Order.Status.PENDING_B);
        world.orders.save(order);

        world.restartApplication();
        world.admin.recoverUnfinishedOrders();

        world.thenTicketIs(order.getId(), Ticket.Status.VOIDED);
        world.thenSeatIsFree("D1");
    }

    @Test
    void recoveryDoesNotTouchCompletedOrders() {
        Order order = world.whenBuying("Anna", "A1");

        world.restartApplication();
        int fixed = world.admin.recoverUnfinishedOrders();

        assertEquals(0, fixed);
        world.thenOrderIs(order.getId(), Order.Status.COMPLETED);
    }

    @Test
    void blocklistAndClosedGateSurviveRestart() {
        world.givenBuyerIsBlocked("Boris").givenGateIsClosed();

        world.restartApplication();

        assertTrue(world.gate.isGateClosed(ConcertWorld.CONCERT));
        world.whenBuyingFails("Boris", "A1");
    }
}
