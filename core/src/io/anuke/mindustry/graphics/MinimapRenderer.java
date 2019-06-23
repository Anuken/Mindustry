package io.anuke.mindustry.graphics;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.Pixmap.Format;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.util.Disposable;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.type.Unit;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.io.MapIO;
import io.anuke.mindustry.world.Tile;

import static io.anuke.mindustry.Vars.tilesize;
import static io.anuke.mindustry.Vars.world;

public class MinimapRenderer implements Disposable{
    private static final float baseSize = 16f;
    private final Array<Unit> units = new Array<>();
    private Pixmap pixmap;
    private Texture texture;
    private TextureRegion region;
    private Rectangle rect = new Rectangle();
    private float zoom = 4;

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
        setZoom(zoom);
    }

    public void setZoom(float amount){
        zoom = Mathf.clamp(amount, 1f, Math.min(world.width(), world.height()) / baseSize / 2f);
    }

    public float getZoom(){
        return zoom;
    }

    public void reset(){
        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
        }
        setZoom(4f);
        pixmap = new Pixmap(world.width(), world.height(), Format.RGBA8888);
        texture = new Texture(pixmap);
        region = new TextureRegion(texture);
    }

    public void drawEntities(float x, float y, float w, float h){
        updateUnitArray();

        float sz = baseSize * zoom;
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width() - sz);
        dy = Mathf.clamp(dy, sz, world.height() - sz);

        rect.set((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);

        for(Unit unit : units){
            float rx = (unit.x - rect.x) / rect.width * w, ry = (unit.y - rect.y) / rect.width * h;
            Draw.color(unit.getTeam().color);
            Fill.rect(x + rx, y + ry, io.anuke.arc.scene.ui.layout.Unit.dp.scl(baseSize / 2f), io.anuke.arc.scene.ui.layout.Unit.dp.scl(baseSize / 2f));
        }

        Draw.color();
    }

    public TextureRegion getRegion(){
        if(texture == null) return null;

        float sz = Mathf.clamp(baseSize * zoom, baseSize, Math.min(world.width(), world.height()));
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
        float sz = baseSize * zoom;
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width() - sz);
        dy = Mathf.clamp(dy, sz, world.height() - sz);

        units.clear();
        Units.nearby((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize, units::add);
    }

    private int colorFor(Tile tile){
        tile = tile.link();
        return MapIO.colorFor(tile.floor(), tile.block(), tile.overlay(), tile.getTeam());
    }

    @Override
    public void dispose(){
        if(pixmap != null && texture != null){
            pixmap.dispose();
            texture.dispose();
            texture = null;
            pixmap = null;
        }
    }
}
