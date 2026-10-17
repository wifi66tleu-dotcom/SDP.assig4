package concert.seating;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReservationRepository {
    private final Path file;
    public ReservationRepository(Path dataDir) {

        this.file = dataDir.resolve("reservations.csv");
    }

    public List<Reservation> findAll() {
        List<Reservation> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] p = line.split(",", -1);
                result.add(new Reservation(p[0], p[1], p[2], p[3],
                        Integer.parseInt(p[4]), Reservation.Status.valueOf(p[5])));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    public Reservation findById(String id) {
        for (Reservation r : findAll()) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    public Reservation findByOrderRef(String orderRef) {
        for (Reservation r : findAll()) {
            if (r.getOrderRef().equals(orderRef)) {
                return r;
            }
        }
        return null;
    }

    public void save(Reservation reservation) {
        List<Reservation> all = findAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(reservation.getId())) {
                all.set(i, reservation);
                replaced = true;
            }
        }
        if (!replaced) {
            all.add(reservation);
        }
        writeAll(all);
    }

    public String nextId() {
        return "R-" + (findAll().size() + 1);
    }

    private void writeAll(List<Reservation> all) {
        List<String> lines = new ArrayList<>();
        for (Reservation r : all) {
            lines.add(r.getId() + "," + r.getConcertId() + "," + r.getSeatId() + ","
                    + r.getOrderRef() + "," + r.getPrice() + "," + r.getStatus());
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
