Bootstrap interpreter | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This interpreter parses and interprets Canard programs. It provides a set of primitives sufficient to generate the file that will contain the reference Canard compiler. To do this, it emits
the list on the top of the stack to stdout when the program exits, interpreting it as a linked list of bytes.

    caterwaul.module('canard', ':all', function ($) {});