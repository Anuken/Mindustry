package mindustry.core;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.mod.*;
import mindustry.net.*;
import mindustry.net.Net.*;
import mindustry.type.*;
import mindustry.ui.dialogs.*;
import rhino.*;

import java.io.*;
import java.net.*;

import static mindustry.Vars.*;

public interface Platform{

    /** Dynamically creates a class loader for a jar file. This loader must be child-first. */
    default ClassLoader loadJar(Fi jar, ClassLoader parent) throws Exception{
        return new URLClassLoader(new URL[]{jar.file().toURI().toURL()}, parent){
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
                //check for loaded state
                Class<?> loadedClass = findLoadedClass(name);
                if(loadedClass == null){
                    try{
                        //try to load own class first
                        loadedClass = findClass(name);
                    }catch(ClassNotFoundException e){
                        //use parent if not found
                        return parent.loadClass(name);
                    }
                }

                if(resolve){
                    resolveClass(loadedClass);
                }
                return loadedClass;
            }
        };
    }

    /** Steam: Update lobby visibility.*/
    default void updateLobby(){}

    /** Steam: Show multiplayer friend invite dialog.*/
    default void inviteFriends(){}

    /** Steam: Share a map on the workshop.*/
    default void publish(Publishable pub){}

    /** Steam: View a listing on the workshop.*/
    default void viewListing(Publishable pub){}

    /** Steam: View a listing on the workshop by an ID.*/
    default void viewListingID(String mapid){}

    /** Steam: Return external workshop maps to be loaded.*/
    default Seq<Fi> getWorkshopContent(Class<? extends Publishable> type){
        return new Seq<>(0);
    }

    /** Steam: Open workshop for maps.*/
    default void openWorkshop(){}

    /** Get the networking implementation.*/
    default NetProvider getNet(){
        return new ArcNetProvider();
    }

    /** Gets the scripting implementation. */
    default Scripts createScripts(){
        return new Scripts();
    }

    default Context getScriptContext(){
        Context context = Context.getCurrentContext();
        if(context == null) context = Context.enter();
        context.setOptimizationLevel(9);
        return context;
    }

    /** Update discord RPC. */
    default void updateRPC(){
    }

    /** Must be a base64 string 8 bytes in length. */
    default String getUUID(){
        String uuid = Core.settings.getString("uuid", "");
        if(uuid.isEmpty()){
            byte[] result = new byte[8];
            new Rand().nextBytes(result);
            uuid = new String(Base64Coder.encode(result));
            Core.settings.put("uuid", uuid);
            return uuid;
        }
        return uuid;
    }

    /** Only used for iOS or android: open the share menu for a map or save. */
    default void shareFile(Fi file){
    }

    default void export(String name, String extension, FileWriter writer){
        if(!ios){
            platform.showFileChooser(false, extension, file -> {
                ui.loadAnd(() -> {
                    try{
                        writer.write(file);
                    }catch(Throwable e){
                        ui.showException(e);
                        Log.err(e);
                    }
                });
            });
        }else{
            ui.loadAnd(() -> {
                try{
                    Fi result = Core.files.local(name + "." + extension);
                    writer.write(result);
                    platform.shareFile(result);
                }catch(Throwable e){
                    ui.showException(e);
                    Log.err(e);
                }
            });
        }
    }

    /**
     * Show a file chooser.
     * @param cons Selection listener
     * @param open Whether to open or save files
     * @param extension File extension to filter
     * @param title The title of the native dialog
     */
    default void showFileChooser(boolean open, String title, String extension, Cons<Fi> cons){
        if(OS.isLinux && !OS.isAndroid){
            showZenity(open, title, new String[]{extension}, cons, () -> defaultFileDialog(open, title, extension, cons));
        }else{
            defaultFileDialog(open, title, extension, cons);
        }
    }

    /** attempt to use the native file picker with zenity, or runs the fallback Runnable if the operation fails */
    static void showZenity(boolean open, String title, String[] extensions, Cons<Fi> cons, Runnable fallback){
        Threads.daemon(() -> {
            try{
                String formatted = (title.startsWith("@") ? Core.bundle.get(title.substring(1)) : title).replaceAll("\"", "'");

                String last = FileChooser.getLastDirectory().absolutePath();
                if(!last.endsWith("/")) last += "/";

                //zenity doesn't support filtering by extension
                Seq<String> args = Seq.with("zenity",
                    "--file-selection",
                    "--title=" + formatted,
                    "--filename=" + last,
                    "--confirm-overwrite",
                    "--file-filter=" + Seq.with(extensions).toString(" ", s -> "*." + s),
                    "--file-filter=All files | *" //allow anything if the user wants
                );

                if(!open){
                    args.add("--save");
                }

                String result = OS.exec(args.toArray(String.class));
                //first line.
                if(result.length() > 1 && result.contains("\n")){
                    result = result.split("\n")[0];
                }

                //cancelled selection, ignore result
                if(result.isEmpty() || result.equals("\n")) return;

                if(result.endsWith("\n")) result = result.substring(0, result.length() - 1);
                if(result.contains("\n")) throw new IOException("invalid input: \"" + result + "\"");

                Fi file = Core.files.absolute(result);
                Core.app.post(() -> {
                    FileChooser.setLastDirectory(file.isDirectory() ? file : file.parent());

                    if(!open){
                        cons.get(file.parent().child(file.nameWithoutExtension() + "." + extensions[0]));
                    }else{
                        cons.get(file);
                    }
                });
            }catch(Exception e){
                Log.err(e);
                Log.warn("zenity not found, using non-native file dialog. Consider installing `zenity` for native file dialogs.");
                Core.app.post(fallback);
            }
        });
    }

    static void defaultFileDialog(boolean open, String title, String extension, Cons<Fi> cons){
        new FileChooser(title, file -> file.extEquals(extension), open, file -> {
            if(!open){
                cons.get(file.parent().child(file.nameWithoutExtension() + "." + extension));
            }else{
                cons.get(file);
            }
        }).show();
    }

    default void showFileChooser(boolean open, String extension, Cons<Fi> cons){
        showFileChooser(open, open ? "@open": "@save", extension, cons);
    }

    /**
     * Show a file chooser for multiple file types.
     * @param cons Selection listener
     * @param extensions File extensions to filter
     */
    default void showMultiFileChooser(Cons<Fi> cons, String... extensions){
        if(mobile){
            showFileChooser(true, extensions[0], cons);
        }else if(OS.isLinux && !OS.isAndroid){
            showZenity(true, "@open", extensions, cons, () -> defaultMultiFileChooser(cons, extensions));
        }else{
            defaultMultiFileChooser(cons, extensions);
        }
    }

    static void defaultMultiFileChooser(Cons<Fi> cons, String... extensions){
        new FileChooser("@open", file -> Structs.contains(extensions, file.extension().toLowerCase()), true, cons).show();
    }

    /** Hide the app. Android only. */
    default void hide(){
    }

    /** Forces the app into landscape mode.*/
    default void beginForceLandscape(){
    }

    /** Stops forcing the app into landscape orientation.*/
    default void endForceLandscape(){
    }

    interface FileWriter{
        void write(Fi file) throws Throwable;
    }
}
