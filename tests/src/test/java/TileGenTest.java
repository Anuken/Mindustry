import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import mindustry.content.*;
import mindustry.world.*;

public class TileGenTest {

    @Test
    public void testInitialization() {
        //Arrange: Initializing the titleGen objec
        TileGen tileGen = new TileGen();

        //Act As Blocks is a static metiond, it is called without creating object in the assert phase.

        // Assert: Verify that the initial values are set correctly
        assertEquals(Blocks.stone, tileGen.floor);
        assertEquals(Blocks.air, tileGen.block);
        assertEquals(Blocks.air, tileGen.overlay);
    }

    @Test
    public void testReset() {
        //Arrange: Initializing the titleGen object and setting the values to different blocks
        TileGen tileGen = new TileGen();

        tileGen.floor = Blocks.sand;
        tileGen.block = Blocks.copperWall;
        tileGen.overlay = Blocks.conveyor;

        // Act: Reset the values
        tileGen.reset();

        // Assert: Verify that the values have been reset to their initial values
        assertEquals(Blocks.stone, tileGen.floor);
        assertEquals(Blocks.air, tileGen.block);
        assertEquals(Blocks.air, tileGen.overlay);
    }
}
