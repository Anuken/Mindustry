package io.anuke.mindustry.editor;

import io.anuke.arc.collection.GridBits;
import io.anuke.arc.collection.ObjectMap;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Pack;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.TileOp;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.Floor;

import static io.anuke.mindustry.Vars.content;
import static io.anuke.mindustry.Vars.world;

public class MapEditor{
    public static final int[] brushSizes = {1, 2, 3, 4, 5, 9, 15, 20};

    private ObjectMap<String, String> tags = new ObjectMap<>();
    private MapRenderer renderer = new MapRenderer(this);
    private Tile[][] tiles;

    private OperationStack stack = new OperationStack();
    private DrawOperation op;
    private GridBits used;

    public int brushSize = 1;
    public int rotation;
    public Block drawBlock = Blocks.stone;
    public Team drawTeam = Team.blue;

    public ObjectMap<String, String> getTags(){
        return tags;
    }

    public void beginEdit(Tile[][] map, ObjectMap<String, String> tags, boolean clear){

        this.brushSize = 1;
        this.tags = tags;

        if(clear){
            for(int x = 0; x < map.length; x++){
                for(int y = 0; y < map[0].length; y++){
                    map[x][y].setFloor((Floor)Blocks.stone);
                }
            }
        }

        drawBlock = Blocks.stone;
        renderer.resize(map.length, map[0].length);
    }

    public Tile[][] tiles(){
        return tiles;
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
        byte writeID = drawBlock.id;
        byte partID = Blocks.part.id;
        byte rotationTeam = Pack.byteByte(drawBlock.rotate ? (byte)rotation : 0, drawBlock.synthetic() ? (byte)drawTeam.ordinal() : 0);

        boolean isfloor = drawBlock instanceof Floor && drawBlock != Blocks.air;

        if(drawBlock.isMultiblock()){
            x = Mathf.clamp(x, (drawBlock.size-1)/2, world.width() - drawBlock.size/2 - 1);
            y = Mathf.clamp(y, (drawBlock.size-1)/2, world.height() - drawBlock.size/2 - 1);

            int offsetx = -(drawBlock.size - 1) / 2;
            int offsety = -(drawBlock.size - 1) / 2;

            for(int i = 0; i < 2; i++){
                for(int dx = 0; dx < drawBlock.size; dx++){
                    for(int dy = 0; dy < drawBlock.size; dy++){
                        int worldx = dx + offsetx + x;
                        int worldy = dy + offsety + y;

                        if(Structs.inBounds(worldx, worldy, world.width(), world.height())){
                            TileDataMarker prev = getPrev(worldx, worldy, false);

                            if(i == 1){
                                map.write(worldx, worldy, DataPosition.wall, partID);
                                map.write(worldx, worldy, DataPosition.rotationTeam, rotationTeam);
                                map.write(worldx, worldy, DataPosition.link, Pack.byteByte((byte) (dx + offsetx + 8), (byte) (dy + offsety + 8)));
                            }else{
                                byte link = map.read(worldx, worldy, DataPosition.link);
                                byte block = map.read(worldx, worldy, DataPosition.wall);

                                if(link != 0){
                                    removeLinked(worldx - (Pack.leftByte(link) - 8), worldy - (Pack.rightByte(link) - 8));
                                }else if(content.block(block).isMultiblock()){
                                    removeLinked(worldx, worldy);
                                }
                            }

                            onWrite(worldx, worldy, prev);
                        }
                    }
                }
            }

            TileDataMarker prev = getPrev(x, y, false);

            map.write(x, y, DataPosition.wall, writeID);
            map.write(x, y, DataPosition.link, (byte) 0);
            map.write(x, y, DataPosition.rotationTeam, rotationTeam);

            onWrite(x, y, prev);
        }else{
            for(int rx = -brushSize; rx <= brushSize; rx++){
                for(int ry = -brushSize; ry <= brushSize; ry++){
                    if(Mathf.dst(rx, ry) <= brushSize - 0.5f && (chance >= 0.999 || Mathf.chance(chance))){
                        int wx = x + rx, wy = y + ry;

                        if(wx < 0 || wy < 0 || wx >= map.width() || wy >= map.height() || (paint && !isfloor && content.block(map.read(wx, wy, DataPosition.wall)) == Blocks.air)){
                            continue;
                        }

                        TileDataMarker prev = getPrev(wx, wy, true);

                        if(!isfloor){
                            byte link = map.read(wx, wy, DataPosition.link);

                            if(content.block(map.read(wx, wy, DataPosition.wall)).isMultiblock()){
                                removeLinked(wx, wy);
                            }else if(link != 0){
                                removeLinked(wx - (Pack.leftByte(link) - 8), wy - (Pack.rightByte(link) - 8));
                            }
                        }

                        if(isfloor){
                            map.write(wx, wy, DataPosition.floor, writeID);
                        }else{
                            map.write(wx, wy, DataPosition.wall, writeID);
                            map.write(wx, wy, DataPosition.link, (byte) 0);
                            map.write(wx, wy, DataPosition.rotationTeam, rotationTeam);
                        }

                        onWrite(x + rx, y + ry, prev);
                    }
                }
            }
        }
    }

