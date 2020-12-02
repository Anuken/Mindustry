package mindustry.tools;

import arc.*;
import arc.files.*;
import arc.util.*;
import arc.util.io.*;

import java.io.*;

/* icon font pipeline:
 1. take set of pre-defined icons and SVGs
 2. use Fontello API to get a font with these
 3. combine fontello font and standard font, get output font
 4. use json to generate a file with constants for every icon size+type (during annotation processing)
 */
public class FontGenerator{

    //E000 to F8FF
    public static void main(String[] args){
        Net net = Core.net = new Net();
        net.setBlock(true);
        Fi folder = Fi.get("core/assets-raw/fontgen/out/");
        folder.mkdirs();

        Log.info("Session...");

        OS.exec("curl", "--fail", "--output", "core/assets-raw/fontgen/out/session", "--form", "config=@core/assets-raw/fontgen/config.json", "https://fontello.com");

        Log.info("Zip...");

        String session = folder.child("session").readString();
        net.httpGet("https://fontello.com/" + session + "/get", result -> {
            try{
                Streams.copy(result.getResultAsStream(), folder.child("font.zip").write());
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }, Log::err);

        Log.info("Icon font...");

        ZipFi zip = new ZipFi(folder.child("font.zip"));
        Fi dest = folder.child("font.woff");
        zip.list()[0].child("font").child("fontello.ttf").copyTo(dest);
        dest.copyTo(Fi.get("core/assets/fonts/icon.ttf"));

        Log.info("Merge...");

        //TODO this is broken

        Log.info(OS.exec("fontforge", "-script",
            Fi.get("core/assets-raw/fontgen/merge.pe").absolutePath(),
            Fi.get("core/assets/fonts/font.woff").absolutePath(),
            Fi.get("core/assets-raw/fontgen/out/font.woff").absolutePath())
        );

        Log.info("Done.");
    }
}
