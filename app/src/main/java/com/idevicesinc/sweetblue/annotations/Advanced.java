package com.idevicesinc.sweetblue.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to tag certain classes or methods that
 * someone new to the library can most-likely safely ignore. Thus it doesn't necessarily
 * signify that a concept is hard to grasp, just that you probably don't need to grasp it now or maybe ever.
 */
@Retention(RetentionPolicy.CLASS)
public @interface Advanced
{
}
