package io.anuke.mindustry.world;

import io.anuke.arc.collection.*;
import io.anuke.arc.func.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.entities.traits.*;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.blocks.*;
import io.anuke.mindustry.world.modules.*;

import static io.anuke.mindustry.Vars.*;

public class Tile implements Position, TargetTrait{
    /** Tile traversal cost. */
    public byte cost = 1;
    /** Tile entity, usually null. */
    public TileEntity entity;
    public short x, y;
    protected Block block;
    protected Floor floor;
    protected Floor overlay;
    /** Rotation, 0-3. Also used to store offload location, in which case it can be any number.*/
    protected byte rotation;
    /** Team ordinal. */
    protected byte team;

    public Tile(int x, int y){
        this.x = (short)x;
        this.y = (short)y;
        block = floor = overlay = (Floor)Blocks.air;
    }

    public Tile(int x, int y, int floor, int overlay, int wall){
        this.x = (short)x;
        this.y = (short)y;
        this.floor = (Floor)content.block(floor);
        this.overlay = (Floor)content.block(overlay);
        this.block = content.block(wall);

        //update entity and create it if needed
        changed();
    }

    /** Returns this tile's position as a {@link Pos}. */
    public int pos(){
        return Pos.get(x, y);
    }

    public byte relativeTo(Tile tile){
        return relativeTo(tile.x, tile.y);
    }

    /** Return relative rotation to a coordinate. Returns -1 if the coordinate is not near this tile. */
    public byte relativeTo(int cx, int cy){
        if(x == cx && y == cy - 1) return 1;
        if(x == cx && y == cy + 1) return 3;
        if(x == cx - 1 && y == cy) return 0;
        if(x == cx + 1 && y == cy) return 2;
        return -1;
    }

    public static byte relativeTo(int x, int y, int cx, int cy){
        if(x == cx && y == cy - 1) return 1;
        if(x == cx && y == cy + 1) return 3;
        if(x == cx - 1 && y == cy) return 0;
        if(x == cx + 1 && y == cy) return 2;
        return -1;
    }

    public byte absoluteRelativeTo(int cx, int cy){
        if(x == cx && y <= cy - 1) return 1;
        if(x == cx && y >= cy + 1) return 3;
        if(x <= cx - 1 && y == cy) return 0;
        if(x >= cx + 1 && y == cy) return 2;
        return -1;
    }

    public static byte absoluteRelativeTo(int x, int y, int cx, int cy){
        if(x == cx && y <= cy - 1) return 1;
        if(x == cx && y >= cy + 1) return 3;
        if(x <= cx - 1 && y == cy) return 0;
        if(x >= cx + 1 && y == cy) return 2;
        return -1;
    }

    /** Configure a tile with the current, local player. */
    public void configure(int value){
        Call.onTileConfig(player, this, value);
    }

    public void configureAny(int value){
        Call.onTileConfig(null, this, value);
    }

    @SuppressWarnings("unchecked")
    public <T extends TileEntity> T entity(){
        return (T)entity;
    }

    public float worldx(){
        return x * tilesize;
    }

    public float worldy(){
        return y * tilesize;
    }

    public float drawx(){
        return block().offset() + worldx();
    }

    public float drawy(){
        return block().offset() + worldy();
    }

    public boolean isDarkened(){
        return block().solid && !block().synthetic() && block().fillsTile;
    }

    public @NonNull Floor floor(){
        return floor;
    }

    public @NonNull Block block(){
        return block;
    }

    public @NonNull Floor overlay(){
        return overlay;
    }

    @SuppressWarnings("unchecked")
    public <T extends Block> T cblock(){
        return (T)block;
    }

    @Override
    public Team getTeam(){
        return Team.all[link().team];
    }

    public void setTeam(Team team){
        this.team = (byte)team.ordinal();
    }

    public byte getTeamID(){
        return team;
    }

    public void setBlock(@NonNull Block type, Team team, int rotation){
        preChanged();
        this.block = type;
        this.team = (byte)team.ordinal();
        this.rotation = (byte)Mathf.mod(rotation, 4);
        changed();
    }

