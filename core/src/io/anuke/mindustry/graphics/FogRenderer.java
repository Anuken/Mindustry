package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.game.EventType.WorldLoadGraphicsEvent;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.entities.EntityDraw;
import io.anuke.ucore.graphics.ClipSpriteBatch;
import io.anuke.ucore.graphics.Draw;

import static io.anuke.mindustry.Vars.*;

/**Used for rendering fog of war. A framebuffer is used for this.*/
public class FogRenderer implements Disposable{
    private TextureRegion region = new TextureRegion();
    private FrameBuffer buffer;

    public FogRenderer(){
        Events.on(WorldLoadGraphicsEvent.class, () -> {
            dispose();
            buffer = new FrameBuffer(Format.RGBA8888, world.width(), world.height(), false);

            //clear buffer to black
            buffer.begin();
            Graphics.clear(Color.BLACK);
            buffer.end();
        });
    }

    public void draw(){
        if(buffer == null) return;

        float vw = Core.camera.viewportWidth * Core.camera.zoom;
        float vh = Core.camera.viewportHeight * Core.camera.zoom;

        float px = Core.camera.position.x -= vw/2f;
        float py = Core.camera.position.y -= vh/2f;

        float u = px / tilesize / world.width();
        float v = py / tilesize / world.height();

        float u2 = (px + vw)/ tilesize / world.width();
        float v2 = (py + vh)/ tilesize / world.height();

        if(Core.batch instanceof ClipSpriteBatch){
            ((ClipSpriteBatch) Core.batch).enableClip(false);
        }

        Core.batch.getProjectionMatrix().setToOrtho2D(0, 0, world.width() * tilesize, world.height() * tilesize);

        Draw.color(Color.WHITE);

        buffer.begin();
        Graphics.begin();
        EntityDraw.setClip(false);

        renderer.drawAndInterpolate(playerGroup, player -> player.getTeam() == players[0].getTeam(), Unit::drawView);
        renderer.drawAndInterpolate(unitGroups[players[0].getTeam().ordinal()], unit -> true, Unit::drawView);

        EntityDraw.setClip(true);
        Graphics.end();
        buffer.end();

        region.setTexture(buffer.getColorBufferTexture());
        //region.setRegion(0, 0, 1, 1);
        region.setRegion(u, v2, u2, v);

        Core.batch.setProjectionMatrix(Core.camera.combined);
        Graphics.shader(Shaders.fog);
        renderer.pixelSurface.getBuffer().begin();
        Graphics.begin();

       // Core.batch.draw(buffer.getColorBufferTexture(), px + 50, py, 200, 200 * world.height()/(float)world.width());
        Core.batch.draw(region, px, py, vw, vh);

        Graphics.end();
        renderer.pixelSurface.getBuffer().end();
        Graphics.shader();

        Graphics.begin();

        Core.batch.draw(renderer.pixelSurface.texture(), px, py + vh, vw, -vh);
        Graphics.end();

        if(Core.batch instanceof ClipSpriteBatch){
            ((ClipSpriteBatch) Core.batch).enableClip(true);
        }

    }

    public Texture getTexture(){
        return buffer.getColorBufferTexture();
    }

    @Override
    public void dispose() {
        if(buffer != null) buffer.dispose();
    }
}
