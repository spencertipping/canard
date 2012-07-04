# Symbol encoding

Symbols are encoded as length-prefixed byte strings. The length is the number of
bytes occupied by the symbol's string data, so the total space is one greater.
For example, here is how 'hello' is encoded:

    05 68 65 6c 6c 6f

Symbols are deliberately minimalistic; you should use blobs of memory if you
want more general-purpose strings.