package io.anuke.mindustry.graphics;

import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.CacheBatch;
import io.anuke.ucore.graphics.Draw;

import static io.anuke.mindustry.Vars.renderer;

public enum DrawLayer {
    water{
        @Override
        public void begin(CacheBatch batch){
            batch.setProjectionMatrix(Core.camera.combined);
            Graphics.useBatch(batch.drawBatch());

            Graphics.begin();
            Graphics.surface(renderer.waterSurface);
        }

        @Override
        public void end(CacheBatch batch){
            Graphics.surface();
            Graphics.end();

            Graphics.popBatch();

            Graphics.shader(Shaders.water);
            Graphics.begin();
            Draw.rect(renderer.waterSurface.texture(), Core.camera.position.x, Core.camera.position.y,
                    Core.camera.viewportWidth * Core.camera.zoom, -Core.camera.viewportHeight * Core.camera.zoom);
            Graphics.end();
            Graphics.shader();
        }
    },
    normal,
    walls;

    public void begin(CacheBatch batch){
        batch.setProjectionMatrix(Core.camera.combined);
        Graphics.useBatch(batch.drawBatch());

        Graphics.begin();
    }

    public void end(CacheBatch batch){
        Graphics.end();

        Graphics.popBatch();
    }
}
