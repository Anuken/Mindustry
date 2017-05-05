package io.anuke.mindustry.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;

import io.anuke.mindustry.Mindustry;

public class HtmlLauncher extends GwtApplication {
    static final int WIDTH = 800;
    static final int HEIGHT = 600;
    static HtmlLauncher instance;

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
