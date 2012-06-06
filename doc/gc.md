# Garbage collection

GC is implemented as a library and is, importantly, non-primitive. Most GC libraries probably provide custom stack representations and cons allocators, and their entry point is an evaluation
function.