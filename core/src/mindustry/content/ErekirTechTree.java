package mindustry.content;

import arc.struct.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;

import static mindustry.content.Blocks.*;
import static mindustry.content.TechTree.*;

public class ErekirTechTree{

    public static void load(){
        Seq<Objective> erekirSector = Seq.with(new OnPlanet(Planets.erekir));

        var costMultipliers = new ObjectFloatMap<Item>();
        costMultipliers.put(Items.silicon, 6);
        costMultipliers.put(Items.surgeAlloy, 4);
        costMultipliers.put(Items.thorium, 7);
        costMultipliers.put(Items.graphite, 6);

        Planets.erekir.techTree = nodeRoot("erekir", coreBastion, true, () -> {
            context().researchCostMultipliers = costMultipliers;

            node(duct, () -> {
                node(ductRouter, () -> {
                    node(ductBridge, () -> {
                        node(surgeConveyor, () -> {
                            node(surgeRouter);
                        });

                        node(unitCargoLoader, () -> {
                            node(unitCargoUnloadPoint, () -> {

                            });
                        });
                    });

                    node(overflowDuct, () -> {
                        node(ductUnloader, () -> {

                        });
                    });

                    node(reinforcedContainer, () -> {
                        node(reinforcedVault, () -> {

                        });
                    });
                });

                node(constructor, erekirSector, () -> {
                    node(payloadLoader, () -> {
                        node(payloadUnloader, () -> {
                            node(payloadPropulsionTower, () -> {

                            });
                        });
                    });

                    node(smallDeconstructor, () -> {
                        node(largeConstructor, () -> {

                        });

                        node(deconstructor, () -> {

                        });
                    });
                });
            });

            node(turbineCondenser, () -> {
                node(beamNode, () -> {
                    node(ventCondenser, erekirSector, () -> {
                        node(chemicalCombustionChamber, () -> {
                            node(pyrolysisGenerator, () -> {

                            });
                        });
                    });

                    node(beamTower, () -> {

                    });


                    node(regenProjector, () -> {
                        //TODO more tiers of build tower or "support" structures like overdrive projectors
                        node(buildTower, () -> {

                        });
                    });
                });
            });

            node(siliconArcFurnace, () -> {
                node(cliffCrusher, () -> {
                    node(electrolyzer, erekirSector, () -> {
                        node(oxidationChamber, () -> {
                            node(electricHeater, () -> {
                                node(heatRedirector, () -> {

                                });

                                node(atmosphericConcentrator, () -> {
                                    node(cyanogenSynthesizer, () -> {

                                    });
                                });

                                node(carbideCrucible, () -> {
                                    node(surgeCrucible, () -> {
                                        node(phaseSynthesizer, () -> {
                                            node(phaseHeater, () -> {

                                            });
                                        });
                                    });
                                });
                            });
                        });

                        node(slagIncinerator, () -> {

                            node(slagCentrifuge, () -> {

                            });

                            node(heatReactor, () -> {

                            });
                        });
                    });
                });
            });

            //TODO move into turbine condenser?
            node(plasmaBore, () -> {

                node(impactDrill, erekirSector, () -> {
                    node(largePlasmaBore, () -> {
                        node(eruptionDrill, () -> {

                        });
                    });
                });
            });

            node(reinforcedConduit, erekirSector, () -> {
                node(reinforcedPump, () -> {
                    //TODO T2 pump
                });

                node(reinforcedLiquidJunction, () -> {
                    node(reinforcedBridgeConduit, () -> {

                    });

                    node(reinforcedLiquidRouter, () -> {
                        node(reinforcedLiquidContainer, () -> {
                            node(reinforcedLiquidTank, () -> {

                            });
                        });
                    });
                });
            });

            node(breach, () -> {
                node(berylliumWall, () -> {
                    node(berylliumWallLarge, () -> {

                    });

                    node(tungstenWall, () -> {
                        node(tungstenWallLarge, () -> {

                        });
                    });
                });


                //TODO implement
                node(sublimate, () -> {
                    node(titan, () -> {

                    });
                });
            });

            node(coreCitadel, () -> {
                node(coreAcropolis, () -> {

                });
            });

            nodeProduce(Items.beryllium, () -> {
                nodeProduce(Items.oxide, () -> {
                    nodeProduce(Items.fissileMatter, () -> {

                    });
                });

                nodeProduce(Liquids.ozone, () -> {
                    nodeProduce(Liquids.hydrogen, () -> {
                        nodeProduce(Liquids.nitrogen, () -> {
                            nodeProduce(Liquids.cyanogen, () -> {

                            });
                        });
                    });
                });

                nodeProduce(Items.tungsten, () -> {
                    nodeProduce(Items.carbide, () -> {
                        nodeProduce(Liquids.gallium, () -> {

                        });
                    });
                });
            });
        });
    }
}
