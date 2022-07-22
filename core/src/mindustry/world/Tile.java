package mindustry.world;

import arc.*;
import arc.func.*;
import arc.math.*;
import arc.math.geom.*;
import arc.math.geom.QuadTree.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class Tile implements Position, QuadTreeObject, Displayable{
    private static final TileChangeEvent tileChange = new TileChangeEvent();
    private static final TilePreChangeEvent preChange = new TilePreChangeEvent();
    private static final ObjectSet<Building> tileSet = new ObjectSet<>();

    /** Extra data for very specific blocks. */
    public byte data;
    /** Tile entity, usually null. */
    public @Nullable Building build;
    public short x, y;
    protected Block block;
    protected Floor floor;
    protected Floor overlay;
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
        changeBuild(Team.derelict, wall::newBuilding, 0);
        changed();
    }

    public Tile(int x, int y, int floor, int overlay, int wall){
        this(x, y, content.block(floor), content.block(overlay), content.block(wall));
    }

    /** Returns this tile's position as a packed point. */
    public int pos(){
        return Point2.pack(x, y);
    }

    /** @return this tile's position, packed to the world width - for use in width*height arrays. */
    public int array(){
        return x + y * world.tiles.width;
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

    public static int relativeTo(float x, float y, float cx, float cy){
        if(Math.abs(x - cx) > Math.abs(y - cy)){
            if(x <= cx - 1) return 0;
            if(x >= cx + 1) return 2;
        }else{
            if(y <= cy - 1) return 1;
            if(y >= cy + 1) return 3;
        }
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

    /**
     * Returns the flammability of the tile. Used for fire calculations.
     * Takes flammability of floor liquid into account.
     */
    public float getFlammability(){
        if(block == Blocks.air){
            if(floor.liquidDrop != null) return floor.liquidDrop.flammability;
            return 0;
        }else if(build != null){
            float result = 0f;

            if(block.hasItems){
                result += build.items.sum((item, amount) -> item.flammability * amount) / Math.max(block.itemCapacity, 1) * Mathf.clamp(block.itemCapacity / 2.4f, 1f, 3f);
            }

            if(block.hasLiquids){
                result += build.liquids.sum((liquid, amount) -> liquid.flammability * amount / 1.6f) / Math.max(block.liquidCapacity, 1) * Mathf.clamp(block.liquidCapacity / 30f, 1f, 2f);
            }

            return result;
        }
        return 0;
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
        return block.solid && ((!block.synthetic() && block.fillsTile) || block.forceDark);
    }

    public Floor floor(){
        return floor;
    }

    public Block block(){
        return block;
    }

    public Floor overlay(){
        return overlay;
    }

    @SuppressWarnings("unchecked")
    public <T extends Block> T cblock(){
        return (T)block;
    }

    public Team team(){
        return build == null ? Team.derelict : build.team;
    }

    /** Do not call unless you know what you are doing! This does not update the indexer! */
    public void setTeam(Team team){
        if(build != null){
            build.team(team);
        }
    }

    public boolean isCenter(){
        return build == null || build.tile == this;
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

    public void setBlock(Block type, Team team, int rotation){
        setBlock(type, team, rotation, type::newBuilding);
    }

    public void setBlock(Block type, Team team, int rotation, Prov<Building> entityprov){
        changing = true;

        if(type.isStatic() || this.block.isStatic()){
            recache();
            recacheWall();
        }

        preChanged();

        this.block = type;
        changeBuild(team, entityprov, (byte)Mathf.mod(rotation, 4));

        if(build != null){
            build.team(team);
        }

        //set up multiblock
        if(block.isMultiblock()){
            int offset = -(block.size - 1) / 2;
            Building entity = this.build;
            Block block = this.block;

            //two passes: first one clears, second one sets
            for(int pass = 0; pass < 2; pass++){
                for(int dx = 0; dx < block.size; dx++){
                    for(int dy = 0; dy < block.size; dy++){
                        int worldx = dx + offset + x;
                        int worldy = dy + offset + y;
                        if(!(worldx == x && worldy == y)){
                            Tile other = world.tile(worldx, worldy);

                            if(other != null){
                                if(pass == 0){
                                    //first pass: delete existing blocks - this should automatically trigger removal if overlap exists
                                    //TODO pointless setting air to air?
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

    public void setBlock(Block type, Team team){
        setBlock(type, team, 0);
    }

    public void setBlock(Block type){
        setBlock(type, Team.derelict, 0);
    }

    /** This resets the overlay! */
    public void setFloor(Floor type){
        this.floor = type;
        this.overlay = (Floor)Blocks.air;

        if(!headless && !world.isGenerating() && !isEditorTile()){
            renderer.blocks.removeFloorIndex(this);
        }

        recache();
        if(build != null){
            build.onProximityUpdate();
        }
    }

    public boolean isEditorTile(){
        return false;
    }

    /** Sets the floor, preserving overlay.*/
    public void setFloorUnder(Floor floor){
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

    public void circle(int radius, Cons<Tile> cons){
        circle(radius, (x, y) -> cons.get(world.rawTile(x, y)));
    }

    public void recacheWall(){
        if(!headless && !world.isGenerating()){
            renderer.blocks.recacheWall(this);
        }
    }

    public void recache(){
        if(!headless && !world.isGenerating()){
            renderer.blocks.floor.recacheTile(this);
            renderer.minimap.update(this);
            renderer.blocks.invalidateTile(this);
            renderer.blocks.addFloorIndex(this);
            //update neighbor tiles as well
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

    /** set()-s this tile, except it's synced across the network */
    public void setOverlayNet(Block overlay){
        Call.setOverlay(this, overlay);
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

    /** Sets the overlay without a recache. */
    public void setOverlayQuiet(Block block){
        this.overlay = (Floor)block;
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
        return block.solid || floor.solid || (build != null && build.checkSolid());
    }

    public boolean breakable(){
        return block.destructible || block.breakable || block.update;
    }

    /** @return whether the floor on this tile deals damage or can be drowned on. */
    public boolean dangerous(){
        return !block.solid && (floor.isDeep() || floor.damageTaken > 0);
    }

    /**
     * Iterates through the list of all tiles linked to this multiblock, or just itself if it's not a multiblock.
     * The result contains all linked tiles, including this tile itself.
     */
    public void getLinkedTiles(Cons<Tile> cons){
        if(block.isMultiblock()){
            int size = block.size, o = block.sizeOffset;
            for(int dx = 0; dx < size; dx++){
                for(int dy = 0; dy < size; dy++){
                    Tile other = world.tile(x + dx + o, y + dy + o);
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
        getLinkedTilesAs(block, tmpArray::add);
        return tmpArray;
    }

    /**
     * Returns the list of all tiles linked to this multiblock if it were this block.
     * The result contains all linked tiles, including this tile itself.
     */
    public void getLinkedTilesAs(Block block, Cons<Tile> tmpArray){
        if(block.isMultiblock()){
            int size = block.size, o = block.sizeOffset;
            for(int dx = 0; dx < size; dx++){
                for(int dy = 0; dy < size; dy++){
                    Tile other = world.tile(x + dx + o, y + dy + o);
                    if(other != null) tmpArray.get(other);
                }
            }
        }else{
            tmpArray.get(this);
        }
    }

    public Rect getHitbox(Rect rect){
        return rect.setCentered(drawx(), drawy(), block.size * tilesize, block.size * tilesize);
    }

    public Rect getBounds(Rect rect){
        return rect.set(x * tilesize - tilesize/2f, y * tilesize - tilesize/2f, tilesize, tilesize);
    }

    @Override
    public void hitbox(Rect rect){
        getHitbox(rect);
    }

    public @Nullable Tile nearby(Point2 relative){
        return world.tile(x + relative.x, y + relative.y);
    }

    public @Nullable Tile nearby(int dx, int dy){
        return world.tile(x + dx, y + dy);
    }

    public @Nullable Tile nearby(int rotation){
        return switch(rotation){
            case 0 -> world.tile(x + 1, y);
            case 1 -> world.tile(x, y + 1);
            case 2 -> world.tile(x - 1, y);
            case 3 -> world.tile(x, y - 1);
            default -> null;
        };
    }

    public @Nullable Building nearbyBuild(int rotation){
        return switch(rotation){
            case 0 -> world.build(x + 1, y);
            case 1 -> world.build(x, y + 1);
            case 2 -> world.build(x - 1, y);
            case 3 -> world.build(x, y - 1);
            default -> null;
        };
    }

    public boolean interactable(Team team){
        return state.teams.canInteract(team, team());
    }

    public @Nullable Item drop(){
        return overlay == Blocks.air || overlay.itemDrop == null ? floor.itemDrop : overlay.itemDrop;
    }

    public @Nullable Item wallDrop(){
        return block.solid ?
            block.itemDrop != null ? block.itemDrop :
            overlay.wallOre && !block.synthetic() ? overlay.itemDrop :
            null : null;
    }

    public int staticDarkness(){
        return block.solid && block.fillsTile && !block.synthetic() ? data : 0;
    }

    /** @return true if these tiles are right next to each other. */
    public boolean adjacentTo(Tile tile){
        return relativeTo(tile) != -1;
    }

    protected void preChanged(){
        firePreChanged();

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
                                //manually call pre-change event for other tile
                                other.firePreChanged();

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
    }

    protected void changeBuild(Team team, Prov<Building> entityprov, int rotation){
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

        if(block.hasBuilding()){
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
        if(!world.isGenerating()){
            Events.fire(tileChange.set(this));
        }
    }

    protected void firePreChanged(){
        if(!world.isGenerating()){
            Events.fire(preChange.set(this));
        }
    }

    @Override
    public void display(Table table){

        Block toDisplay =
            block.itemDrop != null ? block :
            overlay.itemDrop != null || wallDrop() != null ? overlay :
            floor;

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
    public static void setOverlay(Tile tile, Block overlay){
        tile.setOverlay(overlay);
    }

    @Remote(called = Loc.server)
    public static void removeTile(Tile tile){
        tile.remove();
    }

    @Remote(called = Loc.server)
    public static void setTile(Tile tile, Block block, Team team, int rotation){
        if(tile == null) return;
        tile.setBlock(block, team, rotation);
    }

    @Remote(called = Loc.server)
    public static void setTeam(Building build, Team team){
        if(build != null){
            build.changeTeam(team);
        }
    }

    @Remote(called = Loc.server)
    public static void buildDestroyed(Building build){
        if(build == null) return;
        build.killed();
    }

    @Remote
    public static void buildHealthUpdate(IntSeq buildings){
        for(int i = 0; i < buildings.size; i += 2){
            int pos = buildings.items[i];
            float health = Float.intBitsToFloat(buildings.items[i + 1]);
            var build = world.build(pos);
            if(build != null && build.health != health){
                build.health = health;
                indexer.notifyHealthChanged(build);
            }
        }
    }
}
