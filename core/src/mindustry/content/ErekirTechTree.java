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
        costMultipliers.put(Items.silicon, 9);
        costMultipliers.put(Items.surgeAlloy, 4);
        costMultipliers.put(Items.phaseFabric, 4);
        costMultipliers.put(Items.thorium, 9);
        costMultipliers.put(Items.graphite, 9);
        //oxide is hard to make
        costMultipliers.put(Items.oxide, 0.5f);

        //TODO remove
        Objective tmpNever = new Research(Items.fissileMatter);

        //TODO gate behind capture

        Planets.erekir.techTree = nodeRoot("erekir", coreBastion, true, () -> {
            //context().researchCostMultipliers = costMultipliers;

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

                    node(overflowDuct, Seq.with(new OnSector(two)), () -> {
                        node(reinforcedContainer, () -> {
                            node(ductUnloader, () -> {

                            });

                            node(reinforcedVault, () -> {

                            });
                        });
                    });
                });

                node(reinforcedPayloadConveyor, Seq.with(new OnSector(four)), () -> {
                    //TODO should only be unlocked in unit sector
                    node(constructor, Seq.with(new Research(siliconArcFurnace), new OnSector(four)), () -> {
                        node(payloadMassDriver, Seq.with(new OnSector(four)), () -> {
                            //TODO further limitations
                            node(payloadLoader, () -> {
                                node(payloadUnloader, () -> {
                                    //TODO replace.
                                    //node(payloadPropulsionTower, () -> {

                                    //});
                                });
                            });

                            node(smallDeconstructor, () -> {
                                node(largeConstructor, () -> {

                                });

                                node(deconstructor, () -> {

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
                node(impactDrill, Seq.with(new OnSector(two)), () -> {
                    node(largePlasmaBore, Seq.with(new OnSector(four)), () -> {
                        node(eruptionDrill, () -> {

                        });
                    });
                });
            });

            node(turbineCondenser, () -> {
                node(beamNode, () -> {
                    node(ventCondenser, Seq.with(new OnSector(two)), () -> {
                        node(chemicalCombustionChamber, Seq.with(new OnSector(three)), () -> {
                            node(pyrolysisGenerator, () -> {

                            });
                        });
                    });

                    node(beamTower, Seq.with(new OnSector(four)), () -> {

                    });


                    node(regenProjector, () -> {
                        //TODO more tiers of build tower or "support" structures like overdrive projectors
                        node(buildTower, Seq.with(new OnSector(four)), () -> {

                        });
                    });
                });

                node(reinforcedConduit, Seq.with(new OnSector(two)), () -> {
                    //TODO maybe should be even later
                    node(reinforcedPump, Seq.with(new OnSector(three)), () -> {
                        //TODO T2 pump, consume cyanogen or similar
                    });

                    node(reinforcedLiquidJunction, () -> {
                        node(reinforcedBridgeConduit, () -> {

                        });

                        node(reinforcedLiquidRouter, () -> {
                            node(reinforcedLiquidContainer, () -> {
                                node(reinforcedLiquidTank, Seq.with(new SectorComplete(three)), () -> {

                                });
                            });
                        });
                    });
                });

                node(cliffCrusher, () -> {
                    node(siliconArcFurnace, () -> {
                        node(electrolyzer, Seq.with(new OnSector(three)), () -> {
                            node(oxidationChamber, Seq.with(new Research(tankRefabricator), new OnSector(four)), () -> {
                                node(electricHeater, Seq.with(new OnSector(four)), () -> {
                                    node(heatRedirector, () -> {
                                        node(surgeCrucible, () -> {

                                        });
                                    });

                                    node(slagHeater, () -> {

                                    });
                                    
                                    node(atmosphericConcentrator, Seq.with(new OnSector(four)), () -> {
                                        node(cyanogenSynthesizer, Seq.with(new OnSector(four)), () -> {

                                        });
                                    });

                                    node(carbideCrucible, Seq.with(tmpNever), () -> {
                                        node(phaseSynthesizer, () -> {
                                            node(phaseHeater, () -> {

                                            });
                                        });
                                    });
                                });
                            });

                            node(slagIncinerator, Seq.with(new OnSector(four)), () -> {

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

                            });
                        });

                        node(carbideWall, () -> {
                            node(carbideWallLarge, () -> {

                            });
                        });
                    });
                });

                node(diffuse, Seq.with(new OnSector(two)), () -> {
                    node(sublimate, () -> {
                        node(titan, Seq.with(new OnSector(four)), () -> {
                            node(afflict, Seq.with(new OnSector(four)), () -> {

                            });
                        });

                        node(disperse, Seq.with(new OnSector(four)), () -> {

                        });
                    });
                });


                node(radar, Seq.with(new Research(beamNode), new Research(turbineCondenser), new Research(tankFabricator), new OnSector(SectorPresets.two)), () -> {

                });
            });

            node(coreCitadel, Seq.with(new SectorComplete(four)), () -> {
                node(coreAcropolis, () -> {

                });
            });

            node(tankFabricator, Seq.with(new Research(siliconArcFurnace), new Research(plasmaBore), new Research(turbineCondenser)), () -> {
                node(UnitTypes.stell);

                node(unitRepairTower, Seq.with(new OnSector(two)), () -> {

                });

                node(shipFabricator, Seq.with(new OnSector(two)), () -> {
                    node(UnitTypes.elude);

                    node(mechFabricator, Seq.with(new OnSector(three)), () -> {
                        node(UnitTypes.merui);

                        node(tankRefabricator, Seq.with(new OnSector(three)), () -> {
                            node(UnitTypes.locus);

                            node(mechRefabricator, Seq.with(new OnSector(three)), () -> {
                                node(UnitTypes.cleroi);

                                node(shipRefabricator, Seq.with(new OnSector(four), tmpNever), () -> {
                                    node(UnitTypes.avert);

                                    //TODO
                                    node(primeRefabricator, () -> {
                                        node(UnitTypes.precept);
                                        node(UnitTypes.anthicus);
                                        node(UnitTypes.obviate);
                                    });

                                    node(tankAssembler, Seq.with(new OnSector(three), new Research(constructor), new Research(atmosphericConcentrator)), () -> {

                                        node(UnitTypes.vanquish, () -> {
                                            node(UnitTypes.conquer, Seq.with(tmpNever), () -> {

                                            });
                                        });

                                        node(shipAssembler, Seq.with(new OnSector(four)), () -> {
                                            node(UnitTypes.quell, () -> {
                                                node(UnitTypes.disrupt, Seq.with(tmpNever), () -> {

                                                });
                                            });

                                            node(mechAssembler, Seq.with(tmpNever), () -> {
                                                node(UnitTypes.tecta, () -> {
                                                    node(UnitTypes.collaris, Seq.with(tmpNever), () -> {

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

            //TODO more sectors
            node(onset, () -> {
                node(two, Seq.with(new SectorComplete(onset), new Research(ductRouter), new Research(ductBridge)), () -> {
                    node(three, Seq.with(new SectorComplete(two), new Research(ventCondenser), new Research(shipFabricator)), () -> {
                        node(four, Seq.with(new SectorComplete(three)), () -> {

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
                                nodeProduce(Items.surgeAlloy, () -> {
                                    nodeProduce(Items.phaseFabric, () -> {

                                    });
                                });

                                //nodeProduce(Liquids.gallium, () -> {});
                            });
                        });
                    });
                });
            });
        });
    }
}
