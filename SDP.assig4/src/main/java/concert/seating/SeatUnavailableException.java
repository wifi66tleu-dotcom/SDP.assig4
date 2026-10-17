package concert.seating;
public class SeatUnavailableException extends IllegalStateException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
