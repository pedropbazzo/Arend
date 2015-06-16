package com.jetbrains.jetpad.vclang.parser;

import com.jetbrains.jetpad.vclang.term.Abstract;
import com.jetbrains.jetpad.vclang.term.Concrete;
import com.jetbrains.jetpad.vclang.term.Prelude;
import com.jetbrains.jetpad.vclang.term.definition.ClassDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Definition;
import com.jetbrains.jetpad.vclang.term.definition.FunctionDefinition;
import com.jetbrains.jetpad.vclang.term.definition.Universe;
import com.jetbrains.jetpad.vclang.term.definition.visitor.DefinitionPrettyPrintVisitor;
import com.jetbrains.jetpad.vclang.term.expr.Clause;
import com.jetbrains.jetpad.vclang.term.expr.ElimExpression;
import com.jetbrains.jetpad.vclang.term.expr.Expression;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import static com.jetbrains.jetpad.vclang.parser.ParserTestCase.*;
import static com.jetbrains.jetpad.vclang.term.expr.ExpressionFactory.*;
import static org.junit.Assert.*;

public class PrettyPrintingParserTest {
  private void testExpr(Expression expected, Expression expr) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    expr.prettyPrint(builder, new ArrayList<String>(), Abstract.Expression.PREC);
    Concrete.Expression result = parseExpr(builder.toString());
    assertTrue(compare(expected, result));
  }

  private void testDef(ClassDefinition root, FunctionDefinition expected, FunctionDefinition def) throws UnsupportedEncodingException {
    StringBuilder builder = new StringBuilder();
    def.accept(new DefinitionPrettyPrintVisitor(builder, new ArrayList<String>(), 0), null);
    Concrete.FunctionDefinition result = (Concrete.FunctionDefinition) parseDef(root, builder.toString());
    assertEquals(expected.getArguments().size(), result.getArguments().size());
    for (int i = 0; i < expected.getArguments().size(); ++i) {
      assertTrue(compare(expected.getArguments().get(i).getType(), result.getArguments().get(i).getType()));
    }
    assertTrue(compare(expected.getResultType(), result.getResultType()));
    assertNotNull(result.getTerm());
    assertTrue(compare(expected.getTerm(), result.getTerm()));
    assertEquals(expected.getArrow(), result.getArrow());
  }

  @Test
  public void prettyPrintingParserLamApp() throws UnsupportedEncodingException {
    // (\x y. x (x y)) (\x y. x) ((\x. x) (\x. x))
    Expression expr = Apps(Lam(lamArgs(Name("x"), Name("y")), Apps(Index(1), Apps(Index(1), Index(0)))), Lam(lamArgs(Name("x"), Name("y")), Index(1)), Apps(Lam("x", Index(0)), Lam("x", Index(0))));
    testExpr(expr, expr);
  }

  @Test
  public void prettyPrintingParserPi() throws UnsupportedEncodingException {
    // (x y : Nat) -> Nat -> Nat -> (x y -> y x) -> Nat x y
    Expression expr = Pi(args(Tele(vars("x", "y"), Nat())), Pi(Nat(), Pi(Nat(), Pi(Pi(Apps(Index(1), Index(0)), Apps(Index(0), Index(1))), Apps(Nat(), Index(1), Index(0))))));
    testExpr(expr, expr);
  }

  @Test
  public void prettyPrintingParserPiImplicit() throws UnsupportedEncodingException {
    // (x : Nat) {y z : Nat} -> Nat -> (t z' : Nat) {x' : Nat -> Nat} -> Nat x' y z' t
    Expression expr = Pi("x", Nat(), Pi(args(Tele(false, vars("y", "z"), Nat())), Pi(Nat(), Pi(args(Tele(vars("t", "z'"), Nat())), Pi(false, "x'", Pi(Nat(), Nat()), Apps(Nat(), Index(0), Index(4), Index(1), Index(2)))))));
    testExpr(expr, expr);
  }

  @Test
  public void prettyPrintingParserFunDef() throws UnsupportedEncodingException {
    // f (x : Nat) : Nat x => \y z. y z;
    ClassDefinition root = new ClassDefinition("\\root", null, new Universe.Type(0), new ArrayList<Definition>(0));
    FunctionDefinition def = new FunctionDefinition("f", root, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, teleArgs(Tele(vars("x"), Nat())), Apps(Nat(), Index(0)), Definition.Arrow.RIGHT, Lam(lamArgs(Name("y"), Name("z")), Apps(Index(1), Index(0))));
    testDef(root, def, def);
  }

  @Test
  public void prettyPrintingParserElim() throws UnsupportedEncodingException {
    // \function foo (z : (Nat -> Nat) -> Nat) (x y : Nat) : Nat <= \elim x | zero => y | suc x' => z (foo z x')
    ClassDefinition root = new ClassDefinition("\\root", null, new Universe.Type(0), new ArrayList<Definition>(0));

    List<Clause> fooClausesActual = new ArrayList<>();
    ElimExpression fooTermActual = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1), fooClausesActual, null);
    FunctionDefinition fooDef = new FunctionDefinition("foo", root, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, teleArgs(Tele(vars("z"), Pi(Pi(Nat(), Nat()), Nat())), Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, fooTermActual);
    fooClausesActual.add(new Clause(Prelude.ZERO, nameArgs(), Abstract.Definition.Arrow.RIGHT, Index(0), fooTermActual));
    fooClausesActual.add(new Clause(Prelude.SUC, nameArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, Apps(Index(2), Apps(DefCall(fooDef), Index(2), Index(1))), fooTermActual));
    root.getFields().add(fooDef);

    List<Clause> clausesActual = new ArrayList<>();
    ElimExpression termActual = Elim(Abstract.ElimExpression.ElimType.ELIM, Index(1), clausesActual, null);
    FunctionDefinition def = new FunctionDefinition("bar", root, Abstract.Definition.DEFAULT_PRECEDENCE, Abstract.Definition.Fixity.PREFIX, teleArgs(Tele(vars("z"), Pi(Pi(Nat(), Nat()), Nat())), Tele(vars("x", "y"), Nat())), Nat(), Abstract.Definition.Arrow.LEFT, termActual);
    clausesActual.add(new Clause(Prelude.ZERO, nameArgs(), Abstract.Definition.Arrow.RIGHT, Index(0), termActual));
    clausesActual.add(new Clause(Prelude.SUC, nameArgs(Name("x'")), Abstract.Definition.Arrow.RIGHT, Apps(Index(2), Apps(DefCall(fooDef), Index(2), Index(1))), termActual));

    testDef(root, def, def);
  }
}
