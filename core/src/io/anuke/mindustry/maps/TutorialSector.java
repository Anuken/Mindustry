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
            new BlockMission(ProductionBlocks.mechanicalDrill).setMessage("$tutorial.begin"),
            new BlockMission(DistributionBlocks.conveyor),
            new ItemMission(Items.copper, 40),
            new BlockMission(TurretBlocks.duo),
            new WaveMission(5),
            new ExpandMission(1, 0),
            new ItemMission(Items.lead, 30),
            new ItemMission(Items.copper, 150),
            new BlockMission(CraftingBlocks.smelter),
            new ItemMission(Items.densealloy, 30),
            new BlockMission(PowerBlocks.combustionGenerator),
            new BlockMission(PowerBlocks.powerNode),
            new BlockMission(CraftingBlocks.siliconsmelter),
            new ItemMission(Items.silicon, 30),
            new BlockMission(UnitBlocks.daggerFactory),
            new UnitMission(UnitTypes.dagger),
            new ExpandMission(-1, 0),
            new BattleMission()
        );
    }
}
