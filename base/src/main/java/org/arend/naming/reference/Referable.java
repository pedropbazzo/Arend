package org.arend.naming.reference;

import org.arend.ext.reference.ArendRef;
import org.jetbrains.annotations.NotNull;

public interface Referable extends ArendRef {
  @NotNull String textRepresentation();

  @NotNull
  @Override
  default String getRefName() {
    return textRepresentation();
  }

  @NotNull
  default Referable getUnderlyingReferable() {
    return this;
  }
}
