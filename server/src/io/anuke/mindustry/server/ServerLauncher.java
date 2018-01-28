package io.anuke.mindustry.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.MindustryServer;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.util.Log;

public class ServerLauncher{

    public static void main(String[] args){

        Net.setClientProvider(new KryoClient());
        Net.setServerProvider(new KryoServer());

        new HeadlessApplication(new MindustryServer()){
            @Override
            public boolean executeRunnables() {
                try {
                    return super.executeRunnables();
                }catch(Throwable e) {
                    Log.err(e);
                    System.exit(-1);
                    return false;
                }
            }
        };

    }
}