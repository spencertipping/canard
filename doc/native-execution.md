# Native execution

Canard doesn't have a garbage collector initially, so how should the initial execution model work? It is unclear whether the native environment should even provide an allocation function at
all; maybe this is best left to the standard library. Maybe the initial environment uses a linear stack and provides no allocation other than mmap(). The other stuff could be built as custom
managed heaps on top of this.

There's a lot to recommend this approach. The problem is that the main environment is likely to have a representation that differs from list-based environments, which means that potentially
more code is required to work with it.