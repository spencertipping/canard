package canard;

import clojure.lang.ISeq;
import clojure.lang.Symbol;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;

import java.util.Stack;

public class ApplicativeReader {
  public static Object read(final Reader source) throws IOException {
    final PushbackReader input = new PushbackReader(source);
    final Stack<Object> values = new Stack<Object>();
    values.push(null);

    char c;
    while ((c = (char) input.read()) != 65535) {
      System.out.println("char = " + (int) c);
      while (c <= ' ' && c != 65535) c = (char) input.read();

      if (c == '[') values.push(null);
      else if (c == ']') {
        final Object head = values.pop();
        final Object rest = values.pop();
        values.push(new Quote(Cons.cons(head, (ISeq) rest)));
      } else {
        final StringBuffer symbol = new StringBuffer();
        symbol.append(c);
        while ((c = (char) input.read()) != 65535 && c != '[' && c != ']' && c > ' ')
          symbol.append(c);
        input.unread(c);
        values.push(Cons.cons(Symbol.intern("canard", symbol.toString()), (ISeq) values.pop()));
      }
    }

    if (values.size() > 1) throw new RuntimeException("unbalanced open bracket");
    if (values.size() == 0) throw new RuntimeException("unbalanced close bracket");
    return values.pop();
  }
}
