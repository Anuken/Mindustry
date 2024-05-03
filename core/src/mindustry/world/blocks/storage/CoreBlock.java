package mindustry.world.blocks.storage;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.meta.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    protected static final float cloudScaling = 1700f, cfinScl = -2f, cfinOffset = 0.3f, calphaFinOffset = 0.25f, cloudAlpha = 0.81f;
    protected static final float[] cloudAlphas = {0, 0.5f, 1f, 0.1f, 0, 0f};

    //hacky way to pass item modules between methods
    private static ItemModule nextItems;
    protected static final float[] thrusterSizes = {0f, 0f, 0f, 0f, 0.3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 0f};

    public @Load(value = "@-thruster1", fallback = "clear-effect") TextureRegion thruster1; //top right
    public @Load(value = "@-thruster2", fallback = "clear-effect") TextureRegion thruster2; //bot left
    public float thrusterLength = 14f/4f;
    public boolean isFirstTier;
    /** If true, this core type requires a core zone to upgrade. */
    public boolean requiresCoreZone;
    public boolean incinerateNonBuildable = false;

    public UnitType unitType = UnitTypes.alpha;
    public float landDuration = 160f;
    public Music landMusic = Musics.land;
    public Music launchMusic = Musics.coreLaunch;
    public Effect launchEffect = Fx.launch;

    public Interp landZoomInterp = Interp.pow3;
    public float landZoomFrom = 0.02f, landZoomTo = 4f;

    public float captureInvicibility = 60f * 15f;

    public CoreBlock(String name){
        super(name);

        solid = true;
        update = true;
        hasItems = true;
        priority = TargetPriority.core;
        flags = EnumSet.of(BlockFlag.core);
        unitCapModifier = 10;
        loopSound = Sounds.respawning;
        loopSoundVolume = 1f;
        drawDisabled = false;
        canOverdrive = false;
        envEnabled |= Env.space;

        //support everything
        replaceable = false;
    }

    @Remote(called = Loc.server)
    public static void playerSpawn(Tile tile, Player player){
        if(player == null || tile == null || !(tile.build instanceof CoreBuild core)) return;

        UnitType spawnType = ((CoreBlock)core.block).unitType;
        if(core.wasVisible){
            Fx.spawn.at(core);
        }

        player.set(core);

        if(!net.client()){
            Unit unit = spawnType.create(tile.team());
            unit.set(core);
            unit.rotation(90f);
            unit.impulse(0f, 3f);
            unit.spawnedByCore(true);
            unit.controller(player);
            unit.add();
        }

        if(state.isCampaign() && player == Vars.player){
            spawnType.unlock();
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.buildTime);
        stats.add(Stat.unitType, table -> {
            table.row();
            table.table(Styles.grayPanel, b -> {
                b.image(unitType.uiIcon).size(40).pad(10f).left().scaling(Scaling.fit);
                b.table(info -> {
                    info.add(unitType.localizedName).left();
                    if(Core.settings.getBool("console")){
                        info.row();
                        info.add(unitType.name).left().color(Color.lightGray);
                    }
                });
                b.button("?", Styles.flatBordert, () -> ui.content.show(unitType)).size(40f).pad(10).right().grow().visible(() -> unitType.unlockedNow());
            }).growX().pad(5).row();
        });
    }

    @Override
    public void setBars(){
        super.setBars();

        addBar("capacity", (CoreBuild e) -> new Bar(
            () -> Core.bundle.format("bar.capacity", UI.formatAmount(e.storageCapacity)),
            () -> Pal.items,
            () -> e.items.total() / ((float)e.storageCapacity * content.items().count(UnlockableContent::unlockedNow))
        ));
    }

    @Override
    public void init(){
        //assign to update clipSize internally
        lightRadius = 30f + 20f * size;
        fogRadius = Math.max(fogRadius, (int)(lightRadius / 8f * 3f) + 13);
        emitLight = true;

        super.init();
    }

    @Override
    public boolean canBreak(Tile tile){
        return state.isEditor();
    }

    @Override
    public boolean canReplace(Block other){
        //coreblocks can upgrade smaller cores
        return super.canReplace(other) || (other instanceof CoreBlock && size >= other.size && other != this);
    }

    @Override
    public boolean canPlaceOn(Tile tile, Team team, int rotation){
        if(tile == null) return false;
        //in the editor, you can place them anywhere for convenience
        if(state.isEditor()) return true;

        CoreBuild core = team.core();

        //special floor upon which cores can be placed
        tile.getLinkedTilesAs(this, tempTiles);
        if(!tempTiles.contains(o -> !o.floor().allowCorePlacement || o.block() instanceof CoreBlock)){
            return true;
        }

        //must have all requirements
        if(core == null || (!state.rules.infiniteResources && !core.items.has(requirements, state.rules.buildCostMultiplier))) return false;

        return tile.block() instanceof CoreBlock && size > tile.block().size && (!requiresCoreZone || tempTiles.allMatch(o -> o.floor().allowCorePlacement));
    }

    @Override
    public void placeBegan(Tile tile, Block previous, Unit builder){
        //finish placement immediately when a block is replaced.
        if(previous instanceof CoreBlock){
            tile.setBlock(this, tile.team());
            tile.block().placeEffect.at(tile, tile.block().size);
            Fx.upgradeCore.at(tile.drawx(), tile.drawy(), 0f, tile.block());
            Fx.upgradeCoreBloom.at(tile, tile.block().size);

            //set up the correct items
            if(nextItems != null){
                //force-set the total items
                if(tile.team().core() != null){
                    tile.team().core().items.set(nextItems);
                }

                nextItems = null;
            }

            Events.fire(new BlockBuildEndEvent(tile, builder, tile.team(), false, null));
        }
    }

    @Override
    public void beforePlaceBegan(Tile tile, Block previous){
        if(tile.build instanceof CoreBuild){
            //right before placing, create a "destination" item array which is all the previous items minus core requirements
            ItemModule items = tile.build.items.copy();
            if(!state.rules.infiniteResources){
                items.remove(ItemStack.mult(requirements, state.rules.buildCostMultiplier));
            }

            nextItems = items;
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        if(world.tile(x, y) == null) return;

        if(!canPlaceOn(world.tile(x, y), player.team(), rotation)){

            drawPlaceText(Core.bundle.get(
                isFirstTier ?
                    //TODO better message
                    "bar.corefloor" :
                    (player.team().core() != null && player.team().core().items.has(requirements, state.rules.buildCostMultiplier)) || state.rules.infiniteResources ?
                    "bar.corereq" :
                    "bar.noresources"
            ), x, y, valid);
        }
    }

    public void drawLanding(CoreBuild build, float x, float y){
        float fin = renderer.getLandTimeIn();
        float fout = 1f - fin;

        float scl = Scl.scl(4f) / renderer.getDisplayScale();
        float shake = 0f;
        float s = region.width * region.scl() * scl * 3.6f * Interp.pow2Out.apply(fout);
        float rotation = Interp.pow2In.apply(fout) * 135f;
        x += Mathf.range(shake);
        y += Mathf.range(shake);
        float thrustOpen = 0.25f;
        float thrusterFrame = fin >= thrustOpen ? 1f : fin / thrustOpen;
        float thrusterSize = Mathf.sample(thrusterSizes, fin);

        //when launching, thrusters stay out the entire time.
        if(renderer.isLaunching()){
            Interp i = Interp.pow2Out;
            thrusterFrame = i.apply(Mathf.clamp(fout*13f));
            thrusterSize = i.apply(Mathf.clamp(fout*9f));
        }

        Draw.color(Pal.lightTrail);
        //TODO spikier heat
        Draw.rect("circle-shadow", x, y, s, s);

        Draw.scl(scl);

        //draw thruster flame
        float strength = (1f + (size - 3)/2.5f) * scl * thrusterSize * (0.95f + Mathf.absin(2f, 0.1f));
        float offset = (size - 3) * 3f * scl;

        for(int i = 0; i < 4; i++){
            Tmp.v1.trns(i * 90 + rotation, 1f);

            Tmp.v1.setLength((size * tilesize/2f + 1f)*scl + strength*2f + offset);
            Draw.color(build.team.color);
            Fill.circle(Tmp.v1.x + x, Tmp.v1.y + y, 6f * strength);

            Tmp.v1.setLength((size * tilesize/2f + 1f)*scl + strength*0.5f + offset);
            Draw.color(Color.white);
            Fill.circle(Tmp.v1.x + x, Tmp.v1.y + y, 3.5f * strength);
        }

        drawLandingThrusters(x, y, rotation, thrusterFrame);

        Drawf.spinSprite(region, x, y, rotation);

        Draw.alpha(Interp.pow4In.apply(thrusterFrame));
        drawLandingThrusters(x, y, rotation, thrusterFrame);
        Draw.alpha(1f);

        if(teamRegions[build.team.id] == teamRegion) Draw.color(build.team.color);

        Drawf.spinSprite(teamRegions[build.team.id], x, y, rotation);

        Draw.color();
        Draw.scl();
        Draw.reset();
    }

    protected void drawLandingThrusters(float x, float y, float rotation, float frame){
        float length = thrusterLength * (frame - 1f) - 1f/4f;
        float alpha = Draw.getColor().a;

        //two passes for consistent lighting
        for(int j = 0; j < 2; j++){
            for(int i = 0; i < 4; i++){
                var reg = i >= 2 ? thruster2 : thruster1;
                float rot = (i * 90) + rotation % 90f;
                Tmp.v1.trns(rot, length * Draw.xscl);

                //second pass applies extra layer of shading
                if(j == 1){
                    Tmp.v1.rotate(-90f);
                    Draw.alpha((rotation % 90f) / 90f * alpha);
                    rot -= 90f;
                    Draw.rect(reg, x + Tmp.v1.x, y + Tmp.v1.y, rot);
                }else{
                    Draw.alpha(alpha);
                    Draw.rect(reg, x + Tmp.v1.x, y + Tmp.v1.y, rot);
                }
            }
        }
        Draw.alpha(1f);
    }

    public class CoreBuild extends Building{
        public int storageCapacity;
        public boolean noEffect = false;
        public Team lastDamage = Team.derelict;
        public float iframes = -1f;
        public float thrusterTime = 0f;

        protected float cloudSeed;

        //utility methods for less Block-to-CoreBlock casts. also allows for more customization
        public float landDuration(){
            return landDuration;
        }

        public Music landMusic(){
            return landMusic;
        }

        public Music launchMusic(){
            return launchMusic;
        }

        @Override
        public void draw(){
            //draw thrusters when just landed
            if(thrusterTime > 0){
                float frame = thrusterTime;

                Draw.alpha(1f);
                drawThrusters(frame);
                Draw.rect(block.region, x, y);
                Draw.alpha(Interp.pow4In.apply(frame));
                drawThrusters(frame);
                Draw.reset();

                drawTeamTop();
            }else{
                super.draw();
            }
        }

        // `launchType` is null if it's landing instead of launching.
        public void beginLaunch(@Nullable CoreBlock launchType){
            cloudSeed = Mathf.random(1f);
            if(launchType != null){
                Fx.coreLaunchConstruct.at(x, y, launchType.size);
            }

            if(!headless){
                // Add fade-in and fade-out foreground when landing or launching.
                if(renderer.isLaunching()){
                    float margin = 30f;

                    Image image = new Image();
                    image.color.a = 0f;
                    image.touchable = Touchable.disabled;
                    image.setFillParent(true);
                    image.actions(Actions.delay((landDuration() - margin) / 60f), Actions.fadeIn(margin / 60f, Interp.pow2In), Actions.delay(6f / 60f), Actions.remove());
                    image.update(() -> {
                        image.toFront();
                        ui.loadfrag.toFront();
                        if(state.isMenu()){
                            image.remove();
                        }
                    });
                    Core.scene.add(image);
                }else{
                    Image image = new Image();
                    image.color.a = 1f;
                    image.touchable = Touchable.disabled;
                    image.setFillParent(true);
                    image.actions(Actions.fadeOut(35f / 60f), Actions.remove());
                    image.update(() -> {
                        image.toFront();
                        ui.loadfrag.toFront();
                        if(state.isMenu()){
                            image.remove();
                        }
                    });
                    Core.scene.add(image);

                    Time.run(landDuration(), () -> {
                        launchEffect.at(this);
                        Effect.shake(5f, 5f, this);
                        thrusterTime = 1f;

                        if(state.isCampaign() && Vars.showSectorLandInfo && (state.rules.sector.preset == null || state.rules.sector.preset.showSectorLandInfo)){
                            ui.announce("[accent]" + state.rules.sector.name() + "\n" +
                                (state.rules.sector.info.resources.any() ? "[lightgray]" + Core.bundle.get("sectors.resources") + "[white] " +
                                    state.rules.sector.info.resources.toString(" ", UnlockableContent::emoji) : ""), 5);
                        }
                    });
                }
            }
        }

        public void endLaunch(){}

        public void drawLanding(CoreBlock block){
            var clouds = Core.assets.get("sprites/clouds.png", Texture.class);

            float fin = renderer.getLandTimeIn();
            float cameraScl = renderer.getDisplayScale();

            float fout = 1f - fin;
            float scl = Scl.scl(4f) / cameraScl;
            float pfin = Interp.pow3Out.apply(fin), pf = Interp.pow2In.apply(fout);

            //draw particles
            Draw.color(Pal.lightTrail);
            Angles.randLenVectors(1, pfin, 100, 800f * scl * pfin, (ax, ay, ffin, ffout) -> {
                Lines.stroke(scl * ffin * pf * 3f);
                Lines.lineAngle(x + ax, y + ay, Mathf.angle(ax, ay), (ffin * 20 + 1f) * scl);
            });
            Draw.color();

            block.drawLanding(this, x, y);

            Draw.color();
            Draw.mixcol(Color.white, Interp.pow5In.apply(fout));

            //accent tint indicating that the core was just constructed
            if(renderer.isLaunching()){
                float f = Mathf.clamp(1f - fout * 12f);
                if(f > 0.001f){
                    Draw.mixcol(Pal.accent, f);
                }
            }

            //draw clouds
            if(state.rules.cloudColor.a > 0.0001f){
                float scaling = cloudScaling;
                float sscl = Math.max(1f + Mathf.clamp(fin + cfinOffset) * cfinScl, 0f) * cameraScl;

                Tmp.tr1.set(clouds);
                Tmp.tr1.set(
                    (Core.camera.position.x - Core.camera.width/2f * sscl) / scaling,
                    (Core.camera.position.y - Core.camera.height/2f * sscl) / scaling,
                    (Core.camera.position.x + Core.camera.width/2f * sscl) / scaling,
                    (Core.camera.position.y + Core.camera.height/2f * sscl) / scaling);

                Tmp.tr1.scroll(10f * cloudSeed, 10f * cloudSeed);

                Draw.alpha(Mathf.sample(cloudAlphas, fin + calphaFinOffset) * cloudAlpha);
                Draw.mixcol(state.rules.cloudColor, state.rules.cloudColor.a);
                Draw.rect(Tmp.tr1, Core.camera.position.x, Core.camera.position.y, Core.camera.width, Core.camera.height);
                Draw.reset();
            }
        }

        public void drawThrusters(float frame){
            float length = thrusterLength * (frame - 1f) - 1f/4f;
            for(int i = 0; i < 4; i++){
                var reg = i >= 2 ? thruster2 : thruster1;
                float dx = Geometry.d4x[i] * length, dy = Geometry.d4y[i] * length;
                Draw.rect(reg, x + dx, y + dy, i * 90);
            }
        }

        @Override
        public void damage(@Nullable Team source, float damage){
            if(iframes > 0) return;

            if(source != null && source != team){
                lastDamage = source;
            }
            super.damage(source, damage);
        }

        @Override
        public void created(){
            super.created();

            Events.fire(new CoreChangeEvent(this));
        }

        @Override
        public void changeTeam(Team next){
            if(this.team == next) return;

            onRemoved();

            super.changeTeam(next);

            onProximityUpdate();

            Events.fire(new CoreChangeEvent(this));
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.itemCapacity) return storageCapacity;
            return super.sense(sensor);
        }

        @Override
        public boolean canControlSelect(Unit player){
            return player.isPlayer();
        }

        @Override
        public void onControlSelect(Unit unit){
            if(!unit.isPlayer()) return;
            Player player = unit.getPlayer();

            Fx.spawn.at(player);
            if(net.client() && player == Vars.player){
                control.input.controlledType = null;
            }

            player.clearUnit();
            player.deathTimer = Player.deathDelay + 1f;
            requestSpawn(player);
        }

        public void requestSpawn(Player player){
            //do not try to respawn in unsupported environments at all
            if(!unitType.supportsEnv(state.rules.env)) return;

            Call.playerSpawn(tile, player);
        }

        @Override
        public void updateTile(){
            iframes -= Time.delta;
            thrusterTime -= Time.delta/90f;
        }

        /** @return Camera zoom while landing or launching. May optionally do other things such as setting camera position to itself. */
        public float zoomLaunching(){
            Core.camera.position.set(this);
            return landZoomInterp.apply(Scl.scl(landZoomFrom), Scl.scl(landZoomTo), renderer.getLandTimeIn());
        }

        public void updateLaunching(){
            updateLandParticles();
        }

        public void updateLandParticles(){
            float in = renderer.getLandTimeIn() * landDuration();
            float tsize = Mathf.sample(thrusterSizes, (in + 35f) / landDuration());

            renderer.setLandPTimer(renderer.getLandPTimer() + tsize * Time.delta);
            if(renderer.getLandTime() >= 1f){
                tile.getLinkedTiles(t -> {
                    if(Mathf.chance(0.4f)){
                        Fx.coreLandDust.at(t.worldx(), t.worldy(), angleTo(t.worldx(), t.worldy()) + Mathf.range(30f), Tmp.c1.set(t.floor().mapColor).mul(1.5f + Mathf.range(0.15f)));
                    }
                });

                renderer.setLandPTimer(0f);
            }
        }

        @Override
        public boolean canPickup(){
            //cores can never be picked up
            return false;
        }

        @Override
        public void onDestroyed(){
            if(state.rules.coreCapture){
                //just create an explosion, no fire. this prevents immediate recapture
                Damage.dynamicExplosion(x, y, 0, 0, 0, tilesize * block.size / 2f, state.rules.damageExplosions);
                Fx.commandSend.at(x, y, 140f);
            }else{
                super.onDestroyed();
            }

            //add a spawn to the map for future reference - waves should be disabled, so it shouldn't matter
            if(state.isCampaign() && team == state.rules.waveTeam && team.cores().size <= 1 && state.rules.sector.planet.enemyCoreSpawnReplace){
                //do not recache
                tile.setOverlayQuiet(Blocks.spawn);

                if(!spawner.getSpawns().contains(tile)){
                    spawner.getSpawns().add(tile);
                }
            }

            Events.fire(new CoreChangeEvent(this));
        }

        @Override
        public void afterDestroyed(){
            if(state.rules.coreCapture){
                if(!net.client()){
                    tile.setBlock(block, lastDamage);
                }

                //delay so clients don't destroy it afterwards
                Core.app.post(() -> tile.setNet(block, lastDamage, 0));

                //building does not exist on client yet
                if(!net.client()){
                    //core is invincible for several seconds to prevent recapture
                    ((CoreBuild)tile.build).iframes = captureInvicibility;
                }
            }
        }

        @Override
        public void drawLight(){
            Drawf.light(x, y, lightRadius, Pal.accent, 0.65f + Mathf.absin(20f, 0.1f));
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public int getMaximumAccepted(Item item){
            return state.rules.coreIncinerates ? storageCapacity * 20 : storageCapacity;
        }

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            for(Building other : state.teams.cores(team)){
                if(other.tile() != tile){
                    this.items = other.items;
                }
            }
            state.teams.registerCore(this);

            storageCapacity = itemCapacity + proximity().sum(e -> owns(e) ? e.block.itemCapacity : 0);
            proximity.each(this::owns, t -> {
                t.items = items;
                ((StorageBuild)t).linkedCore = this;
            });

            for(Building other : state.teams.cores(team)){
                if(other.tile() == tile) continue;
                storageCapacity += other.block.itemCapacity + other.proximity().sum(e -> owns(other, e) ? e.block.itemCapacity : 0);
            }

            //Team.sharded.core().items.set(Items.surgeAlloy, 12000)
            if(!world.isGenerating()){
                for(Item item : content.items()){
                    items.set(item, Math.min(items.get(item), storageCapacity));
                }
            }

            for(CoreBuild other : state.teams.cores(team)){
                other.storageCapacity = storageCapacity;
            }
        }

        @Override
        public void handleStack(Item item, int amount, Teamc source){
            boolean incinerate = incinerateNonBuildable && !item.buildable;
            int realAmount = incinerate ? 0 : Math.min(amount, storageCapacity - items.get(item));
            super.handleStack(item, realAmount, source);

            if(team == state.rules.defaultTeam && state.isCampaign()){
                if(!incinerate){
                    state.rules.sector.info.handleCoreItem(item, amount);
                }

                if(realAmount == 0 && wasVisible){
                    Fx.coreBurn.at(x, y);
                }
            }
        }

        @Override
        public int removeStack(Item item, int amount){
            int result = super.removeStack(item, amount);

            if(team == state.rules.defaultTeam && state.isCampaign()){
                state.rules.sector.info.handleCoreItem(item, -result);
            }

            return result;
        }

        @Override
        public void drawSelect(){
            //do not draw a pointless single outline when there's no storage
            if(team.cores().size <= 1 && !proximity.contains(storage -> storage.items == items)) return;

            Lines.stroke(1f, Pal.accent);
            Cons<Building> outline = b -> {
                for(int i = 0; i < 4; i++){
                    Point2 p = Geometry.d8edge[i];
                    float offset = -Math.max(b.block.size - 1, 0) / 2f * tilesize;
                    Draw.rect("block-select", b.x + offset * p.x, b.y + offset * p.y, i * 90);
                }
            };
            team.cores().each(core -> {
                outline.get(core);
                core.proximity.each(storage -> storage.items == items, outline);
            });
            Draw.reset();
        }

        public boolean owns(Building tile){
            return owns(this, tile);
        }

        public boolean owns(Building core, Building tile){
            return tile instanceof StorageBuild b && ((StorageBlock)b.block).coreMerge && (b.linkedCore == core || b.linkedCore == null);
        }

        @Override
        public void damage(float amount){
            if(player != null && team == player.team()){
                Events.fire(Trigger.teamCoreDamage);
            }
            super.damage(amount);
        }

        @Override
        public void onRemoved(){
            int total = proximity.count(e -> e.items != null && e.items == items);
            float fract = 1f / total / state.teams.cores(team).size;

            proximity.each(e -> owns(e) && e.items == items && owns(e), t -> {
                StorageBuild ent = (StorageBuild)t;
                ent.linkedCore = null;
                ent.items = new ItemModule();
                for(Item item : content.items()){
                    ent.items.set(item, (int)(fract * items.get(item)));
                }
            });

            state.teams.unregisterCore(this);

            for(CoreBuild other : state.teams.cores(team)){
                other.onProximityUpdate();
            }
        }

        @Override
        public void placed(){
            super.placed();
            state.teams.registerCore(this);
        }

        @Override
        public void itemTaken(Item item){
            if(state.isCampaign() && team == state.rules.defaultTeam){
                //update item taken amount
                state.rules.sector.info.handleCoreItem(item, -1);
            }
        }

        @Override
        public void handleItem(Building source, Item item){
            boolean incinerate = incinerateNonBuildable && !item.buildable;

            if(team == state.rules.defaultTeam){
                state.stats.coreItemCount.increment(item);
            }

            if(net.server() || !net.active()){
                if(team == state.rules.defaultTeam && state.isCampaign() && !incinerate){
                    state.rules.sector.info.handleCoreItem(item, 1);
                }

                if(items.get(item) >= storageCapacity || incinerate){
                    //create item incineration effect at random intervals
                    if(!noEffect){
                        incinerateEffect(this, source);
                    }
                    noEffect = false;
                }else{
                    super.handleItem(source, item);
                }
            }else if(((state.rules.coreIncinerates && items.get(item) >= storageCapacity) || incinerate) && !noEffect){
                //create item incineration effect at random intervals
                incinerateEffect(this, source);
                noEffect = false;
            }
        }
    }
}
