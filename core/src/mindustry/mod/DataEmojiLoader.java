package mindustry.mod;

import arc.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.mod.data.*;
import mindustry.ui.*;

public class DataEmojiLoader{
    private Seq<String> loadedNames = new Seq<>();

    public void load(Seq<EmojiAsset> emojis){
        unload();

        //note: if mods exceed the emoji amount up to 0xebb0, there will be a desync between client and server of the codepoints used; this is extremely unlikely to happen in practice
        int start = Math.max(Fonts.getLastUsedModCodepoint() + 1, 0xebb0);

        for(var emoji : emojis){
            if(!Fonts.hasIcon(emoji.name)){
                TextureRegion region = Core.atlas.find(DataImagePacker.regionPrefix + emoji.name, emoji.name);
                if(!region.found()) continue;

                Fonts.registerIcon(emoji.name, start, region);
                loadedNames.add(emoji.name);

                start ++;
            }else{
                Log.warn("Emoji '@' attempts to overwrite an existing vanilla emoji, skipping.", emoji.name);
            }
        }
    }

    public void unload(){
        if(loadedNames.isEmpty()) return;

        for(String s : loadedNames){
            Fonts.unregisterIcon(s);
        }
        loadedNames.clear();
    }
}
