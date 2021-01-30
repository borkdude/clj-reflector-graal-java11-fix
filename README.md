# clj-reflector-graal-java11-fix

***
:tada: **Great news!** Starting with GraalVM v21, this fix should no longer be needed.
***

[![Clojars Project](https://img.shields.io/clojars/v/borkdude/clj-reflector-graal-java11-fix.svg)](https://clojars.org/borkdude/clj-reflector-graal-java11-fix)

This library offers a fix for an issue with `clojure.lang.Reflector` when used
together with GraalVM `native-image` on java11.

## Usage

Include this library in your leiningen profile or deps.edn alias so it's
available on the classpath for GraalVM `native-image`. 

Important:

- Use the right GraalVM version modifier: e.g. `graalvm-20.3.0`. The modifier must exactly match the version of GraalVM
`native-image` you are using.
- Do NOT distribute this library as part of libraries or applications that are run
with a JVM. Use it for compiling to native binaries only.

### Leiningen

Relevant config for `project.cljc`:
``` clojure
(defproject foo "0.0.1-SNAPSHOT"
  :profiles {:native-image {:dependencies [[borkdude/clj-reflector-graal-java11-fix "0.0.1-graalvm-20.3.0"]]}})
```

To produce an uberjar that is fed to `native-image` you can:

``` shell
$ lein with-profiles +native-image do clean, uberjar
```

and then:

``` shell
$ native-image -jar target/foo-0.0.1-SNAPSHOT-standalone.jar
```

### Tools Deps

Relevant config for `deps.edn`:
``` clojure
{:aliases
 {:native-image {:extra-deps {borkdude/clj-reflector-graal-java11-fix
                              {:mvn/version "0.0.1-graalvm-20.3.0"
                               :exclusions [org.graalvm.nativeimage/svm]}}}}}
```

Notice the exclusion? This project depends on native dep `org.graalvm.nativeimage/svm`.
While technically correct, tools deps does not support native deps and fails for this native dep.
Since the native dep is provided by the Graal installation anyway, it can be safely excluded for tools deps.

Your compile script would get the classspath for `native-image` via:

```shell
clojure -A:native-image -Spath
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
time constant. It will complain when it analyzes `clojure.lang.Reflector` on
JDK11:

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
