package io.anuke.mindustry.maps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Array.ArrayIterable;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Difficulty;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.maps.SectorPresets.SectorPreset;
import io.anuke.mindustry.maps.generation.Generation;
import io.anuke.mindustry.maps.generation.WorldGenerator.GenResult;
import io.anuke.mindustry.maps.missions.BattleMission;
import io.anuke.mindustry.maps.missions.Mission;
import io.anuke.mindustry.maps.missions.Missions;
import io.anuke.mindustry.maps.missions.WaveMission;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.type.Recipe.RecipeVisibility;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.mindustry.world.blocks.defense.Wall;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.util.*;

import static io.anuke.mindustry.Vars.*;

public class Sectors{
    public static final int sectorImageSize = 32;

    private final GridMap<Sector> grid = new GridMap<>();
    private final SectorPresets presets = new SectorPresets();
    private final Array<Item> allOres = Item.getAllOres();
    private final AsyncExecutor executor = new AsyncExecutor(6);

    public void playSector(Sector sector){
        if(!headless && sector.hasSave() && SaveIO.breakingVersions.contains(sector.getSave().getBuild())){
            sector.getSave().delete();
            ui.showInfo("$text.save.old");
        }

        if(!sector.hasSave()){
            for(Mission mission : sector.missions){
                mission.reset();
            }
            world.loadSector(sector);
            logic.play();
            if(!headless){
                sector.saveID = control.saves.addSave("sector-" + sector.pos()).index;
            }
            world.sectors.save();
            world.setSector(sector);
            if(!sector.complete) sector.currentMission().onBegin();
        }else if(SaveIO.breakingVersions.contains(sector.getSave().getBuild())){
            ui.showInfo("$text.save.old");
        }else try{
            sector.getSave().load();
            world.setSector(sector);
            state.set(State.playing);
            if(!sector.complete) sector.currentMission().onBegin();
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

    public Difficulty getDifficulty(Sector sector){
        if(sector.difficulty == 0){
            return Difficulty.hard;
        }else if(sector.difficulty < 4){
            return Difficulty.normal;
        }else if(sector.difficulty < 9){
            return Difficulty.hard;
        }else{
            return Difficulty.insane;
        }
    }

    public Array<Item> getOres(int x, int y){
        return presets.getOres(x, y) == null ? allOres : presets.getOres(x, y);
    }

    /**Unlocks a sector. This shows nearby sectors.*/
    public void completeSector(int x, int y){
        createSector(x, y);
        Sector sector = get(x, y);
        sector.complete = true;

        for(GridPoint2 g : Geometry.d4){
            createSector(x + g.x, y + g.y);
        }
    }

    /**Creates a sector at a location if it is not present, but does not complete it.*/
    public void createSector(int x, int y){

        if(grid.containsKey(x, y)) return;

        Sector sector = new Sector();
        sector.x = (short)x;
        sector.y = (short)y;
        sector.complete = false;
        initSector(sector);

        grid.put(sector.x, sector.y, sector);

        if(sector.texture == null){
            threads.runGraphics(() -> createTexture(sector));
        }

        if(sector.missions.size == 0){
            completeSector(sector.x, sector.y);
        }
    }

    public void abandonSector(Sector sector){
        if(sector.hasSave()){
            sector.getSave().delete();
        }
        sector.completedMissions = 0;
        sector.complete = false;
        initSector(sector);

        grid.put(sector.x, sector.y, sector);

        threads.runGraphics(() -> createTexture(sector));

        save();
    }

    public void load(){
        for(Sector sector : grid.values()){
            sector.texture.dispose();
        }
        grid.clear();

        Array<Sector> out = Settings.getObject("sector-data-2", Array.class, Array::new);

        for(Sector sector : out){
            
            createTexture(sector);
            initSector(sector);
            grid.put(sector.x, sector.y, sector);
        }

        if(out.size == 0){
            createSector(0, 0);
        }
    }

    public void clear(){
        grid.clear();
        save();
        createSector(0, 0);
    }

    public void save(){
        Array<Sector> out = new Array<>();

        for(Sector sector : grid.values()){
            if(sector != null && !out.contains(sector, true)){
                out.add(sector);
            }
        }

        Settings.putObject("sector-data-2", out);
        Settings.save();
    }

    private void initSector(Sector sector){
        sector.difficulty = (int)(Mathf.dst(sector.x, sector.y));

        if(presets.get(sector.x, sector.y) != null){
            SectorPreset p = presets.get(sector.x, sector.y);
            sector.missions.addAll(p.missions);
            sector.x = (short)p.x;
            sector.y = (short)p.y;
        }else{
            generate(sector);
        }

        sector.spawns = new Array<>();

        for(Mission mission : sector.missions){
            sector.spawns.addAll(mission.getWaves(sector));
        }

        //set starter items
        if(sector.difficulty > 12){ //now with titanium
            sector.startingItems = Array.with(new ItemStack(Items.copper, 1900), new ItemStack(Items.lead, 500), new ItemStack(Items.densealloy, 470), new ItemStack(Items.silicon, 460), new ItemStack(Items.titanium, 230));
        }else if(sector.difficulty > 8){ //just more resources
            sector.startingItems = Array.with(new ItemStack(Items.copper, 1500), new ItemStack(Items.lead, 400), new ItemStack(Items.densealloy, 340), new ItemStack(Items.silicon, 250));
        }else if(sector.difficulty > 5){ //now with silicon
            sector.startingItems = Array.with(new ItemStack(Items.copper, 950), new ItemStack(Items.lead, 300), new ItemStack(Items.densealloy, 190), new ItemStack(Items.silicon, 140));
        }else if(sector.difficulty > 3){ //now with carbide
            sector.startingItems = Array.with(new ItemStack(Items.copper, 700), new ItemStack(Items.lead, 200), new ItemStack(Items.densealloy, 130));
        }else if(sector.difficulty > 2){ //more starter items for faster start
            sector.startingItems = Array.with(new ItemStack(Items.copper, 400), new ItemStack(Items.lead, 100));
        }else{ //empty default
            sector.startingItems = Array.with();
        }
    }

    /**Generates a mission for a sector. This is deterministic and the same for each client.*/
    private void generate(Sector sector){

        //50% chance to get a wave mission
        if(Mathf.randomSeed(sector.getSeed() + 7) < 0.5){
            //recipe mission (maybe)
            addRecipeMission(sector, 3);
            sector.missions.add(new WaveMission(sector.difficulty*5 + Mathf.randomSeed(sector.getSeed(), 1, 4)*5));
        }else{
            //battle missions don't get recipes
            sector.missions.add(new BattleMission());
        }

        //possibly add another recipe mission
        addRecipeMission(sector, 11);

        Generation gen = new Generation(sector, null, sectorSize, sectorSize, null);

        Array<GridPoint2> points = new Array<>();
        for(Mission mission : sector.missions){
            points.addAll(mission.getSpawnPoints(gen));
        }

        GenResult result = new GenResult();

        for(GridPoint2 point : new ArrayIterable<>(points)){
            world.generator.generateTile(result, sector.x, sector.y, point.x, point.y, true, null, null);
            if(((Floor)result.floor).isLiquid || result.wall.solid){
                sector.missions.clear();
                break;
            }
        }
    }

    private void addRecipeMission(Sector sector, int offset){
        //build list of locked recipes to add mission for obtaining it
        if(Mathf.randomSeed(sector.getSeed() + offset) < 0.5){
            Array<Recipe> recipes = new Array<>();
            for(Recipe r : content.recipes()){
                if(r.result instanceof Wall || (r.visibility != RecipeVisibility.all) || r.cost < 10f) continue;
                recipes.add(r);
            }
            float maxdiff = 8f;
            recipes.sort((r1, r2) -> Float.compare(r1.cost, r2.cost));
            int end = (int)(Mathf.clamp(sector.difficulty / maxdiff + 0.25f) * (recipes.size - 1));
            int start = (int)(Mathf.clamp(sector.difficulty / maxdiff) * (recipes.size / 2f));

            if(recipes.size > 0 && end > start){
                Recipe recipe = recipes.get(Mathf.randomSeed(sector.getSeed() + 10, start, end));
                sector.missions.addAll(Missions.blockRecipe(recipe.result));
            }
        }
    }

    private void createTexture(Sector sector){
        if(headless) return; //obviously not created or needed on server

        if(sector.texture != null){
            sector.texture.dispose();
        }

        executor.submit(() -> {
            Pixmap pixmap = new Pixmap(sectorImageSize, sectorImageSize, Format.RGBA8888);
            GenResult result = new GenResult();
            GenResult secResult = new GenResult();

            for(int x = 0; x < pixmap.getWidth(); x++){
                for(int y = 0; y < pixmap.getHeight(); y++){
                    int toX = x * sectorSize / sectorImageSize;
                    int toY = y * sectorSize / sectorImageSize;

                    world.generator.generateTile(result, sector.x, sector.y, toX, toY, false, null, null);
                    world.generator.generateTile(secResult, sector.x, sector.y, toX, ((y+1) * sectorSize / sectorImageSize), false, null, null);

                    int color = ColorMapper.colorFor(result.floor, result.wall, Team.none, result.elevation, secResult.elevation > result.elevation ? (byte)(1 << 6) : (byte)0);
                    pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, color);
                }
            }

            Gdx.app.postRunnable(() -> {
                sector.texture = new Texture(pixmap);
                pixmap.dispose();
            });

            return null;
        });
    }


}
