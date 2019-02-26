package io.anuke.mindustry.world;

import io.anuke.arc.collection.Array;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Geometry;
import io.anuke.arc.math.geom.Point2;
import io.anuke.arc.math.geom.Position;
import io.anuke.arc.math.geom.Vector2;
import io.anuke.arc.util.Pack;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.entities.type.TileEntity;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.modules.ConsumeModule;
import io.anuke.mindustry.world.modules.ItemModule;
import io.anuke.mindustry.world.modules.LiquidModule;
import io.anuke.mindustry.world.modules.PowerModule;

import static io.anuke.mindustry.Vars.*;


public class Tile implements Position, TargetTrait{
    /**
     * The coordinates of the core tile this is linked to, in the form of two bytes packed into one.
     * This is relative to the block it is linked to; negate coords to find the link.
     */
    public byte link = 0;
    /** Tile traversal cost. */
    public byte cost = 1;
    /** Tile entity, usually null. */
    public TileEntity entity;
    public short x, y;
    private Block wall;
    private Floor floor;
    /** Rotation, 0-3. Also used to store offload location, in which case it can be any number. */
    private byte rotation;
    /** Team ordinal. */
    private byte team;

    public Tile(int x, int y){
        this.x = (short) x;
        this.y = (short) y;
    }

    public Tile(int x, int y, byte floor, byte wall){
        this(x, y);
        this.floor = (Floor) content.block(floor);
        this.wall = content.block(wall);
        changed();
    }

    public Tile(int x, int y, byte floor, byte wall, byte rotation, byte team){
        this(x, y);
        this.floor = (Floor) content.block(floor);
        this.wall = content.block(wall);
        this.rotation = rotation;
        changed();
        this.team = team;
    }

    /**Returns this tile's position as a {@link Pos}.*/
    public int pos(){
        return Pos.get(x, y);
    }

    public byte getBlockID(){
        return wall.id;
    }

    public byte getFloorID(){
        return floor.id;
    }

    /** Return relative rotation to a coordinate. Returns -1 if the coordinate is not near this tile. */
    public byte relativeTo(int cx, int cy){
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

    @SuppressWarnings("unchecked")
    public <T extends TileEntity> T entity(){
        return (T) entity;
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

    public Floor floor(){
        return floor;
    }

    public Block block(){
        return wall;
    }

    @Override
    public Team getTeam(){
        return Team.all[target().team];
    }

    public void setTeam(Team team){
        this.team = (byte) team.ordinal();
    }

    public byte getTeamID(){
        return team;
    }

    public void setBlock(Block type, int rotation){
        preChanged();
        if(rotation < 0) rotation = (-rotation + 2);
        this.wall = type;
        this.link = 0;
        setRotation((byte) (rotation % 4));
        changed();
    }

    public void setBlock(Block type, Team team){
        preChanged();
        this.wall = type;
        this.team = (byte)team.ordinal();
        this.link = 0;
        changed();
    }

    public void setBlock(Block type){
        preChanged();
        this.wall = type;
        this.link = 0;
        changed();
    }

    public void setFloor(Floor type){
        this.floor = type;
    }

    public byte getRotation(){
        return rotation;
    }

    public void setRotation(byte rotation){
        this.rotation = rotation;
    }

    public byte getDump(){
        return rotation;
    }

    public void setDump(byte dump){
        this.rotation = dump;
    }

    public boolean passable(){
        Block block = block();
        Block floor = floor();
        return isLinked() || !((floor.solid && (block == Blocks.air || block.solidifes)) || (block.solid && (!block.destructible && !block.update)));
    }

    /** Whether this block was placed by a player/unit. */
    public boolean synthetic(){
        Block block = block();
        return block.update || block.destructible;
    }

    public boolean solid(){
        Block block = block();
        Block floor = floor();
        return block.solid || (floor.solid && (block == Blocks.air || block.solidifes)) || block.isSolidFor(this)
        || (isLinked() && getLinked().block().isSolidFor(getLinked()));
    }

    public boolean breakable(){
        Block block = block();
        if(link == 0){
            return (block.destructible || block.breakable || block.update);
        }else{
            return getLinked() != this && getLinked().getLinked() == null && getLinked().breakable();
        }
    }

    public boolean isEnemyCheat(){
        return getTeam() == waveTeam && !state.rules.pvp;
    }

    public boolean isLinked(){
        return link != 0;
    }

    /** Sets this to a linked tile, which sets the block to a part. dx and dy can only be -8-7. */
    public void setLinked(byte dx, byte dy){
        setBlock(Blocks.part);
        link = Pack.byteByte((byte)(dx + 8), (byte)(dy + 8));
    }

    /**
     * Returns the list of all tiles linked to this multiblock, or an empty array if it's not a multiblock.
     * This array contains all linked tiles, including this tile itself.
     */
    public Array<Tile> getLinkedTiles(Array<Tile> tmpArray){
        Block block = block();
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

    /** Returns the block the multiblock is linked to, or null if it is not linked to any block. */
    public Tile getLinked(){
        if(link == 0){
            return null;
        }else{
            byte dx = Pack.leftByte(link);
            byte dy = Pack.rightByte(link);
            return world.tile(x - (dx - 8), y - (dy - 8));
        }
    }

    public void allNearby(Consumer<Tile> cons){
        for(Point2 point : Edges.getEdges(block().size)){
            Tile tile = world.tile(x + point.x, y + point.y);
            if(tile != null){
                cons.accept(tile.target());
            }
        }
    }

    public void allInside(Consumer<Tile> cons){
        for(Point2 point : Edges.getInsideEdges(block().size)){
            Tile tile = world.tile(x + point.x, y + point.y);
            if(tile != null){
                cons.accept(tile);
            }
        }
    }

    public Tile target(){
        Tile link = getLinked();
        return link == null ? this : link;
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

    public boolean interactable(Team team){
        return getTeam() == Team.none || team == getTeam();
    }

    public void updateOcclusion(){
        cost = 1;
        boolean occluded = false;

        //check for occlusion
        for(int i = 0; i < 8; i++){
            Point2 point = Geometry.d8[i];
            Tile tile = world.tile(x + point.x, y + point.y);
            if(tile != null && tile.solid()){
                occluded = true;
                break;
            }
        }

        if(occluded){
            cost += 2;
        }

        if(target().synthetic()){
            cost += Mathf.clamp(target().block().health / 10f, 0, 20);
        }

        if(floor.isLiquid){
            cost += 10;
        }
    }

    private void preChanged(){
        block().removed(this);
        if(entity != null){
            entity.removeFromProximity();
        }
        team = 0;
    }

    private void changed(){
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
                Tile tile = world.tile(x + p.x, y + p.y);
                if(tile != null){
                    tile = tile.target();
                    tile.block().onProximityUpdate(tile);
                }
            }
        }

        updateOcclusion();

        world.notifyChanged(this);
    }

    @Override
    public boolean isDead(){
        return false; //tiles never die
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
        Block block = block();
        Block floor = floor();

        return floor.name + ":" + block.name + "[" + x + "," + y + "] " + "entity=" + (entity == null ? "null" : (entity.getClass())) +
        (link != 0 ? " link=[" + (Pack.leftByte(link) - 8) + ", " + (Pack.rightByte(link) - 8) + "]" : "");
    }
}