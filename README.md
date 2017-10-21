## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Rationale

Null references are [considered](https://en.wikipedia.org/wiki/Null_pointer#History) to be one of the most expensive mistakes in IT design. It's not surprising that there are many tools which try to report it as early as possible.  

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

## 3. Alternatives

I found the only alternative which provides similar functionality - [Project Lombok](https://projectlombok.org/features/NonNull). Here are pros and cons for using it:
* only [lombok.NonNull](https://projectlombok.org/api/lombok/NonNull.html) annotation is supported - there might be problems with *IDE* highlighting possible errors in source code
* the feature is implemented through a custom [Annotaton Processing Tool](https://docs.oracle.com/javase/7/docs/technotes/guides/apt/index.html), which means that there are two set of *\*.class* files after the compilation - one from original code and another one with the tool-added instrumentations. Compiler plugin-based approach is more natural for such task as it's completely transparent for the further assembly construction
* a solution offered by the current project [works only for the javac8](core/javac/README.md#5-limitations), *Lombok* might operate with *javac6* and *javac7* (at least in theory as *APT API* is available starting from *java6*, have not verified *Lombok* processing with them though)

## 4. Overview

TBD

## 5. Build

[![Build Status](https://travis-ci.org/denis-zhdanov/traute.svg?branch=master)](https://travis-ci.org/denis-zhdanov/traute)

TBD