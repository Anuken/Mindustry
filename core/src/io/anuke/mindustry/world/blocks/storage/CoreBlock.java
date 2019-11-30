package io.anuke.mindustry.world.blocks.storage;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.func.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.meta.*;
import io.anuke.mindustry.world.modules.*;

import static io.anuke.mindustry.Vars.*;

public class CoreBlock extends StorageBlock{
    protected Mech mech = Mechs.starter;

    public CoreBlock(String name){
        super(name);

        solid = true;
        update = true;
        hasItems = true;
        flags = EnumSet.of(BlockFlag.core, BlockFlag.producer);
        activeSound = Sounds.respawning;
        activeSoundVolume = 1f;
        layer = Layer.lights;
        entityType = CoreEntity::new;
        configurable = true;
        rotate = true;
        hasShadow = false;
    }

    @Remote(called = Loc.server)
    public static void onUnitRespawn(Tile tile, Player player){
        if(player == null || tile.entity == null) return;

        CoreEntity entity = tile.entity();
        Effects.effect(Fx.spawn, entity);
        entity.progress = 0;
        entity.spawnPlayer = player;
        entity.spawnPlayer.onRespawn(tile);
        entity.spawnPlayer.applyImpulse(0, 8f);
        entity.spawnPlayer = null;
    }

    @Override
    public void setStats(){
        super.setStats();

        bars.add("capacity", e ->
            new Bar(
                () -> Core.bundle.format("bar.capacity", ui.formatAmount(((CoreEntity)e).storageCapacity)),
                () -> Pal.items,
                () -> e.items.total() / (float)(((CoreEntity)e).storageCapacity * content.items().count(i -> i.type == ItemType.material))
            ));
    }

    @Override
    public void drawLight(Tile tile){
        draw(tile);
        renderer.lights.add(tile.drawx(), tile.drawy(), 30f * size, Pal.accent, 0.5f + Mathf.absin(20f, 0.1f));
    }

    @Override
    public boolean acceptItem(Item item, Tile tile, Tile source){
        return tile.entity.items.get(item) < getMaximumAccepted(tile, item);
    }

    @Override
    public int getMaximumAccepted(Tile tile, Item item){
        CoreEntity entity = tile.entity();
        return item.type == ItemType.material ? entity.storageCapacity : 0;
    }

    @Override
    public void onProximityUpdate(Tile tile){
        CoreEntity entity = tile.entity();

        for(Tile other : state.teams.get(tile.getTeam()).cores){
            if(other != tile){
                entity.items = other.entity.items;
            }
        }
        state.teams.get(tile.getTeam()).cores.add(tile);

        entity.storageCapacity = itemCapacity + entity.proximity().sum(e -> isContainer(e) ? e.block().itemCapacity : 0);
        entity.proximity().each(this::isContainer, t -> {
            t.entity.items = entity.items;
            t.<StorageBlockEntity>entity().linkedCore = tile;
        });

        for(Tile other : state.teams.get(tile.getTeam()).cores){
            if(other == tile) continue;
            entity.storageCapacity += other.block().itemCapacity + other.entity.proximity().sum(e -> isContainer(e) ? e.block().itemCapacity : 0);
        }

        if(!world.isGenerating()){
            for(Item item : content.items()){
                entity.items.set(item, Math.min(entity.items.get(item), entity.storageCapacity));
            }
        }

        for(Tile other : state.teams.get(tile.getTeam()).cores){
            CoreEntity oe = other.entity();
            oe.storageCapacity = entity.storageCapacity;
        }
    }

    @Override
    public void drawSelect(Tile tile){
        Lines.stroke(1f, Pal.accent);
        Cons<Tile> outline = t -> {
            for(int i = 0; i < 4; i++){
                Point2 p = Geometry.d8edge[i];
                float offset = -Math.max(t.block().size - 1, 0) / 2f * tilesize;
                Draw.rect("block-select", t.drawx() + offset * p.x, t.drawy() + offset * p.y, i * 90);
            }
        };
        if(tile.entity.proximity().contains(e -> isContainer(e) && e.entity.items == tile.entity.items)){
            outline.get(tile);
        }
        tile.entity.proximity().each(e -> isContainer(e) && e.entity.items == tile.entity.items, outline);
        Draw.reset();
    }


    public boolean isContainer(Tile tile){
        return tile.entity instanceof StorageBlockEntity;
    }

