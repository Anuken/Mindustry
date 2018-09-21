package io.anuke.mindustry.maps;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.maps.generation.WorldGenerator.GenResult;
import io.anuke.mindustry.maps.missions.BattleMission;
import io.anuke.mindustry.maps.missions.WaveMission;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.Edges;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.GridMap;
import io.anuke.ucore.util.Log;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class Sectors{
    private static final int sectorImageSize = 32;
    private static final float sectorLargeChance = 0.24f;

    private GridMap<Sector> grid = new GridMap<>();

    public void playSector(Sector sector){
        if(sector.hasSave() && SaveIO.breakingVersions.contains(sector.getSave().getBuild())){
            sector.getSave().delete();
            ui.showInfo("$text.save.old");
        }

        if(!sector.hasSave()){
            world.loadSector(sector);
            logic.play();
            sector.saveID = control.getSaves().addSave("sector-" + sector.packedPosition()).index;
            world.sectors().save();
            world.setSector(sector);
        }else if(SaveIO.breakingVersions.contains(sector.getSave().getBuild())){
            ui.showInfo("$text.save.old");
        }else try{
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

    /**If a sector is not yet unlocked, returns null.*/
    public Sector get(int x, int y){
        return grid.get(x, y);
    }

    public Sector get(int position){
        return grid.get(Bits.getLeftShort(position), Bits.getRightShort(position));
    }

    /**Tries to a sector in a specific direciton, specified by expandX and expandY.
     * The player *must* currently be playing in this sector.
     * If a sector is in that direction, this method will return false (failure)
     * @param sector the sector to expand
     * @param expandX spaces in X coordinate to expand, can be negative
     * @param expandY spaces in Y coordinate to expand, can be negative*/
    public boolean expandSector(Sector sector, int expandX, int expandY){
        if(world.getSector() != sector){
            throw new IllegalArgumentException("Sector is not being played in!");
        }

        sector.width += expandX;
        sector.height += expandY;

        for(int x = sector.x; x < sector.x+sector.width; x++){
            for(int y = sector.y; y < sector.y+sector.height; y++){
                grid.put(x, y, null);
            }
        }
        
        if(expandX < 0) sector.x += expandX;
        if(expandY < 0) sector.y += expandY;

        for(int x = sector.x; x < sector.x+sector.width; x++){
            for(int y = sector.y; y < sector.y+sector.height; y++){
                grid.put(x, y, sector);
            }
        }

        return false;
    }

    /**Unlocks a sector. This shows nearby sectors.*/
    public void completeSector(int x, int y){
        createSector(x, y);
        Sector sector = get(x, y);
        sector.complete = true;

        //TODO work for unique width + height?
        for(GridPoint2 point : Edges.getEdges(sector.width)){
            createSector(sector.x + point.x, sector.y + point.y);
        }
    }

    /**Creates a sector at a location if it is not present, but does not unlock it.*/
    public void createSector(int x, int y){

        if(grid.containsKey(x, y)) return;

        Sector sector = new Sector();
        sector.x = (short)x;
        sector.y = (short)y;
        sector.complete = false;
        sector.width = sector.height = 1;
        initSector(sector);

        for(int cx = 0; cx < sector.width; cx++){
            for(int cy = 0; cy < sector.height; cy++){
                grid.put(x + cx, y + cy, sector);
            }
        }

        if(sector.texture == null) createTexture(sector);
    }

    public void load(){
        for(Sector sector : grid.values()){
            sector.texture.dispose();
        }
        grid.clear();

        Array<Sector> out = Settings.getBinary("sectors", Array.class, () -> new Array<>());

        for(Sector sector : out){
            createTexture(sector);
            initSector(sector);
            for(int cx = 0; cx < sector.width; cx++){
                for(int cy = 0; cy < sector.height; cy++){
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

        Settings.putBinary("sectors", out);
        Settings.save();
    }

    private void initSector(Sector sector){
        double waveChance = 0.3;

        sector.difficulty = (int)(Mathf.dst(sector.x, sector.y));

        if(sector.difficulty == 0){
            sector.missions.add(new WaveMission(10));
        }else{
            sector.missions.add(Mathf.randomSeed(sector.getSeed() + 1) < waveChance ? new WaveMission(Math.min(sector.difficulty*5 + Mathf.randomSeed(sector.getSeed(), 0, 3)*5, 100))
                    : new BattleMission());
        }

        sector.spawns = sector.missions.first().getWaves(sector);

        sector.ores.addAll(Items.copper);

        //set starter items
        if(sector.difficulty > 12){ //now with titanium
            sector.startingItems = Array.with(new ItemStack(Items.copper, 1900), new ItemStack(Items.lead, 500), new ItemStack(Items.densealloy, 470), new ItemStack(Items.silicon, 460), new ItemStack(Items.titanium, 230));
        }else if(sector.difficulty > 8){ //just more resources
            sector.startingItems = Array.with(new ItemStack(Items.copper, 1500), new ItemStack(Items.lead, 400), new ItemStack(Items.densealloy, 340), new ItemStack(Items.silicon, 250));
        }else if(sector.difficulty > 5){ //now with silicon
            sector.startingItems = Array.with(new ItemStack(Items.copper, 950), new ItemStack(Items.lead, 300), new ItemStack(Items.densealloy, 190), new ItemStack(Items.silicon, 140));
        }else if(sector.difficulty > 3){ //now with carbide
            sector.startingItems = Array.with(new ItemStack(Items.copper, 700), new ItemStack(Items.lead, 200), new ItemStack(Items.densealloy, 130));
        }else if(sector.difficulty > 1){ //more starter items for faster start
            sector.startingItems = Array.with(new ItemStack(Items.copper, 400), new ItemStack(Items.lead, 100));
        }else{ //base starting items to prevent grinding much
            sector.startingItems = Array.with(new ItemStack(Items.copper, 130));
        }
    }

    private void createTexture(Sector sector){
        if(headless) return; //obviously not created or needed on server

        Pixmap pixmap = new Pixmap(sectorImageSize * sector.width, sectorImageSize * sector.height, Format.RGBA8888);

        for(int x = 0; x < pixmap.getWidth(); x++){
            for(int y = 0; y < pixmap.getHeight(); y++){
                int toX = x * sectorSize / sectorImageSize;
                int toY = y * sectorSize / sectorImageSize;

                GenResult result = world.generator().generateTile(sector.x, sector.y, toX, toY, false);

                int color = ColorMapper.colorFor(result.floor, result.wall, Team.none, result.elevation, (byte)0);
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, color);
            }
        }

        sector.texture = new Texture(pixmap);
        pixmap.dispose();
    }
}
