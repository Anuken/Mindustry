package io.anuke.mindustry.reporter;

import static java.lang.System.out;

public class ReportHandler{

    public void handle(String text){
        out.println("recieved text: " + text);
    }
}
