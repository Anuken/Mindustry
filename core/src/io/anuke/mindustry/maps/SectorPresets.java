package io.anuke.mindustry.maps;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.entities.units.UnitCommand;
import io.anuke.mindustry.maps.missions.*;
import io.anuke.mindustry.type.Item;
import io.anuke.ucore.util.GridMap;
import io.anuke.ucore.util.Structs;

import static io.anuke.mindustry.Vars.mobile;

public class SectorPresets{
    private final GridMap<SectorPreset> presets = new GridMap<>();
    private final GridMap<Array<Item>> orePresets = new GridMap<>();

    public SectorPresets(){

        //base tutorial mission
        add(new SectorPreset(0, 0,
            TutorialSector.getMissions(),
            Array.with(),
            1));

        //command center mission
        add(new SectorPreset(2, 0,
            Structs.array(
                new BlockMission(UnitBlocks.daggerFactory),
                Missions.blockRecipe(UnitBlocks.commandCenter),
                new CommandMission(UnitCommand.retreat),
                new CommandMission(UnitCommand.attack),
                new BattleMission()
            ),
            Array.with(Items.copper, Items.lead, Items.copper),
            2));

        //pad mission
        add(new SectorPreset(0, -2,
            Structs.array(
                Missions.blockRecipe(mobile ? UpgradeBlocks.tridentPad : UpgradeBlocks.deltaPad),
                new MechMission(Mechs.delta),
                new WaveMission(15)
            ),
            Array.with(Items.copper, Items.lead, Items.copper, Items.titanium),
            2));

        //oil mission
        add(new SectorPreset(-2, 0,
            Structs.array(
                Missions.blockRecipe(ProductionBlocks.cultivator),
                Missions.blockRecipe(ProductionBlocks.waterExtractor),
                new ContentMission(Items.biomatter),
                Missions.blockRecipe(CraftingBlocks.biomatterCompressor),
                new ContentMission(Liquids.oil)
            ),
            Array.with(Items.copper, Items.lead, Items.copper, Items.titanium),
            2));

        //tutorial ore presets
        orePresets.put(0, 0, Array.with(Items.copper));
        orePresets.put(1, 0, Array.with(Items.copper, Items.lead, Items.coal));
    }

    public Array<Item> getOres(int x, int y){
        return orePresets.get(x, y);
    }

    public SectorPreset get(int x, int y){
        return presets.get(x, y);
    }

    private void add(SectorPreset preset){
        for(int x = 0; x < preset.size; x++){
            for(int y = 0; y < preset.size; y++){
                presets.put(x + preset.x, y + preset.y, preset);
                orePresets.put(x + preset.x, y + preset.y, preset.ores);
            }
        }
    }

    public static class SectorPreset{
        public final Array<Mission> missions;
        public final Array<Item> ores;
        public final int size, x, y;

        public SectorPreset(int x, int y, Array<Mission> missions, Array<Item> ores, int size){
            this.missions = missions;
            this.size = size;
            this.x = x;
            this.y = y;
            this.ores = ores;
        }

        void generate(Sector sector){

        }
    }
}
