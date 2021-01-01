package mindustry.net;

import arc.*;

public class ServerGroup{
    public String name;
    public String[] addresses;

    public ServerGroup(String name, String[] addresses){
        this.name = name;
        this.addresses = addresses;
    }

    public ServerGroup(){
    }

    public boolean hidden(){
        return Core.settings.getBool(key() + "-hidden", false);
    }

    public void setHidden(boolean hidden){
        Core.settings.put(key() + "-hidden", hidden);
    }

    String key(){
        return "server-" + (name.isEmpty() ? addresses.length == 0 ? "" : addresses[0] : name);
    }
}
