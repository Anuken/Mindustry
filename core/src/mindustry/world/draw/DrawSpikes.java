package mindustry.world.draw;

import arc.graphics.*;
import arc.graphics.g2d.*;
import mindustry.gen.*;

public class DrawSpikes extends DrawBlock{
    public Color color = Color.valueOf("7457ce");

    public int amount = 10, layers = 1;
    public float stroke = 2f, rotateSpeed = 0.8f;
    public float radius = 6f, length = 4f, x = 0f, y = 0f, layerSpeed = -1f;

    public DrawSpikes(Color color){
        this.color = color;
    }

    public DrawSpikes(){
    }

    @Override
    public void draw(Building build){
        if(build.warmup() <= 0.001f) return;

        Draw.color(color, build.warmup() * color.a);

        Lines.stroke(stroke);
        float curSpeed = 1f;
        for(int i = 0; i < layers; i++){
            Lines.spikes(build.x + x, build.y + y, radius, length, amount, build.totalProgress() * rotateSpeed * curSpeed);
            curSpeed *= layerSpeed;
        }

        Draw.reset();
    }
}
