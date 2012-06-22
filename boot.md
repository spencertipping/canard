Canard bootstrap interpreter | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This file defines a binary ELF image that contains a bootstrap canard
interpreter. The implementation is heavily influenced by jonesforth, though the
compiler uses a different output format. Like jonesforth, however, %rsp is used
for the data stack and %rbp is used for the return stack.

Basic syntax used in this file is:

    1. Address assertion: @!address. Dies unless the next byte will be emitted at
       the given address. I use these to verify/document the code.
    2. Address displacement: @address. Inserts null bytes until the given address
       is reached.
    3. /n/byte - byte repeated n times.

That's about it. The rest is just binary data written verbatim in hex (no
prefix), octal (o prefix), or binary (- prefix).

# ELF header

See elf(5) for details about what this is made of.

    # Elf64_Ehdr                    # e_ident
    @!00 7f 'ELF                    #   ELF magic
    @!04 02                         #   64-bit binary
    @!05 01                         #   Two's complement little-endian
    @!06 01                         #   Current ELF version
    @!07 00                         #   System V UNIX ABI

    @!08 00                         #   ABI version
    @!09 /7/00                      # padding; end

    @!10 02 00                      # e_type    = Executable file
    @!12 3e 00                      # e_machine = x86-64
    @!14 01 00 00 00                # e_version = current version
    @!18 78 00 40 00 /4/00          # e_entry

    @!20 40 00 00 00 /4/00          # e_phoff
    @!28 00 00 00 00 /4/00          # e_shoff

    @!30 /4/00                      # e_flags
    @!34 40 00                      # e_ehsize
    @!36 38 00                      # e_phentsize
    @!38 01 00                      # e_phnum

    @!3a 0000                       # e_shentsize
    @!3c 0000                       # e_shnum
    @!3e 0000                       # e_shstrndx
    # end

    @!40

    # Elf64_Phdr (rwx image -- this contains code and definitions)
    @!40 01 00 00 00                # p_type  = PT_LOAD
         07 00 00 00                # p_flags = PT_R | PT_W | PT_X

         00 00 00 00 /4/00          # p_offset (includes ELF header)
         00 00 40 00 /4/00          # p_vaddr
         00 00 00 00 /4/00          # p_paddr
         00 04 00 00 /4/00          # p_filesz
         00 04 00 00 /4/00          # p_memsz
         00 10 00 00 /4/00          # p_align
    # end

# Canard interpreter

The interpreter structure is heavily inspired by jonesforth, but with some
significant differences as well. The biggest difference is that Canard makes
code generally isomorphic to data; therefore, the interpreter code is itself a
data structure that appears in the memory image.

Unlike jonesforth, this interpreter is indirect-threaded through the symbol
table. This allows you to use symbols in recursive definitions:

    foo [? [foo] [] > 1]

# Lists and symbols

Like Joy, Canard is made up of lists and symbols. Everything, including numbers,
is modeled this way; there is in fact no literal numeric syntax at all. Each
object is represented by the machine code that provides its runtime behavior.
There are three primary cases:

    cons x y = e8 y e9 x          <- call to y, then tail call (jump) to x
    symbol s = e9 target(s)       <- tail call to symbol definition
    nil      = c3                 <- return from subroutine

In practice, nil is rarely encoded due to this shorthand:

    cons nil x = e9 x             <- tail-call straight to x

Notice the similarity between a symbol and a thing consed to nil. This
establishes the nice equivalence:

    'foo = [foo]

And therefore we can remove a case from the interpreter, since quoting is
already handled by the list construct.

# Threading

Jonesforth uses a very minimalistic and elegant direct-threading model that ends
up saving a signficant amount of space over writing out the opcodes. Canard
takes a different approach for a couple of reasons. First, Canard is 64-bit, so
the 4-byte absolute addresses become 8-byte ones if we use a direct encoding.
Second, we can't do eager symbol resolution since we'll use (i.e. compile) some
symbols before they have been defined.

At the same time, however, we want to avoid the overhead that comes with the
second layer of indirection intrinsic to indirect threading. The easiest way to
do this, assuming that we're willing to be arbitrarily hackish (which I am), is
to simply update the calling address to remove the indirection [1]. So we end up
with code that looks like this:

    e8 xx xx xx xx                <- xx xx xx xx points to the symbol table entry
    xx xx xx xx:
      movzxl $yy yy yy yy, %eax   <- absolute address of yy yy yy yy
      subq (%rsp), %rax           <- make it relative to the calling address
      movl %eax, -4(%rsp)         <- go back and fix up the address
      jmp -5(%rsp)                <- and use the new and improved jump

This has an interesting effect on the resulting code in the code-as-data model.
The symbols will end up being erased and lists will refer back to each other
directly rather than going through the symbol table:

    fact [? [* fact - 1 %0] [1 %v] %0]
         ^     v
         +--<--+

Notice that we've now lost information [2]: we don't know why the list refers to
itself, just that it does. We arguably shouldn't make this transformation
lightly, since the user will see the difference between the initial abstract
list and the resulting resolved one. This transformation becomes particularly
apparent when taken to its logical limit: every primitive operation will end up
being replaced by its definition until the whole list is rewritten into fully
executable machine code and will contain no intermediate symbol indirection at
all.

Note that this has the added benefit that tail calls propagate through symbol
dereferencing. (I think this is true; need to verify in all cases.)

    [1] This could be a problem because it makes it impractical to generalize the
        calling convention to any form of CPS. (Specifically, you can't invoke a
        piece of code with a different continuation without seriously borking
        things.) I'm going to need to put a lot of thought into this...

    [2] Technically, the information is still there, though only due to a
        counterintuitive property of the mechanism. Lists preserve their identity
        across this transformation and we know that every list might have an
        associated symbol. Therefore, we can easily enough go backwards from
        list-address to symbol name in the event that lists must be printed. Code
        that inspects the lists will still need to manually invert the symbol
        lookup.

# Symbols

The symbol table contains executable code that looks up symbols, returning the
default 'define-this-symbol' function if the mapping doesn't exist. The initial
symbol table is a part of this bootstrap image and is one of the few things
about Canard that isn't written in the language itself.

Once defined, symbols are fairly straightforward. The symbol table maps each
symbol to a caller-modifying function as described above.

    @!78 4831 o300 b03c             # syscall = 60 (exit)
         4831 o355                  # status  = 0
         0f05