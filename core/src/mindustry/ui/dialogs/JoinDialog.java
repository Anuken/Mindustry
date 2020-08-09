package mindustry.ui.dialogs;

import arc.*;
import arc.input.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.core.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.legacy.*;
import mindustry.net.*;
import mindustry.net.Packets.*;
import mindustry.ui.*;

import static mindustry.Vars.*;

public class JoinDialog extends BaseDialog{
    Seq<Server> servers = new Seq<>();
    Dialog add;
    Server renaming;
    Table local = new Table();
    Table remote = new Table();
    Table global = new Table();
    Table hosts = new Table();
    int totalHosts;

    public JoinDialog(){
        super("@joingame");

        loadServers();

        if(!steam) buttons.add().width(60f);
        buttons.add().growX().width(-1);

        addCloseButton();

        buttons.add().growX().width(-1);
        if(!steam){
            buttons.button("?", () -> ui.showInfo("@join.info")).size(60f, 64f).width(-1);
        }

        add = new BaseDialog("@joingame.title");
        add.cont.add("@joingame.ip").padRight(5f).left();

        TextField field = add.cont.field(Core.settings.getString("ip"), text -> {
            Core.settings.put("ip", text);
        }).size(320f, 54f).maxTextLength(100).addInputDialog().get();

        add.cont.row();
        add.buttons.defaults().size(140f, 60f).pad(4f);
        add.buttons.button("@cancel", add::hide);
        add.buttons.button("@ok", () -> {
            if(renaming == null){
                Server server = new Server();
                server.setIP(Core.settings.getString("ip"));
                servers.add(server);
            }else{
                renaming.setIP(Core.settings.getString("ip"));
            }
            saveServers();
            setupRemote();
            refreshRemote();
            add.hide();
        }).disabled(b -> Core.settings.getString("ip").isEmpty() || net.active());

        add.shown(() -> {
            add.title.setText(renaming != null ? "@server.edit" : "@server.add");
            if(renaming != null){
                field.setText(renaming.displayIP());
            }
        });

        keyDown(KeyCode.f5, this::refreshAll);

        shown(() -> {
            setup();
            refreshAll();

            if(!steam){
                Core.app.post(() -> Core.settings.getBoolOnce("joininfo", () -> ui.showInfo("@join.info")));
            }
        });

        onResize(() -> {
            setup();
            refreshAll();
        });
    }

    void refreshAll(){
        refreshLocal();
        refreshRemote();
        refreshGlobal();
    }

    void setupRemote(){
        remote.clear();

        for(Server server : servers){
            //why are java lambdas this bad
            TextButton[] buttons = {null};

            TextButton button = buttons[0] = remote.button("[accent]" + server.displayIP(), Styles.cleart, () -> {
                if(!buttons[0].childrenPressed()){
                    if(server.lastHost != null){
                        safeConnect(server.ip, server.port, server.lastHost.version);
                    }else{
                        connect(server.ip, server.port);
                    }
                }
            }).width(targetWidth()).pad(4f).get();

            button.getLabel().setWrap(true);

            Table inner = new Table();
            button.clearChildren();
            button.add(inner).growX();

            inner.add(button.getLabel()).growX();

            inner.button(Icon.upOpen, Styles.emptyi, () -> {
                moveRemote(server, -1);

            }).margin(3f).padTop(6f).top().right();

            inner.button(Icon.downOpen, Styles.emptyi, () -> {
                moveRemote(server, +1);

            }).margin(3f).pad(2).padTop(6f).top().right();

            inner.button(Icon.refresh, Styles.emptyi, () -> {
                refreshServer(server);
            }).margin(3f).pad(2).padTop(6f).top().right();

            inner.button(Icon.pencil, Styles.emptyi, () -> {
                renaming = server;
                add.show();
            }).margin(3f).pad(2).padTop(6f).top().right();

            inner.button(Icon.trash, Styles.emptyi, () -> {
                ui.showConfirm("@confirm", "@server.delete", () -> {
                    servers.remove(server, true);
                    saveServers();
                    setupRemote();
                    refreshRemote();
                });
            }).margin(3f).pad(2).pad(6).top().right();

            button.row();

            server.content = button.table(t -> {}).grow().get();

            remote.row();
        }
    }

    void moveRemote(Server server, int sign){
        int index = servers.indexOf(server);

        if(index + sign < 0) return;
        if(index + sign > servers.size - 1) return;

        servers.remove(index);
        servers.insert(index + sign, server);

        saveServers();
        setupRemote();
        for(Server other : servers){
            if(other.lastHost != null){
                setupServer(other, other.lastHost);
            }else{
                refreshServer(other);
            }
        }
    }

    void refreshRemote(){
        for(Server server : servers){
            refreshServer(server);
        }
    }

