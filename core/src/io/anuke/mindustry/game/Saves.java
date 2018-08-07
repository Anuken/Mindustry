package io.anuke.mindustry.game;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.TimeUtils;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.EventType.StateChangeEvent;
import io.anuke.mindustry.io.SaveIO;
import io.anuke.mindustry.io.SaveMeta;
import io.anuke.mindustry.maps.Map;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Strings;
import io.anuke.ucore.util.ThreadArray;

import java.io.IOException;

import static io.anuke.mindustry.Vars.*;

public class Saves{
    private int nextSlot;
    private Array<SaveSlot> saves = new ThreadArray<>();
    private IntMap<SaveSlot> saveMap = new IntMap<>();
    private SaveSlot current;
    private boolean saving;
    private float time;

    private long totalPlaytime;
    private long lastTimestamp;

    public Saves(){
        Events.on(StateChangeEvent.class, (prev, state) -> {
            if(state == State.menu){
                threads.run(() -> {
                    totalPlaytime = 0;
                    lastTimestamp = 0;
                    current = null;
                });
            }
        });
    }

    public void load(){
        saves.clear();
        int[] slots = Settings.getJson("save-slots", int[].class);

        for(int index : slots){
            if(SaveIO.isSaveValid(index)){
                SaveSlot slot = new SaveSlot(index);
                saves.add(slot);
                saveMap.put(slot.index, slot);
                slot.meta = SaveIO.getData(index);
                nextSlot = Math.max(index + 1, nextSlot);
            }
        }
    }

    public SaveSlot getCurrent(){
        return current;
    }

    public void update(){
        SaveSlot current = this.current;

        if(current != null && !state.is(State.menu)
            && !(state.isPaused() && ui.hasDialog())){
            if(lastTimestamp != 0){
                totalPlaytime += TimeUtils.timeSinceMillis(lastTimestamp);
            }
            lastTimestamp = TimeUtils.millis();
        }

        if(!state.is(State.menu) && !state.gameOver && current != null && current.isAutosave()){
            time += Timers.delta();
            if(time > Settings.getInt("saveinterval") * 60){
                saving = true;

                Timers.run(2f, () -> {
                    try{
                        current.save();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    saving = false;
                });

                time = 0;
            }
        }else{
            time = 0;
        }
    }

    public long getTotalPlaytime(){
        return totalPlaytime;
    }

    public void resetSave(){
        current = null;
    }

    public boolean isSaving(){
        return saving;
    }

    public SaveSlot addSave(String name){
        SaveSlot slot = new SaveSlot(nextSlot);
        nextSlot++;
        slot.setName(name);
        saves.add(slot);
        saveMap.put(slot.index, slot);
        slot.save();
        saveSlots();
        return slot;
    }

    public SaveSlot importSave(FileHandle file) throws IOException{
        SaveSlot slot = new SaveSlot(nextSlot);
        slot.importFile(file);
        nextSlot++;
        slot.setName(file.nameWithoutExtension());
        saves.add(slot);
        saveMap.put(slot.index, slot);
        slot.meta = SaveIO.getData(slot.index);
        current = slot;
        saveSlots();
        return slot;
    }

    public SaveSlot getByID(int id){
        return saveMap.get(id);
    }

    public Array<SaveSlot> getSaveSlots(){
        return saves;
    }

    private void saveSlots(){
        int[] result = new int[saves.size];
        for(int i = 0; i < result.length; i++){
            result[i] = saves.get(i).index;
        }
        Settings.putJson("save-slots", result);
        Settings.save();
    }

    public class SaveSlot{
        public final int index;
        SaveMeta meta;

        public SaveSlot(int index){
            this.index = index;
        }

        public void load(){
            SaveIO.loadFromSlot(index);
            meta = SaveIO.getData(index);
            current = this;
            totalPlaytime = meta.timePlayed;
        }

        public void save(){
            threads.runGraphics(() -> {
                //Renderer fog needs to be written on graphics thread, but save() can run on logic thread
                //thus, runGraphics is required here
                renderer.fog().writeFog();

                //save on the logic thread
                threads.run(() -> {
                    SaveIO.saveToSlot(index);
                    meta = SaveIO.getData(index);
                    current = this;
                });
            });
        }

        public boolean isHidden(){
            return meta.sector != invalidSector;
        }

        public String getPlayTime(){
            return Strings.formatMillis(current == this ? totalPlaytime : meta.timePlayed);
        }

        public String getDate(){
            return meta.date;
        }

        public Map getMap(){
            return meta.map;
        }

        public String getName(){
            return Settings.getString("save-" + index + "-name", "untittled");
        }

        public void setName(String name){
            Settings.putString("save-" + index + "-name", name);
            Settings.save();
        }

        public int getBuild(){
            return meta.build;
        }

        public int getWave(){
            return meta.wave;
        }

        public Difficulty getDifficulty(){
            return meta.difficulty;
        }

        public GameMode getMode(){
            return meta.mode;
        }

        public boolean isAutosave(){
            return Settings.getBool("save-" + index + "-autosave", true);
        }

        public void setAutosave(boolean save){
            Settings.putBool("save-" + index + "-autosave", save);
            Settings.save();
        }

        public void importFile(FileHandle file) throws IOException{
            try{
                file.copyTo(SaveIO.fileFor(index));
            }catch(Exception e){
                throw new IOException(e);
            }
        }

        public void exportFile(FileHandle file) throws IOException{
            try{
                if(!file.extension().equals(saveExtension)){
                    file = file.parent().child(file.nameWithoutExtension() + "." + saveExtension);
                }
                SaveIO.fileFor(index).copyTo(file);
            }catch(Exception e){
                throw new IOException(e);
            }
        }

        public void delete(){
            if(!gwt){ //can't delete files
                SaveIO.fileFor(index).delete();
            }
            saves.removeValue(this, true);
            saveMap.remove(index);
            if(this == current){
                current = null;
            }

            saveSlots();
        }
    }
}
