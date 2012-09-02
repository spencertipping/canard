
package canard;

import clojure.lang.ISeq;
import clojure.lang.ASeq;
import clojure.lang.Symbol;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

public class ConcatenativeReader extends Cons implements Fn {
  public static final int SYMBOL_LENGTH_LIMIT = 256;

  private Object first;
  private ConcatenativeReader next;
  private boolean empty = false;

  private final transient char[] symbolBuffer;
  private transient PushbackReader input;

  private ConcatenativeReader(final PushbackReader input,
                              final char[] symbolBuffer) {
    this.input = input;
    this.symbolBuffer = symbolBuffer != null ? symbolBuffer
                                             : new char[SYMBOL_LENGTH_LIMIT];
  }

  public static ConcatenativeReader from(final Reader input) {
    // Always allocate a new buffer at this point, since we are unsure of the ownership.
    return new ConcatenativeReader(new PushbackReader(input), null);
  }

  private void force() {
    if (input != null)
      try {
        int c;
        while ((c = input.read()) <= ' ' && c != -1);

        if (c == '[') {
          // We need to force every sublist; we can't lazily parse those.
          final ConcatenativeReader sublist = new ConcatenativeReader(input, symbolBuffer);
          sublist.count();
          first = new Quote(sublist);
          next = new ConcatenativeReader(input, symbolBuffer);
        } else if (c == ']' || c == -1) {
          first = next = null;
          empty = true;
        } else if (c != -1) {
          int p = 0;
          while (c != -1 && c > ' ' && c != '[' && c != ']') {
            if (p >= SYMBOL_LENGTH_LIMIT)
              throw new RuntimeException("symbol too long: " + new String(symbolBuffer));
            symbolBuffer[p++] = (char) c;
            c = input.read();
          }
          input.unread(c);
          first = new ExecutableSymbol(Symbol.intern("canard", new String(symbolBuffer, 0, p)));
          next = new ConcatenativeReader(input, symbolBuffer);
        }
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    input = null;
  }

  public boolean isForced() {
    return input == null;
  }

  public boolean isSpurious() {
    force();
    return empty;
  }

  @Override public Object first() {
    force();
    return first;
  }

  @Override public ISeq next() {
    force();
    return next;
  }

  @Override public String toString() {
    if (isSpurious()) return "";

    ConcatenativeReader r = (ConcatenativeReader) next();
    final StringBuffer result = new StringBuffer("[" + first());
    while (r != null && r.isForced() && !r.isSpurious()) {
      result.append(" " + r.first());
      r = (ConcatenativeReader) r.next();
    }
    if (r != null) result.append("...");
    return result.append("]").toString();
  }
}
