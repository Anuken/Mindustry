"use strict";

const log = function(context, obj){
    Vars.mods.scripts.log(context, String(obj))
}

const readString = path => Vars.mods.scripts.readString(path)
const readBytes = path => Vars.mods.scripts.readBytes(path)
const loadMusic = path => Vars.mods.scripts.loadMusic(path)
const loadSound = path => Vars.mods.scripts.loadSound(path)

const readFile = (purpose, ext, cons) => Vars.mods.scripts.readFile(purpose, ext, cons);
const readBinFile = (purpose, ext, cons) => Vars.mods.scripts.readBinFile(purpose, ext, cons);
const writeFile = (purpose, ext, str) => Vars.mods.scripts.writeFile(purpose, ext, str);
const writeBinFile = (purpose, ext, bytes) => Vars.mods.scripts.writeBinFile(purpose, ext, bytes);

let scriptName = "base.js"
let modName = "none"

const print = text => log(modName + "/" + scriptName, text);

const extendContent = function(classType, name, params){
    return new JavaAdapter(classType, params, name)
}

const extend = function(classType, params){
    return new JavaAdapter(classType, params)
}

//these are not sctrictly necessary, but are kept for edge cases
const run = method => new java.lang.Runnable(){run: method}
const boolf = method => new Boolf(){get: method}
const boolp = method => new Boolp(){get: method}
const floatf = method => new Floatf(){get: method}
const floatp = method => new Floatp(){get: method}
const cons = method => new Cons(){get: method}
const prov = method => new Prov(){get: method}
const func = method => new Func(){get: method}

const newEffect = (lifetime, renderer) => new Effects.Effect(lifetime, new Effects.EffectRenderer({render: renderer}))
Call = Packages.mindustry.gen.Call
