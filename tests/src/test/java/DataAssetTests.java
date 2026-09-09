import arc.struct.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.entities.bullet.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.mod.data.*;
import mindustry.type.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class DataAssetTests{

    @BeforeAll
    static void init(){
        ApplicationTests.launchApplication(false);
    }

    @AfterEach
    void resetAfter(){
        Vars.logic.reset();
    }

    @BeforeEach
    void resetBefore(){
        Vars.logic.reset();
    }

    @Test
    void basicItem(){
        int totalItems = Vars.content.items().size;

        loadContent(ContentType.item, "testitem", """
        name: 'Test Item'
        hardness: 10
        """);

        Item it = find(ContentType.item, "testitem");
        assertNotNull(it);
        assertEquals(10f, it.hardness, 0.001f);
        assertEquals("Test Item", it.localizedName);

        resetAfter();

        assertEquals(totalItems, Vars.content.items().size, "Item content must be properly reset");
        assertNull(find(ContentType.item, "testitem"), "Item must be properly removed from map");
    }

    @Test
    void basicUnit(){

        loadContent(ContentType.unit, "testunit", """
        name: 'Test Unit'
        type: tank
        weapons: [
            {
                mirror: true
                bullet: {
                    damage: 10
                    type: Laser
                    length: 1000
                }
            }
        ]
        """);

        UnitType it = find(ContentType.unit, "testunit");

        assertNotNull(it);
        assertTrue(it.create(Team.sharded) instanceof TankUnit);
        assertEquals("Test Unit", it.localizedName);
        assertEquals(2, it.weapons.size);
        assertEquals(LaserBulletType.class, it.weapons.get(0).bullet.getClass());
        assertEquals(1000f, ((LaserBulletType)it.weapons.get(0).bullet).length, 0.001f);
    }

    @Test
    void noContentAddedWithError(){

        loadContent(ContentType.block, "badblock", """
        name: 'This will explode'
        type: Bad
        """);

        assertNull(find(ContentType.block, "badblock"), "Content should not be loaded when an error occurs");
        assertEquals(1, Vars.state.data.getContent().first().warnings.size);
    }

    @Test
    void noNullFieldsAllowed(){

        loadContent(ContentType.block, "badblock", """
        name: 'This will explode'
        flags: null
        """);

        assertNull(find(ContentType.block, "badblock"), "Content should not be loaded when an error occurs");
        assertEquals(1, Vars.state.data.getContent().first().warnings.size);
        assertTrue(Vars.state.data.getContent().first().warnings.toString().contains("null"), "Warnings must contain mention of field being null: " + Vars.state.data.getContent().first().warnings);
    }

    static <T> T find(ContentType type, String name){
        return (T)Vars.content.getByName(type, "dp-" + name);
    }

    static void loadContent(ContentType type, String name, String data){
        Vars.state.data.load(Seq.with(new ContentAsset(name + ".json", type, data)));
    }
}
