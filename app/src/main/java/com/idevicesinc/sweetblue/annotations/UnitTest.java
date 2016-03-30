package com.idevicesinc.sweetblue.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used to dictate certain classes and/or fields which are
 * only used for Unit testing, and should be ignored.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface UnitTest
{
}
