Canard bootstrap interpreter | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This file is written in preprocessed binary text, a format defined by the
'binary' and 'preprocessor' self-modifying Perl objects. You can get these
objects from http://github.com/spencertipping/perl-objects; the HTML files allow
you to inspect them online:

    http://spencertipping.com/perl-objects/binary.html
    http://spencertipping.com/perl-objects/preprocessor.html

## Conventions

The most consistent convention I use is encoding the ModR/M and SIB bytes in
octal rather than hex. This better reflects their structure; so, for instance:

    movq %rax, %rbx             # copy %rax into %rbx
    488b o330                   # clearer than 488b d8
     | |  |||
     | |  ||+-- r/m = 000, %rax
     | |  |+--- reg = 011, %rbx
     | |  +---- mod = 11, reg/reg
     | +------- opcode = 8b (move right -> left)
     +--------- REX.W prefix (0100 = REX, 1000 = WRXB)

I avoid using any prefix aside from REX.W, which is encoded as 48 and is a
fairly good giveaway that something is an opcode. (I also use 66 as needed.)

## Constants

These dictate where the image goes and how big it is in memory. This is
related to virtual addressing.

    :[::image_base = 0x400000]
    :[::image_size = 0x100000]
    :[sub b { shift(@_) + :image_base }]

    :[::image_end  = b:image_size]

# ELF header

See elf(5) for details about what this is made of.

    ::bootstrap_begin

    ::elf_ehdr_begin                # e_ident array
      ::elf_e_ident    7f 'ELF      # ELF magic
      ::elf_ei_class   02           # 64-bit binary
      ::elf_ei_data    01           # Two's complement little-endian
      ::elf_ei_version 01           # Current ELF version
      ::elf_ei_osabi   00           # System V UNIX ABI

      ::elf_ei_abiversion 00        # ABI version
      ::elf_ei_padding /7/00        # padding; end of e_ident

      ::elf_e_type      :2[L 2]     # Executable file
      ::elf_e_machine   :2[L 0x3e]  # x86-64
      ::elf_e_version   :4[L 1]     # current version

      ::elf_e_entry     :8[L b:entry]

      ::elf_e_phoff     :8[L :phdr_begin]
      ::elf_e_shoff     :8[L 0]     # no section headers

      ::elf_e_flags     :4[L 0]
      ::elf_e_ehsize    :2[L :elf_ehdr_end - :elf_ehdr_begin]
      ::elf_e_phentsize :2[L :phdr_end     - :phdr_begin]
      ::elf_e_phnum     :2[L 1]

      ::elf_e_shentsize :2[L 0]
      ::elf_e_shnum     :2[L 0]
      ::elf_e_shstrndx  :2[L 0]
    ::elf_ehdr_end

# Program header for main image

This section is read/write/execute and contains the bootstrap code, data stack,
and heap. It doesn't contain the return stack since we use the OS-provided one.

    :[::bootstrap_size = :bootstrap_end - :bootstrap_begin]

    ::phdr_begin
      ::phdr_p_type   :4[L 1]         # PT_LOAD
      ::phdr_p_flags  :4[L 4 | 2 | 1] # PT_R | PT_W | PT_X
      ::phdr_p_offset :8[L 0]
      ::phdr_p_vaddr  :8[L :image_base]
      ::phdr_p_paddr  :8[L 0]
      ::phdr_p_filesz :8[L :bootstrap_size]
      ::phdr_p_memsz  :8[L :image_size]
      ::phdr_p_align  :8[L 0x1000]
    ::phdr_end

# Core image layout

The core image consists of two stacks and a heap. The stacks go in opposite
directions; the return stack is provided by the host operating system and uses
the processor's native push/pop instructions. The data stack grows forwards from
the first free address mapped into the interpreter's address space. The commands
used to manipulate the data stack are inlined into use sites.

The heap grows downwards and is used for allocating cons cells and other
contiguous blocks of memory. Register mapping is:

    %rsp (register 4) return stack pointer (grows downwards)
    %rdi (register 7) data stack pointer (grows upwards towards %rsi)
    %rsi (register 6) heap pointer (grows downwards towards %rdi)

All other general-purpose registers are available for clobbering. The choices
for the above registers are not arbitrary; they are chosen to minimize the
number of spurious SIB bytes and displacements required to access memory. (See
the Intel Instruction Set Reference for the machine-level exception cases for
various registers.)

At the top (high addresses) of the heap is the symbol table, which consists of
heap-allocated cons cells as more definitions occur. When Canard boots up, it
immediately invokes the 'main' symbol; this function contains logic for the
interpreter/REPL. The original value of %rsp (the one that the OS provided) is
pushed onto the data stack prior to invoking 'main'.

## Global state

The interpreter has one global variable aside from the stack/heap registers.
This variable is the symbol table, which is used by the bootstrap interpreter
to resolve any symbols entered by the user.

    ::symbol_table :8[L 0]

## Symbol constants

We refer to some constants throughout the code below, most of which are
symbols. The bootstrap image is smaller if we pull them out of the code
itself, since this way we won't have to jump around them or write the
constants as the immediate data of a mov instruction.

This constant table is used two ways. The first way is as a space for us to
use later on when grabbing symbol references -- for example, when we push the
'main' symbol onto the stack in the toplevel code.

