import jnim

jclass io.anuke.mindustry.desktop.DesktopLauncher of JVMObject:
    proc main(args: seq[string]) {.`static`.}

initJNI(JNIVersion.v1_6, @["-Djava.class.path=build/Mindustry.jar"])

DesktopLauncher.main(@[])

