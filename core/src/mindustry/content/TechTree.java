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
                        node(launchPad, Seq.with(new SectorComplete(extractionOutpost)), () -> {
                            node(interplanetaryAccelerator, Seq.with(new SectorComplete(planetaryTerminal)), () -> {
                                
                            });
                        });

                        node(distributor);
                        node(sorter, () -> {
                            node(invertedSorter);
                            node(overflowGate, () -> {
                                node(underflowGate);
                            });
                        });
                        node(container, Seq.with(new SectorComplete(biomassFacility)), () -> {
                            node(unloader);
                            node(vault, Seq.with(new SectorComplete(stainedMountains)), () -> {

                            });
                        });

                        node(itemBridge, () -> {
                            node(titaniumConveyor, Seq.with(new SectorComplete(craters)), () -> {
                                node(phaseConveyor, () -> {
                                    node(massDriver, () -> {
                                        node(payloadPropulsionTower, () -> {

                                        });
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

                                node(pulseConduit, Seq.with(new SectorComplete(windsweptIslands)), () -> {
                                    node(phaseConduit, () -> {

                                    });

                                    node(platedConduit, () -> {

                                    });

                                    node(rotaryPump, () -> {
                                        node(thermalPump, () -> {

                                        });
                                    });
                                });
                            });
                        });
                    });
                });

                node(graphitePress, () -> {
                    node(pneumaticDrill, Seq.with(new SectorComplete(frozenForest)), () -> {
                        node(cultivator, Seq.with(new SectorComplete(biomassFacility)), () -> {

                        });

                        node(laserDrill, () -> {
                            node(blastDrill, Seq.with(new SectorComplete(nuclearComplex)), () -> {

                            });

                            node(waterExtractor, Seq.with(new SectorComplete(saltFlats)), () -> {
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
                                node(multiPress, () -> {
                                    node(siliconCrucible, () -> {

                                    });
                                });
                            });

                            node(plastaniumCompressor, Seq.with(new SectorComplete(windsweptIslands)), () -> {
                                node(phaseWeaver, Seq.with(new SectorComplete(tarFields)), () -> {

                                });
                            });
                        });

                        node(kiln, Seq.with(new SectorComplete(craters)), () -> {
                            node(pulverizer, () -> {
                                node(incinerator, () -> {
                                    node(melter, () -> {
                                        node(surgeSmelter, () -> {

                                        });

                                        node(separator, () -> {
                                            node(disassembler, () -> {

                                            });
                                        });

                                        node(cryofluidMixer, () -> {

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

                        node(illuminator, () -> {

                        });
                    });
                });


                node(combustionGenerator, Seq.with(new Research(Items.coal)), () -> {
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
                                node(forceProjector, Seq.with(new SectorComplete(impact0078)), () -> {
                                    node(overdriveProjector, Seq.with(new SectorComplete(impact0078)), () -> {
                                        node(overdriveDome, Seq.with(new SectorComplete(impact0078)), () -> {

                                        });
                                    });
                                });

                                node(repairPoint, () -> {
                                    node(repairTurret, () -> {

                                    });
                                });
                            });
                        });

                        node(steamGenerator, Seq.with(new SectorComplete(craters)), () -> {
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
                    node(hail, Seq.with(new SectorComplete(craters)), () -> {
                        node(salvo, () -> {
                            node(swarmer, () -> {
                                node(cyclone, () -> {
                                    node(spectre, Seq.with(new SectorComplete(nuclearComplex)), () -> {

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
                            node(meltdown, () -> {
                                node(foreshadow, () -> {

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

                    node(navalFactory, Seq.with(new SectorComplete(ruinousShores)), () -> {
                        node(risso, () -> {
                            node(minke, () -> {
                                node(bryde, () -> {
                                    node(sei, () -> {
                                        node(omura, () -> {

                                        });
                                    });
                                });
                            });

                            node(retusa, () -> {
                                node(oxynoe, () -> {
                                    node(cyerce, () -> {
                                        node(aegires, () -> {
                                            node(navanax, () -> {

                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                });

                node(additiveReconstructor, Seq.with(new SectorComplete(biomassFacility)), () -> {
                    node(multiplicativeReconstructor, () -> {
                        node(exponentialReconstructor, Seq.with(new SectorComplete(overgrowth)), () -> {
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
                            new Research(kiln),
                            new Research(mechanicalPump)
                        ), () -> {
                            node(windsweptIslands, Seq.with(
                                new SectorComplete(ruinousShores),
                                new Research(pneumaticDrill),
                                new Research(hail),
                                new Research(siliconSmelter),
                                new Research(steamGenerator)
                            ), () -> {
                                node(tarFields, Seq.with(
                                    new SectorComplete(windsweptIslands),
                                    new Research(coalCentrifuge),
                                    new Research(conduit),
                                    new Research(wave)
                                ), () -> {
                                    node(impact0078, Seq.with(
                                        new SectorComplete(tarFields),
                                        new Research(Items.thorium),
                                        new Research(lancer),
                                        new Research(salvo),
                                        new Research(coreFoundation)
                                    ), () -> {
                                        node(desolateRift, Seq.with(
                                            new SectorComplete(impact0078),
                                            new Research(thermalGenerator),
                                            new Research(thoriumReactor),
                                            new Research(coreNucleus)
                                        ), () -> {
                                            node(planetaryTerminal, Seq.with(
                                                new SectorComplete(desolateRift),
                                                new SectorComplete(nuclearComplex),
                                                new SectorComplete(overgrowth),
                                                new SectorComplete(extractionOutpost),
                                                new SectorComplete(saltFlats),
                                                new Research(risso),
                                                new Research(minke),
                                                new Research(bryde),
                                                new Research(spectre),
                                                new Research(launchPad),
                                                new Research(massDriver),
                                                new Research(impactReactor),
                                                new Research(additiveReconstructor),
                                                new Research(exponentialReconstructor)
                                            ), () -> {

                                            });
                                        });
                                    });
                                });

                                node(extractionOutpost, Seq.with(
                                    new SectorComplete(stainedMountains),
                                    new SectorComplete(windsweptIslands),
                                    new Research(groundFactory),
                                    new Research(nova),
                                    new Research(airFactory),
                                    new Research(mono)
                                ), () -> {

                                });

                                node(saltFlats, Seq.with(
                                    new SectorComplete(windsweptIslands),
                                    new Research(commandCenter),
                                    new Research(groundFactory),
                                    new Research(additiveReconstructor),
                                    new Research(airFactory),
                                    new Research(door)
                                ), () -> {

                                });
                            });
                        });

                        node(overgrowth, Seq.with(
                            new SectorComplete(craters),
                            new SectorComplete(fungalPass),
                            new Research(cultivator),
                            new Research(sporePress),
                            new Research(additiveReconstructor),
                            new Research(UnitTypes.mace),
                            new Research(UnitTypes.flare)
                        ), () -> {

                        });
                    });

                    node(biomassFacility, Seq.with(
                        new SectorComplete(frozenForest),
                        new Research(powerNode),
                        new Research(steamGenerator),
                        new Research(scatter),
                        new Research(graphitePress)
                    ), () -> {
                        node(stainedMountains, Seq.with(
                            new SectorComplete(biomassFacility),
                            new Research(pneumaticDrill),
                            new Research(siliconSmelter)
                        ), () -> {
                            node(fungalPass, Seq.with(
                                new SectorComplete(stainedMountains),
                                new Research(groundFactory),
                                new Research(door)
                            ), () -> {
                                node(nuclearComplex, Seq.with(
                                    new SectorComplete(fungalPass),
                                    new Research(thermalGenerator),
                                    new Research(laserDrill),
                                    new Research(Items.plastanium),
                                    new Research(swarmer)
                                ), () -> {

                                });
                            });
                        });
                    });
                });
            });

            nodeProduce(Items.copper, () -> {
                nodeProduce(Liquids.water, () -> {

                });

                nodeProduce(Items.lead, () -> {
                    nodeProduce(Items.titanium, () -> {
                        nodeProduce(Liquids.cryofluid, () -> {

                        });

                        nodeProduce(Items.thorium, () -> {
                            nodeProduce(Items.surgeAlloy, () -> {

                            });

                            nodeProduce(Items.phaseFabric, () -> {

                            });
                        });
                    });

                    nodeProduce(Items.metaglass, () -> {

                    });
                });

                nodeProduce(Items.sand, () -> {
                    nodeProduce(Items.scrap, () -> {
                        nodeProduce(Liquids.slag, () -> {

                        });
                    });

                    nodeProduce(Items.coal, () -> {
                        nodeProduce(Items.graphite, () -> {
                            nodeProduce(Items.silicon, () -> {

                            });
                        });

                        nodeProduce(Items.pyratite, () -> {
                            nodeProduce(Items.blastCompound, () -> {

                            });
                        });

                        nodeProduce(Items.sporePod, () -> {

                        });

                        nodeProduce(Liquids.oil, () -> {
                            nodeProduce(Items.plastanium, () -> {

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
            node.objectives.addAll(objectives);
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

    static TechNode nodeProduce(UnlockableContent content, Seq<Objective> objectives, Runnable children){
        return node(content, content.researchRequirements(), objectives.and(new Produce(content)), children);
    }

    static TechNode nodeProduce(UnlockableContent content, Runnable children){
        return nodeProduce(content, new Seq<>(), children);
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
        public ItemStack[] finishedRequirements;
        /** Extra objectives needed to research this. */
        public Seq<Objective> objectives = new Seq<>();
        /** Nodes that depend on this node. */
        public final Seq<TechNode> children = new Seq<>();

        public TechNode(@Nullable TechNode parent, UnlockableContent content, ItemStack[] requirements){
            if(parent != null) parent.children.add(this);

            this.parent = parent;
            this.content = content;
            this.depth = parent == null ? 0 : parent.depth + 1;
            setupRequirements(requirements);

            var used = new ObjectSet<Content>();

            //add dependencies as objectives.
            content.getDependencies(d -> {
                if(used.add(d)){
                    objectives.add(new Research(d));
                }
            });

            map.put(content, this);
            all.add(this);
        }

        public void setupRequirements(ItemStack[] requirements){
            this.requirements = requirements;
            this.finishedRequirements = new ItemStack[requirements.length];

            //load up the requirements that have been finished if settings are available
            for(int i = 0; i < requirements.length; i++){
                finishedRequirements[i] = new ItemStack(requirements[i].item, Core.settings == null ? 0 : Core.settings.getInt("req-" + content.name + "-" + requirements[i].item.name));
            }
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
