package io.anuke.mindustry.desktop;

import io.anuke.mindustry.io.Version;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;
import io.anuke.ucore.util.Strings;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashHandler {

    public static void handle(Throwable e){
        //TODO send full error report to server via HTTP
        e.printStackTrace();

        boolean netActive = false, netServer = false;

        //attempt to close connections, if applicable
        try{
            netActive = Net.active();
            netServer = Net.server();
            Net.dispose();
        }catch (Throwable p){
            p.printStackTrace();
        }

        //don't create crash logs for me (anuke), as it's expected
        if(System.getProperty("user.name").equals("anuke")) return;

        String header = "--CRASH REPORT--\n";

        try{
            header += "--GAME INFO--\n";
            header += "Build: " + Version.build + "\n";
            header += "Net Active: " + netActive + "\n";
            header += "Net Server: " + netServer + "\n";
            header += "OS: " + System.getProperty("os.name")+ "\n----\n";
            header += "Multithreading: " + Settings.getBool("multithread")+ "\n";
        }catch (Throwable e4){
            header += "[Error getting additional game info.]\n";
            e4.printStackTrace();
        }

        //parse exception
        String result = header + Strings.parseFullException(e);
        boolean failed = false;

        String filename = "";

        //try to write it
        try{
            filename = "crash-report-" + new SimpleDateFormat("dd-MM-yy h.mm.ss").format(new Date()) + ".txt";
            Files.write(Paths.get(filename), result.getBytes());
        }catch (Throwable i){
            i.printStackTrace();
            failed = true;
        }

        try{
            javax.swing.JOptionPane.showMessageDialog(null, "An error has occured: \n" + result + "\n\n" +
                (!failed ? "A crash report has been written to " + new File(filename).getAbsolutePath() + ".\nPlease send this file to the developer!"
                        : "Failed to generate crash report.\nPlease send an image of this crash log to the developer!"));
        }catch (Throwable i){
            i.printStackTrace();
            //what now?
        }
    }
}
