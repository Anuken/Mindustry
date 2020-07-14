package mindustry.world.blocks.power;

import arc.*;
import arc.graphics.g2d.*;
import mindustry.annotations.Annotations.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class SeamlessSolarGenerator extends SolarGenerator{
    @Load(value = "@-#", length = 8)
    public TextureRegion[] edges;

    public SeamlessSolarGenerator(String name){
        super(name);
    }

    @Override
    protected TextureRegion[] icons(){
        return new TextureRegion[]{Core.atlas.find(name + "-icon")};
    }

    public class SeamlessSolarGeneratorEntity extends SolarGeneratorEntity{
        boolean up, right, down, left;

        @Override
        public void onProximityUpdate(){
            super.onProximityUpdate();

            up = foreign(tile, 0, 1);
            right = foreign(tile, 1, 0);
            down = foreign(tile, 0, -1);
            left = foreign(tile, -1, 0);
        }

        @Override
        public void draw(){
            super.draw();

            // outside edges
            if(up)    Draw.rect(edges[0], x, y);
            if(right) Draw.rect(edges[2], x, y);
            if(down)  Draw.rect(edges[4], x, y);
            if(left)  Draw.rect(edges[6], x, y);

            // outside corners
            if(up && right)   Draw.rect(edges[1], x, y);
            if(right && down) Draw.rect(edges[3], x, y);
            if(down && left)  Draw.rect(edges[5], x, y);
            if(left && up)    Draw.rect(edges[7], x, y);

            //inside corners
            if(!right && !down && foreign(tile, 1, -1)) Draw.rect(edges[7], x + (tilesize * 0.75f), y - (tilesize * 0.75f));
            if(!left && !down && foreign(tile, -1, -1)) Draw.rect(edges[1], x - (tilesize * 0.75f), y - (tilesize * 0.75f));
            if(!left && !up && foreign(tile, -1, 1)) Draw.rect(edges[3], x - (tilesize * 0.75f), y + (tilesize * 0.75f));
            if(!right && !up && foreign(tile, 1, 1)) Draw.rect(edges[5], x + (tilesize * 0.75f), y + (tilesize * 0.75f));
        }

        private boolean foreign(Tile tile, int dx, int dy){
            Tile other = tile.getNearby(dx, dy);
            return other == null || other.block() != SeamlessSolarGenerator.this;
        }
    }
}