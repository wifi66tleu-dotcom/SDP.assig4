package concert.access;
public class AccessPass {

    public enum Status {
        PENDING,
        ACTIVE,
        REVOKED
    }

    private final String id;
    private final String concertId;
    private final String seatId;
    private final String orderRef;
    private Status status;

    public AccessPass(String id, String concertId, String seatId, String orderRef, Status status) {
        this.id = id;
        this.concertId = concertId;
        this.seatId = seatId;
        this.orderRef = orderRef;
        this.status = status;
    }

    public void activate() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Cannot activate pass " + id + " because it is " + status);
        }
        status = Status.ACTIVE;
    }

    public void revoke() {
        if (status == Status.REVOKED) {
            throw new IllegalStateException("Pass " + id + " is already revoked");
        }
        status = Status.REVOKED;
    }

    public String getId() { return id; }
    public String getConcertId() { return concertId; }
    public String getSeatId() { return seatId; }
    public String getOrderRef() { return orderRef; }
    public Status getStatus() { return status; }
}
