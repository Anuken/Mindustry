import mindustry.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class FpsCapTests{

    @Test
    void maxCapIs1000(){
        assertEquals(1000, Vars.maxFpsCap);
        assertEquals(Vars.maxFpsCap, Vars.defaultFpsCap);
    }

    @Test
    void capsEveryValueUpToMax(){
        assertTrue(ClientLauncher.fpsCapped(10));
        assertTrue(ClientLauncher.fpsCapped(240));
        assertTrue(ClientLauncher.fpsCapped(1000));
    }

    @Test
    void disabledAboveMaxAndAtZero(){
        assertFalse(ClientLauncher.fpsCapped(0));
        assertFalse(ClientLauncher.fpsCapped(-1));
        assertFalse(ClientLauncher.fpsCapped(1005));
        assertFalse(ClientLauncher.fpsCapped(2000));
    }
}
