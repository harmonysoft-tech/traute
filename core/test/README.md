## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Overview

This module serves the following purposes:
* define a test framework to be used for all 'production' modules - [tech.harmonysoft.oss.traute.test.api](src/test/java/tech/harmonysoft/oss/traute/test/api).  
  Generally, it establishes two actions:
  * *[compile](src/test/java/tech/harmonysoft/oss/traute/test/api/engine/TestCompiler.java)* - transform a _String_ (test source) into a _Collection<byte[]>_ (compiled binaries). Note that we use a _Collection_ here because a single source file might produce more than one *\*.class* file in case of [nested](https://docs.oracle.com/javase/tutorial/java/javaOO/nested.html) classes
  * *[run](src/test/java/tech/harmonysoft/oss/traute/test/api/engine/TestRunner.java)* - execute a class with the given name from the given binaries  

  It's also allowed to define [expectations](src/test/java/tech/harmonysoft/oss/traute/test/api/expectation/Expectation.java) for the actions above.
   
  *Note: basic implementation is also provided by the current module -* [tech.harmonysoft.oss.traute.test.impl](src/test/java/tech/harmonysoft/oss/traute/test/impl)
* an abstract test suite which is assumed to be re-used in every 'production' module - [ech.harmonysoft.oss.traute.test.suite](src/test/java/tech/harmonysoft/oss/traute/test/suite).  
  
  Every module is expected to inject its own test framework service implementations through the [junit extension](http://junit.org/junit5/docs/current/user-guide/#extensions) which specifies [TestCompiler](src/test/java/tech/harmonysoft/oss/traute/test/suite/AbstractTrauteTest.java#L20) and [TestRunner](src/test/java/tech/harmonysoft/oss/traute/test/suite/AbstractTrauteTest.java#L21) to use  
  
  *Note: [in-memory TestRunner](src/test/java/tech/harmonysoft/oss/traute/test/impl/engine/TrauteInMemoryTestRunner.java) is [used by default](src/test/java/tech/harmonysoft/oss/traute/test/impl/engine/AbstractTrauteExtension.java#L23)*
  
E.g. [javac-plugin](../javac/README.md) uses current module in the following way:
1. Test sources provided by the suite are compiled in-memory by *javac* called [programmatically](https://docs.oracle.com/javase/8/docs/api/javax/tools/JavaCompiler.html) (achieved through the [TrauteJavacTestCompiler](../javac/src/test/java/tech/harmonysoft/oss/traute/javac/test/impl/TrauteJavacTestCompiler.java) plugged through the [TrauteJavacExtension](../javac/src/test/java/tech/harmonysoft/oss/traute/javac/test/impl/TrauteJavacExtension.java))
2. Compiled binaries are executed by loading them by a custom class loader and calling *main()* through *Reflection API* ([default TestRunner](src/test/java/tech/harmonysoft/oss/traute/test/impl/engine/TrauteInMemoryTestRunner.java))