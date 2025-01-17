package mindustry.content;

import arc.struct.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;
import mindustry.type.unit.*;
import mindustry.world.blocks.defense.turrets.*;

import static mindustry.Vars.*;
import static mindustry.content.Blocks.*;
import static mindustry.content.SectorPresets.*;
import static mindustry.content.TechTree.*;

public class TantrosTechTree {
    public static void load(){
        Planets.tantros.techTree = nodeRoot("tantros", coreRaft, true, () -> {
            node(itemPipe, () -> {
                node(itemPipeRouter, () -> {
                    node(itemOverflowPipe, () -> {
                        node(crate, () -> {
                            //TODO unloader, more requirements?
                        });
                        node(itemUnderflowPipe);
                    });
                    node(itemPipeBridge);
                });
            });

            node(liquidChannel, Seq.with(new Research(glassSmelter)), () -> {
                node(liquidChannelJunction);
            });

            node(waterPoweredDrill, () -> {
                node(hydroelectricGenerator, () -> {
                    node(scrapSolarPanel, () -> {
                        node(scrapSolarPanelNormal, () -> {

                        });
                    });
                });
            });

            node(sunder, Seq.with(new Research(waterPoweredDrill)), () -> {
                //TODO reqs
                node(spitfire, Seq.with(new Research(scrapSiliconFurnace)), () -> {

                });

                node(hardnedClayWall, () -> {
                    node(scrapWall, () -> {
                        node(scrapWallLarge, () -> {

                        });

                        node(copperWall, () -> {
                            node(copperWallLarge, () -> {

                            });
                        });
                    });

                    node(hardnedClayWallLarge);
                });
            });

            node(pulverizerTantros, Seq.with(new Research(Items.copper), new Research(Items.scrap), new OnSector(crimsonDive)), () -> {
                node(scrapSiliconFurnace, Seq.with(new Research(hydroelectricGenerator)), () -> {
                    //TODO reqs
                    node(glassSmelter, () -> {

                    });
                });
            });

            node(scrapAirFactory, Seq.with(new Research(glassSmelter)), () -> {
                node(UnitTypes.filaria);

                node(scrapAirRefabricator, () -> {
                    node(UnitTypes.botfly);

                    node(scrapPrimeRefabricator, () -> {
                        node(UnitTypes.daremir);

                    });
                });
            });

            node(crimsonDive, () -> {

            });

            nodeProduce(Items.copper, Seq.with(new Research(waterPoweredDrill)), () -> {

            });
            nodeProduce(Items.scrap, Seq.with(new Research(waterPoweredDrill)), () -> {
                nodeProduce(Items.sand, () -> {
                    nodeProduce(Items.silicon, () -> {

                    });
                });
            });
            nodeProduce(Items.hardenedClay, Seq.with(new Research(waterPoweredDrill)), () -> {

            });
        });
    }
}