    public void setBlock(@NonNull Block type, Team team){
        setBlock(type, team, 0);
    }

    public void setBlock(@NonNull Block type){
        if(type == null) throw new IllegalArgumentException("Block cannot be null.");
        preChanged();
        this.block = type;
        this.rotation = 0;
        changed();
    }

    /**This resets the overlay!*/
    public void setFloor(@NonNull Floor type){
        this.floor = type;
        this.overlay = (Floor)Blocks.air;
    }

    /** Sets the floor, preserving overlay.*/
    public void setFloorUnder(@NonNull Floor floor){
        Block overlay = this.overlay;
        setFloor(floor);
        setOverlay(overlay);
    }

    public byte rotation(){
        return rotation;
    }

    public void rotation(int rotation){
        this.rotation = (byte)rotation;
    }

    public short overlayID(){
        return overlay.id;
    }

    public short blockID(){
        return block.id;
    }

    public short floorID(){
        return floor.id;
    }

    public void setOverlayID(short ore){
        this.overlay = (Floor)content.block(ore);
    }

    public void setOverlay(Block block){
        this.overlay = (Floor)block;
    }

    public void clearOverlay(){
        setOverlayID((short)0);
    }

    public boolean passable(){
        return isLinked() || !((floor.solid && (block == Blocks.air || block.solidifes)) || (block.solid && (!block.destructible && !block.update)));
    }

    /** Whether this block was placed by a player/unit. */
    public boolean synthetic(){
        return block.update || block.destructible;
    }

    public boolean solid(){
        return block.solid || block.isSolidFor(this) || (isLinked() && link().solid());
    }

    public boolean breakable(){
        return !isLinked() ? (block.destructible || block.breakable || block.update) : link().breakable();
    }

    public Tile link(){
        return block.linked(this);
    }

    public boolean isEnemyCheat(){
        return getTeam() == waveTeam && state.rules.enemyCheat;
    }

    public boolean isLinked(){
        return block instanceof BlockPart;
    }

    /**
     * Returns the list of all tiles linked to this multiblock, or an empty array if it's not a multiblock.
     * This array contains all linked tiles, including this tile itself.
     */
    public void getLinkedTiles(Cons<Tile> cons){
        if(block.isMultiblock()){
            int size = block.size;
            int offsetx = -(size - 1) / 2;
            int offsety = -(size - 1) / 2;
            for(int dx = 0; dx < size; dx++){
                for(int dy = 0; dy < size; dy++){
                    Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
                    if(other != null) cons.get(other);
                }
            }
        }else{
            cons.get(this);
        }
    }

    /**
     * Returns the list of all tiles linked to this multiblock, or an empty array if it's not a multiblock.
     * This array contains all linked tiles, including this tile itself.
     */
    public Array<Tile> getLinkedTiles(Array<Tile> tmpArray){
        tmpArray.clear();
        getLinkedTiles(tmpArray::add);
        return tmpArray;
    }

    /**
     * Returns the list of all tiles linked to this multiblock if it were this block, or an empty array if it's not a multiblock.
     * This array contains all linked tiles, including this tile itself.
     */
    public Array<Tile> getLinkedTilesAs(Block block, Array<Tile> tmpArray){
        tmpArray.clear();
        if(block.isMultiblock()){
            int offsetx = -(block.size - 1) / 2;
            int offsety = -(block.size - 1) / 2;
            for(int dx = 0; dx < block.size; dx++){
                for(int dy = 0; dy < block.size; dy++){
                    Tile other = world.tile(x + dx + offsetx, y + dy + offsety);
                    if(other != null) tmpArray.add(other);
                }
            }
        }else{
            tmpArray.add(this);
        }
        return tmpArray;
    }

    public Rectangle getHitbox(Rectangle rect){
        return rect.setSize(block().size * tilesize).setCenter(drawx(), drawy());
    }

    public Tile getNearby(Point2 relative){
        return world.tile(x + relative.x, y + relative.y);
    }

    public Tile getNearby(int dx, int dy){
        return world.tile(x + dx, y + dy);
    }

