package mindustry.graphics;

import arc.*;
import arc.graphics.*;
import arc.graphics.Texture.*;
import arc.graphics.g2d.*;
import arc.graphics.gl.*;
import arc.util.*;
import mindustry.gen.*;

import static arc.Core.*;
import static mindustry.Vars.renderer;

public class Pixelator implements Disposable{
    private FrameBuffer buffer = new FrameBuffer();

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

        buffer.resize(w, h);

        buffer.begin();
        renderer.draw();
        buffer.end();

        Draw.blend(Blending.disabled);
        Draw.rect(buffer);
        Draw.blend();

        Groups.drawNames();

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
