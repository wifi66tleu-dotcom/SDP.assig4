package concert.access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AccessPassRepository {

    private final Path passesFile;
    private final Path closedFile;

    public AccessPassRepository(Path dataDir) {
        this.passesFile = dataDir.resolve("passes.csv");
        this.closedFile = dataDir.resolve("closed_gates.csv");
    }

    public List<AccessPass> findAll() {
        List<AccessPass> result = new ArrayList<>();
        for (String line : readLines(passesFile)) {
            String[] p = line.split(",", -1);
            result.add(new AccessPass(p[0], p[1], p[2], p[3], AccessPass.Status.valueOf(p[4])));
        }
        return result;
    }

    public AccessPass findById(String id) {
        for (AccessPass p : findAll()) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    public AccessPass findByOrderRef(String orderRef) {
        for (AccessPass p : findAll()) {
            if (p.getOrderRef().equals(orderRef)) {
                return p;
            }
        }
        return null;
    }

    public void save(AccessPass pass) {
        List<AccessPass> all = findAll();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(pass.getId())) {
                all.set(i, pass);
                replaced = true;
            }
        }
        if (!replaced) {
            all.add(pass);
        }

        List<String> lines = new ArrayList<>();
        for (AccessPass p : all) {
            lines.add(p.getId() + "," + p.getConcertId() + "," + p.getSeatId() + ","
                    + p.getOrderRef() + "," + p.getStatus());
        }
        writeLines(passesFile, lines);
    }

    public String nextId() {

        return "P-" + (findAll().size() + 1);
    }

    public boolean isGateClosed(String concertId) {

        return readLines(closedFile).contains(concertId);
    }

    public void setGateClosed(String concertId, boolean closed) {
        List<String> closedGates = readLines(closedFile);
        if (closed && !closedGates.contains(concertId)) {
            closedGates.add(concertId);
        }
        if (!closed) {
            closedGates.remove(concertId);
        }
        writeLines(closedFile, closedGates);
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
