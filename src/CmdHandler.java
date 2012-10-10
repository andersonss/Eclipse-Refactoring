package AppleApricot;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.internal.corext.dom.Selection;
import org.eclipse.jdt.internal.corext.dom.SelectionAnalyzer;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

public class CmdHandler extends AbstractHandler  {
	
	   private ICompilationUnit fCompilationUnit;
	   private int selectionStart;
	   private int selectionLength;

	   public void dispose() {
	      // Do nothing
	   }

	   @Override
	   public Object execute(ExecutionEvent event) throws ExecutionException {
	      updateDataFromSelection(HandlerUtil.getCurrentSelection(event), event);
	      startWizard(new WhileToForWizard(createToFinalRefactoring()),
	            HandlerUtil.getActiveShell(event), "AppleApricot");
	      return null;
	   }
	   
	   public void startWizard(RefactoringWizard wizard, Shell parent,
		         String dialogTitle) {
		      try {
		         RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(
		               wizard);
		         operation.run(parent, dialogTitle);
		      } catch (InterruptedException exception) {
		         // Do nothing
		      }
		   }
	   
	   private ChangeWhileToForRefactoring createToFinalRefactoring() {
		      ChangeWhileToForRefactoring refactoring = new ChangeWhileToForRefactoring(fCompilationUnit,selectionStart,selectionLength);
		     
		      return refactoring;
		   }
	   
	   // I don't like having to use this method, i'd rather make this work
	   // like the built in refactorings.
	   private ICompilationUnit getCompilationUnitForCurrentEditor(ExecutionEvent theEvent) {
	      IEditorPart editorSite = HandlerUtil.getActiveEditor(theEvent);
	      IEditorInput editorInput = editorSite.getEditorInput();
	      IResource resource = (IResource) editorInput.getAdapter(IResource.class);
	      ICompilationUnit icu = JavaCore
	            .createCompilationUnitFrom((IFile) resource);
	      return icu;
	   }
	   public void updateDataFromSelection(ISelection selection,
		         ExecutionEvent event) {
		      fCompilationUnit = getCompilationUnitForCurrentEditor(event);
		    
		      if (selection instanceof IStructuredSelection) {
		    	  //Incorret selection
		         IStructuredSelection extended = (IStructuredSelection) selection;
		         Object[] elements = extended.toArray();
	        	 System.out.println( "Incorret selection" );
		      } else if (selection instanceof ITextSelection) {
		         selectionStart = ((ITextSelection) selection).getOffset();
				 selectionLength = ((ITextSelection) selection).getLength();
		      }
		   }

}
