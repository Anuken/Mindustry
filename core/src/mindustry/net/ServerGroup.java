package mindustry.net;

import arc.*;

public class ServerGroup{
    public String name;
    public String[] addresses;
    public boolean priority = false;

    public ServerGroup(String name, String[] addresses, boolean priority){
        this.name = name;
        this.addresses = addresses;
        this.priority = priority;
    }

    public ServerGroup(String name, String[] addresses){
        this(name, addresses, false);
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
        return "server-" + (priority ? "priority-" : "") + (name.isEmpty() ? addresses.length == 0 ? "" : addresses[0] : name);
    }
}
