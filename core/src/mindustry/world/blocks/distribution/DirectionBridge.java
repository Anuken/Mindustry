package mindustry.world.blocks.distribution;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.annotations.Annotations.*;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.world.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class DirectionBridge extends Block{
    private static BuildPlan otherReq;
    private int otherDst = 0;

    public @Load("@-bridge") TextureRegion bridgeRegion;
    public @Load("@-bridge-bottom") TextureRegion bridgeBotRegion;
    public @Load("@-bridge-liquid") TextureRegion bridgeLiquidRegion;
    public @Load("@-arrow") TextureRegion arrowRegion;
    public @Load("@-dir") TextureRegion dirRegion;

    public int range = 4;

    public DirectionBridge(String name){
        super(name);
        update = true;
        solid = true;
        rotate = true;
        group = BlockGroup.transportation;
        noUpdateDisabled = true;
        priority = TargetPriority.transport;
        envEnabled = Env.space | Env.terrestrial | Env.underwater;
        drawArrow = false;
        regionRotated1 = 1;
    }

    @Override
    public void init(){
        updateClipRadius((range + 0.5f) * tilesize);
        super.init();
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(dirRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
    }

    @Override
    public void drawPlanConfigTop(BuildPlan plan, Eachable<BuildPlan> list){
        otherReq = null;
        otherDst = range;
        Point2 d = Geometry.d4(plan.rotation);
        list.each(other -> {
            if(other.block == this && plan != other && Mathf.clamp(other.x - plan.x, -1, 1) == d.x && Mathf.clamp(other.y - plan.y, -1, 1) == d.y){
                int dst = Math.max(Math.abs(other.x - plan.x), Math.abs(other.y - plan.y));
                if(dst <= otherDst){
                    otherReq = other;
                    otherDst = dst;
                }
            }
        });

        if(otherReq != null){
            drawBridge(plan.rotation, plan.drawx(), plan.drawy(), otherReq.drawx(), otherReq.drawy(), null);
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

            if(other != null && other.build instanceof DirectionBridgeBuild build && build.block == this && build.team == player.team()){
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

    public void drawBridge(int rotation, float x1, float y1, float x2, float y2, @Nullable Color liquidColor){
        Draw.alpha(Renderer.bridgeOpacity);
        float
        angle = Angles.angle(x1, y1, x2, y2),
        cx = (x1 + x2)/2f,
        cy = (y1 + y2)/2f,
        len = Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2)) - size * tilesize;

        Draw.rect(bridgeRegion, cx, cy, len, bridgeRegion.height * Draw.scl, angle);
        if(liquidColor != null){
            Draw.color(liquidColor, liquidColor.a * Renderer.bridgeOpacity);
            Draw.rect(bridgeLiquidRegion, cx, cy, len, bridgeLiquidRegion.height * Draw.scl, angle);
            Draw.color();
            Draw.alpha(Renderer.bridgeOpacity);
        }
        if(bridgeBotRegion.found()){
            Draw.color(0.4f, 0.4f, 0.4f, 0.4f * Renderer.bridgeOpacity);
            Draw.rect(bridgeBotRegion, cx, cy, len, bridgeBotRegion.height * Draw.scl, angle);
            Draw.reset();
        }
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

    public class DirectionBridgeBuild extends Building{
        public Building[] occupied = new Building[4];

        @Override
        public void draw(){
            Draw.rect(block.region, x, y);
            Draw.rect(dirRegion, x, y, rotdeg());
            var link = findLink();
            if(link != null){
                Draw.z(Layer.power - 1);
                drawBridge(rotation, x, y, link.x, link.y, null);
            }
        }

        @Override
        public void drawSelect(){
            drawPlace(tile.x, tile.y, rotation, true, false);
            //draw incoming bridges
            for(int dir = 0; dir < 4; dir++){
                if(dir != rotation){
                    int dx = Geometry.d4x(dir), dy = Geometry.d4y(dir);
                    Building found = occupied[(dir + 2) % 4];

                    if(found != null){
                        int length = Math.max(Math.abs(found.tileX() - tileX()), Math.abs(found.tileY() - tileY()));
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
        public DirectionBridgeBuild findLink(){
            for(int i = 1; i <= range; i++){
                Tile other = tile.nearby(Geometry.d4x(rotation) * i, Geometry.d4y(rotation) * i);
                if(other != null && other.build instanceof DirectionBridgeBuild build && build.block == DirectionBridge.this && build.team == team){
                    return build;
                }
            }
            return null;
        }
    }
}
