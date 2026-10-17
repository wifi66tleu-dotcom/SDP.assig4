package concert.ticketing;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TicketRepository {

    private final Path ticketsFile;
    private final Path blockedFile;

    public TicketRepository(Path dataDir) {
        this.ticketsFile = dataDir.resolve("tickets.csv");
        this.blockedFile = dataDir.resolve("blocked_buyers.csv");
    }

    public List<Ticket> findAll() {
        List<Ticket> result = new ArrayList<>();
        for (String line : readLines(ticketsFile)) {
            String[] p = line.split(",", -1);
            result.add(new Ticket(p[0], p[1], p[2], p[3], p[4], Ticket.Status.valueOf(p[5])));
        }
        return result;
    }

    public Ticket findById(String id) {
        for (Ticket t : findAll()) {
            if (t.getId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    public Ticket findByOrderRef(String orderRef) {
        for (Ticket t : findAll()) {
            if (t.getOrderRef().equals(orderRef)) {
                return t;
            }
        }
        return null;
    }

    public int countValidTickets(String buyer, String concertId) {
        int count = 0;
        for (Ticket t : findAll()) {
            if (t.getBuyer().equalsIgnoreCase(buyer) && t.getConcertId().equals(concertId) && t.isValid()) {
                count++;
            }
        }
        return count;
    }

    public void save(Ticket ticket) {
        List<Ticket> all = findAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(ticket.getId())) {
                all.set(i, ticket);
                replaced = true;
            }
        }
        if (!replaced) {
            all.add(ticket);
        }

        List<String> lines = new ArrayList<>();
        for (Ticket t : all) {
            lines.add(t.getId() + "," + t.getConcertId() + "," + t.getSeatId() + ","
                    + t.getBuyer() + "," + t.getOrderRef() + "," + t.getStatus());
        }
        writeLines(ticketsFile, lines);
    }

    public String nextId() {

        return "T-" + (findAll().size() + 1);
    }

    public boolean isBlocked(String buyer) {

        return readLines(blockedFile).contains(buyer.toLowerCase());
    }

    public void block(String buyer) {
        List<String> names = readLines(blockedFile);
        if (!names.contains(buyer.toLowerCase())) {
            names.add(buyer.toLowerCase());
            writeLines(blockedFile, names);
        }
    }

    private List<String> readLines(Path file) {
        List<String> result = new ArrayList<>();
        if (!Files.exists(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (!line.isBlank()) {
                    result.add(line);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return result;
    }

    private void writeLines(Path file, List<String> lines) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, lines);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
