package io.anuke.mindustry.graphics;

import io.anuke.ucore.graphics.Shader;

public enum CacheLayer{
    water{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.water);
        }
    },
    lava{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.lava);
        }
    },
    oil{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.oil);
        }
    },
    space{
        @Override
        public void begin(){
            beginShader();
        }

        @Override
        public void end(){
            endShader(Shaders.space);
        }
    },
    normal;

    public void begin(){

    }

    public void end(){

    }

    protected void beginShader(){
        //renderer.getBlocks().endFloor();
       // renderer.effectSurface.getBuffer().begin();
        //Graphics.clear(Color.CLEAR);
        //renderer.getBlocks().beginFloor();
    }

    public void endShader(Shader shader){
        /*
        renderer.blocks.floor.endDraw();

        renderer.effectSurface.getBuffer().end();
        //renderer.pixelSurface.getBuffer().bind();

        Graphics.shader(shader);
        Graphics.begin();
        Draw.rect(renderer.effectSurface.texture(), Core.camera.position.x, Core.camera.position.y,
                Core.camera.viewportWidth * Core.camera.zoom, -Core.camera.viewportHeight * Core.camera.zoom);
        Graphics.end();
        Graphics.shader();
        renderer.blocks.floor.beginDraw();*/
    }
}
