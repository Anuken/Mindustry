package mindustry.world.blocks.storage;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

public class LaunchPad extends Block{
    public final int timerLaunch = timers++;
    /** Time inbetween launches. */
    public float launchTime;

    public LaunchPad(String name){
        super(name);
        hasItems = true;
        solid = true;
        update = true;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(BlockStat.launchTime, launchTime / 60f, StatUnit.seconds);
    }

    @Override
    public void setBars(){
        super.setBars();

        bars.add("items", entity -> new Bar(() -> Core.bundle.format("bar.items", entity.items().total()), () -> Pal.items, () -> (float)entity.items().total() / itemCapacity));
    }

    public class LaunchPadEntity extends TileEntity{
        @Override
        public void draw(){
            super.draw();

            Draw.rect("launchpod", x, y);
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            return items.total() < itemCapacity;
        }

        @Override
        public void updateTile(){

            //launch when full
            if(items.total() >= itemCapacity){
                LaunchPayloadc entity = LaunchPayloadEntity.create();
                items.each((item, amount) -> entity.stacks().add(new ItemStack(item, amount)));
                entity.set(this);
                entity.lifetime(120f);
                entity.team(team);
                entity.add();
                Fx.launchPod.at(this);
                items.clear();
            }
        }
    }

    @EntityDef(LaunchPayloadc.class)
    @Component
    static abstract class LaunchPayloadComp implements Drawc, Timedc, Teamc{
        static final float speed = 1.6f;

        @Import float x,y;

        float height;
        Array<ItemStack> stacks = new Array<>();

        @Override
        public void draw(){
            float alpha = fout(Interp.pow5Out);
            float cx = x + fin(Interp.pow2In) * 90f, cy = y + height;
            float rotation = fin() * 120f;

            Draw.z(Layer.effect);

            Draw.color(Pal.engine);

            float rad = 0.2f + fslope();

            Fill.light(cx, cy, 10, 25f * rad, Pal.engine, Tmp.c1.set(Pal.engine).a(0f));

            for(int i = 0; i < 4; i++){
                Drawf.tri(cx, cy, 6f, 40f * rad, i * 90f + rotation);
            }

            Draw.color();

            Draw.z(Layer.weather - 1);

            Draw.alpha(alpha);
            Draw.rect("launchpod", cx, cy, rotation);

            Tmp.v1.trns(225f, height);

            Draw.z(Layer.flyingUnit + 1);
            Draw.color(0, 0, 0, 0.22f * alpha);
            Draw.rect("launchpod", cx + Tmp.v1.x, cy + Tmp.v1.y, rotation);

            Draw.reset();
        }

        @Override
        public void update(){
            height += Time.delta() * speed;
        }

        @Override
        public void remove(){
            if(team() == Vars.state.rules.defaultTeam){
                for(ItemStack stack : stacks){
                    Vars.data.addItem(stack.item, stack.amount);
                    Events.fire(new LaunchItemEvent(stack));
                    Vars.state.stats.handleItemExport(stack);
                }
            }
        }
    }
}
