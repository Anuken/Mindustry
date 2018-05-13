package io.anuke.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public class Annotations {

    /**Marks a method as invokable remotely.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Remote{}

    /**Marks a method to be locally invoked as well as remotely invoked.*/
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.CLASS)
    public @interface Local{}
}
