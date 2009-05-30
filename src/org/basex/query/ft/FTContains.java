package org.basex.query.ft;

import static org.basex.query.QueryText.*;
import java.io.IOException;
import org.basex.data.Serializer;
import org.basex.ft.Tokenizer;
import org.basex.query.IndexContext;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.expr.CmpG;
import org.basex.query.expr.Expr;
import org.basex.query.expr.Return;
import org.basex.query.item.Bln;
import org.basex.query.item.DBNode;
import org.basex.query.item.Item;
import org.basex.query.item.Type;
import org.basex.query.iter.Iter;
import org.basex.query.path.AxisPath;
import org.basex.query.path.Step;
import org.basex.query.util.Var;
import org.basex.util.IntList;

/**
 * FTContains expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public class FTContains extends Expr {
  /** Expression. */
  public Expr expr;
  /** Full-text expression. */
  public FTExpr ftexpr;
  /** Full-text parser. */
  public Tokenizer ft = new Tokenizer();
  /** Flag for first evaluation.*/
  private int div;
  
  /**
   * Constructor.
   * @param e expression
   * @param fte full-text expression
   */
  public FTContains(final Expr e, final FTExpr fte) {
    expr = e;
    ftexpr = fte;
  }
  
  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    expr = expr.comp(ctx).addText(ctx);
    ftexpr = ftexpr.comp(ctx);
    
    Expr e = this;
    if(expr.e()) e = Bln.FALSE;
    if(e != this) ctx.compInfo(OPTPRE, this);
    return e;
  }

  @Override
  public Bln atomic(final QueryContext ctx) throws QueryException {    
    if(div == 0) div = ++ctx.ftcount;
    
    final Iter iter = expr.iter(ctx);
    final Tokenizer tmp = ctx.fttoken;
    final IntList[] ftd = ctx.ftd;
    ctx.fttoken = ft;
    double s = 0;
    Item i;

    while((i = iter.next()) != null) {
      ft.init(i.str());
      final double d = ftexpr.atomic(ctx).score();
      s = ctx.score.and(s, d);

      // add entry to visualization
      if(ctx.ftpos != null && ctx.ftd != null && d > 0 && i instanceof DBNode)
        ctx.ftpos.add(ctx.ftd, ((DBNode) i).pre, div);
    }
    
    ctx.fttoken = tmp;
    ctx.ftd = ftd;
    return Bln.get(s);
  }

  @Override
  public boolean indexAccessible(final IndexContext ic) throws QueryException {
    // return if step is no text node, or if no index is available
    final Step s = CmpG.indexStep(expr);
    return s != null && ic.data.meta.ftxindex && s.test.type == Type.TXT &&
      ftexpr.indexAccessible(ic);
  }
  
  @Override
  public Expr indexEquivalent(final IndexContext ic) throws QueryException {
    ic.ctx.compInfo(OPTFTXINDEX);

    final FTExpr ie = ftexpr.indexEquivalent(ic);

    // sequential evaluation with index access
    if(ic.seq) return new FTContainsSIndex(expr, ie, !ic.ftnot);

    // standard index evaluation; first expression will always be an axis path
    return ((AxisPath) expr).invertPath(new FTIndexAccess(ie, ft, ic), ic.step);
  }
  
  @Override
  public boolean uses(final Use u, final QueryContext ctx) {
    return expr.uses(u, ctx) || ftexpr.uses(u, ctx);
  }

  @Override
  public boolean removable(final Var v, final QueryContext ctx) {
    return expr.removable(v, ctx) && ftexpr.removable(v, ctx);
  }

  @Override
  public Expr remove(final Var v) {
    expr = expr.remove(v);
    ftexpr = ftexpr.remove(v);
    return this;
  }

  @Override
  public Return returned(final QueryContext ctx) {
    return Return.BLN;
  }

  @Override
  public String color() {
    return "33CC33";
  }

  @Override
  public void plan(final Serializer ser) throws IOException {
    ser.openElement(this);
    expr.plan(ser);
    ftexpr.plan(ser);
    ser.closeElement();
  }

  @Override
  public String toString() {
    return expr + " ftcontains " + ftexpr;
  }
}
