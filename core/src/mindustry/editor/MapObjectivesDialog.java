package mindustry.editor;

import arc.func.*;
import arc.graphics.*;
import arc.math.geom.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.TextField.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.*;
import mindustry.game.MapObjectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;

import java.lang.annotation.*;
import java.lang.reflect.*;

import static mindustry.Vars.*;
import static mindustry.editor.MapObjectivesCanvas.*;

@SuppressWarnings({"unchecked", "rawtypes"})
public class MapObjectivesDialog extends BaseDialog{
    public MapObjectivesCanvas canvas;
    protected Cons<Seq<MapObjective>> out = arr -> {};

    /** Defines default value providers. */
    private static final ObjectMap<Class<?>, FieldProvider<?>> providers = new ObjectMap<>();
    /** Maps annotation type with its field parsers. Non-annotated fields are mapped with {@link Override}. */
    private static final ObjectMap<Class<? extends Annotation>, ObjectMap<Class<?>, FieldInterpreter<?>>> interpreters = new ObjectMap<>();

    static{
        // Default un-annotated field interpreters.
        setProvider(String.class, (type, cons) -> cons.get(""));
        setInterpreter(String.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);

            if(field != null && field.isAnnotationPresent(Multiline.class)){
                cont.area(get.get(), set).height(85f).growX();
            }else{
                cont.field(get.get(), set).growX();
            }
        });

        setProvider(boolean.class, (type, cons) -> cons.get(false));
        setInterpreter(boolean.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);
            cont.check("", get.get(), set::get).growX().fillY().get().getLabelCell().growX();
        });

        setProvider(byte.class, (type, cons) -> cons.get((byte)0));
        setInterpreter(byte.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);
            cont.field(Byte.toString(get.get()), str -> set.get((byte)Strings.parseInt(str)))
                .growX().fillY()
                .valid(Strings::canParseInt)
                .get().setFilter(TextFieldFilter.digitsOnly);
        });

        setProvider(int.class, (type, cons) -> cons.get(0));
        setInterpreter(int.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);
            cont.field(Integer.toString(get.get()), str -> set.get(Strings.parseInt(str)))
                .growX().fillY()
                .valid(Strings::canParseInt)
                .get().setFilter(TextFieldFilter.digitsOnly);
        });

        setProvider(float.class, (type, cons) -> cons.get(0f));
        setInterpreter(float.class, (cont, name, type, field, remover, indexer, get, set) -> {
            float m = 1f;
            if(field != null){
                if(field.isAnnotationPresent(Second.class)){
                    m = 60f;
                }else if(field.isAnnotationPresent(TilePos.class)){
                    m = 8f;
                }
            }

            float mult = m;

            name(cont, name, remover, indexer);
            cont.field(Float.toString(get.get() / mult), str -> set.get(Strings.parseFloat(str) * mult))
                .growX().fillY()
                .valid(Strings::canParseFloat)
                .get().setFilter(TextFieldFilter.floatsOnly);
        });

        setProvider(UnlockableContent.class, (type, cons) -> cons.get(Blocks.coreShard));
        setInterpreter(UnlockableContent.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);
            cont.table(t -> t.left().button(
                b -> b.image().size(iconSmall).update(i -> i.setDrawable(get.get().uiIcon)),
                () -> showContentSelect(null, set, b -> (field != null && !field.isAnnotationPresent(Researchable.class)) || b.techNode != null)
            ).fill().pad(4)).growX().fillY();
        });

        setProvider(Block.class, (type, cons) -> cons.get(Blocks.copperWall));
        setInterpreter(Block.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);
            cont.table(t -> t.left().button(
                b -> b.image().size(iconSmall).update(i -> i.setDrawable(get.get().uiIcon)),
                () -> showContentSelect(ContentType.block, set, b -> (field != null && !field.isAnnotationPresent(Synthetic.class)) || b.synthetic())
            ).fill().pad(4f)).growX().fillY();
        });

        setProvider(Item.class, (type, cons) -> cons.get(Items.copper));
        setInterpreter(Item.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);
            cont.table(t -> t.left().button(
                b -> b.image().size(iconSmall).update(i -> i.setDrawable(get.get().uiIcon)),
                () -> showContentSelect(ContentType.item, set, item -> true)
            ).fill().pad(4f)).growX().fillY();
        });

        setProvider(UnitType.class, (type, cons) -> cons.get(UnitTypes.dagger));
        setInterpreter(UnitType.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);
            cont.table(t -> t.left().button(
                b -> b.image().size(iconSmall).update(i -> i.setDrawable(get.get().uiIcon)),
                () -> showContentSelect(ContentType.unit, set, unit -> true)
            ).fill().pad(4f)).growX().fillY();
        });

        setProvider(Team.class, (type, cons) -> cons.get(Team.sharded));
        setInterpreter(Team.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);
            cont.table(t -> t.left().button(
                b -> b.image(Tex.whiteui).size(iconSmall).update(i -> i.setColor(get.get().color)),
                () -> showTeamSelect(set)
            ).fill().pad(4f)).growX().fillY();
        });

        setProvider(Color.class, (type, cons) -> cons.get(Pal.accent.cpy()));
        setInterpreter(Color.class, (cont, name, type, field, remover, indexer, get, set) -> {
            var out = get.get();

            name(cont, name, remover, indexer);
            cont.table(t -> t.left().button(
                b -> b.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                    update(() -> setColor(out));
                }}).grow(),
                Styles.squarei,
                () -> ui.picker.show(out, res -> set.get(out.set(res)))
            ).margin(4f).pad(4f).size(50f)).growX().fillY();
        });

        setProvider(Vec2.class, (type, cons) -> cons.get(new Vec2()));
        setInterpreter(Vec2.class, (cont, name, type, field, remover, indexer, get, set) -> {
            var obj = get.get();

            name(cont, name, remover, indexer);
            cont.table(t -> {
                boolean isInt = type.raw == int.class;

                FieldInterpreter in = getInterpreter(float.class);
                if(isInt) in = getInterpreter(int.class);

                in.build(
                    t, "x", new TypeInfo(isInt ? int.class : float.class),
                    field, null, null,
                    isInt ? () -> (int)obj.x : () -> obj.x,
                    res -> {
                        obj.x = isInt ? (Integer)res : (Float)res;
                        set.get(obj);
                    }
                );

                in.build(
                    t.row(), "y", new TypeInfo(isInt ? int.class : float.class),
                    field, null, null,
                    isInt ? () -> (int)obj.y : () -> obj.y,
                    res -> {
                        obj.y = isInt ? (Integer)res : (Float)res;
                        set.get(obj);
                    }
                );
            }).growX().fillY();
        });

        setProvider(Point2.class, (type, cons) -> cons.get(new Point2()));
        setInterpreter(Point2.class, (cont, name, type, field, remover, indexer, get, set) -> {
            var obj = get.get();
            var vec = new Vec2(obj.x, obj.y);
            getInterpreter(Vec2.class).build(
                cont, name, new TypeInfo(int.class),
                field, remover, indexer,
                () -> vec,
                res -> {
                    vec.set(res);
                    set.get(obj.set((int)vec.x, (int)vec.y));
                }
            );
        });

        // Types that have a provider, but delegate to the default interpreter.
        setProvider(MapObjective.class, (type, cons) -> new BaseDialog("@add"){{
            cont.pane(p -> {
                p.background(Tex.button);
                p.marginRight(14f);
                p.defaults().size(195f, 56f);

                int i = 0;
                for(var gen : MapObjectives.allObjectiveTypes){
                    var obj = gen.get();
                    p.button(obj.typeName(), Styles.flatt, () -> {
                        cons.get(obj);
                        hide();
                    }).with(Table::left).get().getLabelCell().growX().left().padLeft(5f).labelAlign(Align.left);

                    if(++i % 3 == 0) p.row();
                }
            }).scrollX(false);

            addCloseButton();
            show();
        }});

        setProvider(ObjectiveMarker.class, (type, cons) -> new BaseDialog("@add"){{
            cont.pane(p -> {
                p.background(Tex.button);
                p.marginRight(14f);
                p.defaults().size(195f, 56f);

                int i = 0;
                for(var gen : MapObjectives.allMarkerTypes){
                    var marker = gen.get();
                    p.button(marker.typeName(), Styles.flatt, () -> {
                        cons.get(marker);
                        hide();
                    }).with(Table::left).get().getLabelCell().growX().left().padLeft(5f).labelAlign(Align.left);

                    if(++i % 3 == 0) p.row();
                }
            }).scrollX(false);

            addCloseButton();
            show();
        }});

        // Types that use the default interpreter. It would be nice if all types could use it, but I don't know how to reliably prevent classes like [? extends Content] from using it.
        for(var obj : MapObjectives.allObjectiveTypes) setInterpreter(obj.get().getClass(), defaultInterpreter());
        for(var mark : MapObjectives.allMarkerTypes) setInterpreter(mark.get().getClass(), defaultInterpreter());

        // Annotated field interpreters.
        setInterpreter(LabelFlag.class, byte.class, (cont, name, type, field, remover, indexer, get, set) -> {
            name(cont, name, remover, indexer);
            cont.table(t -> {
                t.left().defaults().left();
                byte
                    value = get.get(),
                    bg = WorldLabel.flagBackground, out = WorldLabel.flagOutline;

                t.check("@marker.background", (value & bg) == bg, res -> set.get((byte)(res ? value | bg : value & ~bg)))
                    .growX().fillY()
                    .padTop(4f).padBottom(4f).get().getLabelCell().growX();

                t.row();
                t.check("@marker.outline", (value & out) == out, res -> set.get((byte)(res ? value | out : value & ~out)))
                    .growX().fillY().get().getLabelCell().growX();
            }).growX().fillY();
        });

        // Special data structure interpreters.
        // Instantiate default `Seq`s with a reflectively allocated array.
        setProvider(Seq.class, (type, cons) -> cons.get(new Seq<>(type.element.raw)));
        setInterpreter(Seq.class, (cont, name, type, field, remover, indexer, get, set) -> cont.table(main -> {
            Runnable[] rebuild = {null};
            var arr = get.get();

            main.margin(0f, 10f, 0f, 10f);
            var header = main.table(Tex.button, t -> {
                t.left();
                t.margin(10f);

                if(name.length() > 0) t.add(name + ":").color(Pal.accent);
                t.add().growX();

                if(remover != null) t.button(Icon.trash, Styles.emptyi, remover).fill().padRight(4f);
                if(indexer != null){
                    t.button(Icon.upOpen, Styles.emptyi, () -> indexer.get(true)).fill().padRight(4f);
                    t.button(Icon.downOpen, Styles.emptyi, () -> indexer.get(false)).fill().padRight(4f);
                }

                t.button(Icon.add, Styles.emptyi, () -> getProvider(type.element.raw).get(type.element, res -> {
                    arr.add(res);
                    rebuild[0].run();
                })).fill();
            }).growX().height(46f).pad(0f, -10f, 0f, -10f).get();

            main.row().table(Tex.button, t -> rebuild[0] = () -> {
                t.clear();
                t.top();

                if(arr.isEmpty()){
                    t.background(Tex.clear).margin(0f).setSize(0f);
                }else{
                    t.background(Tex.button).margin(10f).marginTop(20f);
                }

                for(int i = 0, len = arr.size; i < len; i++){
                    int index = i;
                    if(index > 0) t.row();

                    getInterpreter((Class<Object>)arr.get(index).getClass()).build(
                        t, "", new TypeInfo(arr.get(index).getClass()),
                        field, () -> {
                            arr.remove(index);
                            rebuild[0].run();
                        }, field == null || !field.isAnnotationPresent(Unordered.class) ? in -> {
                            if(in && index > 0){
                                arr.swap(index, index - 1);
                                rebuild[0].run();
                            }else if(!in && index < len - 1){
                                arr.swap(index, index + 1);
                                rebuild[0].run();
                            }
                        } : null,
                        () -> arr.get(index),
                        res -> {
                            arr.set(index, res);
                            set.get(arr);
                        }
                    );
                }

                set.get(arr);
            }).padTop(-10f).growX().fillY();
            rebuild[0].run();

            header.toFront();
        }).growX().fillY().pad(4f).colspan(2));

        // Reserved for array types that are not explicitly handled. Essentially handles it the same way as `Seq`.
        setProvider(Object[].class, (type, cons) -> cons.get(Reflect.newArray(type.element.raw, 0)));
        setInterpreter(Object[].class, (cont, name, type, field, remover, indexer, get, set) -> {
            var arr = Seq.with(get.get());
            getInterpreter(Seq.class).build(
                cont, name, new TypeInfo(Seq.class, type.element),
                field, remover, indexer,
                () -> arr,
                res -> set.get(arr.toArray(type.element.raw))
            );
        });
    }

    public static <T> FieldInterpreter<T> defaultInterpreter(){
        return (cont, name, type, field, remover, indexer, get, set) -> cont.table(main -> {
            main.margin(0f, 10f, 0f, 10f);
            var header = main.table(Tex.button, t -> {
                t.left();
                t.margin(10f);

                if(name.length() > 0) t.add(name + ":").color(Pal.accent);
                t.add().growX();

                Cell<ImageButton> remove = null;
                if(remover != null) remove = t.button(Icon.trash, Styles.emptyi, remover).fill();
                if(indexer != null){
                    if(remove != null) remove.padRight(4f);
                    t.button(Icon.upOpen, Styles.emptyi, () -> indexer.get(true)).fill().padRight(4f);
                    t.button(Icon.downOpen, Styles.emptyi, () -> indexer.get(false)).fill();
                }
            }).growX().height(46f).pad(0f, -10f, -0f, -10f).get();

            main.row().table(Tex.button, t -> {
                t.left();
                t.top().margin(10f).marginTop(20f);

                t.defaults().minHeight(40f).left();
                var obj = get.get();

                int i = 0;
                for(var e : JsonIO.json.getFields(type.raw).values()){
                    if(i++ > 0) t.row();

                    var f = e.field;
                    var ft = f.getType();
                    int mods = f.getModifiers();

                    if(!Modifier.isPublic(mods) || (Modifier.isFinal(mods) && (
                        String.class.isAssignableFrom(ft) ||
                        unbox(ft).isPrimitive()
                    ))) continue;

                    var anno = Structs.find(f.getDeclaredAnnotations(), a -> hasInterpreter(a.annotationType(), ft));
                    getInterpreter(anno == null ? Override.class : anno.annotationType(), ft).build(
                        t, f.getName(), new TypeInfo(f),
                        f, null, null,
                        () -> Reflect.get(obj, f),
                        Modifier.isFinal(mods) ? res -> {} : res -> Reflect.set(obj, f, res)
                    );
                }
            }).padTop(-10f).growX().fillY();

            header.toFront();
        }).growX().fillY().pad(4f).colspan(2);
    }

    public static void name(Table cont, CharSequence name, @Nullable Runnable remover, @Nullable Boolc indexer){
        if(indexer != null || remover != null){
            cont.table(t -> {
                if(remover != null) t.button(Icon.trash, Styles.emptyi, remover).fill().padRight(4f);
                if(indexer != null){
                    t.button(Icon.upOpen, Styles.emptyi, () -> indexer.get(true)).fill().padRight(4f);
                    t.button(Icon.downOpen, Styles.emptyi, () -> indexer.get(false)).fill().padRight(4f);
                }
            }).fill();
        }else{
            cont.add(name + ": ");
        }
    }

    public MapObjectivesDialog(){
        super("@editor.objectives");
        clear();
        margin(0f);

        stack(
            new Image(Styles.black5),
            canvas = new MapObjectivesCanvas(),
            new Table(){{
                buttons.defaults().size(160f, 64f).pad(2f);
                buttons.button("@back", Icon.left, MapObjectivesDialog.this::hide);
                buttons.button("@add", Icon.add, () -> getProvider(MapObjective.class).get(new TypeInfo(MapObjective.class), canvas::query));

                if(mobile){
                    buttons.button("@cancel", Icon.cancel, canvas::stopQuery).disabled(b -> !canvas.isQuerying());
                    buttons.button("@ok", Icon.ok, canvas::placeQuery).disabled(b -> !canvas.isQuerying());
                }

                setFillParent(true);
                margin(3f);

                add(titleTable).growX().fillY();
                row().add().grow();
                row().add(buttons).fill();
                addCloseListener();
            }}
        ).grow().pad(0f).margin(0f);

        hidden(() -> {
            out.get(canvas.objectives);
            out = arr -> {};
        });
    }

    public void show(Seq<MapObjective> objectives, Cons<Seq<MapObjective>> out){
        this.out = out;

        canvas.clearObjectives();
        if(
            objectives.any() && (
            // If the objectives were previously programmatically made...
            objectives.contains(obj -> obj.editorX == -1 || obj.editorY == -1) ||
            // ... or some idiot somehow made it not work...
            objectives.contains(obj -> !canvas.tilemap.createTile(obj))
        )){
            // ... then rebuild the structure.
            canvas.clearObjectives();

            // This is definitely NOT a good way to do it, but only insane people or people from the distant past would actually encounter this anyway.
            int w = objWidth + 2,
                len = objectives.size * w,
                columns = objectives.size,
                rows = 1;

            if(len > bounds){
                rows = len / bounds;
                columns = bounds / w;
            }

            int i = 0;
            loop:
            for(int y = 0; y < rows; y++){
                for(int x = 0; x < columns; x++){
                    if(canvas.tilemap.createTile(x * w, y, objectives.get(i))){
                        i++;
                    }
                    if(i >= objectives.size) break loop;
                }
            }
        }

        canvas.objectives.set(objectives);
        show();
    }

    public static <T extends UnlockableContent> void showContentSelect(@Nullable ContentType type, Cons<T> cons, Boolf<T> check){
        BaseDialog dialog = new BaseDialog("");
        dialog.cont.pane(Styles.noBarPane, t -> {
            int i = 0;
            for(var content : (type == null ? content.blocks().copy().<UnlockableContent>as()
                .add(content.items())
                .add(content.liquids())
                .add(content.units()) :
                content.getBy(type).<UnlockableContent>as()
            )){
                if(content.isHidden() || !check.get((T)content)) continue;
                t.image(content == Blocks.air ? Icon.none.getRegion() : content.uiIcon).size(iconMed).pad(3)
                    .with(b -> b.addListener(new HandCursorListener()))
                    .tooltip(content.localizedName).get().clicked(() -> {
                        cons.get((T)content);
                        dialog.hide();
                    });

                if(++i % 10 == 0) t.row();
            }
        }).fill();

        dialog.closeOnBack();
        dialog.show();
    }

    public static void showTeamSelect(Cons<Team> cons){
        BaseDialog dialog = new BaseDialog("");
        for(var team : Team.baseTeams){
            dialog.cont.image(Tex.whiteui).size(iconMed).color(team.color).pad(4)
                .with(i -> i.addListener(new HandCursorListener()))
                .tooltip(team.localized()).get().clicked(() -> {
                    cons.get(team);
                    dialog.hide();
                });
        }

        dialog.closeOnBack();
        dialog.show();
    }

    public static Class<?> unbox(Class<?> boxed){
        return switch(boxed.getSimpleName()){
            case "Boolean" -> boolean.class;
            case "Byte" -> byte.class;
            case "Character" -> char.class;
            case "Short" -> short.class;
            case "Integer" -> int.class;
            case "Long" -> long.class;
            case "Float" -> float.class;
            case "Double" -> double.class;
            default -> boxed;
        };
    }

    public static <T> void setInterpreter(Class<T> type, FieldInterpreter<? super T> interpreter){
        setInterpreter(Override.class, type, interpreter);
    }

    public static <T> void setInterpreter(Class<? extends Annotation> anno, Class<T> type, FieldInterpreter<? super T> interpreter){
        interpreters.get(anno, ObjectMap::new).put(type, interpreter);
    }

    public static boolean hasInterpreter(Class<?> type){
        return hasInterpreter(Override.class, type);
    }

    public static boolean hasInterpreter(Class<? extends Annotation> anno, Class<?> type){
        return interpreters.get(anno, ObjectMap::new).containsKey(unbox(type));
    }

    public static <T> FieldInterpreter<T> getInterpreter(Class<T> type){
        return getInterpreter(Override.class, type);
    }

    public static <T> FieldInterpreter<T> getInterpreter(Class<? extends Annotation> anno, Class<T> type){
        if(hasInterpreter(anno, type)){
            return (FieldInterpreter<T>)interpreters.get(anno, ObjectMap::new).get(unbox(type));
        }else if(hasInterpreter(Override.class, type)){
            return (FieldInterpreter<T>)interpreters.get(Override.class, ObjectMap::new).get(unbox(type));
        }else if(type.isArray() && !type.getComponentType().isPrimitive()){
            return (FieldInterpreter<T>)(hasInterpreter(anno, Object[].class)
                ? interpreters.get(anno).get(Object[].class)
                : interpreters.get(Override.class).get(Object[].class)
            );
        }else{
            throw new IllegalArgumentException("Interpreter for type " + type + " not set up yet.");
        }
    }

    public static <T> void setProvider(Class<T> type, FieldProvider<T> provider){
        providers.put(unbox(type), provider);
    }

    public static boolean hasProvider(Class<?> type){
        return providers.containsKey(unbox(type));
    }

    public static <T> FieldProvider<T> getProvider(Class<T> type){
        return (FieldProvider<T>)providers.getThrow(unbox(type), () -> new IllegalArgumentException("Provider for type " + type + " not set up yet."));
    }

    public interface FieldInterpreter<T>{
        /**
         * Builds the interpreter for (not-necessarily) a possibly annotated field. Implementations must add exactly
         * 2 columns to the table.
         * @param name    May be empty.
         * @param remover If this callback is not {@code null}, this interpreter should add a button that invokes the
         *                callback to signal element removal.
         * @param indexer If this callback is not {@code null}, this interpreter should add 2 buttons that invoke the
         *                callback to signal element rearrangement with the following values:<ul>
         *                <li>{@code true}: Swap element with previous index.</li>
         *                <li>{@code false}: Swap element with next index.</li>
         *                </ul>
         */
        void build(Table cont,
                   CharSequence name, TypeInfo type,
                   @Nullable Field field,
                   @Nullable Runnable remover, @Nullable Boolc indexer,
                   Prov<T> get, Cons<T> set);
    }

    public interface FieldProvider<T>{
        void get(TypeInfo type, Cons<T> cons);
    }

    /**
     * Stores parameterized or array type information for convenience.
     * For {@code A[]}: {@link #raw} is {@code A[]}, {@link #element} is {@code A}, {@link #key} is {@code null}.
     * For {@code Seq<A>}: {@link #raw} is {@link Seq}, {@link #element} is {@code A}, {@link #key} is {@code null}.
     * For {@code ObjectMap<A, B>}: {@link #raw} is {@link ObjectMap}, {@link #element} is {@code B}, {@link #key} is {@code A}.
     */
    public static class TypeInfo{
        public final Class<?> raw;
        public final TypeInfo element, key;

        public TypeInfo(Field field){
            this(field.getType(), field.getGenericType());
        }

        public TypeInfo(Class<?> raw){
            this(raw, raw);
        }

        /** Use with care! */
        public TypeInfo(Class<?> raw, TypeInfo element){
            this.raw = unbox(raw);
            this.element = element;
            key = null;
        }

        public TypeInfo(Class<?> raw, Type generic){
            this.raw = unbox(raw);
            if(raw.isArray()){
                key = null;
                element = new TypeInfo(raw.getComponentType(), generic instanceof GenericArrayType type ? type.getGenericComponentType() : raw.getComponentType());
            }else if(Seq.class.isAssignableFrom(raw)){
                key = null;
                element = getParam(generic, 0);
            }else if(ObjectMap.class.isAssignableFrom(raw)){
                key = getParam(generic, 0);
                element = getParam(generic, 1);
            }else{
                key = element = null;
            }
        }

        public static TypeInfo getParam(Type generic, int index){
            Type[] params =
                generic instanceof ParameterizedType type ? type.getActualTypeArguments() :
                generic instanceof GenericDeclaration type ? type.getTypeParameters() : null;

            if(params != null && index < params.length){
                var target = params[index];
                return new TypeInfo(raw(target), target);
            }

            return new TypeInfo(Object.class, Object.class);
        }

        public static Class<?> raw(Type type){
            if(type instanceof Class<?> c) return c;
            if(type instanceof ParameterizedType c) return (Class<?>)c.getRawType();
            if(type instanceof GenericArrayType c) return Reflect.newArray(raw(c.getGenericComponentType()), 0).getClass();
            if(type instanceof TypeVariable<?> c) return raw(c.getBounds()[0]);
            return Object.class;
        }
    }
}
