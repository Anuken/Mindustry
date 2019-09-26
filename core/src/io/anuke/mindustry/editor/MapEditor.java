package io.anuke.mindustry.editor;

import io.anuke.arc.collection.StringMap;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.function.Consumer;
import io.anuke.arc.function.Predicate;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.TileOp;
import io.anuke.mindustry.io.LegacyMapIO;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.BlockPart;

import static io.anuke.mindustry.Vars.*;

public class MapEditor{
    public static final int[] brushSizes = {1, 2, 3, 4, 5, 9, 15, 20};

    private final Context context = new Context();
    private StringMap tags = new StringMap();
    private MapRenderer renderer = new MapRenderer(this);

    private OperationStack stack = new OperationStack();
    private DrawOperation currentOp;
    private boolean loading;

    public int brushSize = 1;
    public int rotation;
    public Block drawBlock = Blocks.stone;
    public Team drawTeam = Team.sharded;

    public StringMap getTags(){
        return tags;
    }

    public void beginEdit(int width, int height){
        reset();

        loading = true;
        createTiles(width, height);
        renderer.resize(width(), height());
        loading = false;
    }

    public void beginEdit(Map map){
        reset();

        loading = true;
        tags.putAll(map.tags);
        if(map.file.parent().parent().name().equals("1127400") && steam){
            tags.put("steamid",  map.file.parent().name());
        }
        MapIO.loadMap(map, context);
        checkLinkedTiles();
        renderer.resize(width(), height());
        loading = false;
    }

    public void beginEdit(Pixmap pixmap){
        reset();

        createTiles(pixmap.getWidth(), pixmap.getHeight());
        load(() -> LegacyMapIO.readPixmap(pixmap, tiles()));
        renderer.resize(width(), height());
    }

    //adds missing blockparts
    public void checkLinkedTiles(){
        Tile[][] tiles = world.getTiles();

        //clear block parts first
        for(int x = 0; x < width(); x++){
            for(int y = 0; y < height(); y++){
                if(tiles[x][y].block() instanceof BlockPart){
                    tiles[x][y].setBlock(Blocks.air);
                }
            }
        }

        //set up missing blockparts
        for(int x = 0; x < width(); x++){
            for(int y = 0; y < height(); y++){
                if(tiles[x][y].block().isMultiblock()){
                    world.setBlock(tiles[x][y], tiles[x][y].block(), tiles[x][y].getTeam());
                }
            }
        }
    }

    public void load(Runnable r){
        loading = true;
        r.run();
        loading = false;
    }

    /** Creates a 2-D array of EditorTiles with stone as the floor block. */
    private void createTiles(int width, int height){
        Tile[][] tiles = world.createTiles(width, height);

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                tiles[x][y] = new EditorTile(x, y, Blocks.stone.id, (short)0, (short)0);
            }
        }
    }

    public Map createMap(FileHandle file){
        return new Map(file, width(), height(), new StringMap(tags), true);
    }

    private void reset(){
        clearOp();
        brushSize = 1;
        drawBlock = Blocks.stone;
        tags = new StringMap();
    }

    public Tile[][] tiles(){
        return world.getTiles();
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
        drawBlocks(x, y, false, tile -> true);
    }

    public void drawBlocks(int x, int y, Predicate<Tile> tester){
        drawBlocks(x, y, false, tester);
    }

    public void drawBlocks(int x, int y, boolean square, Predicate<Tile> tester){
        if(drawBlock.isMultiblock()){
            x = Mathf.clamp(x, (drawBlock.size - 1) / 2, width() - drawBlock.size / 2 - 1);
            y = Mathf.clamp(y, (drawBlock.size - 1) / 2, height() - drawBlock.size / 2 - 1);

            int offsetx = -(drawBlock.size - 1) / 2;
            int offsety = -(drawBlock.size - 1) / 2;

            for(int dx = 0; dx < drawBlock.size; dx++){
                for(int dy = 0; dy < drawBlock.size; dy++){
                    int worldx = dx + offsetx + x;
                    int worldy = dy + offsety + y;

                    if(Structs.inBounds(worldx, worldy, width(), height())){
                        Tile tile = tile(worldx, worldy);

                        Block block = tile.block();

                        //bail out if there's anything blocking the way
                        if(block.isMultiblock() || block instanceof BlockPart){
                            return;
                        }

                        renderer.updatePoint(worldx, worldy);
                    }
                }
            }

            world.setBlock(tile(x, y), drawBlock, drawTeam);
        }else{
            boolean isFloor = drawBlock.isFloor() && drawBlock != Blocks.air;

            Consumer<Tile> drawer = tile -> {
                if(!tester.test(tile)) return;

                //remove linked tiles blocking the way
                if(!isFloor && (tile.isLinked() || tile.block().isMultiblock())){
                    world.removeBlock(tile.link());
                }

                if(isFloor){
                    tile.setFloor(drawBlock.asFloor());
                }else{
                    tile.setBlock(drawBlock);
                    if(drawBlock.synthetic()){
                        tile.setTeam(drawTeam);
                    }
                    if(drawBlock.rotate){
                        tile.rotation((byte)rotation);
                    }
                }
            };

            if(square){
                drawSquare(x, y, drawer);
            }else{
                drawCircle(x, y, drawer);
            }
        }
    }

    public void drawCircle(int x, int y, Consumer<Tile> drawer){
        for(int rx = -brushSize; rx <= brushSize; rx++){
            for(int ry = -brushSize; ry <= brushSize; ry++){
                if(Mathf.dst2(rx, ry) <= (brushSize - 0.5f) * (brushSize - 0.5f)){
                    int wx = x + rx, wy = y + ry;

                    if(wx < 0 || wy < 0 || wx >= width() || wy >= height()){
                        continue;
                    }

                    drawer.accept(tile(wx, wy));
                }
            }
        }
    }

    public void drawSquare(int x, int y, Consumer<Tile> drawer){
        for(int rx = -brushSize; rx <= brushSize; rx++){
            for(int ry = -brushSize; ry <= brushSize; ry++){
                int wx = x + rx, wy = y + ry;

                if(wx < 0 || wy < 0 || wx >= width() || wy >= height()){
                    continue;
                }

                drawer.accept(tile(wx, wy));
            }
        }
    }

    public MapRenderer renderer(){
        return renderer;
    }

    public void resize(int width, int height){
        clearOp();

        Tile[][] previous = world.getTiles();
        int offsetX = -(width - width()) / 2, offsetY = -(height - height()) / 2;
        loading = true;

        Tile[][] tiles = world.createTiles(width, height);
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int px = offsetX + x, py = offsetY + y;
                if(Structs.inBounds(px, py, previous.length, previous[0].length)){
                    tiles[x][y] = previous[px][py];
                    tiles[x][y].x = (short)x;
                    tiles[x][y].y = (short)y;
                }else{
                    tiles[x][y] = new EditorTile(x, y, Blocks.stone.id, (short)0, (short)0);
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

        if(currentOp == null) currentOp = new DrawOperation(this);
        currentOp.addOperation(data);

        renderer.updatePoint(TileOp.x(data), TileOp.y(data));
    }

    class Context implements WorldContext{
        @Override
        public Tile tile(int x, int y){
            return world.tile(x, y);
        }

        @Override
        public void resize(int width, int height){
            world.createTiles(width, height);
        }

        @Override
        public Tile create(int x, int y, int floorID, int overlayID, int wallID){
            return (tiles()[x][y] = new EditorTile(x, y, floorID, overlayID, wallID));
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