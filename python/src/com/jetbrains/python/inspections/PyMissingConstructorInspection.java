package com.jetbrains.python.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiReference;
import com.jetbrains.python.PyBundle;
import com.jetbrains.python.PyNames;
import com.jetbrains.python.actions.AddCallSuperQuickFix;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.types.TypeEvalContext;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: catherine
 *
 * Inspection to warn if call to super constructor in class is missed
 */
public class PyMissingConstructorInspection extends PyInspection {
  @Nls
  @NotNull
  @Override
  public String getDisplayName() {
    return PyBundle.message("INSP.NAME.missing.super.constructor");
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                        boolean isOnTheFly,
                                        @NotNull LocalInspectionToolSession session) {
    return new Visitor(holder, session);
  }

  private static class Visitor extends PyInspectionVisitor {
    public Visitor(@Nullable ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
      super(holder, session);
    }

    @Override
    public void visitPyClass(final PyClass node) {
      PsiElement[] superClasses = node.getSuperClassExpressions();
      if (superClasses.length == 0 || (superClasses.length == 1 && PyNames.OBJECT.equals(superClasses[0].getText())))
        return;

      if (!superHasConstructor(node)) return;
      PyFunction initMethod = node.findMethodByName(PyNames.INIT, false);
      if (initMethod != null) {
        if (isExceptionClass(node, myTypeEvalContext) || hasConstructorCall(node, initMethod)) {
          return;
        }
        if (superClasses.length == 1 || node.isNewStyleClass())
          registerProblem(initMethod.getNameIdentifier(), "Call to constructor of super class is missed",
                          new AddCallSuperQuickFix(node.getSuperClasses()[0], superClasses[0].getText()));
        else
          registerProblem(initMethod.getNameIdentifier(), "Call to constructor of super class is missed");
      }
    }

    private static boolean superHasConstructor(PyClass node) {
      for (PyClass s : node.iterateAncestorClasses()) {
        if (!PyNames.OBJECT.equals(s.getName()) && !PyNames.FAKE_OLD_BASE.equals(s.getName()) &&
            node.getName() != null && !node.getName().equals(s.getName())
            && s.findMethodByName(PyNames.INIT, false) != null) {
          return true;
        }
      }
      return false;
    }

    private boolean isExceptionClass(@NotNull PyClass cls, @NotNull TypeEvalContext context) {
      if (PyBroadExceptionInspection.equalsException(cls, context)) {
        return true;
      }
      for (PyClass baseClass : cls.iterateAncestorClasses()) {
        if (PyBroadExceptionInspection.equalsException(baseClass, context)) {
          return true;
        }
      }
      return false;
    }

    private static boolean hasConstructorCall(PyClass node, PyFunction initMethod) {
      PyStatementList statementList = initMethod.getStatementList();
      CallVisitor visitor = new CallVisitor(node);
      if (statementList != null) {
        statementList.accept(visitor);
        return visitor.myHasConstructorCall;
      }
      return false;
    }

    private static class CallVisitor extends PyRecursiveElementVisitor {
      private boolean myHasConstructorCall = false;
      private PyClass myClass;
      CallVisitor(PyClass node) {
        myClass = node;
      }

      @Override
      public void visitPyCallExpression(PyCallExpression node) {
        if (isConstructorCall(node, myClass))
          myHasConstructorCall = true;
      }

      private static boolean isConstructorCall(PyCallExpression expression, PyClass cl) {
        PyExpression callee = expression.getCallee();
        if (callee instanceof PyQualifiedExpression) {
          PyExpression qualifier = ((PyQualifiedExpression)callee).getQualifier();
          if (qualifier != null) {
            String tmp = "";
            if (qualifier instanceof PyCallExpression) {
              PyExpression innerCallee = ((PyCallExpression)qualifier).getCallee();
              if (innerCallee != null) {
                tmp = innerCallee.getName();
              }
              if (PyNames.SUPER.equals(tmp) && (PyNames.INIT.equals(callee.getName()))) {
                PyExpression[] args = ((PyCallExpression)qualifier).getArguments();
                if (args.length > 0) {
                  String firstArg = args[0].getText();
                  if (firstArg.equals(cl.getName()) || firstArg.equals(PyNames.CANONICAL_SELF+"."+PyNames.CLASS))
                      return true;
                  for (PyClass s : cl.iterateAncestorClasses()) {
                    if (firstArg.equals(s.getName()))
                      return true;
                  }
                }
                else
                  return true;
              }
            }
            if (PyNames.INIT.equals(callee.getName())) {
              return isSuperClassCall(cl, qualifier);
            }
          }
        }
        return false;
      }

      private static boolean isSuperClassCall(PyClass cl, PyExpression qualifier) {
        PsiElement callingClass = null;
        if (qualifier instanceof PyCallExpression) {
          PyExpression innerCallee = ((PyCallExpression)qualifier).getCallee();
          if (innerCallee != null) {
            PsiReference ref = innerCallee.getReference();
            if (ref != null)
              callingClass = ref.resolve();
          }
        }
        else {
          PsiReference ref = qualifier.getReference();
          if (ref != null)
            callingClass = ref.resolve();
        }
        for (PyClass s : cl.iterateAncestorClasses()) {
          if (s.equals(callingClass)) {
            return true;
          }
        }
        return false;
      }
    }
  }
}
