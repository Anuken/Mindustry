package mindustry.world.blocks.storage;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
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

    public @Load("@-light") TextureRegion lightRegion;
    public @Load("launchpod") TextureRegion podRegion;
    public Color lightColor = Color.valueOf("eab678");

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

            if(lightRegion.found()){
                Draw.color(lightColor);
                float progress = Math.min((float)items.total() / itemCapacity, timer.getTime(timerLaunch) / launchTime);
                int steps = 3;
                float step = 1f;

                for(int i = 0; i < 4; i++){
                    for(int j = 0; j < steps; j++){
                        float alpha = Mathf.curve(progress, (float)j / steps, (j+1f) / steps);
                        float offset = -(j - 1f) * step;

                        Draw.color(Pal.metalGrayDark, lightColor, alpha);
                        Draw.rect(lightRegion, x + Geometry.d8edge(i).x * offset, y + Geometry.d8edge(i).y * offset, i * 90);
                    }
                }

                Draw.reset();
            }

            float cooldown = Mathf.clamp(timer.getTime(timerLaunch) / 60f);

            Draw.mixcol(lightColor, 1f - cooldown);

            Draw.rect(podRegion, x, y);

            Draw.reset();
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            return items.total() < itemCapacity;
        }

        @Override
        public void updateTile(){

            //launch when full and base conditions are met
            if(items.total() >= itemCapacity && efficiency() >= 1f && timer(timerLaunch, launchTime)){
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
        @Import float x,y;

        Array<ItemStack> stacks = new Array<>();

        @Override
        public void draw(){
            float alpha = fout(Interp.pow5Out);
            float cx = x + fin(Interp.pow2In) * 15f, cy = y + fin(Interp.pow5In) * 130f;
            float rotation = fin() * 120f;

            Draw.z(Layer.effect);

            Draw.color(Pal.engine);

            float rad = 0.2f + fslope();

            Fill.light(cx, cy, 10, 25f * rad, Tmp.c2.set(Pal.engine), Tmp.c1.set(Pal.engine).a(0f));

            for(int i = 0; i < 4; i++){
                Drawf.tri(cx, cy, 6f, 40f * rad, i * 90f + rotation);
            }

            Draw.color();

            Draw.z(Layer.weather - 1);

            Draw.alpha(alpha);
            Draw.rect("launchpod", cx, cy, rotation);

            Tmp.v1.trns(225f, fin(Interp.linear) * 250f);

            Draw.z(Layer.flyingUnit + 1);
            Draw.color(0, 0, 0, 0.22f * alpha);
            Draw.rect("launchpod", cx + Tmp.v1.x, cy + Tmp.v1.y, rotation);

            Draw.reset();
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
