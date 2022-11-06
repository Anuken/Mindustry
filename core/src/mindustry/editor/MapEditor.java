package mindustry.editor;

import arc.files.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.editor.DrawOperation.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MapEditor{
    public static final float[] brushSizes = {1, 1.5f, 2, 3, 4, 5, 9, 15, 20};

    public StringMap tags = new StringMap();
    public MapRenderer renderer = new MapRenderer();

    private final Context context = new Context();
    private OperationStack stack = new OperationStack();
    private DrawOperation currentOp;
    private boolean loading;

    public float brushSize = 1;
    public int rotation;
    public Block drawBlock = Blocks.stone;
    public Team drawTeam = Team.sharded;

    public boolean isLoading(){
        return loading;
    }

    public void beginEdit(int width, int height){
        reset();

        loading = true;
        createTiles(width, height);
        renderer.resize(width, height);
        loading = false;
    }

    public void beginEdit(Map map){
        reset();

        loading = true;
        tags.putAll(map.tags);
        if(map.file.parent().parent().name().equals("1127400") && steam){
            tags.put("steamid",  map.file.parent().name());
        }
        load(() -> MapIO.loadMap(map, context));
        renderer.resize(width(), height());
        loading = false;
    }

    public void beginEdit(Pixmap pixmap){
        reset();

        createTiles(pixmap.width, pixmap.height);
        load(() -> MapIO.readImage(pixmap, tiles()));
        renderer.resize(width(), height());
    }

    public void updateRenderer(){
        Tiles tiles = world.tiles;
        Seq<Building> builds = new Seq<>();

        for(int i = 0; i < tiles.width * tiles.height; i++){
            Tile tile = tiles.geti(i);
            var build = tile.build;
            if(build != null){
                builds.add(build);
            }
            tiles.seti(i, new EditorTile(tile.x, tile.y, tile.floorID(), tile.overlayID(), build == null ? tile.blockID() : 0));
        }

        for(var build : builds){
            tiles.get(build.tileX(), build.tileY()).setBlock(build.block, build.team, build.rotation, () -> build);
        }

        renderer.resize(width(), height());
    }

    public void load(Runnable r){
        loading = true;
        r.run();
        loading = false;
    }

    /** Creates a 2-D array of EditorTiles with stone as the floor block. */
    private void createTiles(int width, int height){
        Tiles tiles = world.resize(width, height);

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                tiles.set(x, y, new EditorTile(x, y, Blocks.stone.id, (short)0, (short)0));
            }
        }
    }

    public Map createMap(Fi file){
        return new Map(file, width(), height(), new StringMap(tags), true);
    }

    private void reset(){
        clearOp();
        brushSize = 1;
        drawBlock = Blocks.stone;
        tags = new StringMap();
    }

    public Tiles tiles(){
        return world.tiles;
    }

    public Tile tile(int x, int y){
        return world.rawTile(x, y);
    }

    public int width(){
        return world.width();
    }

    public int height(){
        return world.height();
    }

    public void drawBlocksReplace(int x, int y){
        drawBlocks(x, y, tile -> tile.block() != Blocks.air || drawBlock.isFloor());
    }

    public void drawBlocks(int x, int y){
        drawBlocks(x, y, false, false, tile -> true);
    }

    public void drawBlocks(int x, int y, Boolf<Tile> tester){
        drawBlocks(x, y, false, false, tester);
    }

    public void drawBlocks(int x, int y, boolean square, boolean forceOverlay, Boolf<Tile> tester){
        if(drawBlock.isMultiblock()){
            x = Mathf.clamp(x, (drawBlock.size - 1) / 2, width() - drawBlock.size / 2 - 1);
            y = Mathf.clamp(y, (drawBlock.size - 1) / 2, height() - drawBlock.size / 2 - 1);
            if(!hasOverlap(x, y)){
                tile(x, y).setBlock(drawBlock, drawTeam, rotation);
            }
        }else{
            boolean isFloor = drawBlock.isFloor() && drawBlock != Blocks.air;

            Cons<Tile> drawer = tile -> {
                if(!tester.get(tile)) return;

                if(isFloor){
                    if(forceOverlay){
                        tile.setOverlay(drawBlock.asFloor());
                    }else{
                        if(!(drawBlock.asFloor().wallOre && !tile.block().solid)){
                            tile.setFloor(drawBlock.asFloor());
                        }
                    }
                }else if(!(tile.block().isMultiblock() && !drawBlock.isMultiblock())){
                    if(drawBlock.rotate && tile.build != null && tile.build.rotation != rotation){
                        addTileOp(TileOp.get(tile.x, tile.y, (byte)OpType.rotation.ordinal(), (byte)rotation));
                    }

                    tile.setBlock(drawBlock, drawTeam, rotation);
                }
            };

            if(square){
                drawSquare(x, y, drawer);
            }else{
                drawCircle(x, y, drawer);
            }
        }
    }

    boolean hasOverlap(int x, int y){
        Tile tile = world.tile(x, y);
        //allow direct replacement of blocks of the same size
        if(tile != null && tile.isCenter() && tile.block() != drawBlock && tile.block().size == drawBlock.size && tile.x == x && tile.y == y){
            return false;
        }

        //else, check for overlap
        int offsetx = -(drawBlock.size - 1) / 2;
        int offsety = -(drawBlock.size - 1) / 2;
        for(int dx = 0; dx < drawBlock.size; dx++){
            for(int dy = 0; dy < drawBlock.size; dy++){
                int worldx = dx + offsetx + x;
                int worldy = dy + offsety + y;
                Tile other = world.tile(worldx, worldy);

                if(other != null && other.block().isMultiblock()){
                    return true;
                }
            }
        }

        return false;
    }

    public void addCliffs(){
        for(Tile tile : world.tiles){
            if(!tile.block().isStatic() || tile.block() == Blocks.cliff) continue;

            int rotation = 0;
            for(int i = 0; i < 8; i++){
                Tile other = world.tiles.get(tile.x + Geometry.d8[i].x, tile.y + Geometry.d8[i].y);
                if(other != null && !other.block().isStatic()){
                    rotation |= (1 << i);
                }
            }

            if(rotation != 0){
                tile.setBlock(Blocks.cliff);
            }

            tile.data = (byte)rotation;
        }

        for(Tile tile : world.tiles){
            if(tile.block() != Blocks.cliff && tile.block().isStatic()){
                tile.setBlock(Blocks.air);
            }
        }
    }

    public void addFloorCliffs(){
        for(Tile tile : world.tiles){
            if(!tile.floor().hasSurface() || tile.block() == Blocks.cliff) continue;

            int rotation = 0;
            for(int i = 0; i < 8; i++){
                Tile other = world.tiles.get(tile.x + Geometry.d8[i].x, tile.y + Geometry.d8[i].y);
                if(other != null && !other.floor().hasSurface()){
                    rotation |= (1 << i);
                }
            }

            if(rotation != 0){
                tile.setBlock(Blocks.cliff);
            }

            tile.data = (byte)rotation;
        }
    }

    public void drawCircle(int x, int y, Cons<Tile> drawer){
        int clamped = (int)brushSize;
        for(int rx = -clamped; rx <= clamped; rx++){
            for(int ry = -clamped; ry <= clamped; ry++){
                if(Mathf.within(rx, ry, brushSize - 0.5f + 0.0001f)){
                    int wx = x + rx, wy = y + ry;

                    if(wx < 0 || wy < 0 || wx >= width() || wy >= height()){
                        continue;
                    }

                    drawer.get(tile(wx, wy));
                }
            }
        }
    }

    public void drawSquare(int x, int y, Cons<Tile> drawer){
        int clamped = (int)brushSize;
        for(int rx = -clamped; rx <= clamped; rx++){
            for(int ry = -clamped; ry <= clamped; ry++){
                int wx = x + rx, wy = y + ry;

                if(wx < 0 || wy < 0 || wx >= width() || wy >= height()){
                    continue;
                }

                drawer.get(tile(wx, wy));
            }
        }
    }

    public void resize(int width, int height, int shiftX, int shiftY){
        clearOp();

        Tiles previous = world.tiles;
        int offsetX = (width() - width) / 2 - shiftX, offsetY = (height() - height) / 2 - shiftY;
        loading = true;

        world.clearBuildings();

        Tiles tiles = world.tiles = new Tiles(width, height);

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int px = offsetX + x, py = offsetY + y;
                if(previous.in(px, py)){
                    tiles.set(x, y, previous.getn(px, py));
                    Tile tile = tiles.getn(x, y);
                    tile.x = (short)x;
                    tile.y = (short)y;

                    if(tile.build != null && tile.isCenter()){
                        tile.build.x = x * tilesize + tile.block().offset;
                        tile.build.y = y * tilesize + tile.block().offset;

                        //shift links to account for map resize
                        Object config = tile.build.config();
                        if(config != null){
                            Object out = BuildPlan.pointConfig(tile.block(), config, p -> p.sub(offsetX, offsetY));
                            if(out != config){
                                tile.build.configureAny(out);
                            }
                        }
                    }

                }else{
                    tiles.set(x, y, new EditorTile(x, y, Blocks.stone.id, (short)0, (short)0));
                }
            }
        }

        renderer.resize(width, height);
        loading = false;
    }

    public void clearOp(){
        stack.clear();
    }

    public void undo(){
        if(stack.canUndo()){
            stack.undo();
        }
    }

    public void redo(){
        if(stack.canRedo()){
            stack.redo();
        }
    }

    public boolean canUndo(){
        return stack.canUndo();
    }

    public boolean canRedo(){
        return stack.canRedo();
    }

    public void flushOp(){
        if(currentOp == null || currentOp.isEmpty()) return;
        stack.add(currentOp);
        currentOp = null;
    }

    public void addTileOp(long data){
        if(loading) return;

        if(currentOp == null) currentOp = new DrawOperation();
        currentOp.addOperation(data);

        renderer.updatePoint(TileOp.x(data), TileOp.y(data));
    }

    class Context implements WorldContext{
        @Override
        public Tile tile(int index){
            return world.tiles.geti(index);
        }

        @Override
        public void resize(int width, int height){
            world.resize(width, height);
        }

        @Override
        public Tile create(int x, int y, int floorID, int overlayID, int wallID){
            Tile tile = new EditorTile(x, y, floorID, overlayID, wallID);
            tiles().set(x, y, tile);
            return tile;
        }

        @Override
        public boolean isGenerating(){
            return world.isGenerating();
        }

        @Override
        public void begin(){
            world.beginMapLoad();
        }

        @Override
        public void end(){
            world.endMapLoad();
        }
    }
}
