package mindustry.game;

import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.maps.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.modules.*;

import java.util.*;

import static mindustry.Vars.*;

public class SectorInfo{
    /** average window size in samples */
    private static final int valueWindow = 60;
    /** refresh period of export in ticks */
    private static final float refreshPeriod = 60;

    /** Core input statistics. */
    public ObjectMap<Item, ExportStat> production = new ObjectMap<>();
    /** Export statistics. */
    public ObjectMap<Item, ExportStat> export = new ObjectMap<>();
    /** Items stored in all cores. */
    public ItemSeq items = new ItemSeq();
    /** The best available core type. */
    public Block bestCoreType = Blocks.air;
    /** Max storage capacity. */
    public int storageCapacity = 0;
    /** Whether a core is available here. */
    public boolean hasCore = true;
    /** Sector that was launched from. */
    public @Nullable Sector origin;
    /** Launch destination. */
    public @Nullable Sector destination;
    /** Resources known to occur at this sector. */
    public Seq<UnlockableContent> resources = new Seq<>();
    /** Whether waves are enabled here. */
    public boolean waves = true;
    /** Whether attack mode is enabled here. */
    public boolean attack = false;
    /** Wave # from state */
    public int wave = 1, winWave = -1;
    /** Time between waves. */
    public float waveSpacing = 60 * 60 * 2;
    /** Damage dealt to sector. */
    public float damage;
    /** How many waves have passed while the player was away. */
    public int wavesPassed;
    /** Packed core spawn position. */
    public int spawnPosition;
    /** How long the player has been playing elsewhere. */
    public float secondsPassed;
    /** Display name. */
    public @Nullable String name;

    /** Special variables for simulation. */
    public float sumHealth, sumRps, sumDps, waveHealthBase, waveHealthSlope, waveDpsBase, waveDpsSlope;

    /** Counter refresh state. */
    private transient Interval time = new Interval();
    /** Core item storage to prevent spoofing. */
    private transient int[] coreItemCounts;

    /** Handles core item changes. */
    public void handleCoreItem(Item item, int amount){
        if(coreItemCounts == null){
            coreItemCounts = new int[content.items().size];
        }
        coreItemCounts[item.id] += amount;
    }

    /** @return the real location items go when launched on this sector */
    public Sector getRealDestination(){
        //on multiplayer the destination is, by default, the first captured sector (basically random)
        return !net.client() || destination != null ? destination : state.rules.sector.planet.sectors.find(Sector::hasBase);
    }

    /** Updates export statistics. */
    public void handleItemExport(ItemStack stack){
        handleItemExport(stack.item, stack.amount);
    }

    /** Updates export statistics. */
    public void handleItemExport(Item item, int amount){
        export.get(item, ExportStat::new).counter += amount;
    }

    /** Subtracts from export statistics. */
    public void handleItemImport(Item item, int amount){
        export.get(item, ExportStat::new).counter -= amount;
    }

    public float getExport(Item item){
        return export.get(item, ExportStat::new).mean;
    }

    /** Write contents of meta into main storage. */
    public void write(){
        //enable attack mode when there's a core.
        if(state.rules.waveTeam.core() != null){
            attack = true;
            winWave = 0;
        }

        //if there are infinite waves and no win wave, add a win wave.
        if(waves && winWave <= 0 && !attack){
            winWave = 30;
        }

        state.wave = wave;
        state.rules.waves = waves;
        state.rules.waveSpacing = waveSpacing;
        state.rules.winWave = winWave;
        state.rules.attackMode = attack;

        CoreBuild entity = state.rules.defaultTeam.core();
        if(entity != null){
            entity.items.clear();
            entity.items.add(items);
            //ensure capacity.
            entity.items.each((i, a) -> entity.items.set(i, Math.min(a, entity.storageCapacity)));
        }
    }

    /** Prepare data for writing to a save. */
    public void prepare(){
        //update core items
        items.clear();

        CoreBuild entity = state.rules.defaultTeam.core();

        if(entity != null){
            ItemModule items = entity.items;
            for(int i = 0; i < items.length(); i++){
                this.items.set(content.item(i), items.get(i));
            }

            spawnPosition = entity.pos();
        }

        waveSpacing = state.rules.waveSpacing;
        wave = state.wave;
        winWave = state.rules.winWave;
        waves = state.rules.waves;
        attack = state.rules.attackMode;
        hasCore = entity != null;
        bestCoreType = !hasCore ? Blocks.air : state.rules.defaultTeam.cores().max(e -> e.block.size).block;
        storageCapacity = entity != null ? entity.storageCapacity : 0;
        secondsPassed = 0;
        wavesPassed = 0;
        damage = 0;

        if(state.rules.sector != null){
            state.rules.sector.info = this;
            state.rules.sector.saveInfo();
        }

        SectorDamage.writeParameters(this);
    }

    /** Update averages of various stats, updates some special sector logic.
     * Called every frame. */
    public void update(){
        //updating in multiplayer as a client doesn't make sense
        if(net.client()) return;

        CoreBuild ent = state.rules.defaultTeam.core();

        //refresh throughput
        if(time.get(refreshPeriod)){

            //refresh export
            export.each((item, stat) -> {
                //initialize stat after loading
                if(!stat.loaded){
                    stat.means.fill(stat.mean);
                    stat.loaded = true;
                }

                //add counter, subtract how many items were taken from the core during this time
                stat.means.add(Math.max(stat.counter, 0));
                stat.counter = 0;
                stat.mean = stat.means.rawMean();
            });

            if(coreItemCounts == null){
                coreItemCounts = new int[content.items().size];
            }

            //refresh core items
            for(Item item : content.items()){
                ExportStat stat = production.get(item, ExportStat::new);
                if(!stat.loaded){
                    stat.means.fill(stat.mean);
                    stat.loaded = true;
                }

                //get item delta
                //TODO is preventing negative production a good idea?
                int delta = Math.max(ent == null ? 0 : coreItemCounts[item.id], 0);

                //store means
                stat.means.add(delta);
                stat.mean = stat.means.rawMean();
            }

            Arrays.fill(coreItemCounts, 0);
        }
    }

    public ObjectFloatMap<Item> exportRates(){
        ObjectFloatMap<Item> map = new ObjectFloatMap<>();
        export.each((item, value) -> map.put(item, value.mean));
        return map;
    }

    public static class ExportStat{
        public transient float counter;
        public transient WindowedMean means = new WindowedMean(valueWindow);
        public transient boolean loaded;

        /** mean in terms of items produced per refresh rate (currently, per second) */
        public float mean;

        public String toString(){
            return mean + "";
        }
    }
}
