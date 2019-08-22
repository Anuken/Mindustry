package io.anuke.mindustry.desktop;

import com.codedisaster.steamworks.*;
import io.anuke.arc.Files.*;
import io.anuke.arc.backends.sdl.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.core.*;
import io.anuke.mindustry.desktop.steam.*;
import io.anuke.mindustry.net.*;

public class DesktopLauncher{

    public static void main(String[] arg){
        try{
            Platform.instance = new DesktopPlatform(arg);

            if(SteamAPI.isSteamRunning()){
                Net.setClientProvider(new ArcNetClient());
                Net.setServerProvider(new SteamServerImpl(new ArcNetServer()));
            }else{
                Net.setClientProvider(new ArcNetClient());
                Net.setServerProvider(new ArcNetServer());
            }

            new SdlApplication(new Mindustry(), new SdlConfig(){{
                title = "Mindustry";
                maximized = true;
                depth = 0;
                stencil = 0;
                width = 900;
                height = 700;
                setWindowIcon(FileType.Internal, "icons/icon_64.png");
            }});
        }catch(Throwable e){
            DesktopPlatform.handleCrash(e);
        }
    }
}
