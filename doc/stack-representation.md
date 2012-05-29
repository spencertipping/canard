# Stack representation

Canard uses multiple stacks to execute a program. The central one is the data stack; this is the only stack that is implicitly accessed by primitive functions, and this stack maintains
pointers to the others. The other primary stack is used for return addresses.

Every stack is encoded as a linked list and can be manipulated as such.