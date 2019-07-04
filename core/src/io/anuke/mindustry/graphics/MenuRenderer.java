package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.glutils.FrameBuffer;
import io.anuke.arc.math.Matrix3;
import io.anuke.arc.util.*;
import io.anuke.arc.util.noise.Simplex;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.world.*;
import io.anuke.mindustry.world.blocks.Floor;

import static io.anuke.mindustry.Vars.*;

public class MenuRenderer implements Disposable{
    private static final int width = 100, height = 50;
    private static final float darkness = 0.1f;

    private int cacheFloor, cacheWall;
    private Camera camera = new Camera();
    private Matrix3 mat = new Matrix3();
    private FrameBuffer shadows;
    private CacheBatch batch;

    public MenuRenderer(){
        Time.mark();
        generate();
        cache();
        Log.info("Time to generate menu: {0}", Time.elapsed());
    }

    private void generate(){
        Tile[][] tiles = world.createTiles(width, height);
        shadows = new FrameBuffer(width, height);
        Simplex s1 = new Simplex(0);
        Simplex s2 = new Simplex(1);
        Simplex s3 = new Simplex(2);

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Block floor = Blocks.shale;
                Block ore = Blocks.air;
                Block wall = Blocks.air;

                if(s1.octaveNoise2D(3, 0.5, 1/20.0, x, y) > 0.5){
                    wall = Blocks.shaleRocks;
                }

                if(s3.octaveNoise2D(3, 0.5, 1/20.0, x, y) > 0.5){
                    floor = Blocks.stone;
                    if(wall != Blocks.air){
                        wall = Blocks.rocks;
                    }
                }

                if(s2.octaveNoise2D(3, 0.3, 1/30.0, x, y) > 0.5){
                    ore = Blocks.oreCopper;
                }

                if(s2.octaveNoise2D(2, 0.2, 1/15.0, x, y+99999) > 0.7){
                    ore = Blocks.oreLead;
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
        shadows.beginDraw(Color.CLEAR);
        Draw.color(Color.BLACK);
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

        //Draw.rect("error", world.width() * tilesize/2f, world.height() * tilesize/2f, 100f, 100f);

        cacheWall = batch.endCache();

        Core.batch = prev;
    }

    public void render(){
        float scaling = 4f;
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
        Draw.rect(Draw.wrap(shadows.getTexture()),
        width * tilesize / 2f - 4f, height * tilesize / 2f - 4f,
        width * tilesize, -height * tilesize);
        Draw.flush();
        batch.beginDraw();
        batch.drawCache(cacheWall);
        batch.endDraw();

        Draw.proj(mat);
        Draw.color(0f, 0f, 0f, darkness);
        Fill.crect(0, 0, Core.graphics.getWidth(), Core.graphics.getHeight());
        Draw.color();
    }

    @Override
    public void dispose(){
        batch.dispose();
        shadows.dispose();
    }
}
