package concert.access;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class AccessControlGateTest {

    private Path dir;
    private AccessControlGate gate;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        dir = tempDir;
        gate = new AccessControlGate(new AccessPassRepository(dir), new RefundService());
    }

    @Test
    void registeredPassIsActive() {
        AccessPass pass = gate.registerPass("MUSIC-NIGHT", "A1", "ORD-1");
        assertEquals(AccessPass.Status.ACTIVE, pass.getStatus());
        assertEquals("P-1", pass.getId());
    }

    @Test
    void closedGateRejectsPassAndSavesNothing() {
        gate.closeGate("MUSIC-NIGHT");
        assertThrows(IllegalStateException.class, () -> gate.registerPass("MUSIC-NIGHT", "A1", "ORD-1"));
        assertNull(gate.findPassByOrder("ORD-1"));
    }

    @Test
    void closedGateOnlyAffectsItsOwnConcert() {
        gate.closeGate("MUSIC-NIGHT");
        assertDoesNotThrow(() -> gate.registerPass("SUNSET-CONCERT", "A1", "ORD-1"));
    }

    @Test
    void reopenedGateWorksAgain() {
        gate.closeGate("MUSIC-NIGHT");
        gate.closeGate("MUSIC-NIGHT"); // closing twice is fine
        gate.openGate("MUSIC-NIGHT");
        assertFalse(gate.isGateClosed("MUSIC-NIGHT"));
        assertDoesNotThrow(() -> gate.registerPass("MUSIC-NIGHT", "A1", "ORD-1"));
    }

    @Test
    void revokedPassCannotBeRevokedAgain() {
        AccessPass pass = gate.registerPass("MUSIC-NIGHT", "A1", "ORD-1");
        gate.revokePass(pass.getId());

        assertEquals(AccessPass.Status.REVOKED, gate.findPass(pass.getId()).getStatus());
        assertThrows(IllegalStateException.class, () -> gate.revokePass(pass.getId()));
    }

    @Test
    void revokedPassCannotBeActivatedAgain() {
        AccessPass pass = gate.registerPass("MUSIC-NIGHT", "A1", "ORD-1");
        gate.revokePass(pass.getId());

        AccessPass revoked = gate.findPass(pass.getId());

        assertThrows(IllegalStateException.class, revoked::activate);
    }

    @Test
    void unknownPassCannotBeRevoked() {
        assertThrows(IllegalStateException.class, () -> gate.revokePass("P-99"));
    }

    @Test
    void passAndClosedGateAreReadBackAfterRestart() {
        gate.registerPass("MUSIC-NIGHT", "A1", "ORD-1");
        gate.closeGate("SUNSET-CONCERT");

        AccessControlGate restarted = new AccessControlGate(new AccessPassRepository(dir), new RefundService());

        assertEquals(AccessPass.Status.ACTIVE, restarted.findPassByOrder("ORD-1").getStatus());
        assertTrue(restarted.isGateClosed("SUNSET-CONCERT"));
    }
}
