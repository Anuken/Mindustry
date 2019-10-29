package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.function.PositionConsumer;
import io.anuke.arc.graphics.Camera;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.glutils.FrameBuffer;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.ui.layout.Scl;
import io.anuke.arc.util.*;
import io.anuke.arc.util.noise.RidgedPerlin;
import io.anuke.arc.util.noise.Simplex;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.UnitTypes;
import io.anuke.mindustry.type.UnitType;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.OreBlock;

import static io.anuke.mindustry.Vars.*;

public class MenuRenderer implements Disposable{
    private static final float darkness = 0.3f;
    private final int width = !mobile ? 100 : 60, height = !mobile ? 50 : 40;

    private int cacheFloor, cacheWall;
    private Camera camera = new Camera();
    private Matrix3 mat = new Matrix3();
    private FrameBuffer shadows;
    private CacheBatch batch;
    private float time = 0f;
    private float flyerRot = 45f;
    private int flyers = Mathf.chance(0.2) ? Mathf.random(35) : Mathf.random(15);
    private UnitType flyerType = Structs.select(UnitTypes.wraith, UnitTypes.wraith, UnitTypes.ghoul, UnitTypes.phantom, UnitTypes.phantom, UnitTypes.revenant);

    public MenuRenderer(){
        Time.mark();
        generate();
        cache();
        Log.info("Time to generate menu: {0}", Time.elapsed());
    }

