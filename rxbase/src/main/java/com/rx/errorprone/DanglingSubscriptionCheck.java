package com.rx.errorprone;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.rx.errorprone.utils.AbstractReturnValueIgnored;
import com.rx.errorprone.utils.MatcherUtils;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** @author harshit.bangar@gmail.com (Harshit Bangar) */
@AutoService(BugChecker.class)
@BugPattern(
  name = "DanglingSubscriptionCheck",
  summary = "Subscription should be assigned to a disposable.",
  explanation =
      "Observable's subscription should be assigned to a disposable for cleanup, otherwise it may lead to a leak",
  category = JDK,
  severity = WARNING
)
public class DanglingSubscriptionCheck extends AbstractReturnValueIgnored {

  private final ImmutableList<String> classesWithLifecycle;

  public DanglingSubscriptionCheck() {
    this(ErrorProneFlags.empty());
  }

  public DanglingSubscriptionCheck(ErrorProneFlags errorProneFlags) {
    Optional<ImmutableList<String>> list = errorProneFlags.getList("LifeCycleClasses");
    classesWithLifecycle =
        ImmutableList.<String>builder()
            .add("android.app.Activity")
            .add("android.app.Fragment")
            .addAll(list.orElse(ImmutableList.of()))
            .build();
  }

  private static final Matcher<ExpressionTree> ON_SUBSCRIBE = MatcherUtils.subscribeWithForRx2();

  // Check for return type Disposable.
  // It is not able to detect correct type in subscribeWith so ON_SUBSCRIBE matcher is combined.
  // public final <E extends Observer<? super T>> E subscribeWith(E observer)
  private static Matcher<ExpressionTree> matcher(List<String> classesWithLifecycle) {
    return Matchers.anyOf(
        ON_SUBSCRIBE,
        new Matcher<ExpressionTree>() {
          @Override
          public boolean matches(ExpressionTree tree, VisitorState state) {
            Type disposableType =
                Objects.requireNonNull(
                    state.getTypeFromString("io.reactivex.disposables.Disposable"));
            Symbol untypedSymbol = ASTHelpers.getSymbol(tree);
            if (!(untypedSymbol instanceof Symbol.MethodSymbol)) {
              return false;
            }
            Symbol.MethodSymbol sym = (Symbol.MethodSymbol) untypedSymbol;
            if (hasAnnotation(sym, CanIgnoreReturnValue.class, state)) {
              return false;
            }
            Type returnType = sym.getReturnType();
            ClassTree enclosingClass =
                ASTHelpers.findEnclosingNode(state.getPath(), ClassTree.class);
            Type.ClassType enclosingClassType = ASTHelpers.getType(enclosingClass);
            if (ASTHelpers.isSubtype(
                ASTHelpers.getUpperBound(returnType, state.getTypes()), disposableType, state)) {
              for (String s : classesWithLifecycle) {
                Type lifecycleType = state.getTypeFromString(s);
                if (ASTHelpers.isSubtype(enclosingClassType, lifecycleType, state)) {
                  return true;
                }
              }
            }
            return false;
          }
        });
  }

  @Override
  public Matcher<? super ExpressionTree> specializedMatcher() {
    return matcher(classesWithLifecycle);
  }

  @Override
  public String linkUrl() {
    return "https://bitbucket.org/littlerobots/rxlint";
  }
}