The second way is as an initial binding table. The toplevel bootstrap code
reads through this table and conses up bindings before executing main. All of
these are offset-encoded from the beginning of the file to save space; this
way we can get away with using only two bytes instead of three or four.

Code labels are prefixed with a /; symbol labels are prefixed with @. This
allows us to duplicate the names and factor the binding table generation into
a preprocessor macro. (It also makes it more obvious when a label is used as a
symbol binding.)

    ::binding_table
    - def -p binding << end
      :2[L:/$_] ::\@$_ @{[sprintf "%02x", length $_]}00'$_
    - end

    - binding main, /:k, @:k, @?k, @<, @>, r<, r>, @<x, @<o, !<
    - binding %x, %v, %0, %1
    - binding i+, i&, i|, i^, i<<, i>>, i>>>, i-, i~
    - binding ., ?
    - binding =1, =2, =4, =8, @1, @2, @4, @8, &0, &1, &2, &3
    - binding &read, &write, &open, &close, &stat

The binary-text compiler can't handle a label called /:: -- the two colons
make it think it's supposed to use a label reference. It also fails for things
containing single-quotes.

    :2[L:/@<q]     ::@<q       0300'@<'
    :2[L:cons]     ::@cons     0200'::
    :2[L:swons]    ::@swons    0300':$:
    :2[L:allocate] ::@allocate 0200':v
    :2[L:uncons]   ::@uncons   0200':^

    /2/00         # end marker
    ::end_binding_table

## Register initialization

Set up these registers to point to useful places. %rsp is provided by the OS,
so we don't need to do anything else with it. We set up %rdi and %rsi like
this:

    +-- 400000   +-- 0x4000c9     +-- 40xxxx            +-- 500000
    V            V                V                     V
    | ELF header | bootstrap code | data stack ... heap | .... | %rsp stack

I'm leaving the data stack lower bound as a variable here because it depends
on the amount of bootstrap code that we use. But the idea is the same either
way; the data stack begins immediately after the bootstrap logic.

    :[::file_end = b:bootstrap_size]

    ::entry
    ::rdi_init 48c7 o307:4[L:file_end]
    ::rsi_init 48c7 o306:4[L:image_end]

At this point the registers are initialized, so we can write to both stacks
and to the heap. We need to set up a 'return' into the exit function so that
when the main function exits (i.e. we hit a nil at the end of the main program
list) it will die gracefully.

Two things need to happen here. First, we need to put the system-provided %rsp
value onto the data stack; this lets 'main' inspect argv and environment
variables. Then we need to push 'exit' as the continuation for 'main'.

    ::push_rsp    488bo304 48ab
    ::init_return 68:4[Lb:/&exit]

# Symbol table

There are several pieces of machinery involved in creating the symbol table.
First, the symbol table is ultimately a function composition, which is
represented in Canard as a linked list of cons cells. So the general form is
something like this:

    symbol_table: 00000000ssssssss
    ssssssss: e8 xxxxxxxx | e8 yyyyyyyy | e9 zzzzzzzz
    zzzzzzzz: e8 qqqqqqqq | ... | c3

Specifically, when executed it will use an x86 CALL to each entry, and when
examined as a cons tree it will give you each symbol resolver directly. At the
very end is a RET instruction; the result of this is that if no resolver can
handle the symbol you pass in, the symbol table just hands you your symbol back.
In list form, this corresponds to a nil as the final tail.

    e8:4[L:/nil - :>]

At this point, the heap looks like this:

    %rsi -> | c3 | :image_end

This is necessary for the initialization we're going to do below.

## Undefined symbol handlers

These handle undefined symbols and construct symbol-specific functions for
quoted or literal constructs. These can go in basically any order.

    b8:4[Lb:/@<x] 48ab e8:4[L:cons - :>]  # Hexadecimal literals
    b8:4[Lb:/@<o] 48ab e8:4[L:cons - :>]  # Octal literals
    b8:4[Lb:/@<q] 48ab e8:4[L:cons - :>]  # Quoted literals

## Default bindings

Set up a few bindings by default. These comprise the bootstrap image. The
definitions themselves are below.

Right now the symbol table as such doesn't exist; we need to build it by
consing up a bunch of symbol matchers, each of which is generated by one of
the functions below. These functions will then be added to the symbol table
and will be part of the image.

Entries in the binding table are formatted like this:

    [16-bit value] | [16-bit symbol length] | [symbol data]

The last entry has a zero value and no following symbol. To parse this out, we
read the values into %rbx and then point %rax at the symbol length. If we have
a value, push the value and the symbol pointer (which is simply the integer
value of %rax).

    ::read_binding_table
    48c7 o305:4[Lb:binding_table]

    ::rbt_loop
    480fb7 o13500                         # %rbx = Z< 16-byte (%rbp)
    488d   o10502                         # %rax = %rbp + 2
    480fb7 o010                           # %rcx = Z< 16-byte (%rax)
    6685   o311 74:1[L:rbt_loop_end - :>] # break if value == 0

    4893 05:4[L:image_base] 48ab          # push %rbx + image_base (value)
    4893 48ab                             # push %rax (= %rbp + 2)

    488d o154o015 02                      # %rbp += %rcx + 2
    55 e8:4[L:/@:k - :>]                  # stash %rbp
       e8:4[L:cons - :>] 5d               # restore %rbp

    eb:1[L:rbt_loop - :>]
    ::rbt_loop_end

