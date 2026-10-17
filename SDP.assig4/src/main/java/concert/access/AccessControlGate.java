package concert.access;
public class AccessControlGate {

    private final AccessPassRepository repository;
    private final RefundService refundService;

    public AccessControlGate(AccessPassRepository repository, RefundService refundService) {
        this.repository = repository;
        this.refundService = refundService;
    }

    public AccessPass registerPass(String concertId, String seatId, String orderRef) {
        if (repository.isGateClosed(concertId)) {
            throw new GateClosedException("Gate for " + concertId + " is closed");
        }
        AccessPass pass = new AccessPass(repository.nextId(), concertId, seatId, orderRef, AccessPass.Status.PENDING);
        pass.activate();
        repository.save(pass);
        return pass;
    }

    public void revokePass(String passId) {
        AccessPass pass = repository.findById(passId);
        if (pass == null) {
            throw new IllegalStateException("Pass " + passId + " not found");
        }
        pass.revoke();
        repository.save(pass);
    }

    public AccessPass findPass(String passId) {

        return repository.findById(passId);
    }

    public AccessPass findPassByOrder(String orderRef) {

        return repository.findByOrderRef(orderRef);
    }

    public void closeGate(String concertId) {

        repository.setGateClosed(concertId, true);
    }

    public void openGate(String concertId) {

        repository.setGateClosed(concertId, false);
    }

    public boolean isGateClosed(String concertId) {

        return repository.isGateClosed(concertId);
    }

    public int refundFor(int price) {

        return refundService.calculateRefund(price);
    }
}
