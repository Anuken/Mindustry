import mindustry.io.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.core.GameState.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.ItemTurret.*;
import org.junit.jupiter.api.*;

import java.io.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class TurretAmmoTests{

    @BeforeAll
    public static void launchApplication(){
        ApplicationTests.launchApplication();
    }

    @BeforeEach
    void loadWorld() throws Throwable{
        logic.reset();
        state.set(State.menu);
        GameState.loadMap(ApplicationTests.testMap);
        state.set(State.playing);
        state.rules.limitMapArea = false;
        HeatBehaviorTests.clearArea();
    }

    static int fill(ItemTurretBuild build, mindustry.type.Item item){
        int added = 0;
        while(build.acceptItem(null, item)){
            build.handleItem(null, item);
            added++;
        }
        return added;
    }

    static int shots(ItemTurretBuild build){
        int shots = 0;
        //hasAmmo() also requires canConsume(), which needs a consumer update
        while(build.ammo.any() && build.ammo.peek().canShoot()){
            build.useAmmo();
            shots++;
        }
        return shots;
    }

    @Test
    void itemCapacityAndUse(){
        var duo = (ItemTurretBuild)HeatBehaviorTests.place(Blocks.duo, 10, 10, 0);
        int cap = Blocks.duo.itemCapacity;
        assertEquals(cap, duo.acceptStack(Items.copper, 100, null));
        assertEquals(cap, fill(duo, Items.copper));
        //copper uses half an item per shot
        assertEquals(cap * 2, shots(duo));
        assertEquals(0f, duo.totalAmmo);
    }

    @Test
    void fractionalUseHasNoDrift(){
        //silicon uses 1/3 item per shot in hail
        var hail = (ItemTurretBuild)HeatBehaviorTests.place(Blocks.hail, 10, 10, 0);
        assertEquals(Blocks.hail.itemCapacity, fill(hail, Items.silicon));
        assertEquals(Blocks.hail.itemCapacity * 3, shots(hail));
        assertEquals(0, hail.ammo.size);
    }

    @Test
    void legacySaveIsCapped() throws IOException{
        var duo = (ItemTurretBuild)HeatBehaviorTests.place(Blocks.duo, 10, 10, 0);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        duo.writeAll(new Writes(new DataOutputStream(out)));
        byte[] data = out.toByteArray();
        //last byte is the empty ammo count; replace it with one revision 2 entry of 60 ammo units
        ByteArrayOutputStream legacy = new ByteArrayOutputStream();
        legacy.write(data, 0, data.length - 1);
        DataOutputStream stream = new DataOutputStream(legacy);
        stream.writeByte(1);
        stream.writeShort(Items.copper.id);
        stream.writeShort(60);

        duo.readAll(new Reads(new DataInputStream(new ByteArrayInputStream(legacy.toByteArray()))), (byte)2);
        assertEquals(Blocks.duo.itemCapacity, duo.totalAmmo);
        assertEquals(Blocks.duo.itemCapacity, duo.ammo.first().amount);
        assertFalse(duo.acceptItem(null, Items.copper));

        //current revision round trip
        out.reset();
        duo.writeAll(new Writes(new DataOutputStream(out)));
        duo.readAll(new Reads(new DataInputStream(new ByteArrayInputStream(out.toByteArray()))), duo.version());
        assertEquals(Blocks.duo.itemCapacity, duo.totalAmmo);
        assertEquals(1, duo.ammo.size);
    }

    @Test
    void liquidStatConsumption(){
        for(var turret : new LiquidTurret[]{(LiquidTurret)Blocks.wave, (LiquidTurret)Blocks.tsunami}){
            turret.computeStats();
            turret.ammoTypes.each((liquid, type) -> {
                float perSecond = type.statLiquidConsumed * type.ammoUsed * 60f;
                assertEquals(type.ammoUsed * type.reloadMultiplier * 60f / turret.reload, perSecond, 0.0001f, turret.name + " " + liquid.name);
            });
        }
    }
}
