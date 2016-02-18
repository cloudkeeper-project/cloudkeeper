# CloudKeeper
CloudKeeper is a domain-specific language and a corresponding runtime system for data flows. While motivated by
Lifecode's needs of analyzing the human genome at scale, CloudKeeper is entirely general-purpose and abstracts away
tasks like data transfer, serialization, scheduling, checkpointing, and package/dependency management. For Lifecode this
means, e.g., that without any source-code modifications, our genome-analysis data flow can be run purely in-memory
within a single JVM as well as in a distributed fashion in the cloud.

CloudKeeper is superficially similar to academic workflow management systems like Taverna or Pegasus, though it targets
software engineers instead of users. The statically typed DSL piggybacks on existing IDE support for Java, Scala, or
Groovy -- it also allows seamless integration of data-flow programming into any JVM-based language. The runtime system
is available as just a library, and it is lightweight enough to be used as alternative to lower-level parallelization
concepts such as threads, Java executor services, actor systems, futures/promises etc. CloudKeeper is highly modular and
versatile: E.g., intermediate results can be kept as in-memory Java objects as well as in the file system or in a
cloud-storage service. Likewise, processing of individual tasks can be as different as using an existing thread pool or
by submitting a job to a distributed resource manager like Grid Engine.

# Build

CloudKeeper uses the [Maven](https://maven.apache.org) build tool, so all that is necessary to build CloudKeeper is to
run `mvn install`.

## Special Instructions

### IntelliJ

- Import from existing sources as Maven project. Once imported, in the “Maven Projects” tool window, ignore project
  “ASM repackaged”. Unless this is done, IntelliJ would complain that it cannot find classes in package
  `com.svbio.cloudkeeper.relocated.org.objectweb.asm.*`.

# License

Copyright 2016, Lifecode Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this project except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
