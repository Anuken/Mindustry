package mindustry.graphics;

import arc.Core;
import arc.graphics.Blending;
import arc.graphics.Texture.TextureFilter;
import arc.graphics.g2d.Draw;
import arc.graphics.gl.FrameBuffer;
import arc.util.Disposable;
import mindustry.entities.type.Player;

import static arc.Core.camera;
import static arc.Core.graphics;
import static mindustry.Vars.playerGroup;
import static mindustry.Vars.renderer;

public class Pixelator implements Disposable{
    private FrameBuffer buffer = new FrameBuffer(2, 2);

    {
        buffer.getTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
    }

    public void drawPixelate(){
        float pre = renderer.getScale();
        float scale = renderer.getScale();
        scale = (int)scale;
        renderer.setScale(scale);
        camera.width = (int)camera.width;
        camera.height = (int)camera.height;

        graphics.clear(0f, 0f, 0f, 1f);

        float px = Core.camera.position.x, py = Core.camera.position.y;
        Core.camera.position.set((int)px + ((int)(camera.width) % 2 == 0 ? 0 : 0.5f), (int)py + ((int)(camera.height) % 2 == 0 ? 0 : 0.5f));

        int w = (int)(Core.camera.width * renderer.landScale());
        int h = (int)(Core.camera.height * renderer.landScale());

        if(!graphics.isHidden() && (buffer.getWidth() != w || buffer.getHeight() != h)){
            buffer.resize(w, h);
        }

        Draw.flush();
        buffer.begin();
        renderer.draw();

        Draw.flush();
        buffer.end();

        Draw.blend(Blending.disabled);
        Draw.rect(Draw.wrap(buffer.getTexture()), Core.camera.position.x, Core.camera.position.y, Core.camera.width, -Core.camera.height);
        Draw.blend();

        playerGroup.draw(p -> !p.isDead(), Player::drawName);

        Core.camera.position.set(px, py);
        renderer.setScale(pre);
    }

    public boolean enabled(){
        return Core.settings.getBool("pixelate");
    }

    @Override
    public void dispose(){
        buffer.dispose();
    }
}
