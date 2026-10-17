package concert.ticketing;
public class Ticket {
    public enum Status {
        NEW,
        PRINTED,
        VOIDED
    }

    private final String id;
    private final String concertId;
    private final String seatId;
    private final String buyer;
    private final String orderRef;
    private Status status;

    public Ticket(String id, String concertId, String seatId, String buyer, String orderRef, Status status) {
        this.id = id;
        this.concertId = concertId;
        this.seatId = seatId;
        this.buyer = buyer;
        this.orderRef = orderRef;
        this.status = status;
    }
    public void print() {
        if (status != Status.NEW) {
            throw new IllegalStateException("Ticket printing failed " + id + " because it is " + status);
        }
        status = Status.PRINTED;
    }

    public void voidTicket() {
        if (status == Status.VOIDED) {
            throw new IllegalStateException("Ticket " + id + " is already voided");
        }
        status = Status.VOIDED;
    }

    public boolean isValid() {

        return status != Status.VOIDED;
    }

    public String getId() { return id; }
    public String getConcertId() { return concertId; }
    public String getSeatId() { return seatId; }
    public String getBuyer() { return buyer; }
    public String getOrderRef() { return orderRef; }
    public Status getStatus() { return status; }
}
