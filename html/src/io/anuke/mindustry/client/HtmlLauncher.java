package io.anuke.mindustry.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderCallback;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderState;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Base64Coder;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.user.client.ui.*;
import io.anuke.mindustry.Mindustry;
import io.anuke.mindustry.core.Platform;
import io.anuke.ucore.function.Consumer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

public class HtmlLauncher extends GwtApplication {
    static final int WIDTH = 800;
    static final int HEIGHT = 600;
    static HtmlLauncher instance;
    static Consumer<FileHandle> fileCons;
    
    @Override
    public PreloaderCallback getPreloaderCallback () {
		final Panel preloaderPanel = new VerticalPanel();
		preloaderPanel.setStyleName("gdx-preloader");
		final Image logo = new Image(GWT.getModuleBaseURL() + "logo.png");
		logo.setStyleName("logo");		
		preloaderPanel.add(logo);
		final Panel meterPanel = new SimplePanel();
		meterPanel.setStyleName("gdx-meter");
		meterPanel.addStyleName("red");
		final InlineHTML meter = new InlineHTML();
		final Style meterStyle = meter.getElement().getStyle();
		meterStyle.setWidth(0, Unit.PCT);
		meterPanel.add(meter);
		preloaderPanel.add(meterPanel);
		getRootPanel().add(preloaderPanel);
		return new PreloaderCallback() {
			@Override
			public void error (String file) {
				System.out.println("error: " + file);
			}
			
			@Override
			public void update (PreloaderState state) {
				meterStyle.setWidth(100f * state.getProgress(), Unit.PCT);
			}
		};
	}

    @Override
    public GwtApplicationConfiguration getConfig() {
        GwtApplicationConfiguration config = new GwtApplicationConfiguration(WIDTH, HEIGHT);

        Element element = Document.get().getElementById("embed-html");
        VerticalPanel panel = new VerticalPanel();
        panel.setWidth("100%");
        panel.setHeight("100%");
        panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        element.appendChild(panel.getElement());
        config.rootPanel = panel;
        config.width = 2000;
        config.height = 2000;

        return config;
    }

    @Override
    public ApplicationListener createApplicationListener() {
        instance = this;
        setLogLevel(LOG_NONE);
        setLoadingListener(new LoadingListener() {
            @Override
            public void beforeSetup() {

            }

            @Override
            public void afterSetup() {
                scaleCanvas();
                setupResizeHook();
            }
        });
        
        Platform.instance = new Platform(){
        	DateTimeFormat format = DateTimeFormat.getFormat("EEE, dd MMM yyyy HH:mm:ss");

            @Override
            public void showFileChooser(String text, String content, Consumer<FileHandle> cons, boolean open, String filetype) {
                if(!open) return; //can't save files on gwt

                fileCons = cons;
                createFileChooser();
            }

            @Override
			public String format(Date date){
				return format.format(date);
			}

			@Override
			public String format(int number){
				return NumberFormat.getDecimalFormat().format(number);
			}

			@Override
            public boolean canJoinGame(){
			    String ref = Document.get().getReferrer();
			    return !ref.startsWith("https") && !ref.contains("itch.io");
            }

            @Override
            public void downloadFile(String name, byte[] bytes) {
                downloadBytes(name, new String(Base64Coder.encode(bytes)));
            }
        };
        
        return new Mindustry();
    }

    void scaleCanvas() {
        Element element = Document.get().getElementById("embed-html");
        int innerWidth = getWindowInnerWidth();
        int innerHeight = getWindowInnerHeight();
        int newWidth = innerWidth;
        int newHeight = innerHeight;
        float ratio = innerWidth / (float) innerHeight;
        float viewRatio = WIDTH / (float) HEIGHT;

        if (ratio > viewRatio) {
            newWidth = (int) (innerHeight * viewRatio);
        } else {
            newHeight = (int) (innerWidth / viewRatio);
        }

        NodeList<Element> nl = element.getElementsByTagName("canvas");

        if (nl != null && nl.getLength() > 0) {
            Element canvas = nl.getItem(0);
            canvas.setAttribute("width", "" + newWidth + "px");
            canvas.setAttribute("height", "" + newHeight + "px");
            canvas.getStyle().setWidth(newWidth, Style.Unit.PX);
            canvas.getStyle().setHeight(newHeight, Style.Unit.PX);
            canvas.getStyle().setTop((int) ((innerHeight - newHeight) * 0.5f), Style.Unit.PX);
            canvas.getStyle().setLeft((int) ((innerWidth - newWidth) * 0.5f), Style.Unit.PX);
            canvas.getStyle().setPosition(Style.Position.ABSOLUTE);
        }
    }

    native void createFileChooser() /*-{
        function getBase64(file, callback) {
            var reader = new FileReader();
            reader.readAsDataURL(file);
            reader.onload = function(){ callback(reader.result); }
            reader.onerror = function(error){ console.log(error); }
        }

        var input = document.createElement('input');
        input.type = 'file';
        input.onchange = function() {
           getBase64(input.files[0], function(data){ @io.anuke.mindustry.client.HtmlLauncher::handleFileSelect(Ljava/lang/String;)(data); });
        };
        input.click();
    }-*/;

    native void downloadBytes(String name, String base64) /*-{
        var binaryString = window.atob(base64);
        var binaryLen = binaryString.length;
        var bytes = new Uint8Array(binaryLen);
        for (var i = 0; i < binaryLen; i++) {
           var ascii = binaryString.charCodeAt(i);
           bytes[i] = ascii;
        }

        var blob = new Blob([bytes]);
        var link = document.createElement('a');
        link.href = window.URL.createObjectURL(blob);
        link.download = name;
        link.click();
    }-*/;

    native int getWindowInnerWidth() /*-{
        return $wnd.innerWidth;
    }-*/;

    native int getWindowInnerHeight() /*-{
        return $wnd.innerHeight;
    }-*/;

    native void setupResizeHook() /*-{
        var htmlLauncher_onWindowResize = $entry(@io.anuke.mindustry.client.HtmlLauncher::handleResize());
        $wnd.addEventListener('resize', htmlLauncher_onWindowResize, false);
    }-*/;

    public static void handleResize() {
        instance.scaleCanvas();
    }

    public static void handleFileSelect(String base64){
        ByteArrayInputStream stream = new ByteArrayInputStream(Base64Coder.decode(base64.substring("data:;base64,".length())));
        fileCons.accept(new FileHandle(){
            @Override
            public InputStream read() {
                return stream;
            }

            @Override
            public String nameWithoutExtension() {
                return "unknown";
            }

            @Override
            public String name() {
                return "unknown";
            }
        });
    }
}
