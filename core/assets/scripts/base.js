"use strict";

let scriptName = "base.js"
let modName = "none"

const log = (context, obj) => Vars.mods.scripts.log(context, String(obj))
const print = text => log(modName + "/" + scriptName, text)
const readString = path => Vars.mods.scripts.readString(path)
const readBytes = path => Vars.mods.scripts.readBytes(path)
const loadMusic = path => Vars.mods.scripts.loadMusic(path)
const loadSound = path => Vars.mods.scripts.loadSound(path)

const readFile = (purpose, ext, cons) => Vars.mods.scripts.readFile(purpose, ext, cons);
const readBinFile = (purpose, ext, cons) => Vars.mods.scripts.readBinFile(purpose, ext, cons);
const writeFile = (purpose, ext, str) => Vars.mods.scripts.writeFile(purpose, ext, str);
const writeBinFile = (purpose, ext, bytes) => Vars.mods.scripts.writeBinFile(purpose, ext, bytes);
const newFloats = cap => Vars.mods.getScripts().newFloats(cap);

//these are not strictly necessary, but are kept for edge cases
const run = method => new java.lang.Runnable(){run: method}
const boolf = method => new Boolf(){get: method}
const boolp = method => new Boolp(){get: method}
const floatf = method => new Floatf(){get: method}
const floatp = method => new Floatp(){get: method}
const cons = method => new Cons(){get: method}
const prov = method => new Prov(){get: method}
const func = method => new Func(){get: method}

const newEffect = (lifetime, renderer) => new Effect.Effect(lifetime, new Effect.EffectRenderer({render: renderer}))
Call = Packages.mindustry.gen.Call

//js 'extend(Base, ..., {})' = java 'new Base(...) {}'
function extend(/*Base, ..., def*/){
    const Base = arguments[0]
    const def = arguments[arguments.length - 1]
    //swap order from Base, def, ... to Base, ..., def
    const args = [Base, def].concat(Array.from(arguments).splice(1, arguments.length - 2))

    //forward constructor arguments to new JavaAdapter
    const instance = JavaAdapter.apply(null, args)
    //JavaAdapter only overrides functions; set fields too
    for(var i in def){
        if(typeof(def[i]) != "function"){
            instance[i] = def[i]
        }
    }
    return instance
}

//For backwards compatibility, use extend instead
const extendContent = extend;
