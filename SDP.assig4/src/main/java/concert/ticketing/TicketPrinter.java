package concert.ticketing;
public class TicketPrinter {
    private final TicketRepository repository;
    private final FraudChecker fraudChecker;

    public TicketPrinter(TicketRepository repository, FraudChecker fraudChecker) {
        this.repository = repository;
        this.fraudChecker = fraudChecker;
    }
    public Ticket printTicket(String concertId, String seatId, String buyer, String orderRef) {
        if (buyer == null || buyer.isBlank() || buyer.contains(",")) {
            throw new BuyerRejectedException("Buyer name is not valid");
        }

        boolean blocked = repository.isBlocked(buyer);
        int alreadyBought = repository.countValidTickets(buyer, concertId);
        fraudChecker.check(buyer, blocked, alreadyBought);

        Ticket ticket = new Ticket(repository.nextId(), concertId, seatId, buyer, orderRef, Ticket.Status.NEW);
        ticket.print();
        repository.save(ticket);
        return ticket;
    }

    public void voidTicket(String ticketId) {
        Ticket ticket = repository.findById(ticketId);
        if (ticket == null) {
            throw new IllegalStateException("Ticket " + ticketId + " not found");
        }
        ticket.voidTicket();
        repository.save(ticket);
    }

    public Ticket findTicket(String ticketId) {

        return repository.findById(ticketId);
    }

    public Ticket findTicketByOrder(String orderRef) {

        return repository.findByOrderRef(orderRef);
    }

    public void blockBuyer(String buyer) {

        repository.block(buyer);
    }
}
