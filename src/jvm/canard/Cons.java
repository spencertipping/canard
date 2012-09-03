package canard;

public class Cons implements Fn {
  public final Object head;
  public final Object tail;

  public Cons(final Object head, final Object tail) {
    this.head = head;
    this.tail = tail;
  }

  public static final Cons cons(final Object head, final Object tail) {
    return new Cons(head, tail);
  }

  public static final Cons list(final Object ... elements) {
    Cons result = null;
    for (int i = elements.length - 1; i >= 0; --i)
      result = cons(elements[i], result);
    return result;
  }

  public static final int count(Cons seq) {
    int i = 0;
    for (; seq != null; seq = (Cons) seq.tail, ++i);
    return i;
  }

  public static final Object[] toArray(Cons seq) {
    final int count = count(seq);
    final Object[] result = new Object[count];
    for (int i = 0; seq != null; seq = (Cons) seq.tail, ++i)
      result[i] = seq.head;
    return result;
  }

  @Override public void apply(Interpreter environment) {
    environment.push(this);
  }

  @Override public String toString() {
    final StringBuffer result = new StringBuffer("]");
    Object c = this;
    while (c instanceof Cons) {
      final Cons cell = (Cons) c;
      result.insert(0, cell.head + (cell == this ? "" : " "));
      c = cell.tail;
    }
    if (c != null) result.insert(0, c + " :: ");
    return result.insert(0, "[").toString();
  }

  @Override public boolean equals(final Object o) {
    return o instanceof Cons &&
           Stuff.eq(head, ((Cons) o).head) &&
           Stuff.eq(tail, ((Cons) o).tail);
  }
}
