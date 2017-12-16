package com.rx.errorprone;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.AbstractReturnValueIgnored;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.Objects;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;

@BugPattern(
    name = "com.rx.errorprone.DanglingSubscriptionCheck",
    summary = "Observable's subscription should be assigned to a disposable for cleanup",
    explanation = "Observable's subscription should be assigned to a disposable for cleanup",
    category = JDK,
    severity = WARNING
)
public class DanglingSubscriptionCheck extends AbstractReturnValueIgnored {

  private static final Matcher<ExpressionTree> ON_SUBSCRIBE = Matchers.anyOf(
      Matchers.instanceMethod().onExactClass(Observable.class.getName()).named("subscribeWith"),
      Matchers.instanceMethod().onExactClass(Single.class.getName()).named("subscribeWith"),
      Matchers.instanceMethod().onExactClass(Completable.class.getName()).named("subscribeWith"),
      Matchers.instanceMethod().onExactClass(Maybe.class.getName()).named("subscribeWith"),
      Matchers.instanceMethod().onExactClass(Flowable.class.getName()).named("subscribeWith"));

  // Check for return type Disposable.
  // It is not able to detect correct type in subscribeWith so ON_SUBSCRIBE matcher is combined.
  // public final <E extends Observer<? super T>> E subscribeWith(E observer)
  private static final Matcher<ExpressionTree> MATCHER =
      Matchers.anyOf(new Matcher<ExpressionTree>() {
        @Override
        public boolean matches(ExpressionTree tree, VisitorState state) {
          Type disposableType =
              Objects.requireNonNull(state.getTypeFromString("io.reactivex.disposables.Disposable"));
          Symbol untypedSymbol = ASTHelpers.getSymbol(tree);
          if (!(untypedSymbol instanceof Symbol.MethodSymbol)) {
            return false;
          }
          Symbol.MethodSymbol sym = (Symbol.MethodSymbol) untypedSymbol;
          if (hasAnnotation(sym, CanIgnoreReturnValue.class, state)) {
            return false;
          }
          Type returnType = sym.getReturnType();
          return ASTHelpers.isSubtype(
              ASTHelpers.getUpperBound(returnType, state.getTypes()), disposableType, state);
        }
      }, ON_SUBSCRIBE);

  @Override public Matcher<? super MethodInvocationTree> specializedMatcher() {
    return MATCHER;
  }
}