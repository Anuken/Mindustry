import arc.struct.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.entities.bullet.*;
import mindustry.type.*;
import mindustry.world.blocks.units.*;
import mindustry.world.meta.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import static org.junit.jupiter.api.Assertions.*;

public class PatcherTests{

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

    @ParameterizedTest
    @ValueSource(strings = {"""
    block.ground-factory.plans.+: {
        unit: flare
        requirements: [surge-alloy/10]
        time: 100
    }
    """,
    """
    block: {
        ground-factory: {
            plans.+: {
                unit: flare
                requirements: [surge-alloy/10]
                time: 100
            }
        }
    }
    """
    })
    void unitFactoryPlans(String value) throws Exception{
        Vars.state.patcher.apply(Seq.with(value));

        var plan = ((UnitFactory)Blocks.groundFactory).plans.find(u -> u.unit == UnitTypes.flare);
        assertNotNull(plan, "A plan for flares must have been added.");
        assertEquals(UnitTypes.flare, plan.unit);
        assertArrayEquals(new ItemStack[]{new ItemStack(Items.surgeAlloy, 10)}, plan.requirements);
        assertEquals(100f, plan.time);

        Vars.state.patcher.unapply();

        plan = ((UnitFactory)Blocks.groundFactory).plans.find(u -> u.unit == UnitTypes.flare);

        assertNull(plan);
    }

    @Test
    void testUnitWeapons() throws Exception{
        UnitTypes.dagger.checkStats();
        UnitTypes.dagger.stats.add(Stat.charge, 999);
        assertNotNull(UnitTypes.dagger.stats.toMap().get(StatCat.general).get(Stat.charge));

        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.weapons.+: {
            name: navanax-weapon
            bullet: {
                type: LightningBulletType
                lightningLength: 999
            }
        }
        """));

        assertEquals(3, UnitTypes.dagger.weapons.size);
        assertEquals("navanax-weapon", UnitTypes.dagger.weapons.get(2).name);
        assertEquals(LightningBulletType.class, UnitTypes.dagger.weapons.get(2).bullet.getClass());
        assertEquals(999, UnitTypes.dagger.weapons.get(2).bullet.lightningLength);

        Vars.logic.reset();

        UnitTypes.dagger.checkStats();
        assertNull(UnitTypes.dagger.stats.toMap().get(StatCat.general).get(Stat.charge));
    }
}
