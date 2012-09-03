package canard;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;

public class Repl extends Interpreter {
  public boolean verbose = false;

  public Repl(final String[] args) {
    super(Bootstrap.loadedResolver());

    boolean interactive = false;
    boolean files = false;

    for (final String arg : args) {
      if ("-v".equals(arg)) verbose = true;
      else if ("-i".equals(arg)) interactive = true;
      else
        try {
          files = true;
          push(Reader.read(new FileReader(arg)));

          final int priorDepth = dataStackPointer;
          this.apply(this);
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
      System.err.print("\033[1;32m" + (previousInput.length() == 0 ? "> " : "... ") + "\033[0;0m");

      try {
        final String line = in.readLine();
        if (line.length() == 0) {
          previousInput = "";
        } else {
          previousInput += line;
          try {
            push(Reader.read(new StringReader(previousInput)));
            previousInput = "";
            this.apply(this);
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

  @Override public void execute(final Fn f) {
    if (verbose) {
      System.err.println("\033[1;33mexecuting " + f + "\033[0;0m");
      System.err.println("\033[1;33mresolver is " + resolver() + "\033[0;0m");
      printStackState();
      System.err.println("");
    }
    super.execute(f);
  }

  public static String toString(final Object o) {
    if (o == null) return "[]";
    else           return o.toString();
  }

  public void printStackState() {
    for (int i = dataStackPointer - 1; i >= 0; --i)
      System.err.println((dataStackPointer - i - 1) + "   \033[1;34m" +
                         toString(dataStack[i]) + "\033[0;0m");

    for (int i = returnStackPointer - 1; i >= 0; --i)
      System.err.println((returnStackPointer - i - 1) + "   \033[1;32m" +
                         toString(returnStack[i]) + "\033[0;0m");
  }

  public static void main(final String[] args) {
    new Repl(args);
  }
}
