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

syn region  canard_bracketed_region     matchgroup=canard_comment           start=/\[/ end=/]/ contained transparent
syn region  canard_comment              matchgroup=canard_comment_delimiter start=/nb\s*\[/ end=/]/ contains=canard_bracketed_region
syn region  canard_quoted_symbol        start=/\<'/ end=/[ \n\t\r\[\]]\@=\|$/
syn match   canard_bracket              /:\?\[/
syn match   canard_bracket              /]/
syn match   canard_number               /\<[0-9a-f][0-9a-f]\>/

syn match   canard_stack_intrinsic      /\<%[01234][a-d]\{0,4\}\>/
syn match   canard_stack_intrinsic      /\<^[1234]\>/

syn keyword canard_def                  =
syn keyword canard_intrinsic            $< $\| $# $ $^ $+ $: $. $= ? :? /? :: :^ . @ !
syn keyword canard_arithmetic           + - * / % << >> >>> \| & ^ < > <= >= == != === !==

hi link canard_comment                  Comment
hi link canard_comment_delimiter        Special
hi link canard_quoted_symbol            Identifier
hi link canard_def                      Keyword
hi link canard_bracket                  Special
hi link canard_number                   Number
hi link canard_intrinsic                Type
hi link canard_arithmetic               Type
hi link canard_stack_intrinsic          Operator
