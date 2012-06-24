# Symbol resolution

The biggest reason to dynamically inline symbols is to allow the dictionary to
use a linear-time lookup strategy without negatively impacting invocation
performance. We can't do eager compilation like Forth because Canard supports
self-reference inside sublists. (Forth doesn't have lists, so this wouldn't make
sense there.)

No dynamic rewriting needs to happen at all if we can avoid a linear-time
lookup. I don't mind using one extra jump per function call; this is a constant
overhead and is relatively trivial. We just can't search the symbol table on
every occurrence.