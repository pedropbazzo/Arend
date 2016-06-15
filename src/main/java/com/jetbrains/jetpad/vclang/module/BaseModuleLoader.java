package com.jetbrains.jetpad.vclang.module;

import com.jetbrains.jetpad.vclang.module.error.ModuleCycleError;
import com.jetbrains.jetpad.vclang.module.error.ModuleLoadingError;
import com.jetbrains.jetpad.vclang.module.error.ModuleNotFoundError;
import com.jetbrains.jetpad.vclang.module.output.DummyOutputSupplier;
import com.jetbrains.jetpad.vclang.module.output.Output;
import com.jetbrains.jetpad.vclang.module.output.OutputSupplier;
import com.jetbrains.jetpad.vclang.module.source.DummySourceSupplier;
import com.jetbrains.jetpad.vclang.module.source.Source;
import com.jetbrains.jetpad.vclang.module.source.SourceSupplier;
import com.jetbrains.jetpad.vclang.serialization.ModuleDeserialization;
import com.jetbrains.jetpad.vclang.error.GeneralError;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class BaseModuleLoader implements ModuleLoader {
  private final List<ModuleID> myLoadingModules = new ArrayList<>();
  private final Map<ModuleID, Result> myLoadedModules = new HashMap<>();

  private SourceSupplier mySourceSupplier;
  private OutputSupplier myOutputSupplier;

  private final SerializedLoader mySerializedLoader;
  private final boolean myRecompile;

  public BaseModuleLoader(boolean recompile) {
    mySourceSupplier = DummySourceSupplier.getInstance();
    myOutputSupplier = DummyOutputSupplier.getInstance();
    myRecompile = recompile;
    mySerializedLoader = new SerializedLoader(myLoadingModules, this, mySourceSupplier, myOutputSupplier);
  }

  public void setSourceSupplier(SourceSupplier sourceSupplier) {
    mySourceSupplier = sourceSupplier;
    mySerializedLoader.setSourceSupplier(sourceSupplier);
  }

  public void setOutputSupplier(OutputSupplier outputSupplier) {
    myOutputSupplier = outputSupplier;
    mySerializedLoader.setOutputSupplier(outputSupplier);
  }

  @Override
  public void save(ModuleID module) {
    Output output = myOutputSupplier.getOutput(module);
    if (output != null && output.canWrite()) {
      try {
        output.write();
      } catch (IOException e) {
        savingError(new GeneralError("Saving module '" + module.getModulePath() + "': " + GeneralError.ioError(e), null));
      }
    }
  }

  @Override
  public Result load(ModuleID module) {
    Result loaded = myLoadedModules.get(module);
    if (loaded != null) {
      return loaded;
    }

    int index = myLoadingModules.indexOf(module);
    if (index != -1) {
      loadingError(new ModuleCycleError(new ArrayList<>(myLoadingModules.subList(index, myLoadingModules.size()))));
      return null;
    }

    try {
      if (!myRecompile) {
        Result result = mySerializedLoader.load(module);
        if (result != null)
          return result;
      }

      Source source = mySourceSupplier.getSource(module);
      if (source == null) {
        loadingError(new ModuleNotFoundError(module));
        return null;
      }

      try {
        myLoadingModules.add(module);

        Result result = source.load();
        Helper.processLoaded(this, module, result);
        myLoadedModules.put(module, result);

        return result;
      } finally {
        myLoadingModules.remove(myLoadingModules.size() - 1);
      }
    } catch (EOFException e) {
      loadingError(new ModuleLoadingError(module, "Incorrect format: Unexpected EOF"));
    } catch (ModuleDeserialization.DeserializationException e) {
      loadingError(new ModuleLoadingError(module, e.toString()));
    } catch (IOException e) {
      loadingError(new ModuleLoadingError(module, GeneralError.ioError(e)));
    }
    return null;
  }

  @Override
  public ModuleID locateModule(ModulePath modulePath) {
    ModuleID result = mySourceSupplier.locateModule(modulePath);
    return result != null ? result : myOutputSupplier.locateModule(modulePath);
  }
}
