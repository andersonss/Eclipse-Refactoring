package AppleApricot;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;



public class WhileToForWizard  extends RefactoringWizard {

   public WhileToForWizard (ChangeWhileToForRefactoring refactoring) {
      super(refactoring, WIZARD_BASED_USER_INTERFACE);
   }

   @Override
   protected void addUserInputPages() {

   }

}