    private void generate(){
        Tile[][] tiles = world.createTiles(width, height);
        Array<Block> ores = content.blocks().select(b -> b instanceof OreBlock);
        shadows = new FrameBuffer(width, height);
        int offset = Mathf.random(100000);
        Simplex s1 = new Simplex(offset);
        Simplex s2 = new Simplex(offset + 1);
        Simplex s3 = new Simplex(offset + 2);
        RidgedPerlin rid = new RidgedPerlin(1 + offset, 1);
        Block[] selected = Structs.select(
            new Block[]{Blocks.sand, Blocks.sandRocks},
            new Block[]{Blocks.shale, Blocks.shaleRocks},
            new Block[]{Blocks.ice, Blocks.icerocks},
            new Block[]{Blocks.sand, Blocks.sandRocks},
            new Block[]{Blocks.shale, Blocks.shaleRocks},
            new Block[]{Blocks.ice, Blocks.icerocks},
            new Block[]{Blocks.moss, Blocks.sporePine}
        );
        Block[] selected2 = Structs.select(
            new Block[]{Blocks.ignarock, Blocks.duneRocks},
            new Block[]{Blocks.ignarock, Blocks.duneRocks},
            new Block[]{Blocks.stone, Blocks.rocks},
            new Block[]{Blocks.stone, Blocks.rocks},
            new Block[]{Blocks.moss, Blocks.sporerocks},
            new Block[]{Blocks.salt, Blocks.saltRocks}
        );

        Block ore1 = ores.random();
        ores.remove(ore1);
        Block ore2 = ores.random();

        double tr1 = Mathf.random(0.65f, 0.85f);
        double tr2 = Mathf.random(0.65f, 0.85f);
        boolean doheat = Mathf.chance(0.25);
        boolean tendrils = Mathf.chance(0.25);
        boolean tech = Mathf.chance(0.25);
        int secSize = 10;

        Block floord = selected[0], walld = selected[1];
        Block floord2 = selected2[0], walld2 = selected2[1];

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Block floor = floord;
                Block ore = Blocks.air;
                Block wall = Blocks.air;

                if(s1.octaveNoise2D(3, 0.5, 1/20.0, x, y) > 0.5){
                    wall = walld;
                }

                if(s3.octaveNoise2D(3, 0.5, 1/20.0, x, y) > 0.5){
                    floor = floord2;
                    if(wall != Blocks.air){
                        wall = walld2;
                    }
                }

                if(s2.octaveNoise2D(3, 0.3, 1/30.0, x, y) > tr1){
                    ore = ore1;
                }

                if(s2.octaveNoise2D(2, 0.2, 1/15.0, x, y+99999) > tr2){
                    ore = ore2;
                }

                if(doheat){
                    double heat = s3.octaveNoise2D(4, 0.6, 1 / 50.0, x, y + 9999);
                    double base = 0.65;

                    if(heat > base){
                        ore = Blocks.air;
                        wall = Blocks.air;
                        floor = Blocks.ignarock;

                        if(heat > base + 0.1){
                            floor = Blocks.hotrock;

                            if(heat > base + 0.15){
                                floor = Blocks.magmarock;
                            }
                        }
                    }
                }

                if(tech){
                    int mx = x % secSize, my = y % secSize;
                    int sclx = x / secSize, scly = y / secSize;
                    if(s1.octaveNoise2D(2, 1f / 10f, 0.5f, sclx, scly) > 0.4f && (mx == 0 || my == 0 || mx == secSize - 1 || my == secSize - 1)){
                        floor = Blocks.darkPanel3;
                        if(Mathf.dst(mx, my, secSize/2, secSize/2) > secSize/2f + 1){
                            floor = Blocks.darkPanel4;
                        }


                        if(wall != Blocks.air && Mathf.chance(0.7)){
                            wall = Blocks.darkMetal;
                        }
                    }
                }

                if(tendrils){
                    if(rid.getValue(x, y, 1f / 17f) > 0f){
                        floor = Mathf.chance(0.2) ? Blocks.sporeMoss : Blocks.moss;

                        if(wall != Blocks.air){
                            wall = Blocks.sporerocks;
                        }
                    }
                }

                Tile tile;
                tiles[x][y] = (tile = new CachedTile());
                tile.x = (short)x;
                tile.y = (short)y;
                tile.setFloor((Floor) floor);
                tile.setBlock(wall);
                tile.setOverlay(ore);
            }
        }
    }

    private void cache(){

        //draw shadows
        Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());
        shadows.beginDraw(Color.clear);
        Draw.color(Color.black);
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                if(world.rawTile(x, y).block() != Blocks.air){
                    Fill.rect(x + 0.5f, y + 0.5f, 1, 1);
                }
            }
        }
        Draw.color();
        shadows.endDraw();

        SpriteBatch prev = Core.batch;

        Core.batch = batch = new CacheBatch(new SpriteCache(width * height * 6, false));
        batch.beginCache();

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Tile tile = world.rawTile(x, y);
                tile.floor().draw(tile);
            }
        }

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Tile tile = world.rawTile(x, y);
                if(tile.overlay() != Blocks.air){
                    tile.overlay().draw(tile);
                }
            }
        }

        cacheFloor = batch.endCache();
        batch.beginCache();

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Tile tile = world.rawTile(x, y);
                if(tile.block() != Blocks.air){
                    tile.block().draw(tile);
                }
            }
        }

        cacheWall = batch.endCache();

        Core.batch = prev;
    }

    public void render(){
        time += Time.delta();
        float scaling = Math.max(Scl.scl(4f), Math.max(Core.graphics.getWidth() / ((width - 1f) * tilesize), Core.graphics.getHeight() / ((height - 1f) * tilesize)));
        camera.position.set(width * tilesize / 2f, height * tilesize / 2f);
        camera.resize(Core.graphics.getWidth() / scaling,
        Core.graphics.getHeight() / scaling);

        mat.set(Draw.proj());
        Draw.flush();
        Draw.proj(camera.projection());
        batch.setProjection(camera.projection());
        batch.beginDraw();
        batch.drawCache(cacheFloor);
        batch.endDraw();
        Draw.color();
        Draw.rect(Draw.wrap(shadows.getTexture()),
        width * tilesize / 2f - 4f, height * tilesize / 2f - 4f,
        width * tilesize, -height * tilesize);
        Draw.flush();
        batch.beginDraw();
        batch.drawCache(cacheWall);
        batch.endDraw();

        drawFlyers();

        Draw.proj(mat);
        Draw.color(0f, 0f, 0f, darkness);
        Fill.crect(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        Draw.color();
    }

    private void drawFlyers(){
        Draw.color(0f, 0f, 0f, 0.4f);

        TextureRegion icon = flyerType.icon(Cicon.full);

        float size = Math.max(icon.getWidth(), icon.getHeight()) * Draw.scl * 1.6f;

        flyers((x, y) -> {
            Draw.rect(flyerType.region, x - 12f, y - 13f, flyerRot - 90);
        });

        flyers((x, y) -> {
            Draw.rect("circle-shadow", x, y, size, size);
        });
        Draw.color();

        flyers((x, y) -> {
            float engineOffset = flyerType.engineOffset, engineSize = flyerType.engineSize, rotation = flyerRot;

            Draw.color(Pal.engine);
            Fill.circle(x + Angles.trnsx(rotation + 180, engineOffset), y + Angles.trnsy(rotation + 180, engineOffset),
            engineSize + Mathf.absin(Time.time(), 2f, engineSize / 4f));

            Draw.color(Color.white);
            Fill.circle(x + Angles.trnsx(rotation + 180, engineOffset - 1f), y + Angles.trnsy(rotation + 180, engineOffset - 1f),
            (engineSize + Mathf.absin(Time.time(), 2f, engineSize / 4f)) / 2f);
            Draw.color();

            Draw.rect(flyerType.region, x, y, flyerRot - 90);
        });
    }

    private void flyers(PositionConsumer cons){
        float tw = width * tilesize * 1f + tilesize;
        float th = height * tilesize * 1f + tilesize;
        float range = 500f;
        float offset = -100f;

        for(int i = 0; i < flyers; i++){
            Tmp.v1.trns(flyerRot, time * (2f + flyerType.speed));

            cons.accept((Mathf.randomSeedRange(i, range) + Tmp.v1.x + Mathf.absin(time + Mathf.randomSeedRange(i + 2, 500), 10f, 3.4f) + offset) % (tw + Mathf.randomSeed(i + 5, 0, 500)),
            (Mathf.randomSeedRange(i + 1, range) + Tmp.v1.y + Mathf.absin(time + Mathf.randomSeedRange(i + 3, 500), 10f, 3.4f) + offset) % th);
        }
    }

    @Override
    public void dispose(){
        batch.dispose();
        shadows.dispose();
    }
}
