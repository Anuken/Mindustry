package mindustry.world.blocks.storage;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
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
    //hacky way to pass item modules between methods
    private static ItemModule nextItems;
    protected static final float[] thrusterSizes = {0f, 0f, 0f, 0f, 0.3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 0f};

    public @Load(value = "@-thruster1", fallback = "clear-effect") TextureRegion thruster1; //top right
    public @Load(value = "@-thruster2", fallback = "clear-effect") TextureRegion thruster2; //bot left
    public float thrusterLength = 14f/4f;
    public boolean isFirstTier;
    public boolean incinerateNonBuildable = false;

    public UnitType unitType = UnitTypes.alpha;

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
        //TODO should AI ever rebuild this?
        //rebuildable = false;
    }

    @Remote(called = Loc.server)
    public static void playerSpawn(Tile tile, Player player){
        if(player == null || tile == null || !(tile.build instanceof CoreBuild entity)) return;

        CoreBlock block = (CoreBlock)tile.block();
        if(entity.wasVisible){
            Fx.spawn.at(entity);
        }

        player.set(entity);

        if(!net.client()){
            Unit unit = block.unitType.create(tile.team());
            unit.set(entity);
            unit.rotation(90f);
            unit.impulse(0f, 3f);
            unit.controller(player);
            unit.spawnedByCore(true);
            unit.add();
        }

        if(state.isCampaign() && player == Vars.player){
            block.unitType.unlock();
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.buildTime);
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

        return tile.block() instanceof CoreBlock && size > tile.block().size;
    }

    @Override
    public void placeBegan(Tile tile, Block previous){
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
        float fout = renderer.getLandTime() / coreLandDuration;

        if(renderer.isLaunching()) fout = 1f - fout;

        float fin = 1f - fout;

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
            super.changeTeam(next);

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

        public void updateLandParticles(){
            float time = renderer.isLaunching() ? coreLandDuration - renderer.getLandTime() : renderer.getLandTime();
            float tsize = Mathf.sample(thrusterSizes, (time + 35f) / coreLandDuration);

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

            int max = itemCapacity * state.teams.cores(team).size;
            for(Item item : content.items()){
                items.set(item, Math.min(items.get(item), max));
            }

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
