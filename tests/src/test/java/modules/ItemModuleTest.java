package modules;

import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.world.modules.ItemModule;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ItemModuleTest {

    @BeforeAll
    static void setUp() {
        // Set up the necessary dependencies
        Vars.content = new ContentLoader();
        Vars.content.items().each(i -> {
        });
    }

    @Test
    void copy() {
        // Create an initial item module
        ItemModule itemModule = new ItemModule();
        itemModule.set(new ItemModule());

        // Make a copy of the item module
        ItemModule copiedModule = itemModule.copy();

        // Assert that the copied module is not the same instance as the original module
        assertNotSame(itemModule, copiedModule);

        // Assert that the copied module has the same state as the original module
        assertEquals(itemModule.total(), copiedModule.total());
        assertEquals(itemModule.length(), copiedModule.length());
    }

}
