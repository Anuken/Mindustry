package mindustry.net;

import arc.*;

public class ServerGroup{
    public String name;
    public String[] addresses;
    public boolean prioritized = false;

    public ServerGroup(String name, String[] addresses, boolean prioritized){
        this.name = name;
        this.addresses = addresses;
        this.prioritized = prioritized;
    }

    public ServerGroup(String name, String[] addresses){
        this(name, addresses, false);
    }

    public ServerGroup(){
    }

    public boolean hidden(){
        return Core.settings.getBool(key() + "-hidden", false);
    }

    public boolean favorite(){
        return Core.settings.getBool(key() + "-favorite", false);
    }

    public void setHidden(boolean hidden){
        Core.settings.put(key() + "-hidden", hidden);
    }

    public void setFavorite(boolean favorite){
        Core.settings.put(key() + "-favorite", favorite);
    }

    String key(){
        return "server-" + (name.isEmpty() ? addresses.length == 0 ? "" : addresses[0] : name);
    }
}
