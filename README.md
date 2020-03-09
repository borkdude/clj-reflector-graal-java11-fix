# clj-reflector-graal-java11-fix

[![Clojars Project](https://img.shields.io/clojars/v/borkdude/clj-reflector-graal-java11-fix.svg)](https://clojars.org/borkdude/clj-reflector-graal-java11-fix)

This library offers a fix for an issue with `clojure.lang.Reflector` when used
together with GraalVM `native-image` on java11.

## Usage

Include this library in your leiningen profile or deps.edn alias so it's
available on the classpath for GraalVM `native-image`. E.g.:

``` clojure
(defproject foo "0.0.1-SNAPSHOT"
  :profiles {:native-image {:dependencies [[borkdude/clj-reflector-graal-java11-fix "0.0.1-graalvm-19.3.1-alpha.2"]]}})
```

Use the right GraalVM version modifier: `graalvm-19.3.1` or
`graalvm-20.0.0`. The modifier must exactly match the version of GraalVM
`native-image`.

Do NOT distribute this library as part of libraries or uberjars that are
supposed to be run with a JVM. Use it for compiling native binaries only.

E.g. to produce an uberjar that is fed to `native-image` you can do:

``` shell
$ lein with-profiles +native-image do clean, uberjar
```

and then:

``` shell
$ native-image -jar target/foo-0.0.1-SNAPSHOT-standalone.jar
```

## The problem

JDK11 is supported since GraalVM 19.3.0. The class `clojure.lang.Reflector` uses
a `MethodHandle` to maintain compatibility with java8 and java11 at the same
time:

``` java
static {
	MethodHandle pred = null;
	try {
		if (! isJava8())
			pred = MethodHandles.lookup().findVirtual(Method.class, "canAccess", MethodType.methodType(boolean.class, Object.class));
	} catch (Throwable t) {
		Util.sneakyThrow(t);
	}
	CAN_ACCESS_PRED = pred;
}
```

GraalVM does not support `MethodHandle`s that cannot be analyzed as a compile
time constant and will complain when used on JDK11:

``` java
Exception in thread "main" com.oracle.svm.core.jdk.UnsupportedFeatureError: Invoke with MethodHandle argument could not be reduced to at most a single call or single field access. The method handle must be a compile time constant, e.g., be loaded from a `static final` field. Method that contains the method handle invocation: java.lang.invoke.Invokers$Holder.invoke_MT(Object, Object, Object, Object)
    at com.oracle.svm.core.util.VMError.unsupportedFeature(VMError.java:101)
    at clojure.lang.Reflector.canAccess(Reflector.java:49)
    ...
```

See the [issue](https://github.com/oracle/graal/issues/2214) on the GraalVM
repo.

## The solution

GraalVM supports substitutions. For JDK11 or later the method `canAccess` is replaced as follows:

``` java
@TargetClass(className = "clojure.lang.Reflector")
final class Target_clojure_lang_Reflector {

    @Substitute
    @TargetElement(onlyWith = JDK11OrLater.class)
    private static boolean canAccess(Method m, Object target) {
        // JDK9+ use j.l.r.AccessibleObject::canAccess, which respects module rules
        try {
            return (boolean) m.canAccess(target);
        } catch (Throwable t) {
            throw Util.sneakyThrow(t);
        }
    }
}
```

## License

Copyright © 2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.

This project contains code from:
- Clojure, which is licensed under the same EPL License.
