package concert.facade;
public class Order {

    public enum Status {
        PENDING_A,
        PENDING_B,
        PENDING_C,
        COMPLETED,
        COMPENSATED,
        REJECTED,
        REFUNDED
    }

    private final String id;
    private final String concertId;
    private final String seatId;
    private final String buyer;
    private int price;
    private Status status;
    private String reservationId;
    private String ticketId;
    private String passId;
    private int refund;
    private String note = "";

    public Order(String id, String concertId, String seatId, String buyer) {
        this.id = id;
        this.concertId = concertId;
        this.seatId = seatId;
        this.buyer = buyer;
        this.status = Status.PENDING_A;
    }

    public Order(String id, String concertId, String seatId, String buyer, int price, Status status,
                 String reservationId, String ticketId, String passId, int refund, String note) {
        this.id = id;
        this.concertId = concertId;
        this.seatId = seatId;
        this.buyer = buyer;
        this.price = price;
        this.status = status;
        this.reservationId = reservationId;
        this.ticketId = ticketId;
        this.passId = passId;
        this.refund = refund;
        this.note = note;
    }

    public boolean isUnfinished() {
        return status == Status.PENDING_A || status == Status.PENDING_B || status == Status.PENDING_C;
    }

    public String getId() { return id; }
    public String getConcertId() { return concertId; }
    public String getSeatId() { return seatId; }
    public String getBuyer() { return buyer; }
    public int getPrice() { return price; }
    public Status getStatus() { return status; }
    public String getReservationId() { return reservationId; }
    public String getTicketId() { return ticketId; }
    public String getPassId() { return passId; }
    public int getRefund() { return refund; }
    public String getNote() { return note; }

    public void setPrice(int price) { this.price = price; }
    public void setStatus(Status status) { this.status = status; }
    public void setReservationId(String reservationId) { this.reservationId = reservationId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }
    public void setPassId(String passId) { this.passId = passId; }
    public void setRefund(int refund) { this.refund = refund; }
    public void setNote(String note) { this.note = note.replace(",", ";"); }
}
