package mindustry.world.blocks.units;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.payloads.*;

import static mindustry.Vars.*;

/**
 * Steps:
 * 0. place the assembler with the rectangle indicating build area
 * 1. wait for power/consValid
 * 2. print / create the 3-5 drones for free, make sure they're tethered - build speed depends on drone active fraction
 * 3.
 * */
public class UnitAssembler extends PayloadBlock{
    public int areaSize = 10;
    public UnitType droneType;
    public int dronesCreated = 4;

    //TODO should be different for every tier.
    public Seq<BlockStack> requirements = new Seq<>();
    public UnitType output = UnitTypes.vanquish;

    public UnitAssembler(String name){
        super(name);
        update = solid = true;
        rotate = true;
        rotateDraw = false;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        x *= tilesize;
        y *= tilesize;

        Tmp.r1.setCentered(x, y, areaSize * tilesize);
        float len = tilesize * (areaSize + size)/2f;

        Tmp.r1.x += Geometry.d4x(rotation) * len;
        Tmp.r1.y += Geometry.d4y(rotation) * len;

        //TODO better visuals here?
        Drawf.dashRect(valid ? Pal.accent : Pal.remove, Tmp.r1);
    }

    public class UnitAssemblerBuild extends PayloadBlockBuild<BuildPayload>{
        public Seq<Unit> units = new Seq<>();

        //TODO how should payloads be stored? counts of blocks? intmap? references?
        //public Seq<BuildPayload> payloads = new Seq<>();

        @Override
        public void updateTile(){
            units.removeAll(u -> !u.isAdded() || u.dead);

            if(consValid() && units.size < dronesCreated){
                //TODO build animation? distribute spawning?
                var unit = droneType.create(team);
                if(unit instanceof BuildingTetherc bt){
                    bt.building(this);
                }
                unit.set(x, y);
                unit.rotation = 90f;
                unit.add();

                Fx.spawn.at(unit);
                units.add(unit);
            }

            //TODO drones need to indicate that they are in position and actually play an animation
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            //super.handlePayload(source, payload);

            //payloads.add((BuildPayload)payload);
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return payload instanceof BuildPayload bp && requirements.contains(b -> b.block == bp.block());
        }

        @Override
        public void write(Writes write){
            super.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
        }
    }
}
