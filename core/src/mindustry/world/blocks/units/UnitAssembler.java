package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.ai.types.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.blocks.units.UnitAssemblerModule.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * Steps:
 * 0. place the assembler with the rectangle indicating build area
 * 1. wait for power/consValid
 * 2. print / create the 3-5 drones for free, make sure they're tethered - build speed depends on drone active fraction
 * 3.
 * */
public class UnitAssembler extends PayloadBlock{
    public @Load("@-side1") TextureRegion sideRegion1;
    public @Load("@-side2") TextureRegion sideRegion2;

    public int areaSize = 11;
    public UnitType droneType = UnitTypes.assemblyDrone;
    public int dronesCreated = 4;
    public float droneConstructTime = 60f * 3f;

    public Seq<AssemblerUnitPlan> plans = new Seq<>(4);

    public UnitAssembler(String name){
        super(name);
        update = solid = true;
        rotate = true;
        rotateDraw = false;
        acceptsPayload = true;
        flags = EnumSet.of(BlockFlag.unitAssembler);
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

        bars.add("progress", (UnitAssemblerBuild e) -> new Bar("bar.progress", Pal.ammo, () -> e.progress));

        bars.add("units", (UnitAssemblerBuild e) ->
            new Bar(() ->
            Core.bundle.format("bar.unitcap",
                Fonts.getUnicodeStr(e.unit().name),
                e.team.data().countType(e.unit()),
                Units.getStringCap(e.team)
            ),
            () -> Pal.power,
            () -> (float)e.team.data().countType(e.unit()) / Units.getCap(e.team)
        ));
    }

