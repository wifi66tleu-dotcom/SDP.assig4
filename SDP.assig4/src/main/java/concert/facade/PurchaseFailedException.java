package concert.facade;
public class PurchaseFailedException extends RuntimeException {
    private final String orderId;
    public PurchaseFailedException(String orderId, String message) {
        super(message);
        this.orderId = orderId;
    }
    public String getOrderId() {

        return orderId;
    }
}
