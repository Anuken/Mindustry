package mindustry.graphics;

import arc.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;

import static mindustry.Vars.*;

public class MenuRenderer implements Disposable{
    private static final float darkness = 0.3f;
    private final int width = !mobile ? 100 : 60, height = !mobile ? 50 : 40;

    private int cacheFloor, cacheWall;
    private Camera camera = new Camera();
    private Mat mat = new Mat();
    private FrameBuffer shadows;
    private CacheBatch batch;
    private float time = 0f;
    private float flyerRot = 45f;
    private int flyers = Mathf.chance(0.2) ? Mathf.random(35) : Mathf.random(15);
    private UnitType flyerType = content.units().select(u -> u.hitSize <= 20f && u.flying && u.region.found()).random();

    public MenuRenderer(){
        Time.mark();
        generate();
        cache();
        Log.debug("Time to generate menu: @", Time.elapsed());
    }

    private void generate(){
        world.beginMapLoad();
        Tiles tiles = world.resize(width, height);
        Seq<Block> ores = content.blocks().select(b -> b instanceof OreBlock && !(b instanceof WallOreBlock));
        shadows = new FrameBuffer(width, height);
        int offset = Mathf.random(100000);
        int s1 = offset, s2 = offset + 1, s3 = offset + 2;
        Block[] selected = Structs.select(
            new Block[]{Blocks.sand, Blocks.sandWall},
            new Block[]{Blocks.shale, Blocks.shaleWall},
            new Block[]{Blocks.ice, Blocks.iceWall},
            new Block[]{Blocks.sand, Blocks.sandWall},
            new Block[]{Blocks.shale, Blocks.shaleWall},
            new Block[]{Blocks.ice, Blocks.iceWall},
            new Block[]{Blocks.moss, Blocks.sporePine}
        );
        Block[] selected2 = Structs.select(
            new Block[]{Blocks.basalt, Blocks.duneWall},
            new Block[]{Blocks.basalt, Blocks.duneWall},
            new Block[]{Blocks.stone, Blocks.stoneWall},
            new Block[]{Blocks.stone, Blocks.stoneWall},
            new Block[]{Blocks.moss, Blocks.sporeWall},
            new Block[]{Blocks.salt, Blocks.saltWall}
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

                if(Simplex.noise2d(s1, 3, 0.5, 1/20.0, x, y) > 0.5){
                    wall = walld;
                }

                if(Simplex.noise2d(s3, 3, 0.5, 1/20.0, x, y) > 0.5){
                    floor = floord2;
                    if(wall != Blocks.air){
                        wall = walld2;
                    }
                }

                if(Simplex.noise2d(s2, 3, 0.3, 1/30.0, x, y) > tr1){
                    ore = ore1;
                }

                if(Simplex.noise2d(s2, 2, 0.2, 1/15.0, x, y+99999) > tr2){
                    ore = ore2;
                }

                if(doheat){
                    double heat = Simplex.noise2d(s3, 4, 0.6, 1 / 50.0, x, y + 9999);
                    double base = 0.65;

                    if(heat > base){
                        ore = Blocks.air;
                        wall = Blocks.air;
                        floor = Blocks.basalt;

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
                    if(Simplex.noise2d(s1, 2, 1f / 10f, 0.5f, sclx, scly) > 0.4f && (mx == 0 || my == 0 || mx == secSize - 1 || my == secSize - 1)){
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
                    if(Ridged.noise2d(1 + offset, x, y, 1f / 17f) > 0f){
                        floor = Mathf.chance(0.2) ? Blocks.sporeMoss : Blocks.moss;

                        if(wall != Blocks.air){
                            wall = Blocks.sporeWall;
                        }
                    }
                }

                Tile tile;
                tiles.set(x, y, (tile = new CachedTile()));
                tile.x = (short)x;
                tile.y = (short)y;
                tile.setFloor(floor.asFloor());
                tile.setBlock(wall);
                tile.setOverlay(ore);
            }
        }

        world.endMapLoad();
    }

    private void cache(){

        //draw shadows
        Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());
        shadows.begin(Color.clear);
        Draw.color(Color.black);

        for(Tile tile : world.tiles){
            if(tile.block() != Blocks.air){
                Fill.rect(tile.x + 0.5f, tile.y + 0.5f, 1, 1);
            }
        }

        Draw.color();
        shadows.end();

        Batch prev = Core.batch;

        Core.batch = batch = new CacheBatch(new SpriteCache(width * height * 6, false));
        batch.beginCache();

        for(Tile tile : world.tiles){
            tile.floor().drawBase(tile);
        }

        for(Tile tile : world.tiles){
            tile.overlay().drawBase(tile);
        }

        cacheFloor = batch.endCache();
        batch.beginCache();

        for(Tile tile : world.tiles){
            tile.block().drawBase(tile);
        }

        cacheWall = batch.endCache();

        Core.batch = prev;
    }

    public void render(){
        time += Time.delta;
        float scaling = Math.max(Scl.scl(4f), Math.max(Core.graphics.getWidth() / ((width - 1f) * tilesize), Core.graphics.getHeight() / ((height - 1f) * tilesize)));
        camera.position.set(width * tilesize / 2f, height * tilesize / 2f);
        camera.resize(Core.graphics.getWidth() / scaling,
        Core.graphics.getHeight() / scaling);

        mat.set(Draw.proj());
        Draw.flush();
        Draw.proj(camera);
        batch.setProjection(camera.mat);
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

        TextureRegion icon = flyerType.fullIcon;

        float size = Math.max(icon.width, icon.height) * Draw.scl * 1.6f;

        flyers((x, y) -> {
            Draw.rect(icon, x - 12f, y - 13f, flyerRot - 90);
        });

        flyers((x, y) -> {
            Draw.rect("circle-shadow", x, y, size, size);
        });
        Draw.color();

        flyers((x, y) -> {
            float engineOffset = flyerType.engineOffset, engineSize = flyerType.engineSize, rotation = flyerRot;

            Draw.color(Pal.engine);
            Fill.circle(x + Angles.trnsx(rotation + 180, engineOffset), y + Angles.trnsy(rotation + 180, engineOffset),
            engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f));

            Draw.color(Color.white);
            Fill.circle(x + Angles.trnsx(rotation + 180, engineOffset - 1f), y + Angles.trnsy(rotation + 180, engineOffset - 1f),
            (engineSize + Mathf.absin(Time.time, 2f, engineSize / 4f)) / 2f);
            Draw.color();

            Draw.rect(icon, x, y, flyerRot - 90);
        });
    }

    private void flyers(Floatc2 cons){
        float tw = width * tilesize * 1f + tilesize;
        float th = height * tilesize * 1f + tilesize;
        float range = 500f;
        float offset = -100f;

        for(int i = 0; i < flyers; i++){
            Tmp.v1.trns(flyerRot, time * (flyerType.speed));

            cons.get(
            (Mathf.randomSeedRange(i, range) + Tmp.v1.x + Mathf.absin(time + Mathf.randomSeedRange(i + 2, 500), 10f, 3.4f) + offset) % (tw + Mathf.randomSeed(i + 5, 0, 500)),
            (Mathf.randomSeedRange(i + 1, range) + Tmp.v1.y + Mathf.absin(time + Mathf.randomSeedRange(i + 3, 500), 10f, 3.4f) + offset) % th
            );
        }
    }

    @Override
    public void dispose(){
        batch.dispose();
        shadows.dispose();
    }
}
