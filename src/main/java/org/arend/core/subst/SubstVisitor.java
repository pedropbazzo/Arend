package org.arend.core.subst;

import org.arend.core.constructor.ClassConstructor;
import org.arend.core.context.binding.Binding;
import org.arend.core.context.binding.EvaluatingBinding;
import org.arend.core.context.binding.inference.MetaInferenceVariable;
import org.arend.core.context.param.DependentLink;
import org.arend.core.context.param.SingleDependentLink;
import org.arend.core.definition.ClassField;
import org.arend.core.elimtree.BranchElimTree;
import org.arend.core.elimtree.ElimTree;
import org.arend.core.elimtree.LeafElimTree;
import org.arend.core.expr.*;
import org.arend.core.expr.let.LetClause;
import org.arend.core.expr.visitor.BaseExpressionVisitor;
import org.arend.ext.core.elimtree.CoreBranchKey;

import java.util.*;

public class SubstVisitor extends BaseExpressionVisitor<Void, Expression> {
  private final ExprSubstitution myExprSubstitution;
  private final LevelSubstitution myLevelSubstitution;

  public static class SubstException extends RuntimeException {}

  public SubstVisitor(ExprSubstitution exprSubstitution, LevelSubstitution levelSubstitution) {
    myExprSubstitution = exprSubstitution;
    myLevelSubstitution = levelSubstitution;
  }

  public ExprSubstitution getExprSubstitution() {
    return myExprSubstitution;
  }

  public LevelSubstitution getLevelSubstitution() {
    return myLevelSubstitution;
  }

  public boolean isEmpty() {
    return myExprSubstitution.isEmpty() && myLevelSubstitution.isEmpty();
  }

  @Override
  public Expression visitApp(AppExpression expr, Void params) {
    return AppExpression.make(expr.getFunction().accept(this, null), expr.getArgument().accept(this, null));
  }

