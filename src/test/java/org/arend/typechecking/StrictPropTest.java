package org.arend.typechecking;

import org.junit.Test;

public class StrictPropTest extends TypeCheckingTestCase {
  @Test
  public void parametersTest() {
    typeCheckDef("\\func f {A : \\Prop} (x y : A) : x = y => path (\\lam _ => x)");
  }

  @Test
  public void setError() {
    typeCheckDef("\\func f {A : \\Set0} (x y : A) : x = y => path (\\lam _ => x)", 1);
  }

  @Test
  public void setPathTest() {
    typeCheckDef("\\func f {A : \\Set} (x y : A) (p q : x = y) : p = q => path (\\lam _ => p)");
  }

  @Test
  public void classTest() {
    typeCheckModule(
      "\\record B\n" +
      "\\func idp {X : \\Type} {x : X} => path (\\lam _ => x)\n" +
      "\\func f (b b' : B) : b = b' => idp");
  }

  @Test
  public void classUseLevelTest() {
    typeCheckModule(
      "\\record B (X : \\Type) (p : \\Pi (x x' : X) -> x = x') (x0 : X)\n" +
      " \\where \\use \\level levelProp {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : b = b' => path (\\lam i => \\new B X p (p b.x0 b'.x0 @ i))\n" +
      "\\func idp {X : \\Type} {x : X} => path (\\lam _ => x)\n" +
      "\\func f {X : \\Type} {p : \\Pi (x x' : X) -> x = x'} (b b' : B X p) : b = {B X p} b' => idp");
  }
}