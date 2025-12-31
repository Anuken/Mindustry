package mindustry.world.blocks.environment;

import arc.*;
import arc.graphics.g2d.*;
import arc.math.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class TiledFloor extends Floor{
    public TextureRegion[][][][] sizedRegions;
    public int maxSize = 3;

    public TiledFloor(String name){
        super(name);
    }

    public TiledFloor(String name, int variants, int maxSize){
        super(name);
        this.variants = variants;
        this.maxSize = maxSize;
    }

    @Override
    public void load(){
        super.load();

        sizedRegions = new TextureRegion[maxSize + 1][variants][][];

        for(int size = 1; size <= maxSize; size++){
            for(int v = 0; v < variants; v++){
                TextureRegion base = Core.atlas.find(name + "-" + size + "-" + v);
                int actualSize = size;
                if(!base.found()) base = Core.atlas.find(name + "-" + size);
                if(!base.found()){
                    actualSize = 1; //the 1x1 sprite should not be split like a larger region
                    base = region;
                }

                sizedRegions[size][v] = base.split(base.width / actualSize, base.height / actualSize);
            }
        }
    }

    long state(Tile tile){
        return world.tiles.getTmpFloorState(tile.array());
    }

    @Override
    public void floorChanged(Tile tile){

        if(TiledState.changes(state(tile)) != world.floorChanges && !world.isGenerating()){
            scan(tile);
        }
    }

    void scan(Tile tile){
        //max size possible
        int size = maxSize;
        int changes = world.floorChanges;
        boolean isOverlay = tile.floor() != this;

        //scan to the top right for the biggest size possible
        for(int cx = 0; cx < size; cx++){
            for(int cy = 0; cy < size; cy++){
                Tile other = tile.nearby(cx, cy);
                if(other == null || ((isOverlay ? other.overlay() : other.floor()) != this) || TiledState.changes(state(other)) == changes){
                    int max = Math.max(cx, cy);
                    size = Math.min(max, size);
                    size = Math.min(max, size);
                }
            }
        }

        //assign roots based on max value
        for(int cx = 0; cx < size; cx++){
            for(int cy = 0; cy < size; cy++){
                Tile other = tile.nearby(cx, cy);
                if(other != null){
                    long otherState = state(other);

                    if(!headless && TiledState.changes(otherState) != 0){
                        Tile otherRoot = other.nearby(-TiledState.x(otherState), -TiledState.y(otherState));
                        if(otherRoot != null){
                            Core.app.post(() -> renderer.blocks.floor.recacheTile(otherRoot));
                        }
                    }

                    //mark as updated
                    world.tiles.setTmpFloorState(other.array(), TiledState.get(cx, cy, size, changes));
                    if(!headless && otherState != 0){
                        Core.app.post(() -> renderer.blocks.floor.recacheTile(other));
                    }
                }
            }
        }
    }

    @Override
    public void drawMain(Tile tile){
        long state = state(tile);
        //stale state, start scanning.
        if(TiledState.changes(state) != world.floorChanges){
            scan(tile);
            //state has most likely updated
            state = state(tile);
        }

        int size = Mathf.clamp(TiledState.size(state), 1, maxSize);
        int cx = TiledState.x(state), cy = TiledState.y(state);
        int variant = variant(tile.x - cx, tile.y - cy, variants);

        TextureRegion[][] regions = sizedRegions[size][variant];

        Draw.rect(regions[Mathf.clamp(cx, 0, regions.length - 1)][Mathf.clamp(size - 1 - cy, 0, regions[0].length - 1)], tile.worldx(), tile.worldy());
    }

    @Struct
    class TiledStateStruct{
        @StructField(4)
        int x;
        @StructField(4)
        int y;
        @StructField(8)
        int size;

        int changes;
    }
}
