/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 * git commit: 473b5b7339473fea887281989f20e6af22c9b07d
 */

package com.google.common.base;

/**
 * Static convenience methods that help a method or constructor check whether it was invoked
 * correctly (that is, whether its <i>preconditions</i> were met).
 *
 * <p>If the precondition is not met, the {@code Preconditions} method throws an unchecked exception
 * of a specified type, which helps the method in which the exception was thrown communicate that
 * its caller has made a mistake. This allows constructs such as
 *
 * <pre>{@code
 * public static double sqrt(double value) {
 *   if (value < 0) {
 *     throw new IllegalArgumentException("input is negative: " + value);
 *   }
 *   // calculate square root
 * }
 * }</pre>
 *
 * <p>to be replaced with the more compact
 *
 * <pre>{@code
 * public static double sqrt(double value) {
 *   checkArgument(value >= 0, "input is negative: %s", value);
 *   // calculate square root
 * }
 * }</pre>
 *
 * <p>so that a hypothetical bad caller of this method, such as:
 *
 * <pre>{@code
 * void exampleBadCaller() {
 *   double d = sqrt(-1.0);
 * }
 * }</pre>
 *
 * <p>would be flagged as having called {@code sqrt()} with an illegal argument.
 *
 * <h3>Performance</h3>
 *
 * <p>Avoid passing message arguments that are expensive to compute; your code will always compute
 * them, even though they usually won't be needed. If you have such arguments, use the conventional
 * if/throw idiom instead.
 *
 * <p>Depending on your message arguments, memory may be allocated for boxing and varargs array
 * creation. However, the methods of this class have a large number of overloads that prevent such
 * allocations in many common cases.
 *
 * <p>The message string is not formatted unless the exception will be thrown, so the cost of the
 * string formatting itself should not be a concern.
 *
 * <p>As with any performance concerns, you should consider profiling your code (in a production
 * environment if possible) before spending a lot of effort on tweaking a particular element.
 *
 * <h3>Other types of preconditions</h3>
 *
 * <p>Not every type of precondition failure is supported by these methods. Continue to throw
 * standard JDK exceptions such as {@link java.util.NoSuchElementException} or {@link
 * UnsupportedOperationException} in the situations they are intended for.
 *
 * <h3>Non-preconditions</h3>
 *
 * <p>It is of course possible to use the methods of this class to check for invalid conditions
 * which are <i>not the caller's fault</i>. Doing so is <b>not recommended</b> because it is
 * misleading to future readers of the code and of stack traces. See <a
 * href="https://github.com/google/guava/wiki/ConditionalFailuresExplained">Conditional failures
 * explained</a> in the Guava User Guide for more advice. Notably, Verify offers assertions
 * similar to those in this class for non-precondition checks.
 *
 * <h3>{@code java.util.Objects.requireNonNull()}</h3>
 *
 * <p>Projects which use {@code com.google.common} should generally avoid the use of {@link
 * java.util.Objects#requireNonNull(Object)}. Instead, use whichever of
 * #checkNotNull(Object) or Verify#verifyNotNull(Object) is appropriate to the situation.
 * (The same goes for the message-accepting overloads.)
 *
 * <h3>Only {@code %s} is supported</h3>
 *
 * <p>{@code Preconditions} uses Strings#lenientFormat to format error message template
 * strings. This only supports the {@code "%s"} specifier, not the full range of {@link
 * java.util.Formatter} specifiers. However, note that if the number of arguments does not match the
 * number of occurrences of {@code "%s"} in the format string, {@code Preconditions} will still
 * behave as expected, and will still include all argument values in the error message; the message
 * will simply not be formatted exactly as intended.
 *
 * <h3>More information</h3>
 *
 * <p>See the Guava User Guide on <a
 * href="https://github.com/google/guava/wiki/PreconditionsExplained">using {@code
 * Preconditions}</a>.
 *
 * @author Kevin Bourrillion
 * @since 2.0
 */
public final class Preconditions {
    private Preconditions() {
    }

    /**
     * Ensures the truth of an expression involving one or more parameters to the calling method.
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if {@code expression} is false
     */
    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }
}
