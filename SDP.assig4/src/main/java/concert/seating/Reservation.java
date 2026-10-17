package concert.seating;
public class Reservation {
    public enum Status {
        HELD,
        CONFIRMED,
        RELEASED
    }

    private final String id;
    private final String concertId;
    private final String seatId;
    private final String orderRef;
    private final int price;
    private Status status;

    public Reservation(String id, String concertId, String seatId, String orderRef, int price, Status status) {
        this.id = id;
        this.concertId = concertId;
        this.seatId = seatId;
        this.orderRef = orderRef;
        this.price = price;
        this.status = status;
    }

    public void confirm() {
        if (status != Status.HELD) {
            throw new IllegalStateException("Reservation confirmation failed  " + id + " because it is " + status);
        }
        status = Status.CONFIRMED;
    }

    public void release() {
        if (status == Status.RELEASED) {
            throw new IllegalStateException("Reservation " + id + " is already released");
        }
        status = Status.RELEASED;
    }
    public boolean blocksSeat() {

        return status != Status.RELEASED;
    }
    public String getId() { return id; }
    public String getConcertId() { return concertId; }
    public String getSeatId() { return seatId; }
    public String getOrderRef() { return orderRef; }
    public int getPrice() { return price; }
    public Status getStatus() { return status; }
}
