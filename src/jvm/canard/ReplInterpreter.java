package canard;

import java.io.InputStreamReader;
import java.io.IOException;

public class ReplInterpreter extends DebugInterpreter {
  public ReplInterpreter() {
    super(Bootstrap.loadedResolver());

    System.err.println("Canard REPL | Spencer Tipping");
    System.err.println("Licensed under the terms of the MIT source code license");
    System.err.println("https://github.com/spencertipping/canard");
    repl();
  }

  private void repl() {
    ConcatenativeReader in = ConcatenativeReader.from(new InputStreamReader(System.in));
    while (in != null) {
      System.err.print("\033[1;32m> \033[0;0m");
      push(in.first());
      this.apply(this);
      printStackState();
      in = (ConcatenativeReader) in.next();
    }
    System.exit(0);
  }

  public void printStackState() {
    System.err.println("\033[1;32m" + returnStackPointer + "\033[0;0m");
    for (int i = dataStackPointer - 1; i >= 0; --i)
      System.err.println((dataStackPointer - i) + "   \033[1;34m" +
                         dataStack[i] + "\033[0;0m");
  }

  public static void main(final String[] args) {
    new ReplInterpreter();
  }
}
