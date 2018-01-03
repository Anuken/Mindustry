package io.anuke.mindustry.io;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.world.GameMode;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;

import java.io.IOException;

public class Saves {
    private int nextSlot;
    private Array<SaveSlot> saves = new Array<>();
    private SaveSlot current;
    private boolean saving;
    private float time;

    private AsyncExecutor exec = new AsyncExecutor(1);

    public void load(){
        saves.clear();
        for(int i = 0; i < Vars.saveSlots; i ++){
            if(SaveIO.isSaveValid(i)){
                SaveSlot slot = new SaveSlot(i);
                saves.add(slot);
                slot.meta = SaveIO.getData(i);
                nextSlot = i + 1;
            }
        }
    }

    public void convertSaves(){
        for(SaveSlot slot : saves){
            SaveIO.checkConvert(slot.index);
        }
    }

    public SaveSlot getCurrent() {
        return current;
    }

    public void update(){
        if(!GameState.is(State.menu) && !GameState.is(State.dead) && current != null && current.isAutosave()){
            time += Timers.delta();
            if(time > Settings.getInt("saveinterval")*60) {
                saving = true;

                exec.submit(() -> {
                    SaveIO.saveToSlot(current.index);
                    saving = false;
                    return true;
                });

                time = 0;
            }
        }else{
            time = 0;
        }
    }

    public void resetSave(){
        current = null;
    }

    public boolean isSaving(){
        return saving;
    }

    public boolean canAddSave(){
        return nextSlot <= Vars.saveSlots;
    }

    public void addSave(String name){
        SaveSlot slot = new SaveSlot(nextSlot);
        nextSlot ++;
        slot.setName(name);
        saves.add(slot);
        SaveIO.saveToSlot(slot.index);
        slot.meta = SaveIO.getData(slot.index);
        current = slot;
    }

    public Array<SaveSlot> getSaveSlots(){
        return saves;
    }

    public class SaveSlot{
        public final int index;
        SaveMeta meta;

        public SaveSlot(int index){
            this.index = index;
        }

        public void load(){
            current = this;
            SaveIO.loadFromSlot(index);
            meta = SaveIO.getData(index);
        }

        public void save(){
            current = this;
            SaveIO.saveToSlot(index);
            meta = SaveIO.getData(index);
        }

        public String getDate(){
            return meta.date;
        }

        public Map getMap(){
            return meta.map;
        }

        public String getName(){
            return Settings.getString("save-"+index+"-name");
        }

        public void setName(String name){
            Settings.putString("save-"+index+"-name", name);
            Settings.save();
        }

        public int getWave(){
            return meta.wave;
        }

        public GameMode getMode(){
            return meta.mode;
        }

        public boolean isAutosave(){
            return Settings.getBool("save-"+index+"-autosave");
        }

        public void setAutosave(boolean save){
            Settings.putBool("save-"+index + "-autosave", save);
            Settings.save();
        }

        public void importFile(FileHandle file) throws IOException{
            try{
                file.copyTo(SaveIO.fileFor(index));
            }catch (Exception e){
                throw new IOException(e);
            }
        }

        public void exportFile(FileHandle file) throws IOException{
            try{
                if(!file.extension().equals("mins")){
                    file = file.parent().child(file.nameWithoutExtension() + ".mins");
                }
                SaveIO.fileFor(index).copyTo(file);
            }catch (Exception e){
                throw new IOException(e);
            }
        }

        public void delete(){
            SaveIO.fileFor(index).delete();
            saves.removeValue(this, true);
            if(this == current){
                current = null;
            }
        }
    }
}
