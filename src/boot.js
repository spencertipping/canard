caterwaul.module( 'canard' , (function(e1) {var result= (function($) { (function() {var annotate=e1.annotate,parsers=e1.parsers,bfs=e1.bfs,bfc=e1.bfc,state_matrix=e1.state_matrix,step_matrix_mutable=e1.step_matrix_mutable,step_matrix_immutable=e1.step_matrix_immutable,row_composite_states_from=e1.row_composite_states_from,alt=e1.alt,all=e1.all,manyc=e1.manyc,manyc_one=e1.manyc_one,many=e1.many,optional=e1.optional,step_matrix_immutable_null=e1.step_matrix_immutable_null,step_matrix_mutable_null=e1.step_matrix_mutable_null,has_non_null_states=e1.has_non_null_states,row_null_states_from=e1.row_null_states_from,zero=e1.zero,fail=e1.fail,match=e1.match,reject=e1.reject,pluralize=e1.pluralize,iv=e1.iv,map=e1.map,flat_map=e1.flat_map,map_state=e1.map_state,flat_map_state=e1.flat_map_state,logical_state=e1.logical_state,linear_string_state=e1.linear_string_state,anchor_regexp=e1.anchor_regexp,linear_string=e1.linear_string,linear_regexp=e1.linear_regexp,structure_state=e1.structure_state,array_state=e1.array_state,proxy_state=e1.proxy_state,position_state=e1.position_state,position=e1.position;
return(function() {var statics=function() {;
return{syntax:$.merge($.syntax_subclass(tree_ctor() ,tree_methods() ) ,tree_statics() ) ,default_bindings:function() {;
return default_bindings() } ,parse:parser() } } ,parser=function() {;
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
return this.is_atom() &&this.data=== '[]' } ,unquote:function() {;
return this.is_atom() && /^'/ .test(this.data) ?$.canard.syntax.atom( (this.data) .substr(1) ) 
:this} ,quote:function() {;
return this.is_atom() ?$.canard.syntax.atom( ( "'" + (this.data) + "" ) ) 
:this} ,toString:function() {;
return this.is_nil() ? '[]' 
:this.is_cons() && !this.t() .is_nil() &&this.h() .is_cons() ? ( '' + (this.t() ) + ' [' + (this.h() ) + ']' ) 
:this.is_cons() &&this.t() .is_nil() ?this.h() .toString() 
:this.is_cons() ? ( '' + (this.t() ) + ' ' + (this.h() ) + '' ) 
:this.data} } ,interpreter_methods() ) } ,interpreter_methods=function() {;
return{should_run:function() {;
return!this.is_nil() &&this.is_atom() && !this.is_quote() } ,execute:function(stack,bindings) {;
return(function() {try{return this.should_run() ?bindings[this.name() ] .interpret(stack,bindings) 
: {h:this.unquote() ,t:stack} }catch(e) {return(function() {throw( '' + (this) + ':[e]\n' + (e) + '' ) } ) .call(this) } } ) .call(this) } ,interpret:function(stack,bindings) {;
return(function() {try{return this.is_cons() ?this.t() .interpret(this.h() .execute(stack,bindings) ,bindings) 
:this.is_nil() ?stack
:this.should_run() ?bindings[this.name() ] .interpret(stack,bindings) 
: {h:this.unquote() ,t:stack} }catch(e) {return(function() {throw( '' + (this) + ':[i]\n' + (e) + '' ) } ) .call(this) } } ) .call(this) } } } ,up=function(stack,n) {;
return n?up(stack.t,n-1) 
:stack} ,append_items_to=function(s,o,l) {;
return l.is_cons() ?append_items_to( {t:s,h:up(o, +l.h() .data) .h} ,o,l.t() ) 
:s} ,stash_helper=function(n,b,s,bs) {;
return n? {h:s.h,t:stash_helper(n-1,b,s.t,bs) } 
:b.interpret(s,bs) } ,stack_depth=function(s) {;
return s?1+stack_depth(s.t) 
:0} ,default_bindings=function() {;
return $.merge( (function(xs) {var x,x0,xi,xl,xr;
var xr=new xs.constructor() ;
for(var k1 in xs)if(Object.prototype.hasOwnProperty.call(xs,k1) )x=xs[k1] ,xr[k1] = ( {interpret:x} ) ;
return xr} ) .call(this, { '=' :function(stack,bindings) {;
return(bindings[stack.h.name() ] =stack.t.h,stack.t.t) } , '@' :function(stack,bindings) {;
return{h:bindings[stack.h.name() ] ,t:stack.t} } ,ls:function(stack,bindings) {;
return(process.stdout.write( (function(it) {return it.join( ' ' ) } ) .call(this, ( (function(o) {var ks= [] ;
for(var k in o)Object.prototype.hasOwnProperty.call(o,k) &&ks.push(k) ;
return ks} ) .call(this, (bindings) ) ) ) + '\n' ) ,stack) } ,depth:function(stack) {;
return{h:$.canard.syntax.atom(stack_depth(stack) ) ,t:stack} } ,clear:function(stack) {;
return null} , '$<' :function(stack) {;
return(console.log(stack.h.toString() ) ,stack.t) } , '$|' :function(stack) {;
return(console.log( ( '\033[1;32m' + (stack.h) + '\033[0;0m' ) ) ,stack) } , '#<' :function(stack) {;
return(process.stdout.write( (function(it) {return it[0] = +stack.h,it} ) .call(this, (new Buffer(1) ) ) ) ,stack.t) } , '$#' :function(stack) {;
return{h:$.canard.syntax.atom(stack.h.data.length) ,t:stack.t} } , '$' :function(stack) {;
return{h:$.canard.syntax.atom(String.fromCharCode( +stack.h) ) ,t:stack.t} } , '$^' :function(stack) {;
return{h:stack.t.h.data.charCodeAt( +stack.h.data) ,t:stack.t.t} } , '$+' :function(stack) {;
return{h:$.canard.syntax.atom(stack.h.data+stack.t.h.data) ,t:stack.t.t} } , '$:' :function(stack) {;
return{h:stack.h.quote() ,t:stack.t} } , '$.' :function(stack) {;
return{h:stack.h.unquote() ,t:stack.t} } , '$=' :function(stack) {;
return{h: + (stack.h.data===stack.t.h.data) ,t:stack.t.t} } , '?' :function(stack,bindings) {;
return+stack.t.t.h?stack.h.interpret(stack.t.t.t,bindings) 
:stack.t.h.interpret(stack.t.t.t,bindings) } , '%%' :function(stack) {;
return append_items_to(up(stack.t.t, +stack.h) ,stack.t.t,stack.t.h) } , '%^' :function(stack,bindings) {;
return stash_helper( +stack.h,stack.t.h,stack.t.t,bindings) } , '/?' :function(stack) {;
return{h: +stack.h.is_nil() ,t:stack.t} } , ':?' :function(stack) {;
return{h: +stack.h.is_cons() ,t:stack.t} } , '!' :function(stack) {;
return{h: + !stack.h,t:stack.t} } , '::' :function(stack) {;
return{h:$.canard.syntax.cons(stack.h,stack.t.h) ,t:stack.t.t} } , ':^' :function(stack) {;
return( !stack.h.is_cons() && (function() {throw new Error( ( '' + (stack.h) + ' is not a cons cell' ) ) } ) .call(this) , {h:stack.h.t() ,t: {h:stack.h.h() ,t:stack.t} } ) } , '.' :function(stack,bindings) {;
return stack.h.interpret(stack.t,bindings) } } ) ,numeric_bindings() ,arithmetic_bindings() ) } ,numeric_bindings=function() {;
return(function(o) {for(var r= {} ,i=0,l=o.length,x;
i<l;
 ++i)x=o[i] ,r[x[0] ] =x[1] ;
return r} ) .call(this, ( (function(xs) {var x,x0,xi,xl,xr;
for(var xr=new xs.constructor() ,xi=0,xl=xs.length;
xi<xl;
 ++xi)x=xs[xi] ,xr.push( ( [ '0123456789abcdef' .charAt(x>>>4) + '0123456789abcdef' .charAt(x&15) , {interpret: (function(x) {return(function(stack) {return{h:x,t:stack} } ) } ) (x) } ] ) ) ;
return xr} ) .call(this, (function(i,u,s) {if( (u-i) *s<=0)return[] ;
for(var r= [] ,d=u-i;
d>0?i<u
:i>u;
i+=s)r.push(i) ;
return r} ) ( (0) , (256) , (1) ) ) ) ) } ,arithmetic_bindings=function() {;
return(function(o) {for(var r= {} ,i=0,l=o.length,x;
i<l;
 ++i)x=o[i] ,r[x[0] ] =x[1] ;
return r} ) .call(this, ( (function(xs) {var x,x0,xi,xl,xr;
for(var xr=new xs.constructor() ,xi=0,xl=xs.length;
xi<xl;
 ++xi)x=xs[xi] ,xr.push( ( [x, {interpret:$.compile($.parse( ( "function (stack) {return {h: stack.t.h " + (x) + " stack.h, t: stack.t.t}}" ) ) ) } ] ) ) ;
return xr} ) .call(this, [ '+' , '-' , '*' , '/' , '%' , '>>' , '<<' , '>>>' , '|' , '&' , '^' , '<' , '>' , '<=' , '>=' , '===' , '!==' , '==' , '!=' ] ) ) ) } ;
return $.merge( ($.canard=function(source,stack,bindings) {;
return $.canard.parse(source) .interpret(stack||null,$.merge( {} ,$.canard.default_bindings() ,bindings) ) } ) ,statics() ) } ) .call(this) } ) .call(this) } ) ;
result.caterwaul_expression_ref_table= {e1: ( "caterwaul.parser" ) } ;
return(result) } ) .call(this,caterwaul.parser) ) ;
