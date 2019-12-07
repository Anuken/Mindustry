//Generated class. Do not modify.

const print = function(obj){
    java.lang.System.out.println(obj ? String(obj) : "null")
}

const extendContent = function(classType, name, params){
    return new JavaAdapter(classType, params, modName + "-" + name)
}

const extend = function(classType, params){
    return new JavaAdapter(classType, params)
}

