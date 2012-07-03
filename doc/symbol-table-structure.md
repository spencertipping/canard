# Symbol table structure

The symbol table should ideally be a cons-based structure so that you can
inspect it using normal list functions. Ultimately I think it's just a big
composition of matchers with these properties:

    :h symbol-table       -> the most recent matcher
    :t symbol-table       -> the symbol table prior to the last definition

Each matcher takes two continuations on the return stack, and should invoke
whichever one makes more sense:

    %rsp -> | next-matcher | return | ...

If the match is successful, it can skip the next-matcher continuation and send
its result directly to the return continuation. To do this:

    pop %rax              <- discard next-matcher continuation
    pushd result          <- push result onto data stack (important!)
    ret                   <- use return continuation

If the match is unsuccessful, the matcher should invoke the next-matcher
continuation with the string to be matched:

    pushd the-string      <- push the input string back onto the data stack
    ret                   <- use next-matcher continuation

None of this logic needs to be particularly efficient because the symbol table
will be used at most once for each symbol.