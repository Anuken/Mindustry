package io.anuke.mindustry;

import io.anuke.arc.*;
import io.anuke.arc.backends.lwjgl3.*;
import io.anuke.arc.files.FileHandle;
import io.anuke.arc.graphics.Pixmap;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.game.EventType.GameLoadEvent;

public class Upscaler{

    public static void main(String[] args){
        Events.on(GameLoadEvent.class, e -> scale());
        new Lwjgl3Application(new Mindustry(), new Lwjgl3ApplicationConfiguration());
    }

    static void scale(){
        FileHandle file = Core.files.local("../assets-raw/sprites/ui/icons");

        SquareMarcher marcher = new SquareMarcher();

        for(FileHandle img : file.list()){
            if(img.extension().equals("png")){
                marcher.render(new Pixmap(img), Core.files.external("images/").child(img.name()));
            }
        }

        Log.info("done.");

        Core.app.exit();
    }
}
