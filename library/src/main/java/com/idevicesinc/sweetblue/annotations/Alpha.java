package com.idevicesinc.sweetblue.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to tag certain classes or methods that are considered "alpha" level quality.
 * This means that the feature will be release quality at some point in the future, but it
 * may require bug fixes, further documentation, code additions, backwards incompatible changes, moving to different
 * packages, etc., etc., to get to that point, so just be aware.
 */
@Retention(RetentionPolicy.CLASS)
public @interface Alpha
{
}
