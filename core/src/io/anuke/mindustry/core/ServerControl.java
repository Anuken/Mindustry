package io.anuke.mindustry.core;

import com.badlogic.gdx.Gdx;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.Packets.ChatPacket;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.UCore;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Sounds;
import io.anuke.ucore.modules.Module;
import io.anuke.ucore.util.ColorCodes;
import io.anuke.ucore.util.CommandHandler;
import io.anuke.ucore.util.CommandHandler.Command;
import io.anuke.ucore.util.CommandHandler.Response;
import io.anuke.ucore.util.CommandHandler.ResponseType;

import java.io.IOException;
import java.util.Scanner;

import static io.anuke.mindustry.Vars.*;
import static io.anuke.ucore.util.ColorCodes.*;

public class ServerControl extends Module {
    private final CommandHandler handler = new CommandHandler("");

    public ServerControl(){
        Effects.setScreenShakeProvider((a, b) -> {});
        Effects.setEffectProvider((a, b, c, d, e) -> {});
        Sounds.setHeadless(true);

        //override default handling
        Net.handle(ChatPacket.class, (packet) -> {
            info("&y" + (packet.name == null ? "" : packet.name) +  ": &lb{0}", packet.text);
        });

        registerCommands();
        Thread thread = new Thread(this::readCommands, "Server Controls");
        thread.setDaemon(true);
        thread.start();

        info("&lcServer loaded. Type &ly'help'&lc for help.");
    }

    private void registerCommands(){
        handler.register("help", "", "Displays this command list.", arg -> {
            info("Commands:");
            for(Command command : handler.getCommandList()){
                print("   &y" + command.text + (command.params.isEmpty() ? "" : " ") + command.params + " - &lm" + command.description);
            }
        });

        handler.register("exit", "", "Exit the server application.", arg -> {
            info("Shutting down server.");
            Net.dispose();
            Gdx.app.exit();
        });

        handler.register("stop", "", "Stop hosting the server.", arg -> {
            Net.closeServer();
            state.set(State.menu);
        });

        handler.register("host", "<mapname>", "Open the server with a specific map.", arg -> {
            if(state.is(State.playing)){
                err("Already hosting. Type 'stop' to stop hosting first.");
            }

            String search = arg[0];
            Map result = null;
            for(Map map : world.maps().list()){
                if(map.name.equalsIgnoreCase(search))
                    result = map;
            }

            if(result == null){
                err("No map with name &y'{0}'&lg found.", search);
                return;
            }

            info("Loading map...");
            logic.reset();
            world.loadMap(result);
            state.set(State.playing);
            info("Map loaded.");

            try {
                Net.host(port);
                info("Server opened.");
            }catch (IOException e){
                UCore.error(e);
            }
        });
    }

    private void readCommands(){
        Scanner scan = new Scanner(System.in);
        System.out.print(LIGHT_BLUE + "> " + RESET);
        while(true){
            String line = scan.nextLine();

            Gdx.app.postRunnable(() -> {
                Response response = handler.handleMessage(line);

                if (response.type == ResponseType.unknownCommand) {
                    err("Invalid command. Type 'help' for help.");
                } else if (response.type == ResponseType.invalidArguments) {
                    err("Invalid command arguments. Usage: " + response.command.text + " " + response.command.params);
                }

                System.out.print(LIGHT_BLUE + "> " + RESET);
            });
        }
    }

    private void print(String text, Object... args){
        System.out.println(format(text, args) + RESET);
    }

    private void info(String text, Object... args){
        print(LIGHT_GREEN + BOLD + format(text, args));
    }

    private void err(String text, Object... args){
        print(LIGHT_RED + BOLD + format(text, args));
    }

    private String format(String text, Object... args){
        for(int i = 0; i < args.length; i ++){
            text = text.replace("{" + i + "}", args[i].toString());
        }

        for(String color : ColorCodes.getColorCodes()){
            text = text.replace("&" + color, ColorCodes.getColorText(color));
        }
        return text;
    }
}
