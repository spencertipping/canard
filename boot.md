Canard bootstrap interpreter | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

Basic syntax used in this file is:

    1. Address assertion: @!address. Dies unless the next byte will be emitted at
       the given address. I use these to verify/document the code.
    2. Address displacement: @address. Inserts null bytes until the given address
       is reached.
    3. /n/byte - byte repeated n times (n is decimal, not hex).

That's about it. The rest is just binary data written verbatim in hex (no
prefix), octal (o prefix), binary (- prefix), or ASCII (' prefix).

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
Multi-byte immediate operands are generally separate from opcodes:

    push $0x400010              # push a 32-bit immediate
    68 10004000
     |  |
     |  +-- immediate: little-endian, so bytes are reversed
     +----- opcode: 68 = push 32-bit immediate

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

    @!40
    # end

    # Elf64_Phdr (rwx image -- this contains code and definitions)
    @!40 01 00 00 00                # p_type  = PT_LOAD
    @!44 07 00 00 00                # p_flags = PT_R | PT_W | PT_X

    @!48 00 00 00 00 /4/00          # p_offset (includes ELF header)
    @!50 00 00 40 00 /4/00          # p_vaddr
    @!58 00 00 00 00 /4/00          # p_paddr
    @!60 00 02 00 00 /4/00          # p_filesz
    @!68 00 00 10 00 /4/00          # p_memsz
    @!70 00 10 00 00 /4/00          # p_align

    @!78
    # end

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

## Register initialization

Set up these registers to point to useful places. %rsp is provided by the OS,
so we don't need to do anything else with it. We set up %rdi and %rsi like
this:

    +-- 400000   +-- 400078       +-- 40xxxx            +-- 500000
    V            V                V                     V
    | ELF header | bootstrap code | data stack ... heap | .... | %rsp stack

I'm leaving the data stack lower bound as a variable here because it depends
on the amount of bootstrap code that we use. But the idea is the same either
way; the data stack begins immediately after the bootstrap logic. In order to
make this as automatic as possible, we grab the value from the ELF header. In
this case, we want to add 0x400000 to the p_filesz field in the first program
header entry; this field is located at 0x400060.

Because this number is frequently used, I'm storing it in %rbp during the
initialization process.

    @!78 488b o054 o045 60 00 40 00 @!80  # get p_filesz
    @!80 4881 o305      00 00 40 00 @!87  # add p_vaddr to get memory offset

At this point %rbp points to the bottom (lowest address) of the data stack.

    @!87 488b o375                # initialize %rdi to bottom of data stack
         48c7 o306 f8 ff 4f 00    # initialize %rsi to top of heap minus 8
         488b o304 48ab @!96      # %rsp -> %rax; stosq to push onto data stack

I'm leaving an 8-byte slot open at the top of the heap. This is used to keep
track of the current global symbol table cons cell, since it changes every
time we define something. This is the only piece of global mutable state
maintained by the interpreter (aside from the stacks and heap).

At this point the registers are initialized, so we can write to both stacks
and to the heap. We need to set up a 'return' into the exit function so that
when the main function exits (i.e. we hit a nil at the end of the main program
list) it will die gracefully. First let's push the exit function address onto
the return stack:

    @!96 68 bebafeca @!9b                 # FIXME once we define exit

Now push the symbol 'main' onto the data stack and call the symbol table. This
boots up the interpreter.

## Symbol encoding

As mentioned in the design documentation, symbols are encoded using a
little-endian two-byte prefix for the inner (logical) length. So 'hello' is
encoded in these seven bytes: 0500 68656c6c6f. UTF-8 is not parsed, though it
will work transparently provided that the total number of bytes doesn't exceed
the limit of 65535.

What we need to do here is encode the symbol 'main. We don't actually need to
write it again since it's already present in the symbol table; however, this
provides a good functional test of the symbol resolution code so it's worth
doing. We can drop the symbol directly into the machine code and just refer to
it by absolute address.

We can easily drop short symbols directly into machine code by using a
mov-immediate into %rax that will be subsequently overwritten. The advantage
of doing things this way is that disassemblers won't have to follow jumps to
figure out where the instructions are.

    @!9b 48b8 @!9d 0400 'main /2/00 @!a5
    @!a5 48c7 o300 9d 00 40 00 48ab @!ae  # data-push the 'main symbol address

Now do a symbol table lookup to get the address. When this returns, we'll have
the address of the 'main function on top of the data stack.

Before we can do this lookup, however, we need to construct the symbol table.

# Symbol table

There are several pieces of machinery involved in creating the symbol table.
First, the symbol table is ultimately a function composition, which is
represented in Canard as a linked list of cons cells. So the general form is
something like this:

    4ffff8: 00000000ssssssss
    ssssssss: e8 xxxxxxxx | e8 yyyyyyyy | e9 zzzzzzzz
    zzzzzzzz: e8 qqqqqqqq | ... | c3

Specifically, when executed it will use an x86 CALL to each entry, and when
examined as a cons tree it will give you each symbol resolver directly. At the
very end is a RET instruction; the result of this is that if no resolver can
handle the symbol you pass in, the symbol table just hands you your symbol back.
In list form, this corresponds to a nil as the final tail.

    @!ae 48ff o316    @!b1          # allocate one byte for nil
    @!b1 c6   o006 c3 @!b4          # move the byte c3 to this address

    @!b4 488b o306 48ab @!b9        # push the c3 reference onto the data stack

At this point, the heap looks like this:

    %rsi -> | c3 | xxxxxxxx xxxxxxxx |

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

Right now I'm defining the generator for matcher functions. However, we'll
want to store this generator in the symbol table, so we'll end up needing to
apply it to itself. The easiest way to do that is to first set up its
arguments on the data stack, then just list out the generator code and let it
run. After the definition/invocation, we'll have the first symbol table entry
on the data stack, ready to be consed into the symbol table.

    @!b9 48c7 o300 0b 01 40 00 48ab @!c2  # data-push code reference
    @!c2 48b8 @!c4 0300 '@:k  /3/00 @!cc  # literal symbol @:k
    @!cc 48c7 o300 c4 00 40 00 48ab @!d5  # data-push symbol reference

Now we can write the matcher generator here, running the code on its own
definition in the process. We can easily compute the amount of space required
to store the generated code, so we can preallocate. Here's the machine code
for the assembly above:

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

    @!d5 eb2f @!d7                        # jump over constant_matcher

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

    @!d7  488b o157 f8                    # -8(%rdi) -> %rbp
    @!db  488b o107 f0                    # -16(%rdi) -> %rax
    @!df  488b o137 e8                    # -24(%rdi) -> %rbx
    @!e3  4883 o357 10                    # %rdi -= 16 (pop two entries)

    @!e7  4831 o311 668b o010             # %rcx = length
    @!ed  6639 o013 e101 c3               # length check + bailout

    @!f3  8a o124 o013 02                 # top of loop: populate %dl
    @!f7  38 o124 o010 02                 # compare characters
    @!fb  74 01 c3                        # bail if not equal
    @!fe  e2 f3 @!100                     # next character

    @!100 4889 o157 f8                    # %rbp -> -8(%rdi)
    @!104 58 c3                           # return continuation
    @!106

Now we have the constant matcher function defined at address 0x4000d7, so we
can go ahead and define/execute the generate_constant_matcher function,
referred to as @:k from now on.

Push a trivial return address here so that the function below will return to
the continuation immediately following its definition.

    @!106 68 4c014000 @!10b

    @!10b 488b o107 f8                    # symbol pointer
    @!10f 488b o137 f0                    # binding
    @!113 4883 o357 08                    # pop one (we replace the other later)

    @!117 488b o316                               # %rsi -> %rcx
    @!11a 4883 o356 1d                            # %rsi -= 29
    @!11e 66c7 o006 48b8      4889 o106 02        # 48b8, symbol pointer
    @!127 c7 o106 0a 48b848ab 4889 o136 0e        # 48b8, 48ab, binding
    @!132 c7 o106 16 48abe900                     # 48ab, e9
    @!139 48c7 o300 d8004000                      # $constant_matcher -> %rax
    @!140 482b o301 4889 o106 19                  # 25(%rsi) = %rax - %rcx
    @!147 4889 o167 f8                            # %rsi -> -8(%rdi)
    @!14b c3

    @!14c

At this point we have a constant matcher for @:k on the top of the data stack
with a pointer to nil beneath that. This is ideal for the setup that happens
next.

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

As usual, we need to push a return continuation before we define/execute this
code.

    @!14c 68 81014000 @!151

    @!151 488b o107 f8            # head element
    @!155 488b o137 f0            # tail element
    @!159 4883 o357 08            # pop one (the other will be overwritten)

    @!15d 4839 o336 740d                  # tail allocation check
    @!162 482b o336 89 o136 fc            # relative, write e9 jump offset
    @!168 4883 o356 05 c6 o006 e9         # allocate space, write e9 jump opcode

    @!16f 482b o306 89 o106 fc            # relative, write e8 call offset
    @!175 4883 o356 05 c6 o006 e8         # allocate space, write e8 call opcode

    @!17c 4889 o167 f8 c3                 # return new cons cell
    @!181

At this point we have the cons cell of @:k's definition and nil. This now
needs to be set as the symbol table; but before we do that, we need to add
cons and create a function that updates the symbol table address. First let's
bind cons.

    @!181 4831 o300 b8 51010400 48ab      # push pointer to cons function
    @!18b 48b8 @!18d 0200 ':: /4/00 @!195
    @!195 48c7 o300 8d014000 48ab         # push symbol ::

    @!19e e8 68ffffff                     # invoke @:k to generate the closure
    @!1a3 e8 a9ffffff @!1a8               # invoke :: to add binding

    4831 o300 b03c 4831 o377 0f05