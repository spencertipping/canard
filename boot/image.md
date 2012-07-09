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
      ::phdr_p_flags  :4[L 4 | 2 | 1] # PT_R | PT_R | PT_X
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

    ::@main 0400'main  ::@@:k 0300'@:k  ::@@> 0200'@>  ::@&0 0200'&0 ::@&1 0200'&1
    ::@::   0200'::    ::@@?k 0300'@?k  ::@@< 0200'@<  ::@&2 0200'&2 ::@&3 0200'&3

    ::@=1 0200'=1 ::@=2 0200'=2 ::@=4 0200'=4 ::@=8 0200'=8
    ::@@1 0200'@1 ::@@2 0200'@2 ::@@4 0200'@4 ::@@8 0200'@8

## Binding table

To save space, we encode each binding as an offset from the beginning of the
file. This will break if the file grows to be larger than 65535 bytes.

    ::binding_table
    :2[L:@@:k]:2[L:generate_constant_matcher] :2[L:@@?k]:2[L:constant_matcher]

    :2[L:@@>]:2[L:get_symbol_table]
    :2[L:@@<]:2[L:set_symbol_table]

    :2[L:@&0]:2[L:syscall0] :2[L:@&1]:2[L:syscall1]
    :2[L:@&2]:2[L:syscall2] :2[L:@&3]:2[L:syscall3]

    :2[L:@::]:2[L:cons]

    :2[L:@@1]:2[L:r8] :2[L:@@2]:2[L:r16] :2[L:@@4]:2[L:r32] :2[L:@@8]:2[L:r64]
    :2[L:@=1]:2[L:w8] :2[L:@=2]:2[L:w16] :2[L:@=4]:2[L:w32] :2[L:@=8]:2[L:w64]

    :2[L:@main]:2[L:main]

    /2/00                                 # end marker
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

    ::push_rsp    488b o305 48ab
    ::init_return 68:4[Lb:exit]

Now push the symbol 'main' onto the data stack and call the symbol table. This
boots up the interpreter.

## Symbol encoding

As mentioned in the design documentation, symbols are encoded using a
little-endian two-byte prefix for the inner (logical) length. So 'hello' is
encoded in these seven bytes: 0500 68656c6c6f. UTF-8 is not parsed, though it
will work transparently provided that the total number of bytes doesn't exceed
the limit of 65535.

    ::push_main 4831 o300 b8:4[Lb:@main] 48ab

Now do a symbol table lookup to get the address. When this returns, we'll have
the address of the 'main function on top of the data stack.

Before we can do this lookup, however, we need to construct the symbol table.

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

    e8:4[L:nil - :>]

At this point, the heap looks like this:

    %rsi -> | c3 | :image_end

This is necessary for the initialization we're going to do below.

## Default bindings

Set up a few bindings by default. These comprise the bootstrap image. The
definitions themselves are below.

Right now the symbol table as such doesn't exist; we need to build it by
consing up a bunch of symbol matchers, each of which is generated by one of
the functions below. These functions will then be added to the symbol table
and will be part of the image.

    ::read_binding_table
    48c7 o305:4[Lb:binding_table]

    ::read_binding_table_loop
    4831 o300 8b o105 00                                  # (%rbp + 0) -> %rax
    66a9 ffff 74:1[L:read_binding_table_loop_end - :>]    # break if symbol == 0
    488b o310 31 o322                                     # %rcx = %rax; %rdx = 0
    0fac o320 10 05:4[L:image_base] 48ab                  # push %rax >> 16 + b
    668b o301 48ab                                        # push symbol

    4883 o305 04                                          # %rbp += 4
    55 e8:4[L:generate_constant_matcher - :>]             # stash %rbp
       e8:4[L:cons                      - :>] 5d          # restore %rbp

    eb:1[L:read_binding_table_loop - :>]
    ::read_binding_table_loop_end

Now we have a complete symbol table. Bind it to the global address and start
up the interpreter.

    488b o137 f8                          # copy the symbol table to %rbx
    e8:4[L:set_symbol_table - :>]         # and install it globally

