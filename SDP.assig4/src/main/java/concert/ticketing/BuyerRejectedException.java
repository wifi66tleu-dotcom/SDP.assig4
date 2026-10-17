package concert.ticketing;
public class BuyerRejectedException extends IllegalStateException {
    public BuyerRejectedException(String message) {
        super(message);
    }
}