    @Override
    public float handleDamage(Tile tile, float amount){
        if(player != null && tile.getTeam() == player.getTeam()){
            Events.fire(Trigger.teamCoreDamage);
        }
        return amount;
    }

    @Override
    public boolean canBreak(Tile tile){
        return state.teams.get(tile.getTeam()).cores.size > 1;
    }

    @Override
    public void removed(Tile tile){
        int total = tile.entity.proximity().count(e -> e.entity.items == tile.entity.items);
        float fract = 1f / total / state.teams.get(tile.getTeam()).cores.size;

        tile.entity.proximity().each(e -> isContainer(e) && e.entity.items == tile.entity.items, t -> {
            StorageBlockEntity ent = (StorageBlockEntity)t.entity;
            ent.linkedCore = null;
            ent.items = new ItemModule();
            for(Item item : content.items()){
                ent.items.set(item, (int)(fract * tile.entity.items.get(item)));
            }
        });

        state.teams.get(tile.getTeam()).cores.remove(tile);

        int max = itemCapacity * state.teams.get(tile.getTeam()).cores.size;
        for(Item item : content.items()){
            tile.entity.items.set(item, Math.min(tile.entity.items.get(item), max));
        }

        for(Tile other : state.teams.get(tile.getTeam()).cores){
            other.block().onProximityUpdate(other);
        }
    }

    @Override
    public void placed(Tile tile){
        super.placed(tile);
        state.teams.get(tile.getTeam()).cores.add(tile);
    }

    @Override
    public void drawLayer(Tile tile){
        CoreEntity entity = tile.entity();

        if(entity.heat > 0.001f){
            RespawnBlock.drawRespawn(tile, entity.heat, entity.progress, entity.time, entity.spawnPlayer, mech);
        }
    }

    @Override
    public void handleItem(Item item, Tile tile, Tile source){
        if(net.server() || !net.active()){
            super.handleItem(item, tile, source);
            if(state.rules.tutorial){
                Events.fire(new CoreItemDeliverEvent());
            }
        }
    }

    @Override
    public void update(Tile tile){
        CoreEntity entity = tile.entity();

        if(entity.state == pootis.launching){
            entity.enginePower = Mathf.lerpDelta(entity.enginePower, 1f, 0.03f);
            if(entity.enginePower > 0.9f) entity.state = pootis.rotating;
        }

        if(entity.state == pootis.rotating){
            hasShadow = false;
            Tile destination = world.tile(entity.target);
            float angle = Angles.angle(tile.drawx(), tile.drawy(), destination.drawx(), destination.drawy());
            entity.rotation = Mathf.lerpDelta(entity.rotation, angle, 0.03f);
            if(Math.abs(entity.rotation - angle) < 1) entity.state = pootis.traveling;
        }

        if(entity.state == pootis.traveling){
            entity.traveled = Mathf.clamp(entity.traveled + 0.003f, 0f, 1f);
            if(entity.traveled == 1f) entity.state = pootis.landing;
        }

        if(entity.state == pootis.landing){
            entity.rotation = Mathf.lerpDelta(entity.rotation, 0, 0.03f);
            if(entity.rotation < 1f) entity.state = pootis.touchdown;
        }

        if(entity.state == pootis.touchdown){
            entity.enginePower = Mathf.lerpDelta(entity.enginePower, 0f, 0.03f);
            if(entity.enginePower < 0.1f){
                // entity.state = pootis.landed;

                Tile destination = world.tile(entity.target);
                destination.setBlock(this, tile.getTeam());
                world.removeBlock(tile);
            };
        }

        if(entity.spawnPlayer != null){
            if(!entity.spawnPlayer.isDead() || !entity.spawnPlayer.isAdded()){
                entity.spawnPlayer = null;
                return;
            }

            entity.spawnPlayer.set(tile.drawx(), tile.drawy());
            entity.heat = Mathf.lerpDelta(entity.heat, 1f, 0.1f);
            entity.time += entity.delta();
            entity.progress += 1f / state.rules.respawnTime * entity.delta();

            if(entity.progress >= 1f){
                Call.onUnitRespawn(tile, entity.spawnPlayer);
            }
        }else{
            entity.heat = Mathf.lerpDelta(entity.heat, 0f, 0.1f);
        }
    }

    @Override
    public boolean shouldActiveSound(Tile tile){
        CoreEntity entity = tile.entity();

        return entity.spawnPlayer != null;
    }

