package io.anuke.mindustry.desktop;

import io.anuke.mindustry.net.Net;
import io.anuke.ucore.util.Strings;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CrashHandler {

    public static void handle(Throwable e){
        //TODO send full error report to server via HTTP
        e.printStackTrace();

        //attempt to close connections, if applicable
        try{
            Net.dispose();
        }catch (Throwable p){
            p.printStackTrace();
        }

        //don't create crash logs for me (anuke), as it's expected
        if(System.getProperty("user.name").equals("anuke")) return;

        //parse exception
        String result = Strings.parseException(e, true);
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
            JOptionPane.showMessageDialog(null, "An error has occured: \n" + result + "\n\n" +
                (!failed ? "A crash report has been written to " + new File(filename).getAbsolutePath() + ".\nPlease send this file to the developer!"
                        : "Failed to generate crash report.\nPlease send an image of this crash log to the developer!"));
        }catch (Throwable i){
            i.printStackTrace();
            //what now?
        }
    }
}
