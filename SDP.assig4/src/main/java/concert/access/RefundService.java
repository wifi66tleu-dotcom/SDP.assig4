package concert.access;
public class RefundService {

    public int calculateRefund(int price) {
        if (price <= 0) {
            return 0;
        }
        int fee = Math.max(1, price / 10);
        return price - fee;
    }
}
