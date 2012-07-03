# Partial cells

Cons cells are encoded in increments of five bytes. This is an unfortunate
number because while it fits inside a 64-bit stack cell, it also requires at
least two separate instructions to generate a functioning machine code command
(unless prefixed by a 3-byte nop, in which case the cell can be copied and
executed verbatim).

I think this is acceptable. We can have separate instructions for writing
various sizes of memory; the limit case is that everything happens in terms of
bytes, but this creates a significant performance overhead. We can have several
instructions, one for each integer size:

    =q    move all eight cell bytes
    =d    move lower four cell bytes
    =w    move lower two cell bytes
    =b    move lower byte

These instructions are impacted by the endianness of the processor; so for Intel
x64, you would have this (the value on the top of the stack is
8877665544332211):

    11 22 33 44 55 66 77 88
    --------- =q ----------
    --- =d ----
    -=w--
    =b