package mindustry.ui;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.scene.style.*;
import arc.scene.ui.Button.*;
import arc.scene.ui.CheckBox.*;
import arc.scene.ui.Dialog.*;
import arc.scene.ui.ImageButton.*;
import arc.scene.ui.Label.*;
import arc.scene.ui.ScrollPane.*;
import arc.scene.ui.Slider.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.TreeElement.*;
import mindustry.annotations.Annotations.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.dialogs.*;

import static mindustry.gen.Tex.*;

@StyleDefaults
public class Styles{
    //TODO all these names are inconsistent and not descriptive
    public static Drawable black, black9, black8, black6, black3, black5, none, flatDown, flatOver, accentDrawable;
    public static ButtonStyle defaultb, waveb, modsb, underlineb;
    public static TextButtonStyle defaultt, squaret, nodet, cleart, discordt, nonet, infot, clearPartialt, clearTogglet, logicTogglet, clearToggleMenut, togglet, transt, fullTogglet, squareTogglet, logict;
    public static ImageButtonStyle defaulti, nodei, righti, emptyi, emptytogglei, selecti, logici, geni, colori, accenti, cleari, clearFulli, clearPartiali, clearPartial2i, clearTogglei, clearTransi, clearToggleTransi, clearTogglePartiali;
    public static ScrollPaneStyle defaultPane, horizontalPane, smallPane, nonePane;
    public static KeybindDialog.KeybindDialogStyle defaultKeybindDialog;
    public static SliderStyle defaultSlider;
    public static LabelStyle defaultLabel, outlineLabel, techLabel;
    public static TextFieldStyle defaultField, nodeField, areaField, nodeArea;
    public static CheckBoxStyle defaultCheck;
    public static DialogStyle defaultDialog, fullDialog;
    public static TreeStyle defaultTree;

    public static void load(){
        var whiteui = (TextureRegionDrawable)Tex.whiteui;

        black = whiteui.tint(0f, 0f, 0f, 1f);
        black9 = whiteui.tint(0f, 0f, 0f, 0.9f);
        black8 = whiteui.tint(0f, 0f, 0f, 0.8f);
        black6 = whiteui.tint(0f, 0f, 0f, 0.6f);
        black5 = whiteui.tint(0f, 0f, 0f, 0.5f);
        black3 = whiteui.tint(0f, 0f, 0f, 0.3f);
        none = whiteui.tint(0f, 0f, 0f, 0f);
        flatDown = createFlatDown();
        flatOver = whiteui.tint(Color.valueOf("454545"));
        accentDrawable = whiteui.tint(Pal.accent);

        defaultb = new ButtonStyle(){{
            down = buttonDown;
            up = button;
            over = buttonOver;
            disabled = buttonDisabled;
        }};

        modsb = new ButtonStyle(){{
            down = flatOver;
            up = underline;
            over = underline2;
        }};

        underlineb = new ButtonStyle(){{
            down = flatOver;
            up = sideline;
            over = sidelineOver;
            checked = flatOver;
        }};
        
        waveb = new ButtonStyle(){{
            up = wavepane;
            over = wavepane; //TODO wrong
            disabled = wavepane;
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
        nonet = new TextButtonStyle(){{
            font = Fonts.outline;
            fontColor = Color.lightGray;
            overFontColor = Pal.accent;
            disabledFontColor = Color.gray;
            up = none;
        }};
        cleart = new TextButtonStyle(){{
            over = flatOver;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            down = flatOver;
            up = black;
        }};
        logict = new TextButtonStyle(){{
            over = flatOver;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            down = flatOver;
            up = underlineWhite;
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
            down = flatOver;
            up = pane;
            over = flatDownBase;
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
        }};
        transt = new TextButtonStyle(){{
            down = flatDown;
            up = none;
            over = flatOver;
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
        logicTogglet = new TextButtonStyle(){{
            font = Fonts.outline;
            fontColor = Color.white;
            checked = accentDrawable;
            down = accentDrawable;
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
        fullTogglet = new TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            checked = flatOver;
            down = flatOver;
            up = black;
            over = flatOver;
            disabled = black;
            disabledFontColor = Color.gray;
        }};
        squareTogglet = new TextButtonStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            checked = flatOver;
            down = flatOver;
            up = pane;
            over = flatOver;
            disabled = black;
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
            disabled = buttonRightDisabled;
            imageDisabledColor = Color.clear;
            imageUpColor = Color.white;
        }};
        emptyi = new ImageButtonStyle(){{
            imageDownColor = Pal.accent;
            imageOverColor = Color.lightGray;
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
        logici = new ImageButtonStyle(){{
            //imageDownColor = Pal.accent;
            imageUpColor = Color.black;
        }};
        geni = new ImageButtonStyle(){{
            imageDownColor = Pal.accent;
            imageUpColor = Color.black;
        }};
        colori = new ImageButtonStyle(){{
            //imageDownColor = Pal.accent;
            imageUpColor = Color.white;
        }};
        accenti = new ImageButtonStyle(){{
            //imageDownColor = Pal.accent;
            imageUpColor = Color.lightGray;
            imageDownColor = Color.white;
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
            disabled = none;
            imageDisabledColor = Color.gray;
            imageUpColor = Color.white;
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
        nonePane = new ScrollPaneStyle();

        defaultKeybindDialog = new KeybindDialog.KeybindDialogStyle(){{
            keyColor = Pal.accent;
            keyNameColor = Color.white;
            controllerColor = Color.lightGray;
        }};

        defaultSlider = new SliderStyle(){{
            background = sliderBack;
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
        techLabel = new LabelStyle(){{
            font = Fonts.tech;
            fontColor = Color.white;
        }};

        defaultField = new TextFieldStyle(){{
            font = Fonts.def;
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

        nodeField = new TextFieldStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            disabledBackground = underlineDisabled;
            selection = Tex.selection;
            background = underlineWhite;
            invalidBackground = underlineRed;
            cursor = Tex.cursor;
            messageFont = Fonts.def;
            messageFontColor = Color.gray;
        }};

        areaField = new TextFieldStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            selection = Tex.selection;
            background = underline;
            cursor = Tex.cursor;
            messageFont = Fonts.def;
            messageFontColor = Color.gray;
        }};

        nodeArea = new TextFieldStyle(){{
            font = Fonts.def;
            fontColor = Color.white;
            disabledFontColor = Color.gray;
            selection = Tex.selection;
            background = underlineWhite;
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

        defaultTree = new TreeStyle(){{
            plus = Icon.downOpen;
            minus = Icon.upOpen;
            background = black5;
            over = flatOver;
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
