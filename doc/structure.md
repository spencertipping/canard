# Interpreter structure

The interpreter should itself be a data structure. This will make it much easier
to parse and generate images that can be executable. The ELF header contains a
single program header entry (to refer to the heap), and its eh_entry field is
used to point to a closure consisting of the main symbol and the symbol table.
Here's what this would look like:

    elf header data...
    eh_entry: 0x7fff0000ffffff10
    ...
    program_header_1:
      ph_vaddr: 0x7fff0000ffffff10
      ph_filesz: 0xNNN            <- this + ph_vaddr should be 0x7fff000100000000
      ph_offset: 0xff10           <- must == ph_vaddr % page size (usually 0x1000)
    ...
    0xff10:
      48b8 SS...SS 48ab           <- push initial symbol
      e8 XXXXXXXX                 <- invoke symbol table to resolve
      488b o107f8                 <- stack top -> %rax
      4883 o357 08                <- %rdi -= 8 (pop entry)
      ff o350                     <- jump to %rax

The symbol table is itself a cons structure that checks symbol mappings.
However, in order for it to support introspection it can't just be an arbitrary
cons of partial functions. If it did, we would see this:

    [fn ... f2 f1]                <- each fx is opaque

In actuality, each fx is, most likely, itself a cons representing some closure
over a matcher. So we would see this:

    [... [@?k kv1 ks1]]

We could then invoke the kv1 and ks1 functions to determine the values they
hold. We know that the other function corresponds to @?k because we have a
similar entry for it in the symbol table:

    [... [@?k @?k '@?k] ...]

We can assume that an entry using its own matcher as its value will be a
constant entry. The reason is that you would most likely never bind a wildcard
matcher under a non-constant name since the structure would be different. For
instance:

    [... [@<x @<x '???] ...]      <- you wouldn't write this