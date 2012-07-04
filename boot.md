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
    3. /n/byte - byte repeated n times (n is decimal, not hex).

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

    @!40
    # end

    # Elf64_Phdr (rwx image -- this contains code and definitions)
    @!40 01 00 00 00                # p_type  = PT_LOAD
         07 00 00 00                # p_flags = PT_R | PT_W | PT_X

         00 00 00 00 /4/00          # p_offset (includes ELF header)
         00 00 40 00 /4/00          # p_vaddr
         00 00 00 00 /4/00          # p_paddr
         00 04 00 00 /4/00          # p_filesz
         00 04 00 00 /4/00          # p_memsz
         00 10 00 00 /4/00          # p_align

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

    %rsp (4) return stack pointer (grows downwards)
    %rdi (7) data stack pointer (grows upwards towards %rbp)
    %rbp (5) heap pointer (grows downwards towards %rdi)

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

    @!78 

         488a o304                  # %rsp -> %rax
         48ab                       # stosq: push, increment %rdi

    @!7d

# Symbol table

This is the central abstraction provided by Canard.

    @!7d 4831 o300 b03c             # syscall = 60 (exit)
         4831 o355                  # status  = 0
         0f05