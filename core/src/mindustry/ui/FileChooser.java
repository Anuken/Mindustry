package mindustry.ui;

import arc.*;
import arc.files.*;
import arc.func.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.Platform.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class FileChooser{

    public static void export(String name, String extension, FileWriter writer){
        if(!ios){
            FileChooser.save(extension).name(name).submit(file -> {
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

    public static FileChooserParams open(String... extensions){
        return new FileChooserParams().open(true).extensions(extensions);
    }

    public static FileChooserParams save(String... extensions){
        return new FileChooserParams().open(false).extensions(extensions);
    }

    public static FileChooserDialog createFallbackFileChooser(FileChooserParams params){
        return new FileChooserDialog(params.title, file -> Structs.contains(params.extensions, file::extEquals), params.open, file -> {
            if(!params.open){
                params.handleChooseResult(file.parent().child(file.nameWithoutExtension() + "." + params.extensions[0]));
            }else{
                params.handleChooseResult(file);
            }
        });
    }

    public static void showFallbackFileChooser(FileChooserParams params){
        createFallbackFileChooser(params).show();
    }

    public static class FileChooserParams{
        public boolean open;
        public boolean allowMultiple;
        public String title;
        public String fileName;
        public String[] extensions;
        public @Nullable Cons<Fi> handler;
        public @Nullable Cons<Fi[]> multipleHandler;

        private void checkParams(){
            if(extensions == null || extensions.length == 0) throw new IllegalArgumentException("Extension types must be defined.");

            if(title == null){
                title = open ? Core.bundle.get("open") : Core.bundle.get("save");
            }else if(title.startsWith("@")){
                title = Core.bundle.get(title.substring(1));
            }

            if(fileName == null){
                fileName = "file." + extensions[0];
            }
        }

        /** Submits the request to handle a single file. */
        public void submit(Cons<Fi> handler){
            checkParams();
            this.handler = handler;
            Vars.platform.showFileChooser(this);
        }

        /** Submits the request to handle multiple files. */
        public void submitMulti(Cons<Fi[]> multipleHandler){
            checkParams();
            if(!open) throw new IllegalArgumentException("Saving in a file chooser with multiple choices does not make sense.");

            this.multipleHandler = multipleHandler;
            this.allowMultiple = true;
            Vars.platform.showFileChooser(this);
        }

        /** Submits the request to handle multiple files as a loop that iterates through the resulting files and invokes the callback repeatedly. */
        public void submitMultiLoop(Cons<Fi> multipleHandler){
            submitMulti(files -> {
                for(var f : files){
                    multipleHandler.get(f);
                }
            });
        }

        public FileChooserParams name(String fileName){
            this.fileName = fileName;
            return this;
        }

        public FileChooserParams extensions(String... extensions){
            this.extensions = extensions;
            return this;
        }

        public FileChooserParams open(boolean open){
            this.open = open;
            return this;
        }

        public FileChooserParams title(String title){
            this.title = title;
            return this;
        }

        public void handleChooseResult(Fi... files){
            if(files.length == 0) return;

            if(handler != null){
                handler.get(files[0]);
            }else if(multipleHandler != null){
                multipleHandler.get(files);
            }
        }
    }
}
