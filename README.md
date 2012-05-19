# Canard programming language

Canard is a concatenative language that is executed from right to left. It provides modular memory management, thread-local heaps, and supports IPC through message passing. The language
compiles to x86-64 machine code and POSIX C, and contains subsets that can be compiled to Javascript and Java source.

## Lists

Canard, like Joy, supports lists using [] notation. Constructing a list quotes its contents, so `[foo]` is a list of a symbol whereas `foo` is a function call. Like Gnarly, lists are consed
backwards; [foo bar bif] is consed as (((nil : foo) : bar) : bif), and 'bif' is the head. Where possible, Canard encodes tail calls as appropriate; that is, (nil : x) results in `x` being
encoded as a tail call.

Lists are also used to represent both the data and return stacks. Each stack is made up of immutable cons cells, and cells are garbage-collected rather than being freed in stack order. This
makes continuations into first-class objects. It also allows you to treat the stack as a tree, which makes it easier to represent things like nondeterministic computation.

## Execution semantics

Canard programs begin with an empty data and an empty return list. The source file is parsed from back to front, forming the 'source list'. This source list is then executed by dereferencing
and calling the head cell, setting the return address to a closure that evaluates the tail. So, for example, `foo bar bif` is executed as `bif`, then `bar`, then `foo`. Lists and symbols are
immutable, so bracket-notation is folded into constant-time push operations at compile time. Runtime lists can be allocated using cons, written `::`.

## Syntax

The following syntactic elements are recognized:

    1. SDoc-style paragraph comments.
    2. Hash line comments.
    3. Atoms, which include any character except [, ], and whitespace. Atoms beginning with ' are quoted symbols.
    4. Lists, which begin with [ and end with ]. The empty list is legal and refers to nil. Lists are quoted until executed.

Files are parsed from left to right, but execution happens in the opposite order.

# Low-level variant

As mentioned earlier, Canard has two variants. The low-level variant is rendered as machine code or C; this allows direct access to memory and custom GC strategies. The standard library in the
low-level variant provides enough functionality to execute commands in the high-level variant, so technically the high-level variant is a subset.

## Primitives

The low-level variant provides the following predefined symbols:

    ::          cons    (x y) -> (x::y)
    :%          uncons  (x::y) -> (x y)
    :i          invoke  ([...]) -> eval(...)
    %%          dup     (x) -> (x x)
    %,          drop    (x) -> ()
    %^          lift    (xk ... x2 x1 [f] n) -> (xk ... xn+1 xn [f]:i xn-1 ... x2 x1)
    %>          self    push current data stack cell
    %*          deref   (x) -> (*x)
    %=          assign  (a v) -> (), movq v, *a