At this point the data stack top is the 'main' symbol, which is ideal: we can
resolve it and then tail-call into the result. 'main' can return into 'exit'.

    ff   o323                             # resolve 'main'
    488b o107 f8 4883 o357 08             # data-pop %rax
    ff   o340                             # jump into main

# Internals

Everything from here down is referenced or called by the above to build out the
image, but is not executed in the toplevel stream.

## Main function

This will end up tying together a number of pieces, but for the moment it just
pushes a zero to return.

    ::main
    4831 o300 48ab        # data-push 0
    c3                    # return (to exit function)

## Nil

This is easy; it's just a single byte on the heap. We then push a pointer to
that byte onto the data stack.

    ::nil
    ff   o316             # allocate one byte for nil
    c6   o006 c3          # move the byte c3 to this address
    488b o306 48ab        # push the c3 reference onto the data stack
    c3                    # return

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

## Constant symbol matcher generator

This function takes a symbol and an address on the stack and returns a pointer
to a matcher that matches the symbol and returns the address. If the match
fails, the matcher invokes the 'next' continuation as described above.

Each generated matcher is a separately-allocated chunk of machine code that
contains the immediate data required to behave as a closure. So, for example,
here's what gets generated for the symbol 'foo:

    foo_matcher:
      movq $foo_data, %rax              <- data-push 'foo' symbol
      stosq
      movq $binding_immediate, %rax     <- data-push symbol binding
      stosq
      jmp constant_matcher              <- tail-call matching function

We can easily compute the amount of space required to store the generated
code, so we can preallocate. Here's the machine code for the assembly above:

    foo_matcher:
      48b8 foo_data [8 bytes] 48ab      <- 12 bytes total
      48b8 binding  [8 bytes] 48ab      <- 12 bytes total
      e9 constant_matcher [4 bytes]     <- 5 bytes total

So we'll need 29 bytes of space per symbol matcher. Right now we don't have a
proper heap allocator, but luckily it's straightforward enough. We just
subtract some amount from %rsi and use the new memory.

Here's how the matcher generator works:

    generate_constant_matcher: (@:k in the symbol table)
      data-pop %rax                     <- symbol pointer
      data-pop %rbx                     <- binding
      movq %rsi, %rcx                   <- original heap pointer
      subq $29, %rsi                    <- allocate code space
      movw $0xb848, (%rsi)              <- write 48b8 instruction
      movq %rax, 2(%rsi)                <- write symbol address
      movl $0xb848ab48, 10(%rsi)        <- write 48ab and 48b8 instructions
      movq %rbx, 14(%rsi)               <- write binding address
      movl $0x00e9ab48, 22(%rsi)        <- write 48ab and e9 instructions
      movq $constant_matcher, %rax      <- absolute address of matcher fn
      subq %rcx, %rax                   <- compute %rax - (29 + %rsi)
      movl %ecx, 25(%rsi)               <- write call to constant_matcher
      data-push %rsi                    <- return reference to new matcher
      ret                               <- return

Notice that we've got this reference to constant_matcher in the middle of the
code. This isn't a problem because we can just put the constant matcher into
the bootstrap section (basically right here), where it will have a known
address. We can then hard-code that address into generate_constant_matcher.
None of this requires any interaction with the symbol table, which at this
point is not yet functional.

## Constant matcher definition

The constant matcher just compares bytes within a contiguous region of memory.
It takes two symbols and a binding address as data stack arguments; the
binding address will be sent to the second continuation if the two symbols
match; otherwise the immediate continuation will be used and the binding
address and one of the symbols will be popped from the data stack. (This is
basically how it needs to work in order to adhere to the calling convention
for symbol table matchers.)