    void refreshServer(Server server){
        server.content.clear();
        server.content.label(() -> Core.bundle.get("server.refreshing") + Strings.animated(Time.time(), 4, 11, "."));

        net.pingHost(server.ip, server.port, host -> setupServer(server, host), e -> {
            server.content.clear();
            server.content.add("@host.invalid").padBottom(4);
        });
    }

    void setupServer(Server server, Host host){
        server.lastHost = host;
        server.content.clear();
        buildServer(host, server.content);
    }

    void buildServer(Host host, Table content){
        String versionString;

        if(host.version == -1){
            versionString = Core.bundle.format("server.version", Core.bundle.get("server.custombuild"), "");
        }else if(host.version == 0){
            versionString = Core.bundle.get("server.outdated");
        }else if(host.version < Version.build && Version.build != -1){
            versionString = Core.bundle.get("server.outdated") + "\n" +
            Core.bundle.format("server.version", host.version, "");
        }else if(host.version > Version.build && Version.build != -1){
            versionString = Core.bundle.get("server.outdated.client") + "\n" +
            Core.bundle.format("server.version", host.version, "");
        }else if(host.version == Version.build && Version.type.equals(host.versionType)){
            //not important
            versionString = "";
        }else{
            versionString = Core.bundle.format("server.version", host.version, host.versionType);
        }

        content.table(t -> {
            t.add("[lightgray]" + host.name + "   " + versionString).width(targetWidth() - 10f).left().get().setEllipsis(true);
            t.row();
            if(!host.description.isEmpty()){
                t.add("[gray]" + host.description).width(targetWidth() - 10f).left().wrap();
                t.row();
            }
            t.add("[lightgray]" + (Core.bundle.format("players" + (host.players == 1 && host.playerLimit <= 0 ? ".single" : ""), (host.players == 0 ? "[lightgray]" : "[accent]") + host.players + (host.playerLimit > 0 ? "[lightgray]/[accent]" + host.playerLimit : "")+ "[lightgray]"))).left();
            t.row();
            t.add("[lightgray]" + Core.bundle.format("save.map", host.mapname) + "[lightgray] / " + host.mode.toString()).width(targetWidth() - 10f).left().get().setEllipsis(true);
        }).expand().left().bottom().padLeft(12f).padBottom(8);
    }

    void setup(){
        local.clear();
        remote.clear();
        global.clear();
        float w = targetWidth();

        hosts.clear();

        section("@servers.local", local);
        section("@servers.remote", remote);
        section("@servers.global", global);

        ScrollPane pane = new ScrollPane(hosts);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false);

        setupRemote();