    public class CoreEntity extends TileEntity implements SpawnerTrait{
        protected Player spawnPlayer;
        protected float progress;
        protected float time;
        protected float heat;
        protected int storageCapacity;

        public pootis state = pootis.landed;
        public float enginePower;
        public boolean liftoff;
        public int target;
        public float rotation;
        public float traveled;

        @Override
        public boolean hasUnit(Unit unit){
            return unit == spawnPlayer;
        }

        @Override
        public void updateSpawning(Player player){
            if(!netServer.isWaitingForPlayers() && spawnPlayer == null){
                spawnPlayer = player;
                progress = 0f;
                player.mech = mech;
                player.beginRespawning(this);
            }
        }
    }

    public enum pootis{
        landed, launching, rotating, traveling, landing, touchdown,
    }

    @Override
    public void draw(Tile tile){
        CoreEntity entity = tile.entity();
        drawEngines(tile);
        Draw.rect(region, tile.drawx(), tile.drawy(), entity.rotation + 90);
    }

    public void drawEngines(Tile tile){
        CoreEntity entity = tile.entity();
//        float brcx = tile.drawx() + (size * tilesize / 2f) - (tilesize / 2f);
//        float brcy = tile.drawy() - (size * tilesize / 2f) + (tilesize / 2f);

//        float size = 3;
//
//        for(int corner=0; corner < 4; corner++){
//            int rotation = 360 / 4 * corner;
//
//            Draw.color(Color.blue);
//            Fill.circle(x + Angles.trnsx(rotation + 180, mech.engineOffset), y + Angles.trnsy(rotation + 180, mech.engineOffset),
//            size + Mathf.absin(Time.time(), 2f, size / 4f));
//
//            Draw.color(Color.white);
//            Fill.circle(x + Angles.trnsx(rotation + 180, mech.engineOffset - 1f), y + Angles.trnsy(rotation + 180, mech.engineOffset - 1f),
//            (size + Mathf.absin(Time.time(), 2f, size / 4f)) / 2f);
//            Draw.color();
//        }

        if(entity.state == pootis.landed) return;

        Vector2 around = new Vector2(tile.drawx(), tile.drawy());
        for(Vector2 corner : corners(tile)){
//            Drawf.circles(corner.x, corner.y, 1f);

            corner.rotateAround(around, entity.rotation);
            float angle = Angles.angle(tile.drawx(), tile.drawy(), corner.x, corner.y);
            float thrust = 1.25f * size * entity.enginePower;

            Draw.color(Pal.lightTrail);
            Fill.circle(corner.x + Angles.trnsx(angle, 0f), corner.y + Angles.trnsy(angle, 0f),thrust + Mathf.absin(Time.time(), 2f, thrust / 4f));

            Draw.color(Color.white);
            Fill.circle(corner.x + Angles.trnsx(angle, -1f), corner.y + Angles.trnsy(angle, -1f), (thrust + Mathf.absin(Time.time(), 2f, thrust / 4f)) / 2f);
        }
        Draw.color();
    }

    @Override
    public boolean onConfigureTileTapped(Tile tile, Tile other){
        CoreEntity entity = tile.entity();

        if(entity.state == pootis.landed){
            entity.target = other.pos();
            entity.state = pootis.launching;
        }

        return super.onConfigureTileTapped(tile, other);
    }

    public Vector2[] corners(Tile tile){

        Vector2 topRight = new Vector2(
        tile.drawx() + (size * tilesize / 2f),
        tile.drawy() + (size * tilesize / 2f));

        Vector2 bottomRight = new Vector2(
        tile.drawx() + (size * tilesize / 2f),
        tile.drawy() - (size * tilesize / 2f));

        Vector2 bottomLeft = new Vector2(
        tile.drawx() - (size * tilesize / 2f),
        tile.drawy() - (size * tilesize / 2f));

        Vector2 topLeft = new Vector2(
        tile.drawx() - (size * tilesize / 2f),
        tile.drawy() + (size * tilesize / 2f));

        return new Vector2[] {topRight, bottomRight, bottomLeft, topLeft};
    }

    // https://stackoverflow.com/a/33907440/6056864
    public static Vector2 midpoint(float lat1, float long1, float lat2, float long2, float per) {

        Vector2 from = new Vector2(lat1, long1);
        Vector2 to = new Vector2(lat2, long2);
        Vector2 mid = from.interpolate(to, per, Interpolation.pow3);

//        Drawf.circles(mid.x, mid.y, 1f);
        return mid;
    }

}
