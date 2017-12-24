## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. Sample](#3-sample)
* [4. Options](#4-options)
  * [4.1. NotNull Annotations](#41-notnull-annotations)
  * [4.2. NotNullByDefault Annotations](#42-notnullbydefault-annotations)
  * [4.3. Nullable Annotations](#43-nullable-annotations)
  * [4.4. Instrumentations Types](#44-instrumentation-types)
  * [4.5. Exception to Throw](#45-exception-to-throw)
  * [4.6. Exception Text](#46-exception-text)
  * [4.7. Logging](#47-logging)
  * [4.8. Log Location](#48-log-location)

## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Overview

*Traute* can be used in *Ant* by putting its [Javac plugin's](../../core/javac/README.md) *\*.jar* into compiler's classpath and specifying necessary *javac* options.

## 3. Sample

*ivy.xml*
```xml
<dependencies>
    <dependency org="tech.harmonysoft" name="traute-javac" rev="1.1.5"/>
    <!-- ... -->
</dependencies>
```

*build.xml*
```xml
<target name="compile">
    <ivy:cachepath pathid="lib.path.id"/>
    <javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
        <compilerarg value="-Xplugin:Traute"/>
    </javac>
</target>
```

A complete standalone sample project can be found [here](https://github.com/denis-zhdanov/traute/tree/master/facade/ant/sample).

## 4. Options

Any [Traute Javac Plugin setting](../../core/javac/README.md#7-settings) can be provided to the *Ant's* &lt;javac&gt; task through the &lt;compilerarg&gt; element.  

### 4.1. NotNull Annotations  

*NotNull* annotations to use are defined through the *traute.annotations.not.null* option (multiple annotations might be specified separated by the colon (*:*)):  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Add null-checks only for method parameters/return values marked by @my.company.NotNull -->
    <compilerarg value="-Atraute.annotations.not.null=my.company.NotNull"/>
</javac>
```  

More details on that can be found [here](../../core/javac/README.md#71-notnull-annotations).

### 4.2. NotNullByDefault Annotations

*NotNullByDefault* annotations to use are defined through the *traute.annotations.not.null.by.default.* option prefix followed by the [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69):  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Use my.custom.NotNullByDefault for method parameters -->
    <compilerarg value="-Atraute.annotations.not.null.by.default.parameter=my.custom.NotNullByDefault"/>
</javac>
```  

More details on that can be found [here](../../core/javac/README.md#72-notnullbydefault-annotations).  

### 4.3. Nullable Annotations  

*Nullable* annotations to use are defined through the *traute.annotations.nullable* option (multiple annotations might be specified separated by *:*):  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Do not generate null-checks for method parameters/return values marked by @my.company.Nullable -->
    <compilerarg value="-Atraute.annotations.not.null=my.company.Nullable"/>
</javac>
```

More details on that can be found [here](../../core/javac/README.md#73-nullable-annotations).

### 4.4. Instrumentation Types  

Instrumentations types to use are defined through the *traute.instrumentations* option:  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Add checks only for method parameters (do not add check for return values) -->
    <compilerarg value="-Atraute.instrumentations=parameter"/>
</javac>
```  

More details on that can be found [here](../../core/javac/README.md#74-instrumentation-types).  

### 4.5. Exception to Throw  

Custom exception class to throw from failed *null*-checks is defined through the *traute.exception.* option prefix followed by the [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69):  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Throw a IllegalArgumentException when a null is given to a method parameter marked by @NotNull -->
    <compilerarg value="-Atraute.exception.parameter=IllegalArgumentException"/>
    <!-- Throw a IllegalStateException when a null is given from a method marked by @NotNull -->
    <compilerarg value="-Atraute.exception.return=IllegalStateException"/>
</javac>
```  

More details on that can be found [here](../../core/javac/README.md#75-exception-to-throw).  

### 4.6. Exception Text

Custom exception text to use in exceptions thrown from failed *null*-checks is defined through the *traute.failure.text.* option prefix followed by the [instrumentation type](https://github.com/denis-zhdanov/traute/blob/master/core/common/src/main/java/tech/harmonysoft/oss/traute/common/instrumentation/InstrumentationType.java#L69):  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Use exception message like 'MyArg must not be null' for a method parameter names 'myArg' -->
    <compilerarg value="-Atraute.failure.text.parameter=${capitalize(PARAMETER_NAME)} must not be null"/>
</javac>
```  

More details on that can be found [here](../../core/javac/README.md#76-exception-text).  

### 4.7. Logging  

Logging verbosity is defined through the *traute.log.verbose* option:  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Use verbose logging -->
    <compilerarg value="-Atraute.log.verbose=true"/>
</javac>
```  

More details on that can be found [here](../../core/javac/README.md#77-logging).  

### 4.8. Log Location  

Plugin's log file is defined through the *traute.log.file* option:  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Instruct the plugin to write its logs to the /home/me/traute.log -->
    <compilerarg value="-Atraute.log.file=/home/me/traute.log"/>
</javac>
```  

More details on that can be found [here](../../core/javac/README.md#78-log-location).