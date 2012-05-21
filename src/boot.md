Bootstrap interpreter | Spencer Tipping
Licensed under the terms of the MIT source code license

# Introduction

This interpreter parses and interprets Canard programs. It provides a set of primitives sufficient to generate the file that will contain the reference Canard compiler. This interpreter
provides no comment syntax; comments can be written as 'nb [stuff]'.

    caterwaul.module('canard', ':all', function ($) {
      ($.canard(source, stack, bindings) = $.canard.parse(source).interpret(stack || null, {} / default_bindings() /-$.merge/ bindings)) /-$.merge/ statics()

      -where [statics() = capture [syntax = tree_ctor() /-$.syntax_subclass/ tree_methods() /-$.merge/ tree_statics(),
                                   parse  = parser()],

# Parsing logic

The parser is really simple; it just handles juxtaposition (treated as cons), list encapsulation, and atom parsing.

              parser() = "toplevel([new linear_string_state(_)])[0].value()".qf
                         -where [toplevel(xs) = toplevel(xs), toplevel = annotate(toplevel, 'toplevel', []),
                                 term(xs)     = term(xs),     term     = annotate(term,     'term',     []),

                                 maybe_ws(p)  = linear_regexp(/\s+/) /-bfs/ p /-alt/ p /-bfc/ optional(linear_regexp(/\s+/)) /-map/ "_[0]".qf,
                                 cons         = maybe_ws(term) /!manyc /-map/ "_ /[$.canard.syntax.nil()][x0 /-$.canard.syntax.cons/ x] -seq".qf,
                                 atom         = linear_regexp(/([^ \n\r\t\[\]]+)/) /-map/ "_[1] /!$.canard.syntax.atom".qf,
                                 list         = linear_regexp(/\[/) / cons /-bfc/ linear_regexp(/\]/) /-map/ "_[1]".qf,
                                 term         = atom /-alt/ list,
                                 toplevel     = cons],

# Syntax trees and interpreter

The interpreter is just a tree-walker that acts as a nondestructive function from stack to stack. It carries a binding table that the program can modify.

              tree_ctor()(d, c1, c2) = d instanceof this.constructor ? this -se [it.data = d.data, it.length = 0]
                                                                     : this -se [it.data = d, it.length = c1 && c2 ? 2 : 0, it[0] /eq.c1 /when.c1, it[1] /eq.c2 /when.c2],

              tree_statics()         = capture [nil()      = new $.canard.syntax('[]'),
                                                atom(data) = new $.canard.syntax(data),
                                                cons(t, h) = new $.canard.syntax('cons', t, h)],

              tree_methods()         = capture [is_cons()  = this.length === 2 && this.data === 'cons',  h()    = this[1],
                                                is_atom()  = !this.length,                               t()    = this[0],
                                                is_quote() = this.is_atom() && /^'/.test(this.data),     name() = this.data.replace(/^'/, ''),
                                                is_nil()   = this.is_atom() && this.data === '[]',
                                                is_n()     = this.is_atom() && /^-?\d+\.?\d*([eE][-+]?\d+)?$/.test(this.data),

                                                toString() = this.is_nil()                                              ? '[]'
                                                           : this.is_cons() && !this.t().is_nil() && this.h().is_cons() ? '#{this.t()} [#{this.h()}]'
                                                           : this.is_cons() &&  this.t().is_nil()                       ? this.h().toString()
                                                           : this.is_cons()                                             ? '#{this.t()} #{this.h()}'
                                                                                                                        : this.data]
                                       /-$.merge/ interpreter_methods(),

              interpreter_methods()  = capture [should_run()               = this.is_atom() && !this.is_quote() && !this.is_n(),

                                                execute(stack, bindings)   = this.should_run() ? bindings[this.name()].interpret(stack, bindings)
                                                                                               : {h: this, t: stack},

                                                interpret(stack, bindings) = this.is_cons()    ? this.t().interpret(this.h().execute(stack, bindings), bindings)
                                                                           : this.should_run() ? bindings[this.name()].interpret(stack, bindings)
                                                                                               : {h: this, t: stack}],

# Default bindings

These provide a very simple base language that should be sufficient to write the abstractions necessary to generate the reference compiler. Most of these forms are self-explanatory with the
exception of 'q', which implements a decisional. The syntax is:

    q [true-branch] [false-branch] number

The true branch is run if the number is non-zero; the false-branch is run otherwise. Either branch can be nil.

              up(stack, n)          = n ? up(stack.t, n - 1) : stack.h,

              default_bindings()    = capture [def(stack, bindings)   = bindings[stack.h.name()] -eq- stack.t.h -then- stack.t.t,
                                               log(stack)             = console.log(stack.h.toString())              -then- stack.t,
                                               trace(stack)           = console.log('\033[1;32m#{stack.h}\033[0;0m') -then- stack,

                                               chr(stack)             = {h: $.canard.syntax.atom(String.fromCharCode(+stack.h.data)), t: stack.t},
                                               ord(stack)             = {h: $.canard.syntax.atom(stack.h.data.charCodeAt(0)),         t: stack.t},

                                               q(stack, bindings)     = +stack.t.t.h.data ? stack  .h.interpret(stack.t.t.t, bindings)
                                                                                          : stack.t.h.interpret(stack.t.t.t, bindings),

                                               is_nil(stack)          = {h: +stack.h.is_nil() /!$.canard.syntax.atom,  t: stack.t},
                                               is_cons(stack)         = {h: +stack.h.is_cons() /!$.canard.syntax.atom, t: stack.t},

                                               nip(stack)             = {h: stack.t.h, t: stack},
                                               get(stack)             = {h: up(stack.t, +stack.h.data), t: stack},

                                               not(stack)             = {h: +!stack.h.data /!$.canard.syntax.atom, t: stack.t},
                                               swap(stack)            = {h: stack.t.h, t: {h: stack.h, t: stack.t.t}},
                                               dup(stack)             = {h: stack.h, t: stack},
                                               nb(stack)              = stack.t,
                                               stash(stack, bindings) = {h: stack.t.h, t: stack.h.interpret(stack.t.t, bindings)},
                                               cons(stack)            = {h: $.canard.syntax.cons(stack.h, stack.t.h), t: stack.t.t},
                                               uncons(stack)          = new Error('#{stack.h} is not a cons cell') /raise /unless [stack.h.is_cons()]
                                                                        -then- {h: stack.h.h(), t: {h: stack.h.t(), t: stack.t}},

                                               id(stack)              = stack,
                                               i(stack, bindings)     = stack.h.interpret(stack.t, bindings)] %v*[{interpret: x}] /seq /se [it['[]'] = it.id]

                                      /-$.merge/ arithmetic_bindings(),

              arithmetic_bindings() = '+ - * / % >> << >>> | & ^ < > <= >= === !== == !='.qw
                                      *[[x, {interpret: "function (stack) {return {h: caterwaul.canard.syntax.atom(+stack.t.h.data #{x} +stack.h.data), t: stack.t.t}}" /!$.parse /!$.compile}]]
                                      -object -seq]

      -using- caterwaul.parser});