package mindustry.world.blocks.campaign;

import arc.*;
import arc.Graphics.*;
import arc.Graphics.Cursor.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.liquid.*;
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

    public @Load(value = "@-pod", fallback = "advanced-launch-pad-pod") TextureRegion podRegion;
    public float arrivalDuration = 150f;
    public float cooldownTime = 150f;
    public float consumeLiquidAmount = 100f;
    public Liquid consumeLiquid = Liquids.water;

    public Effect landEffect = Fx.podLandShockwave;
    public Effect coolingEffect = Fx.none;
    public float coolingEffectChance = 0.2f;

    public float liquidPad = 2f;
    public Color bottomColor = Pal.darkerMetal;

    public LandingPad(String name){
        super(name);

        hasItems = true;
        hasLiquids = true;
        solid = true;
        update = true;
        configurable = true;
        acceptsItems = false;
        canOverdrive = false; //overdriving can't do anything meaningful besides decrease cooldown, which is very small anyway, so don't bother
        emitLight = true;
        lightRadius = 90f;

        config(Item.class, (LandingPadBuild build, Item item) -> {
            if(!build.accessible()) return;

            build.config = item;
        });
        configClear((LandingPadBuild build) -> {
            if(!build.accessible()) return;

            build.config = null;
        });
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
        addBar("cooldown", (LandingPadBuild entity) -> new Bar("bar.cooldown", Pal.lightOrange, () -> entity.cooldown));
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.cooldownTime, (cooldownTime+arrivalDuration)/60f, StatUnit.seconds);
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
        public float cooldown = 0f, landParticleTimer;

        public float arrivingTimer = 0f;
        public @Nullable Item arriving;
        public float liquidRemoved;

        public void handleLanding(){
            if(config == null) return;

            cooldown = 1f;
            arriving = config;
            arrivingTimer = 0f;
            liquidRemoved = 0f;

            if(state.isCampaign() && !isFake()){
                state.rules.sector.info.importCooldownTimers.put(config, 0f);
            }
        }

        public boolean accessible(){
            //In custom games, this block can be configured by anyone except the player team; this allows for enemy builder AI to use it
            return state.rules.editor || state.rules.allowEditWorldProcessors || state.isCampaign() || state.rules.infiniteResources || (team != state.rules.defaultTeam && !state.rules.pvp && team != Team.derelict);
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
                    }else{
                        //nothing is being imported, so reset the timer
                        state.rules.sector.info.importCooldownTimers.put(item, 0f);
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
        public void draw(){
            if(consumeLiquid != null){
                Draw.color(bottomColor);
                Fill.square(x, y, size * tilesize/2f - liquidPad);
                Draw.color();
                LiquidBlock.drawTiledFrames(block.size, x, y, liquidPad, liquidPad, liquidPad, liquidPad, consumeLiquid, liquids.get(consumeLiquid) / liquidCapacity);
            }

            super.draw();

            if(arriving != null){
                float fin = Mathf.clamp(arrivingTimer), fout = 1f - fin;
                float alpha = Interp.pow5Out.apply(fin);
                float scale = (1f - alpha) * 1.3f + 1f;
                float
                cx = x,
                cy = y + Interp.pow4In.apply(fout) * (100f + Mathf.randomSeedRange(id() + 2, 30f));

                float rotation = fout * (90f + Mathf.randomSeedRange(id(), 50f));

                Draw.z(Layer.effect + 0.001f);

                Draw.color(Pal.engine);

                float rad = 0.15f + Interp.pow5Out.apply(Mathf.slope(fin));

                Fill.light(cx, cy, 10, 25f * (rad + scale-1f), Tmp.c2.set(Pal.engine).a(alpha), Tmp.c1.set(Pal.engine).a(0f));

                Draw.alpha(alpha);
                for(int i = 0; i < 4; i++){
                    Drawf.tri(cx, cy, 6f, 40f * (rad + scale-1f), i * 90f + rotation);
                }

                Draw.color();

                Draw.z(Layer.weather - 1);

                scale *= podRegion.scl();
                float rw = podRegion.width * scale, rh = podRegion.height * scale;

                Draw.alpha(alpha);
                Drawf.shadow(cx, cy, size * tilesize, fin);
                Draw.rect(podRegion, cx, cy, rw, rh, rotation);

                Tmp.v1.trns(225f, Interp.pow3In.apply(fout) * 250f);

                Draw.z(Layer.flyingUnit + 1);
                Draw.color(0, 0, 0, 0.22f * alpha);

                Draw.rect(podRegion, cx + Tmp.v1.x, cy + Tmp.v1.y, rw, rh, rotation);

            }else if(cooldown > 0f){

                Drawf.shadow(x, y, size * tilesize, cooldown);
                Draw.alpha(cooldown);
                Draw.mixcol(Pal.accent, 1f - cooldown);
                Draw.rect(podRegion, x, y);
            }

            Draw.reset();
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, lightRadius, Pal.accent, Mathf.clamp(Math.max(cooldown, arrivingTimer * 1.5f)));
        }

        @Override
        public void updateTile(){
            updateTimers();

            if(arriving != null){
                if(!headless){ //pod particles
                    float fin = arrivingTimer;
                    float tsize = Interp.pow5Out.apply(fin);

                    landParticleTimer += tsize * Time.delta / 2f;
                    if(landParticleTimer >= 1f){
                        tile.getLinkedTiles(t -> {
                            if(Mathf.chance(0.1f)){
                                Fx.podLandDust.at(t.worldx(), t.worldy(), angleTo(t.worldx(), t.worldy()) + Mathf.range(30f), Tmp.c1.set(t.floor().mapColor).mul(1.5f + Mathf.range(0.15f)));
                            }
                        });

                        landParticleTimer = 0f;
                    }
                }

                arrivingTimer += Time.delta / arrivalDuration;

                float toRemove = Math.min(consumeLiquidAmount / arrivalDuration * Time.delta, consumeLiquidAmount - liquidRemoved);
                liquidRemoved += toRemove;

                liquids.remove(consumeLiquid, toRemove);

                if(Mathf.chanceDelta(coolingEffectChance * Interp.pow5Out.apply(arrivingTimer))){
                    coolingEffect.at(this);
                }

                if(arrivingTimer >= 1f){
                    //remove any leftovers to make sure it's precise
                    liquids.remove(consumeLiquid, consumeLiquidAmount - liquidRemoved);

                    landEffect.at(this);
                    Effect.shake(3f, 3f, this);

                    items.set(arriving, itemCapacity);
                    if(!isFake()){
                        //receiving items counts as "production" for now
                        produced(arriving, itemCapacity);
                        state.getSector().info.handleItemImport(arriving, itemCapacity);
                    }

                    arriving = null;
                    arrivingTimer = 0f;
                }
            }

            if(items.total() > 0){
                dumpAccumulate(config == null || items.get(config) != items.total() ? null : config);
            }

            if(arriving == null){
                cooldown -= delta() / cooldownTime;
                cooldown = Mathf.clamp(cooldown);
            }

            if(config != null && (isFake() || (state.isCampaign() && !state.getPlanet().campaignRules.legacyLaunchPads))){

                if(cooldown <= 0f && efficiency > 0f && items.total() == 0 && (isFake() || (state.rules.sector.info.getImportRate(state.getPlanet(), config) > 0f && state.rules.sector.info.importCooldownTimers.get(config, 0f) >= 1f))){

                    if(isFake()){
                        //there is no queue for enemy team blocks, it's all fake
                        Call.landingPadLanded(tile);
                    }else{
                        //queue landing for next frame
                        waiting.get(config, Seq::new).add(this);
                    }
                }
            }
        }

        /** @return whether this pad should receive items forever, essentially acting as an item source for maps. */
        public boolean isFake(){
            return team != state.rules.defaultTeam || !state.isCampaign();
        }

        @Override
        public void drawSelect(){
            drawItemSelection(config);
        }

        @Override
        public Cursor getCursor(){
            return !accessible() ? SystemCursor.arrow : super.getCursor();
        }

        @Override
        public boolean shouldShowConfigure(Player player){
            return accessible();
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            if(this == other || !accessible()){
                deselect();
                return false;
            }

            return super.onConfigureBuildTapped(other);
        }

        @Override
        public void buildConfiguration(Table table){

            ItemSelection.buildTable(LandingPad.this, table, content.items(), () -> config, this::configure, selectionRows, selectionColumns);

            if(!net.client() && !isFake()){
                table.row();

                table.table(t -> {
                    t.background(Styles.black6);

                    t.button(Icon.downOpen, Styles.clearNonei, 40f, () -> {
                        if(config == null || !state.isCampaign()) return;

                        for(Sector sector : state.getPlanet().sectors){
                            if(!canRedirectExports(sector)) continue;
                            sector.info.destination = state.getSector();
                            sector.saveInfo();
                        }
                        state.getSector().info.refreshImportRates(state.getPlanet());
                    }).disabled(button -> config == null || !state.isCampaign() || (!state.getPlanet().sectors.contains(this::canRedirectExports)))
                    .tooltip("@sectors.redirect").get();
                }).fillX().left();
            }
        }

        private boolean canRedirectExports(Sector sector){
            return sector.hasBase() && sector != state.getSector() && sector.info.hasExport(config) && sector.info.destination != state.getSector();
        }

        @Override
        public void display(Table table){
            super.display(table);

            if(!state.isCampaign() || net.client() || team != player.team() || isFake()) return;

            table.row();
            table.label(() -> {
                if(!state.isCampaign() || isFake()) return "";

                if(state.getPlanet().campaignRules.legacyLaunchPads){
                    return Core.bundle.get("landingpad.legacy.disabled");
                }

                if(config == null) return "";

                int sources = 0;
                float perSecond = 0f;
                for(var otherSector : state.getPlanet().sectors){
                    if(otherSector == state.getSector() || !otherSector.hasBase() || otherSector.info.destination != state.getSector()) continue;

                    float amount = otherSector.info.getExport(config);
                    if(amount <= 0) continue;
                    sources ++;
                    perSecond += amount;
                }

                String str = Core.bundle.format("landing.sources", sources == 0 ? Core.bundle.get("none") : sources);
                if(perSecond > 0){
                    str += "\n" + Core.bundle.format("landing.import", config.emoji(), (int)(perSecond * 60f));
                }
                return str;
            }).pad(4).wrap().width(200f).left();
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
        public byte version(){
            return 1;
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            config = TypeIO.readItem(read);
            priority = read.i();
            cooldown = read.f();

            if(revision >= 1){
                arriving = TypeIO.readItem(read);
                arrivingTimer = read.f();
                liquidRemoved = read.f();
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            TypeIO.writeItem(write, config);
            write.i(priority);
            write.f(cooldown);

            TypeIO.writeItem(write, arriving);
            write.f(arrivingTimer);
            write.f(liquidRemoved);
        }
    }
}
