# Minimal symbol table

A minimal Canard image needs the following from a symbol table:

    1. A way to parse input. We can't just assume this is taken care of, since we
       might want to load more input or inspect the bootstrap parser.
    2. A way to construct, inspect, and execute lists.
    3. A way to manipulate the stack pointer and read/write arbitrary memory.
    4. A way to define more symbols.
    5. A way to write new machine code from within Canard source.

The simplest way to do all of this, most likely, is to just provide a few
functions to manipulate the stack and deal with lists, and to then provide one
macro-definition that allows the user to type hexadecimal numbers. This macro
definition can be used to write the machine code necessary to write new
symbol-definition functions.