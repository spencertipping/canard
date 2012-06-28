# Bootstrapping

There isn't much need to write a binary Canard image in itself. The reason is
that this will be just as platform-specific as the original bootstrap
interpreter. The interpreter allows you to inspect all of its internals, so you
could conceivably create a copy of it at another location in memory.