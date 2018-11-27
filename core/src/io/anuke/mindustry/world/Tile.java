package io.anuke.mindustry.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.traits.TargetTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.world.blocks.BlockPart;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.modules.ConsumeModule;
import io.anuke.mindustry.world.modules.ItemModule;
import io.anuke.mindustry.world.modules.LiquidModule;
import io.anuke.mindustry.world.modules.PowerModule;
import io.anuke.ucore.entities.trait.PosTrait;
import io.anuke.ucore.function.Consumer;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Geometry;

import static io.anuke.mindustry.Vars.*;


public class Tile implements PosTrait, TargetTrait{
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
    /** Position of cliffs around the tile, packed into bits 0-8. */
    private byte cliffs;
    private Block wall;
    private Floor floor;
    /** Rotation, 0-3. Also used to store offload location, in which case it can be any number. */
    private byte rotation;
    /** Team ordinal. */
    private byte team;
    /** Tile elevation. -1 means slope.*/
    private byte elevation;
    /** Fog visibility status: 3 states, but saved as a single bit. 0 = unexplored, 1 = visited, 2 = currently visible (saved as 1)*/
    private byte visibility;

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

    public Tile(int x, int y, byte floor, byte wall, byte rotation, byte team, byte elevation){
        this(x, y);
        this.floor = (Floor) content.block(floor);
        this.wall = content.block(wall);
        this.rotation = rotation;
        this.setElevation(elevation);
        changed();
        this.team = team;
    }

    public boolean discovered(){
        return visibility > 0;
    }

    public int packedPosition(){
        return x + y * world.width();
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

    public <T extends TileEntity> T entity(){
        return (T) entity;
    }

    public int id(){
        return x + y * world.width();
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

    public byte getVisibility(){
        return visibility;
    }

    public void setVisibility(byte visibility){
        this.visibility = visibility;
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

    public byte getElevation(){
        return elevation;
    }

    public void setElevation(int elevation){
        this.elevation = (byte)elevation;
    }

    public byte getCliffs(){
        return cliffs;
    }

    public void setCliffs(byte cliffs){
        this.cliffs = cliffs;
    }

    public boolean hasCliffs(){
        return getCliffs() != 0;
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
        return block.solid || getCliffs() != 0 || (floor.solid && (block == Blocks.air || block.solidifes)) || block.isSolidFor(this)
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
        return getTeam() == waveTeam && !state.mode.isPvp;
    }

    public boolean isLinked(){
        return link != 0;
    }

    /** Sets this to a linked tile, which sets the block to a blockpart. dx and dy can only be -8-7. */
    public void setLinked(byte dx, byte dy){
        setBlock(Blocks.blockpart);
        link = Bits.packByte((byte) (dx + 8), (byte) (dy + 8));
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
            byte dx = Bits.getLeftByte(link);
            byte dy = Bits.getRightByte(link);
            return world.tile(x - (dx - 8), y - (dy - 8));
        }
    }

    public void allNearby(Consumer<Tile> cons){
        for(GridPoint2 point : Edges.getEdges(block().size)){
            Tile tile = world.tile(x + point.x, y + point.y);
            if(tile != null){
                cons.accept(tile.target());
            }
        }
    }

    public void allInside(Consumer<Tile> cons){
        for(GridPoint2 point : Edges.getInsideEdges(block().size)){
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

    public Tile getNearby(GridPoint2 relative){
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

    public void updateOcclusion(){
        cost = 1;
        cliffs = 0;
        boolean occluded = false;

        //check for occlusion
        for(int i = 0; i < 8; i++){
            GridPoint2 point = Geometry.d8[i];
            Tile tile = world.tile(x + point.x, y + point.y);
            if(tile != null && tile.solid()){
                occluded = true;
                break;
            }
        }

        //check for bitmasking cliffs
        for(int i = 0; i < 4; i++){
            Tile tc = getNearby(i);

            //check for cardinal direction elevation changes and bitmask that
            if(tc != null && ((tc.elevation < elevation && tc.elevation != -1))){
                cliffs |= (1 << (i * 2));
            }
        }

        if(occluded){
            cost += 1;
        }

        if(floor.isLiquid){
            cost += 100f;
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
            entity.cons = new ConsumeModule();
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
            for(GridPoint2 p : Geometry.d4){
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
    public Vector2 getVelocity(){
        return Vector2.Zero;
    }

    @Override
    public float getX(){
        return drawx();
    }

    @Override
    public void setX(float x){
    }

    @Override
    public float getY(){
        return drawy();
    }

    @Override
    public void setY(float y){
    }

    @Override
    public String toString(){
        Block block = block();
        Block floor = floor();

        return floor.name() + ":" + block.name() + "[" + x + "," + y + "] " + "entity=" + (entity == null ? "null" : (entity.getClass())) +
        (link != 0 ? " link=[" + (Bits.getLeftByte(link) - 8) + ", " + (Bits.getRightByte(link) - 8) + "]" : "");
    }
}