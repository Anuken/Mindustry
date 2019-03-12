package io.anuke.mindustry.editor;

import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Pack;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.TileOp;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

import java.io.IOException;

public class MapEditor{
    public static final int[] brushSizes = {1, 2, 3, 4, 5, 9, 15, 20};

    private ObjectMap<String, String> tags = new ObjectMap<>();
    private MapRenderer renderer = new MapRenderer(this);
    private Tile[][] tiles;

    private OperationStack stack = new OperationStack();
    private DrawOperation currentOp;
    private boolean loading;

    public int brushSize = 1;
    public int rotation;
    public Block drawBlock = Blocks.stone;
    public Team drawTeam = Team.blue;

    public ObjectMap<String, String> getTags(){
        return tags;
    }

    public void beginEdit(int width, int height){
        reset();

        loading = true;
        tiles = createTiles(width, height);
        renderer.resize(width(), height());
        loading = false;
    }

    public void beginEdit(Map map) throws IOException{
        reset();

        loading = true;
        tiles = createTiles(map.width, map.height);
        tags.putAll(map.tags);
        MapIO.readTiles(map, tiles);
        checkTiles();
        renderer.resize(width(), height());
        loading = false;
    }

    public void beginEdit(Tile[][] tiles){
        reset();

        this.tiles = tiles;
        checkTiles();
        renderer.resize(width(), height());
    }

    //adds missing blockparts
    void checkTiles(){
        //clear block parts first
        for(int x = 0; x < width(); x ++){
            for(int y = 0; y < height(); y++){
                if(tiles[x][y].block() == Blocks.part){
                    tiles[x][y].setBlock(Blocks.air);
                    tiles[x][y].setLinkByte((byte)0);
                }
            }
        }

        //set up missing blockparts
        for(int x = 0; x < width(); x ++){
            for(int y = 0; y < height(); y ++){
                Block drawBlock = tiles[x][y].block();
                if(drawBlock.isMultiblock()){
                    int offsetx = -(drawBlock.size - 1) / 2;
                    int offsety = -(drawBlock.size - 1) / 2;
                    for(int dx = 0; dx < drawBlock.size; dx++){
                        for(int dy = 0; dy < drawBlock.size; dy++){
                            int worldx = dx + offsetx + x;
                            int worldy = dy + offsety + y;

                            if(Structs.inBounds(worldx, worldy, width(), height()) && !(dx + offsetx == 0 && dy + offsety == 0)){
                                Tile tile = tiles[worldx][worldy];
                                tile.setBlock(Blocks.part);
                                tile.setLinkByte(Pack.byteByte((byte) (dx + offsetx + 8), (byte) (dy + offsety + 8)));
                            }
                        }
                    }
                }
            }
        }
    }

    public void load(Runnable r){
        loading = true;
        r.run();
        loading = false;
    }

