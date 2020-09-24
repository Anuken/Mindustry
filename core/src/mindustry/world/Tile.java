package mindustry.world;

import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class Tile implements Position, QuadTreeObject, Displayable{
    static final ObjectSet<Building> tileSet = new ObjectSet<>();

    /** Extra data for very specific blocks. */
    public byte data;
    /** Tile entity, usually null. */
    public @Nullable Building build;
    public short x, y;
    protected @NonNull Block block;
    protected @NonNull Floor floor;
    protected @NonNull Floor overlay;
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
        changeEntity(Team.derelict, wall::newBuilding, 0);
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

        //very straightforward for odd sizes
        if(block.size % 2 == 1){
            if(Math.abs(x - cx) > Math.abs(y - cy)){
                if(x <= cx - 1) return 0;
                if(x >= cx + 1) return 2;
            }else{
                if(y <= cy - 1) return 1;
                if(y >= cy + 1) return 3;
            }
        }else{ //need offsets here
            if(Math.abs(x - cx + 0.5f) > Math.abs(y - cy + 0.5f)){
                if(x+0.5f <= cx - 1) return 0;
                if(x+0.5f >= cx + 1) return 2;
            }else{
                if(y+0.5f <= cy - 1) return 1;
                if(y+0.5f >= cy + 1) return 3;
            }
        }

        return -1;
    }

    /** Convenience method that returns the building of this tile with a cast.
     * Method name is shortened to prevent conflict. */
    @SuppressWarnings("unchecked")
    public <T extends Building> T bc(){
        return (T)build;
    }

    public float worldx(){
        return x * tilesize;
    }

    public float worldy(){
        return y * tilesize;
    }

    public float drawx(){
        return block().offset + worldx();
    }

    public float drawy(){
        return block().offset + worldy();
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
        return build == null ? Team.derelict : build.team;
    }

    public void setTeam(Team team){
        if(build != null){
            build.team(team);
        }
    }

    public boolean isCenter(){
        return build == null || build.tile() == this;
    }

    public int centerX(){
        return build == null ? x : build.tile.x;
    }

    public int centerY(){
        return build == null ? y : build.tile.y;
    }

    public int getTeamID(){
        return team().id;
    }

    public void setBlock(@NonNull Block type, Team team, int rotation){
        setBlock(type, team, rotation, type::newBuilding);
    }

    public void setBlock(@NonNull Block type, Team team, int rotation, Prov<Building> entityprov){
        changing = true;

        if(type.isStatic() || this.block.isStatic()){
            recache();
        }

        this.block = type;
        preChanged();
        changeEntity(team, entityprov, (byte)Mathf.mod(rotation, 4));

        if(build != null){
            build.team(team);
        }

        //set up multiblock
        if(block.isMultiblock()){
            int offsetx = -(block.size - 1) / 2;
            int offsety = -(block.size - 1) / 2;
            Building entity = this.build;
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
                                    other.build = entity;
                                    other.block = block;
                                }
                            }
                        }
                    }
                }
            }

            this.build = entity;
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
        if(build != null){
            build.onProximityUpdate();
        }
    }

    /** Sets the floor, preserving overlay.*/
    public void setFloorUnder(@NonNull Floor floor){
        Block overlay = this.overlay;
        setFloor(floor);
        setOverlay(overlay);
    }

    /** Sets the block to air. */
    public void setAir(){
        setBlock(Blocks.air);
    }

    public void circle(int radius, Intc2 cons){
        Geometry.circle(x, y, world.width(), world.height(), radius, cons);
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
    public void setNet(Block block){
        Call.setTile(this, block, Team.derelict, 0);
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

    public void setOverlay(@NonNull Block block){
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
        return block.solid || (build != null && build.checkSolid());
    }

    public boolean breakable(){
        return block.destructible || block.breakable || block.update;
    }

    /**
     * Iterates through the list of all tiles linked to this multiblock, or just itself if it's not a multiblock.
     * The result contains all linked tiles, including this tile itself.
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
     * Returns the list of all tiles linked to this multiblock.
     * This array contains all linked tiles, including this tile itself.
     */
    public Seq<Tile> getLinkedTiles(Seq<Tile> tmpArray){
        tmpArray.clear();
        getLinkedTiles(tmpArray::add);
        return tmpArray;
    }

    /**
     * Returns the list of all tiles linked to this multiblock if it were this block.
     * The result contains all linked tiles, including this tile itself.
     */
    public Seq<Tile> getLinkedTilesAs(Block block, Seq<Tile> tmpArray){
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

    public Building getNearbyEntity(int rotation){
        if(rotation == 0) return world.build(x + 1, y);
        if(rotation == 1) return world.build(x, y + 1);
        if(rotation == 2) return world.build(x - 1, y);
        if(rotation == 3) return world.build(x, y - 1);
        return null;
    }

    public boolean interactable(Team team){
        return state.teams.canInteract(team, team());
    }

    public @Nullable Item drop(){
        return overlay == Blocks.air || overlay.itemDrop == null ? floor.itemDrop : overlay.itemDrop;
    }

    public int staticDarkness(){
        return block.solid && block.fillsTile && !block.synthetic() ? data : 0;
    }

    protected void preChanged(){
        if(build != null){
            //only call removed() for the center block - this only gets called once.
            build.onRemoved();
            build.removeFromProximity();

            //remove this tile's dangling entities
            if(build.block.isMultiblock()){
                int cx = build.tileX(), cy = build.tileY();
                int size = build.block.size;
                int offsetx = -(size - 1) / 2;
                int offsety = -(size - 1) / 2;
                for(int dx = 0; dx < size; dx++){
                    for(int dy = 0; dy < size; dy++){
                        Tile other = world.tile(cx + dx + offsetx, cy + dy + offsety);
                        if(other != null){
                            //reset entity and block *manually* - thus, preChanged() will not be called anywhere else, for multiblocks
                            if(other != this){ //do not remove own entity so it can be processed in changed()
                                other.build = null;
                                other.block = Blocks.air;

                                //manually call changed event
                                other.fireChanged();
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

    protected void changeEntity(Team team, Prov<Building> entityprov, int rotation){
        if(build != null){
            int size = build.block.size;
            build.remove();
            build = null;

            //update edge entities
            tileSet.clear();

            for(Point2 edge : Edges.getEdges(size)){
                Building other = world.build(x + edge.x, y + edge.y);
                if(other != null){
                    tileSet.add(other);
                }
            }

            //update proximity, since multiblock was just removed
            for(Building t : tileSet){
                t.updateProximity();
            }
        }

        if(block.hasEntity()){
            build = entityprov.get().init(this, team, block.update && !state.isEditor(), rotation);
        }
    }

    protected void changed(){
        if(!world.isGenerating()){
            if(build != null){
                build.updateProximity();
            }else{
                //since the entity won't update proximity for us, update proximity for all nearby tiles manually
                for(Point2 p : Geometry.d4){
                    Building tile = world.build(x + p.x, y + p.y);
                    if(tile != null && !tile.tile.changing){
                        tile.onProximityUpdate();
                    }
                }
            }
        }

        fireChanged();

        //recache when static block is added
        if(block.isStatic()){
            recache();
        }
    }

    protected void fireChanged(){
        world.notifyChanged(this);
    }

    @Override
    public void display(Table table){
        Block toDisplay = overlay.itemDrop != null ? overlay : floor;

        table.table(t -> {
            t.left();
            t.add(new Image(toDisplay.getDisplayIcon(this))).size(8 * 4);
            t.labelWrap(toDisplay.getDisplayName(this)).left().width(190f).padLeft(5);
        }).growX().left();
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
        return floor.name + ":" + block.name + ":" + overlay + "[" + x + "," + y + "] " + "entity=" + (build == null ? "null" : (build.getClass().getSimpleName())) + ":" + team();
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
    public static void tileDamage(Building build, float health){
        if(build == null) return;

        build.health = health;

        if(build.damaged()){
            indexer.notifyTileDamaged(build);
        }
    }

    @Remote(called = Loc.server)
    public static void tileDestroyed(Building build){
        if(build == null) return;
        build.killed();
    }
}
