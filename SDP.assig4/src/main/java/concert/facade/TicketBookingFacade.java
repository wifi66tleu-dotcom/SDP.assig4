package concert.facade;
import concert.access.AccessControlGate;
import concert.access.AccessPass;
import concert.access.GateClosedException;
import concert.seating.Reservation;
import concert.seating.SeatUnavailableException;
import concert.seating.SeatingChart;
import concert.ticketing.BuyerRejectedException;
import concert.ticketing.Ticket;
import concert.ticketing.TicketPrinter;
import java.io.UncheckedIOException;
import java.util.List;

public class TicketBookingFacade {

    private final SeatingChart seating;
    private final TicketPrinter printer;
    private final AccessControlGate gate;
    private final OrderRepository orders;
    private final ChainCompensator compensator;

    public TicketBookingFacade(SeatingChart seating, TicketPrinter printer, AccessControlGate gate, OrderRepository orders) {
        this.seating = seating;
        this.printer = printer;
        this.gate = gate;
        this.orders = orders;
        this.compensator = new ChainCompensator(seating, printer, gate);
    }

    public Order buyTicket(String concertId, String seatId, String buyer) {
        checkInput(concertId, seatId, buyer);
        Order order = new Order(orders.nextId(), concertId, seatId, buyer);
        orders.save(order);

        Reservation reservation;
        try {
            reservation = seating.holdSeat(concertId, seatId, order.getId());
        } catch (SeatUnavailableException e) {
            order.setStatus(Order.Status.REJECTED);
            order.setNote("Step A failed: " + e.getMessage());
            orders.save(order);
            throw new PurchaseFailedException(order.getId(), order.getNote());
        }
        order.setReservationId(reservation.getId());
        order.setPrice(reservation.getPrice());
        order.setStatus(Order.Status.PENDING_B);
        orders.save(order);

        Ticket ticket;
        try {
            ticket = printer.printTicket(concertId, seatId, buyer, order.getId());
        } catch (BuyerRejectedException | UncheckedIOException e) {
            throw failAndCompensate(order, "Step B failed: " + e.getMessage());
        }
        order.setTicketId(ticket.getId());
        order.setStatus(Order.Status.PENDING_C);
        orders.save(order);

        AccessPass pass;
        try {
            pass = gate.registerPass(concertId, seatId, order.getId());
        } catch (GateClosedException | UncheckedIOException e) {
            throw failAndCompensate(order, "Step C failed: " + e.getMessage());
        }
        order.setPassId(pass.getId());

        seating.confirm(reservation.getId());
        order.setStatus(Order.Status.COMPLETED);
        order.setNote("Ticket " + ticket.getId() + " and pass " + pass.getId() + " issued");
        orders.save(order);
        return order;
    }

    public List<String> showFreeSeats(String concertId) {

        return seating.getFreeSeats(concertId);
    }

    private PurchaseFailedException failAndCompensate(Order order, String reason) {
        try {
            String undone = compensator.undoWhatIsOnDisk(order.getId());
            order.setStatus(Order.Status.COMPENSATED);
            order.setNote(reason + ". " + undone);
        } catch (RuntimeException rollbackError) {
            order.setNote(reason + ". Compensation failed (" + rollbackError.getMessage()
                    + "), the order stays " + order.getStatus() + " and will be retried by recovery");
        }
        orders.save(order);
        return new PurchaseFailedException(order.getId(), order.getNote());
    }

    private void checkInput(String concertId, String seatId, String buyer) {
        if (concertId == null || !concertId.matches("[A-Za-z0-9-]{1,30}")) {
            throw new InvalidRequestException("Concert name must be 1-30 letters, digits or '-'");
        }
        if (seatId == null || !seatId.matches("[A-Za-z0-9]{1,5}")) {
            throw new InvalidRequestException("Seat must be 1-5 letters or digits, for example A1");
        }
        if (buyer == null || !buyer.matches("[A-Za-z0-9 ]{2,30}")) {
            throw new InvalidRequestException("Buyer name must be 2-30 letters, digits or spaces");
        }
    }
}