Here's the logic:

    constant_matcher:
      data-pop %rbp                     <- binding address
      data-pop %rax                     <- first symbol
      movq -8(%rdi), %rbx               <- second symbol (peek, not pop)
      xorq %rcx, %rcx                   <- clear high bits
      movw (%rax), %cx                  <- get length
      cmpw %cx, (%rbx)                  <- both same length?
      loope length_ok                   <- then check characters
    bail:
      ret                               <- else bail
    length_ok:
      movb 2(%rbx,%rcx,1), %dl
      cmpb %dl, 2(%rax,%rcx,1)
      je ok
      ret
    ok:
      loop
    success:
      movq %rbp, -8(%rdi)               <- set data stack result
      pop %rax                          <- drop 'next' continuation
      ret                               <- invoke 'return' continuation

This function ends up being bound as @?k in the symbol table. We bind this
once we've defined cons and bind below.

    ::constant_matcher
    488b o157 f8                          # -8(%rdi) -> %rbp
    488b o107 f0                          # -16(%rdi) -> %rax
    488b o137 e8                          # -24(%rdi) -> %rbx
    4883 o357 10                          # %rdi -= 16 (pop two entries)

    4831 o311 668b o010                   # %rcx = length
    6639 o013 e101 c3                     # length check + bailout

    ::constant_matcher_loop
    8a   o124 o013 02                     # top of loop: populate %dl
    38   o124 o010 02                     # compare characters
    7401 c3                               # bail if not equal
    e2:1[L:constant_matcher_loop - :>]

    4889 o157 f8                          # %rbp -> -8(%rdi)
    58 c3                                 # pop; invoke return continuation

Here's the definition of generate_constant_matcher. Push a trivial return
address here so that the function below will return to the continuation
immediately following its definition.

    ::generate_constant_matcher
    488b o107 f8                          # symbol pointer
    488b o137 f0                          # binding
    4883 o357 08                          # pop one (we replace the other later)

    488b o316                             # %rsi -> %rcx
    4883 o356 1d                          # %rsi -= 29
    66c7 o006 48b8        4889 o106 02    # 48b8, symbol pointer
    c7   o106 0a 48ab48b8 4889 o136 0e    # 48b8, 48ab, binding
    c7   o106 16 48abe900                 # 48ab, e9
    c7   o300:4[Lb:constant_matcher]      # $constant_matcher -> %rax
    482b o301 89 o106 19                  # 25(%rsi) = %rax - %rcx
    4889 o167 f8                          # %rsi -> -8(%rdi)
    c3

# Consing and memory allocation

We need a way to allocate new cons cells without manually getting memory from
the heap. To do this, we define the cons function, which will ultimately be
stored in the symbol table as ::.

Right now we have some leftover stuff on the stack. The data stack top is the
symbol matcher for @:k, which we'll ultimately need to cons onto the nil we put
into a single byte on the heap.

## Cons definition

As described in the design documentation, cons has a few different cases that
it knows how to deal with. If the tail matches the current heap pointer, we
can omit the e9 jump and just rely on sequence to execute the tail as-is,
saving five bytes. Otherwise we write the e9 jump.

Cons takes the head on top of the stack and the tail beneath that. This is
counterintuitive from code, as arguments appear to be in the wrong order:

    :: 3 []             <- correct
    :: [] 3             <- improper list

However, generally it's more useful to access the head of a list than the
tail, so we want it to be immediately available.

Given all of that, here's the algorithm for cons (referred to as :: from now
on):

    cons:
      data-pop %rax             <- head
      data-pop %rbx             <- tail
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

    ::cons
    488b o107 f8                          # head element
    488b o137 f0                          # tail element
    4883 o357 08                          # pop one

    4839 o336 74:1[L:cons_head - :>]      # tail allocation check
    482b o336 89 o136 fc                  # relative, write e9 jump offset
    4883 o356 05 c6 o006 e9               # allocate space, write e9 jump opcode

    ::cons_head
    482b o306 89 o106 fc                  # relative, write e8 call offset
    4883 o356 05 c6 o006 e8               # allocate space, write e8 call opcode

    4889 o167 f8 c3                       # return new cons cell

## Low-level memory access

