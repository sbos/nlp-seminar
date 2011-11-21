package ru.ispras.nlpcourse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class FragileAction<T> {
  Log log = LogFactory.getLog(FragileAction.class);

  protected abstract T act() throws Exception;

  protected abstract void pickUpPieces();

  public T go() {
    try {
      return act();
    }
    catch (Exception e) {
      log.error("Oops, your fragile action went to shit fuck such", e);
      pickUpPieces();
      return go();
    }
  }
}
