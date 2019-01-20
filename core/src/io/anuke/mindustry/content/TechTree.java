package io.anuke.mindustry.content;

import io.anuke.arc.collection.Array;
import io.anuke.mindustry.game.ContentList;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.world.Block;

import static io.anuke.mindustry.type.ItemStack.with;

public class TechTree implements ContentList{
    public static TechNode root;

    @Override
    public void load(){
        root = node(Blocks.core, with(), () -> {

            node(Blocks.conveyor, with(Items.copper, 100), () -> {
                node(Blocks.launchPad, with(Items.copper, 100), () -> {

                });

                node(Blocks.junction, with(Items.copper, 100), () -> {
                    node(Blocks.itemBridge, with(Items.copper, 100));
                    node(Blocks.router, with(Items.copper, 100), () -> {
                        node(Blocks.distributor, with(Items.copper, 100));
                        node(Blocks.overflowGate, with(Items.copper, 100));
                        node(Blocks.sorter, with(Items.copper, 100));
                        node(Blocks.container, with(Items.copper, 100), () -> {
                            node(Blocks.unloader, with(Items.copper, 100));
                            node(Blocks.vault, with(Items.copper, 100), () -> {

                            });
                        });
                    });
                    node(Blocks.titaniumConveyor, with(Items.copper, 100), () -> {
                        node(Blocks.phaseConveyor, with(Items.copper, 100), () -> {
                            node(Blocks.massDriver, with(Items.copper, 100), () -> {

                            });
                        });
                    });
                });
            });

            node(Blocks.duo, with(Items.copper, 100), () -> {
                node(Blocks.hail, with(Items.copper, 100), () -> {

                    node(Blocks.salvo, with(Items.copper, 100), () -> {
                        node(Blocks.swarmer, with(Items.copper, 100), () -> {
                            node(Blocks.cyclone, with(Items.copper, 100), () -> {
                                node(Blocks.spectre, with(Items.copper, 100), () -> {

                                });
                            });
                        });

                        node(Blocks.ripple, with(Items.copper, 100), () -> {
                            node(Blocks.fuse, with(Items.copper, 100), () -> {

                            });
                        });
                    });
                });

                node(Blocks.arc, with(Items.copper, 100), () -> {
                    node(Blocks.wave, with(Items.copper, 100), () -> {

                    });

                    node(Blocks.lancer, with(Items.copper, 100), () -> {
                        node(Blocks.meltdown, with(Items.copper, 100), () -> {

                        });

                        node(Blocks.shockMine, with(Items.metaglass, 100), () -> {

                        });
                    });
                });

                node(Blocks.copperWall, with(Items.copper, 100), () -> {
                    node(Blocks.copperWallLarge, with(Items.copper, 100));
                    node(Blocks.titaniumWall, with(Items.copper, 100), () -> {
                        node(Blocks.door, with(Items.copper, 100), () -> {
                            node(Blocks.doorLarge, with(Items.copper, 100));
                        });
                        node(Blocks.titaniumWallLarge, with(Items.copper, 100));
                        node(Blocks.thoriumWall, with(Items.copper, 100), () -> {
                            node(Blocks.thoriumWallLarge, with(Items.copper, 100));
                            node(Blocks.surgeWall, with(Items.copper, 100), () -> {
                                node(Blocks.surgeWallLarge, with(Items.copper, 100));
                                node(Blocks.phaseWall, with(Items.copper, 100), () -> {
                                    node(Blocks.phaseWallLarge, with(Items.copper, 100));
                                });
                            });
                        });
                    });
                });
            });

            node(Blocks.mechanicalDrill, with(Items.copper, 100), () -> {
                node(Blocks.pneumaticDrill, with(Items.copper, 100), () -> {
                    node(Blocks.cultivator, with(Items.copper, 100), () -> {

                    });

                    node(Blocks.laserDrill, with(Items.copper, 100), () -> {
                        node(Blocks.blastDrill, with(Items.copper, 100), () -> {

                        });

                        node(Blocks.waterExtractor, with(Items.copper, 100), () -> {
                            node(Blocks.oilExtractor, with(Items.copper, 100), () -> {

                            });
                        });
                    });
                });

                node(Blocks.siliconSmelter, with(Items.copper, 100), () -> {

                    node(Blocks.pyratiteMixer, with(Items.copper, 100), () -> {
                        node(Blocks.blastMixer, with(Items.copper, 100), () -> {

                        });
                    });

                    node(Blocks.biomatterCompressor, with(Items.copper, 100), () -> {
                        node(Blocks.plastaniumCompressor, with(Items.copper, 100), () -> {
                            node(Blocks.phaseWeaver, with(Items.copper, 100), () -> {

                            });
                        });
                    });

                    node(Blocks.incinerator, with(Items.copper, 100), () -> {
                        node(Blocks.melter, with(Items.copper, 100), () -> {
                            node(Blocks.surgeSmelter, with(Items.copper, 100), () -> {

                            });

                            node(Blocks.separator, with(Items.copper, 100), () -> {
                                node(Blocks.pulverizer, with(Items.copper, 100), () -> {

                                });
                            });

                            node(Blocks.cryofluidMixer, with(Items.copper, 100), () -> {

                            });
                        });
                    });
                });

                node(Blocks.mechanicalPump, with(Items.metaglass, 100), () -> {
                    node(Blocks.conduit, with(Items.metaglass, 100), () -> {
                        node(Blocks.liquidJunction, with(Items.metaglass, 100), () -> {
                            node(Blocks.liquidRouter, with(Items.metaglass, 100), () -> {
                                node(Blocks.liquidTank, with(Items.metaglass, 100));

                                node(Blocks.pulseConduit, with(Items.metaglass, 100), () -> {
                                    node(Blocks.phaseConduit, with(Items.metaglass, 100), () -> {

                                    });
                                });

                                node(Blocks.rotaryPump, with(Items.metaglass, 100), () -> {
                                    node(Blocks.thermalPump, with(Items.metaglass, 100), () -> {

                                    });
                                });
                            });
                            node(Blocks.bridgeConduit, with(Items.metaglass, 100));
                        });
                    });
                });
            });

            node(Blocks.powerNode, with(Items.copper, 100), () -> {
                node(Blocks.combustionGenerator, with(Items.copper, 100), () -> {
                    node(Blocks.powerNodeLarge, with(Items.copper, 100), () -> {
                        node(Blocks.battery, with(Items.copper, 100), () -> {
                            node(Blocks.batteryLarge, with(Items.copper, 100), () -> {

                            });
                        });

                        node(Blocks.mendProjector, with(Items.copper, 100), () -> {
                            node(Blocks.forceProjector, with(Items.copper, 100), () -> {
                                node(Blocks.overdriveProjector, with(Items.copper, 100), () -> {

                                });
                            });

                            node(Blocks.repairPoint, with(Items.copper, 100), () -> {

                            });
                        });
                    });

                    node(Blocks.turbineGenerator, with(Items.copper, 100), () -> {
                        node(Blocks.thermalGenerator, with(Items.copper, 100), () -> {
                            node(Blocks.rtgGenerator, with(Items.copper, 100), () -> {
                                node(Blocks.thoriumReactor, with(Items.copper, 100), () -> {

                                });
                            });
                        });
                    });

                    node(Blocks.solarPanel, with(Items.copper, 100), () -> {
                        node(Blocks.largeSolarPanel, with(Items.copper, 100), () -> {

                        });
                    });
                });

                node(Blocks.alphaPad, with(Items.copper, 100), () -> {
                    node(Blocks.dartPad, with(Items.copper, 100));
                    node(Blocks.deltaPad, with(Items.copper, 100), () -> {
                        node(Blocks.javelinPad, with(Items.copper, 100));
                        node(Blocks.tauPad, with(Items.copper, 100), () -> {
                            node(Blocks.tridentPad, with(Items.copper, 100));
                            node(Blocks.omegaPad, with(Items.copper, 100), () -> {
                                node(Blocks.glaivePad, with(Items.copper, 100));
                            });
                        });
                    });

                    node(Blocks.spiritFactory, with(Items.copper, 100), () -> {
                        node(Blocks.daggerFactory, with(Items.copper, 100), () -> {
                            node(Blocks.daggerFactory, with(Items.copper, 100), () -> {
                                node(Blocks.titanFactory, with(Items.copper, 100), () -> {
                                    node(Blocks.fortressFactory, with(Items.copper, 100));
                                });
                                node(Blocks.wraithFactory, with(Items.copper, 100), () -> {
                                    node(Blocks.phantomFactory, with(Items.copper, 100));
                                    node(Blocks.ghoulFactory, with(Items.copper, 100), () -> {
                                        node(Blocks.revenantFactory, with(Items.copper, 100));
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    }

    private TechNode node(Block block, ItemStack[] requirements, Runnable children){
        return new TechNode(block, requirements, children);
    }

    private void node(Block block, ItemStack[] requirements){
        new TechNode(block, requirements, () -> {});
    }

    public static class TechNode{
        static TechNode context;

        public final Block block;
        public final ItemStack[] requirements;
        public final Array<TechNode> children = new Array<>();

        TechNode(Block block, ItemStack[] requirements, Runnable children){
            if(block != null && Recipe.getByResult(block) == null) throw new IllegalArgumentException("Block " + block + " does not have a recipe.");
            if(context != null){
                context.children.add(this);
            }

            this.block = block;
            this.requirements = requirements;

            TechNode last = context;
            context = this;
            children.run();
            context = last;
        }

        public Recipe recipe(){
            return Recipe.getByResult(block);
        }
    }
}
