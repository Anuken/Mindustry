package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Camera;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.glutils.FrameBuffer;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.util.Disposable;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class MenuRenderer implements Disposable{
    private static final int width = 30, height = 30;

    private int cacheFloor, cacheWall;
    private Camera camera = new Camera();
    private FrameBuffer shadows;
    private CacheBatch batch;

    public MenuRenderer(){
        generate();
        cache();
    }

    private void generate(){
        Tile[][] tiles = world.createTiles(width, height);
        shadows = new FrameBuffer(width, height);

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                Block floor = Blocks.metalFloor;
                Block ore = Blocks.oreCopper;
                Block wall = Mathf.chance(0.5) ? Blocks.air : Blocks.darkMetal;

                tiles[x][y] = new Tile(x, y, floor.id, ore.id, wall.id);
            }
        }
    }

    private void cache(){

        //draw shadows
        Draw.proj().setOrtho(0, 0, shadows.getWidth(), shadows.getHeight());
        shadows.beginDraw(Color.CLEAR);
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                if(world.rawTile(x, y).block() != Blocks.air){
                    Fill.rect(x + 0.5f, y + 0.5f, 1, 1);
                }
            }
        }
        shadows.endDraw();

        batch = new CacheBatch(new SpriteCache(width * height * 5, true));
        batch.beginCache();

        SpriteBatch prev = Core.batch;
        Core.batch = batch;

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
        camera.position.set(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f);
        camera.resize(Core.graphics.getWidth(), Core.graphics.getHeight());

        Draw.flush();
        batch.setProjection(camera.projection());
        batch.beginDraw();
        batch.drawCache(cacheFloor);
        batch.endDraw();
        Draw.rect(Draw.wrap(shadows.getTexture()), width * tilesize/2f, height * tilesize/2f, width * tilesize, height * tilesize);
        Draw.flush();
        batch.beginDraw();
        batch.drawCache(cacheWall);
        batch.endDraw();
    }

    @Override
    public void dispose(){
        batch.dispose();
        shadows.dispose();
    }
}
