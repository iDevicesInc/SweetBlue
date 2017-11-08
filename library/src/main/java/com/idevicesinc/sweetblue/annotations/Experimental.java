package com.idevicesinc.sweetblue.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Classes, or methods marked with this annotation are experimental features. You should be aware that if you use anything marked with this
 * annotation, that the resulting behavior may be unpredictable.
 */
@Target(value = {ElementType.TYPE, ElementType.METHOD})
public @interface Experimental
{
}