    public Tile getNearby(int rotation){
        if(rotation == 0) return world.tile(x + 1, y);
        if(rotation == 1) return world.tile(x, y + 1);
        if(rotation == 2) return world.tile(x - 1, y);
        if(rotation == 3) return world.tile(x, y - 1);
        return null;
    }

    public Tile getNearbyLink(int rotation){
        if(rotation == 0) return world.ltile(x + 1, y);
        if(rotation == 1) return world.ltile(x, y + 1);
        if(rotation == 2) return world.ltile(x - 1, y);
        if(rotation == 3) return world.ltile(x, y - 1);
        return null;
    }

    // ▲ ▲ ▼ ▼ ◀ ▶ ◀ ▶ B A
    public Tile front(){
        return getNearbyLink((rotation + 4) % 4);
    }

    public Tile right(){
        return getNearbyLink((rotation + 3) % 4);
    }

    public Tile back(){
        return getNearbyLink((rotation + 2) % 4);
    }

    public Tile left(){
        return getNearbyLink((rotation + 1) % 4);
    }

    public boolean interactable(Team team){
        return getTeam() == Team.derelict || team == getTeam();
    }

    public Item drop(){
        return overlay == Blocks.air || overlay.itemDrop == null ? floor.itemDrop : overlay.itemDrop;
    }

    public void updateOcclusion(){
        cost = 1;
        boolean occluded = false;

        //check for occlusion
        for(int i = 0; i < 8; i++){
            Point2 point = Geometry.d8[i];
            Tile tile = world.tile(x + point.x, y + point.y);
            if(tile != null && tile.floor.isLiquid){
                cost += 4;
            }
            if(tile != null && tile.solid()){
                occluded = true;
                break;
            }
        }

        //+24

        if(occluded){
            cost += 2;
        }

        //+26

        if(link().synthetic() && link().solid()){
            cost += Mathf.clamp(link().block.health / 10f, 0, 20);
        }

        //+46

        if(floor.isLiquid){
            cost += 10;
        }

        //+56

        if(floor.drownTime > 0){
            cost += 70;
        }

        //+126

        if(cost < 0){
            cost = Byte.MAX_VALUE;
        }
    }

    protected void preChanged(){
        block().removed(this);
        if(entity != null){
            entity.removeFromProximity();
        }
        team = 0;
    }

    protected void changed(){
        if(entity != null){
            entity.remove();
            entity = null;
        }

        Block block = block();

        if(block.hasEntity()){
            entity = block.newEntity().init(this, block.update);
            entity.cons = new ConsumeModule(entity);
            if(block.hasItems) entity.items = new ItemModule();
            if(block.hasLiquids) entity.liquids = new LiquidModule();
            if(block.hasPower){
                entity.power = new PowerModule();
                entity.power.graph.add(this);
            }

            if(!world.isGenerating()){
                entity.updateProximity();
            }
        }else if(!(block instanceof BlockPart) && !world.isGenerating()){
            //since the entity won't update proximity for us, update proximity for all nearby tiles manually
            for(Point2 p : Geometry.d4){
                Tile tile = world.ltile(x + p.x, y + p.y);
                if(tile != null){
                    tile.block().onProximityUpdate(tile);
                }
            }
        }

        updateOcclusion();

        world.notifyChanged(this);
    }

    @Override
    public boolean isDead(){
        return entity == null;
    }

    @Override
    public Vector2 velocity(){
        return Vector2.ZERO;
    }

    @Override
    public float getX(){
        return drawx();
    }

    @Override
    public void setX(float x){
        throw new IllegalArgumentException("Tile position cannot change.");
    }

    @Override
    public float getY(){
        return drawy();
    }

    @Override
    public void setY(float y){
        throw new IllegalArgumentException("Tile position cannot change.");
    }

    @Override
    public String toString(){
        return floor.name + ":" + block.name + ":" + overlay + "[" + x + "," + y + "] " + "entity=" + (entity == null ? "null" : (entity.getClass())) + ":" + getTeam();
    }
}
