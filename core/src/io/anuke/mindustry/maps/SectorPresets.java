package io.anuke.mindustry.maps;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.GridMap;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.units.UnitCommand;
import io.anuke.mindustry.maps.missions.*;
import io.anuke.mindustry.type.Item;

import static io.anuke.mindustry.Vars.mobile;

public class SectorPresets{
    private final GridMap<SectorPreset> presets = new GridMap<>();
    private final GridMap<Array<Item>> orePresets = new GridMap<>();

    public SectorPresets(){

        //base tutorial mission
        add(new SectorPreset(0, 0,
            TutorialSector.getMissions(),
            Array.with(Items.copper, Items.coal, Items.lead)));

        //command center mission
        add(new SectorPreset(0, 1,
            Array.ofRecursive(
                Missions.blockRecipe(Blocks.daggerFactory),
                new UnitMission(UnitTypes.dagger),
                Missions.blockRecipe(Blocks.commandCenter),
                new CommandMission(UnitCommand.retreat),
                new CommandMission(UnitCommand.attack),
                new BattleMission()
            ),
            Array.with(Items.copper, Items.lead, Items.coal)));

        //pad mission
        add(new SectorPreset(0, -2,
        Array.ofRecursive(
                Missions.blockRecipe(mobile ? Blocks.alphaPad : Blocks.dartPad),
                new MechMission(mobile ? Mechs.alpha : Mechs.dart),
                new WaveMission(15)
            ),
            Array.with(Items.copper, Items.lead, Items.coal, Items.titanium)));

        //oil mission
        add(new SectorPreset(-2, 0,
            Array.ofRecursive(
                Missions.blockRecipe(Blocks.cultivator),
                Missions.blockRecipe(Blocks.waterExtractor),
                new ContentMission(Items.biomatter),
                Missions.blockRecipe(Blocks.biomatterCompressor),
                new ContentMission(Liquids.oil),
                new BattleMission()
            ),
            Array.with(Items.copper, Items.lead, Items.coal, Items.titanium)));
    }

    public Array<Item> getOres(int x, int y){
        return orePresets.get(x, y);
    }

    public SectorPreset get(int x, int y){
        return presets.get(x, y);
    }

    public GridMap<SectorPreset> getPresets() { return presets; }

    private void add(SectorPreset preset){
        presets.put(preset.x, preset.y, preset);
        orePresets.put(preset.x, preset.y, preset.ores);
    }

    public static class SectorPreset{
        public final Array<Mission> missions;
        public final Array<Item> ores;
        public final int x, y;

        public SectorPreset(int x, int y, Array<Mission> missions, Array<Item> ores){
            this.missions = missions;
            this.x = x;
            this.y = y;
            this.ores = ores;
        }
    }
}
