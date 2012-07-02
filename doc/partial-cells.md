# Partial cells

Cons cells are encoded in increments of five bytes. This is an unfortunate
number because while it fits inside a 64-bit stack cell, it also requires at
least two separate instructions to generate a functioning machine code command
(unless prefixed by a 3-byte nop, in which case the cell can be copied and
executed verbatim).