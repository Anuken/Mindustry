package io.anuke.mindustry.teavm;

import io.anuke.mindustry.Mindustry;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.xml.Document;
import org.teavm.libgdx.TeaVMApplication;
import org.teavm.libgdx.TeaVMApplicationConfig;

public class TeaVMLauncher {
    public static void main(String[] args) {
        Window window = Window.current();
        Document document = window.getDocument();
        TeaVMApplicationConfig config = new TeaVMApplicationConfig();
        config.setCanvas((HTMLCanvasElement)document.getElementById("canvas"));
        new TeaVMApplication(new Mindustry(), config).start();
    }
}
