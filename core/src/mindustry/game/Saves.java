package mindustry.game;

import arc.*;
import arc.assets.*;
import arc.files.*;
import arc.graphics.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.GameState.*;
import mindustry.game.EventType.*;
import mindustry.io.*;
import mindustry.io.SaveIO.*;
import mindustry.maps.Map;
import mindustry.type.*;
import mindustry.world.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;

public class Saves{
    private static final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance();

    Seq<SaveSlot> saves = new Seq<>();
    @Nullable SaveSlot current;
    private @Nullable SaveSlot lastSectorSave;
    private boolean saving;
    private float time;

    long totalPlaytime;
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

        //read saves in parallel
        Seq<Future<SaveSlot>> futures = new Seq<>();

        saveDirectory.walk(file -> {
            if(!file.name().contains("backup") && SaveIO.isSaveValid(file)){
                futures.add(mainExecutor.submit(() -> {
                    SaveSlot slot = new SaveSlot(file);
                    slot.meta = SaveIO.getMeta(file);
                    return slot;
                }));
            }
        });

        for(var future : futures){
            try{
                saves.add(future.get());
            }catch(Exception e){
                Log.err(e);
            }
        }

        lastSectorSave = saves.find(s -> s.isSector() && s.getName().equals(Core.settings.getString("last-sector-save", "<none>")));

        class Remap{
            //file in the temp folder
            Fi sourceFile;
            //slot of source sector to move file for
            SaveSlot slot;
            Sector sourceSector;
            //sector info from source sector to move into
            SectorInfo sourceInfo;

            //file to copy to
            Fi destFile;
            //destination sector to move to
            Sector destSector;

            Remap(SaveSlot slot, Fi sourceFile, Sector sourceSector, SectorInfo sourceInfo, Fi destFile, Sector destSector){
                this.slot = slot;
                this.sourceFile = sourceFile;
                this.sourceSector = sourceSector;
                this.sourceInfo = sourceInfo;
                this.destFile = destFile;
                this.destSector = destSector;
            }
        }

        Seq<Remap> remaps = new Seq<>();
        ObjectSet<Sector> remapped = new ObjectSet<>();

        //automatically assign sector save slots
        for(SaveSlot slot : saves){
            if(slot.getSector() != null){
                Sector sector = slot.getSector();

                String name = slot.meta.tags.get("sectorPreset");
                Sector remapTarget = null;

                if(name != null){
                    if(!name.isEmpty()){ //if this save had a preset defined...
                        SectorPreset preset = content.sector(name);
                        //...place it in the right sector according to its preset
                        if(preset != null && preset.sector != sector && preset.requireUnlock){
                            remapTarget = preset.sector;
                        }
                    }
                }else{ //there was no sector preset in the meta at all, which means this is a legacy save that may need mapping
                    SectorPreset target = content.sectors().find(s -> s.planet == sector.planet && s.originalPosition == sector.id);
                    if(target != null && target.sector != sector && target.requireUnlock){ //there is indeed a sector preset that used to have this ID, and it needs remapping!
                        remapTarget = target.sector;
                    }
                }

                if(remapTarget != null){
                    //if the file name matches the destination of the remap, assume it has already been remapped, and skip the file movement procedure
                    if(!slot.file.equals(getSectorFile(remapTarget))){
                        Log.info("Remapping sector: @ -> @ (@)", sector.id, remapTarget.id, remapTarget.preset);

                        try{
                            SectorInfo info = Core.settings.getJson(sector.planet.name + "-s-" + sector.id + "-info", SectorInfo.class, SectorInfo::new);
                            Fi tmpRemapFile = saveDirectory.child("remap_" + sector.planet.name + "_" + sector.id + "." + saveExtension);
                            slot.file.moveTo(tmpRemapFile);

                            remaps.add(new Remap(slot, tmpRemapFile, sector, info, getSectorFile(remapTarget), remapTarget));
                            remapped.add(remapTarget);
                        }catch(Exception e){
                            Log.err("Failed to move sector files when remapping: " + sector.id + " -> " + remapTarget.id, e);
                        }
                    }

                    remapTarget.save = slot;
                    slot.meta.rules.sector = remapTarget;

                }else{
                    if(sector.save != null){
                        Log.warn("Sector @ has two corresponding saves: @ and @", sector, sector.save.file, slot.file);
                    }
                    sector.save = slot;
                }
            }
        }

