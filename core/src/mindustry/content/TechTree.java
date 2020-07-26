package mindustry.content;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.content.Blocks.*;
import static mindustry.content.UnitTypes.*;

public class TechTree implements ContentList{
    private static ObjectMap<UnlockableContent, TechNode> map = new ObjectMap<>();

    public static Seq<TechNode> all;
    public static TechNode root;

    @Override
    public void load(){
        setup();

        root = node(coreShard, () -> {

            node(conveyor, () -> {

                node(junction, () -> {
                    node(router, () -> {
                        node(launchPad, () -> {
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

                                node(payloadConveyor, () -> {
                                    node(payloadRouter, () -> {

                                    });
                                });

                                node(armoredConveyor, () -> {
                                    node(plastaniumConveyor, () -> {

                                    });
                                });
                            });
                        });
                    });
                });
            });

            node(coreFoundation, () -> {
                node(coreNucleus, () -> {

                });
            });

            node(mechanicalDrill, () -> {

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

                node(Items.coal, () -> {
                    node(Items.graphite, () -> {
                        node(graphitePress, () -> {
                            node(Items.titanium, () -> {
                                node(pneumaticDrill, () -> {
                                    node(Items.sporePod, () -> {
                                        node(cultivator, () -> {

                                        });
                                    });

                                    node(Items.thorium, () -> {
                                        node(laserDrill, () -> {
                                            node(blastDrill, () -> {

                                            });

                                            node(waterExtractor, () -> {
                                                node(oilExtractor, () -> {

                                                });
                                            });
                                        });
                                    });
                                });
                            });

                            node(Items.pyratite, () -> {
                                node(pyratiteMixer, () -> {
                                    node(Items.blastCompound, () -> {
                                        node(blastMixer, () -> {

                                        });
                                    });
                                });
                            });

                            node(Items.silicon, () -> {
                                node(siliconSmelter, () -> {

                                    node(Liquids.oil, () -> {
                                        node(sporePress, () -> {
                                            node(coalCentrifuge, () -> {
                                                node(multiPress, () -> {
                                                    node(siliconCrucible, () -> {

                                                    });
                                                });
                                            });

                                            node(Items.plastanium, () -> {
                                                node(plastaniumCompressor, () -> {
                                                    node(Items.phasefabric, () -> {
                                                        node(phaseWeaver, () -> {

                                                        });
                                                    });
                                                });
                                            });
                                        });
                                    });

                                    node(Items.metaglass, () -> {
                                        node(kiln, () -> {
                                            node(incinerator, () -> {
                                                node(Items.scrap, () -> {
                                                    node(Liquids.slag, () -> {
                                                        node(melter, () -> {
                                                            node(Items.surgealloy, () -> {
                                                                node(surgeSmelter, () -> {

                                                                });
                                                            });

                                                            node(separator, () -> {
                                                                node(pulverizer, () -> {
                                                                    node(disassembler, () -> {

                                                                    });
                                                                });
                                                            });

                                                            node(Liquids.cryofluid, () -> {
                                                                node(cryofluidMixer, () -> {

                                                                });
                                                            });
                                                        });
                                                    });
                                                });
                                            });
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
                                            node(overdriveDome, () -> {

                                            });
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
                    });
                });
            });

            node(duo, () -> {
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
                            node(parallax, () -> {
                                node(segment, () -> {

                                });
                            });
                        });

                        node(lancer, () -> {
                            node(meltdown, () -> {

                            });

                            node(shockMine, () -> {

                            });
                        });
                    });
                });
            });

            node(groundFactory, () -> {
                node(dagger, () -> {
                    node(mace, () -> {
                        node(fortress, () -> {

                        });
                    });

                    node(nova, () -> {
                        node(pulsar, () -> {
                            node(quasar, () -> {

                            });
                        });
                    });

                    node(crawler, () -> {
                        node(atrax, () -> {
                            node(spiroct, () -> {

                            });
                        });
                    });
                });

                node(airFactory, () -> {
                    node(flare, () -> {
                        node(horizon, () -> {
                            node(zenith, () -> {
                                node(antumbra, () -> {
                                    node(eclipse, () -> {

                                    });
                                });
                            });
                        });

                        node(mono, () -> {
                            node(poly, () -> {
                                node(mega, () -> {

                                });
                            });
                        });
                    });

                    node(navalFactory, () -> {
                        node(risso, () -> {
                            node(minke, () -> {
                                node(bryde, () -> {

                                });
                            });
                        });
                    });
                });

                node(additiveReconstructor, () -> {
                    node(multiplicativeReconstructor, () -> {
                        node(exponentialReconstructor, () -> {
                            node(tetrativeReconstructor, () -> {
                            });
                        });
                    });
                });
            });

            //TODO research sectors
            /*
            node(SectorPresets.groundZero, () -> {
                node(SectorPresets.nuclearComplex, () -> {
                    node(SectorPresets.craters, () -> {
                        node(SectorPresets.saltFlats, () -> {

                        });
                    });
                });
            });
             */
        });
    }

    private static void setup(){
        TechNode.context = null;
        map = new ObjectMap<>();
        all = new Seq<>();
    }

    private static TechNode node(UnlockableContent content, Runnable children){
        ItemStack[] requirements;

        if(content instanceof Block){
            Block block = (Block)content;

            requirements = new ItemStack[block.requirements.length];
            for(int i = 0; i < requirements.length; i++){
                int quantity = 40 + Mathf.round(Mathf.pow(block.requirements[i].amount, 1.25f) * 20, 10);

                requirements[i] = new ItemStack(block.requirements[i].item, UI.roundAmount(quantity));
            }
        }else{
            requirements = ItemStack.empty;
        }

        return new TechNode(content, requirements, children);
    }

    private static TechNode node(UnlockableContent block){
        return node(block, () -> {});
    }

    public static TechNode create(UnlockableContent parent, UnlockableContent block){
        TechNode.context = all.find(t -> t.content == parent);
        return node(block, () -> {});
    }

    public static @Nullable TechNode get(UnlockableContent content){
        return map.get(content);
    }

    public static TechNode getNotNull(UnlockableContent content){
        return map.getThrow(content, () -> new RuntimeException(content + " does not have a tech node"));
    }

    public static class TechNode{
        private static TechNode context;

        /** Depth in tech tree. */
        public int depth;
        /** Requirement node. */
        public @Nullable TechNode parent;
        /** Content to be researched. */
        public UnlockableContent content;
        /** Item requirements for this content. */
        public ItemStack[] requirements;
        /** Extra objectives needed to research this. TODO implement */
        public Seq<Objective> objectives = new Seq<>();
        /** Time required to research this content, in seconds. */
        public float time;
        /** Nodes that depend on this node. */
        public final Seq<TechNode> children = new Seq<>();
        /** Research progress, in seconds. */
        public float progress;

        TechNode(@Nullable TechNode ccontext, UnlockableContent content, ItemStack[] requirements, Runnable children){
            if(ccontext != null){
                ccontext.children.add(this);
            }

            this.parent = ccontext;
            this.content = content;
            this.requirements = requirements;
            this.depth = parent == null ? 0 : parent.depth + 1;
            this.progress = Core.settings == null ? 0 : Core.settings.getFloat("research-" + content.name, 0f);
            this.time = Seq.with(requirements).mapFloat(i -> i.item.cost * i.amount).sum() * 10;

            //add dependencies as objectives.
            content.getDependencies(d -> objectives.add(new Research(d)));

            map.put(content, this);
            context = this;
            children.run();
            context = ccontext;
            all.add(this);
        }

        TechNode(UnlockableContent content, ItemStack[] requirements, Runnable children){
            this(context, content, requirements, children);
        }

        /** Flushes research progress to settings. */
        public void save(){
            Core.settings.put("research-" + content.name, progress);
        }
    }
}
