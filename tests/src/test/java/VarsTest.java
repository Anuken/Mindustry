import mindustry.Vars;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VarsTest {

    @BeforeEach
    void setUp() {
        // Reset the static variables before each test
        Vars.lastZoom = -1;
    }

    @Test
    void testMaxPanSpeed() {
        assertEquals(1.3f, Vars.maxPanSpeed);
    }

    @Test
    void testLastZoomDefault() {
        assertEquals(-1, Vars.lastZoom);
    }

    @Test
    void testLastZoomUpdate() {
        Vars.lastZoom = 2.0f;
        assertEquals(2.0f, Vars.lastZoom);
    }
}
