package concert.access;
public class GateClosedException extends IllegalStateException {
    public GateClosedException(String message) {

        super(message);
    }
}
