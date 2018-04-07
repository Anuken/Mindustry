package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Color;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.graphics.CacheBatch;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Shader;

import static io.anuke.mindustry.Vars.renderer;

public enum DrawLayer {
    water{
        @Override
        public void begin(CacheBatch batch){
            beginShader(batch);
        }

        @Override
        public void end(CacheBatch batch){
            endShader(batch, Shaders.water);
        }
    },
    lava{
        @Override
        public void begin(CacheBatch batch){
            beginShader(batch);
        }

        @Override
        public void end(CacheBatch batch){
            endShader(batch, Shaders.lava);
        }
    },
    oil{
        @Override
        public void begin(CacheBatch batch){
            beginShader(batch);
        }

        @Override
        public void end(CacheBatch batch){
            endShader(batch, Shaders.oil);
        }
    },
    space{
        @Override
        public void begin(CacheBatch batch){
            beginShader(batch);
        }

        @Override
        public void end(CacheBatch batch){
            endShader(batch, Shaders.space);
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

    protected void beginShader(CacheBatch batch){
        batch.setProjectionMatrix(Core.camera.combined);
        Graphics.useBatch(batch.drawBatch());

        Graphics.begin();
        Graphics.surface(renderer.waterSurface);
        Graphics.clear(Color.CLEAR);
    }

    public void endShader(CacheBatch batch, Shader shader){
        Graphics.surface();
        Graphics.end();

        Graphics.popBatch();

        Graphics.shader(shader);
        Graphics.begin();
        Draw.rect(renderer.waterSurface.texture(), Core.camera.position.x, Core.camera.position.y,
                Core.camera.viewportWidth * Core.camera.zoom, -Core.camera.viewportHeight * Core.camera.zoom);
        Graphics.end();
        Graphics.shader();
    }
}
