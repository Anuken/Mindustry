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
import mindustry.world.blocks.environment.*;
import mindustry.world.blocks.environment.Floor.*;
import mindustry.world.blocks.power.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class BlockRenderer{
    //TODO cracks take up far to much space, so I had to limit it to 7. this means larger blocks won't have cracks - draw tiling mirrored stuff instead?
    public static final int crackRegions = 8, maxCrackSize = 7, chunkSize = 30, maxSpritesPerCacheTile = 3;
    public static boolean drawQuadtreeDebug = false;
    public static final Color shadowColor = new Color(0, 0, 0, 0.71f), blendShadowColor = Color.white.cpy().lerp(Color.black, shadowColor.a);

    private static final int initialRequests = 32 * 32;

    public final FloorRenderer floor = new FloorRenderer();
    public TextureRegion[][] cracks;

    private IntSeq chunksToDraw = new IntSeq();
    private IntSet chunksToDrawSet = new IntSet();
    private Seq<Tile> tileview = new Seq<>(false, initialRequests, Tile.class);
    private Seq<Building> tileExtraCachedView = new Seq<>(false, initialRequests, Building.class);
    private Seq<Building> tileWithConsumerView = new Seq<>(false, initialRequests, Building.class);
    private Seq<Tile> lightview = new Seq<>(false, initialRequests, Tile.class);
    //TODO I don't like this system
    private Seq<UpdateRenderState> updateFloors = new Seq<>(UpdateRenderState.class);

    private boolean hadMapLimit;
    private int lastCamX, lastCamY, lastRangeX, lastRangeY;
    private Team lastTeam;
    private float brokenFade = 0f;
    private FrameBuffer shadows = new FrameBuffer();
    private FrameBuffer dark = new FrameBuffer();
    private Seq<Building> outArray2 = new Seq<>();
    private Seq<Tile> shadowEvents = new Seq<>();
    private IntSet darkEvents = new IntSet();
    private IntSet procLinks = new IntSet(), procLights = new IntSet();

    private BlockQuadtree blockTree = new BlockQuadtree(new Rect(0, 0, 1, 1));
    private BlockQuadtree blockCachedTree = new BlockQuadtree(new Rect(0, 0, 1, 1));
    private BlockLightQuadtree blockLightTree = new BlockLightQuadtree(new Rect(0, 0, 1, 1));
    private OverlayQuadtree overlayTree = new OverlayQuadtree(new Rect(0, 0, 1, 1));
    private FloorQuadtree floorTree = new FloorQuadtree(new Rect(0, 0, 1, 1));

    private CacheChunk[][] cacheChunks;
    private CacheBatch cbatch = new CacheBatch(null);
    private Seq<SpriteCache> caches = new Seq<>();
    private Seq<IntSeq> queuedCacheDraws = new Seq<>();
    private IntSeq queuedCacheIndices = new IntSeq();
    private IntSet dirtyChunks = new IntSet();

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
            reload();
        });

        //sometimes darkness gets disabled.
        Events.run(Trigger.newGame, () -> {
            if(hadMapLimit && !state.rules.limitMapArea){
                updateDarkness();
                renderer.minimap.updateAll();
            }
        });

        Events.on(TilePreChangeEvent.class, event -> {
            if(blockTree == null || floorTree == null || overlayTree == null) return;

            if(event.tile.block().drawCached){
                recacheBuilding(event.tile);
            }

            if(indexBlock(event.tile)){
                blockTree.remove(event.tile);
                blockLightTree.remove(event.tile);
            }
            if(indexBlockCached(event.tile)){
                blockCachedTree.remove(event.tile);
            }
            if(indexFloor(event.tile)) floorTree.remove(event.tile);
            if(indexOverlay(event.tile)) overlayTree.remove(event.tile);
        });

        Events.on(TileChangeEvent.class, event -> {
            boolean visible = event.tile.build == null || !event.tile.build.inFogTo(Vars.player.team());
            if(event.tile.build != null){
                event.tile.build.wasVisible = visible;
            }

            if(event.tile.block().drawCached){
                recacheBuilding(event.tile);
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

    public void recacheBuilding(Tile tile){
        if(cacheChunks == null) return;

        //TODO: recache that specific tile and not the whole cache
        CacheChunk chunk = cacheChunks[tile.x / chunkSize][tile.y / chunkSize];

        if(chunk == null) return;

        chunk.dirty = true;
        int packed = Point2.pack(tile.x / chunkSize, tile.y / chunkSize);
        //don't re-cache the chunk unless it was in view
        if(chunksToDrawSet.contains(packed)){
            dirtyChunks.add(packed);
        }
    }

    public void cacheChunk(int cx, int cy){
        int required = chunkSize * chunkSize * maxSpritesPerCacheTile;

        CacheChunk chunk = cacheChunks[cx][cy];
        if(chunk == null){
            chunk = cacheChunks[cx][cy] = new CacheChunk();
            if(caches.isEmpty() || caches.peek().getSpritesUsed() + required > caches.peek().getSpriteCapacity()){
                caches.add(new SpriteCache(16382, true));
                queuedCacheDraws.add(new IntSeq());
            }
            chunk.cache = caches.peek();
            chunk.spriteCacheIndex = caches.size - 1;
            chunk.cache.beginCache();
        }else{
            chunk.cache.beginCache(chunk.id);
        }

        cbatch.cache = chunk.cache;
        Batch lastBatch = Core.batch;
        try{
            Draw.flush();
            batch = cbatch;
            Team pteam = player.team();
            chunk.lastSeenTeam = pteam;
            int x1 = cx * chunkSize, y1 = cy * chunkSize, x2 = x1 + chunkSize, y2 = y1 + chunkSize;

            blockCachedTree.intersect(cx * chunkSize * tilesize, cy * chunkSize * tilesize, chunkSize * tilesize, chunkSize * tilesize, tile -> {
                //only draw blocks strictly inside the chunk
                if(!(tile.x >= x1 && tile.x < x2 && tile.y >= y1 && tile.y < y2) || !tile.block().drawCached) return;

                Block block = tile.block();
                Building build = tile.build;

                boolean visible = (build == null || !build.inFogTo(pteam));

                if(visible || build.wasVisible){
                    block.drawBaseCached(tile);
                    Draw.reset();
                    Draw.z(Layer.block);

                    if(build != null && build.team != pteam && build.block.drawTeamOverlay){
                        build.drawTeam();
                        Draw.z(Layer.block);
                    }
                    Draw.reset();
                }
            });
        }finally{
            Draw.flush();
            batch = lastBatch;
        }

        chunk.dirty = false;
        chunk.cache.reserve(required);
        chunk.id = chunk.cache.endCache();
    }

    public void reload(){
        blockTree = new BlockQuadtree(new Rect(0, 0, world.unitWidth(), world.unitHeight()));
        blockCachedTree = new BlockQuadtree(new Rect(0, 0, world.unitWidth(), world.unitHeight()));
        blockLightTree = new BlockLightQuadtree(new Rect(0, 0, world.unitWidth(), world.unitHeight()));
        overlayTree = new OverlayQuadtree(new Rect(0, 0, world.unitWidth(), world.unitHeight()));
        floorTree = new FloorQuadtree(new Rect(0, 0, world.unitWidth(), world.unitHeight()));

        for(SpriteCache cache : caches){
            cache.dispose();
        }
        caches.clear();
        int chunksx = Mathf.ceil((float)(world.width()) / chunkSize), chunksy = Mathf.ceil((float)(world.height()) / chunkSize);
        cacheChunks = new CacheChunk[chunksx][chunksy];

        shadowEvents.clear();
        updateFloors.clear();
        lastCamY = lastCamX = -99; //invalidate camera position so blocks get updated
        hadMapLimit = state.rules.limitMapArea;

        shadows.getTexture().setFilter(TextureFilter.linear, TextureFilter.linear);
        shadows.resize(world.width(), world.height());
        shadows.begin(Color.white);
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

            if(tile.block().displayShadow(tile) && (tile.build == null || tile.build.wasVisible)){
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
            }
        }

        Draw.flush();
        Draw.color();
        shadows.end();

        updateDarkness();
    }

    public void updateShadows(boolean ignoreBuildings, boolean ignoreTerrain){
        shadows.getTexture().setFilter(TextureFilter.linear, TextureFilter.linear);
        shadows.resize(world.width(), world.height());
        shadows.begin(Color.white);
        Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());

        Draw.color(blendShadowColor);

        for(Tile tile : world.tiles){
            if(tile.block().displayShadow(tile) && (tile.build == null || tile.build.wasVisible) && !(ignoreBuildings && !tile.block().isStatic()) && !(ignoreTerrain && tile.block().isStatic())){
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
            }
        }

        Draw.flush();
        Draw.color();
        shadows.end();
    }

    public void updateDarkness(){
        darkEvents.clear();
        dark.getTexture().setFilter(TextureFilter.linear);
        dark.resize(world.width(), world.height());
        //fill darkness with black when map area is limited
        dark.begin(state.rules.limitMapArea ? Color.black : Color.white);

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

    public FrameBuffer getShadowBuffer(){
        return shadows;
    }

    public void removeFloorIndex(Tile tile){
        if(indexFloor(tile)) floorTree.remove(tile);
    }

    public void addFloorIndex(Tile tile){
        if(indexFloor(tile)) floorTree.insert(tile);
    }

    public void removeOverlayIndex(Tile tile){
        if(indexOverlay(tile)) overlayTree.remove(tile);
    }

    public void addOverlayIndex(Tile tile){
        if(indexOverlay(tile)) overlayTree.insert(tile);
    }

    boolean indexBlock(Tile tile){
        var block = tile.block();
        return tile.isCenter() && block != Blocks.air && block.cacheLayer == CacheLayer.normal;
    }

    boolean indexBlockCached(Tile tile){
        return tile.isCenter() && tile.block().drawCached;
    }

    boolean indexOverlay(Tile tile){
        return !tile.block().obstructsLight && tile.overlay().emitLight && world.getDarkness(tile.x, tile.y) < 3;
    }

    boolean indexFloor(Tile tile){
        return !tile.block().obstructsLight && tile.floor().emitLight && world.getDarkness(tile.x, tile.y) < 3;
    }

    void recordIndex(Tile tile){
        if(indexBlock(tile)){
            blockTree.insert(tile);
            blockLightTree.insert(tile);
        }

        if(indexBlockCached(tile)) blockCachedTree.insert(tile);
        if(indexOverlay(tile)) overlayTree.insert(tile);
        if(indexFloor(tile)) floorTree.insert(tile);
    }

    public void recacheWall(Tile tile){
        for(int cx = tile.x - darkRadius; cx <= tile.x + darkRadius; cx++){
            for(int cy = tile.y - darkRadius; cy <= tile.y + darkRadius; cy++){
                Tile other = world.tile(cx, cy);
                if(other != null){
                    darkEvents.add(other.pos());
                    floor.recacheTile(other);
                    renderer.minimap.updatePixel(other);
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

        if(control.input.isPlacing() || control.input.isBreaking() || (control.input.isRebuildSelecting() && !scene.hasKeyboard())){
            brokenFade = Mathf.lerpDelta(brokenFade, 1f, 0.1f);
        }else{
            brokenFade = Mathf.lerpDelta(brokenFade, 0f, 0.1f);
        }

        if(brokenFade > 0.001f){
            for(BlockPlan plan : player.team().data().plans){
                Block b = plan.block;
                if(!camera.bounds(Tmp.r1).grow(tilesize * 2f).overlaps(Tmp.r2.setSize(b.size * tilesize).setCenter(plan.x * tilesize + b.offset, plan.y * tilesize + b.offset))) continue;

                Draw.alpha(0.33f * brokenFade);
                Draw.mixcol(Color.white, 0.2f + Mathf.absin(Time.globalTime, 6f, 0.2f));
                Draw.rect(b.fullIcon, plan.x * tilesize + b.offset, plan.y * tilesize + b.offset, b.rotate ? plan.rotation * 90 + plan.block.visualRotationOffset : 0f);
            }
            Draw.reset();
        }
    }

    public void processShadows(){
        processShadows(false, false);
    }

    public void processShadows(boolean ignoreBuildings, boolean ignoreTerrain){
        if(!shadowEvents.isEmpty()){
            Draw.flush();

            shadows.begin();
            Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());

            for(Tile tile : shadowEvents){
                if(tile == null) continue;
                //draw white/shadow color depending on blend
                Draw.color((!tile.block().displayShadow(tile) || (state.rules.fog && tile.build != null && !tile.build.wasVisible) || (ignoreBuildings && !tile.block().isStatic()) || (ignoreTerrain && tile.block().isStatic())) ? Color.white : blendShadowColor);
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
            }

            Draw.flush();
            Draw.color();
            shadows.end();
            shadowEvents.clear();

            Draw.proj(camera);
        }
    }

    public void drawShadows(){
        processShadows();

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


        if(avgx == lastCamX && avgy == lastCamY && lastRangeX == rangex && lastRangeY == rangey && lastTeam == player.team()){
            return;
        }

        chunksToDraw.clear();
        chunksToDrawSet.clear();
        lastTeam = player.team();
        tileview.clear();
        tileExtraCachedView.clear();
        tileWithConsumerView.clear();
        lightview.clear();
        procLinks.clear();
        procLights.clear();
        dirtyChunks.clear();

        var bounds = camera.bounds(Tmp.r3).grow(tilesize * 2f);

        //draw floor lights
        floorTree.intersect(bounds, lightview::add);
        overlayTree.intersect(bounds, lightview::add);

        blockLightTree.intersect(bounds, tile -> {
            if(tile.block().emitLight && (tile.build == null || procLights.add(tile.build.pos()))){
                lightview.add(tile);
            }
        });

        Team pteam = player.team();

        blockTree.intersect(bounds, tile -> {
            var build = tile.build;
            var block = tile.block();

            if(block.drawCached){
                int coords = Point2.pack(tile.x / chunkSize, tile.y / chunkSize);
                if(chunksToDrawSet.add(coords)){
                    chunksToDraw.add(coords);
                }
            }

            if(build != null && block.hasConsumers && build.team == pteam){
                tileWithConsumerView.add(build);
            }

            if(!block.drawDynamic){
                if(build != null) tileExtraCachedView.add(build);
                return;
            }

            if(build == null || procLinks.add(build.id)){
                tileview.add(tile);
            }

            if(build != null && build.power != null && build.power.links.size > 0){
                for(Building other : build.getPowerConnections(outArray2)){
                    if(other.block instanceof PowerNode && procLinks.add(other.id)){ //TODO need a generic way to render connections!
                        tileview.add(other.tile);
                    }
                }
            }
        });

        queuedCacheIndices.clear();

        for(var seq : queuedCacheDraws){
            seq.clear();
        }

        //begin by checking any stale/uncached chunks, and caching them if necessary
        chunksToDraw.each(xy -> {
            int cx = Point2.x(xy);
            int cy = Point2.y(xy);
            CacheChunk chunk = cacheChunks[cx][cy];
            if(chunk == null || chunk.dirty || chunk.lastSeenTeam != pteam){
                cacheChunk(cx, cy);
            }
            //in case it was null (it can never be null after caching)
            chunk = cacheChunks[cx][cy];

            //record the sprite cache IDs that will be drawn in queuedCacheIndices, and record the actual cache IDs of the respective sprite caches in queuedCacheDraws
            IntSeq cacheIDsToDraw = queuedCacheDraws.get(chunk.spriteCacheIndex);
            if(cacheIDsToDraw.isEmpty()){
                queuedCacheIndices.add(chunk.spriteCacheIndex);
            }
            cacheIDsToDraw.add(chunk.id);
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

        if(chunksToDraw.size > 0){

            Draw.draw(Layer.block, () -> {
                dirtyChunks.each(c -> cacheChunk(Point2.x(c), Point2.y(c)));
                dirtyChunks.clear();

                //minor optimization: don't transfer the matrix every time
                SpriteCache.getDefaultShader().bind();
                SpriteCache.getDefaultShader().setUniformMatrix4("u_projectionViewMatrix", camera.mat);

                if(false)
                queuedCacheIndices.each(spriteCacheIndex -> {
                    SpriteCache sprites = caches.get(spriteCacheIndex);
                    IntSeq cachesToDraw = queuedCacheDraws.get(spriteCacheIndex);
                    sprites.begin(false);
                    cachesToDraw.each(sprites::draw);
                    sprites.end();
                });
            });
        }

        //draw most tile stuff
        for(int i = 0; i < tileview.size; i++){
            Tile tile = tileview.items[i];
            Block block = tile.block();

            Building build = tile.build;

            Draw.z(Layer.block);

            boolean visible = (build == null || !build.inFogTo(pteam));

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
            }
        }

        //draw overlay of extra cached tiles (they otherwise wouldn't draw cracks / status overlays)
        for(int i = 0; i < tileExtraCachedView.size; i++){
            Building build = tileExtraCachedView.items[i];

            boolean visible = !build.inFogTo(pteam);

            if(visible || build.wasVisible){
                if(visible){
                    build.visibleFlags |= (1L << pteam.id);
                    if(!build.wasVisible){
                        build.wasVisible = true;
                        updateShadow(build);
                        renderer.minimap.update(build.tile);
                    }
                }

                if(build.damaged()){
                    Draw.z(Layer.blockCracks);
                    build.drawCracks();
                }
            }
        }

        if(renderer.drawStatus){
            for(int i = 0; i < tileWithConsumerView.size; i++){
                Building build = tileExtraCachedView.items[i];
                if(build.wasVisible){
                    //always guaranteed to be player team
                    build.drawStatus();
                }
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
                }

                if(!tile.block().obstructsLight){
                    Floor floor = tile.floor();
                    Floor overlay = tile.overlay();

                    if(!floor.obstructsLight && overlay.emitLight){
                        overlay.drawEnvironmentLight(tile);
                    }
                    if(floor.forceDrawLight || (!overlay.obstructsLight && floor.emitLight)){
                        floor.drawEnvironmentLight(tile);
                    }
                }
            }
        }

        if(drawQuadtreeDebug){
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

    public void updateShadowTile(Tile tile){
        shadowEvents.add(tile);
    }

    static class CacheChunk{
        SpriteCache cache;
        int spriteCacheIndex; //index of cache variable in list of global spritcaches
        int id;
        boolean dirty = true;
        Team lastSeenTeam;
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

    static class BlockLightQuadtree extends QuadTree<Tile>{

        public BlockLightQuadtree(Rect bounds){
            super(bounds);
        }

        @Override
        public void hitbox(Tile tile){
            var block = tile.block();
            tmp.setCentered(tile.worldx() + block.offset, tile.worldy() + block.offset, block.lightClipSize, block.lightClipSize);
        }

        @Override
        protected QuadTree<Tile> newChild(Rect rect){
            return new BlockLightQuadtree(rect);
        }
    }

    static class OverlayQuadtree extends QuadTree<Tile>{
        public OverlayQuadtree(Rect bounds){
            super(bounds);
        }

        @Override
        public void hitbox(Tile tile){
            var overlay = tile.overlay();
            tmp.setCentered(tile.worldx(), tile.worldy(), overlay.lightClipSize, overlay.lightClipSize);
        }

        @Override
        protected QuadTree<Tile> newChild(Rect rect){
            return new OverlayQuadtree(rect);
        }
    }

    static class FloorQuadtree extends QuadTree<Tile>{

        public FloorQuadtree(Rect bounds){
            super(bounds);
        }

        @Override
        public void hitbox(Tile tile){
            var floor = tile.floor();
            tmp.setCentered(tile.worldx(), tile.worldy(), floor.lightClipSize, floor.lightClipSize);
        }

        @Override
        protected QuadTree<Tile> newChild(Rect rect){
            return new FloorQuadtree(rect);
        }
    }

}
