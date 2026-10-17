package concert.facade;
import concert.access.AccessPass;
import concert.seating.Reservation;
import concert.ticketing.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class BoxOfficeAdminFacadeTest {

    private ConcertWorld world;

    @BeforeEach
    void setUp(@TempDir Path dir) {

        world = new ConcertWorld(dir);
    }

    @Test
    void refundCancelsEverythingAndReturnsMoneyMinusFee() {
        Order order = world.whenBuying("Anna", "A1"); // price 80

        int refund = world.admin.refundOrder(order.getId());

        assertEquals(72, refund);
        world.thenOrderIs(order.getId(), Order.Status.REFUNDED);
        world.thenPassIs(order.getId(), AccessPass.Status.REVOKED);
        world.thenTicketIs(order.getId(), Ticket.Status.VOIDED);
        world.thenReservationIs(order.getId(), Reservation.Status.RELEASED);
        world.thenSeatIsFree("A1");
    }

    @Test
    void orderCannotBeRefundedTwice() {
        Order order = world.whenBuying("Anna", "A1");
        world.admin.refundOrder(order.getId());

        assertThrows(InvalidRequestException.class, () -> world.admin.refundOrder(order.getId()));
    }

    @Test
    void refundOfNotCompletedOrderIsRejected() {
        world.givenGateIsClosed();
        String orderId = world.whenBuyingFails("Clara", "C1").getOrderId();

        assertThrows(InvalidRequestException.class, () -> world.admin.refundOrder(orderId));
    }

    @Test
    void manualCompensationOfCompletedOrderIsRejected() {
        Order order = world.whenBuying("Anna", "A1");
        assertThrows(InvalidRequestException.class, () -> world.admin.compensateOrder(order.getId()));
    }

    @Test
    void unknownOrderIsRejected() {
        assertThrows(InvalidRequestException.class, () -> world.admin.inspect("ORD-99"));
        assertThrows(InvalidRequestException.class, () -> world.admin.refundOrder("ORD-99"));
    }

    @Test
    void manualCompensationFixesStuckOrder() {
        Order order = new Order(world.orders.nextId(), ConcertWorld.CONCERT, "B1", "Boris");
        world.seating.holdSeat(ConcertWorld.CONCERT, "B1", order.getId());
        order.setStatus(Order.Status.PENDING_B);
        world.orders.save(order);

        world.admin.compensateOrder(order.getId());

        world.thenOrderIs(order.getId(), Order.Status.COMPENSATED);
        world.thenSeatIsFree("B1");
    }

    @Test
    void stuckOrderWithNothingCommittedIsCompensatedToo() {
        Order order = new Order(world.orders.nextId(), ConcertWorld.CONCERT, "B1", "Boris");
        world.orders.save(order);

        world.admin.compensateOrder(order.getId());

        assertTrue(world.orders.findById(order.getId()).getNote().contains("Nothing was committed"));
    }

    @Test
    void inspectShowsStateOfAllThreeSubsystems() {
        Order order = world.whenBuying("Anna", "A1");

        String report = world.admin.inspect(order.getId());

        assertTrue(report.contains("COMPLETED"));
        assertTrue(report.contains("CONFIRMED"));
        assertTrue(report.contains("PRINTED"));
        assertTrue(report.contains("ACTIVE"));
    }

    @Test
    void inspectShowsNoneForMissingParts() {
        Order order = new Order(world.orders.nextId(), ConcertWorld.CONCERT, "B1", "Boris");
        world.orders.save(order);

        assertTrue(world.admin.inspect(order.getId()).contains("(none)"));
    }

    @Test
    void openingGateAgainAllowsPurchases() {
        world.givenGateIsClosed();
        world.whenBuyingFails("Clara", "C1");

        world.admin.openGate(ConcertWorld.CONCERT);
        Order order = world.whenBuying("Clara", "C1");

        world.thenOrderIs(order.getId(), Order.Status.COMPLETED);
    }

    @Test
    void listOrdersReturnsAllOrders() {
        world.whenBuying("Anna", "A1");
        world.whenBuyingFails("Anna", "A1");
        assertEquals(2, world.admin.listOrders().size());
    }
}
