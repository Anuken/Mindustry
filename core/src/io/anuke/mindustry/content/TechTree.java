package io.anuke.mindustry.content;

import io.anuke.arc.collection.Array;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.content.Blocks.*;

public class TechTree implements ContentList{
    public static TechNode root;

    @Override
    public void load(){
        root = node(core, () -> {

            node(conveyor, () -> {
                node(launchPad, () -> {

                });

                node(junction, () -> {
                    node(itemBridge);
                    node(router, () -> {
                        node(distributor);
                        node(overflowGate);
                        node(sorter);
                        node(container, () -> {
                            node(unloader);
                            node(vault, () -> {

                            });
                        });
                    });
                    node(titaniumConveyor, () -> {
                        node(phaseConveyor, () -> {
                            node(massDriver, () -> {

                            });
                        });
                    });
                });
            });

            node(duo, () -> {
                node(hail, () -> {

                    node(salvo, () -> {
                        node(swarmer, () -> {
                            node(cyclone, () -> {
                                node(spectre, () -> {

                                });
                            });
                        });

                        node(ripple, () -> {
                            node(fuse, () -> {

                            });
                        });
                    });
                });

                node(arc, () -> {
                    node(wave, () -> {

                    });

                    node(lancer, () -> {
                        node(meltdown, () -> {

                        });

                        node(shockMine, () -> {

                        });
                    });
                });

                node(copperWall, () -> {
                    node(copperWallLarge);
                    node(titaniumWall, () -> {
                        node(door, () -> {
                            node(doorLarge);
                        });
                        node(titaniumWallLarge);
                        node(thoriumWall, () -> {
                            node(thoriumWallLarge);
                            node(surgeWall, () -> {
                                node(surgeWallLarge);
                                node(phaseWall, () -> {
                                    node(phaseWallLarge);
                                });
                            });
                        });
                    });
                });
            });

            node(mechanicalDrill, () -> {
                node(pneumaticDrill, () -> {
                    node(cultivator, () -> {

                    });

                    node(laserDrill, () -> {
                        node(blastDrill, () -> {

                        });

                        node(waterExtractor, () -> {
                            node(oilExtractor, () -> {

                            });
                        });
                    });
                });

                node(graphitePress, () -> {
                    node(pyratiteMixer, () -> {
                        node(blastMixer, () -> {

                        });
                    });

                    node(siliconSmelter, () -> {

                        node(biomatterCompressor, () -> {
                            node(plastaniumCompressor, () -> {
                                node(phaseWeaver, () -> {

                                });
                            });
                        });

                        node(incinerator, () -> {
                            node(melter, () -> {
                                node(surgeSmelter, () -> {

                                });

                                node(separator, () -> {
                                    node(pulverizer, () -> {

                                    });
                                });

                                node(cryofluidMixer, () -> {

                                });
                            });
                        });
                    });
                });



                node(mechanicalPump, () -> {
                    node(conduit, () -> {
                        node(liquidJunction, () -> {
                            node(liquidRouter, () -> {
                                node(liquidTank);

                                node(pulseConduit, () -> {
                                    node(phaseConduit, () -> {

                                    });
                                });

                                node(rotaryPump, () -> {
                                    node(thermalPump, () -> {

                                    });
                                });
                            });
                            node(bridgeConduit);
                        });
                    });
                });
            });

            node(powerNode, () -> {
                node(combustionGenerator, () -> {
                    node(powerNodeLarge, () -> {
                        node(battery, () -> {
                            node(batteryLarge, () -> {

                            });
                        });

                        node(mendProjector, () -> {
                            node(forceProjector, () -> {
                                node(overdriveProjector, () -> {

                                });
                            });

                            node(repairPoint, () -> {

                            });
                        });
                    });

                    node(turbineGenerator, () -> {
                        node(thermalGenerator, () -> {
                            node(rtgGenerator, () -> {
                                node(thoriumReactor, () -> {

                                });
                            });
                        });
                    });

                    node(solarPanel, () -> {
                        node(largeSolarPanel, () -> {

                        });
                    });
                });

                node(alphaPad, () -> {
                    node(dartPad);
                    node(deltaPad, () -> {
                        node(javelinPad);
                        node(tauPad, () -> {
                            node(tridentPad);
                            node(omegaPad, () -> {
                                node(glaivePad);
                            });
                        });
                    });

                    node(spiritFactory, () -> {
                        node(daggerFactory, () -> {
                            node(daggerFactory, () -> {
                                node(titanFactory, () -> {
                                    node(fortressFactory);
                                });
                                node(wraithFactory, () -> {
                                    node(phantomFactory);
                                    node(ghoulFactory, () -> {
                                        node(revenantFactory);
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    private TechNode node(Block block, Runnable children){
        ItemStack[] requirements = new ItemStack[block.buildRequirements.length];
        for(int i = 0; i < requirements.length; i++){
            requirements[i] = new ItemStack(block.buildRequirements[i].item, block.buildRequirements[i].amount * 50);
        }
        
        return new TechNode(block, requirements, children);
    }

    private TechNode node(Block block){
        return node(block, () -> {});
    }

    public static class TechNode{
        static TechNode context;

        public final Block block;
        public final ItemStack[] requirements;
        public final Array<TechNode> children = new Array<>();

        TechNode(Block block, ItemStack[] requirements, Runnable children){
            if(context != null){
                context.children.add(this);
            }

            //TODO remove requirements... for now
            this.block = block;
            this.requirements = requirements;

            TechNode last = context;
            context = this;
            children.run();
            context = last;
        }
    }
}
