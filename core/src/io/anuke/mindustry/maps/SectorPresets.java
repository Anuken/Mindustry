package io.anuke.mindustry.maps;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.Mechs;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.content.blocks.CraftingBlocks;
import io.anuke.mindustry.content.blocks.ProductionBlocks;
import io.anuke.mindustry.content.blocks.UnitBlocks;
import io.anuke.mindustry.content.blocks.UpgradeBlocks;
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
            Array.with(Items.copper, Items.coal, Items.lead)));

        //command center mission
        add(new SectorPreset(0, 1,
            Structs.array(
                Missions.blockRecipe(UnitBlocks.daggerFactory),
                new UnitMission(UnitTypes.dagger),
                Missions.blockRecipe(UnitBlocks.commandCenter),
                new CommandMission(UnitCommand.retreat),
                new CommandMission(UnitCommand.attack),
                new BattleMission()
            ),
            Array.with(Items.copper, Items.lead, Items.coal)));

        //pad mission
        add(new SectorPreset(0, -2,
            Structs.array(
                Missions.blockRecipe(mobile ? UpgradeBlocks.alphaPad : UpgradeBlocks.dartPad),
                new MechMission(mobile ? Mechs.alpha : Mechs.dart),
                new WaveMission(15)
            ),
            Array.with(Items.copper, Items.lead, Items.coal, Items.titanium)));

        //oil mission
        add(new SectorPreset(-2, 0,
            Structs.array(
                Missions.blockRecipe(ProductionBlocks.cultivator),
                Missions.blockRecipe(ProductionBlocks.waterExtractor),
                new ContentMission(Items.biomatter),
                Missions.blockRecipe(CraftingBlocks.biomatterCompressor),
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
