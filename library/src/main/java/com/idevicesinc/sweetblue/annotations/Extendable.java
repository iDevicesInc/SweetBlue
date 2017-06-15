package com.idevicesinc.sweetblue.annotations;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation denotes a class which is meant to be extendable. Most classes in the library are final for
 * better performance. This annotation just makes is clearer which classes are meant to be extendable.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Extendable
{
}
