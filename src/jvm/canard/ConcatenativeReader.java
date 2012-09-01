
package canard;

import clojure.lang.ISeq;
import clojure.lang.ASeq;
import clojure.lang.Symbol;

import java.io.IOException;
import java.io.Reader;

public class ConcatenativeReader extends Cons implements Fn {
  public static final int SYMBOL_LENGTH_LIMIT = 256;

  private Object first;
  private ConcatenativeReader next;
  private boolean empty = false;

  private final transient char[] symbolBuffer;
  private transient Reader input;

  private ConcatenativeReader(final Reader input,
                              final char[] symbolBuffer) {
    this.input = input;
    this.symbolBuffer = symbolBuffer != null ? symbolBuffer
                                             : new char[SYMBOL_LENGTH_LIMIT];
  }

  public static ConcatenativeReader from(final Reader input) {
    // Always allocate a new buffer at this point, since we are unsure of the ownership.
    return new ConcatenativeReader(input, null);
  }

  private void force() {
    if (input != null)
      try {
        int c;
        while ((c = input.read()) <= ' ' && c != -1);

        if (c == '[') {
          // We need to force every sublist; we can't lazily parse those.
          ((ConcatenativeReader)
           (first = new Quote(new ConcatenativeReader(input, symbolBuffer))).value).count();
          next = new ConcatenativeReader(input, symbolBuffer);
        } else if (c == ']') {
          first = next = null;
          empty = true;
        } else {
          int p = 0;
          while (c > ' ' && c != '[' && c != ']' && p < SYMBOL_LENGTH_LIMIT) {
            symbolBuffer[p++] = (char) c;
            c = input.read();
          }
          first = new ExecutableSymbol(Symbol.intern(new String(symbolBuffer, 0, p)));
          next = new ConcatenativeReader(input, symbolBuffer);
        }
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    input = null;
  }

  @Override public Object first() {
    force();
    if (empty)
      throw new UnsupportedOperationException("cannot retrieve first element of an empty sequence");
    else
      return first;
  }

  @Override public ISeq next() {
    force();
    return next;
  }
}
