package com.kejelah.pencarikeje.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated caller into a controller method.
 *
 * <p>This is the only sanctioned way to learn who is calling (SEC-04).
 *
 * <pre>{@code
 * @GetMapping
 * List<X> list(@CurrentUser AuthenticatedUser user) { ... }
 * }</pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
