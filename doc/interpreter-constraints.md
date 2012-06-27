# Interpreter constraints

| 1. The interpreter should impose little overhead.
  2. Its data should be polymorphic (symbols, conses, nil).
  3. It must provide a representation for tail calls.

All of this is achieved if lists are made of executable code, but as discussed
elsewhere this has its own disadvantages.