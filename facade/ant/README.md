## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. Sample](#3-sample)
* [4. Options](#4-options)
  * [4.1. NotNull Annotations](#41-notnull-annotations)
  * [4.2. Instrumentations Types](#42-instrumentation-types)

## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Overview

*Traute* can be used in *Ant* by putting its [Javac plugin's](../../core/javac/README.md) *\*.jar* into compiler's classpath and specifying necessary *javac* options.

## 3. Sample

*ivy.xml*
```xml
<dependencies>
    <dependency org="tech.harmonysoft" name="traute-javac" rev="1.0.10"/>
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

*NotNull* annotations to use are defined through the *traute.annotations.not.null* option (multiple annotations might be specified separated by *:*):  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Add null-checks only for method parameters/return values marked by @my.company.NotNull -->
    <compilerarg value="-Atraute.annotations.not.null=my.company.NotNull"/>
</javac>
```  

More details on that can be found [here](../../core/javac/README.md#71-notnull-annotations).

### 4.2. Instrumentations Types  

Instrumentations types to use are defined through the *traute.instrumentations* option:  

```xml
<javac srcdir="${src.dir}" destdir="${build.dir}" classpathref="lib.path.id" debug="true">
    <compilerarg value="-Xplugin:Traute"/>
    <!-- Add checks only for method parameters (do not add check for return values) -->
    <compilerarg value="-Atraute.instrumentations=parameter"/>
</javac>
```  

More details on that can be found [here](../../core/javac/README.md#72-instrumentation-types).