package mindustry.content;

import arc.struct.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;

import static mindustry.content.Blocks.*;
import static mindustry.content.SectorPresets.craters;
import static mindustry.content.SectorPresets.*;
import static mindustry.content.TechTree.*;
import static mindustry.content.UnitTypes.*;

public class SerpuloTechTree{

    public static void load(){
        Planets.serpulo.techTree = nodeRoot("serpulo", coreShard, () -> {

            node(conveyor, () -> {

                node(junction, () -> {
                    node(router, () -> {
                        node(advancedLaunchPad, Seq.with(new SectorComplete(extractionOutpost)), () -> {
                            node(landingPad, () -> {
                                node(interplanetaryAccelerator, Seq.with(new SectorComplete(planetaryTerminal)), () -> {

                                });
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
                                node(liquidContainer, () -> {
                                    node(liquidTank);
                                });

                                node(bridgeConduit);

                                node(pulseConduit, Seq.with(new SectorComplete(windsweptIslands)), () -> {
                                    node(phaseConduit, () -> {

                                    });

                                    node(platedConduit, () -> {

                                    });

                                    node(rotaryPump, () -> {
                                        node(impulsePump, () -> {

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
                        node(blastMixer, Seq.with(new SectorComplete(facility32m)), () -> {

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

                            node(plastaniumCompressor, Seq.with(new SectorComplete(windsweptIslands), new OnSector(tarFields)), () -> {
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

                        //logic disabled until further notice
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
                        node(scrapWall, () -> {
                            node(scrapWallLarge, () -> {
                                node(scrapWallHuge, () -> {
                                    node(scrapWallGigantic);
                                });
                            });
                        });

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

                    //override research requirements to have graphite, not coal
                    node(crawler, ItemStack.with(Items.silicon, 400, Items.graphite, 400), () -> {
                        node(atrax, () -> {
                            node(spiroct, () -> {
                                node(arkyid, () -> {
                                    node(toxopid, Seq.with(new SectorComplete(mycelialBastion)), () -> {

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

                    node(navalFactory, Seq.with(new OnSector(windsweptIslands)), () -> {
                        node(risso, () -> {
                            node(minke, () -> {
                                node(bryde, () -> {
                                    node(sei, () -> {
                                        node(omura, () -> {

                                        });
                                    });
                                });
                            });

                            node(retusa, Seq.with(new SectorComplete(windsweptIslands)), () -> {
                                node(oxynoe, Seq.with(new SectorComplete(coastline)), () -> {
                                    node(cyerce, () -> {
                                        node(aegires, () -> {
                                            node(navanax, Seq.with(new SectorComplete(navalFortress)), () -> {

                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                });

                node(additiveReconstructor, Seq.with(new SectorComplete(biomassFacility)), () -> {
                    node(multiplicativeReconstructor, Seq.with(new SectorComplete(overgrowth)), () -> {
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
                        node(frontier, Seq.with(
                        new Research(groundFactory),
                        new Research(airFactory),
                        new Research(thermalGenerator),
                        new Research(dagger),
                        new Research(mono)
                        ), () -> {

                        });

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
                                node(seaPort, Seq.with(
                                new SectorComplete(biomassFacility),
                                new Research(navalFactory),
                                new Research(risso),
                                new Research(retusa),
                                new Research(steamGenerator),
                                new Research(cultivator),
                                new Research(coalCentrifuge)
                                ), () -> {

                                });

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
                                            new Research(sei),
                                            new Research(omura),
                                            new Research(spectre),
                                            new Research(advancedLaunchPad),
                                            new Research(massDriver),
                                            new Research(impactReactor),
                                            new Research(additiveReconstructor),
                                            new Research(exponentialReconstructor),
                                            new Research(tetrativeReconstructor)
                                            ), () -> {
                                                node(geothermalStronghold, Seq.with(
                                                new Research(omura),
                                                new Research(navanax),
                                                new Research(eclipse),
                                                new Research(oct),
                                                new Research(reign),
                                                new Research(corvus),
                                                new Research(toxopid)
                                                ), () -> {

                                                });

                                                node(cruxscape, Seq.with(
                                                new Research(omura),
                                                new Research(navanax),
                                                new Research(eclipse),
                                                new Research(oct),
                                                new Research(reign),
                                                new Research(corvus),
                                                new Research(toxopid)
                                                ), () -> {

                                                });
                                            });
                                        });
                                    });
                                });

                                node(facility32m, Seq.with(
                                new Research(pneumaticDrill),
                                new SectorComplete(stainedMountains)
                                ), () -> {
                                    node(extractionOutpost, Seq.with(
                                    new SectorComplete(windsweptIslands),
                                    new SectorComplete(facility32m),
                                    new Research(groundFactory),
                                    new Research(nova),
                                    new Research(airFactory),
                                    new Research(mono)
                                    ), () -> {
                                        //TODO: removed for now
                                        /*node(polarAerodrome, Seq.with(
                                        new SectorComplete(fungalPass),
                                        new SectorComplete(desolateRift),
                                        new SectorComplete(overgrowth),
                                        new Research(multiplicativeReconstructor),
                                        new Research(zenith),
                                        new Research(swarmer),
                                        new Research(cyclone),
                                        new Research(blastDrill),
                                        new Research(blastDrill),
                                        new Research(massDriver)
                                        ), () -> {

                                        });
                                        */
                                    });
                                });

                                node(saltFlats, Seq.with(
                                new SectorComplete(windsweptIslands),
                                new Research(groundFactory),
                                new Research(additiveReconstructor),
                                new Research(airFactory),
                                new Research(door)
                                ), () -> {
                                    node(testingGrounds, Seq.with(
                                    new Research(cryofluidMixer),
                                    new Research(Liquids.cryofluid),
                                    new Research(waterExtractor),
                                    new Research(ripple)
                                    ), () -> {

                                    });

                                    node(coastline, Seq.with(
                                    new SectorComplete(windsweptIslands),
                                    new SectorComplete(saltFlats),
                                    new Research(navalFactory),
                                    new Research(payloadConveyor)
                                    ), () -> {

                                        node(navalFortress, Seq.with(
                                        new SectorComplete(coastline),
                                        new SectorComplete(extractionOutpost),
                                        new Research(coreNucleus),
                                        new Research(massDriver),
                                        new Research(oxynoe),
                                        new Research(minke),
                                        new Research(bryde),
                                        new Research(cyclone),
                                        new Research(ripple)
                                        ), () -> {
                                            node(weatheredChannels, Seq.with(
                                            new SectorComplete(impact0078),
                                            new Research(bryde),
                                            new Research(surgeSmelter),
                                            new Research(overdriveProjector)
                                            ), () -> {

                                            });
                                        });
                                    });
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
                            node(mycelialBastion, Seq.with(
                            new Research(atrax),
                            new Research(spiroct),
                            new Research(multiplicativeReconstructor),
                            new Research(exponentialReconstructor)
                            ), () -> {

                            });

                            node(atolls, Seq.with(
                            new SectorComplete(windsweptIslands),
                            new Research(multiplicativeReconstructor),
                            new Research(mega)
                            ), () -> {

                            });
                        });
                    });

                    node(biomassFacility, Seq.with(
                    new SectorComplete(frozenForest),
                    new Research(powerNode),
                    new Research(steamGenerator),
                    new Research(scatter),
                    new Research(graphitePress)
                    ), () -> {
                        node(taintedWoods, Seq.with(
                        new SectorComplete(biomassFacility),
                        new Research(Items.sporePod),
                        new Research(wave)
                        ), () -> {

                        });

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
                                node(infestedCanyons, Seq.with(
                                new SectorComplete(fungalPass),
                                new Research(navalFactory),
                                new Research(risso),
                                new Research(minke),
                                new Research(additiveReconstructor)
                                ), () -> {

                                });

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
}
