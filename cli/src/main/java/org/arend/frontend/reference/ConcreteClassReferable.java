package org.arend.frontend.reference;

import org.arend.ext.reference.Precedence;
import org.arend.frontend.parser.Position;
import org.arend.module.ModuleLocation;
import org.arend.module.scopeprovider.EmptyModuleScopeProvider;
import org.arend.naming.reference.Referable;
import org.arend.naming.reference.Reference;
import org.arend.naming.reference.TCClassReferable;
import org.arend.naming.reference.TCReferable;
import org.arend.naming.resolving.visitor.ExpressionResolveNameVisitor;
import org.arend.naming.scope.CachingScope;
import org.arend.naming.scope.LexicalScope;
import org.arend.naming.scope.Scope;
import org.arend.naming.scope.ScopeFactory;
import org.arend.term.concrete.Concrete;
import org.arend.term.group.ChildGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ConcreteClassReferable extends ConcreteLocatedReferable implements TCClassReferable {
  private ChildGroup myGroup;
  private final Collection<? extends ConcreteClassFieldReferable> myFields;
  private final List<? extends Reference> myUnresolvedSuperClasses;
  private final List<TCClassReferable> mySuperClasses;
  private boolean myResolved = false;

  public ConcreteClassReferable(Position position, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, Collection<? extends ConcreteClassFieldReferable> fields, List<? extends Reference> superClasses, TCReferable parent) {
    super(position, name, precedence, aliasName, aliasPrecedence, parent, Kind.TYPECHECKABLE);
    myFields = fields;
    myUnresolvedSuperClasses = superClasses;
    mySuperClasses = new ArrayList<>(superClasses.size());
  }

  public ConcreteClassReferable(Position position, @NotNull String name, Precedence precedence, @Nullable String aliasName, Precedence aliasPrecedence, Collection<? extends ConcreteClassFieldReferable> fields, List<? extends Reference> superClasses, ModuleLocation parent) {
    super(position, name, precedence, aliasName, aliasPrecedence, parent, Kind.TYPECHECKABLE);
    myFields = fields;
    myUnresolvedSuperClasses = superClasses;
    mySuperClasses = new ArrayList<>(superClasses.size());
  }

  public void setGroup(ChildGroup group) {
    myGroup = group;
  }

  @Override
  public Concrete.ClassDefinition getDefinition() {
    return (Concrete.ClassDefinition) super.getDefinition();
  }

  @NotNull
  @Override
  public List<? extends TCClassReferable> getSuperClassReferences() {
    if (myUnresolvedSuperClasses.isEmpty()) {
      return Collections.emptyList();
    }

    resolve();
    return mySuperClasses;
  }

  protected void resolve() {
    if (!myResolved) {
      ChildGroup parent = myGroup.getParentGroup();
      resolve(CachingScope.make(parent == null ? ScopeFactory.forGroup(myGroup, EmptyModuleScopeProvider.INSTANCE) : LexicalScope.insideOf(myGroup, parent.getGroupScope(), true)));
      myResolved = true;
    }
  }

  protected void resolve(Scope scope) {
    mySuperClasses.clear();
    for (Reference superClass : myUnresolvedSuperClasses) {
      Referable ref = ExpressionResolveNameVisitor.resolve(superClass.getReferent(), scope, true, null);
      if (ref instanceof TCClassReferable) {
        mySuperClasses.add((TCClassReferable) ref);
      }
    }
  }

  @NotNull
  @Override
  public Collection<? extends Reference> getUnresolvedSuperClassReferences() {
    return myUnresolvedSuperClasses;
  }

  @NotNull
  @Override
  public Collection<? extends ConcreteClassFieldReferable> getFieldReferables() {
    return myFields;
  }

  @NotNull
  @Override
  public Collection<? extends Referable> getImplementedFields() {
    List<Referable> result = new ArrayList<>();
    for (Concrete.ClassElement element : getDefinition().getElements()) {
      if (element instanceof Concrete.ClassFieldImpl) {
        result.add(((Concrete.ClassFieldImpl) element).getImplementedField());
      }
    }
    return result;
  }

  @Override
  public boolean isRecord() {
    return getDefinition().isRecord();
  }
}
