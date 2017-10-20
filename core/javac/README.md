## Table of Contents

* [1. License](#1-license)
* [2. Rationale](#2-rationale)
* [3. Example](#3-example)
* [4. Features](#4-features)
* [5. Settings](#4-settings)

## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Overview

This is a [Java Compiler](http://docs.oracle.com/javase/8/docs/technotes/tools/unix/javac.html) plugin which enhances resulting *\*.class* files by automatically generating *null*-checks based on source code annotations.  

See [the main project page](../../README.md) for the rationale to have such an instrument.

## 3. Example

Consider a source code below:
```java
@NotNull
public Integer add(@NotNull Integer a, @NotNull Integer b) {
    return a + b;
}
```

The plugin modifies resulting byte code as if the source looks like this:
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

## 4. Features

## 5. Settings