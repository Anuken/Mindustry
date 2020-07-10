package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.util.*;

import static arc.Core.*;
import static mindustry.Vars.renderer;

public class Pixelator implements Disposable{
    private FrameBuffer buffer = new FrameBuffer();
    private float px, py, pre;

    {
        buffer.getTexture().setFilter(TextureFilter.nearest, TextureFilter.nearest);
    }

    public void drawPixelate(){
        pre = renderer.getScale();
        float scale = renderer.getScale();
        scale = (int)scale;
        renderer.setScale(scale);
        camera.width = (int)camera.width;
        camera.height = (int)camera.height;

        px = Core.camera.position.x;
        py = Core.camera.position.y;
        Core.camera.position.set((int)px + ((int)(camera.width) % 2 == 0 ? 0 : 0.5f), (int)py + ((int)(camera.height) % 2 == 0 ? 0 : 0.5f));

        int w = (int)(Core.camera.width * renderer.landScale());
        int h = (int)(Core.camera.height * renderer.landScale());

        buffer.resize(w, h);

        buffer.begin(Color.clear);
        renderer.draw();
    }

    public void register(){
        Draw.draw(Layer.end, () -> {
            buffer.end();

            Blending.disabled.apply();
            buffer.blit(Shaders.screenspace);

            Core.camera.position.set(px, py);
            renderer.setScale(pre);
        });
    }

    public boolean enabled(){
        return Core.settings.getBool("pixelate");
    }

    @Override
    public void dispose(){
        buffer.dispose();
    }
}
