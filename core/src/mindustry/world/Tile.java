package mindustry.world;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.modules.*;

import static mindustry.Vars.*;

public class Tile implements Position, QuadTreeObject{
    static final ObjectSet<Tilec> tileSet = new ObjectSet<>();

    /** Tile traversal cost. */
    public byte cost = 1;
    /** Tile entity, usually null. */
    public @Nullable Tilec entity;
    public short x, y;
    protected @NonNull Block block;
    protected @NonNull Floor floor;
    protected @NonNull Floor overlay;
    /** Rotation, 0-3. Also used to store offload location, in which case it can be any number.*/
    protected byte rotation;
    protected boolean changing = false;

    public Tile(int x, int y){
        this.x = (short)x;
        this.y = (short)y;
        block = floor = overlay = (Floor)Blocks.air;
    }

    public Tile(int x, int y, Block floor, Block overlay, Block wall){
        this.x = (short)x;
        this.y = (short)y;
        this.floor = (Floor)floor;
        this.overlay = (Floor)overlay;
        this.block = wall;

        //update entity and create it if needed
        changeEntity(Team.derelict);
        changed();
    }

    public Tile(int x, int y, int floor, int overlay, int wall){
        this(x, y, content.block(floor), content.block(overlay), content.block(wall));
    }

    /** Returns this tile's position as a packed point. */
    public int pos(){
        return Point2.pack(x, y);
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

    @SuppressWarnings("unchecked")
    public <T extends TileEntity> T ent(){
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
        return block.solid && !block.synthetic() && block.fillsTile;
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

    public Team team(){
        return entity == null ? Team.derelict : entity.team();
    }

    public void setTeam(Team team){
        if(entity != null){
            entity.team(team);
        }
    }

    public boolean isCenter(){
        return entity == null || entity.tile() == this;
    }

    public byte getTeamID(){
        return team().id;
    }

    public void setBlock(@NonNull Block type, Team team, int rotation){
        changing = true;

        this.block = type;
        this.rotation = rotation == 0 ? 0 : (byte)Mathf.mod(rotation, 4);
        preChanged();
        changeEntity(team);

        if(entity != null){
            entity.team(team);
        }

        //set up multiblock
        if(block.isMultiblock()){
            int offsetx = -(block.size - 1) / 2;
            int offsety = -(block.size - 1) / 2;
            Tilec entity = this.entity;
            Block block = this.block;

            //two passes: first one clears, second one sets
            for(int pass = 0; pass < 2; pass++){
                for(int dx = 0; dx < block.size; dx++){
                    for(int dy = 0; dy < block.size; dy++){
                        int worldx = dx + offsetx + x;
                        int worldy = dy + offsety + y;
                        if(!(worldx == x && worldy == y)){
                            Tile other = world.tile(worldx, worldy);

                            if(other != null){
                                if(pass == 0){
                                    //first pass: delete existing blocks - this should automatically trigger removal if overlap exists
                                    other.setBlock(Blocks.air);
                                }else{
                                    //second pass: assign changed data
                                    //assign entity and type to blocks, so they act as proxies for this one
                                    other.entity = entity;
                                    other.block = block;
                                }
                            }
                        }
                    }
                }
            }

            this.entity = entity;
            this.block = block;
        }

        changed();
        changing = false;
    }

    public void setBlock(@NonNull Block type, Team team){
        setBlock(type, team, 0);
    }

    public void setBlock(@NonNull Block type){
        setBlock(type, Team.derelict, 0);
    }

    /** This resets the overlay! */
    public void setFloor(@NonNull Floor type){
        this.floor = type;
        this.overlay = (Floor)Blocks.air;

        recache();
        if(entity != null){
            entity.onProximityUpdate();
        }
    }

    /** Sets the floor, preserving overlay.*/
    public void setFloorUnder(@NonNull Floor floor){
        Block overlay = this.overlay;
        setFloor(floor);
        setOverlay(overlay);
    }

    public void recache(){
        if(!headless && !world.isGenerating()){
            renderer.blocks.floor.recacheTile(this);
            renderer.minimap.update(this);
            for(int i = 0; i < 8; i++){
                Tile other = world.tile(x + Geometry.d8[i].x, y + Geometry.d8[i].y);
                if(other != null){
                    renderer.blocks.floor.recacheTile(other);
                }
            }
        }
    }

    public void remove(){
        //this automatically removes multiblock references to this block
        setBlock(Blocks.air);
    }

    /** remove()-s this tile, except it's synced across the network */
    public void removeNet(){
        Call.removeTile(this);
    }

    /** set()-s this tile, except it's synced across the network */
    public void setNet(Block block, Team team, int rotation){
        Call.setTile(this, block, team, rotation);
    }

    /** set()-s this tile, except it's synced across the network */
    public void setFloorNet(Block floor, Block overlay){
        Call.setFloor(this, floor, overlay);
    }

    /** set()-s this tile, except it's synced across the network */
    public void setFloorNet(Block floor){
        setFloorNet(floor, Blocks.air);
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
        setOverlay(content.block(ore));
    }

    public void setOverlay(Block block){
        this.overlay = (Floor)block;

        recache();
    }

    public void clearOverlay(){
        setOverlayID((short)0);
    }

    public boolean passable(){
        return !((floor.solid && (block == Blocks.air || block.solidifes)) || (block.solid && (!block.destructible && !block.update)));
    }

    /** Whether this block was placed by a player/unit. */
    public boolean synthetic(){
        return block.update || block.destructible;
    }

    public boolean solid(){
        return block.solid || (entity != null && entity.checkSolid());
    }

    public boolean breakable(){
        return block.destructible || block.breakable || block.update;
    }

    public boolean isEnemyCheat(){
        return team() == state.rules.waveTeam && state.rules.enemyCheat;
    }

    /**
     * Returns the list of all tiles linked to this multiblock, or just itself if it's not a multiblock.
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

    public Rect getHitbox(Rect rect){
        return rect.setCentered(drawx(), drawy(), block.size * tilesize, block.size * tilesize);
    }

    @Override
    public void hitbox(Rect rect){
        getHitbox(rect);
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

    public Tilec getNearbyEntity(int rotation){
        if(rotation == 0) return world.ent(x + 1, y);
        if(rotation == 1) return world.ent(x, y + 1);
        if(rotation == 2) return world.ent(x - 1, y);
        if(rotation == 3) return world.ent(x, y - 1);
        return null;
    }

    // ▲ ▲ ▼ ▼ ◀ ▶ ◀ ▶ B A
    public @Nullable Tilec front(){
        return getNearbyEntity((rotation + 4) % 4);
    }

    public @Nullable Tilec right(){
        return getNearbyEntity((rotation + 3) % 4);
    }

    public @Nullable Tilec back(){
        return getNearbyEntity((rotation + 2) % 4);
    }

    public @Nullable Tilec left(){
        return getNearbyEntity((rotation + 1) % 4);
    }

    public boolean interactable(Team team){
        return state.teams.canInteract(team, team());
    }

    public @Nullable Item drop(){
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

        if(block.synthetic() && solid()){
            cost += Mathf.clamp(block.health / 10f, 0, 20);
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
        if(entity != null){
            //only call removed() for the center block - this only gets called once.
            entity.onRemoved();
            entity.removeFromProximity();

            //remove this tile's dangling entities
            if(entity.block().isMultiblock()){
                int cx = entity.tileX(), cy = entity.tileY();
                int size = entity.block().size;
                int offsetx = -(size - 1) / 2;
                int offsety = -(size - 1) / 2;
                for(int dx = 0; dx < size; dx++){
                    for(int dy = 0; dy < size; dy++){
                        Tile other = world.tile(cx + dx + offsetx, cy + dy + offsety);
                        if(other != null){
                            //reset entity and block *manually* - thus, preChanged() will not be called anywhere else, for multiblocks
                            if(other != this){ //do not remove own entity so it can be processed in changed()
                                other.entity = null;
                                other.block = Blocks.air;

                                //manually call changed event
                                other.updateOcclusion();
                                world.notifyChanged(other);
                            }
                        }
                    }
                }
            }
        }


        //recache when static blocks get changed
        if(block.isStatic()){
            recache();
        }
    }

    protected void changeEntity(Team team){
        if(entity != null){
            int size = entity.block().size;
            entity.remove();
            entity = null;

            //update edge entities
            tileSet.clear();

            for(Point2 edge : Edges.getEdges(size)){
                Tilec other = world.ent(x + edge.x, y + edge.y);
                if(other != null){
                    tileSet.add(other);
                }
            }

            //update proximity, since multiblock was just removed
            for(Tilec t : tileSet){
                t.updateProximity();
            }
        }

        if(block.hasEntity()){
            entity = block.newEntity().init(this, team, block.update);
            entity.cons(new ConsumeModule(entity));
            if(block.hasItems) entity.items(new ItemModule());
            if(block.hasLiquids) entity.liquids(new LiquidModule());
            if(block.hasPower){
                entity.power(new PowerModule());
                entity.power().graph.add(entity);
            }
        }
    }

    protected void changed(){
        if(!world.isGenerating()){
            if(entity != null){
                entity.updateProximity();
            }else{
                //since the entity won't update proximity for us, update proximity for all nearby tiles manually
                for(Point2 p : Geometry.d4){
                    Tilec tile = world.ent(x + p.x, y + p.y);
                    if(tile != null && !tile.tile().changing){
                        tile.onProximityUpdate();
                    }
                }
            }
        }

        updateOcclusion();

        world.notifyChanged(this);

        //recache when static block is added
        if(block.isStatic()){
            recache();
        }
    }

    @Override
    public float getX(){
        return drawx();
    }

    @Override
    public float getY(){
        return drawy();
    }

    @Override
    public String toString(){
        return floor.name + ":" + block.name + ":" + overlay + "[" + x + "," + y + "] " + "entity=" + (entity == null ? "null" : (entity.getClass().getSimpleName())) + ":" + team();
    }

    //remote utility methods

    @Remote(called = Loc.server)
    public static void setFloor(Tile tile, Block floor, Block overlay){
        tile.setFloor(floor.asFloor());
        tile.setOverlay(overlay);
    }

    @Remote(called = Loc.server)
    public static void removeTile(Tile tile){
        tile.remove();
    }

    @Remote(called = Loc.server)
    public static void setTile(Tile tile, Block block, Team team, int rotation){
        tile.setBlock(block, team, rotation);
    }

    @Remote(called = Loc.server, unreliable = true)
    public static void onTileDamage(Tile tile, float health){
        if(tile.entity != null){
            tile.entity.health(health);

            if(tile.entity.damaged()){
                indexer.notifyTileDamaged(tile.entity);
            }
        }
    }

    @Remote(called = Loc.server)
    public static void onTileDestroyed(Tile tile){
        if(tile.entity == null) return;
        tile.entity.killed();
    }
}
