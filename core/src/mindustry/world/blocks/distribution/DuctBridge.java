package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

//TODO display range
public class DuctBridge extends Block{
    public @Load("@-bridge") TextureRegion bridgeRegion;
    public @Load("@-bridge-bottom") TextureRegion bridgeBotRegion;
    //public @Load("@-bridge-top") TextureRegion bridgeTopRegion;
    public @Load("@-arrow") TextureRegion arrowRegion;
    public @Load("@-dir") TextureRegion dirRegion;

    public int range = 4;
    public float speed = 5f;

    public DuctBridge(String name){
        super(name);
        update = true;
        solid = true;
        rotate = true;
        itemCapacity = 4;
        hasItems = true;
        group = BlockGroup.transportation;
        noUpdateDisabled = true;
        envEnabled = Env.space | Env.terrestrial | Env.underwater;
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(dirRegion, req.drawx(), req.drawy(), req.rotation * 90);
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, dirRegion};
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation){
        Placement.calculateNodes(points, this, rotation, (point, other) -> Math.max(Math.abs(point.x - other.x), Math.abs(point.y - other.y)) <= range);
    }

    public boolean positionsValid(int x1, int y1, int x2, int y2){
        if(x1 == x2){
            return Math.abs(y1 - y2) <= range;
        }else if(y1 == y2){
            return Math.abs(x1 - x2) <= range;
        }else{
            return false;
        }
    }

    public class DuctBridgeBuild extends Building{
        public Building[] occupied = new Building[4];
        public float progress = 0f;

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            Draw.rect(dirRegion, x, y, rotdeg());
            var link = findLink();
            if(link != null){
                Draw.z(Layer.power);
                Draw.alpha(Renderer.bridgeOpacity);
                float
                angle = angleTo(link),
                cx = (x + link.x)/2f,
                cy = (y + link.y)/2f,
                len = Math.max(Math.abs(x - link.x), Math.abs(y - link.y)) - size * tilesize;

                Draw.rect(bridgeRegion, cx, cy, len, tilesize, angle);
                Draw.color(0.4f, 0.4f, 0.4f, 0.4f * Renderer.bridgeOpacity);
                Draw.rect(bridgeBotRegion, cx, cy, len, tilesize, angle);
                Draw.reset();
                Draw.alpha(Renderer.bridgeOpacity);
                //Draw.rect(bridgeTopRegion, cx, cy, len, tilesize, angle);

                for(float i = 6f; i <= len + size * tilesize - 5f; i += 5f){
                    Draw.rect(arrowRegion, x + Geometry.d4x(rotation) * i, y + Geometry.d4y(rotation) * i, angle);
                }

                Draw.reset();
            }
        }

        @Nullable
        public DuctBridgeBuild findLink(){
            for(int i = 1; i <= range; i++){
                Tile other = tile.nearby(Geometry.d4x(rotation) * i, Geometry.d4y(rotation) * i);
                if(other.build instanceof DuctBridgeBuild build && build.team == team){
                    return build;
                }
            }
            return null;
        }

        @Override
        public void updateTile(){
            var link = findLink();
            if(link != null){
                link.occupied[rotation % 4] = this;
                if(items.any() && link.items.total() < link.block.itemCapacity){
                    progress += edelta();
                    while(progress > speed){
                        Item next = items.take();
                        if(next != null && link.items.total() < link.block.itemCapacity){
                            link.handleItem(this, next);
                        }
                        progress -= speed;
                    }
                }
            }

            if(link == null && items.any()){
                Item next = items.first();
                if(moveForward(next)){
                    items.remove(next, 1);
                }
            }

            for(int i = 0; i < 4; i++){
                if(occupied[i] == null || occupied[i].rotation != i || !occupied[i].isValid()){
                    occupied[i] = null;
                }
            }
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            int rel = this.relativeTo(source);
            return items.total() < itemCapacity && rel != rotation && occupied[(rel + 2) % 4] == null;
        }
    }
}
