## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. Sample](#3-sample)
* [4. Options](#4-options)

## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Overview

*Traute* can be used in *Ant* by putting its *\*.jar* into compiler's classpath and specifying necessary *javac* options

## 3. Sample

*ivy.xml*
```xml
<dependencies>
    <dependency org="tech.harmonysoft" name="traute-javac" rev="1.0.4"/>
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

A complete standalone sample project can be found [here](sample)