package mindustry.server;

import arc.*;
import arc.util.*;

import java.io.*;
import java.util.concurrent.*;

import static mindustry.Vars.*;

/** Manual experiment: boots the server, hosts a map, then runs console commands from stdin until "quit". "errors" prints unclaimed errors. */
public class ServerSpike{

    public static void main(String[] args) throws Exception{
        ServerHarness.boot();
        //real time, like a normal server, since a human or a real client is watching
        ServerHarness.run(() -> Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60f, maxDeltaServer)));

        ServerHarness.createMap("spike_map");
        ServerHarness.command("host spike_map");
        System.out.println("SERVER_SPIKE_READY port=" + ServerHarness.port());

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while((line = in.readLine()) != null && !line.trim().equals("quit")){
            if(line.trim().equals("errors")){
                var errors = ServerHarness.drainErrors();
                System.out.println(errors.isEmpty() ? "no errors" : String.join("\n", errors));
            }else if(!line.isBlank()){
                //the server logs the output itself
                ServerHarness.command(line.trim());
            }
        }

        //stdin closed (background run): keep serving until killed
        if(line == null) new CountDownLatch(1).await();

        ServerHarness.shutdown();
        System.exit(0);
    }
}
