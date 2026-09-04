package mindustry.world.blocks.defense;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.graphics.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class TargetDummy extends Block{
    public final int dpsUpdateTime = timers++;
    public UnitType unitType = UnitTypes.dummy;
    public float pullScale = 0.33f;
    public String emptyStr = "---";
    public @Load(value = "@-tether-end", fallback = "@") TextureRegion tetherEnd;
    public @Load(value = "@-tether", fallback = "@") TextureRegion tether;
    public @Load(value = "@-preview", fallback = "@") TextureRegion previewRegion;

    public TargetDummy(String name){
        super(name);

        update = alwaysUpdateInUnits = true;
        configurable = logicConfigurable = true;
        underBullets = true;
        targetable = false;

        saveConfig = true;

        config(FloatSeq.class, (TargetDummyBuild tile, FloatSeq config) -> {
            if(config.size > 0){
                tile.unitTeam = tile.team;
                if(config.get(0) == 1) tile.unitTeam = Team.get(tile.dummyTeam());
            }
            if(config.size > 1) tile.boosting = config.get(1) == 1;
            if(config.size > 2) tile.unitArmor = config.get(2);
            if(config.size > 3) tile.resetTime = config.get(3);
            if(config.size > 4) tile.dummySize = config.get(4);
        });
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{previewRegion};
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("health");

        addBar("dps", (TargetDummyBuild entity) -> new Bar(
            () -> entity.displayDPS(false),
            () -> Pal.ammo,
            () -> 1f - (entity.reset / entity.resetTime)
        ));
    }

    @Override
    public void drawDefaultPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        super.drawDefaultPlanRegion(plan, list);

        Draw.rect(unitType.fullIcon, plan.drawx(), plan.drawy());
    }

    public class TargetDummyBuild extends Building implements UnitTetherBlock{
        //needs to be "unboxed" after reading, since units are read after buildings.
        public int readUnitId = -1;
        public Unit unit;
        public float resetTime = 120f;
        public float total, reset = resetTime, time, dummySize = 12f;
        public float dps, totalDisplay, timeDisplay;
        public int hits;
        public int hitsDisplay;
        public boolean boosting;
        public float unitArmor;
        public Team unitTeam = Team.derelict;

        @Override
        public void created(){
            if(unitTeam == Team.derelict) unitTeam = team;
        }

        @Override
        public void updateTile(){
            //unit was lost/destroyed somehow
            if(unit != null && (unit.dead || !unit.isAdded())){
                unit = null;
            }

            if(readUnitId != -1){
                unit = Groups.unit.getByID(readUnitId);
                if(unit != null || !net.client()){
                    readUnitId = -1;
                }
            }

            if(unitTeam == Team.derelict) unitTeam = team;

            if(unit == null){
                if(!net.client()){
                    unit = unitType.create(team);
                    if(unit instanceof TargetDummyUnit td){
                        td.building(this);
                    }
                    unit.set(x, y);
                    unit.rotation = 90f;
                    unit.add();
                    Call.unitTetherBlockSpawned(tile, unit.id);
                }
            }

            if(unit != null){
                unit.updateBoosting(boosting);
                unit.armor(unitArmor);
                unit.team(unitTeam);
                unit.hitSize = dummySize;

                //similar to impulseNet but does not factor in mass
                Tmp.v1.set(this).sub(unit).limit(dst(unit) * pullScale);
                unit.vel.add(Tmp.v1);

                //manually move units to simulate velocity for remote players
                if(unit.isRemote()) unit.move(Tmp.v1);

                if(unit.moving()) unit.lookAt(unit.vel().angle());
            }

            time += Time.delta;
            reset += Time.delta;

            if(timer(dpsUpdateTime, 20)){
                dps = total / time * 60f;
                totalDisplay = total;
                timeDisplay = time / 60f;
                hitsDisplay = hits;
            }

            if(reset >= resetTime){
                total = time = dps = hits = 0;
            }
        }

        public void spawned(int id){
            Fx.spawn.at(x, y);
            if(net.client()){
                readUnitId = id;
            }
        }

        @Override
        public void draw(){
            super.draw();

            if(tether.found() && unit != null){
                float z = unit.elevation > 0.5f ? unit.type.flyingLayer - 0.01f : unitType.groundLayer + Mathf.clamp(unitType.hitSize / 4000f, 0, 0.01f);
                Draw.z(z - 0.01f);
                Draw.color(team.color);
                Drawf.laser(tether, tetherEnd, x, y, unit.x, unit.y);
                Draw.color();
            }

            Draw.z(Layer.overlayUI);
            String text = displayDPS(true) + "\n" + "(" + displayTotal() + ")" + "\n" + displayHits();
            Drawf.text(text, x, y + size / (2.5f*2f), team.color, size / 2.5f);
        }

        @Override
        public void drawSelect(){
            Drawf.square(x, y, dummySize, 0f, team.color);
        }

        public String displayDPS(boolean round){
            if(time > 0){
                return Core.bundle.format("bar.dps", (round ? (dps > 0 ? Mathf.round(dps) : emptyStr) : Strings.autoFixed(total / time * 60f, 2)));
            }else{
                return Core.bundle.format("bar.dps", emptyStr);
            }
        }

        public String displayTotal(){
            if(time > 0){
                return Core.bundle.format("bar.damagetimetotal", Mathf.round(totalDisplay), Strings.autoFixed(timeDisplay, 2));
            }else{
                return Core.bundle.format("bar.damagetimetotal", emptyStr, emptyStr);
            }
        }

        public String displayHits(){
            if(time > 0){
                return Core.bundle.format("bar.hits", hitsDisplay);
            }else{
                return Core.bundle.format("bar.hits", emptyStr);
            }
        }

        @Override
        public boolean collide(Bullet other){ //Hit the unit, not the building
            return false;
        }

        @Override
        public boolean collision(Bullet other){ //Hit the unit, not the building
            return false;
        }

        public void dummyHit(float damage){
            reset = 0f;
            total += damage;
            hits++;
        }

        @Override
        public void damage(float damage){
            //just in case
        }

        @Override
        public void buildConfiguration(Table table){
            table.table(t -> {
                t.background(Styles.black6);
                t.defaults().left();

                t.check(Core.bundle.get("rules.enemyteam"), unitTeam != team, b -> configureFloat(0, Mathf.num(b))).colspan(3).row();
                t.check(Core.bundle.get("stat.flying"), boosting, b -> configureFloat(1, Mathf.num(b))).colspan(3).row();
                t.add(Core.bundle.get("stat.armor"));
                t.field("" + (int)unitArmor, TextFieldFilter.digitsOnly, s -> configureFloat(2, Strings.parseInt(s))).width(200f).padLeft(8f).colspan(2).row();
                t.add(Core.bundle.get("stat.resettime"));
                t.field(Strings.autoFixed(resetTime / 60f, 2), TextFieldFilter.floatsOnly, s -> configureFloat(3, Strings.parseFloat(s) * 60f)).padLeft(8f).growX();
                t.add(StatUnit.seconds.localized()).padLeft(8).row();
                t.add(Core.bundle.get("stat.hitsize"));
                t.field("" + (dummySize/tilesize), TextFieldFilter.floatsOnly, s -> configureFloat(4, Strings.parseFloat(s) * tilesize)).padLeft(8f).growX();
                t.add(StatUnit.blocks.localized()).padLeft(8f);
            }).top().grow().margin(8f);
        }

        //helper to send the rest of values, since config does not support arrays
        public void configureFloat(int index, float value){
            FloatSeq seq = FloatSeq.with(Mathf.num(unitTeam != team), Mathf.num(boosting), unitArmor, resetTime, dummySize);
            seq.set(index, value);
            configure(seq);
        }

        public int dummyTeam(){
            if(unitTeam != team) return team.id; //Return to own team
            return team != state.rules.waveTeam ? state.rules.waveTeam.id : (team.id % (Team.all.length - 1)) + 1;
        }

        @Override
        public Object config(){
            return FloatSeq.with(Mathf.num(unitTeam != team), Mathf.num(boosting), unitArmor, resetTime, dummySize);
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.i(unit == null ? -1 : unit.id);
            write.bool(boosting);
            write.f(unitArmor);
            write.f(resetTime);
            write.f(dummySize);
            write.i(unitTeam.id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            readUnitId = read.i();
            boosting = read.bool();
            unitArmor = read.f();
            resetTime = read.f();
            dummySize = read.f();
            unitTeam = Team.get(read.i());
        }
    }
}