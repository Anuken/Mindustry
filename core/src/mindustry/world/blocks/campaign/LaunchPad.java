package mindustry.world.blocks.campaign;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class LaunchPad extends Block{
    /** Time inbetween launches. */
    public float launchTime = 1f;
    public Sound launchSound = Sounds.none;

    public @Load("@-light") TextureRegion lightRegion;
    public @Load(value = "@-pod", fallback = "launchpod") TextureRegion podRegion;
    public Color lightColor = Color.valueOf("eab678");

    public LaunchPad(String name){
        super(name);
        hasItems = true;
        solid = true;
        update = true;
        configurable = true;
        flags = EnumSet.of(BlockFlag.launchPad);
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.launchTime, launchTime / 60f, StatUnit.seconds);
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("items", entity -> new Bar(() -> Core.bundle.format("bar.items", entity.items.total()), () -> Pal.items, () -> (float)entity.items.total() / itemCapacity));

        //TODO is "bar.launchcooldown" the right terminology?
        addBar("progress", (LaunchPadBuild build) -> new Bar(() -> Core.bundle.get("bar.launchcooldown"), () -> Pal.ammo, () -> Mathf.clamp(build.launchCounter / launchTime)));
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    public class LaunchPadBuild extends Building{
        public float launchCounter;

        @Override
        public Cursor getCursor(){
            return !state.isCampaign() || net.client() ? SystemCursor.arrow : super.getCursor();
        }

        @Override
        public boolean shouldConsume(){
            //TODO add launch costs, maybe legacy version
            return launchCounter < launchTime;
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return Mathf.clamp(launchCounter / launchTime);
            return super.sense(sensor);
        }

        @Override
        public void draw(){
            super.draw();

            if(!state.isCampaign()) return;

            if(lightRegion.found()){
                Draw.color(lightColor);
                float progress = Math.min((float)items.total() / itemCapacity, launchCounter / launchTime);
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

            //increment launchCounter then launch when full and base conditions are met
            if((launchCounter += edelta()) >= launchTime && items.total() >= itemCapacity){
                //if there are item requirements, use those.
                consume();
                launchSound.at(x, y);
                LaunchPayload entity = LaunchPayload.create();
                items.each((item, amount) -> entity.stacks.add(new ItemStack(item, amount)));
                entity.set(this);
                entity.lifetime(120f);
                entity.team(team);
                entity.add();
                Fx.launchPod.at(this);
                items.clear();
                Effect.shake(3f, 3f, this);
                launchCounter = 0f;
            }
        }

        @Override
        public void display(Table table){
            super.display(table);

            if(!state.isCampaign() || net.client() || team != player.team()) return;

            table.row();
            table.label(() -> {
                Sector dest = state.rules.sector == null ? null : state.rules.sector.info.getRealDestination();

                return Core.bundle.format("launch.destination",
                    dest == null || !dest.hasBase() ? Core.bundle.get("sectors.nonelaunch") :
                    "[accent]" + dest.name());
            }).pad(4).wrap().width(200f).left();
        }

        @Override
        public void buildConfiguration(Table table){
            if(!state.isCampaign() || net.client()){
                deselect();
                return;
            }

            table.button(Icon.upOpen, Styles.cleari, () -> {
                ui.planet.showSelect(state.rules.sector, other -> {
                    if(state.isCampaign() && other.planet == state.rules.sector.planet){
                        state.rules.sector.info.destination = other;
                    }
                });
                deselect();
            }).size(40f);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(launchCounter);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            if(revision >= 1){
                launchCounter = read.f();
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

            TextureRegion region = blockOn() instanceof mindustry.world.blocks.campaign.LaunchPad p ? p.podRegion : Core.atlas.find("launchpod");
            float rw = region.width * Draw.scl * scale, rh = region.height * Draw.scl * scale;

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
            if(!state.isCampaign()) return;

            Sector destsec = state.rules.sector.info.getRealDestination();

            //actually launch the items upon removal
            if(team() == state.rules.defaultTeam){
                if(destsec != null && (destsec != state.rules.sector || net.client())){
                    ItemSeq dest = new ItemSeq();

                    for(ItemStack stack : stacks){
                        dest.add(stack);

                        //update export
                        state.rules.sector.info.handleItemExport(stack);
                        Events.fire(new LaunchItemEvent(stack));
                    }

                    if(!net.client()){
                        destsec.addItems(dest);
                    }
                }
            }
        }
    }
}
