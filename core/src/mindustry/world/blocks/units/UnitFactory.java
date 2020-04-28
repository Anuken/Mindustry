package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class UnitFactory extends Block{
    public float launchVelocity = 0f;
    public TextureRegion topRegion;
    public int[] capacities;

    public UnitPlan[] plans = new UnitPlan[0];

    public UnitFactory(String name){
        super(name);
        update = true;
        hasPower = true;
        hasItems = true;
        solid = false;
        flags = EnumSet.of(BlockFlag.producer);
        configurable = true;

        config(Integer.class, (tile, i) -> ((UnitFactoryEntity)tile).currentPlan = i < 0 || i >= plans.length ? -1 : i);
    }

    @Remote(called = Loc.server)
    public static void onUnitFactorySpawn(Tile tile){
        if(!(tile.entity instanceof UnitFactoryEntity)) return;
        tile.<UnitFactoryEntity>ent().spawned();
    }

    @Override
    public void init(){
        super.init();

        capacities = new int[Vars.content.items().size];
        if(consumes.has(ConsumeType.item)){
            ConsumeItems cons = consumes.get(ConsumeType.item);
            for(ItemStack stack : cons.items){
                capacities[stack.item.id] = stack.amount * 2;
            }
        }
    }

    @Override
    public void load(){
        super.load();

        topRegion = Core.atlas.find(name + "-top");
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("progress", entity -> new Bar("bar.progress", Pal.ammo, ((UnitFactoryEntity)entity)::fraction));
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.itemCapacity);
        //TODO
        //stats.add(BlockStat.productionTime, produceTime / 60f, StatUnit.seconds);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-top")};
    }

    public static class UnitPlan{
        public UnitType unit;
        public ItemStack[] requirements;
        public float time;

        public UnitPlan(UnitType unit, float time, ItemStack[] requirements){
            this.unit = unit;
            this.time = time;
            this.requirements = requirements;
        }

        UnitPlan(){}
    }

    public class UnitFactoryEntity extends TileEntity{
        public int currentPlan = -1;
        public float progress, time, speedScl;

        public float fraction(){
            return currentPlan == -1 ? 0 : progress / plans[currentPlan].time;
        }

        public void spawned(){
            progress = 0f;

            Effects.shake(2f, 3f, this);
            Fx.producesmoke.at(this);

            if(!net.client() && currentPlan != -1){
                UnitPlan plan = plans[currentPlan];
                Unitc unit = plan.unit.create(team);
                unit.set(x + Mathf.range(4), y + Mathf.range(4));
                unit.add();
                unit.vel().y = launchVelocity;
                Events.fire(new UnitCreateEvent(unit));
            }
        }

        @Override
        public void buildConfiguration(Table table){
            Array<UnitType> units = Array.with(plans).map(u -> u.unit);

            ItemSelection.buildTable(table, units, () -> currentPlan == -1 ? null : plans[currentPlan].unit, unit -> tile.configure(units.indexOf(unit)));
        }

        @Override
        public Object config(){
            return currentPlan;
        }

        @Override
        public void draw(){
            super.draw();

            if(currentPlan != -1){
                UnitPlan plan = plans[currentPlan];

                Draw.draw(Layer.blockOver, () -> {
                    TextureRegion region = plan.unit.icon(Cicon.full);

                    Shaders.build.region = region;
                    Shaders.build.progress = progress / plan.time;
                    Shaders.build.color.set(Pal.accent);
                    Shaders.build.color.a = speedScl;
                    Shaders.build.time = -time / 20f;

                    Draw.shader(Shaders.build);
                    Draw.rect(region, x, y);
                    Draw.shader();

                    Draw.color(Pal.accent);
                    Draw.alpha(speedScl);

                    Lines.lineAngleCenter(x + Mathf.sin(time, 20f, Vars.tilesize / 2f * size - 2f), y, 90, size * Vars.tilesize - 4f);

                    Draw.reset();
                });
            }

            Draw.z(Layer.blockOver);
            Draw.rect(topRegion, x, y);
        }

        @Override
        public void updateTile(){
            if(currentPlan < 0 || currentPlan >= plans.length){
                currentPlan = -1;
            }

            if((consValid() || tile.isEnemyCheat()) && currentPlan != -1){
                time += delta() * efficiency() * speedScl * Vars.state.rules.unitBuildSpeedMultiplier;
                progress += delta() * efficiency() * Vars.state.rules.unitBuildSpeedMultiplier;
                speedScl = Mathf.lerpDelta(speedScl, 1f, 0.05f);
            }else{
                speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f);
            }

            if(currentPlan != -1){
                UnitPlan plan = plans[currentPlan];

                if(progress >= plan.time){
                    progress = 0f;

                    Call.onUnitFactorySpawn(tile);
                    useContent(plan.unit);
                    consume();
                }
            }else{
                progress = 0f;
            }
        }

        @Override
        public int getMaximumAccepted(Item item){
            return capacities[item.id];
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.s(currentPlan);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            currentPlan = read.s();
        }
    }
}
