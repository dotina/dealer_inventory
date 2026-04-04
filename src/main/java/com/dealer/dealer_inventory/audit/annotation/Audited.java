package com.dealer.dealer_inventory.audit.annotation;

import com.dealer.dealer_inventory.audit.entity.AuditAction;

import java.lang.annotation.*;

/**
 * Optional annotation to explicitly declare the audit action and entity type
 * on a controller method. If absent, the {@link com.dealer.dealer_inventory.audit.aspect.AuditAspect}
 * infers the action from the HTTP method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
    AuditAction action();
    String entity() default "";
}

