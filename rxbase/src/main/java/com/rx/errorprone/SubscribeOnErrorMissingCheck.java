package com.rx.errorprone;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Type;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import java.util.Objects;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

/** @author harshit.bangar@gmail.com (Harshit Bangar) */
@BugPattern(
    name = "SubscribeOnErrorMissingCheck",
    summary = "Subscriber is missing onError handling",
    explanation =
        "Every observable can report errors. "
            + "Not implementing onError will throw an exception at runtime "
            + "which can be hard to debug when the error is thrown on a Scheduler that is not the invoking thread."
            + "It can also be used for debugging error ",
    category = JDK,
    severity = WARNING
)
public class SubscribeOnErrorMissingCheck extends BugChecker implements
    BugChecker.MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> ON_SUBSCRIBE = Matchers.anyOf(
      Matchers.instanceMethod().onExactClass(Observable.class.getName()).named("subscribe"),
      Matchers.instanceMethod().onExactClass(Single.class.getName()).named("subscribe"),
      Matchers.instanceMethod().onExactClass(Completable.class.getName()).named("subscribe"),
      Matchers.instanceMethod().onExactClass(Maybe.class.getName()).named("subscribe"),
      Matchers.instanceMethod().onExactClass(Flowable.class.getName()).named("subscribe"));

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (ON_SUBSCRIBE.matches(tree, state)) {
      if (tree.getArguments().size() > 1) {
        return Description.NO_MATCH;
      } else if (tree.getArguments().size() == 0) {
        return describeMatch(tree);
      } else {
        Type argType = ASTHelpers.getType(tree.getArguments().get(0));
        Type consumerType =
            Objects.requireNonNull(state.getTypeFromString(Consumer.class.getName()));
        // For completable.
        Type actionType = Objects.requireNonNull(state.getTypeFromString(Action.class.getName()));
        if (ASTHelpers.isSubtype(argType, consumerType, state) || ASTHelpers.isSubtype(argType, actionType, state)) {
          return describeMatch(tree);
        }
      }
    }
    return Description.NO_MATCH;
  }

  @Override public String linkUrl() {
    return "https://speakerdeck.com/dlew/common-rxjava-mistakes";
  }
}
