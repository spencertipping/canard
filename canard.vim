" Canard language highlighter | Spencer Tipping
" Licensed under the terms of the MIT source code license

if !exists("main_syntax")
  if version < 600
    syntax clear
  elseif exists("b:current_syntax")
    finish
  endif
  let main_syntax = "canard"
endif

syn case match
setlocal iskeyword=33-90,92,94-127

" Syntax (libraries cannot change this)
syn match canard_bracket /\[/
syn match canard_bracket /]/

hi link canard_bracket   Special

" JVM interpreter bootstrap functions
syn region  canard_quoted_symbol    start=/\<'/ end=/[ \n\t\r\[\]]\@=\|$/
syn match   canard_stack_intrinsic  /\<%[0-9a-f]\+\>/
syn match   canard_stack_intrinsic  /\<^[0-9a-f]\>/
syn keyword canard_intrinsic        @< @> :: :^ :? ? ' '? . .? r< r> =
syn match   canard_field_reference  /\<#\w\+\>/
syn match   canard_method_reference /\<\.\w\+\>/

hi link canard_quoted_symbol        Identifier
hi link canard_number               Number
hi link canard_intrinsic            Type
hi link canard_stack_intrinsic      Operator

hi link canard_field_reference      Identifier
hi link canard_method_reference     Identifier

" Provided by the core library
syn region  canard_comment          matchgroup=canard_comment_delimiter start=/nb\s*\[/ end=/]/ contains=canard_bracketed_region
syn region  canard_bracketed_region matchgroup=canard_comment           start=/\[/ end=/]/ contained transparent

syn match   canard_shebang          +#!/usr/bin/canard+
syn keyword canard_def              def def'

hi link canard_comment              Comment
hi link canard_comment_delimiter    Special

hi link canard_shebang              Special
hi link canard_def                  Special
