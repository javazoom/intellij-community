package org.jetbrains.postfixCompletion.TemplateProviders;

import com.intellij.codeInsight.lookup.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.*;
import org.jetbrains.postfixCompletion.Infrastructure.*;
import org.jetbrains.postfixCompletion.LookupItems.*;

import java.util.*;

@TemplateProvider(
  templateName = "if",
  description = "Checks boolean expression to be 'true'",
  example = "if (expr)")
public final class IfStatementTemplateProvider extends BooleanTemplateProviderBase {

  @Override public boolean createBooleanItems(
    @NotNull final PrefixExpressionContext context,
    @NotNull final List<LookupElement> consumer) {

    if (context.canBeStatement) {
      consumer.add(new IfLookupItem(context));
      return true;
    }

    return false;
  }

  private static final class IfLookupItem
    extends StatementPostfixLookupElement<PsiIfStatement> {

    public IfLookupItem(@NotNull PrefixExpressionContext context) {
      super("if", context);
    }

    @NotNull @Override protected PsiIfStatement createNewStatement(
      @NotNull final PsiElementFactory factory,
      @NotNull final PsiExpression expression,
      @NotNull final PsiFile context) {

      final PsiIfStatement ifStatement = (PsiIfStatement)
        factory.createStatementFromText("if(expr)", context);

      final PsiExpression condition = ifStatement.getCondition();
      assert condition != null : "condition != null";
      condition.replace(expression);

      return ifStatement;
    }
  }
}