    @Override
    public void drawRequestRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(plan.rotation >= 2 ? sideRegion2 : sideRegion1, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, sideRegion1, topRegion};
    }

    @Override
    public void init(){
        clipSize = Math.max(clipSize, (areaSize + size) * tilesize * 2);
        consumes.add(new ConsumePayloadDynamic((UnitAssemblerBuild build) -> build.plan().requirements));

        super.init();
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.output, table -> {
            Seq<AssemblerUnitPlan> p = plans.select(u -> u.unit.unlockedNow());
            table.row();
            for(var plan : p){
                if(plan.unit.unlockedNow()){
                    table.image(plan.unit.uiIcon).size(8 * 3).padRight(2).right();
                    table.add(plan.unit.localizedName).left();
                    table.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds")).color(Color.lightGray).padLeft(12).left();
                    table.row();
                    table.add().right();
                    table.table(t -> {
                        t.left();
                        for(var stack : plan.requirements){
                            t.add(new ItemImage(stack)).padRight(4).padTop(4).left();
                        }
                    }).left().fillX().padLeft(-8 * 3f - 2).padBottom(6).row();
                }
            }
        });
    }

    public static class AssemblerUnitPlan{
        public UnitType unit;
        public Seq<BlockStack> requirements;
        public float time;

        public AssemblerUnitPlan(UnitType unit, float time, Seq<BlockStack> requirements){
            this.unit = unit;
            this.time = time;
            this.requirements = requirements;
        }

        AssemblerUnitPlan(){}
    }

    /** hbgwerjhbagjegwg */
    public static class YeetData{
        public Vec2 target;
        public Block block;

        public YeetData(Vec2 target, Block block){
            this.target = target;
            this.block = block;
        }
    }

    public class UnitAssemblerBuild extends PayloadBlockBuild<BuildPayload>{
        protected IntSeq readUnits = new IntSeq();

        public Seq<Unit> units = new Seq<>();
        public Seq<UnitAssemblerModuleBuild> modules = new Seq<>();
        public BlockSeq blocks = new BlockSeq();
        public float progress, warmup, droneWarmup;
        public float invalidWarmup = 0f;
        public int currentTier = 0;
        public boolean wasOccupied = false;

        public float droneProgress, totalDroneProgress;

        public Vec2 getUnitSpawn(){
            float len = tilesize * (areaSize + size)/2f;
            float unitX = x + Geometry.d4x(rotation) * len, unitY = y + Geometry.d4y(rotation) * len;
            return Tmp.v4.set(unitX, unitY);
        }

        public boolean moduleFits(Block other, float ox, float oy, int rotation){
            float
            dx = ox + Geometry.d4x(rotation) * (other.size/2 + 1) * tilesize,
            dy = oy + Geometry.d4y(rotation) * (other.size/2 + 1) * tilesize;

            Vec2 spawn = getUnitSpawn();

            if(Tile.relativeTo(ox, oy, spawn.x, spawn.y) != rotation){
                return false;
            }

            float dst = Math.max(Math.abs(dx - spawn.x), Math.abs(dy - spawn.y));
            return Mathf.equal(dst, tilesize * areaSize / 2f - tilesize/2f);
        }

        public void updateModules(UnitAssemblerModuleBuild build){
            modules.addUnique(build);
            checkTier();
        }

        public void removeModule(UnitAssemblerModuleBuild build){
            modules.remove(build);
            checkTier();
        }

        public void checkTier(){
            modules.sort(b -> b.tier());
            int max = 0;
            for(int i = 0; i < modules.size; i++){
                var mod = modules.get(i);
                if(mod.tier() == max || mod.tier() == max + 1){
                    max = mod.tier();
                }else{
                    //tier gap, TODO warning?
                    break;
                }
            }
            currentTier = max;
        }

        public UnitType unit(){
            return plan().unit;
        }

        public AssemblerUnitPlan plan(){
            //clamp plan pos
            return plans.get(Math.min(currentTier, plans.size - 1));
        }

        @Override
        public boolean shouldConsume(){
            //liquid is only consumed when building is being done
            return enabled && !wasOccupied && Units.canCreate(team, plan().unit) && consumes.get(ConsumeType.payload).valid(this);
        }

        @Override
        public void drawSelect(){
            for(var module : modules){
                Drawf.selected(module, Pal.accent);
            }

            //TODO draw area when no power
        }

        //is this necessary? wastes a lot of space
        /*
        @Override
        public void displayConsumption(Table table){
            super.displayConsumption(table);
            table.row();

            table.table(t -> {
                t.left();
                for(var mod : modules){
                    //TODO crosses for missing reqs?
                    t.image(mod.block.uiIcon).size(iconMed).padRight(4).padTop(4);
                }
            }).fillX().row();
        }*/

        @Override
        public void display(Table table){
            super.display(table);

            table.row();
            table.table(t -> {
                t.left().defaults().left();

                Block prev = null;
                for(int i = 0; i < modules.size; i++){
                    var mod = modules.get(i);
                    if(prev == mod.block) continue;
                    //TODO crosses for missing reqs?
                    t.image(mod.block.uiIcon).size(iconMed).padRight(4);

                    prev = mod.block;
                }

                t.label(() -> "[accent] -> []" + unit().emoji() + " " + unit().name);
            }).pad(4).padLeft(0f).fillX().left();
        }

        @Override
        public void updateTile(){
            if(!readUnits.isEmpty()){
                units.clear();
                readUnits.each(i -> {
                    var unit = Groups.unit.getByID(i);
                    if(unit != null){
                        units.add(unit);
                    }
                });
                readUnits.clear();
            }

            units.removeAll(u -> !u.isAdded() || u.dead || !(u.controller() instanceof AssemblerAI));

            droneWarmup = Mathf.lerpDelta(droneWarmup, units.size < dronesCreated ? efficiency() : 0f, 0.1f);
            totalDroneProgress += droneWarmup * Time.delta;

            if(units.size < dronesCreated && (droneProgress += edelta() / droneConstructTime) >= 1f){
                var unit = droneType.create(team);
                if(unit instanceof BuildingTetherc bt){
                    bt.building(this);
                }
                unit.set(x, y);
                unit.rotation = 90f;
                unit.add();

                Fx.spawn.at(unit);
                units.add(unit);
                droneProgress = 0f;
            }

            //TODO units should pick up and move payloads into position

            Vec2 spawn = getUnitSpawn();

            if(moveInPayload() && !wasOccupied){
                yeetPayload(payload);
                payload = null;
            }

            //arrange units around perimeter
            for(int i = 0; i < units.size; i++){
                var unit = units.get(i);
                var ai = (AssemblerAI)unit.controller();

                ai.targetPos.trns(i * 90f + 45f, areaSize / 2f * Mathf.sqrt2 * tilesize).add(spawn);
                ai.targetAngle = i * 90f + 45f + 180f;
            }

            wasOccupied = checkSolid(spawn);
            float eff =  (units.count(u -> ((AssemblerAI)u.controller()).inPosition()) / (float)dronesCreated);

            invalidWarmup = Mathf.lerpDelta(invalidWarmup, wasOccupied ? 1f : 0f, 0.1f);

            var plan = plan();

            //check if all requirements are met
            if(!wasOccupied && consValid() && Units.canCreate(team, plan.unit)){
                warmup = Mathf.lerpDelta(warmup, efficiency(), 0.1f);

                if((progress += edelta() * eff / plan.time) >= 1f){
                    //TODO ???? should this even be part of a trigger
                    consume();

                    //TODO actually just goes poof
                    var unit = plan.unit.create(team);
                    unit.set(spawn.x + Mathf.range(0.001f), spawn.y + Mathf.range(0.001f));
                    unit.rotation = 90f;
                    //TODO annoying so nothing is created yet
                    unit.add();
                    progress = 0f;

                    Fx.unitAssemble.at(unit.x, unit.y, 0f, unit);

                    blocks.clear();
                }
            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.1f);
            }

            //TODO drones need to indicate that they are in position and actually play an animation
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);

            //draw input conveyors
            for(int i = 0; i < 4; i++){
                if(blends(i) && i != rotation){
                    Draw.rect(inRegion, x, y, (i * 90) - 180);
                }
            }

            Draw.rect(rotation >= 2 ? sideRegion2 : sideRegion1, x, y, rotdeg());

            Draw.z(Layer.blockOver);

            payRotation = rotdeg();
            drawPayload();

            Draw.z(Layer.blockOver + 0.1f);

            Draw.rect(topRegion, x, y);

            //draw drone construction
            if(droneWarmup > 0){
                //TODO draw it - better animations?
                Draw.draw(Layer.blockOver + 0.2f, () -> {
                    Drawf.construct(this, droneType.fullIcon, Pal.accent, 0f, droneProgress, droneWarmup, totalDroneProgress, 14f);
                });
            }

            Vec2 spawn = getUnitSpawn();
            float sx = spawn.x, sy = spawn.y;

            var plan = plan();

            Draw.draw(Layer.blockBuilding, () -> {
                Draw.color(Pal.accent, warmup);

                Shaders.blockbuild.region = plan.unit.fullIcon;
                Shaders.blockbuild.time = Time.time;
                //margin due to units not taking up whole region
                Shaders.blockbuild.progress = Mathf.clamp(progress + 0.05f);

                Draw.rect(plan.unit.fullIcon, sx, sy);
                Draw.flush();
                Draw.color();
            });

            /*
            Tmp.tr1.set(plan.unit.fullIcon);
            Tmp.tr1.setY(Tmp.tr1.getY() + plan.unit.fullIcon.height * (1f - progress));

            //TODO what if building animation
            Draw.rect(Tmp.tr1, spawn.x, spawn.y + (Tmp.tr1.height/2f - plan.unit.fullIcon.height/2f) * Draw.scl, Tmp.tr1.width * Draw.scl, Tmp.tr1.height * Draw.scl);
            Lines.stroke(1f, Pal.accent);
            Draw.alpha(warmup);
            Lines.lineAngleCenter(spawn.x, spawn.y - plan.unit.fullIcon.height/2f * Draw.scl + plan.unit.fullIcon.height * progress * Draw.scl, 0f, plan.unit.fullIcon.width * 0.9f * Draw.scl);*/

            Draw.reset();

            Draw.z(Layer.buildBeam);

            //draw unit outline
            Draw.mixcol(Tmp.c1.set(Pal.accent).lerp(Pal.remove, invalidWarmup), 1f);
            Draw.rect(plan.unit.fullIcon, spawn.x, spawn.y);

            //build beams do not draw when invalid
            Draw.alpha(Math.min(1f - invalidWarmup, warmup));

            //draw build beams
            for(var unit : units){
                if(!((AssemblerAI)unit.controller()).inPosition()) continue;

                float
                    px = unit.x + Angles.trnsx(unit.rotation, unit.type.buildBeamOffset),
                    py = unit.y + Angles.trnsy(unit.rotation, unit.type.buildBeamOffset);

                Drawf.buildBeam(px, py, spawn.x, spawn.y, plan.unit.hitSize/2f);
            }

            Draw.reset();

            Draw.z(Layer.buildBeam);

            float fulls = areaSize * tilesize/2f;

            //draw full area
            Lines.stroke(2f, Pal.accent);
            Drawf.dashRectBasic(spawn.x - fulls, spawn.y - fulls, fulls*2f, fulls*2f);

            Draw.reset();

            float outSize = plan.unit.hitSize + 9f;

            if(invalidWarmup > 0){
                //draw small square for area
                Lines.stroke(2f, Tmp.c3.set(Pal.accent).lerp(Pal.remove, invalidWarmup).a(invalidWarmup));
                Drawf.dashSquareBasic(spawn.x, spawn.y, outSize);
            }

            Draw.reset();
        }

        public boolean checkSolid(Vec2 v){
            var output = unit();
            return !output.flying && (collisions.overlapsTile(Tmp.r1.setCentered(v.x, v.y, output.hitSize), EntityCollisions::solid) || Units.anyEntities(v.x, v.y, output.hitSize * 1.4f));
        }

        /** @return true if this block is ready to produce units, e.g. requirements met */
        public boolean ready(){
            return consValid() && !wasOccupied;
        }

        public void yeetPayload(BuildPayload payload){
            var spawn = getUnitSpawn();
            blocks.add(payload.block(), 1);
            float rot = payload.angleTo(spawn);
            Fx.shootPayloadDriver.at(payload.x(), payload.y(), rot);
            Fx.payloadDeposit.at(payload.x(), payload.y(), rot, new YeetData(spawn.cpy(), payload.block()));
        }

        @Override
        public BlockSeq getBlockPayloads(){
            return blocks;
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            var plan = plan();
            return (this.payload == null || source instanceof UnitAssemblerModuleBuild) &&
                    payload instanceof BuildPayload bp && plan.requirements.contains(b -> b.block == bp.block() && blocks.get(bp.block()) < b.amount);
        }

        @Override
        public void write(Writes write){
            super.write(write);

            write.f(progress);
            write.b(units.size);
            for(var unit : units){
                write.i(unit.id);
            }

            blocks.write(write);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            int count = read.b();
            readUnits.clear();
            for(int i = 0; i < count; i++){
                readUnits.add(read.i());
            }

            blocks.read(read);
        }
    }
}
