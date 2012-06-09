Linux/x86-64 Canard compiler | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This library implements the Canard compiler for Linux on x86-64. The compiler is written in machine language and is self-hosting. It also provides a set of primitives that allow the language
to operate, as well as a core memory image that you can use to create an interpreter instance.

# Primitive functions

Primitives are defined in terms of their machine-language equivalents. The following functions are considered to be primitive: