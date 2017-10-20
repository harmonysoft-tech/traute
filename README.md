## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Rationale

Null references are [considered](https://en.wikipedia.org/wiki/Null_pointer#History) to be one of the most expensive mistakes in IT design. It's not surprising that there a many tools which try to report it as early as possible.  

For example, here [IntelliJ IDEA](https://www.jetbrains.com/idea/) warns us about possible *NPE*: 

![warning-intellij.png](docs/img/warning-intellij.png)

Moreover, when the code above is compiled by the *IDE*, it automatically inserts *null*-checks:

```java
public void doJob(@NotNull String input) {
    if (input == null) {
        throw new NullPointerException("Argument for @NotNull parameter 'input' must not be null");
    }
}
```

Practice shows that it's a very useful feature, especially when a project is big - it's possible to setup automatic exceptions monitoring and detect the problem early.  

Unfortunately, it's not always convenient to use *IntelliJ* build system for compiling sources. More likely you setup a build through [Gradle](https://gradle.org/)/[Maven](http://maven.apache.org/). It would not harm to get *IDE* tips on possible *null*-related problems and that auto-generated runtime checks.  

Current tool solves the second problem - it allows to add *null*-checks into *\*.class* files based on source code annotations.

## 2. Overview

TBD

## 3. Build

[![Build Status](https://travis-ci.org/denis-zhdanov/traute.svg?branch=master)](https://travis-ci.org/denis-zhdanov/traute)

TBD