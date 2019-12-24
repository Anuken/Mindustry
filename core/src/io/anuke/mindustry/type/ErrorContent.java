package io.anuke.mindustry.type;

import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.ctype.ContentType;

/** Represents a blank type of content that has an error. Replaces anything that failed to parse. */
public class ErrorContent extends Content{
    @Override
    public ContentType getContentType(){
        return ContentType.error;
    }
}
