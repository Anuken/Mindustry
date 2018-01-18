package io.anuke.mindustry.ui.dialogs;

import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Host;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.style.Drawable;
import io.anuke.ucore.scene.ui.Dialog;
import io.anuke.ucore.scene.ui.ScrollPane;
import io.anuke.ucore.scene.ui.TextButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Strings;

public class JoinDialog extends FloatingDialog {
    Array<Server> servers = new Array<>();
    Dialog join;
    Server renaming;
    Table local = new Table();
    Table remote = new Table();
    Table hosts = new Table();
    float w = 500;

    public JoinDialog(){
        super("$text.joingame");

        loadServers();

        addCloseButton();

        join = new FloatingDialog("$text.joingame.title");
        join.content().add("$text.joingame.ip").left();
        Mindustry.platforms.addDialog(join.content().addField(Settings.getString("ip"),text ->{
            Settings.putString("ip", text);
            Settings.save();
        }).size(240f, 54f).get(), 100);

        join.content().row();
        /*
        join.content().add("$text.server.port").left();
        Mindustry.platforms.addDialog(join.content()
                .addField(Settings.getString("port"), new DigitsOnlyFilter(), text ->{
                    Settings.putString("port", text);
                    Settings.save();
                }).size(240f, 54f).get());*/
        join.buttons().defaults().size(140f, 60f).pad(4f);
        join.buttons().addButton("$text.cancel", join::hide);
        join.buttons().addButton("$text.ok", () -> {
            if(renaming == null) {
                Server server = new Server(Settings.getString("ip"), Strings.parseInt(Settings.getString("port")));
                servers.add(server);
                saveServers();
                setupRemote();
                refreshRemote();
            }else{
                //renaming.port = Strings.parseInt(Settings.getString("port"));
                renaming.ip = Settings.getString("ip");
                saveServers();
                setupRemote();
                refreshRemote();
            }
            join.hide();
        }).disabled(b -> Settings.getString("ip").isEmpty() || Net.active());

        join.shown(() -> {
            join.getTitleLabel().setText(renaming != null ? "$text.server.edit" : "$text.server.add");
        });

        setup();

        shown(() -> {
            refreshLocal();
            refreshRemote();
        });
    }

    void setupRemote(){
        remote.clear();
        for (Server server : servers) {
            //why are java lambdas this bad
            TextButton[] buttons = {null};

            TextButton button = buttons[0] = remote.addButton("[accent]"+server.ip, "clear", () -> {
                if(!buttons[0].childrenPressed()) connect(server.ip, Vars.port);
            }).width(w).height(120f).pad(4f).get();

            button.getLabel().setWrap(true);

            Table inner = new Table();
            button.clearChildren();
            button.add(inner).growX();

            inner.add(button.getLabel()).growX();

            inner.addImageButton("icon-loading", "empty", 16*2, () -> {
                refreshServer(server);
            }).margin(3f).padTop(6f).top().right();

            inner.addImageButton("icon-pencil", "empty", 16*2, () -> {
                renaming = server;
                join.show();
            }).margin(3f).padTop(6f).top().right();

            inner.addImageButton("icon-trash-16", "empty", 16*2, () -> {
                Vars.ui.showConfirm("$text.confirm", "$text.server.delete", () -> {
                    servers.removeValue(server, true);
                    saveServers();
                    setupRemote();
                    refreshRemote();
                });
            }).margin(3f).pad(6).top().right();

            button.row();

            server.content = button.table(t -> {}).grow().get();

            remote.row();
        }
    }

    void refreshRemote(){
        for(Server server : servers){
            refreshServer(server);
        }
    }

    void refreshServer(Server server){
        server.content.clear();
        server.content.label(() -> Bundles.get("text.server.refreshing") + Strings.animated(4, 11, "."));

        Net.pingHost(server.ip, server.port, host -> {
            server.content.clear();

            server.content.add("[lightgray]" + Bundles.format("text.server.hostname", host.name)).pad(4);
            server.content.row();
            server.content.add("[lightgray]" + (host.players != 1 ? Bundles.format("text.players", host.players) :
                    Bundles.format("text.players.single", host.players)));
        }, e -> {
            server.content.clear();
            server.content.add("$text.host.invalid");
        });
    }