    private void removeLinked(int x, int y){
        Block block = content.block(map.read(x, y, DataPosition.wall));

        int offsetx = -(block.size - 1) / 2;
        int offsety = -(block.size - 1) / 2;
        for(int dx = 0; dx < block.size; dx++){
            for(int dy = 0; dy < block.size; dy++){
                int worldx = x + dx + offsetx, worldy = y + dy + offsety;
                if(Structs.inBounds(worldx, worldy, map.width(), map.height())){
                    TileDataMarker prev = getPrev(worldx, worldy, false);

                    map.write(worldx, worldy, DataPosition.link, (byte) 0);
                    map.write(worldx, worldy, DataPosition.rotationTeam, (byte) 0);
                    map.write(worldx, worldy, DataPosition.wall, (byte) 0);

                    onWrite(worldx, worldy, prev);
                }
            }
        }
    }

    boolean checkDupes(int x, int y){
        return Vars.ui.editor.getView().checkForDuplicates((short) x, (short) y);
    }

    void onWrite(int x, int y, TileDataMarker previous){
        if(previous == null){
            renderer.updatePoint(x, y);
            return;
        }

        TileDataMarker current = map.new TileDataMarker();
        map.position(x, y);
        map.read(current);

        Vars.ui.editor.getView().addTileOp(new TileOperation((short) x, (short) y, previous, current));
        renderer.updatePoint(x, y);
    }

    TileDataMarker getPrev(int x, int y, boolean checkDupes){
        if(checkDupes && checkDupes(x, y)){
            return null;
        }else{
            TileDataMarker marker = map.newDataMarker();
            map.position(x, y);
            map.read(marker);
            return marker;
        }
    }

    public MapRenderer renderer(){
        return renderer;
    }

    public void resize(int width, int height){
        Tile[][] previous = tiles;
        int offsetX = -(width - width())/2, offsetY = -(height - height())/2;

        tiles = new Tile[width][height];
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                int px = offsetX + x, py = offsetY + y;
                if(Structs.inBounds(px, py, previous.length, previous[0].length)){
                    tiles[x][y] = previous[px][py];
                    tiles[x][y].x = (short)x;
                    tiles[x][y].y = (short)y;
                }else{
                    tiles[x][y] = new Tile(x, y, Blocks.stone.id, (byte)0);
                }
            }
        }
        renderer.resize(width, height);
    }

    public void changeFloor(int x, int y, Block to){
        Block from = tiles[x][y].floor();
        tiles[x][y].setFloor((Floor)to);
        addTileOp(TileOp.get((short)x, (short)y, (byte)0, from.id, to.id));
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

    public void addTileOp(long data){
        used.set(TileOp.x(data), TileOp.y(data));
        op.addOperation(data);
    }

    public boolean checkUsed(short x, short y){
        if(used == null || used.width() != width() || used.height() != height()){
            used = new GridBits(world.width(), world.height());
        }

        return used.get(x, y);
    }
}
