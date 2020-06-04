package mindustry.maps.generators;

import arc.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.blocks.defense.turrets.ItemTurret.*;
import mindustry.world.blocks.environment.*;

import java.io.*;

public class BaseGenerator{
    static Array<Schematic> schematics = new Array<>();

    public void generate(Tiles tiles, Array<Tile> cores, Tile spawn, Team team, Sector sector){
        for(Tile tile : cores){
            tile.clearOverlay();
            tile.setBlock(Blocks.coreShard, team);
        }

        //TODO remove this, it's just a test
        if(!Core.files.external("SCHEMATICOUTPUT").exists()){
            Log.err("no schematics");
            return;
        }

        if(schematics.isEmpty()){
            schematics.addAll(Array.with(Core.files.external("SCHEMATICOUTPUT").list()).map(s -> {
                try{
                    return Schematics.read(s);
                }catch(IOException e){
                    throw new RuntimeException();
                }
            }));
        }

        Vars.state.rules.enemyCheat = true;

        int range = 180;
        int attempts = 3000;
        int cx = cores.first().x, cy = cores.first().y;

        outer:
        for(int i = 0; i < attempts; i++){
            Tmp.v1.rnd(Mathf.random(range));
            int x = (int)(cx + Tmp.v1.x), y = (int)(cy + Tmp.v1.y);

            Schematic res = schematics.random();
            int ex = x - res.width/2, ey = y - res.height/2;

            for(int rx = ex; rx <= ex + res.width; rx++){
                for(int ry = ey; ry <= ey + res.height; ry++){
                    Tile tile = tiles.get(rx, ry);
                    if(tile == null || Vars.world.getDarkness(rx, ry) > 0 || tile.floor().isDeep() || (!tile.block().isAir() && !(tile.block() instanceof Rock && tile.block().destructible))) continue outer;
                }
            }

            Schematics.place(res, x, y, team);

            //add ammo
            for(int rx = ex; rx <= ex + res.width; rx++){
                for(int ry = ey; ry <= ey + res.height; ry++){
                    Tile tile = tiles.get(rx, ry);
                    if(tile != null && tile.entity instanceof ItemTurretEntity){
                        tile.entity.handleItem(tile.entity, ((ItemTurret)tile.block()).ammoTypes.keys().toArray().first());
                    }
                }
            }
        }

    }
}
