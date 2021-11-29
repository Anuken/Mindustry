package mindustry.content;

import static mindustry.content.Blocks.*;
import static mindustry.content.TechTree.*;

public class ErekirTechTree{

    public static void load(){
        rootErekir = node(coreBastion, () -> {
            node(duct, () -> {
                node(ductRouter, () -> {
                    node(ductBridge, () -> {
                        node(surgeConveyor, () -> {
                            node(surgeRouter);
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

                    node(deconstructor, () -> {
                        node(largeConstructor, () -> {

                        });
                    });
                });
            });

            node(turbineCondenser, () -> {
                node(beamNode, () -> {
                    node(beamTower, () -> {

                    });

                    //TODO more tiers of build tower or "support" structures like overdrive projectors
                    //TODO method of repairing blocks of damage
                    node(buildTower, () -> {

                    });
                });
            });

            //TODO where in the tech tree is the arc furnace placed? is it essential?
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
                            //TODO
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
                    //TODO req
                    node(largePlasmaBore, () -> {

                    });

                    //TODO req
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
                //fracture turret is broken and thus not listed

                //TODO big tech jump here; incomplete turret
                node(sublimate, () -> {

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
                        //TODO how will nitrogen be gated behind the electrolyzer?
                        nodeProduce(Liquids.nitrogen, () -> {
                            nodeProduce(Liquids.cyanogen, () -> {

                            });
                        });
                    });
                });

                nodeProduce(Items.tungsten, () -> {
                    nodeProduce(Items.carbide, () -> {

                    });

                    //TODO does it require carbide?
                    nodeProduce(Liquids.gallium, () -> {

                    });
                });
            });

        });
    }
}
