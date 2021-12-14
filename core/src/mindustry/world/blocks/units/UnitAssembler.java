package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.consumers.*;

import static mindustry.Vars.*;

/**
 * Steps:
 * 0. place the assembler with the rectangle indicating build area
 * 1. wait for power/consValid
 * 2. print / create the 3-5 drones for free, make sure they're tethered - build speed depends on drone active fraction
 * 3.
 * */
public class UnitAssembler extends PayloadBlock{
    public int areaSize = 11;
    public UnitType droneType;
    public int dronesCreated = 4;

    //TODO should be different for every tier.
    public Seq<BlockStack> requirements = new Seq<>();
    public UnitType output;

    public UnitAssembler(String name){
        super(name);
        update = solid = true;
        rotate = true;
        rotateDraw = false;
        acceptsPayload = true;
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

        //TODO better visuals here? dashLine looks bad
        Drawf.dashRect(valid ? Pal.accent : Pal.remove, Tmp.r1);
    }

    @Override
    public void setBars(){
        super.setBars();

        //TODO progress bar
        //bars.add("progress", (UnitAssemblerBuild e) -> new Bar("bar.progress", Pal.ammo, e::fraction));

        bars.add("units", (UnitAssemblerBuild e) ->
            new Bar(() ->
            Core.bundle.format("bar.unitcap",
                Fonts.getUnicodeStr(output.name),
                e.team.data().countType(output),
                Units.getStringCap(e.team)
            ),
            () -> Pal.power,
            () -> (float)e.team.data().countType(output) / Units.getCap(e.team)
        ));
    }

    @Override
    public void drawRequestRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        //Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    @Override
    public void init(){
        consumes.add(new ConsumePayloads(requirements));

        super.init();
    }

    public class UnitAssemblerBuild extends PayloadBlockBuild<BuildPayload>{
        public Seq<Unit> units = new Seq<>();
        public BlockSeq blocks = new BlockSeq();

        public boolean wasOccupied = false;

        //TODO progress
        //TODO how should payloads be stored exactly? counts of blocks? intmap? references?

        public Vec2 getUnitSpawn(){
            float len = tilesize * (areaSize + size)/2f;
            float unitX = x + Geometry.d4x(rotation) * len, unitY = y + Geometry.d4y(rotation) * len;
            return Tmp.v4.set(unitX, unitY);
        }

        @Override
        public void updateTile(){
            units.removeAll(u -> !u.isAdded() || u.dead);

            //units annoying, disabled for now
            if(false && efficiency() > 0 && units.size < dronesCreated){
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

            //TODO units should move stuff into position

            //TODO check if area is occupied

            Vec2 spawn = getUnitSpawn();

            wasOccupied = checkSolid(spawn);

            //check if all requirements are met
            if(!wasOccupied && consValid() & Units.canCreate(team, output)){

                //TODO ???? should this even be part of a trigger
                consume();

                //TODO actually just goes poof
                var unit = output.create(team);
                unit.set(spawn.x + Mathf.range(0.001f), spawn.y + Mathf.range(0.001f));
                unit.rotation = 90f;
                unit.add();

                Fx.spawn.at(unit);

                blocks.clear();
            }

            //TODO drones need to indicate that they are in position and actually play an animation
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            //Draw.rect(outRegion, x, y, rotdeg());

            Draw.z(Layer.blockOver);

            //payRotation = rotdeg();
            //drawPayload();

            Draw.z(Layer.blockOver + 0.1f);

            Draw.rect(topRegion, x, y);

            Vec2 spawn = getUnitSpawn();

            //TODO which layer?
            Draw.z(Layer.power - 1f);

            Draw.mixcol(Pal.accent, 1f);
            Draw.alpha(0.4f + Mathf.absin(10f, 0.2f));
            Draw.rect(output.fullIcon, spawn.x, spawn.y);

            //TODO dash rect for output, fades in/out
            Draw.color(!wasOccupied ? Color.green : Color.red);
            Lines.square(spawn.x, spawn.y, output.hitSize/2f);

            Draw.reset();
        }

        public boolean checkSolid(Vec2 v){
            return !output.flying && (collisions.overlapsTile(Tmp.r1.setCentered(v.x, v.y, output.hitSize), EntityCollisions::solid) || Units.anyEntities(v.x, v.y, output.hitSize));
        }

        @Override
        public BlockSeq getBlockPayloads(){
            return blocks;
        }

        @Override
        public void handlePayload(Building source, Payload payload){
            //super.handlePayload(source, payload);

            blocks.add(((BuildPayload)payload).block());

            //payloads.add((BuildPayload)payload);
        }

        //TODO doesn't accept from payload source
        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return payload instanceof BuildPayload bp && requirements.contains(b -> b.block == bp.block() && blocks.get(bp.block()) < b.amount);
        }

        @Override
        public void write(Writes write){
            super.write(write);

            //blocks.write(write);

            //TODO save:
            //- unit IDs
            //- progress
            //- payloads in position (should they have positions?)
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);

            //blocks.read(read);
        }
    }
}
