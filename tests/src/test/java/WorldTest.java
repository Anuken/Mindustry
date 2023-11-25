import mindustry.core.World;
import org.junit.jupiter.api.Test;
import arc.math.geom.Rect;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorldTest {
    @Test
    void testGetQuadBounds() {
        // Arrange
        World yourClassInstance = new World(); // Replace with the actual class name
        Rect inputRect = new Rect(1, 2, 3, 4);

        // Act
        Rect result = yourClassInstance.getQuadBounds(inputRect);

        // Assert
        assertEquals(-250.0, result.x);
        assertEquals(-250.0, result.y);
        assertEquals(500.0, result.width);
        assertEquals(500.0, result.height);
    }
}
