package io.anuke.mindustry.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import io.anuke.kryonet.KryoClient;
import io.anuke.kryonet.KryoServer;
import io.anuke.mindustry.MindustryServer;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.util.Log;

import java.lang.reflect.Method;

public class ServerLauncher{

    public static void main(String[] args) throws Exception{

        Net.setClientProvider(new KryoClient());
        Net.setServerProvider(new KryoServer());

        HeadlessApplication app = new HeadlessApplication(new MindustryServer()){
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

        Method method = app.getClass().getDeclaredMethod("mainLoop");
        method.setAccessible(true);

        //kill default libGDX thread
        for(Thread thread : Thread.getAllStackTraces().keySet()){
            if(thread.getName().equals("\"HeadlessApplication\"")){
                thread.interrupt();
            }
        }

        //replace it with my own
        Thread mainLoopThread = new Thread("HeadlessApplication") {
            @Override
            public void run () {
                try {
                    method.invoke(app);
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(-1);
                }
            }
        };
        mainLoopThread.start();

    }
}