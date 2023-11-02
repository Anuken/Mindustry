import mindustry.core.World;
import mindustry.world.Tile;
import mindustry.world.Tiles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
public class WorldTest {
    /*
    * Testing unconv Method
    */
    @ParameterizedTest
    @CsvSource({
            "5.0, 40.0",            // Arrange: Set up input=5.0, expected output=40.0
            "-2.5, -20.0",          // Arrange: Set up input=-2.5, expected output=-20.0
            "0.0, 0.0",             // Arrange: Set up input=0.0, expected output=0.0
            "1000000.0, 8000000.0"  // Arrange: Set up input=1000000.0, expected output=8000000.0
    })
    public void testUnconv(float input, float expected) {
        // Act: Calling the 'unconv' method from the 'World' class by providing inputs
        float result = World.unconv(input);

        // Assert: Verifing the result with the expected outcome
        assertEquals(expected, result, 0.001f, "Expected " + input + " * 8.0 = " + expected);
    }

    /*
    * Testing Tile Building With Null Tile object
    */
    @Test
    public void testTileBuildingWithNullTile() {
        // Arrange: Creating a new instance of the World class
        World worldObj = new World();
        
        // Act: Calling the 'tileBuilding' method with coordinates (0, 0)
        Tile result = worldObj.tileBuilding(0, 0);

        // Assert: Verifing that the result is null
        assertNull(result, "Expected result to be null when tile is null");
    }

    /*
    * Testing Tile Building With Null Build
    */
    @Test
    public void testTileBuildingWithNullBuild() {
        // Arrange: Creating a new instance of the World class
        World worldObj = new World(); 
        
        // Arrange: Create a new instance of Tiles class with coordinates (0, 0) and fetching the tile at coordinates (0, 0) from the Tiles object
        
        Tiles tiles = new Tiles(0, 0); 
        Tile tile = tiles.get(0, 0); 
        
        // Act: Calling the 'tileBuilding' method with coordinates (0, 0)
        Tile result = worldObj.tileBuilding(0, 0);
        
        // Assert: Verifing that the result is the same as the original tile
        assertSame(tile, result, "Expected result to be the original tile when build is null");
    }
}
