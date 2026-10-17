package concert.facade;
import concert.access.AccessControlGate;
import concert.access.AccessPass;
import concert.seating.Reservation;
import concert.seating.SeatingChart;
import concert.ticketing.Ticket;
import concert.ticketing.TicketPrinter;

import java.util.List;
public class BoxOfficeAdminFacade {

    private final SeatingChart seating;
    private final TicketPrinter printer;
    private final AccessControlGate gate;
    private final OrderRepository orders;
    private final ChainCompensator compensator;

    public BoxOfficeAdminFacade(SeatingChart seating, TicketPrinter printer, AccessControlGate gate, OrderRepository orders) {
        this.seating = seating;
        this.printer = printer;
        this.gate = gate;
        this.orders = orders;
        this.compensator = new ChainCompensator(seating, printer, gate);
    }

    public List<Order> listOrders() {

        return orders.findAll();
    }
    public String inspect(String orderId) {
        Order order = getOrderOrFail(orderId);
        Reservation reservation = seating.findReservationByOrder(orderId);
        Ticket ticket = printer.findTicketByOrder(orderId);
        AccessPass pass = gate.findPassByOrder(orderId);

        StringBuilder text = new StringBuilder();
        text.append("Order ").append(order.getId()).append(": ").append(order.getStatus())
                .append(", buyer=").append(order.getBuyer())
                .append(", concert=").append(order.getConcertId())
                .append(", seat=").append(order.getSeatId())
                .append(", price=").append(order.getPrice())
                .append(", refund=").append(order.getRefund()).append("\n");
        text.append("  A reservation: ")
                .append(reservation == null ? "(none)" : reservation.getId() + " " + reservation.getStatus()).append("\n");
        text.append("  B ticket:      ")
                .append(ticket == null ? "(none)" : ticket.getId() + " " + ticket.getStatus()).append("\n");
        text.append("  C access pass: ")
                .append(pass == null ? "(none)" : pass.getId() + " " + pass.getStatus()).append("\n");
        text.append("  note: ").append(order.getNote());
        return text.toString();
    }

    public void compensateOrder(String orderId) {
        Order order = getOrderOrFail(orderId);
        if (!order.isUnfinished()) {
            throw new InvalidRequestException("Order " + orderId + " is not unfinished, it is " + order.getStatus());
        }
        String undone = compensator.undoWhatIsOnDisk(orderId);
        order.setStatus(Order.Status.COMPENSATED);
        order.setNote("Compensated by box office. " + undone);
        orders.save(order);
    }

    public int recoverUnfinishedOrders() {
        int fixed = 0;
        for (Order order : orders.findAll()) {
            if (order.isUnfinished()) {
                compensateOrder(order.getId());
                fixed++;
            }
        }
        return fixed;
    }

    public int refundOrder(String orderId) {
        Order order = getOrderOrFail(orderId);
        if (order.getStatus() != Order.Status.COMPLETED) {
            throw new InvalidRequestException("Only COMPLETED orders can be refunded, this one is " + order.getStatus());
        }
        String undone = compensator.undoWhatIsOnDisk(orderId);
        int refund = gate.refundFor(order.getPrice());
        order.setRefund(refund);
        order.setStatus(Order.Status.REFUNDED);
        order.setNote("Refunded. " + undone);
        orders.save(order);
        return refund;
    }

    public void blockBuyer(String buyer) {

        printer.blockBuyer(buyer);
    }

    public void closeGate(String concertId) {

        gate.closeGate(concertId);
    }

    public void openGate(String concertId) {

        gate.openGate(concertId);
    }

    private Order getOrderOrFail(String orderId) {
        Order order = orders.findById(orderId);
        if (order == null) {
            throw new InvalidRequestException("Order " + orderId + " not found");
        }
        return order;
    }
}
