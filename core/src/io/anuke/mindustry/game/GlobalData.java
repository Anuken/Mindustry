package io.anuke.mindustry.game;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.files.*;
import io.anuke.arc.util.io.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.ctype.UnlockableContent;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.type.*;

import java.io.*;
import java.util.zip.*;

import static io.anuke.mindustry.Vars.*;

/** Stores player unlocks. Clientside only. */
public class GlobalData{
    private ObjectMap<ContentType, ObjectSet<String>> unlocked = new ObjectMap<>();
    private ObjectIntMap<Item> items = new ObjectIntMap<>();
    private boolean modified;

    public GlobalData(){
        Core.settings.setSerializer(ContentType.class, (stream, t) -> stream.writeInt(t.ordinal()), stream -> ContentType.values()[stream.readInt()]);
        Core.settings.setSerializer(Item.class, (stream, t) -> stream.writeUTF(t.name), stream -> content.getByName(ContentType.item, stream.readUTF()));

        Core.settings.setSerializer(ItemStack.class, (stream, t) -> {
            stream.writeUTF(t.item.name);
            stream.writeInt(t.amount);
        }, stream -> {
            String name = stream.readUTF();
            int amount = stream.readInt();
            return new ItemStack(content.getByName(ContentType.item, name), amount);
        });
    }

    public void exportData(FileHandle file) throws IOException{
        Array<FileHandle> files = new Array<>();
        files.add(Core.settings.getSettingsFile());
        files.addAll(customMapDirectory.list());
        files.addAll(saveDirectory.list());
        files.addAll(screenshotDirectory.list());
        files.addAll(modDirectory.list());
        files.addAll(schematicDirectory.list());
        String base = Core.settings.getDataDirectory().path();

        try(OutputStream fos = file.write(false, 2048); ZipOutputStream zos = new ZipOutputStream(fos)){
            for(FileHandle add : files){
                if(add.isDirectory()) continue;
                zos.putNextEntry(new ZipEntry(add.path().substring(base.length())));
                Streams.copyStream(add.read(), zos);
                zos.closeEntry();
            }

        }
    }

    public void importData(FileHandle file){
        FileHandle dest = Core.files.local("zipdata.zip");
        file.copyTo(dest);
        FileHandle zipped = new ZipFileHandle(dest);

        FileHandle base = Core.settings.getDataDirectory();
        if(!zipped.child("settings.bin").exists()){
            throw new IllegalArgumentException("Not valid save data.");
        }

        //purge existing data
        for(FileHandle f : base.list()){
            if(f.isDirectory()){
                f.deleteDirectory();
            }else if(!f.name().equals("zipdata.zip")){
                f.delete();
            }
        }

        zipped.walk(f -> f.copyTo(base.child(f.path())));
        dest.delete();
    }

    public void modified(){
        modified = true;
    }

    public int getItem(Item item){
        return items.get(item, 0);
    }

    public void addItem(Item item, int amount){
        if(amount > 0){
            unlockContent(item);
        }
        modified = true;
        items.getAndIncrement(item, 0, amount);
        state.stats.itemsDelivered.getAndIncrement(item, 0, amount);
    }

    public boolean hasItems(Array<ItemStack> stacks){
        return !stacks.contains(s -> items.get(s.item, 0) < s.amount);
    }

    public boolean hasItems(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            if(!has(stack.item, stack.amount)){
                return false;
            }
        }

        return true;
    }

    public void removeItems(ItemStack[] stacks){
        for(ItemStack stack : stacks){
            items.getAndIncrement(stack.item, 0, -stack.amount);
        }
        modified = true;
    }

    public void removeItems(Array<ItemStack> stacks){
        for(ItemStack stack : stacks){
            items.getAndIncrement(stack.item, 0, -stack.amount);
        }
        modified = true;
    }

    public boolean has(Item item, int amount){
        return items.get(item, 0) >= amount;
    }

    public ObjectIntMap<Item> items(){
        return items;
    }

    /** Returns whether or not this piece of content is unlocked yet. */
    public boolean isUnlocked(UnlockableContent content){
        return content.alwaysUnlocked() || unlocked.getOr(content.getContentType(), ObjectSet::new).contains(content.name);
    }

    /**
     * Makes this piece of content 'unlocked', if possible.
     * If this piece of content is already unlocked, nothing changes.
     * Results are not saved until you call {@link #save()}.
     */
    public void unlockContent(UnlockableContent content){
        if(content.alwaysUnlocked()) return;

        //fire unlock event so other classes can use it
        if(unlocked.getOr(content.getContentType(), ObjectSet::new).add(content.name)){
            modified = true;
            content.onUnlock();
            Events.fire(new UnlockEvent(content));
        }
    }

    /** Clears all unlocked content. Automatically saves. */
    public void reset(){
        save();
    }

    public void checkSave(){
        if(modified){
            save();
            modified = false;
        }
    }

    @SuppressWarnings("unchecked")
    public void load(){
        items.clear();
        unlocked = Core.settings.getObject("unlocks", ObjectMap.class, ObjectMap::new);
        for(Item item : Vars.content.items()){
            items.put(item, Core.settings.getInt("item-" + item.name, 0));
        }

        //set up default values
        if(!Core.settings.has("item-" + Items.copper.name)){
            addItem(Items.copper, 50);
        }
    }

    public void save(){
        Core.settings.putObject("unlocks", unlocked);
        for(Item item : Vars.content.items()){
            Core.settings.put("item-" + item.name, items.get(item, 0));
        }
        Core.settings.save();
    }

}
