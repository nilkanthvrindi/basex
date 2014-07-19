package org.basex.gui.view.table;

import org.basex.data.*;

/**
 * This is an iterator for parsing the rows' contents.
 *
 * @author BaseX Team 2005-14, BSD License
 * @author Christian Gruen
 */
final class TableIterator {
  /** Table data. */
  private final TableData tdata;
  /** Data reference. */
  private final Data data;
  /** Current pre value. */
  private int last;
  /** Current element. */
  private int elem;
  /** Root element. */
  private int rootElem;

  /** Current pre value. */
  int pre;
  /** Current column. */
  int col;
  /** Element flag. */
  boolean text;

  /**
   * Default constructor.
   * @param d data reference
   * @param td table data
   */
  TableIterator(final Data d, final TableData td) {
    data = d;
    tdata = td;
  }

  /**
   * Initializes the iterator.
   * @param p pre value to start from (must be an element node)
   */
  void init(final int p) {
    last = p + data.size(p, data.kind(p));
    rootElem = data.name(p);
    pre = p;
    elem = -1;
  }

  /**
   * Checks if more values are found.
   * @return result of check
   */
  boolean more() {
    while(++pre < last) {
      final int k = data.kind(pre);
      text = k == Data.TEXT;

      // content found...
      if(text || k == Data.ATTR) {
        final int id = text ? elem : data.name(pre);
        // find correct column...
        for(col = 0; col < tdata.cols.length; ++col) {
          if(tdata.cols[col].id == id && tdata.cols[col].elem == text) {
            return true;
          }
        }
      } else if(k == Data.ELEM) {
        // remember name of last element
        elem = data.name(pre);
        if(elem == rootElem) return false;
      }
    }
    return false;
  }
}