    void refreshLocal(){
        if(!Vars.gwt) {
            local.clear();
            local.background("button");
            local.label(() -> "[accent]" + Bundles.get("text.hosts.discovering") + new String(new char[(int) (Timers.time() / 10) % 4]).replace("\0", ".")).pad(10f);
            Net.discoverServers(this::addLocalHosts);
        }
    }

    void setup(){
        hosts.clear();

        hosts.add(remote).growX();
        hosts.row();
        hosts.add(local).growX();

        ScrollPane pane = new ScrollPane(hosts, "clear");
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        setupRemote();
        refreshRemote();

        content().clear();
        content().table(t -> {
            t.add("$text.name").padRight(10);
            t.addField(Settings.getString("name"), text -> {
                if(text.isEmpty()) return;
                Vars.player.name = text;
                Settings.put("name", text);
                Settings.save();
            }).grow().pad(8);
        }).width(w).height(70f).pad(4);
        content().row();
        content().add(pane).width(w).pad(0);
        content().row();
        content().addCenteredImageTextButton("$text.server.add", "icon-add", "clear", 14*3, () -> {
            renaming = null;
            join.show();
        }).width(w).height(80f);
    }

    void addLocalHosts(Array<Host> array){
        local.clear();

        if(array.size == 0){
            local.add("$text.hosts.none").pad(10f);
            local.add().growX();
            local.addImageButton("icon-loading", 16*2f, this::refreshLocal).pad(-10f).padLeft(0).padTop(-6).size(70f, 74f);
        }else {
            for (Host a : array) {
                TextButton button = local.addButton("[accent]"+a.name, "clear", () -> {
                    connect(a.address, Vars.port);
                }).width(w).height(80f).pad(4f).get();
                button.left();
                button.row();
                button.add("[lightgray]" + (a.players != 1 ? Bundles.format("text.players", a.players) :
                        Bundles.format("text.players.single", a.players)));
                button.row();
                button.add("[lightgray]" + a.address).pad(4).left();

                local.row();
                local.background((Drawable) null);
            }
        }
    }

    void connect(String ip, int port){
        Vars.ui.loadfrag.show("$text.connecting");

        Timers.runTask(2f, () -> {
            try{
                Vars.netClient.beginConnecting();
                Net.connect(ip, port);
                hide();
                join.hide();
            }catch (Exception e) {
                Throwable t = e;
                while(t.getCause() != null){
                    t = t.getCause();
                }
                //TODO localize
                String error = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
                if(error.contains("connection refused")) {
                    error = "connection refused";
                }else if(error.contains("port out of range")){
                    error = "invalid port!";
                }else if(error.contains("invalid argument")) {
                    error = "invalid IP or port!";
                }else if(t.getClass().toString().toLowerCase().contains("sockettimeout")){
                    error = "timed out!\nmake sure the host has port forwarding set up,\nand that the address is correct!";
                }else{
                    error = Strings.parseException(e, false);
                }
                Vars.ui.showError(Bundles.format("text.connectfail", error));
                Vars.ui.loadfrag.hide();

                e.printStackTrace();
            }
        });
    }

    private void loadServers(){
        String h = Settings.getString("servers");
        String[] list = h.split("\\|\\|\\|");
        for(String fname : list){
            if(fname.isEmpty()) continue;
            String[] split = fname.split(":");
            String host = split[0];
            int port = Strings.parseInt(split[1]);

            if(port != Integer.MIN_VALUE) servers.add(new Server(host, port));
        }
    }

    private void saveServers(){
        StringBuilder out = new StringBuilder();
        for(Server server : servers){
            out.append(server.ip);
            out.append(":");
            out.append(server.port);
            out.append("|||");
        }
        Settings.putString("servers", out.toString());
        Settings.save();
    }

    private class Server{
        public String ip;
        public int port;
        public Host host;
        public Table content;

        public Server(String ip, int port){
            this.ip = ip;
            this.port = port;
        }
    }
}
