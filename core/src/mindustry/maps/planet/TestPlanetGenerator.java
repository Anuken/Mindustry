package mindustry.maps.planet;

import arc.graphics.*;
import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import arc.util.noise.*;
import mindustry.content.*;
import mindustry.world.*;

public class TestPlanetGenerator implements PlanetGenerator{
    Simplex noise = new Simplex();
    float scl = 5f;

    //TODO generate array from planet image later
    Block[][] arr = {
    {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.snow, Blocks.ice},
    {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.snow, Blocks.snow, Blocks.ice},
    {Blocks.water, Blocks.darksandWater, Blocks.darksand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.darksandTaintedWater, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.deepwater, Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.moss, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice},
    {Blocks.deepwater, Blocks.water, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.sand, Blocks.moss, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.snow, Blocks.ice},
    {Blocks.deepwater, Blocks.sandWater, Blocks.sand, Blocks.sand, Blocks.moss, Blocks.moss, Blocks.moss, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.snow, Blocks.ice},
    {Blocks.taintedWater, Blocks.darksandTaintedWater, Blocks.darksand, Blocks.darksand, Blocks.darksandTaintedWater, Blocks.moss, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.darksand, Blocks.moss, Blocks.sporeMoss, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice},
    {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.sporeMoss, Blocks.ice, Blocks.ice, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice},
    {Blocks.taintedWater, Blocks.darksandTaintedWater, Blocks.darksand, Blocks.sporeMoss, Blocks.sporeMoss, Blocks.ice, Blocks.ice, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice},
    {Blocks.darksandTaintedWater, Blocks.darksandTaintedWater, Blocks.darksand, Blocks.sporeMoss, Blocks.moss, Blocks.sporeMoss, Blocks.iceSnow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice},
    {Blocks.darksandWater, Blocks.darksand, Blocks.darksand, Blocks.ice, Blocks.iceSnow, Blocks.iceSnow, Blocks.snow, Blocks.snow, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice, Blocks.ice}
    };

    float water = 2f / arr[0].length;

    float rawHeight(Vec3 position){
        position = Tmp.v33.set(position).scl(scl);
        return Mathf.pow((float)noise.octaveNoise3D(7, 0.48f, 1f/3f, position.x, position.y, position.z), 2.3f);
    }

    @Override
    public float getHeight(Vec3 position){
        float height = rawHeight(position);
        if(height <= water){
            return water;
        }
        return height;
    }

    @Override
    public Color getColor(Vec3 position){
        return getBlock(position).color;
    }

    @Override
    public void generate(Vec3 position, TileGen tile){
        tile.floor = getBlock(position);
    }

    Block getBlock(Vec3 position){
        float height = rawHeight(position);
        position = Tmp.v33.set(position).scl(scl);
        float rad = scl;
        float temp = Mathf.clamp(Math.abs(position.y * 2f) / (rad));
        float tnoise = (float)noise.octaveNoise3D(7, 0.48f, 1f/3f, position.x, position.y + 999f, position.z);
        temp = Mathf.lerp(temp, tnoise, 0.5f);
        height *= 1.2f;
        height = Mathf.clamp(height);


        return arr[Mathf.clamp((int)(temp * arr.length), 0, arr.length - 1)][Mathf.clamp((int)(height * arr[0].length), 0, arr[0].length - 1)];
    }
}
