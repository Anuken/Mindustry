package mindustry.graphics;

import arc.*;
import arc.struct.*;
import arc.graphics.*;
import arc.graphics.Pixmap.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.ArcAnnotate.*;
import arc.util.pooling.*;
import mindustry.entities.*;
import mindustry.entities.type.*;
import mindustry.game.EventType.*;
import mindustry.io.*;
import mindustry.ui.*;
import mindustry.world.*;

import static mindustry.Vars.*;

public class MinimapRenderer implements Disposable{
    private static final float baseSize = 16f;
    private final Array<Unit> units = new Array<>();
    private Pixmap pixmap;
    private Texture texture;
    private TextureRegion region;
    private Rect rect = new Rect();
    private float zoom = 4;

    public MinimapRenderer(){
        Events.on(WorldLoadEvent.class, event -> {
            reset();
            updateAll();
        });

        //make sure to call on the graphics thread
        Events.on(TileChangeEvent.class, event -> Core.app.post(() -> update(event.tile)));
    }

    public Pixmap getPixmap(){
        return pixmap;
    }

    public @Nullable Texture getTexture(){
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

    public void drawEntities(float x, float y, float w, float h, float scaling, boolean withLabels){
        if(!withLabels){
            updateUnitArray();
        }else{
            units.clear();
            Units.all(units::add);
        }

        float sz = baseSize * zoom;
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width() - sz);
        dy = Mathf.clamp(dy, sz, world.height() - sz);

        rect.set((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);

        for(Unit unit : units){
            if(unit.isDead()) continue;
            float rx = !withLabels ? (unit.x - rect.x) / rect.width * w : unit.x / (world.width() * tilesize) * w;
            float ry = !withLabels ? (unit.y - rect.y) / rect.width * h : unit.y / (world.height() * tilesize) * h;

            Draw.mixcol(unit.getTeam().color, 1f);
            float scale = Scl.scl(1f) / 2f * scaling * 32f;
            Draw.rect(unit.getIconRegion(), x + rx, y + ry, scale, scale, unit.rotation - 90);
            Draw.reset();

            if(withLabels && unit instanceof Player){
                Player pl = (Player) unit;
                if(!pl.isLocal){
                    // Only display names for other players.
                    drawLabel(x + rx, y + ry, pl.name, unit.getTeam().color);
                }
            }
        }

        Draw.reset();
    }

    public void drawEntities(float x, float y, float w, float h){
        drawEntities(x, y, w, h, 1f, true);
    }

    public @Nullable TextureRegion getRegion(){
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
                pixmap.draw(x, pixmap.getHeight() - 1 - y, colorFor(world.tile(x, y)));
            }
        }
        texture.draw(pixmap, 0, 0);
    }

    public void update(Tile tile){
        int color = colorFor(world.tile(tile.x, tile.y));
        pixmap.draw(tile.x, pixmap.getHeight() - 1 - tile.y, color);

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
        if(tile == null) return 0;
        tile = tile.link();
        int bc = tile.block().minimapColor(tile);
        if(bc != 0){
            return bc;
        }
        return Tmp.c1.set(MapIO.colorFor(tile.floor(), tile.block(), tile.overlay(), tile.getTeam())).mul(tile.block().cacheLayer == CacheLayer.walls ? 1f - tile.rotation() / 4f : 1f).rgba();
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

    public void drawLabel(float x, float y, String text, Color color){
        BitmapFont font = Fonts.outline;
        GlyphLayout l = Pools.obtain(GlyphLayout.class, GlyphLayout::new);
        boolean ints = font.usesIntegerPositions();
        font.getData().setScale(1 / 1.5f / Scl.scl(1f));
        font.setUseIntegerPositions(false);

        l.setText(font, text, color, 90f, Align.left, true);
        float yOffset = 20f;
        float margin = 3f;

        Draw.color(0f, 0f, 0f, 0.2f);
        Fill.rect(x, y + yOffset - l.height/2f, l.width + margin, l.height + margin);
        Draw.color();
        font.setColor(color);
        font.draw(text, x - l.width/2f, y + yOffset, 90f, Align.left, true);
        font.setUseIntegerPositions(ints);

        font.getData().setScale(1f);

        Pools.free(l);
    }
}