  @Override
  public Expression visitDefCall(DefCallExpression expr, Void params) {
    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }
    return expr.getDefinition().getDefCall(expr.getSortArgument().subst(myLevelSubstitution), args);
  }

  @Override
  public Expression visitConCall(ConCallExpression expr, Void params) {
    List<Expression> dataTypeArgs = new ArrayList<>(expr.getDataTypeArguments().size());
    for (Expression parameter : expr.getDataTypeArguments()) {
      dataTypeArgs.add(parameter.accept(this, null));
    }

    List<Expression> args = new ArrayList<>(expr.getDefCallArguments().size());
    for (Expression arg : expr.getDefCallArguments()) {
      args.add(arg.accept(this, null));
    }

    return new ConCallExpression(expr.getDefinition(), expr.getSortArgument().subst(myLevelSubstitution), dataTypeArgs, args);
  }

  @Override
  public Expression visitClassCall(ClassCallExpression expr, Void params) {
    Map<ClassField, Expression> fieldSet = new HashMap<>();
    ClassCallExpression result = new ClassCallExpression(expr.getDefinition(), expr.getSortArgument().subst(myLevelSubstitution), fieldSet, expr.getSort().subst(myLevelSubstitution), expr.hasUniverses());
    if (expr.getImplementedHere().isEmpty()) {
      return result;
    }

    myExprSubstitution.add(expr.getThisBinding(), new ReferenceExpression(result.getThisBinding()));
    for (Map.Entry<ClassField, Expression> entry : expr.getImplementedHere().entrySet()) {
      fieldSet.put(entry.getKey(), entry.getValue().accept(this, null));
    }
    myExprSubstitution.remove(expr.getThisBinding());
    return result;
  }

  @Override
  public Expression visitFieldCall(FieldCallExpression expr, Void params) {
    Expression result = myExprSubstitution.get(expr.getDefinition());
    if (result != null) {
      return AppExpression.make(result, expr.getArgument().accept(this, null));
    } else {
      return FieldCallExpression.make(expr.getDefinition(), expr.getSortArgument().subst(myLevelSubstitution), expr.getArgument().accept(this, null));
    }
  }

  @Override
  public Expression visitReference(ReferenceExpression expr, Void params) {
    Expression result = myExprSubstitution.get(expr.getBinding());
    if (result != null) {
      return result;
    }
    if (expr.getBinding() instanceof EvaluatingBinding) {
      ((EvaluatingBinding) expr.getBinding()).subst(this);
    }
    return expr;
  }

  @Override
  public Expression visitInferenceReference(InferenceReferenceExpression expr, Void params) {
    if (expr.getSubstExpression() != null) {
      return expr.getSubstExpression().accept(this, null);
    }
    Expression result = myExprSubstitution.get(expr.getVariable());
    if (result != null) {
      return result;
    }

    if (expr.getVariable() instanceof MetaInferenceVariable) {
      if (Collections.disjoint(expr.getVariable().getBounds(), myExprSubstitution.getKeys())) {
        return expr;
      }

      ExprSubstitution newSubst = new ExprSubstitution();
      for (Binding var : expr.getVariable().getBounds()) {
        Expression substExpr = myExprSubstitution.get(var);
        if (substExpr != null) {
          newSubst.add(var, substExpr);
        }
      }
      return SubstExpression.make(expr, newSubst);
    }

    //noinspection SuspiciousMethodCalls
    expr.getVariable().getBounds().removeAll(myExprSubstitution.getKeys());
    return expr;
  }

  @Override
  public Expression visitSubst(SubstExpression expr, Void params) {
    ExprSubstitution newSubstitution = new ExprSubstitution(expr.getSubstitution());
    newSubstitution.subst(myExprSubstitution);
    newSubstitution.addAll(myExprSubstitution);
    return expr.getExpression().accept(new SubstVisitor(newSubstitution, myLevelSubstitution), null);
  }

  @Override
  public Expression visitLam(LamExpression expr, Void params) {
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), this);
    LamExpression result = new LamExpression(expr.getResultSort().subst(myLevelSubstitution), parameters, expr.getBody().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public Expression visitPi(PiExpression expr, Void params) {
    SingleDependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), this);
    PiExpression result = new PiExpression(expr.getResultSort().subst(myLevelSubstitution), parameters, expr.getCodomain().accept(this, null));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public Expression visitSigma(SigmaExpression expr, Void params) {
    SigmaExpression result = new SigmaExpression(expr.getSort().subst(myLevelSubstitution), DependentLink.Helper.subst(expr.getParameters(), this));
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return result;
  }

  @Override
  public Expression visitUniverse(UniverseExpression expr, Void params) {
    return myLevelSubstitution.isEmpty() ? expr : new UniverseExpression(expr.getSort().subst(myLevelSubstitution));
  }

  @Override
  public Expression visitError(ErrorExpression expr, Void params) {
    return expr.getExpression() == null ? expr : new ErrorExpression(expr.getExpression().accept(this, null), expr.isGoal());
  }

  @Override
  public Expression visitTuple(TupleExpression expr, Void params) {
    List<Expression> fields = new ArrayList<>(expr.getFields().size());
    for (Expression field : expr.getFields()) {
      fields.add(field.accept(this, null));
    }
    Expression arg = visitSigma(expr.getSigmaType(), null);
    if (!(arg instanceof SigmaExpression)) {
      throw new SubstException();
    }
    return new TupleExpression(fields, (SigmaExpression) arg);
  }

  @Override
  public Expression visitProj(ProjExpression expr, Void params) {
    return ProjExpression.make(expr.getExpression().accept(this, null), expr.getField());
  }

  @Override
  public Expression visitNew(NewExpression expr, Void params) {
    Expression arg = visitClassCall(expr.getClassCall(), null);
    if (!(arg instanceof ClassCallExpression)) {
      throw new SubstException();
    }
    return new NewExpression(expr.getRenewExpression() == null ? null : expr.getRenewExpression().accept(this, null), (ClassCallExpression) arg);
  }

  @Override
  public Expression visitPEval(PEvalExpression expr, Void params) {
    return new PEvalExpression(expr.getExpression().accept(this, null));
  }

  @Override
  public Expression visitLet(LetExpression letExpression, Void params) {
    List<LetClause> clauses = new ArrayList<>(letExpression.getClauses().size());
    for (LetClause clause : letExpression.getClauses()) {
      LetClause newClause = new LetClause(clause.getName(), clause.getPattern(), clause.getExpression().accept(this, null));
      clauses.add(newClause);
      myExprSubstitution.add(clause, new ReferenceExpression(newClause));
    }
    LetExpression result = new LetExpression(letExpression.isStrict(), clauses, letExpression.getExpression().accept(this, null));
    letExpression.getClauses().forEach(myExprSubstitution::remove);
    return result;
  }

  @Override
  public Expression visitCase(CaseExpression expr, Void params) {
    List<Expression> arguments = new ArrayList<>(expr.getArguments().size());
    for (Expression arg : expr.getArguments()) {
      arguments.add(arg.accept(this, null));
    }

    DependentLink parameters = DependentLink.Helper.subst(expr.getParameters(), this);
    Expression type = expr.getResultType().accept(this, null);
    Expression typeLevel = expr.getResultTypeLevel() == null ? null : expr.getResultTypeLevel().accept(this, null);
    DependentLink.Helper.freeSubsts(expr.getParameters(), myExprSubstitution);
    return new CaseExpression(expr.isSCase(), parameters, type, typeLevel, substElimTree(expr.getElimTree()), arguments);
  }

  public ElimTree substElimTree(ElimTree elimTree) {
    DependentLink vars = DependentLink.Helper.subst(elimTree.getParameters(), this);
    if (elimTree instanceof LeafElimTree) {
      elimTree = new LeafElimTree(vars, ((LeafElimTree) elimTree).getExpression().accept(this, null));
    } else {
      Map<CoreBranchKey, ElimTree> children = new HashMap<>();
      for (Map.Entry<CoreBranchKey, ElimTree> entry : ((BranchElimTree) elimTree).getChildren()) {
        CoreBranchKey key;
        if (!myLevelSubstitution.isEmpty() && entry.getKey() instanceof ClassConstructor) {
          ClassConstructor classCon = (ClassConstructor) entry.getKey();
          key = new ClassConstructor(classCon.getClassDefinition(), classCon.getSort().subst(myLevelSubstitution), classCon.getImplementedFields());
        } else {
          key = entry.getKey();
        }
        children.put(key, substElimTree(entry.getValue()));
      }
      elimTree = new BranchElimTree(vars, children);
    }
    DependentLink.Helper.freeSubsts(elimTree.getParameters(), myExprSubstitution);
    return elimTree;
  }

  @Override
  public Expression visitOfType(OfTypeExpression expr, Void params) {
    return new OfTypeExpression(expr.getExpression().accept(this, null), expr.getTypeOf().accept(this, null));
  }

  @Override
  public Expression visitInteger(IntegerExpression expr, Void params) {
    return expr;
  }
}
