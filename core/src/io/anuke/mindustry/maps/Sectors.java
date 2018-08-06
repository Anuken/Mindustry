package io.anuke.mindustry.maps;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.maps.generation.WorldGenerator.GenResult;
import io.anuke.mindustry.maps.missions.BattleMission;
import io.anuke.mindustry.maps.missions.WaveMission;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.Edges;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.GridMap;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class Sectors{
    private static final int sectorImageSize = 16;
    private static final float sectorLargeChance = 0.23f;

    private GridMap<Sector> grid = new GridMap<>();

    public Sectors(){
        Settings.json().addClassTag("Sector", Sector.class);
    }

    public void playSector(Sector sector){
        if(!sector.hasSave()){
            world.loadSector(sector);
            logic.play();
            sector.saveID = control.getSaves().addSave("sector-" + sector.packedPosition()).index;
            world.sectors().save();
            world.setSector(sector);
        }else{
            try{
                sector.getSave().load();
                world.setSector(sector);
                state.set(State.playing);
            }catch(Exception e){
                Log.err(e);
                sector.getSave().delete();

                playSector(sector);

                if(!headless){
                    threads.runGraphics(() -> ui.showError("$text.sector.corrupted"));
                }
            }
        }
    }

    /**If a sector is not yet unlocked, returns null.*/
    public Sector get(int x, int y){
        return grid.get(x, y);
    }

    public Sector get(int position){
        return grid.get(Bits.getLeftShort(position), Bits.getRightShort(position));
    }

    /**Unlocks a sector. This shows nearby sectors.*/
    public void completeSector(int x, int y){
        createSector(x, y);
        Sector sector = get(x, y);
        sector.complete = true;

        for(GridPoint2 point : Edges.getEdges(sector.size)){
            createSector(sector.x + point.x, sector.y + point.y);
        }
    }

    /**Creates a sector at a location if it is not present, but does not unlock it.*/
    public void createSector(int x, int y){
        boolean isLarge = Mathf.randomSeed(3+Bits.packInt((short)round2(x), (short)round2(y))) < sectorLargeChance;

        if(isLarge){
            x = round2(x);
            y = round2(y);
        }

        if(grid.containsKey(x, y)) return;

        Sector sector = new Sector();
        sector.x = (short)x;
        sector.y = (short)y;
        sector.complete = false;
        sector.size = isLarge ? 2 : 1;
        initSector(sector);

        for(int cx = 0; cx < sector.size; cx++){
            for(int cy = 0; cy < sector.size; cy++){
                grid.put(x + cx, y + cy, sector);
            }
        }

        if(sector.texture == null) createTexture(sector);
    }

    public void load(){
        Array<Sector> out = Settings.getJson("sectors", Array.class);

        for(Sector sector : out){
            createTexture(sector);
            initSector(sector);
            for(int cx = 0; cx < sector.size; cx++){
                for(int cy = 0; cy < sector.size; cy++){
                    grid.put(sector.x + cx, sector.y + cy, sector);
                }
            }
        }

        if(out.size == 0){
            createSector(0, 0);
        }
    }

    public void save(){
        Array<Sector> out = new Array<>();

        for(Sector sector : grid.values()){
            out.add(sector);
        }

        Settings.putJson("sectors", out);
        Settings.save();
    }

    private void initSector(Sector sector){
        double waveChance = 0.3;

        sector.difficulty = (int)(Mathf.dst(sector.x, sector.y));
        sector.spawns = sector.missions.first().getWaves(sector);

        if(sector.difficulty == 0){
            sector.missions.add(new WaveMission(10));
        }else{
            sector.missions.add(Mathf.randomSeed(sector.getSeed() + 1) < waveChance ? new WaveMission(Math.min(10 + sector.difficulty*5 + Mathf.randomSeed(sector.getSeed(), 0, 5)*5, 100))
                    : new BattleMission());
        }

        //add all ores for now since material differences aren't well handled yet
        sector.ores.addAll(Items.tungsten, Items.coal, Items.lead, Items.thorium, Items.titanium);
    }

    private int round2(int i){
        if(i < 0) i --;
        return i/2*2;
    }

    private void createTexture(Sector sector){
        if(headless) return; //obviously not created or needed on server

        Pixmap pixmap = new Pixmap(sectorImageSize * sector.size, sectorImageSize * sector.size, Format.RGBA8888);

        for(int x = 0; x < pixmap.getWidth(); x++){
            for(int y = 0; y < pixmap.getHeight(); y++){
                int toX = x * sectorSize / sectorImageSize;
                int toY = y * sectorSize / sectorImageSize;

                GenResult result = world.generator().generateTile(sector.x, sector.y, toX, toY, false);

                int color = ColorMapper.colorFor(result.floor, result.wall, Team.none, result.elevation);
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, color);
            }
        }

        sector.texture = new Texture(pixmap);
        pixmap.dispose();
    }
}
