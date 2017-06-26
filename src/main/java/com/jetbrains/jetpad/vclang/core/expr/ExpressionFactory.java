package com.jetbrains.jetpad.vclang.core.expr;

import com.jetbrains.jetpad.vclang.core.context.binding.Binding;
import com.jetbrains.jetpad.vclang.core.context.param.*;
import com.jetbrains.jetpad.vclang.core.definition.ClassField;
import com.jetbrains.jetpad.vclang.core.expr.type.Type;
import com.jetbrains.jetpad.vclang.core.internal.FieldSet;
import com.jetbrains.jetpad.vclang.core.pattern.elimtree.ElimTreeNode;
import com.jetbrains.jetpad.vclang.core.sort.Sort;
import com.jetbrains.jetpad.vclang.term.Prelude;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExpressionFactory {
  public static Expression Apps(Expression function, Expression... arguments) {
    if (arguments.length == 0) {
      return function;
    }
    Expression result = function;
    for (Expression argument : arguments) {
      result = new AppExpression(result, argument);
    }
    return result;
  }

  public static Expression FieldCall(ClassField definition, Expression thisExpr) {
    if (thisExpr.toNew() != null) {
      FieldSet.Implementation impl = thisExpr.toNew().getExpression().getFieldSet().getImplementation(definition);
      assert impl != null;
      return impl.term;
    } else
    if (thisExpr.toError() != null && thisExpr.toError().getExpr() != null) {
      return new FieldCallExpression(definition, new ErrorExpression(null, thisExpr.toError().getError()));
    } else {
      return new FieldCallExpression(definition, thisExpr);
    }
  }

  public static DataCallExpression Interval() {
    return new DataCallExpression(Prelude.INTERVAL, Sort.PROP, Collections.emptyList());
  }

  public static ConCallExpression Left() {
    return new ConCallExpression(Prelude.LEFT, Sort.PROP, Collections.emptyList(), Collections.emptyList());
  }

  public static ConCallExpression Right() {
    return new ConCallExpression(Prelude.RIGHT, Sort.PROP, Collections.emptyList(), Collections.emptyList());
  }

  public static DependentLink parameter(boolean explicit, String var, Type type) {
    return new TypedDependentLink(explicit, var, type, EmptyDependentLink.getInstance());
  }

  public static TypedDependentLink parameter(String var, Type type) {
    return new TypedDependentLink(true, var, type, EmptyDependentLink.getInstance());
  }

  public static DependentLink parameter(boolean explicit, List<String> names, Type type) {
    DependentLink link = new TypedDependentLink(explicit, names.get(names.size() - 1), type, EmptyDependentLink.getInstance());
    for (int i = names.size() - 2; i >= 0; i--) {
      link = new UntypedDependentLink(names.get(i), link);
    }
    return link;
  }

  public static SingleDependentLink singleParams(boolean explicit, List<String> names, Type type) {
    SingleDependentLink link = new TypedSingleDependentLink(explicit, names.get(names.size() - 1), type);
    for (int i = names.size() - 2; i >= 0; i--) {
      link = new UntypedSingleDependentLink(names.get(i), link);
    }
    return link;
  }

  public static DataCallExpression Nat() {
    return new DataCallExpression(Prelude.NAT, Sort.SET0, Collections.emptyList());
  }

  public static ConCallExpression Zero() {
    return new ConCallExpression(Prelude.ZERO, Sort.SET0, Collections.emptyList(), Collections.emptyList());
  }

  public static ConCallExpression Suc(Expression expr) {
    return new ConCallExpression(Prelude.SUC, Sort.SET0, Collections.emptyList(), Collections.singletonList(expr));
  }

  public static ElimTreeNode top(DependentLink parameters, ElimTreeNode tree) {
    tree.updateLeavesMatched(DependentLink.Helper.toContext(parameters));
    return tree;
  }

  public static ElimTreeNode top(List<SingleDependentLink> parameters, ElimTreeNode tree) {
    List<Binding> context = new ArrayList<>();
    for (SingleDependentLink link : parameters) {
      context.addAll(DependentLink.Helper.toContext(link));
    }
    tree.updateLeavesMatched(context);
    return tree;
  }
}
