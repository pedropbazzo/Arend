package com.jetbrains.jetpad.vclang.term.abs;

import com.jetbrains.jetpad.vclang.naming.reference.ClassReferable;
import com.jetbrains.jetpad.vclang.naming.reference.GlobalReferable;
import com.jetbrains.jetpad.vclang.naming.reference.LocatedReferable;
import com.jetbrains.jetpad.vclang.naming.reference.Referable;
import com.jetbrains.jetpad.vclang.term.Fixity;
import com.jetbrains.jetpad.vclang.term.NamespaceCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public final class Abstract {
  private Abstract() {}

  public interface SourceNode {
    @Nonnull SourceNode getTopmostEquivalentSourceNode();
    @Nullable SourceNode getParentSourceNode();
  }

  public interface Parameter extends SourceNode {
    @Nullable Object getData();
    boolean isExplicit();
    @Nonnull List<? extends Referable> getReferableList();
    @Nullable Expression getType();
  }

  public interface Clause extends SourceNode {
    @Nullable Object getData();
    @Nonnull List<? extends Pattern> getPatterns();
  }

  public interface FunctionClause extends Clause {
    @Nullable Expression getExpression();
  }

  public interface ConstructorClause extends Clause {
    @Nonnull Collection<? extends Constructor> getConstructors();
  }

  public interface Pattern extends SourceNode {
    @Nullable Object getData();
    boolean isEmpty();
    boolean isExplicit();
    @Nullable Referable getHeadReference();
    @Nonnull List<? extends Pattern> getArguments();
  }

  public interface Reference extends SourceNode {
    @Nullable Object getData();
    @Nonnull Referable getReferent();
  }

  public interface LongReference extends Reference {
    @Nullable Reference getHeadReference();
    @Nonnull Collection<? extends Reference> getTailReferences();
  }

  // Holder

  public interface ParametersHolder extends SourceNode {
    @Nonnull List<? extends Abstract.Parameter> getParameters();
  }

  public interface LetClausesHolder extends SourceNode {
    @Nonnull Collection<? extends Abstract.LetClause> getLetClauses();
  }

  public interface EliminatedExpressionsHolder extends ParametersHolder {
    @Nullable Collection<? extends Reference> getEliminatedExpressions();
  }

  public interface ClassReferenceHolder extends SourceNode {
    @Nullable ClassReferable getClassReference();
  }

  public interface NamespaceCommandHolder extends SourceNode, NamespaceCommand {
    @Nullable Abstract.LongReference getOpenedReference();
  }

  // Expression

  public static final int INFINITY_LEVEL = -33;

  public interface Expression extends SourceNode {
    @Nullable Object getData();
    <P, R> R accept(AbstractExpressionVisitor<? super P, ? extends R> visitor, @Nullable P params);
  }

  public interface BinOpSequenceElem extends SourceNode {
    /* @Nonnull */ @Nullable Expression getExpression();
    @Nonnull Fixity getFixity();
    boolean isExplicit();
  }

  public interface Argument extends SourceNode {
    boolean isExplicit();
    /* @Nonnull */ @Nullable Expression getExpression();
  }

  public interface ClassFieldImpl extends ParametersHolder {
    @Nullable Object getData();
    @Nonnull Referable getImplementedField();
    /* @Nonnull */ @Nullable Expression getImplementation();
  }

  public interface LetClause extends ParametersHolder {
    @Nonnull Referable getReferable();
    @Nullable Expression getResultType();
    /* @Nonnull */ @Nullable Expression getTerm();
  }

  public interface LevelExpression extends SourceNode {
    @Nullable Object getData();
    <P, R> R accept(AbstractLevelExpressionVisitor<? super P, ? extends R> visitor, @Nullable P params);
  }

  // Definition

  public interface Definition extends SourceNode {
    @Nonnull LocatedReferable getReferable();
    <R> R accept(AbstractDefinitionVisitor<? extends R> visitor);
  }

  public interface FunctionDefinition extends Definition, EliminatedExpressionsHolder {
    @Nullable Expression getResultType();
    @Nullable Expression getTerm();
    @Override @Nonnull Collection<? extends Reference> getEliminatedExpressions();
    @Nonnull Collection<? extends FunctionClause> getClauses();
  }

  public interface DataDefinition extends Definition, EliminatedExpressionsHolder {
    boolean isTruncated();
    @Nullable Expression getUniverse();
    @Nonnull Collection<? extends ConstructorClause> getClauses();
  }

  public interface ClassDefinition extends Definition {
    @Override @Nonnull ClassReferable getReferable();
    @Nonnull Collection<? extends Reference> getSuperClasses();
    @Nonnull Collection<? extends ClassField> getClassFields();
    @Nonnull Collection<? extends ClassFieldImpl> getClassFieldImpls();
    boolean hasParameter();
    @Nullable Reference getUnderlyingClass();
    @Nonnull Collection<? extends ClassFieldSynonym> getFieldSynonyms();
  }

  public interface Constructor extends EliminatedExpressionsHolder {
    @Nonnull GlobalReferable getReferable();
    @Override @Nonnull Collection<? extends Reference> getEliminatedExpressions();
    @Nonnull Collection<? extends FunctionClause> getClauses();
  }

  public interface ClassField extends ParametersHolder {
    @Nonnull GlobalReferable getReferable();
    /* @Nonnull */ @Nullable Expression getResultType();
  }

  public interface ClassFieldSynonym extends SourceNode {
    /* @Nonnull */ @Nullable GlobalReferable getReferable();
    /* @Nonnull */ @Nullable Reference getUnderlyingField();
  }

  public interface InstanceDefinition extends Definition, ParametersHolder {
    /* @Nonnull */ @Nullable Reference getResultClass();
    @Nonnull Collection<? extends ClassFieldImpl> getImplementation();
  }
}