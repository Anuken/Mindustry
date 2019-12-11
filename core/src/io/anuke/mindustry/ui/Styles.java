package io.anuke.mindustry.ui;

import io.anuke.annotations.Annotations.*;
import io.anuke.arc.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.graphics.g2d.TextureAtlas.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.Button.*;
import io.anuke.arc.scene.ui.CheckBox.*;
import io.anuke.arc.scene.ui.Dialog.*;
import io.anuke.arc.scene.ui.ImageButton.*;
import io.anuke.arc.scene.ui.KeybindDialog.*;
import io.anuke.arc.scene.ui.Label.*;
import io.anuke.arc.scene.ui.ScrollPane.*;
import io.anuke.arc.scene.ui.Slider.*;
import io.anuke.arc.scene.ui.TextButton.*;
import io.anuke.arc.scene.ui.TextField.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;

import static io.anuke.mindustry.gen.Tex.*;

@StyleDefaults
public class Styles{
    public static Drawable black, black9, black8, black6, black3, none, flatDown, flatOver;
    public static ButtonStyle defaultb, waveb;
    public static TextButtonStyle defaultt, squaret, nodet, cleart, discordt, infot, clearPartialt, clearTogglet, clearToggleMenut, togglet;
    public static ImageButtonStyle defaulti, nodei, righti, emptyi, emptytogglei, selecti, cleari, clearFulli, clearPartiali, clearPartial2i, clearTogglei, clearTransi, clearToggleTransi, clearTogglePartiali;
    public static ScrollPaneStyle defaultPane, horizontalPane, smallPane;
    public static KeybindDialogStyle defaultKeybindDialog;
    public static SliderStyle defaultSlider, vSlider;
    public static LabelStyle defaultLabel, outlineLabel;
    public static TextFieldStyle defaultField, areaField;
    public static CheckBoxStyle defaultCheck;
    public static DialogStyle defaultDialog, fullDialog;

