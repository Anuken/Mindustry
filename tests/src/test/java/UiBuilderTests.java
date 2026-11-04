import arc.*;
import arc.mock.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.Tooltip.*;
import arc.scene.ui.layout.*;
import arc.util.io.*;
import mindustry.ui.builder.*;
import mindustry.ui.builder.UiBuilder.*;
import org.junit.jupiter.api.*;

import java.io.*;

import static mindustry.ui.builder.UiBuilder.*;
import static org.junit.jupiter.api.Assertions.*;

public class UiBuilderTests{

    @Test
    void tooltipRoundTrip(){
        var builder = table().add(
            stack().add(button("Help").tooltip("[accent]Help[]\n\"Details\" — 설명"))
        ).add(label("Count").tooltip("123"));
        var parsed = UiBuilder.parse(builder.toString());
        var bytes = new ByteArrayOutputStream();
        parsed.write(new Writes(new DataOutputStream(bytes)));
        var read = NodeBuilder.read(new Reads(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()))));

        assertEquals(builder.toString(), parsed.toString());
        assertEquals(builder.toString(), read.toString());
    }

    @Test
    void tooltipElements(){
        var app = Core.app;
        Core.app = new MockApplication();
        var tooltips = Tooltips.getInstance();
        var provider = tooltips.textProvider;
        int[] updates = {0};
        tooltips.textProvider = text -> new Tooltip(t -> t.update(() -> updates[0]++));
        try{
            var root = new Table();
            var result = UiTreeBuilder.build(root, table().add(
                stack().id("stack").tooltip("Stack").add(table().id("child").tooltip("Child"))
            ).add(stack().id("plain")));

            for(var element : new Element[]{result.idElements.get("stack"), result.idElements.get("child")}){
                assertEquals(Touchable.enabled, element.touchable);
                var tooltip = (Tooltip)element.getListeners().find(l -> l instanceof Tooltip);
                assertNotNull(tooltip);
                assertTrue(tooltip.allowMobile);
                assertEquals(1, element.getListeners().count(l -> l instanceof Tooltip));
                tooltip.container.act(0f);
            }
            assertEquals(2, updates[0]);

            var plain = result.idElements.get("plain");
            assertEquals(Touchable.childrenOnly, plain.touchable);
            assertNull(plain.getListeners().find(l -> l instanceof Tooltip));
        }finally{
            tooltips.textProvider = provider;
            Core.app = app;
        }
    }
}
