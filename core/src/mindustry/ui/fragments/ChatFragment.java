package mindustry.ui.fragments;

import arc.*;
import arc.Input.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;

import static arc.Core.*;
import static mindustry.Vars.net;
import static mindustry.Vars.*;

public class ChatFragment extends Table{
    private static final int messagesShown = 10;
    private Seq<ChatMessage> messages = new Seq<>();
    private float fadetime;
    private boolean shown = false;
    private TextField chatfield;
    private Label fieldlabel = new Label(">");
    private ChatMode mode = ChatMode.normal;
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
            scene.add(ChatFragment.this);
        }
    };

    public ChatFragment(){
        super();

        setFillParent(true);
        font = Fonts.def;

        visible(() -> {
            if(!net.active() && messages.size > 0){
                clearMessages();

                if(shown){
                    hide();
                }
            }

            return net.active() && ui.hudfrag.shown;
        });

        update(() -> {

            if(net.active() && input.keyTap(Binding.chat) && (scene.getKeyboardFocus() == chatfield || scene.getKeyboardFocus() == null || ui.minimapfrag.shown()) && !ui.scriptfrag.shown()){
                toggle();
            }

            if(shown){
                if(input.keyTap(Binding.chat_history_prev) && historyPos < history.size - 1){
                    if(historyPos == 0) history.set(0, chatfield.getText());
                    historyPos++;
                    updateChat();
                }
                if(input.keyTap(Binding.chat_history_next) && historyPos > 0){
                    historyPos--;
                    updateChat();
                }
                if(input.keyTap(Binding.chat_mode)){
                    nextMode();
                }
                scrollPos = (int)Mathf.clamp(scrollPos + input.axis(Binding.chat_scroll), 0, Math.max(0, messages.size - messagesShown));
            }
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
        chatfield.setMaxLength(Vars.maxTextLength);
        chatfield.getStyle().background = null;
        chatfield.getStyle().font = Fonts.chat;
        chatfield.getStyle().fontColor = Color.white;
        chatfield.setStyle(chatfield.getStyle());

        bottom().left().marginBottom(offsety).marginLeft(offsetx * 2).add(fieldlabel).padBottom(6f);

        add(chatfield).padBottom(offsety).padLeft(offsetx).growX().padRight(offsetx).height(28);

        if(Vars.mobile){
            marginBottom(105f);
            marginRight(240f);
        }
    }

    @Override
    public void draw(){
        float opacity = Core.settings.getInt("chatopacity") / 100f;
        float textWidth = Math.min(Core.graphics.getWidth()/1.5f, Scl.scl(700f));

        Draw.color(shadowColor);

        if(shown){
            Fill.crect(offsetx, chatfield.y + scene.marginBottom, chatfield.getWidth() + 15f, chatfield.getHeight() - 1);
        }

        super.draw();

        float spacing = chatspace;

        chatfield.visible = shown;
        fieldlabel.visible = shown;

        Draw.color(shadowColor);
        Draw.alpha(shadowColor.a * opacity);

        float theight = offsety + spacing + getMarginBottom() + scene.marginBottom;
        for(int i = scrollPos; i < messages.size && i < messagesShown + scrollPos && (i < fadetime || shown); i++){

            layout.setText(font, messages.get(i).formattedMessage, Color.white, textWidth, Align.bottomLeft, true);
            theight += layout.height + textspacing;
            if(i - scrollPos == 0) theight -= textspacing + 1;

            font.getCache().clear();
            font.getCache().addText(messages.get(i).formattedMessage, fontoffsetx + offsetx, offsety + theight, textWidth, Align.bottomLeft, true);

            if(!shown && fadetime - i < 1f && fadetime - i >= 0f){
                font.getCache().setAlphas((fadetime - i) * opacity);
                Draw.color(0, 0, 0, shadowColor.a * (fadetime - i) * opacity);
            }else{
                font.getCache().setAlphas(opacity);
            }

            Fill.crect(offsetx, theight - layout.height - 2, textWidth + Scl.scl(4f), layout.height + textspacing);
            Draw.color(shadowColor);
            Draw.alpha(opacity * shadowColor.a);

            font.getCache().draw();
        }

        Draw.color();

        if(fadetime > 0 && !shown){
            fadetime -= Time.delta / 180f;
        }
    }

    private void sendMessage(){
        String message = chatfield.getText().trim();
        clearChatInput();

        if(message.isEmpty()) return;

        history.insert(1, message);

        Call.sendChatMessage(message);
    }

    public void toggle(){

        if(!shown){
            scene.setKeyboardFocus(chatfield);
            shown = true;
            if(mobile){
                TextInput input = new TextInput();
                input.maxLength = maxTextLength;
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
            //sending chat has a delay; workaround for issue #1943
            Time.runTask(2f, () ->{
                scene.setKeyboardFocus(null);
                shown = false;
                scrollPos = 0;
                sendMessage();
            });
        }
    }

    public void hide(){
        scene.setKeyboardFocus(null);
        shown = false;
        clearChatInput();
    }

    public void updateChat(){
        chatfield.setText(mode.normalizedPrefix() + history.get(historyPos));
        updateCursor();
    }

    public void nextMode(){
        ChatMode prev = mode;

        do{
            mode = mode.next();
        }while(!mode.isValid());

        if(chatfield.getText().startsWith(prev.normalizedPrefix())){
            chatfield.setText(mode.normalizedPrefix() + chatfield.getText().substring(prev.normalizedPrefix().length()));
        }else{
            chatfield.setText(mode.normalizedPrefix());
        }

        updateCursor();
    }

    public void clearChatInput(){
        historyPos = 0;
        history.set(0, "");
        chatfield.setText(mode.normalizedPrefix());
        updateCursor();
    }

    public void updateCursor(){
        chatfield.setCursorPosition(chatfield.getText().length());
    }

    public boolean shown(){
        return shown;
    }

    @Deprecated
    public void addMessage(String message, String sender){
        addMessage("[coral][[", sender, "[coral]]:[white] ", message);
    }

    public void addMessage(String prefix, String sender, String suffix, String message){
        if(sender == null && message == null) return;
        messages.insert(0, new ChatMessage(prefix, sender, suffix, message));

        fadetime += 1f;
        fadetime = Math.min(fadetime, messagesShown) + 1f;

        if(scrollPos > 0) scrollPos++;
    }

    private static class ChatMessage{
        public final String prefix;
        public final String sender;
        public final String suffix;
        public final String message;
        public final String formattedMessage;

        public ChatMessage(String prefix, String sender, String suffix, String message){
            this.prefix = prefix;
            this.sender = sender;
            this.suffix = suffix;
            this.message = message;
            if(sender == null){ //no sender, this is a server message?
                formattedMessage = message == null ? "" : message;
            }else{
                formattedMessage = prefix + sender + suffix + message;
            }
        }
    }

    private enum ChatMode{
        normal(""),
        team("/t"),
        admin("/a", player::admin)
        ;

        public String prefix;
        public Boolp valid;
        public static final ChatMode[] all = values();

        ChatMode(String prefix){
            this.prefix = prefix;
            this.valid = () -> true;
        }

        ChatMode(String prefix, Boolp valid){
            this.prefix = prefix;
            this.valid = valid;
        }

        public ChatMode next(){
            return all[(ordinal() + 1) % all.length];
        }

        public String normalizedPrefix(){
            return prefix.isEmpty() ? "" : prefix + " ";
        }

        public boolean isValid(){
            return valid.get();
        }
    }
}
