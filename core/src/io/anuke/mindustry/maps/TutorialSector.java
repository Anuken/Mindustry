package io.anuke.mindustry.maps;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.content.blocks.*;
import io.anuke.mindustry.maps.missions.*;

/**Just a class for returning the list of tutorial missions.*/
public class TutorialSector{

    public static Array<Mission> getMissions(){
        return Array.with(
            new ItemMission(Items.copper, 30).setMessage("$tutorial.begin"),
            new BlockMission(ProductionBlocks.mechanicalDrill).setMessage("$tutorial.drill"),
            new BlockMission(DistributionBlocks.conveyor).setMessage("$tutorial.conveyor"),
            new ItemMission(Items.copper, 50).setMessage("$tutorial.morecopper"),
            new BlockMission(TurretBlocks.duo).setMessage("$tutorial.turret"),
            new BlockMission(ProductionBlocks.mechanicalDrill).setMessage("$tutorial.drillturret"),
            new WaveMission(5).setMessage("$tutorial.waves"),
            new ExpandMission(1, 0),
            new ItemMission(Items.lead, 30).setMessage("$tutorial.lead"),
            new ItemMission(Items.copper, 150).setMessage("$tutorial.morecopper"),
            new BlockMission(CraftingBlocks.smelter).setMessage("$tutorial.smelter"),
            new ItemMission(Items.densealloy, 30).setMessage("$tutorial.densealloy"),
            new BlockMission(CraftingBlocks.siliconsmelter).setMessage("$tutorial.siliconsmelter"),
            new BlockMission(PowerBlocks.combustionGenerator).setMessage("$tutorial.generator"),
            new BlockMission(PowerBlocks.powerNode).setMessage("$tutorial.node"),
            new ItemMission(Items.silicon, 30).setMessage("$tutorial.silicon"),
            new BlockMission(UnitBlocks.daggerFactory).setMessage("$tutorial.daggerfactory"),
            new UnitMission(UnitTypes.dagger).setMessage("$tutorial.dagger"),
            new ExpandMission(-1, 0),
            new BattleMission().setMessage("$tutorial.battle")
        );
    }
}