    public static void load(){
        black = whiteui.tint(0f, 0f, 0f, 1f);
        black9 = whiteui.tint(0f, 0f, 0f, 0.9f);
        black8 = whiteui.tint(0f, 0f, 0f, 0.8f);
        black6 = whiteui.tint(0f, 0f, 0f, 0.6f);
        black3 = whiteui.tint(0f, 0f, 0f, 0.3f);
        none = whiteui.tint(0f, 0f, 0f, 0f);
        flatDown = createFlatDown();
        flatOver = whiteui.tint(Color.valueOf("454545"));

        defaultb = new ButtonStyle(){{
            down = buttonDown;
            up = button;
            over = buttonOver;
            disabled = buttonDisabled;
        }};
        
        waveb = new ButtonStyle(){{
            up = buttonEdge4;
            over = buttonEdgeOver4;
            disabled = buttonEdge4;
        }};

        defaultt = new TextButtonStyle(){{
            over = buttonOver;
            disabled = buttonDisabled;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            down = buttonDown;
            up = button;
        }};
        squaret = new TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            over = buttonSquareOver;
            disabled = buttonDisabled;
            down = buttonSquareDown;
            up = buttonSquare;
        }};
        nodet = new TextButtonStyle(){{
            disabled = button;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            up = buttonOver;
            over = buttonDown;
        }};
        cleart = new TextButtonStyle(){{
            over = flatOver;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            down = flatOver;
            up = black;
        }};
        discordt = new TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            up = discordBanner;
        }};
        infot = new TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            up = infoBanner;
        }};
        clearPartialt = new TextButtonStyle(){{
            down = whiteui;
            up = pane;
            over = flatDown;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
        }};
        clearTogglet = new TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            checked = flatDown;
            down = flatDown;
            up = black;
            over = flatOver;
            disabled = black;
            disabledFontColor = Color.gray;
        }};
        clearToggleMenut = new TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            checked = flatDown;
            down = flatDown;
            up = clear;
            over = flatOver;
            disabled = black;
            disabledFontColor = Color.gray;
        }};
        togglet = new TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            checked = buttonDown;
            down = buttonDown;
            up = button;
            over = buttonOver;
            disabled = buttonDisabled;
            disabledFontColor = Color.gray;
        }};

        defaulti = new ImageButtonStyle(){{
            down = buttonDown;
            up = button;
            over = buttonOver;
            imageDisabledColor = Color.gray;
            imageUpColor = Color.white;
            disabled = buttonDisabled;
        }};
        nodei = new ImageButtonStyle(){{
            up = buttonOver;
            over = buttonDown;
        }};
        righti = new ImageButtonStyle(){{
            over = buttonRightOver;
            down = buttonRightDown;
            up = buttonRight;
        }};
        emptyi = new ImageButtonStyle(){{
            imageDownColor = Pal.accent;
            imageUpColor = Color.white;
        }};
        emptytogglei = new ImageButtonStyle(){{
            imageCheckedColor = Color.white;
            imageDownColor = Color.white;
            imageUpColor = Color.gray;
        }};
        selecti = new ImageButtonStyle(){{
            checked = buttonSelect;
            up = none;
        }};
        cleari = new ImageButtonStyle(){{
            down = flatOver;
            up = black;
            over = flatOver;
        }};
        clearFulli = new ImageButtonStyle(){{
            down = whiteui;
            up = pane;
            over = flatDown;
        }};
        clearPartiali = new ImageButtonStyle(){{
            down = flatDown;
            up = none;
            over = flatOver;
        }};
        clearPartial2i = new ImageButtonStyle(){{
            down = whiteui;
            up = pane;
            over = flatDown;
        }};
        clearTogglei = new ImageButtonStyle(){{
            down = flatDown;
            checked = flatDown;
            up = black;
            over = flatOver;
        }};
        clearTransi = new ImageButtonStyle(){{
            down = flatDown;
            up = black6;
            over = flatOver;
            disabled = black8;
            imageDisabledColor = Color.lightGray;
            imageUpColor = Color.white;
        }};
        clearToggleTransi = new ImageButtonStyle(){{
            down = flatDown;
            checked = flatDown;
            up = black6;
            over = flatOver;
        }};
        clearTogglePartiali = new ImageButtonStyle(){{
            down = flatDown;
            checked = flatDown;
            up = none;
            over = flatOver;
        }};

        defaultPane = new ScrollPaneStyle(){{
            vScroll = scroll;
            vScrollKnob = scrollKnobVerticalBlack;
        }};
        horizontalPane = new ScrollPaneStyle(){{
            vScroll = scroll;
            vScrollKnob = scrollKnobVerticalBlack;
            hScroll = scrollHorizontal;
            hScrollKnob = scrollKnobHorizontalBlack;
        }};
        smallPane = new ScrollPaneStyle(){{
            vScroll = clear;
            vScrollKnob = scrollKnobVerticalThin;
        }};

        defaultKeybindDialog = new KeybindDialogStyle(){{
            keyColor = Pal.accent;
            keyNameColor = Color.white;
            controllerColor = Color.lightGray;
        }};

        defaultSlider = new SliderStyle(){{
            background = slider;
            knob = sliderKnob;
            knobOver = sliderKnobOver;
            knobDown = sliderKnobDown;
        }};
        vSlider = new SliderStyle(){{
            background = sliderVertical;
            knob = sliderKnob;
            knobOver = sliderKnobOver;
            knobDown = sliderKnobDown;
        }};

        defaultLabel = new LabelStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
        }};
        outlineLabel = new LabelStyle(){{
            font = Fonts.outline;
            fontColor = Color.white;
        }};

        defaultField = new TextFieldStyle(){{
            font = Fonts.chat;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            disabledBackground = underlineDisabled;
            selection = Tex.selection;
            background = underline;
            invalidBackground = underlineRed;
            cursor = Tex.cursor;
            messageFont = Fonts.def;
            messageFontColor = Color.gray;
        }};
        areaField = new TextFieldStyle(){{
            font = Fonts.chat;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            selection = Tex.selection;
            background = underline;
            cursor = Tex.cursor;
            messageFont = Fonts.def;
            messageFontColor = Color.gray;
        }};

        defaultCheck = new CheckBoxStyle(){{
            checkboxOn = checkOn;
            checkboxOff = checkOff;
            checkboxOnOver = checkOnOver;
            checkboxOver = checkOver;
            checkboxOnDisabled = checkOnDisabled;
            checkboxOffDisabled = checkDisabled;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
        }};

        defaultDialog = new DialogStyle(){{
            stageBackground = black9;
            titleFont = Fonts.def;
            background = windowEmpty;
            titleFontColor = Pal.accent;
        }};
        fullDialog = new DialogStyle(){{
            stageBackground = black;
            titleFont = Fonts.def;
            background = windowEmpty;
            titleFontColor = Pal.accent;
        }};
    }

    private static Drawable createFlatDown(){
        AtlasRegion region = Core.atlas.find("flat-down-base");
        int[] splits = region.splits;

        ScaledNinePatchDrawable copy = new ScaledNinePatchDrawable(new NinePatch(region, splits[0], splits[1], splits[2], splits[3])){
            public float getLeftWidth(){ return 0; }
            public float getRightWidth(){ return 0; }
            public float getTopHeight(){ return 0; }
            public float getBottomHeight(){ return 0; }
        };
        copy.setMinWidth(0);
        copy.setMinHeight(0);
        copy.setTopHeight(0);
        copy.setRightWidth(0);
        copy.setBottomHeight(0);
        copy.setLeftWidth(0);
        return copy;
    }
}
