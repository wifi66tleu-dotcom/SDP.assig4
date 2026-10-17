package concert.facade;

import concert.access.AccessControlGate;
import concert.access.AccessPass;
import concert.seating.Reservation;
import concert.seating.SeatingChart;
import concert.ticketing.Ticket;
import concert.ticketing.TicketPrinter;

class ChainCompensator {

    private final SeatingChart seating;
    private final TicketPrinter printer;
    private final AccessControlGate gate;

    ChainCompensator(SeatingChart seating, TicketPrinter printer, AccessControlGate gate) {
        this.seating = seating;
        this.printer = printer;
        this.gate = gate;
    }

    String undoWhatIsOnDisk(String orderId) {
        StringBuilder undone = new StringBuilder();

        AccessPass pass = gate.findPassByOrder(orderId);
        if (pass != null && pass.getStatus() != AccessPass.Status.REVOKED) {
            gate.revokePass(pass.getId());
            undone.append("Revoked pass ").append(pass.getId()).append(". ");
        }

        Ticket ticket = printer.findTicketByOrder(orderId);
        if (ticket != null && ticket.isValid()) {
            printer.voidTicket(ticket.getId());
            undone.append("Voided ticket ").append(ticket.getId()).append(". ");
        }

        Reservation reservation = seating.findReservationByOrder(orderId);
        if (reservation != null && reservation.blocksSeat()) {
            seating.release(reservation.getId());
            undone.append("Released reservation ").append(reservation.getId()).append(". ");
        }

        if (undone.length() == 0) {
            undone.append("Nothing was committed.");
        }
        return undone.toString().trim();
    }
}
