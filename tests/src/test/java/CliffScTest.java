import mindustry.CliffSc;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.core.World;
import mindustry.world.Tile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static mindustry.Vars.world;
import static org.junit.jupiter.api.Assertions.*;

class CliffScTest {
    private CliffSc cliffSc;
    @BeforeEach
    void setUp() {
        cliffSc = new CliffSc();
    }
    @Test
    void testAddWithNoCliff() {
        // Arrange
        world = new World();
        // Act
        cliffSc.add();
        // Verify
        for (Tile tile : world.tiles) {
            assertNotSame(tile.block(), Blocks.cliff);
        }
    }
    @Test
    void testAddWithCliff() {
        // Arrange
        Vars.world = new World();
        // Act
        cliffSc.add();
        // Verify
        for (Tile tile : world.tiles) {
            if (tile.block() != Blocks.cliff) {
                assertEquals(Blocks.air, tile.block());
            }
        }
    }
}