    /**Creates a 2-D array of EditorTiles with stone as the floor block.*/
    public Tile[][] createTiles(int width, int height){
        tiles = new Tile[width][height];

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                tiles[x][y] = new EditorTile(x, y, Blocks.stone.id, (byte)0);
            }
        }
        return tiles;
    }

    public Map createMap(FileHandle file){
        return new Map(file, width(), height(), new ObjectMap<>(tags), true);
    }

    private void reset(){
        clearOp();
        brushSize = 1;
        drawBlock = Blocks.stone;
        tags = new ObjectMap<>();
    }

    public Tile[][] tiles(){
        return tiles;
    }

    public Tile tile(int x, int y){
        return tiles[x][y];
    }

    public int width(){
        return tiles.length;
    }

    public int height(){
        return tiles[0].length;
    }

    public void draw(int x, int y, boolean paint){
        draw(x, y, paint, drawBlock);
    }

    public void draw(int x, int y, boolean paint, Block drawBlock){
        draw(x, y, paint, drawBlock, 1.0);
    }

    public void draw(int x, int y, boolean paint, Block drawBlock, double chance){
        boolean isfloor = drawBlock instanceof Floor && drawBlock != Blocks.air;

        if(drawBlock.isMultiblock()){
            
            x = Mathf.clamp(x, (drawBlock.size-1)/2, width() - drawBlock.size/2 - 1);
            y = Mathf.clamp(y, (drawBlock.size-1)/2, height() - drawBlock.size/2 - 1);

            int offsetx = -(drawBlock.size - 1) / 2;
            int offsety = -(drawBlock.size - 1) / 2;

            for(int i = 0; i < 2; i++){
                for(int dx = 0; dx < drawBlock.size; dx++){
                    for(int dy = 0; dy < drawBlock.size; dy++){
                        int worldx = dx + offsetx + x;
                        int worldy = dy + offsety + y;

                        if(Structs.inBounds(worldx, worldy, width(), height())){
                            Tile tile = tiles[worldx][worldy];

                            if(i == 1){
                                tile.setBlock(Blocks.part);
                                tile.setLinked((byte)(dx + offsetx), (byte)(dy + offsety));
                            }else{
                                byte link = tile.getLinkByte();
                                Block block = tile.block();

                                if(link != 0){
                                    removeLinked(worldx - (Pack.leftByte(link) - 8), worldy - (Pack.rightByte(link) - 8));
                                }else if(block.isMultiblock()){
                                    removeLinked(worldx, worldy);
                                }
                            }
                        }
                    }
                }
            }

            Tile tile = tiles[x][y];
            tile.setBlock(drawBlock);
            tile.setTeam(drawTeam);
        }else{
            for(int rx = -brushSize; rx <= brushSize; rx++){
                for(int ry = -brushSize; ry <= brushSize; ry++){
                    if(Mathf.dst(rx, ry) <= brushSize - 0.5f && (chance >= 0.999 || Mathf.chance(chance))){
                        int wx = x + rx, wy = y + ry;

                        if(wx < 0 || wy < 0 || wx >= width() || wy >= height() || (paint && !isfloor && tiles[wx][wy].block() == Blocks.air)){
                            continue;
                        }

                        Tile tile = tiles[wx][wy];

                        if(!isfloor){
                            byte link = tile.getLinkByte();
                            Log.info("Remove linkd: " + tiles[x][y]);

                            if(tile.block().isMultiblock()){
                                removeLinked(wx, wy);
                            }else if(link != 0 && tiles[x][y].block() == Blocks.part){
                                removeLinked(wx - (Pack.leftByte(link) - 8), wy - (Pack.rightByte(link) - 8));
                            }
                        }

                        if(isfloor){
                            tile.setFloor((Floor)drawBlock);
                        }else{
                            tile.setBlock(drawBlock);
                            if(drawBlock.synthetic()){
                                tile.setTeam(drawTeam);
                            }
                        }
                    }
                }
            }
        }
    }

    private void removeLinked(int x, int y){
        Block block = tiles[x][y].block();

        int offsetx = -(block.size - 1) / 2;
        int offsety = -(block.size - 1) / 2;
        for(int dx = 0; dx < block.size; dx++){
            for(int dy = 0; dy < block.size; dy++){
                int worldx = x + dx + offsetx, worldy = y + dy + offsety;
                if(Structs.inBounds(worldx, worldy, width(), height())){
                    tiles[worldx][worldy].setTeam(Team.none);
                    tiles[worldx][worldy].setBlock(Blocks.air);
                }
            }
        }
    }

    public MapRenderer renderer(){
        return renderer;
    }

    public void resize(int width, int height){
        clearOp();

        Tile[][] previous = tiles;
        int offsetX = -(width - width())/2, offsetY = -(height - height())/2;
        loading = true;

        tiles = new Tile[width][height];
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int px = offsetX + x, py = offsetY + y;
                if(Structs.inBounds(px, py, previous.length, previous[0].length)){
                    tiles[x][y] = previous[px][py];
                    tiles[x][y].x = (short)x;
                    tiles[x][y].y = (short)y;
                }else{
                    tiles[x][y] = new EditorTile(x, y, Blocks.stone.id, (byte)0);
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
            stack.undo(this);
        }
    }

    public void redo(){
        if(stack.canRedo()){
            stack.redo(this);
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
}