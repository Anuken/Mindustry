package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class BlockRenderer{
    public static final int crackRegions = 8, maxCrackSize = 9;

    private static final int initialRequests = 32 * 32;
    private static final Color shadowColor = new Color(0, 0, 0, 0.71f), blendShadowColor = Color.white.cpy().lerp(Color.black, shadowColor.a);

    public final FloorRenderer floor = new FloorRenderer();
    public TextureRegion[][] cracks;

    private Seq<Tile> tileview = new Seq<>(false, initialRequests, Tile.class);
    private Seq<Tile> lightview = new Seq<>(false, initialRequests, Tile.class);

    private int lastCamX, lastCamY, lastRangeX, lastRangeY;
    private float brokenFade = 0f;
    private FrameBuffer shadows = new FrameBuffer();
    private FrameBuffer dark = new FrameBuffer();
    private Seq<Building> outArray2 = new Seq<>();
    private Seq<Tile> shadowEvents = new Seq<>();
    private IntSet darkEvents = new IntSet();
    private IntSet procLinks = new IntSet(), procLights = new IntSet();

    private BlockQuadtree blockTree;
    private FloorQuadtree floorTree;

    public BlockRenderer(){

        Events.on(ClientLoadEvent.class, e -> {
            cracks = new TextureRegion[maxCrackSize][crackRegions];
            for(int size = 1; size <= maxCrackSize; size++){
                for(int i = 0; i < crackRegions; i++){
                    cracks[size - 1][i] = Core.atlas.find("cracks-" + size + "-" + i);
                }
            }
        });

        Events.on(WorldLoadEvent.class, event -> {
            blockTree = new BlockQuadtree(new Rect(0, 0, world.unitWidth(), world.unitHeight()));
            floorTree = new FloorQuadtree(new Rect(0, 0, world.unitWidth(), world.unitHeight()));
            shadowEvents.clear();
            lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated

            shadows.getTexture().setFilter(TextureFilter.linear, TextureFilter.linear);
            shadows.resize(world.width(), world.height());
            shadows.begin();
            Core.graphics.clear(Color.white);
            Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());

            Draw.color(blendShadowColor);

            for(Tile tile : world.tiles){
                if(tile.block().hasShadow){
                    Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                }
            }

            Draw.flush();
            Draw.color();
            shadows.end();

            dark.getTexture().setFilter(TextureFilter.linear);
            dark.resize(world.width(), world.height());
            dark.begin();
            Core.graphics.clear(Color.white);
            Draw.proj().setOrtho(0, 0, dark.getWidth(), dark.getHeight());

            for(Tile tile : world.tiles){
                recordIndex(tile);

                float darkness = world.getDarkness(tile.x, tile.y);

                if(darkness > 0){
                    Draw.colorl(1f - Math.min((darkness + 0.5f) / 4f, 1f));
                    Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                }
            }

            Draw.flush();
            Draw.color();
            dark.end();
        });

        Events.on(TilePreChangeEvent.class, event -> {
            if(indexBlock(event.tile)) blockTree.remove(event.tile);
            if(indexFloor(event.tile)) floorTree.remove(event.tile);
        });

        Events.on(TileChangeEvent.class, event -> {
            shadowEvents.add(event.tile);

            int avgx = (int)(camera.position.x / tilesize);
            int avgy = (int)(camera.position.y / tilesize);
            int rangex = (int)(camera.width / tilesize / 2) + 2;
            int rangey = (int)(camera.height / tilesize / 2) + 2;

            if(Math.abs(avgx - event.tile.x) <= rangex && Math.abs(avgy - event.tile.y) <= rangey){
                lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated
            }

            recordIndex(event.tile);
        });
    }

    boolean indexBlock(Tile tile){
        var block = tile.block();
        return tile.isCenter() && block != Blocks.air && block.cacheLayer == CacheLayer.normal;
    }

    boolean indexFloor(Tile tile){
        return tile.block() == Blocks.air && tile.floor().emitLight && world.getDarkness(tile.x, tile.y) < 3;
    }

    void recordIndex(Tile tile){
        if(indexBlock(tile)) blockTree.insert(tile);
        if(indexFloor(tile)) floorTree.insert(tile);
    }

    public void recacheWall(Tile tile){
        for(int cx = tile.x - darkRadius; cx <= tile.x + darkRadius; cx++){
            for(int cy = tile.y - darkRadius; cy <= tile.y + darkRadius; cy++){
                Tile other = world.tile(cx, cy);
                if(other != null){
                    darkEvents.add(other.pos());
                    floor.recacheTile(other);
                }
            }
        }
    }

    public void checkChanges(){
        darkEvents.each(pos -> {
            var tile = world.tile(pos);
            if(tile != null){
                tile.data = world.getWallDarkness(tile);
            }
        });
    }

    public void drawDarkness(){
        if(!darkEvents.isEmpty()){
            Draw.flush();

            dark.begin();
            Draw.proj().setOrtho(0, 0, dark.getWidth(), dark.getHeight());

            darkEvents.each(pos -> {
                var tile = world.tile(pos);
                if(tile == null) return;
                float darkness = world.getDarkness(tile.x, tile.y);
                //then draw the shadow
                Draw.colorl(darkness <= 0f ? 1f : 1f - Math.min((darkness + 0.5f) / 4f, 1f));
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
            });

            Draw.flush();
            Draw.color();
            dark.end();
            darkEvents.clear();

            Draw.proj(camera);
        }

        Draw.shader(Shaders.darkness);
        Draw.fbo(dark, world.width(), world.height(), tilesize);
        Draw.shader();
    }

    public void drawDestroyed(){
        if(!Core.settings.getBool("destroyedblocks")) return;

        if(control.input.isPlacing() || control.input.isBreaking()){
            brokenFade = Mathf.lerpDelta(brokenFade, 1f, 0.1f);
        }else{
            brokenFade = Mathf.lerpDelta(brokenFade, 0f, 0.1f);
        }

        if(brokenFade > 0.001f){
            for(BlockPlan block : player.team().data().blocks){
                Block b = content.block(block.block);
                if(!camera.bounds(Tmp.r1).grow(tilesize * 2f).overlaps(Tmp.r2.setSize(b.size * tilesize).setCenter(block.x * tilesize + b.offset, block.y * tilesize + b.offset))) continue;

                Draw.alpha(0.33f * brokenFade);
                Draw.mixcol(Color.white, 0.2f + Mathf.absin(Time.globalTime, 6f, 0.2f));
                Draw.rect(b.fullIcon, block.x * tilesize + b.offset, block.y * tilesize + b.offset, b.rotate ? block.rotation * 90 : 0f);
            }
            Draw.reset();
        }
    }

    public void drawShadows(){
        if(!shadowEvents.isEmpty()){
            Draw.flush();

            shadows.begin();
            Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());

            for(Tile tile : shadowEvents){
                //draw white/shadow color depending on blend
                Draw.color(!tile.block().hasShadow ? Color.white : blendShadowColor);
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
            }

            Draw.flush();
            Draw.color();
            shadows.end();
            shadowEvents.clear();

            Draw.proj(camera);
        }

        float ww = world.width() * tilesize, wh = world.height() * tilesize;
        float x = camera.position.x + tilesize / 2f, y = camera.position.y + tilesize / 2f;
        float u = (x - camera.width / 2f) / ww,
        v = (y - camera.height / 2f) / wh,
        u2 = (x + camera.width / 2f) / ww,
        v2 = (y + camera.height / 2f) / wh;

        Tmp.tr1.set(shadows.getTexture());
        Tmp.tr1.set(u, v2, u2, v);

        Draw.shader(Shaders.darkness);
        Draw.rect(Tmp.tr1, camera.position.x, camera.position.y, camera.width, camera.height);
        Draw.shader();
    }

    /** Process all blocks to draw. */
    public void processBlocks(){
        int avgx = (int)(camera.position.x / tilesize);
        int avgy = (int)(camera.position.y / tilesize);

        int rangex = (int)(camera.width / tilesize / 2);
        int rangey = (int)(camera.height / tilesize / 2);

        if(avgx == lastCamX && avgy == lastCamY && lastRangeX == rangex && lastRangeY == rangey){
            return;
        }

        tileview.clear();
        lightview.clear();
        procLinks.clear();
        procLights.clear();

        var bounds = camera.bounds(Tmp.r3).grow(tilesize);

        //draw floor lights
        floorTree.intersect(bounds, tile -> lightview.add(tile));

        blockTree.intersect(bounds, tile -> {
            if(tile.build == null || procLinks.add(tile.build.id)){
                tileview.add(tile);
            }

            //lights are drawn even in the expanded range
            if(((tile.build != null && procLights.add(tile.build.pos())) || tile.block().emitLight)){
                lightview.add(tile);
            }

            if(tile.build != null && tile.build.power != null && tile.build.power.links.size > 0){
                for(Building other : tile.build.getPowerConnections(outArray2)){
                    if(other.block instanceof PowerNode && procLinks.add(other.id)){ //TODO need a generic way to render connections!
                        tileview.add(other.tile);
                    }
                }
            }
        });

        lastCamX = avgx;
        lastCamY = avgy;
        lastRangeX = rangex;
        lastRangeY = rangey;
    }

    //debug method for drawing block bounds
    void drawTree(QuadTree<Tile> tree){
        Draw.color(Color.blue);
        Lines.rect(tree.bounds);

        Draw.color(Color.green);
        for(var tile : tree.objects){
            var block = tile.block();
            Tmp.r1.setCentered(tile.worldx() + block.offset, tile.worldy() + block.offset, block.clipSize, block.clipSize);
            Lines.rect(Tmp.r1);
        }

        if(!tree.leaf){
            drawTree(tree.botLeft);
            drawTree(tree.botRight);
            drawTree(tree.topLeft);
            drawTree(tree.topRight);
        }
        Draw.reset();
    }

    public void drawBlocks(){

        drawDestroyed();

        //draw most tile stuff
        for(int i = 0; i < tileview.size; i++){
            Tile tile = tileview.items[i];
            Block block = tile.block();
            Building entity = tile.build;

            Draw.z(Layer.block);

            if(block != Blocks.air){
                block.drawBase(tile);
                Draw.reset();
                Draw.z(Layer.block);

                if(entity != null){
                    if(entity.damaged()){
                        entity.drawCracks();
                        Draw.z(Layer.block);
                    }

                    if(entity.team != player.team()){
                        entity.drawTeam();
                        Draw.z(Layer.block);
                    }

                    if(entity.team == player.team() && renderer.drawStatus && block.consumes.any()){
                        entity.drawStatus();
                    }
                }
                Draw.reset();
            }
        }

        if(renderer.lights.enabled()){
            //draw lights
            for(int i = 0; i < lightview.size; i++){
                Tile tile = lightview.items[i];
                Building entity = tile.build;

                if(entity != null){
                    entity.drawLight();
                }else if(tile.block().emitLight){
                    tile.block().drawEnvironmentLight(tile);
                }else if(tile.floor().emitLight && tile.block() == Blocks.air){ //only draw floor light under non-solid blocks
                    tile.floor().drawEnvironmentLight(tile);
                }
            }
        }
    }

    static class BlockQuadtree extends QuadTree<Tile>{

        public BlockQuadtree(Rect bounds){
            super(bounds);
        }

        @Override
        public void hitbox(Tile tile){
            var block = tile.block();
            tmp.setCentered(tile.worldx() + block.offset, tile.worldy() + block.offset, block.clipSize, block.clipSize);
        }

        @Override
        protected QuadTree<Tile> newChild(Rect rect){
            return new BlockQuadtree(rect);
        }
    }

    static class FloorQuadtree extends QuadTree<Tile>{

        public FloorQuadtree(Rect bounds){
            super(bounds);
        }

        @Override
        public void hitbox(Tile tile){
            var floor = tile.floor();
            tmp.setCentered(tile.worldx(), tile.worldy(), floor.clipSize, floor.clipSize);
        }

        @Override
        protected QuadTree<Tile> newChild(Rect rect){
            return new FloorQuadtree(rect);
        }
    }

}
