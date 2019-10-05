package io.anuke.mindustry.game;

import io.anuke.arc.*;
import io.anuke.arc.assets.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.util.*;
import io.anuke.arc.util.async.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.io.*;
import io.anuke.mindustry.io.SaveIO.*;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.type.*;

import java.io.*;
import java.text.*;
import java.util.*;

import static io.anuke.mindustry.Vars.*;

public class Saves{
    private int nextSlot;
    private Array<SaveSlot> saves = new Array<>();
    private IntMap<SaveSlot> saveMap = new IntMap<>();
    private SaveSlot current;
    private AsyncExecutor previewExecutor = new AsyncExecutor(1);
    private boolean saving;
    private float time;

    private long totalPlaytime;
    private long lastTimestamp;

    public Saves(){
        Core.assets.setLoader(Texture.class, ".spreview", new SavePreviewLoader());

        Events.on(StateChangeEvent.class, event -> {
            if(event.to == State.menu){
                totalPlaytime = 0;
                lastTimestamp = 0;
                current = null;
            }
        });
    }

    public void load(){
        saves.clear();
        IntArray slots = Core.settings.getObject("save-slots", IntArray.class, IntArray::new);

        for(int i = 0; i < slots.size; i++){
            int index = slots.get(i);
            if(SaveIO.isSaveValid(index)){
                SaveSlot slot = new SaveSlot(index);
                saves.add(slot);
                saveMap.put(slot.index, slot);
                slot.meta = SaveIO.getMeta(index);
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
        && !(state.isPaused() && Core.scene.hasDialog())){
            if(lastTimestamp != 0){
                totalPlaytime += Time.timeSinceMillis(lastTimestamp);
            }
            lastTimestamp = Time.millis();
        }

        if(!state.is(State.menu) && !state.gameOver && current != null && current.isAutosave() && !state.rules.tutorial){
            time += Time.delta();
            if(time > Core.settings.getInt("saveinterval") * 60){
                saving = true;

                Time.runTask(2f, () -> {
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

    public void zoneSave(){
        SaveSlot slot = new SaveSlot(-1);
        slot.setName("zone");
        saves.remove(s -> s.index == -1);
        saves.add(slot);
        saveMap.put(slot.index, slot);
        slot.save();
        saveSlots();
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
        slot.meta = SaveIO.getMeta(slot.index);
        current = slot;
        saveSlots();
        return slot;
    }

    public SaveSlot getZoneSlot(){
        SaveSlot slot = getByID(-1);
        return slot == null || slot.getZone() == null ? null : slot;
    }

    public SaveSlot getByID(int id){
        return saveMap.get(id);
    }

    public Array<SaveSlot> getSaveSlots(){
        return saves;
    }

    private void saveSlots(){
        IntArray result = new IntArray(saves.size);
        for(int i = 0; i < saves.size; i++) result.add(saves.get(i).index);

        Core.settings.putObject("save-slots", result);
        Core.settings.save();
    }

    public class SaveSlot{
        public final int index;
        boolean requestedPreview;
        SaveMeta meta;

        public SaveSlot(int index){
            this.index = index;
        }

        public void load() throws SaveException{
            try{
                SaveIO.loadFromSlot(index);
                meta = SaveIO.getMeta(index);
                current = this;
                totalPlaytime = meta.timePlayed;
                savePreview();
            }catch(Exception e){
                throw new SaveException(e);
            }
        }

        public void save(){
            long time = totalPlaytime;
            long prev = totalPlaytime;
            totalPlaytime = time;

            SaveIO.saveToSlot(index);
            meta = SaveIO.getMeta(index);
            if(!state.is(State.menu)){
                current = this;
            }

            totalPlaytime = prev;
            savePreview();
        }

        private void savePreview(){
            if(Core.assets.isLoaded(loadPreviewFile().path())){
                Core.assets.unload(loadPreviewFile().path());
            }
            previewExecutor.submit(() -> {
                try{
                    previewFile().writePNG(renderer.minimap.getPixmap());
                    requestedPreview = false;
                }catch(Throwable t){
                    t.printStackTrace();
                }
            });
        }

        public Texture previewTexture(){
            if(!previewFile().exists()){
                return null;
            }else if(Core.assets.isLoaded(loadPreviewFile().path())){
                return Core.assets.get(loadPreviewFile().path());
            }else if(!requestedPreview){
                Core.assets.load(new AssetDescriptor<>(loadPreviewFile(), Texture.class));
                requestedPreview = true;
            }
            return null;
        }

        private FileHandle previewFile(){
            return mapPreviewDirectory.child("save_slot_" + index + ".png");
        }

        private FileHandle loadPreviewFile(){
            return previewFile().sibling(previewFile().name() + ".spreview");
        }

        public boolean isHidden(){
            return getZone() != null;
        }

        public String getPlayTime(){
            return Strings.formatMillis(current == this ? totalPlaytime : meta.timePlayed);
        }

        public long getTimestamp(){
            return meta.timestamp;
        }

        public String getDate(){
            return SimpleDateFormat.getDateTimeInstance().format(new Date(meta.timestamp));
        }

        public Map getMap(){
            return meta.map;
        }

        public void cautiousLoad(Runnable run){
            Array<String> mods = Array.with(getMods());
            mods.removeAll(Vars.mods.getModStrings());

            if(!mods.isEmpty()){
                ui.showConfirm("$warning", Core.bundle.format("mod.missing", mods.toString("\n")), run);
            }else{
                run.run();
            }
        }

        public String getName(){
            return Core.settings.getString("save-" + index + "-name", "untitled");
        }

        public void setName(String name){
            Core.settings.put("save-" + index + "-name", name);
            Core.settings.save();
        }

        public String[] getMods(){
            return meta.mods;
        }

        public Zone getZone(){
            return meta == null || meta.rules == null ? null : meta.rules.zone;
        }

        public Gamemode mode(){
            return Gamemode.bestFit(meta.rules);
        }

        public int getBuild(){
            return meta.build;
        }

        public int getWave(){
            return meta.wave;
        }

        public boolean isAutosave(){
            return Core.settings.getBool("save-" + index + "-autosave", true);
        }

        public void setAutosave(boolean save){
            Core.settings.put("save-" + index + "-autosave", save);
            Core.settings.save();
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
                SaveIO.fileFor(index).copyTo(file);
            }catch(Exception e){
                throw new IOException(e);
            }
        }

        public void delete(){
            SaveIO.fileFor(index).delete();
            saves.removeValue(this, true);
            saveMap.remove(index);
            if(this == current){
                current = null;
            }

            if(Core.assets.isLoaded(loadPreviewFile().path())){
                Core.assets.unload(loadPreviewFile().path());
            }

            saveSlots();
        }
    }
}
