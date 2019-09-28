package io.anuke.mindustry.graphics;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.glutils.*;

public class LightRenderer{
    private static final int scaling = 4;
    private FrameBuffer buffer = new FrameBuffer(2, 2);
    private Array<Runnable> lights = new Array<>();

    public void add(Runnable run){
        lights.add(run);
    }

    public void add(float x, float y, float radius, Color color, float opacity){
        add(() -> {
            Draw.color(color, opacity);
            Draw.rect("circle-shadow", x, y, radius * 2, radius * 2);
        });
    }

    public void draw(){
        if(buffer.getWidth() != Core.graphics.getWidth()/scaling || buffer.getHeight() != Core.graphics.getHeight()/scaling){
            buffer.resize(Core.graphics.getWidth()/scaling, Core.graphics.getHeight()/scaling);
        }

        Draw.color();
        buffer.beginDraw(Color.clear);
        Draw.blend(Blending.additive);
        for(Runnable run : lights){
            run.run();
        }
        Draw.reset();
        Draw.blend();
        buffer.endDraw();

        Draw.color();
        Draw.shader(Shaders.light);
        Draw.rect(Draw.wrap(buffer.getTexture()), Core.camera.position.x, Core.camera.position.y, Core.camera.width, -Core.camera.height);
        Draw.shader();

        lights.clear();
    }
}
