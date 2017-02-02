package com.jetbrains.jetpad.vclang.naming.namespace;

import com.jetbrains.jetpad.vclang.error.GeneralError;
import com.jetbrains.jetpad.vclang.naming.error.DuplicateDefinitionError;
import com.jetbrains.jetpad.vclang.term.Abstract;

import java.util.*;

public class SimpleNamespace implements Namespace {
  private final Map<String, Abstract.Definition> myNames = new HashMap<>();
  private List<Abstract.ClassViewInstance> myInstances = Collections.emptyList();

  public SimpleNamespace() {
  }

  public SimpleNamespace(SimpleNamespace other) {
    myNames.putAll(other.myNames);
  }

  public SimpleNamespace(Abstract.Definition def) {
    this();
    addDefinition(def);
  }

  public void addDefinition(Abstract.Definition def) {
    addDefinition(def.getName(), def);
  }

  public void addDefinition(String name, final Abstract.Definition def) {
    final Abstract.Definition prev = myNames.put(name, def);
    if (!(prev == null || prev == def)) {
      throw new InvalidNamespaceException() {
        @Override
        public GeneralError toError() {
          return new DuplicateDefinitionError(prev, def);
        }
      };
    }
  }

  public void addInstance(Abstract.ClassViewInstance instance) {
    if (myInstances.isEmpty()) {
      myInstances = new ArrayList<>();
    }
    myInstances.add(instance);
  }

  public void addAll(SimpleNamespace other) {
    for (Map.Entry<String, Abstract.Definition> entry : other.myNames.entrySet()) {
      addDefinition(entry.getKey(), entry.getValue());
    }
    if (!other.myInstances.isEmpty() && myInstances.isEmpty()) {
      myInstances = new ArrayList<>();
    }
    for (Abstract.ClassViewInstance instance : other.myInstances) {
      myInstances.add(instance);
    }
  }

  @Override
  public Set<String> getNames() {
    return myNames.keySet();
  }

  Set<Map.Entry<String, Abstract.Definition>> getEntrySet() {
    return myNames.entrySet();
  }

  @Override
  public Abstract.Definition resolveName(String name) {
    return myNames.get(name);
  }

  @Override
  public Collection<? extends Abstract.ClassViewInstance> getInstances() {
    return myInstances;
  }
}
