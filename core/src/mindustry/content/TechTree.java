package mindustry.content;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;

import static mindustry.content.Blocks.*;
import static mindustry.content.SectorPresets.craters;
import static mindustry.content.SectorPresets.*;
import static mindustry.content.UnitTypes.*;
import static mindustry.type.ItemStack.*;

public class TechTree implements ContentList{
    static ObjectMap<UnlockableContent, TechNode> map = new ObjectMap<>();
    static TechNode context = null;

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
                            node(Items.titanium, with(Items.graphite, 3000, Items.copper, 7000, Items.lead, 7000), () -> {
                                node(pneumaticDrill, () -> {
                                    node(Items.sporePod, with(Items.coal, 4000, Items.graphite, 4000, Items.lead, 4000), () -> {
                                        node(cultivator, () -> {

                                        });
                                    });

                                    node(Items.thorium, with(Items.titanium, 8000, Items.lead, 12000, Items.copper, 20000), () -> {
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

                            node(Items.pyratite, with(Items.coal, 6000, Items.lead, 8000, Items.sand, 4000), () -> {
                                node(pyratiteMixer, () -> {
                                    node(Items.blastCompound, with(Items.pyratite, 3000, Items.sporePod, 3000), () -> {
                                        node(blastMixer, () -> {

                                        });
                                    });
                                });
                            });

                            node(Items.silicon, with(Items.coal, 3000, Items.sand, 4000), () -> {
                                node(siliconSmelter, () -> {

                                    node(Liquids.oil, with(Items.coal, 8000, Items.pyratite, 6000, Items.sand, 8000), () -> {
                                        node(sporePress, () -> {
                                            node(coalCentrifuge, () -> {
                                                node(multiPress, () -> {
                                                    node(siliconCrucible, () -> {

                                                    });
                                                });
                                            });

                                            node(Items.plastanium, with(Items.titanium, 8000, Items.silicon, 8000), () -> {
                                                node(plastaniumCompressor, () -> {
                                                    node(Items.phaseFabric, with(Items.thorium, 12000, Items.sand, 8000, Items.silicon, 5000), () -> {
                                                        node(phaseWeaver, () -> {

                                                        });
                                                    });
                                                });
                                            });
                                        });
                                    });

                                    node(Items.metaglass, with(Items.sand, 4000, Items.lead, 10000), () -> {
                                        node(kiln, () -> {
                                            node(incinerator, () -> {
                                                node(Items.scrap, with(Items.copper, 8000, Items.sand, 4000), () -> {
                                                    node(Liquids.slag, with(Items.scrap, 4000), () -> {
                                                        node(melter, () -> {
                                                            node(Items.surgeAlloy, with(Items.thorium, 20000, Items.silicon, 20000, Items.lead, 40000), () -> {
                                                                node(surgeSmelter, () -> {

                                                                });
                                                            });

                                                            node(separator, () -> {
                                                                node(pulverizer, () -> {
                                                                    node(disassembler, () -> {

                                                                    });
                                                                });
                                                            });

                                                            node(Liquids.cryofluid, with(Items.titanium, 8000, Items.metaglass, 4000), () -> {
                                                                node(cryofluidMixer, () -> {

                                                                });
                                                            });
                                                        });
                                                    });
                                                });
                                            });
                                        });
                                    });

                                    node(microProcessor, () -> {
                                        node(switchBlock, () -> {
                                            node(message, () -> {
                                                node(logicDisplay, () -> {
                                                    node(largeLogicDisplay, () -> {

                                                    });
                                                });

                                                node(memoryCell, () -> {
                                                    node(memoryBank, () -> {

                                                    });
                                                });
                                            });

                                            node(logicProcessor, () -> {
                                                node(hyperProcessor, () -> {

                                                });
                                            });
                                        });
                                    });
                                });
                            });

                            node(illuminator, () -> {
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

                            node(steamGenerator, () -> {
                                node(thermalGenerator, () -> {
                                    node(differentialGenerator, () -> {
                                        node(thoriumReactor, Seq.with(new Research(Liquids.cryofluid)), () -> {
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

                            node(tsunami, () -> {

                            });
                        });

                        node(lancer, () -> {
                            node(foreshadow, () -> {
                                node(meltdown, () -> {

                                });
                            });

                            node(shockMine, () -> {

                            });
                        });
                    });
                });
            });

            node(groundFactory, () -> {
                node(commandCenter, () -> {

                });

                node(dagger, () -> {
                    node(mace, () -> {
                        node(fortress, () -> {
                            node(scepter, () -> {
                                node(reign, () -> {

                                });
                            });
                        });
                    });

                    node(nova, () -> {
                        node(pulsar, () -> {
                            node(quasar, () -> {
                                node(vela, () -> {
                                    node(corvus, () -> {

                                    });
                                });
                            });
                        });
                    });

                    node(crawler, () -> {
                        node(atrax, () -> {
                            node(spiroct, () -> {
                                node(arkyid, () -> {
                                    node(toxopid, () -> {

                                    });
                                });
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
                                    node(quad, () -> {
                                        node(oct, () -> {

                                        });
                                    });
                                });
                            });
                        });
                    });

                    node(navalFactory, () -> {
                        node(risso, () -> {
                            node(minke, () -> {
                                node(bryde, () -> {
                                    node(sei, () -> {
                                        node(omura, () -> {

                                        });
                                    });
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
                                //TODO change positions?
                                node(impact0078, Seq.with(
                                    new SectorComplete(tarFields),
                                    new Research(Items.thorium),
                                    new Research(overdriveProjector)
                                ), () -> {
                                    node(desolateRift, Seq.with(
                                        new SectorComplete(impact0078),
                                        new Research(thermalGenerator),
                                        new Research(thoriumReactor)
                                    ), () -> {

                                    });
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
                        new Research(steamGenerator)
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

    public static void setup(){
        context = null;
        map = new ObjectMap<>();
        all = new Seq<>();
    }

    //all the "node" methods are hidden, because they are for internal context-dependent use only
    //for custom research, just use the TechNode constructor

    static TechNode node(UnlockableContent content, Runnable children){
        return node(content, content.researchRequirements(), children);
    }

    static TechNode node(UnlockableContent content, ItemStack[] requirements, Runnable children){
        return node(content, requirements, null, children);
    }

    static TechNode node(UnlockableContent content, ItemStack[] requirements, Seq<Objective> objectives, Runnable children){
        TechNode node = new TechNode(context, content, requirements);
        if(objectives != null){
            node.objectives = objectives;
        }

        TechNode prev = context;
        context = node;
        children.run();
        context = prev;

        return node;
    }

    static TechNode node(UnlockableContent content, Seq<Objective> objectives, Runnable children){
        return node(content, content.researchRequirements(), objectives, children);
    }

    static TechNode node(UnlockableContent block){
        return node(block, () -> {});
    }

    @Nullable
    public static TechNode get(UnlockableContent content){
        return map.get(content);
    }

    public static TechNode getNotNull(UnlockableContent content){
        return map.getThrow(content, () -> new RuntimeException(content + " does not have a tech node"));
    }

    public static class TechNode{
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
        /** Extra objectives needed to research this. */
        public Seq<Objective> objectives = new Seq<>();
        /** Nodes that depend on this node. */
        public final Seq<TechNode> children = new Seq<>();

        public TechNode(@Nullable TechNode parent, UnlockableContent content, ItemStack[] requirements){
            if(parent != null) parent.children.add(this);

            this.parent = parent;
            this.content = content;
            this.requirements = requirements;
            this.depth = parent == null ? 0 : parent.depth + 1;
            this.finishedRequirements = new ItemStack[requirements.length];

            //load up the requirements that have been finished if settings are available
            for(int i = 0; i < requirements.length; i++){
                finishedRequirements[i] = new ItemStack(requirements[i].item, Core.settings == null ? 0 : Core.settings.getInt("req-" + content.name + "-" + requirements[i].item.name));
            }

            //add dependencies as objectives.
            content.getDependencies(d -> objectives.add(new Research(d)));

            map.put(content, this);
            all.add(this);
        }

        /** Resets finished requirements and saves. */
        public void reset(){
            for(ItemStack stack : finishedRequirements){
                stack.amount = 0;
            }
            save();
        }

        /** Removes this node from the tech tree. */
        public void remove(){
            all.remove(this);
            if(parent != null){
                parent.children.remove(this);
            }
        }

        /** Flushes research progress to settings. */
        public void save(){

            //save finished requirements by item type
            for(ItemStack stack : finishedRequirements){
                Core.settings.put("req-" + content.name + "-" + stack.item.name, stack.amount);
            }
        }
    }
}
