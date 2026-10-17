package concert.ticketing;
public class FraudChecker {

    public static final int MAX_TICKETS_PER_BUYER = 4;

    public void check(String buyer, boolean isBlocked, int ticketsAlreadyBought) {
        if (isBlocked) {
            throw new BuyerRejectedException("Buyer " + buyer + " is on the fraud blocklist");
        }
        if (ticketsAlreadyBought >= MAX_TICKETS_PER_BUYER) {
            throw new BuyerRejectedException("Buyer " + buyer + " already has " + ticketsAlreadyBought
                    + " tickets for this concert (max " + MAX_TICKETS_PER_BUYER + ")");
        }
    }
}
