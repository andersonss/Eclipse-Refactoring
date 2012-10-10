package AppleApricot;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class ChangeWhileToForDescriptor extends RefactoringDescriptor {

   public static final String REFACTORING_ID = "arz.refactorings.string.to.final";

   private final Map fArguments;

   public ChangeWhileToForDescriptor(String project, String description, String comment,
         Map arguments) {
      super(REFACTORING_ID, project, description, comment,
            RefactoringDescriptor.STRUCTURAL_CHANGE
                  | RefactoringDescriptor.MULTI_CHANGE);
      fArguments = arguments;
   }

   @Override
   public Refactoring createRefactoring(RefactoringStatus status)
         throws CoreException {
      ChangeWhileToForRefactoring refactoring = new ChangeWhileToForRefactoring();
      status.merge(refactoring.initialize(fArguments));
      return refactoring;
   }

   public Map getArguments() {
      return fArguments;
   }

}
