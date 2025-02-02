package mindustry.world.blocks.campaign;

import arc.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class LandingPad extends Block{
    static ObjectMap<Item, Seq<LandingPadBuild>> waiting = new ObjectMap<>();
    static long lastUpdateId = -1;

    static{
        Events.on(ResetEvent.class, e -> {
            waiting.clear();
            lastUpdateId = -1;
        });
    }

    public float cooldownTime = 12f;
    public float consumeLiquidAmount = 100f;
    public Liquid consumeLiquid = Liquids.water;

    public LandingPad(String name){
        super(name);

        hasItems = true;
        hasLiquids = true;
        solid = true;
        update = true;
        configurable = true;
        acceptsItems = false;

        config(Item.class, (LandingPadBuild build, Item item) -> build.config = item);
        configClear((LandingPadBuild build) -> build.config = null);
    }

    @Override
    public void init(){
        consume(new ConsumeLiquid(consumeLiquid, consumeLiquidAmount){

            @Override
            public void build(Building build, Table table){
                table.add(new ReqImage(liquid.uiIcon, () -> build.liquids.get(liquid) >= amount)).size(iconMed).top().left();
            }

            @Override
            public float efficiency(Building build){
                return build.liquids.get(consumeLiquid) >= amount ? 1f : 0f;
            }

            @Override
            public void display(Stats stats){
                stats.add(Stat.input, liquid, amount, false);
            }
        }).update(false);

        super.init();
    }

    @Override
    public void setBars(){
        super.setBars();

        addLiquidBar(consumeLiquid);
        //TODO: does cooldown even need to exist?
        addBar("heat", (LandingPadBuild entity) -> new Bar("bar.heat", Pal.lightOrange, () -> entity.cooldown));
    }

    @Override
    public boolean outputsItems(){
        return true;
    }

    @Remote(called = Loc.server)
    public static void landingPadLanded(Tile tile){
        if(tile == null || !(tile.build instanceof LandingPadBuild build)) return;
        build.handleLanding();
    }

    public class LandingPadBuild extends Building{
        public @Nullable Item config;
        //priority collisions are possible, but should be extremely rare
        public int priority = Mathf.rand.nextInt();
        public float cooldown = 0f;


        public void handleLanding(){
            if(!state.isCampaign() || config == null) return;
            //TODO animation, etc

            cooldown = 1f;
            items.set(config, itemCapacity);
            liquids.remove(consumeLiquid, consumeLiquidAmount);
            for(int i = 0; i < 10; i++){
                Fx.steam.at(this);
            }
            //TODO this is a temporary effect
            Fx.shockwave.at(this);

            state.rules.sector.info.importCooldownTimers.put(config, 0f);
        }

        public void updateTimers(){
            if(state.isCampaign() && lastUpdateId != state.updateId){
                lastUpdateId = state.updateId;

                float[] imports = state.rules.sector.info.getImportRates(state.getPlanet());

                for(Item item : content.items()){
                    float importedPerFrame = imports[item.id]/60f;
                    if(importedPerFrame > 0f){
                        float framesBetweenArrival = itemCapacity / importedPerFrame;

                        state.rules.sector.info.importCooldownTimers.increment(item, 0f, 1f / framesBetweenArrival * Time.delta);
                    }
                }

                waiting.each((item, pads) -> {
                    if(pads.size > 0){
                        pads.sort(p -> p.priority);

                        var first = pads.first();
                        var head = pads.peek();

                        Call.landingPadLanded(first.tile);


                        //swap priorities, moving this block to the end of the list (if there is only one block waiting, this does nothing)
                        var tmp = first.priority;
                        first.priority = head.priority;
                        head.priority = tmp;


                        pads.clear();
                    }
                });
            }
        }

        @Override
        public void updateTile(){
            updateTimers();

            if(items.total() > 0){
                dumpAccumulate(config == null || items.get(config) != items.total() ? null : config);
            }

            if(config != null && state.isCampaign()){

                cooldown -= delta() / cooldownTime;

                if(cooldown <= 0f && efficiency > 0f && items.total() == 0 && state.rules.sector.info.importCooldownTimers.get(config, 0f) >= 1f){

                    //queue landing for next frame
                    waiting.get(config, Seq::new).add(this);
                }
            }
        }

        @Override
        public boolean canDump(Building to, Item item){
            //hack: canDump is only ever called right before item offload, so count the item as "produced" before that.
            //TODO: is this necessary?
            produced(item);
            return true;
        }

        @Override
        public void buildConfiguration(Table table){
            ItemSelection.buildTable(LandingPad.this, table, content.items(), () -> config, this::configure, selectionRows, selectionColumns);
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return false;
        }

        @Override
        public @Nullable Object config(){
            return config;
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            config = TypeIO.readItem(read);
            priority = read.i();
            cooldown = read.f();
        }

        @Override
        public void write(Writes write){
            super.write(write);
            TypeIO.writeItem(write, config);
            write.i(priority);
            write.f(cooldown);
        }
    }
}
