package mindustry.content;

import static mindustry.content.Blocks.*;
import static mindustry.content.TechTree.*;

public class ErekirTechTree{

    public static void load(){
        Planets.erekir.techTree = nodeRoot("erekir", coreBastion, () -> {
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

                node(constructor, () -> {
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
                    node(chemicalCombustionChamber, () -> {
                        node(pyrolysisGenerator, () -> {

                        });
                    });

                    node(beamTower, () -> {

                    });

                    //TODO more tiers of build tower or "support" structures like overdrive projectors
                    //TODO method of repairing blocks of damage
                    node(buildTower, () -> {

                    });
                });
            });

            node(siliconArcFurnace, () -> {
                node(electrolyzer, () -> {
                    node(oxidationChamber, () -> {
                        node(atmosphericConcentrator, () -> {
                            node(cyanogenSynthesizer, () -> {

                            });
                        });

                        node(carbideCrucible, () -> {
                            node(surgeCrucible, () -> {
                                node(phaseSynthesizer, () -> {

                                });
                            });
                        });
                    });

                    node(slagIncinerator, () -> {
                        //when is this actually needed?
                        node(slagHeater, () -> {
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
                node(cliffCrusher, () -> {
                    node(largePlasmaBore, () -> {

                    });

                    node(impactDrill, () -> {

                    });
                });
            });

            node(reinforcedConduit, () -> {
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

                node(fracture, () -> {

                    //TODO big tech jump here; incomplete turret
                    node(sublimate, () -> {

                    });
                });
            });

            //TODO requirements for these
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
