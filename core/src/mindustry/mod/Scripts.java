package mindustry.mod;

import arc.*;
import arc.assets.*;
import arc.assets.loaders.MusicLoader.*;
import arc.assets.loaders.SoundLoader.*;
import arc.audio.*;
import arc.files.*;
import arc.func.*;
import arc.util.*;
import arc.util.Log.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.mod.Mods.*;
import rhino.*;
import rhino.module.*;
import rhino.module.provider.*;

import java.io.*;
import java.net.*;
import java.util.regex.*;

public class Scripts implements Disposable{
    public final Context context;
    public final Scriptable scope;

    private boolean errored;
    LoadedMod currentMod = null;

    public Scripts(){
        Time.mark();

        context = Vars.platform.getScriptContext();
        scope = new ImporterTopLevel(context);

        new RequireBuilder()
            .setModuleScriptProvider(new SoftCachingModuleScriptProvider(new ScriptModuleProvider()))
            .setSandboxed(true).createRequire(context, scope).install(scope);

        if(!run(Core.files.internal("scripts/global.js").readString(), "global.js", false)){
            errored = true;
        }
        Log.debug("Time to load script engine: @", Time.elapsed());
    }

    public boolean hasErrored(){
        return errored;
    }

    public String runConsole(String text){
        try{
            Object o = context.evaluateString(scope, text, "console.js", 1);
            if(o instanceof NativeJavaObject n) o = n.unwrap();
            if(o instanceof Undefined) o = "undefined";
            return String.valueOf(o);
        }catch(Throwable t){
            return getError(t, false);
        }
    }

    private String getError(Throwable t, boolean log){
        if(log) Log.err(t);
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
    }

    public void log(String source, String message){
        log(LogLevel.info, source, message);
    }

    public void log(LogLevel level, String source, String message){
        Log.log(level, "[@]: @", source, message);
    }

    //region utility mod functions

    public float[] newFloats(int capacity){
        return new float[capacity];
    }

    public String readString(String path){
        return Vars.tree.get(path, true).readString();
    }

    public byte[] readBytes(String path){
        return Vars.tree.get(path, true).readBytes();
    }

    public Sound loadSound(String soundName){
        if(Vars.headless) return new Sound();

        String name = "sounds/" + soundName;
        String path = Vars.tree.get(name + ".ogg").exists() ? name + ".ogg" : name + ".mp3";

        var sound = new Sound();
        AssetDescriptor<?> desc = Core.assets.load(path, Sound.class, new SoundParameter(sound));
        desc.errored = Throwable::printStackTrace;

        return sound;
    }

    public Music loadMusic(String soundName){
        if(Vars.headless) return new Music();

        String name = "music/" + soundName;
        String path = Vars.tree.get(name + ".ogg").exists() ? name + ".ogg" : name + ".mp3";

        var music = new Music();
        AssetDescriptor<?> desc = Core.assets.load(path, Music.class, new MusicParameter(music));
        desc.errored = Throwable::printStackTrace;

        return music;
    }

    /** Ask the user to select a file to read for a certain purpose like "Please upload a sprite" */
    public void readFile(String purpose, String ext, Cons<String> cons){
        selectFile(true, purpose, ext, fi -> cons.get(fi.readString()));
    }

    /** readFile but for a byte[] */
    public void readBinFile(String purpose, String ext, Cons<byte[]> cons){
        selectFile(true, purpose, ext, fi -> cons.get(fi.readBytes()));
    }

    /** Ask the user to write a file. */
    public void writeFile(String purpose, String ext, String contents){
        if(contents == null) contents = "";
        final String fContents = contents;
        selectFile(false, purpose, ext, fi -> fi.writeString(fContents));
    }

    /** writeFile but for a byte[] */
    public void writeBinFile(String purpose, String ext, byte[] contents){
        if(contents == null) contents = Streams.emptyBytes;
        final byte[] fContents = contents;
        selectFile(false, purpose, ext, fi -> fi.writeBytes(fContents));
    }

    private void selectFile(boolean open, String purpose, String ext, Cons<Fi> cons){
        purpose = purpose.startsWith("@") ? Core.bundle.get(purpose.substring(1)) : purpose;
        //add purpose and extension at the top
        String title = Core.bundle.get(open ? "open" : "save") + " - " + purpose + " (." + ext + ")";
        Vars.platform.showFileChooser(open, title, ext, fi -> {
            try{
                cons.get(fi);
            }catch(Exception e){
                Log.err("Failed to select file '@' for a mod", fi);
                Log.err(e);
            }
        });
    }

    //endregion

    public void run(LoadedMod mod, Fi file){
        currentMod = mod;
        run(file.readString(), file.name(), true);
        currentMod = null;
    }

    private boolean run(String script, String file, boolean wrap){
        try{
            if(currentMod != null){
                //inject script info into file
                context.evaluateString(scope, "modName = \"" + currentMod.name + "\"\nscriptName = \"" + file + "\"", "initscript.js", 1);
            }
            context.evaluateString(scope,
            wrap ? "(function(){'use strict';\n" + script + "\n})();" : script,
            file, 0);
            return true;
        }catch(Throwable t){
            if(currentMod != null){
                file = currentMod.name + "/" + file;
            }
            log(LogLevel.err, file, "" + getError(t, true));
            return false;
        }
    }

    @Override
    public void dispose(){
        Context.exit();
    }

    private class ScriptModuleProvider extends UrlModuleSourceProvider{
        private final Pattern directory = Pattern.compile("^(.+?)/(.+)");

        public ScriptModuleProvider(){
            super(null, null);
        }

        @Override
        public ModuleSource loadSource(String moduleId, Scriptable paths, Object validator) throws URISyntaxException{
            if(currentMod == null) return null;
            return loadSource(moduleId, currentMod.root.child("scripts"), validator);
        }

        private ModuleSource loadSource(String moduleId, Fi root, Object validator) throws URISyntaxException{
            Matcher matched = directory.matcher(moduleId);
            if(matched.find()){
                LoadedMod required = Vars.mods.locateMod(matched.group(1));
                String script = matched.group(2);
                if(required == null){ // Mod not found, treat it as a folder
                    Fi dir = root.child(matched.group(1));
                    if(!dir.exists()) return null; // Mod and folder not found
                    return loadSource(script, dir, validator);
                }

                currentMod = required;
                return loadSource(script, required.root.child("scripts"), validator);
            }

            Fi module = root.child(moduleId + ".js");
            if(!module.exists() || module.isDirectory()) return null;
            return new ModuleSource(
                new InputStreamReader(new ByteArrayInputStream((module.readString()).getBytes())),
                new URI(moduleId), root.file().toURI(), validator);
        }
    }
}
