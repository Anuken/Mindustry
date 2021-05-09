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
    /** Raw item production statistics. */
    public ObjectMap<Item, ExportStat> rawProduction = new ObjectMap<>();
    /** Export statistics. */
    public ObjectMap<Item, ExportStat> export = new ObjectMap<>();
    /** Items stored in all cores. */
    public ItemSeq items = new ItemSeq();
    /** The best available core type. */
    public Block bestCoreType = Blocks.coreShard;
    /** Max storage capacity. */
    public int storageCapacity = 0;
    /** Whether a core is available here. */
    public boolean hasCore = true;
    /** Whether this sector was ever fully captured. */
    public boolean wasCaptured = false;
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
    /** Whether this sector has any enemy spawns. */
    public boolean hasSpawns = true;
    /** Wave # from state */
    public int wave = 1, winWave = -1;
    /** Waves this sector can survive if under attack. Based on wave in info. <0 means uncalculated. */
    public int wavesSurvived = -1;
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
    /** How many minutes this sector has been captured. */
    public float minutesCaptured;
    /** Display name. */
    public @Nullable String name;
    /** Displayed icon. */
    public @Nullable String icon;
    /** Version of generated waves. When it doesn't match, new waves are generated. */
    public int waveVersion = -1;
    /** Whether this sector was indicated to the player or not. */
    public boolean shown = false;

    /** Special variables for simulation. */
    public float sumHealth, sumRps, sumDps, waveHealthBase, waveHealthSlope, waveDpsBase, waveDpsSlope, bossHealth, bossDps, curEnemyHealth, curEnemyDps;
    /** Wave where first boss shows up. */
    public int bossWave = -1;

    /** Counter refresh state. */
    private transient Interval time = new Interval();
    /** Core item storage input/output deltas. */
    private @Nullable transient int[] coreDeltas;
    /** Core item storage input/output deltas. */
    private @Nullable transient int[] productionDeltas;

    /** Handles core item changes. */
    public void handleCoreItem(Item item, int amount){
        if(coreDeltas == null) coreDeltas = new int[content.items().size];
        coreDeltas[item.id] += amount;
    }

    /** Handles raw production stats. */
    public void handleProduction(Item item, int amount){
        if(productionDeltas == null) productionDeltas = new int[content.items().size];
        productionDeltas[item.id] += amount;
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
        //import statistics are handled separately
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
        if(winWave <= 0 && !attack){
            winWave = 30;
        }

        state.wave = wave;
        state.rules.waves = waves;
        state.rules.waveSpacing = waveSpacing;
        state.rules.winWave = winWave;
        state.rules.attackMode = attack;

        //assign new wave patterns when the version changes
        if(waveVersion != Waves.waveVersion && state.rules.sector.preset == null){
            state.rules.spawns = Waves.generate(state.rules.sector.threat);
        }

        CoreBuild entity = state.rules.defaultTeam.core();
        if(entity != null){
            entity.items.clear();
            entity.items.add(items);
            //ensure capacity.
            entity.items.each((i, a) -> entity.items.set(i, Mathf.clamp(a, 0, entity.storageCapacity)));
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

        waveVersion = Waves.waveVersion;
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
        hasSpawns = spawner.countSpawns() > 0;

        //cap production at raw production.
        production.each((item, stat) -> {
            stat.mean = Math.min(stat.mean, rawProduction.get(item, ExportStat::new).mean);
        });

        if(state.rules.sector != null){
            state.rules.sector.saveInfo();
        }

        SectorDamage.writeParameters(this);
    }

    /** Update averages of various stats, updates some special sector logic.
     * Called every frame. */
    public void update(){
        //updating in multiplayer as a client doesn't make sense
        if(net.client()) return;

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

            if(coreDeltas == null) coreDeltas = new int[content.items().size];
            if(productionDeltas == null) productionDeltas = new int[content.items().size];

            //refresh core items
            for(Item item : content.items()){
                updateDelta(item, production, coreDeltas);
                updateDelta(item, rawProduction, productionDeltas);

                //cap production/export by production
                production.get(item).mean = Math.min(production.get(item).mean, rawProduction.get(item).mean);

                if(export.containsKey(item)){
                    export.get(item).mean = Math.min(export.get(item).mean, Math.max(rawProduction.get(item).mean, -production.get(item).mean));
                }
            }

            Arrays.fill(coreDeltas, 0);
            Arrays.fill(productionDeltas, 0);
        }
    }

    void updateDelta(Item item, ObjectMap<Item, ExportStat> map, int[] deltas){
        ExportStat stat = map.get(item, ExportStat::new);
        if(!stat.loaded){
            stat.means.fill(stat.mean);
            stat.loaded = true;
        }

        //store means
        stat.means.add(deltas[item.id]);
        stat.mean = stat.means.rawMean();
    }

    public ObjectFloatMap<Item> exportRates(){
        ObjectFloatMap<Item> map = new ObjectFloatMap<>();
        export.each((item, value) -> map.put(item, value.mean));
        return map;
    }

    public ObjectMap<Item, ExportStat> importStats(){
        //build empty import stats
        ObjectMap<Item, ExportStat> imports = new ObjectMap<>();
        content.items().each((item) -> {
            ExportStat stat = new ExportStat();
            stat.mean = 0f;
            imports.put(item, stat);
        });
        //for all sectors on all planets that have bases and export to this sector
        for(Planet planet : content.planets()){
            for(Sector sector : planet.sectors){
                Sector dest = sector.info.getRealDestination();
                if (sector.hasBase() && dest != null && dest.info == this) {
                    //add their exports to our imports
                    sector.info.export.each((item, stat) -> {
                        imports.get(item).mean += stat.mean;
                    });
                }
            }
        }
        return imports;
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
