package mindustry.world.blocks.campaign;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

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

        bars.add("items", entity -> new Bar(() -> Core.bundle.format("bar.items", entity.items.total()), () -> Pal.items, () -> (float)entity.items.total() / itemCapacity));
    }

    public class LaunchPadEntity extends Building{
        @Override
        public void draw(){
            super.draw();

            if(!state.isCampaign()) return;

            if(lightRegion.found()){
                Draw.color(lightColor);
                float progress = Math.min((float)items.total() / itemCapacity, timer.getTime(timerLaunch) / (launchTime / timeScale));
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

            float cooldown = Mathf.clamp(timer.getTime(timerLaunch) / (90f / timeScale));

            Draw.mixcol(lightColor, 1f - cooldown);

            Draw.rect(podRegion, x, y);

            Draw.reset();
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.total() < itemCapacity;
        }

        @Override
        public void updateTile(){
            if(!state.isCampaign()) return;

            //launch when full and base conditions are met
            if(items.total() >= itemCapacity && efficiency() >= 1f && timer(timerLaunch, launchTime / timeScale)){
                LaunchPayload entity = LaunchPayload.create();
                items.each((item, amount) -> entity.stacks().add(new ItemStack(item, amount)));
                entity.set(this);
                entity.lifetime(120f);
                entity.team(team);
                entity.add();
                Fx.launchPod.at(this);
                items.clear();
                Effects.shake(3f, 3f, this);
            }
        }
    }

    @EntityDef(LaunchPayloadc.class)
    @Component(base = true)
    static abstract class LaunchPayloadComp implements Drawc, Timedc, Teamc{
        @Import float x,y;

        Seq<ItemStack> stacks = new Seq<>();
        transient Interval in = new Interval();

        @Override
        public void draw(){
            float alpha = fout(Interp.pow5Out);
            float scale = (1f - alpha) * 1.3f + 1f;
            float cx = cx(), cy = cy();
            float rotation = fin() * (130f + Mathf.randomSeedRange(id(), 50f));

            Draw.z(Layer.effect + 0.001f);

            Draw.color(Pal.engine);

            float rad = 0.2f + fslope();

            Fill.light(cx, cy, 10, 25f * (rad + scale-1f), Tmp.c2.set(Pal.engine).a(alpha), Tmp.c1.set(Pal.engine).a(0f));

            Draw.alpha(alpha);
            for(int i = 0; i < 4; i++){
                Drawf.tri(cx, cy, 6f, 40f * (rad + scale-1f), i * 90f + rotation);
            }

            Draw.color();

            Draw.z(Layer.weather - 1);

            TextureRegion region = Core.atlas.find("launchpod");
            float rw = region.getWidth() * Draw.scl * scale, rh = region.getHeight() * Draw.scl * scale;

            Draw.alpha(alpha);
            Draw.rect(region, cx, cy, rw, rh, rotation);

            Tmp.v1.trns(225f, fin(Interp.pow3In) * 250f);

            Draw.z(Layer.flyingUnit + 1);
            Draw.color(0, 0, 0, 0.22f * alpha);
            Draw.rect(region, cx + Tmp.v1.x, cy + Tmp.v1.y, rw, rh, rotation);

            Draw.reset();
        }

        float cx(){
            return x + fin(Interp.pow2In) * (12f + Mathf.randomSeedRange(id() + 3, 4f));
        }

        float cy(){
            return y + fin(Interp.pow5In) * (100f + Mathf.randomSeedRange(id() + 2, 30f));
        }

        @Override
        public void update(){
            float r = 3f;
            if(in.get(4f - fin()*2f)){
                Fx.rocketSmoke.at(cx() + Mathf.range(r), cy() + Mathf.range(r), fin());
            }
        }

        @Override
        public void remove(){
            //actually launch the items upon removal
            if(team() == state.rules.defaultTeam && state.secinfo.origin != null){
                ItemSeq dest = state.secinfo.origin.getExtraItems();

                for(ItemStack stack : stacks){
                    dest.add(stack);

                    //update export
                    state.secinfo.handleItemExport(stack);
                    Events.fire(new LaunchItemEvent(stack));
                }

                state.secinfo.origin.setExtraItems(dest);
            }
        }
    }
}
