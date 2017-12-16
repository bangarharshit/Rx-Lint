package com.rx.errorprone;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import rx.Observable;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

/** @author harshit.bangar@gmail.com (Harshit Bangar) */
@BugPattern(
    name = "com.rx.errorprone.OnCreateCheck",
    summary = "Prefer defer or just instead to onCreate",
    explanation = "Don't call observable's onCreate in Rx1 as it doesn't has backpressure support. Use defer or just instead",
    category = JDK,
    severity = WARNING
)
public class OnCreateCheck extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

  public static final Matcher<ExpressionTree> ON_CREATE = Matchers.instanceMethod().onExactClass(
      Observable.class.getName()).named("create");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (ON_CREATE.matches(tree, state)) {
      return describeMatch(tree);
    } else {
      return Description.NO_MATCH;
    }
  }

  @Override public String linkUrl() {
    return "https://speakerdeck.com/dlew/common-rxjava-mistakes";
  }
}