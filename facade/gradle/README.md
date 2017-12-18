## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. Usage](#3-usage)
* [4. Configuration](#4-configuration)
* [5. Samples](#5-samples)
* [6. Releases](#6-releases)

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
  id "tech.harmonysoft.oss.traute" version "1.0.5"
}
```

**Maven Central**

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath "gradle.plugin.tech.harmonysoft:traute-gradle:1.0.5"
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
    classpath "gradle.plugin.tech.harmonysoft:traute-gradle:1.0.5"
  }
}

apply plugin: "tech.harmonysoft.oss.traute"
```

## 4. Configuration

As current plugin just seamlessly applies [Traute Javac Plugin](../../core/javac/README.md) to end-user's *Gradle* project, it provides the same [feature set](../../core/javac/README.md#7-settings).  

All configuration settings are specified in the `traute` *build.gradle* section. The only mandatory option is `javacPluginVersion` - it specifies *Traute Javac Plugin* version to use (note that *Traute Javac Plugin* is available either in *Maven Central* or *JCenter*):

```groovy
traute {
    javacPluginVersion = '1.0.10'
}
```

*Optional settings*

**NotNull Annotations**

```groovy
traute {
    notNullAnnotations = [ 'org.mycompany.util.NotNull', 'org.jetbrains.annotations.NotNull' ]
}
```

**Instrumentation Types**

```groovy
traute {
    instrumentations = [ 'parameter' ] // <- Available values are 'parameter' and 'return'
}
```

**Logging**

```groovy
traute {
    verbose = true // <- 'verbose mode' is false by default
}
```

**Log Location**

```groovy
traute {
    logFile = "$buildDir/traute.log"
}
```

**Exceptions to Throw**

```groovy
traute {
    exceptionsToThrow = [
                          'parameter' : 'IllegalArgumentException',
                          'return'    : 'IllegalStateException'
                        ]
}
```

**Exception Text**

```groovy
traute {
    exceptionTexts = [ 'parameter' : '${capitalize(PARAMETER_NAME)} must not be null' ]
}
```

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

## 6. Releases

[Release Notes](RELEASE.md)

<a href='https://bintray.com/denis-zhdanov/harmonysoft.tech/traute-gradle?source=watch' alt='Get automatic notifications about new "traute-gradle" versions'><img src='https://www.bintray.com/docs/images/bintray_badge_color.png'></a>