caterwaul.module( 'canard' , (function(e) {var result= (function($) { (function() {var annotate=e.annotate,parsers=e.parsers,bfs=e.bfs,bfc=e.bfc,state_matrix=e.state_matrix,step_matrix_mutable=e.step_matrix_mutable,step_matrix_immutable=e.step_matrix_immutable,row_composite_states_from=e.row_composite_states_from,alt=e.alt,all=e.all,manyc=e.manyc,manyc_one=e.manyc_one,many=e.many,optional=e.optional,step_matrix_immutable_null=e.step_matrix_immutable_null,step_matrix_mutable_null=e.step_matrix_mutable_null,has_non_null_states=e.has_non_null_states,row_null_states_from=e.row_null_states_from,zero=e.zero,fail=e.fail,match=e.match,reject=e.reject,pluralize=e.pluralize,iv=e.iv,map=e.map,flat_map=e.flat_map,map_state=e.map_state,flat_map_state=e.flat_map_state,logical_state=e.logical_state,linear_string_state=e.linear_string_state,anchor_regexp=e.anchor_regexp,linear_string=e.linear_string,linear_regexp=e.linear_regexp,structure_state=e.structure_state,array_state=e.array_state,proxy_state=e.proxy_state,position_state=e.position_state,position=e.position;
return(function() {var statics=function() {;
return{syntax:$.merge($.syntax_subclass(tree_ctor() ,tree_methods() ) ,tree_statics() ) ,parse:parser() } } ,parser=function() {;
return(function() {var toplevel=function(xs) {;
return toplevel(xs) } ,toplevel=annotate(toplevel, 'toplevel' , [] ) ,term=function(xs) {;
return term(xs) } ,term=annotate(term, 'term' , [] ) ,maybe_ws=function(p) {;
return map(bfc(alt(bfs(linear_regexp( /\s+/ ) ,p) ,p) ,optional(linear_regexp( /\s+/ ) ) ) ,function(_) {return _[0] } ) } ,cons=map(manyc(maybe_ws(term) ) ,function(_) {return(function(xs) {var x,x0,xi,xl,xr;
for(var x0= ($.canard.syntax.nil() ) ,xi=0,xl=xs.length;
xi<xl;
 ++xi)x=xs[xi] ,x0= ($.canard.syntax.cons(x0,x) ) ;
return x0} ) .call(this,_) } ) ,atom=map(linear_regexp( /([^ \n\r\t\[\]]+)/ ) ,function(_) {return $.canard.syntax.atom(_[1] ) } ) ,list=map(bfc(linear_regexp( /\[/ ) ,cons,linear_regexp( /\]/ ) ) ,function(_) {return _[1] } ) ,term=alt(atom,list) ,toplevel=cons;
return function(_) {return toplevel( [new linear_string_state(_) ] ) [0] .value() } } ) .call(this) } ,tree_ctor=function() {;
return function(d,c1,c2) {;
return d instanceof this.constructor? (function(it) {return it.data=d.data,it.length=0,it} ) .call(this, (this) ) 
: (function(it) {return it.data=d,it.length=c1&&c2?2
:0,c1&& (it[0] =c1) ,c2&& (it[1] =c2) ,it} ) .call(this, (this) ) } } ,tree_statics=function() {;
return{nil:function() {;
return new $.canard.syntax( '[]' ) } ,atom:function(data) {;
return new $.canard.syntax(data) } ,cons:function(t,h) {;
return new $.canard.syntax( 'cons' ,t,h) } } } ,tree_methods=function() {;
return $.merge( {is_cons:function() {;
return this.length===2&&this.data=== 'cons' } ,h:function() {;
return this[1] } ,is_atom:function() {;
return!this.length} ,t:function() {;
return this[0] } ,is_quote:function() {;
return this.is_atom() && /^'/ .test(this.data) } ,name:function() {;
return this.data.replace( /^'/ , '' ) } ,is_nil:function() {;
return this.is_atom() &&this.data=== '[]' } ,is_n:function() {;
return this.is_atom() && /^-?\d+\.?\d*([eE][-+]?\d+)?$/ .test(this.data) } ,toString:function() {;
return this.is_nil() ? '[]' 
:this.is_cons() && !this.t() .is_nil() &&this.h() .is_cons() ? ( '' + (this.t() ) + ' [' + (this.h() ) + ']' ) 
:this.is_cons() &&this.t() .is_nil() ?this.h() .toString() 
:this.is_cons() ? ( '' + (this.t() ) + ' ' + (this.h() ) + '' ) 
:this.data} } ,interpreter_methods() ) } ,interpreter_methods=function() {;
return{should_run:function() {;
return this.is_atom() && !this.is_quote() && !this.is_n() } ,execute:function(stack,bindings) {;
return this.should_run() ?bindings[this.name() ] .interpret(stack,bindings) 
: {h:this,t:stack} } ,interpret:function(stack,bindings) {;
return this.is_cons() ?this.t() .interpret(this.h() .execute(stack,bindings) ,bindings) 
:this.should_run() ?bindings[this.name() ] .interpret(stack,bindings) 
: {h:this,t:stack} } } } ,default_bindings=function() {;
return $.merge( (function(it) {return it[ '[]' ] =it.id,it} ) .call(this, ( (function(xs) {var x,x0,xi,xl,xr;
var xr=new xs.constructor() ;
for(var k in xs)if(Object.prototype.hasOwnProperty.call(xs,k) )x=xs[k] ,xr[k] = ( {interpret:x} ) ;
return xr} ) .call(this, {def:function(stack,bindings) {;
return(bindings[stack.h.name() ] =stack.t.h,stack.t.t) } ,log:function(stack) {;
return(console.log(stack.h.toString() ) ,stack.t) } ,trace:function(stack) {;
return(console.log(stack.h.toString() ) ,stack) } ,chr:function(stack) {;
return{h:$.canard.syntax.atom(String.fromCharCode( +stack.h.data) ) ,t:stack.t} } ,ord:function(stack) {;
return{h:$.canard.syntax.atom(stack.h.data.charCodeAt(0) ) ,t:stack.t} } ,q:function(stack,bindings) {;
return+stack.t.t.h.data?stack.h.interpret(stack.t.t.t,bindings) 
:stack.t.h.interpret(stack.t.t.t,bindings) } ,not:function(stack) {;
return{h:$.canard.syntax.atom( + !stack.h.data) ,t:stack.t} } ,dup:function(stack) {;
return{h:stack.h,t:stack} } ,drop:function(stack) {;
return stack.t} ,nb:function(stack) {;
return stack.t} ,cons:function(stack) {;
return{h:$.canard.syntax.cons(stack.t.h,stack.h) ,t:stack.t.t} } ,uncons:function(stack) {;
return( !stack.h.is_cons() && (function() {throw new Error( ( '' + (stack.h) + ' is not a cons cell' ) ) } ) .call(this) , {h:stack.h.t() ,t: {h:stack.h.h() ,t:stack.t} } ) } ,id:function(stack) {;
return stack} ,i:function(stack,bindings) {;
return stack.h.interpret(stack.t,bindings) } } ) ) ) ,arithmetic_bindings() ) } ,arithmetic_bindings=function() {;
return(function(o) {for(var r= {} ,i=0,l=o.length,x;
i<l;
 ++i)x=o[i] ,r[x[0] ] =x[1] ;
return r} ) .call(this, ( (function(xs) {var x,x0,xi,xl,xr;
for(var xr=new xs.constructor() ,xi=0,xl=xs.length;
xi<xl;
 ++xi)x=xs[xi] ,xr.push( ( [x, {interpret:$.compile($.parse( ( "function (stack) {return {h: caterwaul.canard.syntax.atom(+stack.t.h.data " + (x) + " +stack.h.data), t: stack.t.t}}" ) ) ) } ] ) ) ;
return xr} ) .call(this, [ '+' , '-' , '*' , '/' , '%' , '>>' , '<<' , '>>>' , '|' , '&' , '^' ] ) ) ) } ;
return $.merge( ($.canard=function(source,stack,bindings) {;
return $.canard.parse(source) .interpret(stack||null,$.merge( {} ,default_bindings() ,bindings) ) } ) ,statics() ) } ) .call(this) } ) .call(this) } ) ;
result.caterwaul_expression_ref_table= {e: ( "caterwaul.parser" ) } ;
return(result) } ) .call(this,caterwaul.parser) ) ;
