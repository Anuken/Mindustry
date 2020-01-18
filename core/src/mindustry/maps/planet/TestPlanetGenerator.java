package mindustry.maps.planet;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;

public class TestPlanetGenerator implements PlanetGenerator{
    Pixmap pix = new Pixmap("planets/colors.png");
    Simplex noise = new Simplex();
    int waterLevel = 5;
    float water = waterLevel / (float)(pix.getHeight());
    float scl = 5f;

    @Override
    public float getHeight(Vec3 position){
        position = Tmp.v33.set(position).scl(scl);

        float height = Mathf.pow((float)noise.octaveNoise3D(7, 0.48f, 1f/3f, position.x, position.y, position.z), 2.4f);
        if(height <= water){
            return water;
        }
        return height;
    }

    @Override
    public Color getColor(Vec3 position){
        float height = getHeight(position);
        position = Tmp.v33.set(position).scl(scl);
        float rad = scl;
        float temp = Mathf.clamp(Math.abs(position.y * 2f) / (rad));
        float tnoise = (float)noise.octaveNoise3D(7, 0.48f, 1f/3f, position.x, position.y + 999f, position.z);
        temp = Mathf.lerp(temp, tnoise, 0.5f);
        height *= 1.2f;
        height = Mathf.clamp(height);

        return Tmp.c1.set(pix.getPixel((int)(temp * (pix.getWidth()-1)), (int)((1f-height) * (pix.getHeight()-1))));
    }
}
