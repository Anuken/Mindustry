package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.graphics.Pixmap.Format;
import io.anuke.arc.graphics.Pixmaps;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Fill;
import io.anuke.arc.graphics.g2d.ScissorStack;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.util.Disposable;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class MinimapRenderer implements Disposable{
    private static final int baseSize = 16;
    private final Array<Unit> units = new Array<>();
    private Pixmap pixmap;
    private Texture texture;
    private TextureRegion region;
    private Rectangle rect = new Rectangle(), scissor = new Rectangle();
    private int zoom = 4;

    public MinimapRenderer(){
        Events.on(WorldLoadEvent.class, event -> {
            reset();
            updateAll();
        });

        //make sure to call on the graphics thread
        Events.on(TileChangeEvent.class, event -> Core.app.post(() -> update(event.tile)));
    }

    public Texture getTexture(){
        return texture;
    }

    public void zoomBy(float amount){
        zoom += amount;
        zoom = Mathf.clamp(zoom, 1, Math.min(world.width(), world.height()) / baseSize / 2);
    }

    public float getZoom(){
        return zoom;
    }

    public void reset(){
        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
        }
        pixmap = new Pixmap(world.width(), world.height(), Format.RGBA8888);
        texture = new Texture(pixmap);
        region = new TextureRegion(texture);
    }

    public void drawEntities(float x, float y, float w, float h){
        updateUnitArray();

        int sz = baseSize * zoom;
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width() - sz);
        dy = Mathf.clamp(dy, sz, world.height() - sz);

        if(!ScissorStack.pushScissors(scissor.set(x, y, w, h))){
            return;
        }

        rect.set((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);

        for(Unit unit : units){
            float rx = (unit.x - rect.x) / rect.width * w, ry = (unit.y - rect.y) / rect.width * h;
            Draw.color(unit.getTeam().color);
            Fill.crect(x + rx, y + ry, w / (sz * 2), h / (sz * 2));
        }

        Draw.color();

        ScissorStack.popScissors();
    }

    public TextureRegion getRegion(){
        if(texture == null) return null;

        int sz = Mathf.clamp(baseSize * zoom, baseSize, Math.min(world.width(), world.height()));
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width() - sz);
        dy = Mathf.clamp(dy, sz, world.height() - sz);
        float invTexWidth = 1f / texture.getWidth();
        float invTexHeight = 1f / texture.getHeight();
        float x = dx - sz, y = world.height() - dy - sz, width = sz * 2, height = sz * 2;
        region.set(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight);
        return region;
    }

    public void updateAll(){
        for(int x = 0; x < world.width(); x++){
            for(int y = 0; y < world.height(); y++){
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, colorFor(world.tile(x, y)));
            }
        }
        texture.draw(pixmap, 0, 0);
    }

    public void update(Tile tile){
        int color = colorFor(world.tile(tile.x, tile.y));
        pixmap.drawPixel(tile.x, pixmap.getHeight() - 1 - tile.y, color);

        Pixmaps.drawPixel(texture, tile.x, pixmap.getHeight() - 1 - tile.y, color);
    }

    public void updateUnitArray(){
        int sz = baseSize * zoom;
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width() - sz);
        dy = Mathf.clamp(dy, sz, world.height() - sz);

        rect.set((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);
        units.clear();
        Units.getNearby(rect, units::add);
    }

    private int colorFor(Tile tile){
        tile = tile.target();
        return ColorMapper.colorFor(tile.floor(), tile.block(), tile.getTeam());
    }

    @Override
    public void dispose(){
        pixmap.dispose();
        texture.dispose();
        texture = null;
        pixmap = null;
    }
}
