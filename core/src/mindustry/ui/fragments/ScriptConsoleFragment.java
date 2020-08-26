package mindustry.ui.fragments;

import arc.*;
import arc.Input.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.input.*;
import mindustry.ui.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class ScriptConsoleFragment extends Table{
    private static final int messagesShown = 30;
    private Seq<String> messages = new Seq<>();
    private boolean open = false, shown;
    private TextField chatfield;
    private Label fieldlabel = new Label(">");
    private Font font;
    private GlyphLayout layout = new GlyphLayout();
    private float offsetx = Scl.scl(4), offsety = Scl.scl(4), fontoffsetx = Scl.scl(2), chatspace = Scl.scl(50);
    private Color shadowColor = new Color(0, 0, 0, 0.4f);
    private float textspacing = Scl.scl(10);
    private Seq<String> history = new Seq<>();
    private int historyPos = 0;
    private int scrollPos = 0;
    private Fragment container = new Fragment(){
        @Override
        public void build(Group parent){
            scene.add(ScriptConsoleFragment.this);
        }
    };

    public ScriptConsoleFragment(){

        setFillParent(true);
        font = Fonts.def;

        visible(() -> {
            if(input.keyTap(Binding.console)  && (scene.getKeyboardFocus() == chatfield || scene.getKeyboardFocus() == null) && !ui.chatfrag.shown()){
                shown = !shown;
                if(shown && !open && enableConsole){
                    toggle();
                }
                clearChatInput();
            }

            return shown;
        });

        update(() -> {
            if(input.keyTap(Binding.chat) && enableConsole && (scene.getKeyboardFocus() == chatfield || scene.getKeyboardFocus() == null)){
                toggle();
            }

            if(open){
                if(input.keyTap(Binding.chat_history_prev) && historyPos < history.size - 1){
                    if(historyPos == 0) history.set(0, chatfield.getText());
                    historyPos++;
                    updateChat();
                }
                if(input.keyTap(Binding.chat_history_next) && historyPos > 0){
                    historyPos--;
                    updateChat();
                }
            }

            scrollPos = (int)Mathf.clamp(scrollPos + input.axis(Binding.chat_scroll), 0, Math.max(0, messages.size - messagesShown));
        });

        history.insert(0, "");
        setup();
    }

    public Fragment container(){
        return container;
    }

    public void clearMessages(){
        messages.clear();
        history.clear();
        history.insert(0, "");
    }

    private void setup(){
        fieldlabel.setStyle(new LabelStyle(fieldlabel.getStyle()));
        fieldlabel.getStyle().font = font;
        fieldlabel.setStyle(fieldlabel.getStyle());

        chatfield = new TextField("", new TextField.TextFieldStyle(scene.getStyle(TextField.TextFieldStyle.class)));
        chatfield.getStyle().background = null;
        chatfield.getStyle().font = Fonts.chat;
        chatfield.getStyle().fontColor = Color.white;
        chatfield.setStyle(chatfield.getStyle());

        bottom().left().marginBottom(offsety).marginLeft(offsetx * 2).add(fieldlabel).padBottom(6f);

        add(chatfield).padBottom(offsety).padLeft(offsetx).growX().padRight(offsetx).height(28);
    }

    @Override
    public void draw(){
        float opacity = 1f;
        float textWidth = graphics.getWidth() - offsetx*2f;

        Draw.color(shadowColor);

        if(open){
            Fill.crect(offsetx, chatfield.y, chatfield.getWidth() + 15f, chatfield.getHeight() - 1);
        }

        super.draw();

        float spacing = chatspace;

        chatfield.visible = open;
        fieldlabel.visible = open;

        Draw.color(shadowColor);
        Draw.alpha(shadowColor.a * opacity);

        float theight = offsety + spacing + getMarginBottom();
        for(int i = scrollPos; i < messages.size && i < messagesShown + scrollPos; i++){

            layout.setText(font, messages.get(i), Color.white, textWidth, Align.bottomLeft, true);
            theight += layout.height + textspacing;
            if(i - scrollPos == 0) theight -= textspacing + 1;

            font.getCache().clear();
            font.getCache().addText(messages.get(i), fontoffsetx + offsetx, offsety + theight, textWidth, Align.bottomLeft, true);

            if(!open){
                font.getCache().setAlphas(opacity);
                Draw.color(0, 0, 0, shadowColor.a * opacity);
            }else{
                font.getCache().setAlphas(opacity);
            }

            Fill.crect(offsetx, theight - layout.height - 2, textWidth + Scl.scl(4f), layout.height + textspacing);
            Draw.color(shadowColor);
            Draw.alpha(opacity * shadowColor.a);

            font.getCache().draw();
        }

        Draw.color();
    }

    private void sendMessage(){
        String message = chatfield.getText();
        clearChatInput();

        if(message.replace(" ", "").isEmpty()) return;

        //special case for 'clear' command
        if(message.equals("clear")){
            clearMessages();
            return;
        }

        history.insert(1, message);

        addMessage("[lightgray]> " + message.replace("[", "[["));
        addMessage(mods.getScripts().runConsole(message).replace("[", "[["));
    }

    public void toggle(){

        if(!open){
            scene.setKeyboardFocus(chatfield);
            open = !open;
            if(mobile){
                TextInput input = new TextInput();
                input.accepted = text -> {
                    chatfield.setText(text);
                    sendMessage();
                    hide();
                    Core.input.setOnscreenKeyboardVisible(false);
                };
                input.canceled = this::hide;
                Core.input.getTextInput(input);
            }else{
                chatfield.fireClick();
            }
        }else{
            scene.setKeyboardFocus(null);
            open = !open;
            scrollPos = 0;
            sendMessage();
        }
    }

    public void hide(){
        scene.setKeyboardFocus(null);
        open = false;
        clearChatInput();
    }

    public void updateChat(){
        chatfield.setText(history.get(historyPos));
        chatfield.setCursorPosition(chatfield.getText().length());
    }

    public void clearChatInput(){
        historyPos = 0;
        history.set(0, "");
        chatfield.setText("");
    }

    public boolean open(){
        return open;
    }

    public boolean shown(){
        return shown;
    }

    public void addMessage(String message){
        messages.insert(0, message);
    }
}
