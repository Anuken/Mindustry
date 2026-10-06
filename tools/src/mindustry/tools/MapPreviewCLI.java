package mindustry.tools;

import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.graphics.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.maps.Map;
import mindustry.net.*;

import java.util.*;

import static mindustry.Vars.*;

/** Usage: MapPreviewCLI in.msav out.png */
public class MapPreviewCLI{

    public static void main(String[] args) throws Exception{
        if(args.length != 2){
            System.err.println("Usage: MapPreviewCLI <in.msav> <out.png>");
            System.exit(1);
        }
        //java.home is unset in a native image, but Vars.init reads it
        if(System.getProperty("java.home") == null) System.setProperty("java.home", System.getProperty("java.io.tmpdir"));

        Fi in = new Fi(args[0]), out = new Fi(args[1]);
        Version.enabled = false;
        //skip default map loading, as the map assets aren't bundled
        Reflect.set(Maps.class, "defaultMapNames", new String[0]);

        new HeadlessApplication(new ApplicationCore(){
            @Override
            public void setup(){
                Core.settings.setDataDirectory(new Fi(System.getProperty("java.io.tmpdir")).child("mindustry-preview"));
                headless = true;
                net = new Net(null);
                tree = new FileTree();
                Vars.init();
                //null maps makes sector presets skip loading their bundled maps
                maps = null;
                content.createBaseContent();
                content.createModContent();
                content.init();
                content.loadColors();
            }
        }){
            @Override
            protected void initialize(){
                //init on the main thread
                for(ApplicationListener listener : listeners){
                    listener.init();
                }
            }
        };

        Map map = MapIO.createMap(in, true);
        Pixmap preview = MapIO.generatePreview(map);
        out.writePng(preview);
        preview.dispose();
        System.exit(0);
    }
}