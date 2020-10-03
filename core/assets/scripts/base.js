"use strict";

const log = function(context, obj){
    Vars.mods.getScripts().log(context, String(obj))
}

const readString = path => Vars.mods.getScripts().readString(path)
const readBytes = path => Vars.mods.getScripts().readBytes(path)
const loadMusic = path => Vars.mods.getScripts().loadMusic(path)
const loadSound = path => Vars.mods.getScripts().loadSound(path)

var scriptName = "base.js"
var modName = "none"

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
