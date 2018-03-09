package io.anuke.mindustry.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderCallback;
import com.badlogic.gdx.backends.gwt.preloader.Preloader.PreloaderState;
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
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Settings;

import java.util.Date;
import java.util.Random;

public class HtmlLauncher extends GwtApplication {
    static final int WIDTH = 800;
    static final int HEIGHT = 600;
    static HtmlLauncher instance;
    boolean canJoin = true;
    
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

        Net.setClientProvider(new WebsocketClient());
        
        Platform.instance = new Platform(){
        	DateTimeFormat format = DateTimeFormat.getFormat("EEE, dd MMM yyyy HH:mm:ss");
			
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
            public byte[] getUUID(){
                Settings.defaults("uuid", "");

                String uuid = Settings.getString("uuid");
                if(uuid.isEmpty()){
                    byte[] result = new byte[8];
                    new Random().nextBytes(result);
                    uuid = new String(Base64Coder.encode(result));
                    Settings.putString("uuid", uuid);
                    Settings.save();
                    return result;
                }
                return Base64Coder.decode(uuid);
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
}
