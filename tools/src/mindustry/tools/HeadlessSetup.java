package mindustry.tools;

import arc.*;
import arc.backend.headless.*;
import arc.files.*;
import arc.util.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.editor.*;
import mindustry.net.*;

import java.util.*;

import static mindustry.Vars.*;

public class HeadlessSetup{

    public static void setup(){
        ApplicationCore core = new ApplicationCore(){
            @Override
            public void setup(){
                Core.settings.setDataDirectory(new Fi("../../tools/build/test_data"));
                Core.bundle = I18NBundle.createBundle(Core.files.internal("bundles/bundle"), Locale.ENGLISH);
                headless = true;
                net = new Net(null);
                tree = new FileTree();
                Vars.init();
                world = new World();
                content.createBaseContent();
                mods.loadScripts();
                content.createModContent();

                add(logic = new Logic());
                add(netServer = new NetServer());

                content.init();
                editor = new MapEditor();
            }
        };

        new HeadlessApplication(core){
            @Override
            protected void initialize(){
                //don't create a thread, just init on the main thread
                for(ApplicationListener listener : listeners){
                    listener.init();
                }
            }
        };
    }
}
