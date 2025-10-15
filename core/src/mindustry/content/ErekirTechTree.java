package mindustry.content;

import arc.struct.*;
import arc.util.*;
import mindustry.entities.bullet.*;
import mindustry.game.Objectives.*;
import mindustry.type.*;
import mindustry.type.unit.*;
import mindustry.world.blocks.defense.turrets.*;

import static mindustry.Vars.*;
import static mindustry.content.Blocks.*;
import static mindustry.content.SectorPresets.*;
import static mindustry.content.TechTree.*;

public class ErekirTechTree{
    static IntSet balanced = new IntSet();

    static void rebalanceBullet(BulletType bullet){
        if(balanced.add(bullet.id)){
            bullet.damage *= 0.75f;
        }
    }

    //TODO remove this
    public static void rebalance(){
        for(var unit : content.units().select(u -> u instanceof ErekirUnitType)){
            for(var weapon : unit.weapons){
                rebalanceBullet(weapon.bullet);
            }
        }

        for(var block : content.blocks()){
            if(block instanceof Turret turret && Structs.contains(block.requirements, i -> !Items.serpuloItems.contains(i.item))){
                if(turret instanceof ItemTurret item){
                    for(var bullet : item.ammoTypes.values()){
                        rebalanceBullet(bullet);
                    }
                }else if(turret instanceof ContinuousLiquidTurret cont){
                    for(var bullet : cont.ammoTypes.values()){
                        rebalanceBullet(bullet);
                    }
                }else if(turret instanceof ContinuousTurret cont){
                    rebalanceBullet(cont.shootType);
                }
            }
        }
    }

