## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. Features](#3-features)
* [4. Example](#4-example)
* [5. Usage](#5-usage)
* [6. Settings](#6-settings)
* [7. Evolution](#7-evolution)

## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Overview

This is a [Java Compiler](http://docs.oracle.com/javase/8/docs/technotes/tools/unix/javac.html) plugin which enhances generated *\*.class* files by inserting *null*-checks based on source code annotations.  

See [the main project page](../../README.md#2-rationale) for the rationale to have such an instrument. Also be aware of [alternatives](../../README.md#3-alternatives).

## 3. Features

Following instrumentations types are supported now:
* *method parameter* - a *null*-check is created for a method parameter marked by a configured *NotNull* annotation
* *method return* - a *return* expression inside a method marked by a configured *NotNull* annotation is re-written in a way to store its result in a local variable, then examine it for *null* and do return only if the check passes

## 4. Example

Consider a source code below:
```java
@NotNull
public Integer add(@NotNull Integer a, @NotNull Integer b) {
    return a + b;
}
```

The plugin modifies resulting byte code as if the source looked like this:
```java
@NotNull
public Integer add(@NotNull Integer a, @NotNull Integer b) {
    if (a == null) {
        throw new NullPointerException("Argument 'a' of the method 'add()' is marked by @NotNull but got null for it");
    }
    if (b == null) {
        throw new NullPointerException("Argument 'b' of the method 'add()' is marked by @NotNull but got null for it");
    }
    Integer tmpVar = a + b;
    if (tmpVar == null) {
        throw new NullPointerException("Detected an attempt to return null from a method marked by @NotNull");
    }
    return tmpVar;
}
```

## 5. Usage

1. Put plugin's jar to compiler's classpath
2. Add *-Xplugin:Traute* option to the *javac* command

Example:
```
javac -cp src/main/java\
:~/.gradle/caches/modules-2/files-2.1/tech.harmonysoft:traute-javac-plugin/1.0.0/7a00452c350de0fb80ecbcecfb8ce0145c46141e/traute-javac-plugin-1.0.0.jar \
-Xplugin:Traute \
org/MyClass.java
```
That makes compiler involve the plugin into the processing which, in turn, adds *null*-checks to the *\*.class* file if necessary.

It's also possible to specify a number of plugin-specific options (see below).

## 6. Settings

All plugin settings are delivered through the *-A* command line switch. See [javac documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javac.html) for more details.

**NotNull Annotations**

Following annotations are checked by default:
* [org.jetbrains.annotations.NotNull](https://www.jetbrains.com/help/idea/nullable-and-notnull-annotations.html) - IntelliJ IDEA
* [javax.annotation.Nonnull](https://jcp.org/en/jsr/detail?id=305) - JSR-305
* [javax.validation.constraints.NotNull](https://docs.oracle.com/javaee/7/api/javax/validation/constraints/NotNull.html) - JavaEE
* [edu.umd.cs.findbugs.annotations.NonNull](http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/NonNull.html) - FindBugs
* [android.support.annotation.NonNull](https://developer.android.com/reference/android/support/annotation/NonNull.html) - Android
* [org.eclipse.jdt.annotation.NonNull](http://help.eclipse.org/oxygen/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftasks%2Ftask-using_null_annotations.htm) - Eclipse
* [lombok.NonNull](https://projectlombok.org/api/lombok/NonNull.html) - Lombok

It's possible to define a custom list of annotations to use through the [traute.annotations.not.null](src/main/java/tech/harmonysoft/oss/traute/javac/TrauteJavacPlugin.java#L118) option.  

Example:
* single custom annotation:  

  ```javac -cp <classpath> -Xplugin:Traute -Atraute.annotations.not.null=mycompany.util.NotNull <classes-to-compile>```  

  This instructs the plugin not generating a check for, say, method declared like ```void service(@org.jetbrains.annotations.NotNull Sring param)``` (default annotations to use are replaced by a single given annotation)
* multiple annotations:  

  ```javac -cp <classpath> -Xplugin:Traute -Atraute.annotations.not.null=mycompany.util.NotNull:org.eclipse.jdt.annotation.NonNull <classes-to-compile>```  

  Here *null*-checks will be generated only for our custom annotation class and eclipse annotation

**Instrumentations Types**

Following instrumentation types are supported now:
* [parameter](../common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L31) - adds *null*-checks for method parameters
* [return](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L53) - re-writes *return* instructions in method bodies

Even though they are [thoroughly tested](../test/src/test/java/tech/harmonysoft/oss/traute/test/suite) it's not possible to exclude a possibility that particular use-case is not covered (e.g. we encountered tricky situations like [here](../test/src/test/java/tech/harmonysoft/oss/traute/test/suite/MethodReturnTest.java#L251)). That's why we allow to skip particular instrumentations through the [traute.instrumentations](src/main/java/tech/harmonysoft/oss/traute/javac/TrauteJavacPlugin.java#L139) option.  

Example:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.instrumentations=parameter <classes-to-compile>```  

This effectively disables *return* instrumentation.

**Logging**

The plugin logs only custom options by default:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.instrumentations=parameter <classes-to-compile>```
  
Compiler output:  
```
[Traute plugin]: using the following instrumentations: [parameter]
```

It's possible to turn on *verbose mode* through the [traute.log.verbose](https://github.com/denis-zhdanov/traute/blob/master/core/javac/src/main/java/tech/harmonysoft/oss/traute/javac/TrauteJavacPlugin.java#L131) to get detailed information about performed instrumentations.  

Example:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.log.verbose=true <classes-to-compile>```  

Output:  

```
[Traute plugin]: 'verbose mode' is on
[Traute plugin]: added a null-check for argument 'i2' in the method org.Test.test()
[Traute plugin]: added a null-check for argument 'i1' in the method org.Test.test()
[Traute plugin]: added a null-check for 'return' expression in method org.Test.test()
[Traute plugin]: added 3 instrumentations to the /Users/denis/sample/src/main/java/org/Test.java - METHOD_PARAMETER: 2, METHOD_RETURN: 1
[Traute plugin]: added a null-check for argument 'i1' in the method org.Test2.test()
[Traute plugin]: added 1 instrumentation to the /Users/denis/sample/src/main/java/org/Test2.java - METHOD_PARAMETER: 1
```

## 7. Evolution