# Numbers

Canard doesn't need a way to represent all numeric literals. It just needs to express each byte value and provide some arithmetic operators. So, as part of the boot image, there are 255
functions:

    00 01 02 ... 0f 10 11 12 ... fe ff

Each of these pushes a single byte value onto the stack. It is not too difficult to build a list->number compiler from there: [00 12 50 10] -> 0x00125010, for instance (or big-endian,
depending on the compiler).

This removes the numeric-parsing case from the boot compiler/interpreter, which is ideal. Now all non-bracket words can be treated exactly the same way.