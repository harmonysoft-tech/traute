## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. Usage](#3-usage)
* [4. Configuration](#4-configuration)
  * [4.1. NotNull Annotations](#41-notnull-annotations)
  * [4.2. NotNullByDefault Annotations](#42-notnullbydefault-annotations)
  * [4.3. Nullable Annotations](#43-nullable-annotations)
  * [4.4. Instrumentations Types](#44-instrumentation-types)
  * [4.5. Exception to Throw](#45-exception-to-throw)
  * [4.6. Exception Text](#46-exception-text)
  * [4.7. Logging](#47-logging)
  * [4.8. Log Location](#48-log-location)
* [5. Samples](#5-samples)

## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Overview

This is a [Gradle](https://gradle.org/) plugin which allows to seamlessly use [Traute Javac Plugin](../../core/javac/README.md) plugin in your *Gradle* projects.  

This functionality is applicable either for *Java* *Gradle* projects or *Android* projects.  

## 3. Usage

Your *Gradle* project can be configured to fetch current plugin from the sources below:

**Gradle Plugins Registry**

Add the following to your *build.gradle*:  

```groovy
plugins {
  id "tech.harmonysoft.oss.traute" version "1.1.5"
}
```

**Maven Central**

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath "tech.harmonysoft:traute-gradle:1.1.5"
  }
}

apply plugin: "tech.harmonysoft.oss.traute"
```

**JCenter**

```groovy
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "tech.harmonysoft:traute-gradle:1.1.5"
  }
}

apply plugin: "tech.harmonysoft.oss.traute"
```

## 4. Configuration

As current plugin just seamlessly applies [Traute Javac Plugin](../../core/javac/README.md) to end-user's *Gradle* project, it provides the same [feature set](../../core/javac/README.md#7-settings).  

All configuration settings are specified in the `traute` *build.gradle* section.  

### 4.1. NotNull Annotations  

*NotNull* annotations to use are defined through the *notNullAnnotations* option:  

```groovy
traute {
    // Add null-checks only for method parameters/return values marked by @my.company.NotNull
    notNullAnnotations = [ 'org.mycompany.util.NotNull', 'org.jetbrains.annotations.NotNull' ]
}
```

More details on that can be found [here](../../core/javac/README.md#71-notnull-annotations).  

### 4.2. NotNullByDefault Annotations

*NotNullByDefault* annotations to use are defined through the *notNullByDefaultAnnotations* option as a *Map* where [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69) is used as a key:  

```groovy
traute {
    // Use my.custom.NotNullByDefault for method parameters
    notNullByDefaultAnnotations = [ 'parameter': [ 'my.custom.NotNullByDefault' ] ]
}
```  

More details on that can be found [here](../../core/javac/README.md#72-notnullbydefault-annotations).  

### 4.3. Nullable Annotations  

*Nullable* annotations to use are defined through the *nullableAnnotations* option:  

```groovy
traute {
    // Do not generate null-checks for method parameters/return values marked by @my.company.Nullable
    nullableAnnotations = [ 'my.company.Nullable' ]
}
```

More details on that can be found [here](../../core/javac/README.md#73-nullable-annotations).  

### 4.4. Instrumentation Types  

Instrumentations types to use are defined through the *instrumentations* option:  

```groovy
traute {
    // Add checks only for method parameters (do not add check for return values)
    instrumentations = [ 'parameter' ]
}
```  

More details on that can be found [here](../../core/javac/README.md#74-instrumentation-types).

### 4.5. Exception to Throw  

Custom exception class to throw from failed *null*-checks is defined through the *exceptionsToThrow* option as a *Map* where [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69) is used as a key:  

```groovy
traute {
    exceptionsToThrow = [
        // Throw a IllegalArgumentException when a null is given to a method parameter marked by @NotNull
        'parameter': 'IllegalArgumentException',
        // Throw a IllegalStateException when a null is given from a method marked by @NotNull
        'return': 'IllegalStateException'
    ]
}
```

More details on that can be found [here](../../core/javac/README.md#75-exception-to-throw).  

### 4.6. Exception Text

Custom exception text to use in exceptions thrown from failed *null*-checks is defined through the *exceptionTexts* option as a *Map* where [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69) is used as a key:  

```groovy
traute {
    // Use exception message like 'MyArg must not be null' for a method parameter named 'myArg'
    exceptionTexts = [ 'parameter': '${capitalize(PARAMETER_NAME)} must not be null' ]
}
```

More details on that can be found [here](../../core/javac/README.md#76-exception-text).  

### 4.7. Logging  

Logging verbosity is defined through the *verbose* option:  

```groovy
traute {
    // Use verbose logging
    verbose = true
}
```  

More details on that can be found [here](../../core/javac/README.md#77-logging).  

### 4.8. Log Location  

Plugin's log file is defined through the *logFile* option:  

```groovy
traute {
    logFile = "$buildDir/traute.log"
}
```  

More details on that can be found [here](../../core/javac/README.md#78-log-location).  

## 5. Samples

**Android**

A sample *Android* project which is configured to use the current plugin can be found [here](https://github.com/denis-zhdanov/traute/tree/master/facade/gradle/sample/android). It uses a custom *NotNull* annotation and 'verbose mode' is set on.  

We get the following console output when it's built:  

```
...
:app:compileDebugJavaWithJavac
[Traute plugin]: 'verbose mode' is on
[Traute plugin]: using the following NotNull annotations: [org.myapplication.NN]
[Traute plugin]: added a null-check for argument 's' in the method org.myapplication.MainActivity.check()
[Traute plugin]: added 1 instrumentation to the /Users/denis/project/my/traute/facade/gradle/sample/android/app/src/main/java/org/myapplication/MainActivity.java - METHOD_PARAMETER: 1
```

When the activity is resumed, it shows that a *null*-check was inserted into activity's code:

<img src="/docs/img/android-ui.png" height="300px">

**Java**

A sample *Java* project which is configured to use the current plugin can be found [here](https://github.com/denis-zhdanov/traute/tree/master/facade/gradle/sample/java):

```
gradlew build execute
:compileJava
[Traute plugin]: 'verbose mode' is on
[Traute plugin]: added a null-check for 'return' expression in method tech.harmonysoft.oss.traute.Test.getInt()
[Traute plugin]: added 1 instrumentation to the /Users/denis/project/my/traute/facade/gradle/sample/java/src/main/java/tech/harmonysoft/oss/traute/Test.java - METHOD_RETURN: 1
:processResources NO-SOURCE
:classes
:jar
:assemble
:compileTestJava NO-SOURCE
:processTestResources NO-SOURCE
:testClasses UP-TO-DATE
:test NO-SOURCE
:check UP-TO-DATE
:build
:execute
Exception in thread "main" java.lang.NullPointerException: Detected an attempt to return null from a method marked by org.jetbrains.annotations.NotNull
        at tech.harmonysoft.oss.traute.Test.getInt(Test.java:13)
        at tech.harmonysoft.oss.traute.Test.main(Test.java:8)
:execute FAILED
```