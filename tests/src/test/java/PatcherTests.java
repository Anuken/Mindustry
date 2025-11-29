import arc.struct.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.content.*;
import mindustry.entities.abilities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.production.*;
import mindustry.world.blocks.units.*;
import mindustry.world.consumers.*;
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

        resetAfter();

        plan = ((UnitFactory)Blocks.groundFactory).plans.find(u -> u.unit == UnitTypes.flare);

        assertNull(plan);
    }

    @Test
    void reconstructorPlans() throws Exception{
        var reconstructor = ((Reconstructor)Blocks.additiveReconstructor);
        var prev = reconstructor.upgrades.copy();
        var prevConsumes = reconstructor.<ConsumeItems>findConsumer(c -> c instanceof ConsumeItems).items;

        Vars.state.patcher.apply(Seq.with(
        """
        block.additive-reconstructor.upgrades: [[dagger, flare]]
        block.additive-reconstructor.consumes: {
            remove: items
            items: [surge-alloy/10, copper/20]
        }
        """
        ));

        assertNoWarnings();
        var plan = reconstructor.upgrades.get(0);
        assertArrayEquals(new UnitType[]{UnitTypes.dagger, UnitTypes.flare}, plan);
        assertArrayEquals(reconstructor.<ConsumeItems>findConsumer(c -> c instanceof ConsumeItems).items, ItemStack.with(Items.surgeAlloy, 10, Items.copper, 20));

        resetAfter();

        assertEquals(prev, reconstructor.upgrades);
        assertArrayEquals(reconstructor.<ConsumeItems>findConsumer(c -> c instanceof ConsumeItems).items, prevConsumes);

    }

    @Test
    void reconstructorPlansEditSpecific() throws Exception{
        var reconstructor = ((Reconstructor)Blocks.additiveReconstructor);
        var prev = reconstructor.upgrades.copy();

        Vars.state.patcher.apply(Seq.with(
        """
        block.additive-reconstructor.upgrades.1: [dagger, flare]
        """
        ));

        assertNoWarnings();
        var plan = reconstructor.upgrades.get(1);
        assertArrayEquals(new UnitType[]{UnitTypes.dagger, UnitTypes.flare}, plan);

        resetAfter();

        assertEquals(prev, reconstructor.upgrades);
    }

    @Test
    void reconstructorPlansAdd() throws Exception{
        var reconstructor = ((Reconstructor)Blocks.additiveReconstructor);
        var prev = reconstructor.upgrades.copy();

        Vars.state.patcher.apply(Seq.with(
        """
        block.additive-reconstructor.upgrades.+: [[dagger, flare]]
        """
        ));

        assertNoWarnings();
        var plan = reconstructor.upgrades.peek();
        assertArrayEquals(new UnitType[]{UnitTypes.dagger, UnitTypes.flare}, plan);

        resetAfter();

        assertEquals(prev, reconstructor.upgrades);
    }

    @Test
    void unitWeapons() throws Exception{
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

        assertNoWarnings();
        assertEquals(3, UnitTypes.dagger.weapons.size);
        assertEquals("navanax-weapon", UnitTypes.dagger.weapons.get(2).name);
        assertEquals(LightningBulletType.class, UnitTypes.dagger.weapons.get(2).bullet.getClass());
        assertEquals(999, UnitTypes.dagger.weapons.get(2).bullet.lightningLength);

        Vars.logic.reset();

        UnitTypes.dagger.checkStats();
        assertNull(UnitTypes.dagger.stats.toMap().get(StatCat.general).get(Stat.charge));
    }

    @Test
    void uUnitWeaponReassign() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.weapons: [
            {
                name: megapoop
                bullet: {
                    type: rail
                    lightningLength: 999
                }
            }
        ]
        """));

        assertEquals(1, UnitTypes.dagger.weapons.size);
        assertEquals("megapoop", UnitTypes.dagger.weapons.get(0).name);
        assertEquals(RailBulletType.class, UnitTypes.dagger.weapons.get(0).bullet.getClass());
        assertEquals(999, UnitTypes.dagger.weapons.get(0).bullet.lightningLength);

        Vars.logic.reset();

        assertEquals(2, UnitTypes.dagger.weapons.size);
        assertEquals("large-weapon", UnitTypes.dagger.weapons.get(0).name);
    }

    @Test
    void unitAbilities() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.abilities.+: {
            type: ShieldArcAbility
            max: 1000
        }
        """));

        assertEquals(1, UnitTypes.dagger.abilities.size);
        assertEquals(ShieldArcAbility.class, UnitTypes.dagger.abilities.get(0).getClass());
        assertEquals(1000f, ((ShieldArcAbility)UnitTypes.dagger.abilities.get(0)).max);

        Vars.logic.reset();

        assertEquals(0, UnitTypes.dagger.abilities.size);
    }

    @Test
    void unitAbilitiesArray() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.abilities.+: [
            {
                type: ShieldArcAbility
                max: 1000
            },
            {
                type: MoveEffectAbility
                amount: 10
            }
        ]
        """));

        assertEquals(2, UnitTypes.dagger.abilities.size);
        assertEquals(ShieldArcAbility.class, UnitTypes.dagger.abilities.get(0).getClass());
        assertEquals(1000f, ((ShieldArcAbility)UnitTypes.dagger.abilities.get(0)).max);

        assertEquals(MoveEffectAbility.class, UnitTypes.dagger.abilities.get(1).getClass());
        assertEquals(10, ((MoveEffectAbility)UnitTypes.dagger.abilities.get(1)).amount);

        Vars.logic.reset();

        assertEquals(0, UnitTypes.dagger.abilities.size);
    }

    @Test
    void unitTypeObject() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        {
            "name": "object syntax",
            "unit.dagger": {
                "type": "legs"
            }
        }
        """));

        assertNoWarnings();
    }

    @Test
    void unitFlagsArray() throws Exception{
        int oldLength = UnitTypes.dagger.targetFlags.length;

        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.targetFlags.+: [
            shield, drill
        ]
        """));

        assertEquals(oldLength + 2, UnitTypes.dagger.targetFlags.length);
        assertEquals(BlockFlag.shield, UnitTypes.dagger.targetFlags[UnitTypes.dagger.targetFlags.length - 2]);
        assertEquals(BlockFlag.drill, UnitTypes.dagger.targetFlags[UnitTypes.dagger.targetFlags.length - 1]);

        Vars.logic.reset();

        assertEquals(oldLength, UnitTypes.dagger.targetFlags.length);
    }

    @Test
    void unitFlags() throws Exception{
        int oldLength = UnitTypes.dagger.targetFlags.length;

        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.targetFlags.+: shield
        """));

        assertEquals(oldLength + 1, UnitTypes.dagger.targetFlags.length);
        assertEquals(BlockFlag.shield, UnitTypes.dagger.targetFlags[UnitTypes.dagger.targetFlags.length - 1]);

        Vars.logic.reset();

        assertEquals(oldLength, UnitTypes.dagger.targetFlags.length);
    }

    @Test
    void unitType() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.type: legs
        """));

        assertEquals(0, Vars.state.patcher.patches.first().warnings.size);
        assertEquals(LegsUnit.class, UnitTypes.dagger.constructor.get().getClass());

        Vars.logic.reset();

        assertEquals(MechUnit.class, UnitTypes.dagger.constructor.get().getClass());
    }

    @Test
    void cannotPatch() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        block.conveyor.size: 2
        """));

        assertEquals(1, Vars.state.patcher.patches.first().warnings.size);
        assertEquals(1, Blocks.conveyor.size);
    }

    @Test
    void gibberish() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        }[35209509()jfkjhadsf,
        ,,,,,[]
        ]{
        """));

        assertEquals(1, Vars.state.patcher.patches.first().warnings.size);
    }

    @Test
    void noIdAssign() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        block.router.id: 9231
        """));

        assertEquals(1, Vars.state.patcher.patches.first().warnings.size);
    }

    @Test
    void unknownFieldWarn() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.weapons.+: {
            bullet: {
                frogs: 99
            }
        }
        unit.dagger.frogs: 10
        """));

        assertEquals(2, Vars.state.patcher.patches.first().warnings.size);
    }

    @Test
    void objectFloatMap() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        block.mechanical-drill.drillMultipliers: {
            titanium: 2.0
        }
        
        block.mechanical-drill: {
            drillMultipliers: {
                copper: 3.0
            }
        }
        block.mechanical-drill.drillMultipliers.surge-alloy: 10
        """));

        assertNoWarnings();
        assertEquals(2f, ((Drill)Blocks.mechanicalDrill).drillMultipliers.get(Items.titanium, 0f));
        assertEquals(3f, ((Drill)Blocks.mechanicalDrill).drillMultipliers.get(Items.copper, 0f));
        assertEquals(10f, ((Drill)Blocks.mechanicalDrill).drillMultipliers.get(Items.surgeAlloy, 0f));

        Vars.logic.reset();

        assertEquals(0f, ((Drill)Blocks.mechanicalDrill).drillMultipliers.get(Items.titanium, 0f));
        assertEquals(0f, ((Drill)Blocks.mechanicalDrill).drillMultipliers.get(Items.surgeAlloy, 0f));
    }

    @Test
    void specificArrayRequirements() throws Exception{
        ItemStack[] reqs = Blocks.scatter.requirements.clone();
        Vars.state.patcher.apply(Seq.with("""
        block.scatter.requirements: {
            0: surge-alloy/10
        }
        block.duo.requirements: [titanium/5, surge-alloy/20]
        """));

        assertNoWarnings();
        assertEquals(Blocks.scatter.requirements[0], new ItemStack(Items.surgeAlloy, 10));
        assertEquals(Blocks.scatter.requirements[1], reqs[1]);
        assertEquals(Blocks.duo.requirements[0], new ItemStack(Items.titanium, 5));


        Vars.logic.reset();
        assertArrayEquals(reqs, Blocks.scatter.requirements);
    }

    @Test
    void attributes() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        block.grass.attributes: {
            oil: 99
        }
        block.grass.attributes.heat: 77
        """));

        assertNoWarnings();
        assertEquals(99, Blocks.grass.attributes.get(Attribute.oil));
        assertEquals(77, Blocks.grass.attributes.get(Attribute.heat));

        Vars.logic.reset();

        assertEquals(0, Blocks.grass.attributes.get(Attribute.oil));
        assertEquals(0, Blocks.grass.attributes.get(Attribute.heat));
    }

    @Test
    void noResolution() throws Exception{
        String name = Pathfinder.class.getCanonicalName();

        Vars.state.patcher.apply(Seq.with("""
        block.conveyor.lastConfig: {
            class: %theClass%
        }
        """.replace("%theClass%", name)));

        assertEquals(1, Vars.state.patcher.patches.first().warnings.size);
    }

    @Test
    void setMultiAdd() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.immunities.+: [slow, fast]
        """));

        assertNoWarnings();
        assertTrue(UnitTypes.dagger.immunities.contains(StatusEffects.slow));
        assertTrue(UnitTypes.dagger.immunities.contains(StatusEffects.fast));

        Vars.logic.reset();

        assertFalse(UnitTypes.dagger.immunities.contains(StatusEffects.slow));
        assertFalse(UnitTypes.dagger.immunities.contains(StatusEffects.fast));
    }

    @Test
    void ammoReassign() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        block.fuse.ammoTypes: {
          titanium: "-"
          surge-alloy: {
            type: LaserBulletType
            ammoMultiplier: 1
            reloadMultiplier: 0.5
            damage: 100
            colors: ["000000", "ff0000", "ffffff"]
          }
        }
        """));

        assertNoWarnings();
        assertTrue(((ItemTurret)Blocks.fuse).ammoTypes.containsKey(Items.surgeAlloy));
        assertFalse(((ItemTurret)Blocks.fuse).ammoTypes.containsKey(Items.titanium));
        assertEquals(100, ((ItemTurret)Blocks.fuse).ammoTypes.get(Items.surgeAlloy).damage);

        Vars.logic.reset();

        assertFalse(((ItemTurret)Blocks.fuse).ammoTypes.containsKey(Items.surgeAlloy));
        assertTrue(((ItemTurret)Blocks.fuse).ammoTypes.containsKey(Items.titanium));
    }

    @Test
    void indexAccess() throws Exception{
        float oldDamage = UnitTypes.dagger.weapons.first().bullet.damage;
        Vars.state.patcher.apply(Seq.with("""
        unit.dagger.weapons.0.bullet.damage: 100
        """));

        assertNoWarnings();
        assertEquals(100, UnitTypes.dagger.weapons.first().bullet.damage);

        Vars.logic.reset();

        assertEquals(oldDamage, UnitTypes.dagger.weapons.first().bullet.damage);
    }

    @Test
    void addWeapon() throws Exception{
        int oldSize = UnitTypes.flare.weapons.size;
        Vars.state.patcher.apply(Seq.with("""
        unit.flare.weapons.+: {
          x: 0
          y: 0
          reload: 10
          bullet: {
            type: LaserBulletType
            damage: 100
          }
        }
        """));

        assertNoWarnings();
        assertEquals(oldSize + 1, UnitTypes.flare.weapons.size);
        assertEquals(100, UnitTypes.flare.weapons.peek().bullet.damage);
    }

    @Test
    void bigPatch() throws Exception{
        Vars.state.patcher.apply(Seq.with("""
        item: {
        	fissile-matter: {
        	    localizedName: Duo
        		hidden: false
        		fullIcon: duo-preview
        		uiIcon: block-duo-ui
        	}
        }
        block: {
        	pulverizer: {
        	    localizedName: Duo Factory
        		consumes: {
        			remove: all
        			item: copper
        		}
        		uiIcon: block-duo-ui
        		region: block-duo-full
        		outputItems: [fissile-matter/1]
        		drawer: [
        			{
        				type: DrawRegion
        				name: block-1
        			}
        			{
        				type: DrawRegion
        				rotateSpeed: 1
        				name: duo-preview
        			}
        		]
        	}
        }
        unit: {
        	dagger: {
        		region: duo-preview
        		weapons: [
        			{
        			    x: 0
        			    y: 0
        			    reload: 20
        				shoot: {
        					type: ShootAlternate
        					spread: 3.5
        				}
        				bullet: {
        					width: 7
        					height: 9
        					lifetime: 60
        					frontColor: eac1a8
        					backColor: d39169
        				}
        			}
        		]
        	}
        }
        """));

        assertNoWarnings();
    }

    static void assertNoWarnings(){
        assertEquals(new Seq<>(), Vars.state.patcher.patches.first().warnings);
    }
}
