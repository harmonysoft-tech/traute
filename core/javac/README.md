## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. Features](#3-features)
* [4. Example](#4-example)
* [5. Limitations](#5-limitations)
* [6. Usage](#6-usage)
* [7. Settings](#7-settings)
  * [7.1. NotNull Annotations](#71-notnull-annotations)
  * [7.2. NotNullByDefault Annotations](#72-notnullbydefault-annotations)
  * [7.3. Nullable Annotations](#73-nullable-annotations)
  * [7.4. Instrumentations Types](#74-instrumentation-types)
  * [7.5. Exception to Throw](#75-exception-to-throw)
  * [7.6. Exception Text](#76-exception-text)
  * [7.7. Logging](#77-logging)
  * [7.8. Log Location](#78-log-location)
* [8. Evolution](#8-evolution)
* [9. Implementation](#9-implementation)
* [10. Releases](#10-releases)

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

## 5. Limitations

The plugin works with *JDK8* or later - [Compiler Plugin API](https://docs.oracle.com/javase/8/docs/jdk/api/javac/tree/com/sun/source/util/Plugin.html) is introduced only in *java8*.

## 6. Usage

1. Put the plugin's jar to compiler's classpath
2. Add *-Xplugin:Traute* option to the *javac* command line

Example:
```
javac -cp src/main/java\
:~/.gradle/caches/modules-2/files-2.1/tech.harmonysoft:traute-javac-plugin/1.0.7/7a00452c350de0fb80ecbcecfb8ce0145c46141e/traute-javac-plugin-1.0.0.jar \
-Xplugin:Traute \
org/MyClass.java
```
That makes compiler involve the plugin into the processing which, in turn, adds *null*-checks to the *\*.class* file if necessary.

It's also possible to specify a number of plugin-specific options (see below).

## 7. Settings

All plugin settings are delivered through the *-A* command line switch. See [javac documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javac.html) for more details.

### 7.1. NotNull Annotations

The plugin inserts *null*-checks for method parameters and return values marked by the annotations below by default:
* [org.jetbrains.annotations.NotNull](https://www.jetbrains.com/help/idea/nullable-and-notnull-annotations.html) - IntelliJ IDEA
* [javax.annotation.Nonnull](https://jcp.org/en/jsr/detail?id=305) - JSR-305
* [javax.validation.constraints.NotNull](https://docs.oracle.com/javaee/7/api/javax/validation/constraints/NotNull.html) - JavaEE
* [edu.umd.cs.findbugs.annotations.NonNull](http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/NonNull.html) - FindBugs
* [android.support.annotation.NonNull](https://developer.android.com/reference/android/support/annotation/NonNull.html) - Android
* [org.eclipse.jdt.annotation.NonNull](http://help.eclipse.org/oxygen/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftasks%2Ftask-using_null_annotations.htm) - Eclipse
* [lombok.NonNull](https://projectlombok.org/api/lombok/NonNull.html) - Lombok
* [org.springframework.lang.NonNull](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/lang/NonNull.html)

It's possible to define a custom list of annotations to use through the *traute.annotations.not.null* option.  

Example:
* single custom annotation:  

  ```javac -cp <classpath> -Xplugin:Traute -Atraute.annotations.not.null=mycompany.util.NotNull <classes-to-compile>```  

  This instructs the plugin not generating a check for, say, a method parameter defined like ```void service(@org.jetbrains.annotations.NotNull Sring param)``` (default annotations to use are replaced by the single given annotation)
* multiple annotations:  

  ```javac -cp <classpath> -Xplugin:Traute -Atraute.annotations.not.null=mycompany.util.NotNull:org.eclipse.jdt.annotation.NonNull <classes-to-compile>```  

  Here *null*-checks will be generated only for our custom annotation class and Eclipse's *@NonNull* annotation

### 7.2. NotNullByDefault Annotations

It's possible to specify that method parameters/return types are not *null* by default, e.g. consider a *package-info.java* file with the content like below:  

```java
@ParametersAreNonnullByDefault
package my.company;

import javax.annotation.ParametersAreNonnullByDefault;
```  

Here *my.company* package is marked by the [*ParametersAreNonnullByDefault*](https://static.javadoc.io/com.google.code.findbugs/jsr305/3.0.1/javax/annotation/ParametersAreNonnullByDefault.html) annotation. That means that all method parameters for classes in the target package are treated as if they are marked by *NotNull* annotation (except those which are explicitly marked by *Nullable* annotations).  

*Traute* supports such *NotNullByDefault* annotations on package, class and method level.  

We can customize that annotations through the *traute.annotations.not.null.by.default.* option prefix followed by the [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69).  

Example:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.annotations.not.null.by.default.parameter=my.custom.NotNullByDefault```  

This instructs the plugin to use *my.custom.NotNullByDefault* annotation as a *NotNullByDefault* during method parameters instrumentation.  

It's possible to specify more than one annotation such separated by the colon (*:*).  

Following annotations are used by default for processing method parameters:  
* [org.eclipse.jdt.annotation.NonNullByDefault](https://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fannotation%2FNonNullByDefault.html) - Eclipse
* [javax.annotation.ParametersAreNonnullByDefault](https://static.javadoc.io/com.google.code.findbugs/jsr305/3.0.1/javax/annotation/ParametersAreNonnullByDefault.html) - JSR-305
* [org.springframework.lang.NonNullApi](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/lang/NonNullApi.html) - Spring Framework  

Following annotations are used by default for processing method return values:  
* [org.springframework.lang.NonNullApi](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/lang/NonNullApi.html) - Spring Framework  

### 7.3. Nullable Annotations

As mentioned above, it's possible to specify that target method parameters/return values are *NotNullByDefault*. However, we might want to allow *null* in particular use-cases. Such method parameters/return types can be marked by a *Nullable* annotation then.  

Consider an example below:  

```java
@javax.annotation.ParametersAreNonnullByDefault
public void test(Object arg1, Object arg2, @Nullable Object arg3) {
}

```  

When this code is compiled, *null* checks are generated for *arg1* and *arg2* but not for *arg3*:  

```java
@javax.annotation.ParametersAreNonnullByDefault
public void test(Object arg1, Object arg2, @Nullable Object arg3) {
    if (arg1 == null) {
        throw new NullPointerException("'arg1' must not be null");
    }
    if (arg2 == null) {
        throw new NullPointerException("'arg2' must not be null");
    }
}
```  

It's possible to specify *Nullable* annotations to use through the *traute.annotations.nullable* option:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.annotations.nullable=mycompany.util.Nullable <classes-to-compile>```  

Multiple annotations separated by colon (*:*) might be provided.  

Following annotations are used by default:  
* [org.jetbrains.annotations.Nullable](https://www.jetbrains.com/help/idea/nullable-and-notnull-annotations.html#nullable) - IntelliJ IDEA
* [javax.annotation.Nullable](https://jcp.org/en/jsr/detail?id=305) - JSR-305
* [javax.validation.constraints.Null](https://docs.oracle.com/javaee/7/api/javax/validation/constraints/Null.html) - JavaEE
* [edu.umd.cs.findbugs.annotations.Nullable](http://findbugs.sourceforge.net/api/edu/umd/cs/findbugs/annotations/Nullable.html) - FindBugs
* [android.support.annotation.Nullable](https://developer.android.com/reference/android/support/annotation/Nullable.html) - Android
* [org.eclipse.jdt.annotation.Nullable](http://help.eclipse.org/oxygen/index.jsp?topic=%2Forg.eclipse.jdt.doc.user%2Ftasks%2Ftask-using_null_annotations.htm) - Eclipse
* [org.springframework.lang.Nullable](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/lang/Nullable.html) - Spring Framework

### 7.4. Instrumentation Types

Following instrumentation types are supported now:
* [parameter](../common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L31) - adds *null*-checks for method parameters
* [return](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L53) - re-writes *return* instructions in method bodies

Even though they are [thoroughly tested](../test/src/test/java/tech/harmonysoft/oss/traute/test/suite) it's not possible to exclude a possibility that particular use-case is not covered (e.g. we encountered tricky situations like [here](https://github.com/denis-zhdanov/traute/blob/master/core/test/src/test/java/tech/harmonysoft/oss/traute/test/suite/MethodReturnTest.java#L251)). That's why we allow to skip particular instrumentations through the *traute.instrumentations* option.  

Example:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.instrumentations=parameter <classes-to-compile>```  

This effectively disables *return* instrumentation.

### 7.5. Exception to Throw

*NullPointerException* is thrown in case of a failed check by default. However, it's possible to specify another exceptions to be thrown. It's defined through the *traute.exception.* prefix followed by the [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69).  

Example:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.exception.parameter=IllegalArgumentException -Atraute.exception.return=IllegalStateException```

This specifies an *IllegalArgumentException* to be thrown when a *null* is received for a *@NotNull* method parameter and *IllegalStateException* to be thrown when a method marked by *@NotNull* tries to return *null*.

### 7.6. Exception Text

The plugin uses pre-defined error text in *null*-checks, however, it's possible to customize that. It's defined through the *traute.failure.text.* option prefix followed by the [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69).  

It's possible to use substitutions in the custom text value. They are defined through the `${VAR_NAME}` syntax. Following variables are supported now:  
* *PARAMETER_NAME* - expands to the name of the method parameter marked by *@NotNull* where *null* is received (available in *parameter* checks only)  

It's also possible to apply functions to the substituted variables:  
* *capitalize* - capitalizes variable name

Example:

```javac -cp <classpath> -Xplugin:Traute '-Atraute.failure.text.parameter=${capitalize(PARAMETER_NAME)} must not be null'```  

Here the plugin generates a check like below:  

```java
public void test(@NotNull Object myArg) {
    if (myArg == null) {
        throw new NullPointerException("MyArg must not be null");
    }
}
```

### 7.7. Logging

The plugin logs only custom options by default:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.instrumentations=parameter <classes-to-compile>```
  
Compiler output:  
```
[Traute plugin]: using the following instrumentations: [parameter]
```

It's possible to turn on *verbose mode* through the *traute.log.verbose* option to get detailed information about performed instrumentations.  

Example:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.log.verbose=true <classes-to-compile>```  

Output:  

```
[Traute plugin]: 'verbose mode' is on
[Traute plugin]: added a null-check for argument 'i2' in the method org.Test.test()
[Traute plugin]: added a null-check for argument 'i1' in the method org.Test.test()
[Traute plugin]: added a null-check for 'return' expression in method org.Test.test()
[Traute plugin]: added 3 instrumentations to the class /Users/denis/sample/src/main/java/org/Test.java - METHOD_PARAMETER: 2, METHOD_RETURN: 1
[Traute plugin]: added a null-check for argument 'i1' in the method org.Test2.test()
[Traute plugin]: added 1 instrumentation to the class /Users/denis/sample/src/main/java/org/Test2.java - METHOD_PARAMETER: 1
```

### 7.8. Log Location

The plugin logs into compiler's output by default. However, it's possible to configure a custom file to hold that data. Corresponding option is *traute.log.file*.  

Example:  

```javac -cp <classpath> -Xplugin:Traute -Atraute.log.file=/home/me/traute.log```

The logs will be written into `/home/me/traute.log`

## 8. Evolution

Current feature set is a must-have for runtime *null*-checks, however, it's possible to extend it. Here are some ideas on what might be done:
* support *NotNull* annotations on fields - insert *null*-checks in constructors for *final* fields and add *null*-check to call-sites for non-*final* fields
* support more checks implied by existing annotations like [@Contract](https://www.jetbrains.com/help/idea/contract-annotations.html) or introduce new 'assure something' annotations

## 9. Implementation

Implementation details are described [in this blog post](http://blog.harmonysoft.tech/2017/10/how-to-write-javac-plugin.html).

## 10. Releases

[Release Notes](RELEASE.md)

<a href='https://bintray.com/bintray/jcenter/tech.harmonysoft%3Atraute-javac?source=watch' alt='Get automatic notifications about new "tech.harmonysoft:traute-javac" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_color.png'></a>