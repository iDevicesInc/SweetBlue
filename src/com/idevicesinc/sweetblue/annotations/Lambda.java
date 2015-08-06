package com.idevicesinc.sweetblue.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to denote <code>interface</code> declarations whose implementations can essentially act like lambdas (i.e. anonymous functions).
 * Implementations are technically classes and not language-level lambda constructs because
 * Java at this time does not support them. Conceptually however they can be treated as lambdas.
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Lambda
{

}
