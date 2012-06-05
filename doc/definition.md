# Symbol definition

Canard gives you two ways to quote things. The first and most obvious is just to put something into a list. For instance, to alias the $< function:

    = 'print [$<]

The other is to quote a symbol:

    = 'print '$<                  <- but this might not work

These two definitions have different meanings:

    [$<] = a function that invokes $< and then returns
    '$<  = the address of the $< function

The problem with = 'print '$< is that after this, the symbol 'print is equivalent to the symbol '$<. This could be a big problem when thinking about quoted lists:

    == head [print] head [$<]     <- returns 1

Maybe the right answer is to just disable aliasing altogether...