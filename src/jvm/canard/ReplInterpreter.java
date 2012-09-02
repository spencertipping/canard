package canard;

import java.io.InputStreamReader;
import java.io.IOException;

public class ReplInterpreter extends BaseInterpreter {
  public ReplInterpreter() {
    super(Bootstrap.loadedResolver());
    repl();
  }

  private void repl() {
    ConcatenativeReader in = ConcatenativeReader.from(new InputStreamReader(System.in));
    while (in != null) {
      System.err.print("\033[1;32m> \033[0;0m");
      this.execute((Fn) in.first());
      printStackState();
      in = (ConcatenativeReader) in.next();
    }
    System.exit(0);
  }

  public void printStackState() {
    for (int i = dataStackPointer - 1; i >= 0; --i)
      System.err.println("[" + (dataStackPointer - i) + "] \033[1;34m" +
                         dataStack[i] + "\033[0;0m");
  }

  public static void main(final String[] args) {
    new ReplInterpreter();
  }
}
