package com.cnethuka.kegare.platform.bus;

import com.cnethuka.kegare.platform.Dist;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class whose static {@link SubscribeEvent} methods are registered at startup. Fabric has no annotation
 * scanning, so the classes are listed in {@code com.cnethuka.kegare.platform.Subscribers}; the annotation documents it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventBusSubscriber {
    String modid() default "";

    Dist[] value() default {Dist.CLIENT, Dist.DEDICATED_SERVER};
}