    public static void load(){
        rebalance();

        //TODO might be unnecessary with no asteroids
        Seq<Objective> erekirSector = Seq.with(new OnPlanet(Planets.erekir));

        var costMultipliers = new ObjectFloatMap<Item>();
        for(var item : content.items()) costMultipliers.put(item, 0.9f);

        //these are hard to make
        costMultipliers.put(Items.oxide, 0.5f);
        costMultipliers.put(Items.surgeAlloy, 0.7f);
        costMultipliers.put(Items.carbide, 0.3f);
        costMultipliers.put(Items.phaseFabric, 0.2f);

        Planets.erekir.techTree = nodeRoot("erekir", coreBastion, true, () -> {
            context().researchCostMultipliers = costMultipliers;

            node(duct, erekirSector, () -> {
                node(ductRouter, () -> {
                    node(ductBridge, () -> {
                        node(armoredDuct, () -> {
                            node(surgeConveyor, () -> {
                                node(surgeRouter);
                            });
                        });

                        node(unitCargoLoader, () -> {
                            node(unitCargoUnloadPoint, () -> {

                            });
                        });
                    });

                    node(overflowDuct, Seq.with(new OnSector(aegis)), () -> {
                        node(underflowDuct);
                        node(reinforcedContainer, () -> {
                            node(ductUnloader, () -> {

                            });

                            node(reinforcedVault, () -> {

                            });
                        });
                    });

                    node(reinforcedMessage, Seq.with(new OnSector(aegis)), () -> {
                        node(canvas);
                    });
                });

                node(reinforcedPayloadConveyor, Seq.with(new OnSector(atlas)), () -> {
                    //TODO should only be unlocked in unit sector
                    node(payloadMassDriver, Seq.with(new Research(siliconArcFurnace), new OnSector(split)), () -> {
                        //TODO further limitations
                        node(payloadLoader, () -> {
                            node(payloadUnloader, () -> {
                                node(largePayloadMassDriver, () -> {

                                });
                            });
                        });

                        node(constructor, Seq.with(new OnSector(split)), () -> {
                            node(smallDeconstructor, Seq.with(new OnSector(peaks)), () -> {
                                node(largeConstructor, Seq.with(new OnSector(siege)), () -> {

                                });

                                node(deconstructor, Seq.with(new OnSector(siege)), () -> {

                                });
                            });
                        });
                    });

                    node(reinforcedPayloadRouter, () -> {

                    });
                });
            });

            //TODO move into turbine condenser?
            node(plasmaBore, () -> {
                node(impactDrill, Seq.with(new OnSector(aegis)), () -> {
                    node(largePlasmaBore, Seq.with(new OnSector(caldera)), () -> {
                        node(eruptionDrill, Seq.with(new OnSector(stronghold)), () -> {

                        });

                        node(largeCliffCrusher, Seq.with(new OnSector(stronghold)), () -> {

                        });
                    });
                });
            });

            node(turbineCondenser, () -> {
                node(beamNode, () -> {
                    node(ventCondenser, Seq.with(new OnSector(aegis)), () -> {
                        node(chemicalCombustionChamber, Seq.with(new OnSector(basin)), () -> {
                            node(pyrolysisGenerator, Seq.with(new OnSector(crevice)), () -> {
                                node(fluxReactor, Seq.with(new OnSector(crossroads), new Research(cyanogenSynthesizer)), () -> {
                                    node(neoplasiaReactor, Seq.with(new OnSector(karst)), () -> {

                                    });
                                });
                            });
                        });
                    });

                    node(beamTower, Seq.with(new OnSector(peaks)), () -> {
                        node(beamLink, Seq.with(new OnSector(crossroads)), () -> {

                        });
                    });


                    node(regenProjector, Seq.with(new OnSector(peaks)), () -> {
                        //TODO more tiers of build tower or "support" structures like overdrive projectors
                        node(buildTower, Seq.with(new OnSector(stronghold)), () -> {
                            node(shockwaveTower, Seq.with(new OnSector(siege)), () -> {

                            });
                        });
                    });
                });

                node(reinforcedConduit, Seq.with(new OnSector(aegis)), () -> {
                    //TODO maybe should be even later
                    node(reinforcedPump, Seq.with(new OnSector(basin)), () -> {
                        //TODO T2 pump, consume cyanogen or similar
                    });

                    node(reinforcedLiquidJunction, () -> {
                        node(reinforcedBridgeConduit, () -> {

                        });

                        node(reinforcedLiquidRouter, () -> {
                            node(reinforcedLiquidContainer, () -> {
                                node(reinforcedLiquidTank, Seq.with(new SectorComplete(intersect)), () -> {

                                });
                            });
                        });
                    });
                });

                node(cliffCrusher, () -> {
                    node(siliconArcFurnace, () -> {
                        node(electrolyzer, Seq.with(new OnSector(atlas)), () -> {
                            node(oxidationChamber, Seq.with(new Research(tankRefabricator), new OnSector(marsh)), () -> {

                                node(surgeCrucible, Seq.with(new OnSector(ravine)), () -> {

                                });
                                node(heatRedirector, Seq.with(new OnSector(ravine)), () -> {
                                    node(electricHeater, Seq.with(new OnSector(ravine), new Research(afflict)), () -> {
                                        node(slagHeater, Seq.with(new OnSector(caldera)), () -> {

                                        });

                                        node(atmosphericConcentrator, Seq.with(new OnSector(caldera)), () -> {
                                            node(cyanogenSynthesizer, Seq.with(new OnSector(siege)), () -> {

                                            });
                                        });

                                        node(carbideCrucible, Seq.with(new OnSector(crevice)), () -> {
                                            node(phaseSynthesizer, Seq.with(new OnSector(karst)), () -> {
                                                node(phaseHeater, Seq.with(new Research(phaseSynthesizer)), () -> {

                                                });
                                            });
                                        });

                                        node(heatRouter, () -> {
                                            node(smallHeatRedirector, () -> {

                                            });
                                        });
                                    });
                                });
                            });

                            node(slagIncinerator, Seq.with(new OnSector(basin)), () -> {

                                //TODO these are unused.
                                //node(slagCentrifuge, () -> {});
                                //node(heatReactor, () -> {});
                            });
                        });
                    });
                });
            });


            node(breach, Seq.with(new Research(siliconArcFurnace), new Research(tankFabricator)), () -> {
                node(berylliumWall, () -> {
                    node(berylliumWallLarge, () -> {

                    });

                    node(tungstenWall, () -> {
                        node(tungstenWallLarge, () -> {
                            node(blastDoor, () -> {

                            });
                        });

                        node(reinforcedSurgeWall, () -> {
                            node(reinforcedSurgeWallLarge, () -> {
                                node(shieldedWall, () -> {

                                });
                            });
                        });

                        node(carbideWall, () -> {
                            node(carbideWallLarge, () -> {

                            });
                        });
                    });
                });

                node(diffuse, Seq.with(new OnSector(lake)), () -> {
                    node(sublimate, Seq.with(new OnSector(marsh)), () -> {
                        node(afflict, Seq.with(new OnSector(ravine)), () -> {
                            node(titan, Seq.with(new OnSector(stronghold)), () -> {
                                node(lustre, Seq.with(new OnSector(crevice)), () -> {
                                    node(smite, Seq.with(new OnSector(karst)), () -> {

                                    });
                                });
                            });
                        });
                    });

                    node(disperse, Seq.with(new OnSector(stronghold)), () -> {
                        node(scathe, Seq.with(new OnSector(siege)), () -> {
                            node(malign, Seq.with(new SectorComplete(karst)), () -> {

                            });
                        });
                    });
                });


                node(radar, Seq.with(new Research(beamNode), new Research(turbineCondenser), new Research(tankFabricator), new OnSector(SectorPresets.aegis)), () -> {

                });
            });

            node(coreCitadel, Seq.with(new SectorComplete(peaks)), () -> {
                node(coreAcropolis, Seq.with(new SectorComplete(siege)), () -> {

                });
            });

            node(tankFabricator, Seq.with(new Research(siliconArcFurnace), new Research(plasmaBore), new Research(turbineCondenser)), () -> {
                node(UnitTypes.stell);

                node(unitRepairTower, Seq.with(new OnSector(ravine), new Research(mechRefabricator)), () -> {

                });

                node(shipFabricator, Seq.with(new OnSector(lake)), () -> {
                    node(UnitTypes.elude);

                    node(mechFabricator, Seq.with(new OnSector(intersect)), () -> {
                        node(UnitTypes.merui);

                        node(tankRefabricator, Seq.with(new OnSector(atlas)), () -> {
                            node(UnitTypes.locus);

                            node(mechRefabricator, Seq.with(new OnSector(basin)), () -> {
                                node(UnitTypes.cleroi);

                                node(shipRefabricator, Seq.with(new OnSector(peaks)), () -> {
                                    node(UnitTypes.avert);

                                    //TODO
                                    node(primeRefabricator, Seq.with(new OnSector(stronghold)), () -> {
                                        node(UnitTypes.precept);
                                        node(UnitTypes.anthicus);
                                        node(UnitTypes.obviate);
                                    });

                                    node(tankAssembler, Seq.with(new OnSector(siege), new Research(constructor), new Research(atmosphericConcentrator)), () -> {

                                        node(UnitTypes.vanquish, () -> {
                                            node(UnitTypes.conquer, Seq.with(new OnSector(karst)), () -> {

                                            });
                                        });

                                        node(shipAssembler, Seq.with(new OnSector(crossroads)), () -> {
                                            node(UnitTypes.quell, () -> {
                                                node(UnitTypes.disrupt, Seq.with(new OnSector(karst)), () -> {

                                                });
                                            });
                                        });

                                        node(mechAssembler, Seq.with(new OnSector(crossroads)), () -> {
                                            node(UnitTypes.tecta, () -> {
                                                node(UnitTypes.collaris, Seq.with(new OnSector(karst)), () -> {

                                                });
                                            });
                                        });

                                        node(basicAssemblerModule, Seq.with(new SectorComplete(karst)), () -> {

                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });

            node(onset, () -> {
                node(aegis, Seq.with(new SectorComplete(onset), new Research(ductRouter), new Research(ductBridge)), () -> {
                    node(lake, Seq.with(new SectorComplete(aegis)), () -> {

                    });

                    node(intersect, Seq.with(new SectorComplete(aegis), new SectorComplete(lake), new Research(ventCondenser), new Research(shipFabricator)), () -> {
                        node(atlas, Seq.with(new SectorComplete(intersect), new Research(mechFabricator)), () -> {
                            node(split, Seq.with(new SectorComplete(atlas), new Research(reinforcedPayloadConveyor), new Research(reinforcedContainer)), () -> {

                            });

                            node(basin, Seq.with(new SectorComplete(atlas)), () -> {
                                node(marsh, Seq.with(new SectorComplete(basin)), () -> {
                                    node(ravine, Seq.with(new SectorComplete(marsh), new Research(Liquids.slag)), () -> {
                                        node(caldera, Seq.with(new SectorComplete(peaks), new SectorComplete(ravine), new Research(heatRedirector)), () -> {
                                            node(stronghold, Seq.with(new SectorComplete(caldera), new Research(coreCitadel)), () -> {
                                                node(crevice, Seq.with(new SectorComplete(stronghold)), () -> {
                                                    node(siege, Seq.with(new SectorComplete(crevice)), () -> {
                                                        node(crossroads, Seq.with(new SectorComplete(siege)), () -> {
                                                            node(karst, Seq.with(new SectorComplete(crossroads), new Research(coreAcropolis)), () -> {
                                                                node(origin, Seq.with(new SectorComplete(karst), new Research(coreAcropolis), new Research(UnitTypes.vanquish), new Research(UnitTypes.disrupt), new Research(UnitTypes.collaris), new Research(malign), new Research(basicAssemblerModule), new Research(neoplasiaReactor)), () -> {

                                                                });
                                                            });
                                                        });
                                                    });
                                                });
                                            });
                                        });
                                    });

                                    node(peaks, Seq.with(new SectorComplete(marsh), new SectorComplete(split)), () -> {

                                    });
                                });
                            });
                        });
                    });
                });
            });

            nodeProduce(Items.beryllium, () -> {
                nodeProduce(Items.sand, () -> {
                    nodeProduce(Items.silicon, () -> {
                        nodeProduce(Items.oxide, () -> {
                            //nodeProduce(Items.fissileMatter, () -> {});
                        });
                    });
                });

                nodeProduce(Liquids.water, () -> {
                    nodeProduce(Liquids.ozone, () -> {
                        nodeProduce(Liquids.hydrogen, () -> {
                            nodeProduce(Liquids.nitrogen, () -> {

                            });

                            nodeProduce(Liquids.cyanogen, () -> {
                                nodeProduce(Liquids.neoplasm, () -> {

                                });
                            });
                        });
                    });
                });

                nodeProduce(Items.graphite, () -> {
                    nodeProduce(Items.tungsten, () -> {
                        nodeProduce(Liquids.slag, () -> {

                        });

                        nodeProduce(Liquids.arkycite, () -> {

                        });

                        nodeProduce(Items.thorium, () -> {
                            nodeProduce(Items.carbide, () -> {

                                //nodeProduce(Liquids.gallium, () -> {});
                            });
                        });

                        nodeProduce(Items.surgeAlloy, () -> {
                            nodeProduce(Items.phaseFabric, () -> {

                            });
                        });
                    });
                });
            });
        });
    }
}
