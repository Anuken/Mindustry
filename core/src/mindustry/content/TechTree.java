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
import static mindustry.content.SectorPresets.*;
import static mindustry.content.SectorPresets.craters;
import static mindustry.content.UnitTypes.*;
import static mindustry.type.ItemStack.*;

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

                node(Items.coal, with(Items.lead, 3000), () -> {
                    node(Items.graphite, with(Items.coal, 1000), () -> {
                        node(graphitePress, () -> {
                            node(Items.titanium, with(Items.graphite, 6000, Items.copper, 10000, Items.lead, 10000), () -> {
                                node(pneumaticDrill, () -> {
                                    node(Items.sporePod, with(Items.coal, 5000, Items.graphite, 5000, Items.lead, 5000), () -> {
                                        node(cultivator, () -> {

                                        });
                                    });

                                    node(Items.thorium, with(Items.titanium, 10000, Items.lead, 15000, Items.copper, 30000), () -> {
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

                            node(Items.pyratite, with(Items.coal, 6000, Items.lead, 10000, Items.sand, 5000), () -> {
                                node(pyratiteMixer, () -> {
                                    node(Items.blastCompound, with(Items.pyratite, 3000, Items.sporePod, 3000), () -> {
                                        node(blastMixer, () -> {

                                        });
                                    });
                                });
                            });

                            node(Items.silicon, with(Items.coal, 4000, Items.sand, 4000), () -> {
                                node(siliconSmelter, () -> {

                                    node(Liquids.oil, with(Items.coal, 8000, Items.pyratite, 6000, Items.sand, 20000), () -> {
                                        node(sporePress, () -> {
                                            node(coalCentrifuge, () -> {
                                                node(multiPress, () -> {
                                                    node(siliconCrucible, () -> {

                                                    });
                                                });
                                            });

                                            node(Items.plastanium, with(Items.titanium, 10000, Items.silicon, 10000), () -> {
                                                node(plastaniumCompressor, () -> {
                                                    node(Items.phasefabric, with(Items.thorium, 15000, Items.sand, 30000, Items.silicon, 5000), () -> {
                                                        node(phaseWeaver, () -> {

                                                        });
                                                    });
                                                });
                                            });
                                        });
                                    });

                                    node(Items.metaglass, with(Items.sand, 6000, Items.lead, 10000), () -> {
                                        node(kiln, () -> {
                                            node(incinerator, () -> {
                                                node(Items.scrap, with(Items.copper, 20000, Items.sand, 10000), () -> {
                                                    node(Liquids.slag, with(Items.scrap, 4000), () -> {
                                                        node(melter, () -> {
                                                            node(Items.surgealloy, with(Items.thorium, 20000, Items.silicon, 30000, Items.lead, 40000), () -> {
                                                                node(surgeSmelter, () -> {

                                                                });
                                                            });

                                                            node(separator, () -> {
                                                                node(pulverizer, () -> {
                                                                    node(disassembler, () -> {

                                                                    });
                                                                });
                                                            });

                                                            node(Liquids.cryofluid, with(Items.titanium, 8000, Items.metaglass, 5000), () -> {
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

            node(groundZero, () -> {
                node(frozenForest, Seq.with(
                    new SectorComplete(groundZero),
                    new Research(junction),
                    new Research(router)
                ), () -> {
                    node(craters, Seq.with(
                        new SectorComplete(frozenForest),
                        new Research(mender),
                        new Research(combustionGenerator)
                    ), () -> {
                        node(ruinousShores, Seq.with(
                            new SectorComplete(craters),
                            new Research(graphitePress),
                            new Research(combustionGenerator),
                            new Research(kiln),
                            new Research(mechanicalPump)
                        ), () -> {

                            node(tarFields, Seq.with(
                                new SectorComplete(ruinousShores),
                                new Research(coalCentrifuge),
                                new Research(conduit),
                                new Research(wave)
                            ), () -> {
                                node(desolateRift, Seq.with(
                                    new SectorComplete(tarFields),
                                    new Research(thermalGenerator),
                                    new Research(thoriumReactor)
                                ), () -> {

                                });
                            });

                            node(saltFlats, Seq.with(
                                new SectorComplete(ruinousShores),
                                new Research(groundFactory),
                                new Research(airFactory),
                                new Research(door),
                                new Research(waterExtractor)
                            ), () -> {

                            });
                        });

                        node(overgrowth, Seq.with(
                            new SectorComplete(craters),
                            new SectorComplete(fungalPass),
                            new Research(cultivator),
                            new Research(sporePress),
                            new Research(UnitTypes.mace),
                            new Research(UnitTypes.flare)
                        ), () -> {

                        });
                    });

                    node(stainedMountains, Seq.with(
                        new SectorComplete(frozenForest),
                        new Research(pneumaticDrill),
                        new Research(powerNode),
                        new Research(turbineGenerator)
                    ), () -> {
                        node(fungalPass, Seq.with(
                            new SectorComplete(stainedMountains),
                            new Research(groundFactory),
                            new Research(door),
                            new Research(siliconSmelter)
                        ), () -> {
                            node(nuclearComplex, Seq.with(
                                new SectorComplete(fungalPass),
                                new Research(thermalGenerator),
                                new Research(laserDrill)
                            ), () -> {

                            });
                        });
                    });
                });
            });
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

        return node(content, requirements, children);
    }

    private static TechNode node(UnlockableContent content, ItemStack[] requirements, Runnable children){
        return new TechNode(content, requirements, children);
    }

    private static TechNode node(UnlockableContent content, Seq<Objective> objectives, Runnable children){
        TechNode node = new TechNode(content, empty, children);
        node.objectives = objectives;
        return node;
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
        /** Requirements that have been fulfilled. Always the same length as the requirement array. */
        public final ItemStack[] finishedRequirements;
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
            this.finishedRequirements = new ItemStack[requirements.length];

            //load up the requirements that have been finished if settings are available
            for(int i = 0; i < requirements.length; i++){
                finishedRequirements[i] = new ItemStack(requirements[i].item, Core.settings == null ? 0 : Core.settings.getInt("req-" + content.name + "-" + requirements[i].item.name));
            }

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

            //save finished requirements by item type
            for(ItemStack stack : finishedRequirements){
                Core.settings.put("req-" + content.name + "-" + stack.item.name, stack.amount);
            }
        }
    }
}
