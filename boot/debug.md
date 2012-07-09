# Canard bootstrap debugging script

GDB definitions to make it easier to debug the main image.

    break *0x40009c
    run

    set $stack_end = 0

    define s
      ni

      if $stack_end == 0
        set $stack_end = $rsp
      end

      print_data_stack
      print_return_stack
      print_heap

      printf "%c[1;32minstruction queue%c[1;30m\n", 27, 27
      x/8i $pc
      printf "%c[0;0m", 27
    end

# Stack inspectors

Print the top few items on any given stack. Generally, all stack items will be
pointers, so dereference those.

    define print_data_stack
      printf "%c[1;32mdata stack%c[1;30m: %lx\n", 27, 27, $rdi
      set $x = $rdi
      set $lower = 0x400000 + *0x400060
      while $x > $lower
        set $x = $x - 8
        print_cell $x
      end
    end

    define print_return_stack
      printf "%c[1;32mreturn stack%c[1;30m: %lx\n", 27, 27, $rsp
      set $x = $rsp
      while $x < $stack_end
        print_cell $x
        set $x = $x + 8
      end
    end

    define print_heap
      printf "%c[1;32mheap%c[1;30m: %lx\n", 27, 27, $rsi
      x/i $rsi
      while $_ < 0x4fffff
        x/i
      end
    end

    define print_cell
      if $arg0 >= 0x400000 && $arg0 < 0x500000
        x/g $arg0
        if $__ >= 0x400000 && $__ < 0x500000
          x/i $__
        end
      end
    end