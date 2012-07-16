# Matchers

The global symbol table is a first-class function that resolves symbols by
composing matchers. Because cons is composition, you can manipulate the symbol
table as a list. Therefore:

    symbol-table = :: ... :: :: :: [] m1 m2 m3 ... mn

Each matcher function takes two continuations on the return stack. The top
continuation takes the original symbol and indicates that no match occurred; the
next continuation takes the resolved value and returns immediately to the symbol
table caller.