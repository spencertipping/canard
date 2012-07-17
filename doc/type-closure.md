# Type closure

Canard doesn't really have types, but garbage collection makes it crucial to
preserve a certain degree of information about objects. Currently, the following
is assumed:

    1. Data and return stack entries are absolute addresses.
    2. Heap (and therefore symbol table) entries are instruction-encoded data.

This will generally be the case in practical situations, but it makes it easy
from a GC perspective too.