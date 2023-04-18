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
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.Floor.*;
import mindustry.world.blocks.power.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class BlockRenderer{
    //TODO cracks take up far to much space, so I had to limit it to 7. this means larger blocks won't have cracks - draw tiling mirrored stuff instead?
    public static final int crackRegions = 8, maxCrackSize = 7;
    public static boolean drawQuadtreeDebug = false;
    public static final Color shadowColor = new Color(0, 0, 0, 0.71f), blendShadowColor = Color.white.cpy().lerp(Color.black, shadowColor.a);

    private static final int initialRequests = 32 * 32;

    public final FloorRenderer floor = new FloorRenderer();
    public TextureRegion[][] cracks;

    private Seq<Tile> tileview = new Seq<>(false, initialRequests, Tile.class);
    private Seq<Tile> lightview = new Seq<>(false, initialRequests, Tile.class);
    //TODO I don't like this system
    private Seq<UpdateRenderState> updateFloors = new Seq<>(UpdateRenderState.class);

    private boolean hadMapLimit;
    private int lastCamX, lastCamY, lastRangeX, lastRangeY;
    private float brokenFade = 0f;
    private FrameBuffer shadows = new FrameBuffer();
    private FrameBuffer dark = new FrameBuffer();
    private Seq<Building> outArray2 = new Seq<>();
    private Seq<Tile> shadowEvents = new Seq<>();
    private IntSet darkEvents = new IntSet();
    private IntSet procLinks = new IntSet(), procLights = new IntSet();

    private BlockQuadtree blockTree = new BlockQuadtree(new Rect(0, 0, 1, 1));
    private FloorQuadtree floorTree = new FloorQuadtree(new Rect(0, 0, 1, 1));

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
            updateFloors.clear();
            lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated
            hadMapLimit = state.rules.limitMapArea;

            shadows.getTexture().setFilter(TextureFilter.linear, TextureFilter.linear);
            shadows.resize(world.width(), world.height());
            shadows.begin();
            Core.graphics.clear(Color.white);
            Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());

            Draw.color(blendShadowColor);

            for(Tile tile : world.tiles){
                recordIndex(tile);

                if(tile.floor().updateRender(tile)){
                    updateFloors.add(new UpdateRenderState(tile, tile.floor()));
                }

                if(tile.overlay().updateRender(tile)){
                    updateFloors.add(new UpdateRenderState(tile, tile.overlay()));
                }

                if(tile.build != null && (tile.team() == player.team() || !state.rules.fog || (tile.build.visibleFlags & (1L << player.team().id)) != 0)){
                    tile.build.wasVisible = true;
                }

                if(tile.block().hasShadow && (tile.build == null || tile.build.wasVisible)){
                    Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
                }
            }

            Draw.flush();
            Draw.color();
            shadows.end();

            updateDarkness();
        });

        //sometimes darkness gets disabled.
        Events.run(Trigger.newGame, () -> {
            if(hadMapLimit && !state.rules.limitMapArea){
                updateDarkness();
                renderer.minimap.updateAll();
            }
        });

        Events.on(TilePreChangeEvent.class, event -> {
            if(blockTree == null || floorTree == null) return;

            if(indexBlock(event.tile)) blockTree.remove(event.tile);
            if(indexFloor(event.tile)) floorTree.remove(event.tile);
        });

        Events.on(TileChangeEvent.class, event -> {
            boolean visible = event.tile.build == null || !event.tile.build.inFogTo(Vars.player.team());
            if(event.tile.build != null){
                event.tile.build.wasVisible = visible;
            }

            if(visible){
                shadowEvents.add(event.tile);
            }

            int avgx = (int)(camera.position.x / tilesize);
            int avgy = (int)(camera.position.y / tilesize);
            int rangex = (int)(camera.width / tilesize / 2) + 2;
            int rangey = (int)(camera.height / tilesize / 2) + 2;

            if(Math.abs(avgx - event.tile.x) <= rangex && Math.abs(avgy - event.tile.y) <= rangey){
                lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated
            }

            invalidateTile(event.tile);
            recordIndex(event.tile);
        });
    }

    public void updateDarkness(){
        darkEvents.clear();
        dark.getTexture().setFilter(TextureFilter.linear);
        dark.resize(world.width(), world.height());
        dark.begin();

        //fill darkness with black when map area is limited
        Core.graphics.clear(state.rules.limitMapArea ? Color.black : Color.white);
        Draw.proj().setOrtho(0, 0, dark.getWidth(), dark.getHeight());

        //clear out initial starting area
        if(state.rules.limitMapArea){
            Draw.color(Color.white);
            Fill.crect(state.rules.limitX, state.rules.limitY, state.rules.limitWidth, state.rules.limitHeight);
        }

        for(Tile tile : world.tiles){
            //skip lighting outside rect
            if(state.rules.limitMapArea && !Rect.contains(state.rules.limitX, state.rules.limitY, state.rules.limitWidth - 1, state.rules.limitHeight - 1, tile.x, tile.y)){
                continue;
            }

            float darkness = world.getDarkness(tile.x, tile.y);

            if(darkness > 0){
                float dark = 1f - Math.min((darkness + 0.5f) / 4f, 1f);
                Draw.colorl(dark);
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
            }
        }

        Draw.flush();
        Draw.color();
        dark.end();
    }

    public void invalidateTile(Tile tile){
        int avgx = (int)(camera.position.x / tilesize);
        int avgy = (int)(camera.position.y / tilesize);
        int rangex = (int)(camera.width / tilesize / 2) + 3;
        int rangey = (int)(camera.height / tilesize / 2) + 3;

        if(Math.abs(avgx - tile.x) <= rangex && Math.abs(avgy - tile.y) <= rangey){
            lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated
        }
    }

    public void removeFloorIndex(Tile tile){
        if(indexFloor(tile)) floorTree.remove(tile);
    }

    public void addFloorIndex(Tile tile){
        if(indexFloor(tile)) floorTree.insert(tile);
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
            if(tile != null && tile.block().fillsTile){
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
        Draw.fbo(dark.getTexture(), world.width(), world.height(), tilesize, tilesize/2f);
        Draw.shader();
    }

    public void drawDestroyed(){
        if(!Core.settings.getBool("destroyedblocks")) return;

        if(control.input.isPlacing() || control.input.isBreaking() || control.input.isRebuildSelecting()){
            brokenFade = Mathf.lerpDelta(brokenFade, 1f, 0.1f);
        }else{
            brokenFade = Mathf.lerpDelta(brokenFade, 0f, 0.1f);
        }

        if(brokenFade > 0.001f){
            for(BlockPlan block : player.team().data().plans){
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
                if(tile == null) continue;
                //draw white/shadow color depending on blend
                Draw.color((!tile.block().hasShadow || (state.rules.fog && tile.build != null && !tile.build.wasVisible)) ? Color.white : blendShadowColor);
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

        if(!state.isPaused()){
            int updates = updateFloors.size;
            var uitems = updateFloors.items;
            for(int i = 0; i < updates; i++){
                var tile = uitems[i];
                tile.floor.renderUpdate(tile);
            }
        }


        if(avgx == lastCamX && avgy == lastCamY && lastRangeX == rangex && lastRangeY == rangey){
            return;
        }

        tileview.clear();
        lightview.clear();
        procLinks.clear();
        procLights.clear();

        var bounds = camera.bounds(Tmp.r3).grow(tilesize * 2f);

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
        Team pteam = player.team();

        drawDestroyed();

        //draw most tile stuff
        for(int i = 0; i < tileview.size; i++){
            Tile tile = tileview.items[i];
            Block block = tile.block();
            Building build = tile.build;

            Draw.z(Layer.block);

            boolean visible = (build == null || !build.inFogTo(pteam));

            //comment wasVisible part for hiding?
            if(block != Blocks.air && (visible || build.wasVisible)){
                block.drawBase(tile);
                Draw.reset();
                Draw.z(Layer.block);

                if(block.customShadow){
                    Draw.z(Layer.block - 1);
                    block.drawShadow(tile);
                    Draw.z(Layer.block);
                }

                if(build != null){
                    if(visible){
                        build.visibleFlags |= (1L << pteam.id);
                        if(!build.wasVisible){
                            build.wasVisible = true;
                            updateShadow(build);
                            renderer.minimap.update(tile);
                        }
                    }

                    if(build.damaged()){
                        Draw.z(Layer.blockCracks);
                        build.drawCracks();
                        Draw.z(Layer.block);
                    }

                    if(build.team != pteam){
                        if(build.block.drawTeamOverlay){
                            build.drawTeam();
                            Draw.z(Layer.block);
                        }
                    }else if(renderer.drawStatus && block.hasConsumers){
                        build.drawStatus();
                    }
                }
                Draw.reset();
            }else if(!visible){
                //TODO here is the question: should buildings you lost sight of remain rendered? if so, how should this information be stored?
                //uncomment lines below for buggy persistence
                //if(build.wasVisible) updateShadow(build);
                //build.wasVisible = false;
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

        if(drawQuadtreeDebug){
            //TODO remove
            Draw.z(Layer.overlayUI);
            Lines.stroke(1f, Color.green);

            blockTree.intersect(camera.bounds(Tmp.r1), tile -> {
                Lines.rect(tile.getHitbox(Tmp.r2));
            });

            Draw.reset();
        }
    }

    public void updateShadow(Building build){
        if(build.tile == null) return;
        int size = build.block.size, of = build.block.sizeOffset, tx = build.tile.x, ty = build.tile.y;

        for(int x = 0; x < size; x++){
            for(int y = 0; y < size; y++){
                shadowEvents.add(world.tile(x + tx + of, y + ty + of));
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
