package mindustry.content;

import arc.math.*;
import arc.struct.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.content.Blocks.*;

public class TechTree implements ContentList{
    public static Array<TechNode> all;
    public static TechNode root;

    @Override
    public void load(){
        TechNode.context = null;
        all = new Array<>();

        root = node(coreShard, () -> {

            node(conveyor, () -> {

                node(junction, () -> {
                    node(router, () -> {
                        node(launchPad, () -> {
                            node(launchPadLarge, () -> {

                            });
                        });

                        node(distributor);
                        node(sorter, () -> {
                            node(invertedSorter);
                            node(message);
                            node(overflowGate, () -> {
                                node(underflowGate);
                            });
                        });
                        node(container, () -> {
                            node(unloader);
                            node(vault, () -> {

                            });
                        });

                        node(itemBridge, () -> {
                            node(titaniumConveyor, () -> {
                                node(phaseConveyor, () -> {
                                    node(massDriver, () -> {

                                    });
                                });

                                node(armoredConveyor, () -> {

                                });
                            });
                        });
                    });
                });
            });

            node(duo, () -> {
                node(scatter, () -> {
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
                });

                node(scorch, () -> {
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
                });


                node(copperWall, () -> {
                    node(copperWallLarge, () -> {
                        node(titaniumWall, () -> {
                            node(titaniumWallLarge);

                            node(door, () -> {
                                node(doorLarge);
                            });
                            node(plastaniumWall, () -> {
                                node(plastaniumWallLarge, () -> {

                                });
                            });
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
            });

            node(mechanicalDrill, () -> {
                node(graphitePress, () -> {
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

                    node(pyratiteMixer, () -> {
                        node(blastMixer, () -> {

                        });
                    });

                    node(siliconSmelter, () -> {

                        node(sporePress, () -> {
                            node(coalCentrifuge, () -> {

                            });
                            node(multiPress, () -> {

                            });

                            node(plastaniumCompressor, () -> {
                                node(phaseWeaver, () -> {

                                });
                            });
                        });

                        node(kiln, () -> {
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
                });


                node(mechanicalPump, () -> {
                    node(conduit, () -> {
                        node(liquidJunction, () -> {
                            node(liquidRouter, () -> {
                                node(liquidTank);

                                node(bridgeConduit);

                                node(pulseConduit, () -> {
                                    node(phaseConduit, () -> {

                                    });

                                    node(platedConduit, () -> {

                                    });
                                });

                                node(rotaryPump, () -> {
                                    node(thermalPump, () -> {

                                    });
                                });
                            });
                        });
                    });
                });

                node(combustionGenerator, () -> {
                    node(powerNode, () -> {
                        node(powerNodeLarge, () -> {
                            node(diode, () -> {
                                node(surgeTower, () -> {

                                });
                            });
                        });

                        node(battery, () -> {
                            node(batteryLarge, () -> {

                            });
                        });

                        node(mender, () -> {
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
                                node(differentialGenerator, () -> {
                                    node(thoriumReactor, () -> {
                                        node(impactReactor, () -> {

                                        });

                                        node(rtgGenerator, () -> {

                                        });
                                    });
                                });
                            });
                        });

                        node(solarPanel, () -> {
                            node(largeSolarPanel, () -> {

                            });
                        });
                    });

                    node(draugFactory, () -> {
                        node(spiritFactory, () -> {
                            node(phantomFactory);
                        });

                        node(daggerFactory, () -> {
                            node(commandCenter, () -> {});
                            node(crawlerFactory, () -> {
                                node(titanFactory, () -> {
                                    node(fortressFactory, () -> {

                                    });
                                });
                            });

                            node(wraithFactory, () -> {
                                node(ghoulFactory, () -> {
                                    node(revenantFactory, () -> {

                                    });
                                });
                            });
                        });
                    });

                    node(dartPad, () -> {
                        node(deltaPad, () -> {

                            node(javelinPad, () -> {
                                node(tridentPad, () -> {
                                    node(glaivePad);
                                });
                            });

                            node(tauPad, () -> {
                                node(omegaPad, () -> {

                                });
                            });
                        });
                    });
                });
            });
        });
    }

    private static TechNode node(Block block, Runnable children){
        ItemStack[] requirements = new ItemStack[block.requirements.length];
        for(int i = 0; i < requirements.length; i++){
            requirements[i] = new ItemStack(block.requirements[i].item, 40 + Mathf.round(Mathf.pow(block.requirements[i].amount, 1.25f) * 6, 10));
        }

        return new TechNode(block, requirements, children);
    }

    private static TechNode node(Block block){
        return node(block, () -> {});
    }

    public static TechNode create(Block parent, Block block){
        TechNode.context = all.find(t -> t.block == parent);
        return node(block, () -> {});
    }

    public static class TechNode{
        static TechNode context;

        public final Block block;
        public final ItemStack[] requirements;
        public final Array<TechNode> children = new Array<>();

        TechNode(TechNode ccontext, Block block, ItemStack[] requirements, Runnable children){
            if(ccontext != null){
                ccontext.children.add(this);
            }

            this.block = block;
            this.requirements = requirements;

            context = this;
            children.run();
            context = ccontext;
            all.add(this);
        }

        TechNode(Block block, ItemStack[] requirements, Runnable children){
            this(context, block, requirements, children);
        }
    }
}
