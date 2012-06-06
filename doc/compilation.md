# Compilation

Canard makes much more sense as a binary memory image than as text. The 'compiler' just takes an input file and converts it to bytecode, a process that can be done in linear average time and
with almost no analysis. In particular, symbols are erased up-front; the string data is not necessarily stored.

Separate compilation is not possible, but this is not a problem from the perspective of performance. Multiple source files in the same environment should be concatenated prior to compilation;
this ensures good symbol packing and, importantly, allows symbol strings to be erased even for symbols that link files together. Because of this, there is no such thing as a precompiled
library.

The compilation process is fairly simple; it's just about converting the text into a list image. This is doable because all pointers are relative; this makes the resulting memory image
position-independent. Compiled images contain no mutable static data references; any such references will need to be pushed onto the stack at runtime. Immutable static data is encoded into
instructions as immediate data.