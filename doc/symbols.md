# Symbols

A symbol is a mutable (!) key/value mapping. On x86 it is encoded as an optional 32-bit nop tag (the string data) followed by a 32-bit relative jump. The jump target is changed to reflect
changes to the symbol's definition. Symbols don't themselves add a return address to the stack because their calling context will generally be a list. The only case when it isn't is when the
symbol is quoted:

    = 'foo 'bar

In this case, the jump address of 'foo is assigned to be the code address of 'bar. This works since the symbol's string data is encoded as a nop and will therefore be ignored by the processor.
So technically, each 'inlined' = operation ends up adding two instructions of overhead -- but does not use any additional stack frames, making it a function alias from a space perspective.