package com.jetbrains.jetpad.vclang.term.expr;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.expr.visitor.AbstractExpressionVisitor;
import com.jetbrains.jetpad.vclang.term.expr.visitor.ExpressionVisitor;

import java.util.List;

public class ElimExpression extends Expression implements Abstract.ElimExpression {
  private final ElimType myElimType;
  private final Expression myExpression;
  private final List<Clause> myClauses;

  public ElimExpression(ElimType elimType, Expression expression, List<Clause> clauses) {
    myElimType = elimType;
    myExpression = expression;
    myClauses = clauses;
  }

  @Override
  public ElimType getElimType() {
    return myElimType;
  }

  @Override
  public Expression getExpression() {
    return myExpression;
  }

  @Override
  public List<Clause> getClauses() {
    return myClauses;
  }

  @Override
  public Clause getClause(int index) {
    return myClauses.get(index);
  }

  @Override
  public <T> T accept(ExpressionVisitor<? extends T> visitor) {
    return visitor.visitElim(this);
  }

  @Override
  public <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, P params) {
    return visitor.visitElim(this, params);
  }
}