This is necessary to define a number of things in the standard library. Memory
can be read and written in a few different sizes, and the processor's
endianness is used. (This is relevant since all stack values are the same
width -- so for small reads/writes, you're working with a byte slice.)

    ::r8  4831 o300 488b o137 f8   8a o003 4889 o107 f8 c3
    ::r16 4831 o300 488b o137 f8 668b o003 4889 o107 f8 c3
    ::r32 4831 o300 488b o137 f8   8b o003 4889 o107 f8 c3
    ::r64 4831 o300 488b o137 f8 488b o003 4889 o107 f8 c3

Writes take the value on the top of the stack, followed by the address. We use
%rax for the value and %rbx for the address.

    ::w8  488b o107 f8 488b o137 f0 4883 o357 10   88 o003 c3
    ::w16 488b o107 f8 488b o137 f0 4883 o357 10 6689 o003 c3
    ::w32 488b o107 f8 488b o137 f0 4883 o357 10   89 o003 c3
    ::w64 488b o107 f8 488b o137 f0 4883 o357 10 4889 o003 c3

# Definition and invocation

This is the last piece of the symbol table. Remember from earlier that the
symbol table pointer is mutable and is stored at a fixed address. The simplest
way to deal with this value in memory is just to build two special-purpose
functions that read or write it.

## Symbol table getter

We refer to this as @> from now on. This function pushes the symbol table
address onto the data stack.

    ::get_symbol_table
    488b o005:4[L:symbol_table - :>] 48ab c3      # st -> %rax; push; ret

## Symbol table setter

This function is only slightly more complex than @> due to the fact that we
don't have a shortcut quite as nice as stosq. The setter is called @<.

    ::set_symbol_table
    488b o107 f8 4883 o357 08                     # data-pop -> %rax;
    4889 o005:4[L:symbol_table - :>] c3           # %rax -> (symbol_table); ret

# System functions

Some definitions that will be required for the interpreter to work correctly
with the system it's running on. All of these end up using syscall functions,
bound in the symbol table as &0, &1, &2, &3, etc (depending on the arity).

## System calling convention

The top argument to any syscallX function is the number of the syscall that
you want to execute. The other arguments are pulled from the data stack
left-to-right; so, for example:

    &1 3c 00    <- 0x3c = exit(status = 0)

The only reason syscall wrappers are particularly interesting is that they
have to save/restore %rsi and %rdi on the return stack. Other registers, %r9,
%r10, etc, are not saved.

    ::syscall0
    488b o107 f8 4883 o357 08     # data-pop -> %rax
    5657 0f05 5f5e 48ab c3        # syscall; pop %rdi, %rsi; data-push

    ::syscall1
    488b o107 f8                  # data-pop -> %rax
    4c8b o117 f0 4883 o357 10     # data-pop -> %rcx
    5657 4887 o317                # push %rsi, %rdi; swap %rcx, %rdi
    0f05 5f5e 48ab c3             # syscall; pop %rdi, %rsi; data-push

    ::syscall2
    56                            # push %rsi
    488b o107 f8                  # data-pop -> %rax
    488b o117 f0                  # data-pop -> %rcx
    488b o167 e8 4883 o357 18     # data-pop -> %rsi
    57 4887 o317                  # push %rdi; swap %rcx, %rdi
    0f05 5f5e 48ab c3             # syscall; pop %rdi, %rsi; data-push

    ::syscall3
    56                            # push %rsi
    488b o107 f8                  # data-pop -> %rax
    488b o117 f0                  # data-pop -> %rcx
    488b o167 e8                  # data-pop -> %rsi
    488b o127 e0 4883 o357 20     # data-pop -> %rdx
    57 4887 o317                  # push %rdi; swap %rcx, %rdi
    0f05 5f5e 48ab c3             # syscall; pop %rdi, %rsi; data-push

## Utility functions

These use the above and abstract away some of the details of system calling.

    ::exit
    4831 o300 b03c 48ab e8:4[L:syscall1 - :>]

    ::bootstrap_end