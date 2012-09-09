package canard;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;

public class Repl extends Interpreter {
  public boolean verbose         = false;
  public boolean resolverVerbose = false;
  public boolean timing          = false;

  public long executeCount = 0;

  private int resolverDepth = 0;

  public Repl(final String[] args) {
    super(Bootstrap.loadedResolver());

    boolean interactive = false;
    boolean files = false;

    for (final String arg : args) {
      if ("-v".equals(arg))      {resolverVerbose = verbose; verbose = true;}
      else if ("-t".equals(arg)) timing      = true;
      else if ("-i".equals(arg)) interactive = true;
      else
        try {
          files = true;
          final int priorDepth = dataStackPointer;
          final long readStart = System.currentTimeMillis();
          push(Reader.read(new FileReader(arg)));
          final long runStart = System.currentTimeMillis();
          this.apply(this);

          if (verbose || timing) {
            final long now = System.currentTimeMillis();
            System.err.println(arg + ": " + (runStart - readStart) + "ms read; " +
                                            (now - runStart)       + "ms run");
          }

          if (dataStackPointer != priorDepth)
            System.err.println("\033[1;33mwarning: executing " + arg +
                               " resulted in a stack delta of " + (dataStackPointer - priorDepth) +
                               "\033[0;0m");
        } catch (final IOException e) {
          System.err.println("failed to read input file " + arg);
          System.exit(1);
        }
    }

    if (!files || interactive) {
      System.err.println("Canard REPL | Spencer Tipping");
      System.err.println("Licensed under the terms of the MIT source code license");
      System.err.println("https://github.com/spencertipping/canard");
      repl();
    }
  }

  private void repl() {
    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String previousInput = "";

    while (in != null) {
      System.err.print("\033[1;32m" + (previousInput.length() == 0 ? executeCount + "> " : "... ") + "\033[0;0m");

      try {
        final String line = in.readLine();
        if (line == null) {
          System.exit(0);
        } else if (line.length() == 0) {
          previousInput = "";
          printStackState(false);
        } else {
          previousInput += line;
          try {
            push(Reader.read(new StringReader(previousInput)));
            previousInput = "";
            this.apply(this);
            printStackState(false);
          } catch (final RuntimeException e) {
            System.err.println("\033[1;31m" + e + "\033[0;0m");
          }
        }
      } catch (final IOException e) {
        System.exit(1);
      }
    }
    System.exit(0);
  }

  @Override public Fn resolver() {
    if (verbose && !resolverVerbose) {
      final Fn originalResolver = super.resolver();
      return new NamedFn("wrapped-resolver") {
          @Override public void apply(final Interpreter environment) {
            ++resolverDepth;
            originalResolver.apply(environment);
            --resolverDepth;
          }
        };
    } else
      return super.resolver();
  }

  @Override public void execute(final Fn f) {
    if (shouldBeVerbose()) {
      System.err.println("\033[1;33mexecuting " + f + "\033[0;0m");
      System.err.println("\033[1;33mresolver is " + resolver() + "\033[0;0m");
      printStackState(false);
      System.err.println("");
    }
    ++executeCount;
    super.execute(f);
  }

  @Override public Object pop() {
    if (dataStackPointer == 0) {
      printStackState(true);
      throw new RuntimeException("trying to pop an empty stack");
    }

    return super.pop();
  }

  @Override public Object at(final int i) {
    try {
      return super.at(i);
    } catch (final ArrayIndexOutOfBoundsException e) {
      printStackState(true);
      throw e;
    }
  }

  @Override public Fn rpop() {
    if (returnStackPointer == 0) {
      printStackState(true);
      throw new RuntimeException("trying to rpop an empty return stack");
    }

    return super.rpop();
  }

  public void printStackState(final boolean beVerboseAnyway) {
    for (int i = dataStackPointer - 1; i >= 0; --i)
      System.err.println((dataStackPointer - i - 1) + "   \033[1;34m" +
                         Stuff.s(dataStack[i]) + "\033[0;0m");

    if (shouldBeVerbose() || beVerboseAnyway)
      for (int i = returnStackPointer - 1; i >= 0; --i)
        System.err.println((returnStackPointer - i - 1) + "   \033[1;32m" +
                           Stuff.s(returnStack[i]) + "\033[0;0m");
  }

  public boolean shouldBeVerbose() {
    return resolverDepth == 0 ? verbose : resolverVerbose;
  }

  public static void main(final String[] args) {
    new Repl(args);
  }
}
