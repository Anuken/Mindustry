package mindustry.world.blocks.distribution;

import arc.graphics.g2d.*;
import arc.math.*;
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

public class DuctBridge extends Block{
    private static BuildPlan otherReq;
    private int otherDst = 0;

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
        drawArrow = false;
    }

    @Override
    public void init(){
        clipSize = Math.max(clipSize, (range + 0.5f) * 2 * tilesize);
        super.init();
    }

    @Override
    public void drawRequestRegion(BuildPlan req, Eachable<BuildPlan> list){
        Draw.rect(region, req.drawx(), req.drawy());
        Draw.rect(dirRegion, req.drawx(), req.drawy(), req.rotation * 90);
    }

    @Override
    public void drawRequestConfigTop(BuildPlan req, Eachable<BuildPlan> list){
        otherReq = null;
        otherDst = range;
        Point2 d = Geometry.d4(req.rotation);
        list.each(other -> {
            if(other.block == this && req != other && Mathf.clamp(other.x - req.x, -1, 1) == d.x && Mathf.clamp(other.y - req.y, -1, 1) == d.y){
                int dst = Math.max(Math.abs(other.x - req.x), Math.abs(other.y - req.y));
                if(dst <= otherDst){
                    otherReq = other;
                    otherDst = dst;
                }
            }
        });

        if(otherReq != null){
            drawBridge(req.rotation, req.drawx(), req.drawy(), otherReq.drawx(), otherReq.drawy());
        }
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, dirRegion};
    }

    @Override
    public void changePlacementPath(Seq<Point2> points, int rotation){
        Placement.calculateNodes(points, this, rotation, (point, other) -> Math.max(Math.abs(point.x - other.x), Math.abs(point.y - other.y)) <= range);
    }

    public void drawPlace(int x, int y, int rotation, boolean valid, boolean line){
        int length = range;
        Building found = null;
        int dx = Geometry.d4x(rotation), dy = Geometry.d4y(rotation);

        //find the link
        for(int i = 1; i <= range; i++){
            Tile other = world.tile(x + dx * i, y + dy * i);

            if(other != null && other.build instanceof DuctBridgeBuild build && build.team == player.team()){
                length = i;
                found = other.build;
                break;
            }
        }

        if(line || found != null){
            Drawf.dashLine(Pal.placing,
            x * tilesize + dx * (tilesize / 2f + 2),
            y * tilesize + dy * (tilesize / 2f + 2),
            x * tilesize + dx * (length) * tilesize,
            y * tilesize + dy * (length) * tilesize
            );
        }

        if(found != null){
            if(line){
                Drawf.square(found.x, found.y, found.block.size * tilesize/2f + 2.5f, 0f);
            }else{
                Drawf.square(found.x, found.y, 2f);
            }
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        drawPlace(x, y, rotation, valid, true);
    }

    public void drawBridge(int rotation, float x1, float y1, float x2, float y2){
        Draw.alpha(Renderer.bridgeOpacity);
        float
        angle = Angles.angle(x1, y1, x2, y2),
        cx = (x1 + x2)/2f,
        cy = (y1 + y2)/2f,
        len = Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2)) - size * tilesize;

        Draw.rect(bridgeRegion, cx, cy, len, tilesize, angle);
        Draw.color(0.4f, 0.4f, 0.4f, 0.4f * Renderer.bridgeOpacity);
        Draw.rect(bridgeBotRegion, cx, cy, len, tilesize, angle);
        Draw.reset();
        Draw.alpha(Renderer.bridgeOpacity);

        for(float i = 6f; i <= len + size * tilesize - 5f; i += 5f){
            Draw.rect(arrowRegion, x1 + Geometry.d4x(rotation) * i, y1 + Geometry.d4y(rotation) * i, angle);
        }

        Draw.reset();
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
                drawBridge(rotation, x, y, link.x, link.y);
            }
        }

        @Override
        public void drawSelect(){
            drawPlace(tile.x, tile.y, rotation, true, false);
            //draw incoming bridges
            for(int dir = 0; dir < 4; dir++){
                if(dir != rotation){
                    int dx = Geometry.d4x(dir), dy = Geometry.d4y(dir);
                    int length = range;
                    Building found = null;

                    //find the link
                    for(int i = 1; i <= range; i++){
                        Tile other = world.tile(tile.x + dx * i, tile.y + dy * i);

                        if(other != null && other.build instanceof DuctBridgeBuild build && build.team == player.team() && (build.rotation + 2) % 4 == dir){
                            length = i;
                            found = other.build;
                            break;
                        }
                    }

                    if(found != null){
                        Drawf.dashLine(Pal.place,
                        found.x - dx * (tilesize / 2f + 2),
                        found.y - dy * (tilesize / 2f + 2),
                        found.x - dx * (length) * tilesize,
                        found.y - dy * (length) * tilesize
                        );

                        Drawf.square(found.x, found.y, 2f, 45f, Pal.place);
                    }
                }
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
            int rel = this.relativeToEdge(source.tile);
            return items.total() < itemCapacity && rel != rotation && occupied[(rel + 2) % 4] == null;
        }
    }
}
