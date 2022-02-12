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
import mindustry.game.*;
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
        regionRotated1 = 1;
        sync = true;
    }

    public Rect getRect(Rect rect, float x, float y, int rotation){
        rect.setCentered(x, y, areaSize * tilesize);
        float len = tilesize * (areaSize + size)/2f;

        rect.x += Geometry.d4x(rotation) * len;
        rect.y += Geometry.d4y(rotation) * len;

        return rect;
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        x *= tilesize;
        y *= tilesize;
        x += offset;
        y += offset;

        Rect rect = getRect(Tmp.r1, x, y, rotation);

        Drawf.dashRect(valid ? Pal.accent : Pal.remove, rect);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        //overlapping construction areas not allowed; grow by a tiny amount so edges can't overlap either.
        Rect rect = getRect(Tmp.r1, tile.worldx() + offset, tile.worldy() + offset, rotation).grow(0.1f);
        return !indexer.getFlagged(team, BlockFlag.unitAssembler).contains(b -> getRect(Tmp.r2, b.x, b.y, b.rotation).overlaps(rect));
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

    @Remote(called = Loc.server)
    public static void assemblerUnitSpawned(Tile tile){
        if(tile == null || !(tile.build instanceof UnitAssemblerBuild build)) return;
        build.spawned();
    }

    @Remote(called = Loc.server)
    public static void assemblerDroneSpawned(Tile tile, int id){
        if(tile == null || !(tile.build instanceof UnitAssemblerBuild build)) return;
        build.droneSpawned(id);
    }

    public class UnitAssemblerBuild extends PayloadBlockBuild<BuildPayload>{
        protected IntSeq readUnits = new IntSeq();
        //holds drone IDs that have been sent, but not synced yet - add to list as soon as possible
        protected IntSeq whenSyncedUnits = new IntSeq();

        public Seq<Unit> units = new Seq<>();
        public Seq<UnitAssemblerModuleBuild> modules = new Seq<>();
        public BlockSeq blocks = new BlockSeq();
        public float progress, warmup, droneWarmup, powerWarmup, sameTypeWarmup;
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
        }

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

            //read newly synced drones on client end
            if(units.size < dronesCreated && whenSyncedUnits.size > 0){
                whenSyncedUnits.each(id -> {
                    var unit = Groups.unit.getByID(id);
                    if(unit != null){
                        units.addUnique(unit);
                    }
                });
            }

            units.removeAll(u -> !u.isAdded() || u.dead || !(u.controller() instanceof AssemblerAI));

            powerWarmup = Mathf.lerpDelta(powerWarmup, efficiency() > 0.0001f ? 1f : 0f, 0.1f);
            droneWarmup = Mathf.lerpDelta(droneWarmup, units.size < dronesCreated ? efficiency() : 0f, 0.1f);
            totalDroneProgress += droneWarmup * Time.delta;

            if(units.size < dronesCreated && (droneProgress += edelta() / droneConstructTime) >= 1f){
                if(!net.client()){
                    var unit = droneType.create(team);
                    if(unit instanceof BuildingTetherc bt){
                        bt.building(this);
                    }
                    unit.set(x, y);
                    unit.rotation = 90f;
                    unit.add();
                    units.add(unit);
                    Call.assemblerDroneSpawned(tile, unit.id);
                }
            }

            if(units.size >= dronesCreated){
                droneProgress = 0f;
            }

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

            wasOccupied = checkSolid(spawn, false);
            boolean visualOccupied = checkSolid(spawn, true);
            float eff = (units.count(u -> ((AssemblerAI)u.controller()).inPosition()) / (float)dronesCreated);

            sameTypeWarmup = Mathf.lerpDelta(sameTypeWarmup, wasOccupied && !visualOccupied ? 0f : 1f, 0.1f);
            invalidWarmup = Mathf.lerpDelta(invalidWarmup, visualOccupied ? 1f : 0f, 0.1f);

            var plan = plan();

            //check if all requirements are met
            if(!wasOccupied && consValid() && Units.canCreate(team, plan.unit)){
                warmup = Mathf.lerpDelta(warmup, efficiency(), 0.1f);

                if((progress += edelta() * eff / plan.time) >= 1f){
                    Call.assemblerUnitSpawned(tile);
                }
            }else{
                warmup = Mathf.lerpDelta(warmup, 0f, 0.1f);
            }
        }

        public void droneSpawned(int id){
            Fx.spawn.at(x, y);
            droneProgress = 0f;
            if(net.client()){
                whenSyncedUnits.add(id);
            }
        }

        public void spawned(){
            var plan = plan();
            Vec2 spawn = getUnitSpawn();
            consume();

            if(!net.client()){
                var unit = plan.unit.create(team);
                unit.set(spawn.x + Mathf.range(0.001f), spawn.y + Mathf.range(0.001f));
                unit.rotation = 90f;
                unit.add();
            }

            progress = 0f;
            Fx.unitAssemble.at(spawn.x, spawn.y, 0f, plan.unit);
            blocks.clear();
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
            if(droneWarmup > 0.001f){
                Draw.draw(Layer.blockOver + 0.2f, () -> {
                    Drawf.construct(this, droneType.fullIcon, Pal.accent, 0f, droneProgress, droneWarmup, totalDroneProgress, 14f);
                });
            }

            Vec2 spawn = getUnitSpawn();
            float sx = spawn.x, sy = spawn.y;

            var plan = plan();

            //draw the unit construction as outline
            //TODO flashes when no gallium
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

            Draw.reset();

            Draw.z(Layer.buildBeam);

            //draw unit silhouette
            Draw.mixcol(Tmp.c1.set(Pal.accent).lerp(Pal.remove, invalidWarmup), 1f);
            Draw.alpha(Math.min(powerWarmup, sameTypeWarmup));
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

            //fill square in middle
            Fill.square(spawn.x, spawn.y, plan.unit.hitSize/2f);

            Draw.reset();

            Draw.z(Layer.buildBeam);

            float fulls = areaSize * tilesize/2f;

            //draw full area
            Lines.stroke(2f, Pal.accent);
            Draw.alpha(powerWarmup);
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

        public boolean checkSolid(Vec2 v, boolean same){
            var output = unit();
            float hsize = output.hitSize * 1.4f;
            return ((!output.flying && collisions.overlapsTile(Tmp.r1.setCentered(v.x, v.y, output.hitSize), EntityCollisions::solid)) ||
                Units.anyEntities(v.x - hsize/2f, v.y - hsize/2f, hsize, hsize, u -> (!same || u.type != output) &&
                    ((u.type.allowLegStep && output.allowLegStep) || (output.flying && u.isFlying()) || (!output.flying && u.isGrounded()))));
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
            whenSyncedUnits.clear();

            blocks.read(read);
        }
    }
}
