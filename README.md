# Canard programming language

Canard is a concatenative language that is executed from right to left. It provides modular memory management, thread-local heaps, and supports IPC through message passing. The language is
implemented in x86-64 assembly language and contains subsets that can be compiled to Javascript and Java source.

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

As mentioned earlier, Canard has two variants. The low-level variant is rendered as assembly language; this allows direct access to memory and custom GC strategies. The standard library in the
low-level variant provides enough functionality to execute commands in the high-level variant, so technically the high-level variant is a subset.

## Primitives

The low-level variant provides the following predefined symbols:

    $          self     () -> (self)                    push interpreter context address onto data stack
    *          deref    (x) -> (*x)
    =          assign   (a v) -> (), movq v, *a
    +          add-i64  (x y) -> (x+y)
    -          neg-i64  (x) -> (-x)
    &          and-i64  (x y) -> (x&y)
    |          or-i64   (x y) -> (x|y)
    ^          xor-i64  (x y) -> (x^y)
    <<         shl-i64  (x y) -> (x<<y)
    >>         asr-i64  (x y) -> (x>>y)
    >>>        shr-i64  (x y) -> (x>>>y)
    ?          cond     (c [t] [f]) -> (t) if c != nil (0), (f) if c == nil (0)
    mmap                (size) -> (addr|error)          use the mmap() syscall to allocate memory
    munmap              (addr) -> (error)               use the munmap() syscall to free memory
    mprotect            (permissions addr) -> (error)   use mprotect() to change memory access permissions
    def                 (definition symbol) -> ()       adds a dictionary entry

All other operators are defined in terms of these. Note that the initial implementation provides no memory management; memory-managed heaps are defined by the standard library.

## Initial representation

The initial parser synchronously reads the source code, constructing a single nested list from it. Comments are erased. Each item in the source is encoded in a 128-bit cell. Each cell looks
like this:

    <32-bit code pointer> <32-bit 'immediate' data> <64 or more bits of 'other' data, stored in 64-bit chunks>

This structure is written as `code<immediate, other>`. So, for example, 'cons' is written as `cons<tail, head>`.

    1. cons<tail, head>
    2. symbol<identity, string-data>
    3. string<length, blob-data...>
    4. hash<mask, entry-count, table-pointer>
    5. hash-entry<key, value>
    6. interpreter<dict, data-stack, return-stack>
    7. integer<00000000, data>
    8. double-float<00000000, data>

The current interpreter is stored in %rbp. Other registers can be used and/or clobbered, including %rsp.