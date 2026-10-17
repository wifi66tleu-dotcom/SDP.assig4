package concert.seating;
public class DynamicPricingEngine {

    public int calculatePrice(String seatId, int seatsTaken, int totalSeats) {
        int price = basePrice(seatId.charAt(0));
        int percentTaken = seatsTaken * 100 / totalSeats;
        if (percentTaken >= 75) {
            price = price * 130 / 100;
        } else if (percentTaken >= 50) {
            price = price * 115 / 100;
        }
        return price;
    }

    private int basePrice(char row) {
        if (row == 'A') {
            return 80;
        }
        if (row == 'B') {
            return 60;
        }
        return 40;
    }
}