        cont.clear();
        cont.table(t -> {
            t.add("@name").padRight(10);
            if(!steam){
                t.field(Core.settings.getString("name"), text -> {
                    player.name(text);
                    Core.settings.put("name", text);
                }).grow().pad(8).addInputDialog(maxNameLength);
            }else{
                t.add(player.name).update(l -> l.setColor(player.color())).grow().pad(8);
            }

            ImageButton button = t.button(Tex.whiteui, Styles.clearFulli, 40, () -> {
                new PaletteDialog().show(color -> {
                    player.color().set(color);
                    Core.settings.put("color-0", color.rgba8888());
                });
            }).size(54f).get();
            button.update(() -> button.getStyle().imageUpColor = player.color());
        }).width(w).height(70f).pad(4);
        cont.row();
        cont.add(pane).width(w + 38).pad(0);
        cont.row();
        cont.buttonCenter("@server.add", Icon.add, () -> {
            renaming = null;
            add.show();
        }).marginLeft(10).width(w).height(80f).update(button -> {
            float pw = w;
            float pad = 0f;
            if(pane.getChildren().first().getPrefHeight() > pane.getHeight()){
                pw = w + 30;
                pad = 6;
            }

            Cell cell = ((Table)pane.parent).getCell(button);

            if(!Mathf.equal(cell.minWidth(), pw)){
                cell.width(pw);
                cell.padLeft(pad);
                pane.parent.invalidateHierarchy();
            }
        });
    }

    void section(String label, Table servers){
        Collapser coll = new Collapser(servers, Core.settings.getBool("collapsed-" + label, false));
        coll.setDuration(0.1f);

        hosts.table(name -> {
            name.add(label).pad(10).growX().left().color(Pal.accent);
            name.button(Icon.downOpen, Styles.emptyi, () -> {
                coll.toggle(false);
                Core.settings.put("collapsed-" + label, coll.isCollapsed());
            }).update(i -> i.getStyle().imageUp = (!coll.isCollapsed() ? Icon.upOpen : Icon.downOpen)).size(40f).right().padRight(10f);
        }).growX();
        hosts.row();
        hosts.image().growX().pad(5).padLeft(10).padRight(10).height(3).color(Pal.accent);
        hosts.row();
        hosts.add(coll).width(targetWidth());
        hosts.row();
    }

    void refreshLocal(){
        totalHosts = 0;

        local.clear();
        local.background(null);
        local.table(Tex.button, t -> t.label(() -> "[accent]" + Core.bundle.get("hosts.discovering.any") + Strings.animated(Time.time(), 4, 10f, ".")).pad(10f)).growX();
        net.discoverServers(this::addLocalHost, this::finishLocalHosts);
    }

    void refreshGlobal(){
        global.clear();
        global.background(null);
        for(String host : defaultServers){
            String resaddress = host.contains(":") ? host.split(":")[0] : host;
            int resport = host.contains(":") ? Strings.parseInt(host.split(":")[1]) : port;
            net.pingHost(resaddress, resport, res -> {
                res.port = resport;
                addGlobalHost(res);
            }, e -> {});
        }
    }

    void finishLocalHosts(){
        if(totalHosts == 0){
            local.clear();
            local.background(Tex.button);
            local.add("@hosts.none").pad(10f);
            local.add().growX();
            local.button(Icon.refresh, this::refreshLocal).pad(-12f).padLeft(0).size(70f);
        }else{
            local.background(null);
        }
    }

    void addLocalHost(Host host){
        if(totalHosts == 0){
            local.clear();
        }
        local.background(null);
        totalHosts++;
        float w = targetWidth();

        local.row();

        TextButton button = local.button("", Styles.cleart, () -> safeConnect(host.address, host.port, host.version))
        .width(w).pad(5f).get();
        button.clearChildren();
        buildServer(host, button);
    }

    void addGlobalHost(Host host){
        global.background(null);
        float w = targetWidth();

        global.row();

        TextButton button = global.button("", Styles.cleart, () -> safeConnect(host.address, host.port, host.version))
        .width(w).pad(5f).get();
        button.clearChildren();
        buildServer(host, button);
    }

    public void connect(String ip, int port){
        if(player.name.trim().isEmpty()){
            ui.showInfo("@noname");
            return;
        }

        ui.loadfrag.show("@connecting");

        ui.loadfrag.setButton(() -> {
            ui.loadfrag.hide();
            netClient.disconnectQuietly();
        });

        Time.runTask(2f, () -> {
            logic.reset();
            net.reset();
            Vars.netClient.beginConnecting();
            net.connect(ip, port, () -> {
                hide();
                add.hide();
            });
        });
    }

    void safeConnect(String ip, int port, int version){
        if(version != Version.build && Version.build != -1 && version != -1){
            ui.showInfo("[scarlet]" + (version > Version.build ? KickReason.clientOutdated : KickReason.serverOutdated).toString() + "\n[]" +
            Core.bundle.format("server.versions", Version.build, version));
        }else{
            connect(ip, port);
        }
    }

    float targetWidth(){
        return Math.min(Core.graphics.getWidth() / Scl.scl() * 0.9f, 500f);
    }

    @SuppressWarnings("unchecked")
    private void loadServers(){
        servers = Core.settings.getJson("servers", Seq.class, Server.class, Seq::new);

        //load imported legacy data
        if(Core.settings.has("server-list")){
            servers = LegacyIO.readServers();
            Core.settings.remove("server-list");
        }

        //get servers
        Core.net.httpGet(becontrol.active() ? serverJsonBeURL : serverJsonURL, result -> {
            try{
                Jval val = Jval.read(result.getResultAsString());
                Core.app.post(() -> {
                    try{
                        defaultServers.clear();
                        val.asArray().each(child -> defaultServers.add(child.getString("address", "<invalid>")));
                        Log.info("Fetched @ global servers.", defaultServers.size);
                    }catch(Throwable ignored){}
                });
            }catch(Throwable ignored){}
        }, t -> {});
    }

    private void saveServers(){
        Core.settings.putJson("servers", Server.class, servers);
    }

    public static class Server{
        public String ip;
        public int port;

        transient Table content;
        transient Host lastHost;

        void setIP(String ip){

            //parse ip:port, if unsuccessful, use default values
            if(ip.lastIndexOf(':') != -1 && ip.lastIndexOf(':') != ip.length() - 1){
                try{
                    int idx = ip.lastIndexOf(':');
                    this.ip = ip.substring(0, idx);
                    this.port = Integer.parseInt(ip.substring(idx + 1));
                }catch(Exception e){
                    this.ip = ip;
                    this.port = Vars.port;
                }
            }else{
                this.ip = ip;
                this.port = Vars.port;
            }
        }

        String displayIP(){
            return ip + (port != Vars.port ? ":" + port : "");
        }

        public Server(){
        }
    }
}