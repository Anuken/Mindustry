package mindustry.net;

import arc.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.io.*;

/** Handles a database of banned Steam users. */
public class SteamAdmin{
    private static boolean scheduled = false;
    private static SteamAdminData data = new SteamAdminData();
    private static final float checkInterval = 60f * 5f;

    public static void fetch(){
        if(!Vars.steam) return;

        fetch(Vars.steamBansURLs[0], () -> fetch(Vars.steamBansURLs[1], () -> {}));
        if(!scheduled){
            scheduled = true;
            Timer.schedule(SteamAdmin::fetch, checkInterval, checkInterval);
        }
    }

    private static void fetch(String url, Runnable fail){
        Http.get(url)
        .error(t -> {
            Log.err("Failed to fetch Steam bans", t);
            fail.run();
        }).submit(result -> {
            String text = result.getResultAsString();
            Core.app.post(() -> {
                try{
                    data = JsonIO.read(SteamAdminData.class, text);
                }catch(Exception e){
                    Log.err("Failed to parse Steam ban data", e);
                }
            });
        });
    }

    public static boolean isBanned(String id){
        if(!id.startsWith("steam:")) return false;
        return data.bans.contains(id.substring("steam:".length()));
    }

    public static boolean isAdmin(String id){
        if(!id.startsWith("steam:")) return false;
        return data.admins.contains(id.substring("steam:".length()));
    }

    static class SteamAdminData{
        ObjectSet<String> bans = new ObjectSet<>();
        ObjectSet<String> admins = new ObjectSet<>();
    }
}
