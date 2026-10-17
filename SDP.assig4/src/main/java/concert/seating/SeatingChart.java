package concert.seating;
import java.util.ArrayList;
import java.util.List;
public class SeatingChart {

    public static final List<String> CONCERTS = List.of("MUSIC-NIGHT","SUNSET-CONCERT");
    private static final String ROWS = "ABCD";
    private static final int SEATS_PER_ROW = 5;

    private final ReservationRepository repository;
    private final DynamicPricingEngine pricing;

    public SeatingChart(ReservationRepository repository, DynamicPricingEngine pricing) {
        this.repository = repository;
        this.pricing = pricing;
    }

    public Reservation holdSeat(String concertId, String seatId, String orderRef) {
        checkConcert(concertId);
        if (!allSeats().contains(seatId)) {
            throw new SeatUnavailableException("Seat " + seatId + " does not exist in the hall");
        }
        if (!getFreeSeats(concertId).contains(seatId)) {
            throw new SeatUnavailableException("Seat " + seatId + " is already taken for " + concertId);
        }

        int taken = allSeats().size() - getFreeSeats(concertId).size();
        int price = pricing.calculatePrice(seatId, taken, allSeats().size());

        Reservation reservation = new Reservation(repository.nextId(), concertId, seatId,
                orderRef, price, Reservation.Status.HELD);
        repository.save(reservation);
        return reservation;
    }

    public void confirm(String reservationId) {
        Reservation reservation = getOrFail(reservationId);
        reservation.confirm();
        repository.save(reservation);
    }

    public void release(String reservationId) {
        Reservation reservation = getOrFail(reservationId);
        reservation.release();
        repository.save(reservation);
    }

    public Reservation findReservation(String reservationId) {

        return repository.findById(reservationId);
    }

    public Reservation findReservationByOrder(String orderRef) {

        return repository.findByOrderRef(orderRef);
    }

    public List<String> getFreeSeats(String concertId) {
        checkConcert(concertId);
        List<String> free = allSeats();
        for (Reservation r : repository.findAll()) {
            if (r.getConcertId().equals(concertId) && r.blocksSeat()) {
                free.remove(r.getSeatId());
            }
        }
        return free;
    }

    public List<String> allSeats() {
        List<String> seats = new ArrayList<>();
        for (char row : ROWS.toCharArray()) {
            for (int number = 1; number <= SEATS_PER_ROW; number++) {
                seats.add("" + row + number);
            }
        }
        return seats;
    }

    private void checkConcert(String concertId) {
        if (!CONCERTS.contains(concertId)) {
            throw new SeatUnavailableException("Unknown concert " + concertId);
        }
    }

    private Reservation getOrFail(String reservationId) {
        Reservation reservation = repository.findById(reservationId);
        if (reservation == null) {
            throw new IllegalStateException("Reservation " + reservationId + " not found");
        }
        return reservation;
    }
}
