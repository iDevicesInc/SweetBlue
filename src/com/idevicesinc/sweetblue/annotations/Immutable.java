package com.idevicesinc.sweetblue.annotations;

import java.lang.annotation.*;

/**
 * Classes marked with this {@link Annotation} have immutable internal state, or the functional behavior of so.
 * Most of the time this means all private final members.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS) // The original version used RUNTIME
public @interface Immutable
{

}
