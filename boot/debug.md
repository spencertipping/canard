# Canard bootstrap debugging script

GDB definitions to make it easier to debug the main image.

    break *0x400078

    define s
      ni
      x/4i $pc
      print_data_stack
      print_heap
    end

# Stack inspectors

Print the top few items on any given stack. Generally, all stack items will be
pointers, so dereference those.

    define print_data_stack
      printf "data stack: %lx\n", $rdi
      set $x = $rdi
      while $x > 0x400200
        set $x = $x - 8
        print_cell $x
      end
    end

    define print_heap
      printf "heap: %lx\n", $rsi
      x/i $rsi
      while $_ < 0x500000
        x/i
      end
    end

    define print_cell
      x/g $arg0
      if ($__ >= 0x400000 && $__ < 0x500000)
        x/i $__
      end
    end