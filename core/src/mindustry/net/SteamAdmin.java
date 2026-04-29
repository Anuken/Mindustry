package mindustry.net;

import arc.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.io.*;

/** Handles a database of banned Steam users. */
public class SteamAdmin{
    private static boolean scheduled = false;
    private static SteamAdminData data = new SteamAdminData();
    private static final float checkInterval = 60f * 5f;

    public static void fetch(){
        fetch(false);
    }

    public static void fetch(boolean cacheBust){
        if(!Vars.steam) return;

        if(cacheBust){ //specific commit avoids the 5 minute cache on the /master/ ref
            Http.get(Vars.ghApi + "/repos/Anuken/MindustrySteamBans/commits?path=data.json&per_page=1").submit(res -> { //fetch latest commit from api
                try{
                    fetchImpl(Vars.steamBansURLs[0].replace("master", Jval.read(res.getResultAsString()).asArray().first().getString("sha")));
                }catch(Exception e){
                    Log.err("Failed to fetch latest commit", e);
                    fetchImpl(Vars.steamBansURLs[0]);
                }
            });
        }else{
            fetchImpl(Vars.steamBansURLs[0]);
        }
    }

    private static void fetchImpl(String githubURL){
        fetch(githubURL, () -> fetch(Vars.steamBansURLs[1], () -> {}));
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
                    //kick newly banned people immediately
                    Groups.player.each(p -> data.bans.contains(p.uuid()), p -> p.kick(Packets.KickReason.banned));
                }catch(Throwable e){
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

    private static class SteamAdminData{
        ObjectSet<String> bans = new ObjectSet<>();
        ObjectSet<String> admins = new ObjectSet<>();
    }
}
