# Minimal allocation

Intel JMP instructions can be encoded in two widths, and CALL instructions can
be encoded in one on 64-bit machines. CALL is always e8; JMP is any one of:

    eb xx                 <- 8-bit relative jump
    e9 xxxxxxxx           <- 32-bit relative jump

Three bytes of memory can be saved if we pack a jump, which could easily be
worth it to compensate for things like tightly-paired cons cells.