Now we have a complete symbol table. Bind it to the global address and start
up the interpreter.

    488b o137f8                           # copy the symbol table to %rbx
    e8:4[L:/@< - :>]                      # and install it globally

Push the 'main' function and use the symbol table to resolve it to a code
address. Once we have it, tail-call into the 'main' function and let it return
into 'exit'. (main needs to push an exit code for this to work.)

    ::push_main b8:4[Lb:@main] 48ab       # push symbol 'main'
    ff   o323                             # resolve 'main' (using table in %rbx)
    488b o107f8 4883 o357 08              # data-pop %rax (address of main)
    ff   o340                             # tail-jump into main

And that concludes the bootstrap logic. Everything below here is a library
function that is referenced statically from the above.

# Internals

Everything from here down is referenced or called by the above to build out the
image, but is not executed in the toplevel stream.

## Main function

This will end up tying together a number of pieces, but for the moment it just
pushes a zero to return.

    ::/main
    4831o300 48ab                         # data-push 0
    c3                                    # return (to exit function)

# Consing and memory allocation

We need a way to allocate new cons cells without manually getting memory from
the heap. To do this, we define the cons function, which will ultimately be
stored in the symbol table as ::.

Right now we have some leftover stuff on the stack. The data stack top is the
symbol matcher for @:k, which we'll ultimately need to cons onto the nil we put
into a single byte on the heap.

## Nil

This is easy; it's just a single byte on the heap. We then push a pointer to
that byte onto the data stack.

    ::/nil
    48ffo316                              # allocate one byte for nil
    c6  o006 c3                           # move the byte c3 to this address
    488bo306 48ab                         # push the c3 reference onto the stack
    c3                                    # return

## Cons definition

As described in the design documentation, cons has a few different cases that
it knows how to deal with. If the tail matches the current heap pointer, we
can omit the e9 jump and just rely on sequence to execute the tail as-is,
saving five bytes. Otherwise we write the e9 jump.

Cons takes the head on top of the stack and the tail beneath that. This is
counterintuitive from code, as arguments appear to be in the wrong order:

    :: 3 []             <- [3]
    :: [] 3             <- improper list

However, generally it's more useful to access the head of a list than the
tail, so we want it to be immediately available.

There are cases where you want the other ordering; for that you can use
'swons' ('swap cons'), which is identical to 'cons' except that its arguments
are reversed:

    :$: 3 []            <- improper list
    :$: [] 3            <- [3]

Given all of that, here's the algorithm for both cons and swons (referred to
as :: and :$: respectively):

    swons:
      data-pop %rbx             <- tail
      data-pop %rax             <- head
      jmp cons_body             <- reuse logic below
    cons:
      data-pop %rax             <- head
      data-pop %rbx             <- tail
    cons_body:
      cmpq %rbx, %rsi           <- check for contiguous segment
    je cons_head                <- skip tail allocation if contiguous
      subq %rsi, %rbx           <- absolute->relative
      movl %ebx, -4(%rsi)       <- write address of e9 jump instruction
      subq $5, %rsi             <- reserve space
      movb $0xe9, (%rsi)        <- write e9 jmp opcode
    cons_head:
      subq %rsi, %rax           <- absolute->relative
      movl %eax, -4(%rsi)       <- write address of e8 call instruction
      subq $5, %rsi             <- reserve space
      movb $0xe8, (%rsi)        <- write e8 call opcode
    data-push %rsi              <- return reference to this cons cell
    ret

Some optimization has been done around the stack parameters. In particular,
since we end up pushing the result, we can just pop one instead of popping
both; then we can write the result over the parameter that we didn't pop.

    ::swons
    488b o137f8                           # tail element
    488b o107f0                           # head element
    eb:1[L:cons_body - :>]                # reuse code below

    ::cons
    488b o107f8                           # head element
    488b o137f0                           # tail element

    ::cons_body
    4883 o357 08                          # pop one
    4839 o336 74:1[L:cons_head - :>]      # tail allocation check
    482b o336 89 o136fc                   # relative, write e9 jump offset
    4883 o356 05 c6 o006 e9               # allocate space, write e9 jump opcode

    ::cons_head
    482b o306 89 o106fc                   # relative, write e8 call offset
    4883 o356 05 c6 o006 e8               # allocate space, write e8 call opcode

    4889 o167f8 c3                        # return new cons cell

## Jump dereferencing

Cons tails are encoded as e9 jumps. This function follows an e9 jump until the
destination is some other opcode. If a circular path of e9 jumps is
constructed, this function will never return.

    ::/!<
    488b o107f8                           # load initial value into %rax

    ::/!<_loop
    80   o070 e9                          # is e9 the byte at that location?
    75:1[L:/!<_bail - :>]                 # it isn't, so we're done
    4863 o13701                           # sign-extend address into %rbx
    488d o104o00205                       # %rax += %rbx + 5
    eb:1[L:/!<_loop - :>]                 # dereference this pointer

    ::/!<_bail
    4889 o107f8 c3                        # %rax -> stack top; return

## Unconsing

This is quite straightforward; we just need to decode the relative addresses.
Fortunately, as demonstrated above, this can be done in two instructions using
movsx and lea.

This function has the following behavior:

    :^ :: a b = a b             <- :^ applied to cons cell
    :^ x      = [] x            <- :^ applied to some other value

