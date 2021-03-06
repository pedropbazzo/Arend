package org.arend.extImpl;

import org.arend.core.expr.Expression;
import org.arend.ext.concrete.expr.ConcreteArgument;
import org.arend.ext.core.expr.CoreExpression;
import org.arend.ext.typechecking.ContextData;
import org.arend.term.concrete.Concrete;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ContextDataImpl implements ContextData {
  private final Concrete.ReferenceExpression myExpression;
  private List<? extends ConcreteArgument> myArguments;
  private Expression myExpectedType;

  public ContextDataImpl(Concrete.ReferenceExpression expression, List<? extends ConcreteArgument> arguments, Expression expectedType) {
    myExpression = expression;
    myArguments = arguments;
    myExpectedType = expectedType;
  }

  @NotNull
  @Override
  public Concrete.ReferenceExpression getReferenceExpression() {
    return myExpression;
  }

  @NotNull
  @Override
  public List<? extends ConcreteArgument> getArguments() {
    return myArguments;
  }

  @Override
  public void setArguments(@NotNull List<? extends ConcreteArgument> arguments) {
    myArguments = arguments;
  }

  @Override
  public Expression getExpectedType() {
    return myExpectedType;
  }

  @Override
  public void setExpectedType(@Nullable CoreExpression expectedType) {
    if (!(expectedType instanceof Expression)) {
      throw new IllegalArgumentException();
    }
    myExpectedType = (Expression) expectedType;
  }
}
