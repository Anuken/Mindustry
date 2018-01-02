package io.anuke.mindustry.io;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;
import com.badlogic.gdx.utils.async.AsyncExecutor;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
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
                saves.add(new SaveSlot(i));
                nextSlot = i + 1;
            }
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
    }

    public Array<SaveSlot> getSaveSlots(){
        return saves;
    }

    public class SaveSlot{
        public final int index;

        public SaveSlot(int index){
            this.index = index;
        }

        public void load(){
            current = this;
            SaveIO.loadFromSlot(index);
        }

        public void save(){
            current = this;
            SaveIO.saveToSlot(index);
        }

        public String getDate(){
            return SaveIO.getTimeString(index);
        }

        public Map getMap(){
            return SaveIO.getMap(index);
        }

        public String getName(){
            return Settings.getString("save-"+index+"-name");
        }

        public void setName(String name){
            Settings.putString("save-"+index+"-name", name);
            Settings.save();
        }

        public int getWave(){
            return SaveIO.getWave(index);
        }

        public GameMode getMode(){
            return SaveIO.getMode(index);
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
            if(this == current){
                current = null;
            }
        }
    }
}