        //process remaps later to allow swaps of sectors
        for(var remap : remaps){
            var remapTarget = remap.destSector;

            //overwrite the target sector's info with the save's info
            Core.settings.putJson(remapTarget.planet.name + "-s-" + remapTarget.id + "-info", remap.sourceInfo);
            remapTarget.loadInfo();

            remapTarget.save = remap.slot;
            try{
                //move file from tmp directory back into the correct location
                remap.sourceFile.moveTo(remap.destFile);
                remap.slot.file = remap.destFile;
            }catch(Exception e){
                Log.err("Failed to move back sector files when remapping: " + remap.sourceSector.id + " -> " + remapTarget.id, e);
            }

            //clear the info, assuming it wasn't a sector that got mapped to
            if(!remapped.contains(remap.sourceSector)){
                remap.sourceSector.clearInfo();
            }
        }
    }

    public @Nullable SaveSlot getLastSector(){
        return lastSectorSave;
    }

    public @Nullable SaveSlot getCurrent(){
        return current;
    }

    public void update(){
        if(current != null && state.isGame()
        && !(state.isPaused() && Core.scene.hasDialog())){
            if(lastTimestamp != 0){
                totalPlaytime += Time.timeSinceMillis(lastTimestamp);
            }
            lastTimestamp = Time.millis();
        }

        if(state.isGame() && !state.gameOver && current != null && current.isAutosave()){
            time += Time.delta;
            if(time > Core.settings.getInt("saveinterval") * 60 && !Vars.disableSave){
                saving = true;

                try{
                    current.save();
                }catch(Throwable t){
                    Log.err(t);
                }

                Time.runTask(3f, () -> saving = false);

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

    public Fi getSectorFile(Sector sector){
        return saveDirectory.child("sector-" + sector.planet.name + "-" + sector.id + "." + saveExtension);
    }

    public void saveSector(Sector sector){
        if(sector.save == null){
            sector.save = new SaveSlot(getSectorFile(sector));
            sector.save.setName(sector.save.file.nameWithoutExtension());
            saves.add(sector.save);
        }
        sector.save.setAutosave(true);
        sector.save.save();
        lastSectorSave = sector.save;
        Core.settings.put("last-sector-save", sector.save.getName());
    }

    public SaveSlot addSave(String name){
        SaveSlot slot = new SaveSlot(getNextSlotFile());
        slot.setName(name);
        saves.add(slot);
        slot.save();
        return slot;
    }

    public SaveSlot importSave(Fi file) throws IOException{
        SaveSlot slot = new SaveSlot(getNextSlotFile());
        slot.importFile(file);
        slot.setName(file.nameWithoutExtension());

        saves.add(slot);
        slot.meta = SaveIO.getMeta(slot.file);
        current = slot;
        return slot;
    }

    public Fi getNextSlotFile(){
        int i = 0;
        Fi file;
        while((file = saveDirectory.child(i + "." + saveExtension)).exists()){
            i ++;
        }
        return file;
    }

    public Seq<SaveSlot> getSaveSlots(){
        return saves;
    }

    public void deleteAll(){
        for(SaveSlot slot : saves.copy()){
            if(!slot.isSector()){
                slot.delete();
            }
        }
    }

    public class SaveSlot{
        public Fi file;
        boolean requestedPreview;
        public SaveMeta meta;

        public SaveSlot(Fi file){
            this.file = file;
        }

        public void load() throws SaveException{
            load(world.context);
        }

        public void load(WorldContext context) throws SaveException{
            try{
                SaveIO.load(file, context);
                meta = SaveIO.getMeta(file);
                current = this;
                totalPlaytime = meta.timePlayed;
                savePreview();
            }catch(Throwable e){
                throw new SaveException(e);
            }
        }

        public void save(){
            long prev = totalPlaytime;

            SaveIO.save(file);
            meta = SaveIO.getMeta(file);
            if(state.isGame()){
                current = this;
            }

            totalPlaytime = prev;
            savePreview();
        }

        private void savePreview(){
            if(Core.assets.isLoaded(loadPreviewFile().path())){
                Core.assets.unload(loadPreviewFile().path());
            }
            mainExecutor.submit(() -> {
                try{
                    previewFile().writePng(renderer.minimap.getPixmap());
                    requestedPreview = false;
                }catch(Throwable t){
                    Log.err(t);
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

        private String index(){
            return file.nameWithoutExtension();
        }

        private Fi previewFile(){
            return mapPreviewDirectory.child("save_slot_" + index() + ".png");
        }

        private Fi loadPreviewFile(){
            return previewFile().sibling(previewFile().name() + ".spreview");
        }

        public boolean isHidden(){
            return isSector();
        }

        public String getPlayTime(){
            return Strings.formatMillis(current == this ? totalPlaytime : meta.timePlayed);
        }

        public long getTimestamp(){
            return meta.timestamp;
        }

        public String getDate(){
            return dateFormat.format(new Date(meta.timestamp));
        }

        public Map getMap(){
            return meta.map;
        }

        public void cautiousLoad(Runnable run){
            Seq<String> mods = Seq.with(getMods());
            mods.removeAll(Vars.mods.getModStrings());

            if(!mods.isEmpty()){
                ui.showConfirm("@warning", Core.bundle.format("mod.missing", mods.toString("\n")), run);
            }else{
                run.run();
            }
        }

        public String getName(){
            return Core.settings.getString("save-" + index() + "-name", "untitled");
        }

        public void setName(String name){
            Core.settings.put("save-" + index() + "-name", name);
        }

        public String[] getMods(){
            return meta.mods;
        }

        public @Nullable Sector getSector(){
            //TODO remap sectors
            return meta == null || meta.rules == null ? null : meta.rules.sector;
        }

        public boolean isSector(){
            return getSector() != null;
        }

        public Gamemode mode(){
            return meta.rules.mode();
        }

        public int getBuild(){
            return meta.build;
        }

        public int getWave(){
            return meta.wave;
        }

        public boolean isAutosave(){
            return Core.settings.getBool("save-" + index() + "-autosave", true);
        }

        public void setAutosave(boolean save){
            Core.settings.put("save-" + index() + "-autosave", save);
        }

        public void importFile(Fi from) throws IOException{
            try{
                from.copyTo(file);
                if(previewFile().exists()){
                    requestedPreview = false;
                    previewFile().delete();
                }
            }catch(Exception e){
                throw new IOException(e);
            }
        }

        public void exportFile(Fi to) throws IOException{
            try{
                file.copyTo(to);
            }catch(Exception e){
                throw new IOException(e);
            }
        }

        public void delete(){
            if(SaveIO.backupFileFor(file).exists()){
                SaveIO.backupFileFor(file).delete();
            }
            file.delete();
            saves.remove(this, true);
            if(this == current){
                current = null;
            }

            if(Core.assets.isLoaded(loadPreviewFile().path())){
                Core.assets.unload(loadPreviewFile().path());
            }
        }
    }
}
