package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.net.Net;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.Scene;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.Label.LabelStyle;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Table;

import static io.anuke.ucore.core.Core.scene;
import static io.anuke.ucore.core.Core.skin;

//TODO show chat even when not toggled
public class ChatFragment extends Table implements Fragment{
    private final static int messagesShown = 10;
    private final static int maxLength = 150;
    private Array<ChatMessage> messages = new Array<>();
    private float fadetime;
    private float lastfadetime;
    private boolean chatOpen = false;
    private TextField chatfield;
    private Label fieldlabel = new Label(">");
    private BitmapFont font;
    private GlyphLayout layout = new GlyphLayout();
    private float offsetx = 4, offsety = 4, fontoffsetx = 2, chatspace = 50;
    private float textWidth = 600;
    private Color shadowColor = new Color(0, 0, 0, 0.4f);
    private float textspacing = 10;

    public ChatFragment(){
        super();

        setFillParent(true);
        font = Core.skin.getFont("default-font");

        setVisible(() -> !GameState.is(State.menu) && Net.active());

        //TODO put it input
        update(() -> {
            if(Net.active() && Inputs.keyTap("chat")){
                toggle();
            }
        });

        setup();
    }

    @Override
    public void build() {
        scene.add(this);
    }

    private void setup(){
        fieldlabel.setStyle(new LabelStyle(fieldlabel.getStyle()));
        fieldlabel.getStyle().font = font;
        fieldlabel.setStyle(fieldlabel.getStyle());

        chatfield = new TextField("", new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class)));
        chatfield.setTextFieldFilter((field, c) -> field.getText().length() < maxLength);
        chatfield.getStyle().background = skin.getDrawable("chatfield");
        chatfield.getStyle().fontColor = Color.WHITE;
        chatfield.getStyle().font = skin.getFont("default-font-chat");

        bottom().left().marginBottom(offsety).marginLeft(offsetx*2).add(fieldlabel).padBottom(4f);

        add(chatfield).padBottom(offsety).padLeft(offsetx).growX().padRight(offsetx).height(28);
    }

    @Override
    public void draw(Batch batch, float alpha){

        batch.setColor(shadowColor);

        if(chatOpen)
            batch.draw(skin.getRegion("white"), offsetx, chatfield.getY(), Gdx.graphics.getWidth()-offsetx*2, chatfield.getHeight()-1);

        font.getData().down = -21.5f;
        font.getData().lineHeight = 22f;

        super.draw(batch, alpha);

        float spacing = chatspace;

        chatfield.setVisible(chatOpen);
        fieldlabel.setVisible(chatOpen);

        batch.setColor(shadowColor);

        float theight = offsety + spacing;
        for(int i = 0; i < messagesShown && i < messages.size && i < fadetime; i ++){

            layout.setText(font, messages.get(i).formattedMessage, Color.WHITE, textWidth, Align.bottomLeft, true);
            theight += layout.height+textspacing;
            if(i == 0) theight -= textspacing+1;

            font.getCache().clear();
            font.getCache().addText(messages.get(i).formattedMessage, fontoffsetx + offsetx, offsety + theight, textWidth, Align.bottomLeft, true);

            if(fadetime-i < 1f && fadetime-i >= 0f){
                font.getCache().setAlphas(fadetime-i);
                batch.setColor(0, 0, 0, shadowColor.a*(fadetime-i));
            }

            batch.draw(skin.getRegion("white"), offsetx, theight-layout.height+1-4, textWidth, layout.height+textspacing);
            batch.setColor(shadowColor);

            font.getCache().draw(batch);
        }

        batch.setColor(Color.WHITE);

        if(fadetime > 0 && !chatOpen)
            fadetime -= Timers.delta()/120f;
    }

    private void sendMessage(){
        String message = chatfield.getText();
        chatfield.clearText();

        if(message.replaceAll(" ", "").isEmpty()) return;

        Vars.netClient.handleSendMessage(message);
    }

    public void toggle(){
        Scene scene = getScene();

        if(!chatOpen && (scene.getKeyboardFocus() == null || !scene.getKeyboardFocus().getParent().isVisible())){
            scene.setKeyboardFocus(chatfield);
            chatOpen = !chatOpen;
            lastfadetime = fadetime;
            fadetime = messagesShown + 1;
        }else if(chatOpen){
            scene.setKeyboardFocus(null);
            chatOpen = !chatOpen;
            sendMessage();
            fadetime = messagesShown + 1; //TODO?
        }
    }

    public boolean chatOpen(){
        return chatOpen;
    }

    public void addMessage(String message, String sender){
        messages.insert(0, new ChatMessage(message, sender));

        fadetime += 1f;
        fadetime = Math.min(fadetime, messagesShown) + 2f;
    }

    private static class ChatMessage{
        public final String sender;
        public final String message;
        public final String formattedMessage;

        public ChatMessage(String message, String sender){
            this.message = message;
            this.sender = sender;
            if(sender == null){ //no sender, this is a server message
                formattedMessage = message;
            }else{
                formattedMessage = "[ROYAL]["+sender+"]: [YELLOW]"+message;
            }
        }
    }

}
