package org.arend.typechecking.instance.provider;

import org.arend.naming.reference.ClassReferable;
import org.arend.term.concrete.Concrete;

import java.util.function.Predicate;

public class EmptyInstanceProvider implements InstanceProvider {
  private static final EmptyInstanceProvider INSTANCE = new EmptyInstanceProvider();

  private EmptyInstanceProvider() {}

  public static EmptyInstanceProvider getInstance() {
    return INSTANCE;
  }

  @Override
  public Concrete.FunctionDefinition findInstance(ClassReferable classRef, Predicate<Concrete.FunctionDefinition> pred) {
    return null;
  }
}