Note that this will be misleading for the nil value, since it will appear as
though nil is infinitely nested in itself. It's written this way to reduce the
number of cases in code dealing with lists, and to provide the equivalence
that [:: :^] is a list monad 'return' function.

    ::uncons
    e8:4[L:/!< - :>]                      # dereference automatically
    488b o107f8                           # data-pop %rax

    80 o070 e8                            # do we have a real cons cell?
    75:1[L:uncons_nil - :>]               # if not, jump to the nil case

    4863 o13701                           # sign-extend address into %rbx
    4883 o300 05                          # %rax += 5    (move past e8 call)
    488d o034o002                         # %rbx += %rax (absolute -> relative)
    4889 o137f8                           # data-push %rbx (head)
    48ab                                  # data-push %rax (tail)
    c3

    ::uncons_nil                          # this section is a tail call
    e9:4[L:/nil - :>]                     # just push a nil as the tail

## Heap allocation

Allocates the given number of bytes onto the heap and returns a pointer to
them. Use this carefully; if you allocate too much stuff you'll make the heap
collide with the data stack and terrible things will happen.

    ::allocate
    482b o167f8                           # %rsi -= data-pop
    4889 o167f8 c3                        # data-push heap; ret

## Low-level memory access

This is necessary to define a number of things in the standard library. Memory
can be read and written in a few different sizes, and the processor's
endianness is used. (This is relevant since all stack values are the same
width -- so for small reads/writes, you're working with a byte slice.)

    ::/@1 488b o137f8 480fb6 o003 4889 o107f8 c3
    ::/@2 488b o137f8 480fb7 o003 4889 o107f8 c3
    ::/@4 488b o137f8     8b o003 4889 o107f8 c3
    ::/@8 488b o137f8   488b o003 4889 o107f8 c3

Writes take the address on the top of the stack, followed by the value. We use
%rax for the value and %rbx for the address.

    ::/=1 488b o137f8 488b o107f0 4883 o357 10   88 o003 c3
    ::/=2 488b o137f8 488b o107f0 4883 o357 10 6689 o003 c3
    ::/=4 488b o137f8 488b o107f0 4883 o357 10   89 o003 c3
    ::/=8 488b o137f8 488b o107f0 4883 o357 10 4889 o003 c3

## Return stack manipulation

These two functions allow you to move values between the data and return
stacks. r> pulls from the return stack, r< pushes onto it.

Each of these uses a nonstandard return operator because it needs to ignore
its own immediate return address. However, this doesn't impact the calling
convention.

    ::/r> 59 58 48ab ff o051
    ::/r< 59 488b o107f8 4883 o357 08 50 ff o051

## Trivial stack permutation

I don't want to do too much here because it turns out that there is a nice way
to encode stack permutation in terms of numerical digits; we're better off
JIT-compiling the specific transformations we need rather than relying on
compositions of basic ones like these.

However, in the interests of getting a bootstrap compiler off the ground, here
are a few:

    ::/%x                                 # Swap two entries
    488b o107f8                           # first entry -> %rax
    4887 o107f0                           # %rax <-> next entry
    4889 o107f8 c3                        # rewrite first entry

    ::/%v                                 # Drop one entry
    4883 o357 08 c3

    ::/%0                                 # Duplicate top entry
    488b o107f8 48ab c3

    ::/%1                                 # %1 a b ... = b a b ...
    488b o107f0 48ab c3

    ::/%r                                 # %r a b c ... = c a b ...
    488b o107f8                           # a -> %rax
    4887 o107f0                           # *b <-> %rax
    4887 o107e8                           # *c <-> %rax
    4889 o107f8 c3                        # rewrite *a; return

    ::/%R                                 # %R a b c ... = b c a ...
    488b o107e8                           # c -> %rax
    4887 o107f0                           # *b <-> %rax
    4887 o107f8                           # *a <-> %rax
    4889 o107e8 c3                        # rewrite *c; return

## Arithmetic

A few functions to make it possible to do basic arithmetic on stack entries.

    # Binary op: load %rax from stack, use entry below as lvalue
    ::/i+   488b o107f8 4801 o107f0 4883 o357 08 c3
    ::/i&   488b o107f8 4821 o107f0 4883 o357 08 c3
    ::/i|   488b o107f8 4809 o107f0 4883 o357 08 c3
    ::/i^   488b o107f8 4831 o107f0 4883 o357 08 c3

    # Shifts: load %rcx from stack (shift amount), entry below is lvalue
    ::/i<<  488b o117f8 48d3 o147f0 488c o357 08 c3
    ::/i>>  488b o117f8 48d3 o177f0 488c o357 08 c3
    ::/i>>> 488b o117f8 48d3 o157f0 488c o357 08 c3

    # Unary: operate on stack top in-place
    ::/i-   48f7 o137 f8 c3
    ::/i~   48f7 o127 f8 c3

## Evaluation

There are two ways to evaluate things. One is to use the . operator, which
evaluates the thing on the top of the stack (and segfaults if the stack top
doesn't point to a valid list). The other is to use the ? operator, which
chooses between two conditions. Here are the equations:

    . [x] y ...       = x y ...
    ? [t] [f] 0 x ... = f x ...
    ? [t] [f] N x ... = t x ...         <- N != 0

First the . function. This is really simple: all we need to do is jump into
the destination since lists are executable. This can be a tail call, so the
'return' that we would expect to see inside the . function is actually
provided by the function it's calling.

    ::/. 488b o107f8 4883 o357 08 ff o340

Now the decisional function. This is a value-oriented form of if/then/else. We
optimistically load the true-case into a register, then load the false case if
the condition is zero. Like . above, this function also tail-calls the branch.

    ::/?
    488b o107f8                           # true branch -> %rax
    488b o137e8                           # condition -> %rbx
    4883 o357 18                          # pop three entries

    4885 o333 480f44 o10708               # false branch -> %rax if cond == 0
    ff   o340                             # jmp *%rax

# Closures

Canard doesn't have closures in the same sense that Lisp or Haskell does, but
you can construct functions that push specific values onto the stack so that the
next function has something to work with. For instance, consider something like
this:

    adder x y = x + y
    f = adder 5
    f 10                          <- returns 15

We can define adder like this in Canard:

    = [adder] [+ /:k]             <- quotation here; we want composition
    = [f] adder 5                 <- notice: no quotation!
    f 10                          <- returns 15

The key is the /:k function, which is analogous to the 'k' combinator in
functional programming. For any value, /:k returns a function that pushes that
value each time it is called.

## Constant function

This is fairly simple. Values are represented as 64-bit numbers internally, so
we just need to write a function that pushes a fixed number onto the stack.
The processor supports a 64-bit move-immediate into %rax using the 48b8
operation, so the generated code will look something like this:

    48b8 xxxxxxxx xxxxxxxx      <- value to be pushed
    48ab c3                     <- push and return

This requires 13 bytes of heap space, which we can obtain by subtracting
directly from %rsi.

    :://:k
    488b o107f8                   # data-pop into %rax
    4883 o356 0d                  # allocate heap space
    66c7 o006 48b8                # write 48b8 instruction at (%rsi)
      c7 o10609 0048abc3          # write 48ab c3 sequence at 9(%rsi)
    4889 o10602                   # write value to be pushed
    4889 o167f8                   # data-push %rsi
    c3

Notice that we're writing the end opcodes first. Doing it this way lets us use
a wider 4-byte mov, which makes the code smaller and decreases the instruction
count. However, in doing this we also overwrite the last byte of the value
that we want to push; so we wait to write the value until after we've written
all of the opcodes.

# Definition and invocation

This is the last piece of the symbol table. Remember from earlier that the
symbol table pointer is mutable and is stored at a fixed address. The simplest
way to deal with this value in memory is just to build two special-purpose
functions that read or write it.

## Symbol table getter

We refer to this as @> from now on. This function pushes the symbol table
address onto the data stack.

    ::/@>
    488b o005:4[L:symbol_table - :>] 48ab c3      # st -> %rax; push; ret

## Symbol table setter

This function is only slightly more complex than @> due to the fact that we
don't have a shortcut quite as nice as stosq. The setter is called @<.

    ::/@<
    488b o107f8 4883 o357 08                      # data-pop -> %rax;
    4889 o005:4[L:symbol_table - :>] c3           # %rax -> (symbol_table); ret

## Matchers and symbol resolution

Each function in the symbol table composition is a matcher. This means that it
uses a calling convention that allows it to operate in conjunction with other
matchers in the symbol table. The calling convention is a consequence of the
way the symbol table works. For example, here's a matching process:

    call resolve_symbol                 <- %rsp is now 'return'
    resolve_symbol:
      call first_matcher                <- %rsp is now 'next'
      call second_matcher               <- %rsp is now 'next'
      ...
      ret                               <- %rsp is now 'no match'

Matchers are always run in order like this, and their return values are not
checked by any conditional. It would appear as though we need to thread some
sort of state through the rest of the calls to communicate when a matcher has
succeeded. This is not the case, however. Matchers can elect to return early
by popping a continuation off of %rsp and then using a RET instruction to
return directly to the caller of resolve_symbol:

    first_matcher:
      ...
      stosq                             <- push result onto data stack
      pop %rax                          <- discard 'next' continuation
      ret                               <- invoke 'return' continuation

If a matcher fails, it should leave the data stack alone and invoke the 'next'
continuation as if nothing had happened:

    first_matcher:
      ...
      ret                               <- invoke 'next' continuation

Philosophically this is sort of like having the functions take multiple
continuations as parameters, with the convention that continuations are passed
on the return stack instead of the data stack. [TODO: Figure out whether this
is maintainable given that r> and r< are used to stash data items...]

## Constant symbol matcher generator

This function takes a symbol and an address on the stack and returns a pointer
to a matcher that matches the symbol and returns the address. If the match
fails, the matcher invokes the 'next' continuation as described above.

It turns out that this is quite easy given that we've defined /:k already. All
we need to do is construct two cons cells, one for each argument to @?k. We're
basically building up this list:

    :: (/:k value) :: (/:k symbol) [@?k]

The symbol is on the top of the stack, the value beneath it. @:k consumes both
and returns a single matcher function.

Here's an initial implementation:

    ::/@:k
    e8:4[L://:k  - :>]                  # symbol -> k symbol
    e8:4[L:/%x   - :>]                  # swap
    e8:4[L://:k  - :>]                  # value -> k value
    48c7 o300 :4[Lb:/@?k] 48ab          # push reference to @?k
    e8:4[L:swons - :>]                  # first composition
    e8:4[L:swons - :>]                  # final composition
    c3

This implementation looks suspiciously similar to a list. The only exceptional
piece is the reference to @?k, but conveniently can use /:k to do this for us.
If we were going to build @:k in code, it would look roughly like this:

    = [@:k] :++ :$: [:: /:k /%x :$:] /:k '@?k [/:k]

Here's a preprocessor macro to help create code that consists only of function
calls:

    - def -p composition << end
      e8:4[L:$_ - :>]
    - end

The only thing is that you'll have to add the final return:

    - composition /x, /y, /z
    c3                          <- don't forget this!

## Constant matcher definition

The constant matcher just compares bytes within a contiguous region of memory.
It takes two symbols and a binding address as data stack arguments; the
binding address will be sent to the second continuation if the two symbols
match; otherwise the immediate continuation will be used and the binding
address and one of the symbols will be popped from the data stack. (This is
basically how it needs to work in order to adhere to the calling convention
for symbol table matchers.)

Here's the logic:

    @?k:
      data-pop %rbp                     <- binding address
      data-pop %rax                     <- first symbol
      movq -8(%rdi), %rbx               <- second symbol (peek, not pop)
      xorq %rcx, %rcx                   <- clear high bits
      movw (%rax), %cx                  <- get length
      cmpw %cx, (%rbx)                  <- both same length?
      jne bail                          <- if not, bail
    length_ok:
      movb 2(%rbx,%rcx,1), %dl
      cmpb %dl, 2(%rax,%rcx,1)
      jne bail                          <- mismatched character
      loop                              <- else do the next one
    success:
      movq %rbp, -8(%rdi)               <- set data stack result
      pop %rax                          <- drop 'next' continuation
    bail:                               <- success falls through
      ret                               <- invoke 'return' continuation

This function ends up being bound as @?k in the symbol table. We bind this
once we've defined cons and bind below.

    ::/@?k
    488b o157f8                   # -8(%rdi) -> %rbp
    488b o107f0                   # -16(%rdi) -> %rax
    488b o137e8                   # -24(%rdi) -> %rbx
    4883 o357 10                  # %rdi -= 16 (pop two entries)

    4831o311 668b o010            # %rcx = length
    6639 o013                     # length check
    75:1[L:/@?k_bail - :>]

    ::/@?k_loop
    8a o124o01302                 # top of loop: populate %dl
    38 o124o01002                 # compare characters
    75:1[L:/@?k_bail - :>]        # bail if not equal
    e2:1[L:/@?k_loop - :>]        # loop if more characters (%cx != 0)

    4889 o157f8                   # %rbp -> -8(%rdi)
    58                            # pop

    ::/@?k_bail                   # fall through either way
    c3                            # invoke return or next continuation

Here's @:k. I chose to write the closure thunk manually just to get things
done; otherwise it would have been allocated using /:k and consed into @:k
using the real cons function. (However, @:k is a valid list anyway.)

Also, this closure is slightly smaller than the one that would have been
written by /:k. (Maybe I should optimize /:k for cases like these...)

    ::/@:k-closure
    c7 o300:4[Lb:/@?k] 48ab c3

    ::/@:k
    - composition //:k, /%x, //:k, /@:k-closure, swons, swons
    c3

## Numeric symbol parsers

We want to generate number-pushing constant functions for any numeric symbol
we observe. It isn't as simple as looking for strings of digits, since these
are valid identifiers.

What we really want to do is use some kind of prefix that is unique to numeric
symbols in various bases. Canard does not assume decimal at all; in fact, it
doesn't even support literal decimal numbers. Here are the prefixes:

    x[0-9a-f]+                  <- hexadecimal number
    o[0-7]+                     <- octal number

Numbers don't have intrinsic size beyond the size of a stack cell, which for
this implementation is 64 bits. Also, no floating-point numbers are supported
yet.

Because these functions are symbol resolvers, they use the usual calling
convention. However, instead of returning a binding that they were preloaded
with (which is what functions generated by @:k do), they allocate a new
constant function for each literal number observed. No attempt is made to
cache previously used number functions even though doing this would save
memory.

The code-generation step is trivial because we have /:k. We just take the
parsed number, push it onto the stack, call /:k, and return a reference to the
resulting closure. The result is a function that pushes the number whenever it
is invoked.

Each digit is checked for membership within two ranges. The first is 0-9,
which is ASCII 0x30 - 0x39. The second is a-f (lowercase!), which is ASCII
0x61 - 0x66.

    ::/@<x                        # Resolve hexadecimal number
    488b o137f8                   # data-pop -> %rbx
    4831 o300 8b o310 8b o320     # %rdx = %rcx = %rax = 0
    668b o013                     # symbol length -> %cx
    ff   o311                     # --%cx (adjusting for the 'x' prefix)
    4883 o303 03                  # %rbx += 3 (length + 'x' prefix)

    80 o073 'x                    # is the prefix x?
    75:1[L:/@<x_bail - :>]        # if not, bail; we can't convert this symbol

    ::/@<x_digit_loop
    88 o003                       # current digit byte -> %al
    3c 'a                         # check this byte against lowercase A
    7d:1[L:/@<x_letter - :>]      # greater? if so, go to letter case
    2c '0                         # digit case: subtract '0 offset
    eb:1[L:/@<x_shift_add - :>]   # go to shift/add section
    ::/@<x_letter
    2c 57                         # letter case: subtract (0x61 - 10 = 0x57)

    ::/@<x_shift_add
    48c1 o342 04                  # %rdx <<= 4
    480b o302                     # %rdx |= %rax
    48ff o303                     # ++%rbx
    e2:1[L:/@<x_digit_loop - :>]  # loop while --%rcx

    4889 o127f8                   # %rdx -> stack top
    e8:4[L://:k]                  # create closure
    58                            # pop 'next' continuation

    ::/@<x_bail
    c3                            # end of @<x

The function for octal is similar but much simpler, since we don't have to
deal with the piecewise nature of digits vs letters.

    ::/@<o                        # Resolve octal number
    488b o137f8                   # data-pop -> %rbx
    4831 o300 8b o310 8b o320     # %rdx = %rcx = %rax = 0
    668b o013                     # symbol length -> %cx
    ff   o311                     # --%cx (adjusting for the 'o' prefix)
    4883 o303 03                  # %rbx += 3 (length + 'o' prefix)

    80 o073 'o                    # is the prefix o?
    75:1[L:/@<o_bail - :>]        # if not, bail; we can't convert this symbol

    ::/@<o_digit_loop
    88 o003                       # current digit byte -> %al
    2c '0                         # digit case: subtract '0 offset
    48c1 o342 03                  # %rdx <<= 3
    480b o302                     # %rdx |= %rax
    48ff o303                     # ++%rbx
    e2:1[L:/@<o_digit_loop - :>]  # loop while --%rcx

    4889 o127f8                   # %rdx -> stack top
    e8:4[L://:k]                  # create closure
    58                            # pop 'next' continuation

    ::/@<o_bail
    c3                            # end of @<o

## Quoted symbol parser

If we observe a symbol that begins with ', we push a closure that pushes a
reference to that symbol. This greatly simplifies the process of defining new
symbols. (The alternative would be to use the quotation property of lists to
say something like :h [symbol].)

Like /@<x above, this function allocates a new closure for every symbol it
resolves.

Note that this function destroys the symbols it converts. Specifically, it
removes the leading ' and moves the length. Here's the specific
transformation:

    +--- original symbol pointer
    |
    06 00  ' hello                <- original
    06 05 00 hello                <- converted
       |
       +--- new symbol pointer

    ::/@<q                        # Resolve quoted symbol
    488b o107f8                   # data-pop -> %rax

    80 o17002 ''                  # is the prefix '?
    75:1[L:/@<q_bail - :>]        # if not, bail; we can't convert this symbol

    4831 o311                     # %rcx = 0
    668b o010 ff o311             # %cx = length - 1
    6689 o11001                   # write new length
    48ff o300                     # ++%rax
    4889 o107f8                   # %rax -> stack top
    e8:4[L://:k]                  # create closure
    58                            # pop 'next' continuation

    ::/@<q_bail
    c3

# Reader

Canard source is parsed by a mechanism similar to Lisp's reader. The primary
difference is that the Canard reader is much simpler and is not extensible by
mechanisms like reader macros. It recognizes the following constructs:

    1. Lists, which are contained within square brackets.
    2. Symbols, which are comprised of any non-whitespace, non-bracket character.
    3. Quoted nil, written as [].

UTF-8 is handled transparently as a symbol character, since any UTF-8 byte is
non-whitespace and non-bracket.

## Continuations and partial parsing

The reader is not resumable -- that is, you can't hand it a chunk of input,
say 'read this', and then give it the rest of the input later on. The reason
is that doing this well means that you could reuse a state, which in turn
means that Canard needs some form of built-in alias detection to minimize
garbage.

This, in turn, implies that garbage collection should be present in the
bootstrap image, which is not where it belongs. So the reader's state is
linearized by wrapping the reader function around the character-retrieval
function, making the interaction non-replayable.

## Character retrieval

Like jonesforth, Canard uses a small buffer and the read() syscall to populate
it. Unlike jonesforth, Canard allows you to reuse this logic on things besides
stdin. Buffers are organized like this:

    length | current | upper-bound | data ...

Length, current, and upper-bound are all 4-byte unsigned integers. 'length'
refers to the number of bytes of data, which does not include 'current' and
'upper-bound'. So the total size of an input buffer is actually 12 + length
bytes.

    ::/|:                         # allocate buffer
    488b o107f8                   # data-pop size into %rax
    488d o1300c                   # %rax + 12 -> %rbx
    482b o363                     # %rsi -= %rbx (total size)
    89   o036                     # %rbx -> (%rsi) (length field)
    4831 o300 4889 o10604         # zero out current and upper-bound
    4889 o167f8 c3                # data-push %rsi; return

## Buffer helper functions

There are two helper functions used here. One is |=, which returns the number
of bytes we can read from a buffer. The other is |., which returns one byte
from the buffer and advances it.

    ::/|=                         # amount of data readable from buffer
    488b o107f8                   # data-pop buffer pointer
    8b   o13008                   # upper bound
    2b   o13004                   # current size
    4889 o137f8 c3                # data-push; return

This function advances the buffer, returning the next byte. Behavior is
undefined if the buffer has no available data (you should check it first using
|= above).

    ::/|.                         # advance buffer by one byte
    488b o117f8                   # data-pop buffer into %rcx
    8b   o13104                   # %ebx = current
    ff   o10104                   # ++current
    4831 o300                     # %rax = 0
    8a   o104o0010c               # %al = 12(%rax,%rbx,1) -- next byte
    4889 o107f8 c3                # return result

## Buffer read function

Reads data from a buffer one byte at a time, invoking a function on each byte.
The function is not invoked if there is no data in the buffer. Here's the
derivation:

    |* f buf -> ? [|* f buf (f *buf++)] [] (|= buf)
    [] |= %1      f buf   = [] (|= buf) f buf
    %1 %x         f buf   = f buf f
    |* %R . |. %1 f buf f = |* f buf (f *buf++)

We can use the composition macro; this makes it possible to inspect the
definition of |* after the fact. We preallocate closures for the auxiliary
functions.

    ::/|*_k_false c7 o300:4[Lb:/|*_false] 48ab c3
    ::/|*_k_true  c7 o300:4[Lb:/|*_true]  48ab c3

    ::/|*_false c3
    ::/|*_true
    - composition /%x, /%1, /%1, /|., /., /%R, /|*
    c3

    ::/|*
    - composition /%1, /|=, /|*_k_false, /|*_k_true, /?
    c3

## Buffer fill function

We can generate a fill function by closing over a file descriptor and
providing a buffer as the second argument. We then return the buffer.

    |< fd buf = buf

Here's the behavior:

    |< fd buf -> buf (=4 buf (&read (@4 buf) (buf + 12) fd))

This function, however, is written in machine code since introspection is not
likely to be particularly useful.

Note that any data in the buffer is obliterated when you call |<. You should
check this by using |= first.

    ::/|<
    56                            # store %rsi for later
    4831 o300                     # %rax = 0 (read syscall)
    488b o117f8                   # %rcx = fd
    488b o167f0                   # %rsi = buf
    4883 o306 0c                  # %rsi += 12 (skip buffer header)
    8b   o026                     # %rdx = buffer size
    57 0f05 5f5e                  # syscall; restore %rsi and %rdi

    # At this point %rax contains an error or a count. Clear the buffer position
    # and set the upper bound accordingly. We don't differentiate between errors
    # and legitimate return values.
    488b o117f0                   # %rcx = buffer
    4831 o322                     # %rdx = 0 (position)
    89   o12104                   # store position
    89   o10108                   # store read() result (count)
    4883 o357 08 c3               # deallocate fd from stack; return

# System functions

Some definitions that will be required for the interpreter to work correctly
with the system it's running on. All of these end up using syscall functions,
bound in the symbol table as &0, &1, &2, &3, etc (depending on the arity).

## System calling convention

The top argument to any syscallX function is the number of the syscall that
you want to execute. The other arguments are pulled from the data stack
left-to-right; so, for example:

    &1 x3c x0   <- 0x3c = exit(status = 0)

The only reason syscall wrappers are particularly interesting is that they
have to save/restore %rsi and %rdi on the return stack. Other registers, %r9,
%r10, etc, are not saved.

    ::/&0
    56
    488b o107f8 4883 o357 08      # data-pop -> %rax
    57 0f05 5f5e 48ab c3          # syscall; pop %rdi, %rsi; data-push

    ::/&1
    56                            # push %rsi
    488b o107f8                   # data-pop -> %rax
    4c8b o117f0 4883 o357 10      # data-pop -> %rcx
    57 4887 o317                  # push %rdi; swap %rcx, %rdi
    0f05 5f5e 48ab c3             # syscall; pop %rdi, %rsi; data-push

    ::/&2
    56                            # push %rsi
    488b o107f8                   # data-pop -> %rax
    488b o117f0                   # data-pop -> %rcx
    488b o167e8 4883 o357 18      # data-pop -> %rsi
    57 4887 o317                  # push %rdi; swap %rcx, %rdi
    0f05 5f5e 48ab c3             # syscall; pop %rdi, %rsi; data-push

    ::/&3
    56                            # push %rsi
    488b o107f8                   # data-pop -> %rax
    488b o117f0                   # data-pop -> %rcx
    488b o167e8                   # data-pop -> %rsi
    488b o127e0 4883 o357 20      # data-pop -> %rdx
    57 4887 o317                  # push %rdi; swap %rcx, %rdi
    0f05 5f5e 48ab c3             # syscall; pop %rdi, %rsi; data-push

## Utility functions

These use the above and abstract away some of the details of system calling.
System call numbers are from /usr/include/asm/unistd_64.h on Linux, and where
applicable they're encoded like this:

    4831o300              <- xor %rax, %rax
    b0NN                  <- mov $0xNN, %al

This ends up being two bytes smaller than the equivalent 48c7 o300 NN000000.
The only case where b0NN is not present is for the read() syscall, which is
number 0.

Arguments to these system calls pass through directly, as do return values.

    ::/&read  4831o300      48ab e9:4[L:/&3 - :>]        # n buf fd -> n
    ::/&write 4831o300 b001 48ab e9:4[L:/&3 - :>]        # n buf fd -> n
    ::/&open  4831o300 b002 48ab e9:4[L:/&3 - :>]        # path f m -> fd
    ::/&close 4831o300 b003 48ab e9:4[L:/&1 - :>]        # fd -> status
    ::/&stat  4831o300 b004 48ab e9:4[L:/&2 - :>]        # path buf -> n

    ::/&exit  4831o300 b03c 48ab e9:4[L:/&1 - :>]        # code -> _

    ::bootstrap_end