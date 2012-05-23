# Binary encoding

The reader conses up the initial program, and it needs some kind of native binary representation for cons cells and symbols. Each value with pointers needs to indicate whether or not it has
been marked in the current GC cycle. This tag information can be encoded in the low three bits of a machine word, eliminating the need for boxing in some cases. Here are the suffixes:

    000   cons (32-bit tail pointer, 32-bit head pointer)
    001   symbol/string (32-bit length, 32-bit data pointer)

The rest are available for compiler libraries to use as they would like. The special symbol 0|0|000 represents nil; you can easily determine whether you have nil by checking the zero flag.

Unlike Lisp, cons is left-associative and lists are generated in reverse. That is, [a b c] creates the structure (cons (cons (cons nil 'a) 'b) 'c), not (cons 'a (cons 'b (cons 'c nil))) as it
would in Lisp or Scheme.