package canard;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;

public class ReplInterpreter extends BaseInterpreter {
  public boolean verbose = false;

  public ReplInterpreter(final String[] args) {
    super(Bootstrap.loadedResolver());

    boolean interactive = false;
    boolean files = false;

    for (final String arg : args) {
      if ("-v".equals(arg)) verbose = true;
      else if ("-i".equals(arg)) interactive = true;
      else
        try {
          files = true;
          push(ApplicativeReader.read(new FileReader(arg)));
          this.apply(this);
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
            push(ApplicativeReader.read(new StringReader(previousInput)));
            previousInput = "";
            this.apply(this);
            if (verbose) printStackState();
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

  public void printStackState() {
    System.err.println("\033[1;32m" + returnStackPointer + "\033[0;0m");
    for (int i = dataStackPointer - 1; i >= 0; --i)
      System.err.println((dataStackPointer - i - 1) + "   \033[1;34m" +
                         dataStack[i] + "\033[0;0m");
  }

  public static void main(final String[] args) {
    new ReplInterpreter(args);
  }
}
