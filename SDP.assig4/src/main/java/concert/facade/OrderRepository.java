package concert.facade;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
public class OrderRepository {

    private final Path file;

    public OrderRepository(Path dataDir) {

        this.file = dataDir.resolve("orders.csv");
    }

    public List<Order> findAll() {
        List<Order> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] p = line.split(",", -1);
                result.add(new Order(p[0], p[1], p[2], p[3], Integer.parseInt(p[4]),
                        Order.Status.valueOf(p[5]), emptyToNull(p[6]), emptyToNull(p[7]),
                        emptyToNull(p[8]), Integer.parseInt(p[9]), p[10]));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    public Order findById(String id) {
        for (Order o : findAll()) {
            if (o.getId().equals(id)) {
                return o;
            }
        }
        return null;
    }

    public void save(Order order) {
        List<Order> all = findAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(order.getId())) {
                all.set(i, order);
                replaced = true;
            }
        }
        if (!replaced) {
            all.add(order);
        }

        List<String> lines = new ArrayList<>();
        for (Order o : all) {
            lines.add(o.getId() + "," + o.getConcertId() + "," + o.getSeatId() + "," + o.getBuyer() + ","
                    + o.getPrice() + "," + o.getStatus() + "," + nullToEmpty(o.getReservationId()) + ","
                    + nullToEmpty(o.getTicketId()) + "," + nullToEmpty(o.getPassId()) + ","
                    + o.getRefund() + "," + o.getNote());
        }
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String nextId() {

        return "ORD-" + (findAll().size() + 1);
    }

    private String emptyToNull(String text) {

        return text.isEmpty() ? null : text;
    }

    private String nullToEmpty(String text) {

        return text == null ? "" : text;
    }
}
