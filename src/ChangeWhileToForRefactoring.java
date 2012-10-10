package AppleApricot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ITrackedNodePosition;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.JavaRefactoringDescriptor;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.corext.dom.NodeFinder;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationRefactoringChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.DynamicValidationStateChange;
import org.eclipse.jdt.internal.corext.refactoring.structure.CompilationUnitRewrite;
import org.eclipse.jdt.internal.corext.refactoring.util.NoCommentSourceRangeComputer;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.ui.SharedASTProvider;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

public class ChangeWhileToForRefactoring extends Refactoring {

	private static final String NOT_A_WhileLoop = "Your selection is not a valid while loop";
	private ICompilationUnit fCu;
	private CompilationUnit fCompilationUnitNode;
	private ASTRewrite astRewrite;
	private TextEdit textEdits;
	private ASTNode whileLoopASTNode;
	private int selectionStart;
	private int selectionLength;
	private final String refactoringName = "ChangeWhileToForRefactoring";
	private String NO_SELECTION = "Please select a valid while loop";

	public ChangeWhileToForRefactoring(ICompilationUnit unit) {
		this.fCu = unit;
		ASTParser parser = ASTParser.newParser(AST.JLS3); // handles JDK 1.0,
															// 1.1, 1.2, 1.3,
															// 1.4, 1.5, 1.6
		parser.setSource(fCu);
		parser.setResolveBindings(true);
		// In order to parse 1.5 code, some compiler options need to be set to
		// 1.5
		Map options = JavaCore.getOptions();
		JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, options);
		// parser.setCompilerOptions(options);
		fCompilationUnitNode = (CompilationUnit) parser.createAST(null);
		this.selectionStart = selectionStart;
		this.selectionLength = selectionLength;

	}

	public ChangeWhileToForRefactoring(ICompilationUnit fCompilationUnit,
			int selectionStart, int selectionLength) {
		fCu = fCompilationUnit;
		this.selectionStart = selectionStart;
		this.selectionLength = selectionLength;
		
	}

	public ChangeWhileToForRefactoring() {
	}

	@Override
	public String getName() {
		return refactoringName;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		try {
			pm.beginTask("checkInititalConditions", 0);
			if(  selectionLength == 0) {
				status.addFatalError(NO_SELECTION );
			} else {			
				fCompilationUnitNode = AstTools.ParseToJavaAst(pm, fCu);
				whileLoopASTNode = NodeFinder.perform(fCompilationUnitNode, selectionStart, selectionLength);				
				if (null == whileLoopASTNode || !(whileLoopASTNode instanceof WhileStatement) ) {
					status.addFatalError(NOT_A_WhileLoop);
				}
			} 
		} finally {
			pm.done();
		}
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {

		// fCURewrite= new CompilationUnitRewrite(fCu, fCompilationUnitNode);
		astRewrite = ASTRewrite.create(fCompilationUnitNode.getAST());
		AST ast = fCompilationUnitNode.getAST();
		

		// Get while statement block
		TypeDeclaration typeDecl = (TypeDeclaration) fCompilationUnitNode
				.types().get(0);
		MethodDeclaration methodDecl = typeDecl.getMethods()[0];
		Block block = methodDecl.getBody();
		List<Statement> statements = block.statements();
		WhileStatement whileStatement = null;
		// the whileStatement will be get from user's selection later
		for (Statement eachStatement : statements) {
			if (eachStatement instanceof WhileStatement) {
				whileStatement = (WhileStatement) eachStatement;
			}
		}
		if (whileStatement == null) {
			try {
				throw new Exception("No while statement exists.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Get name of while-loop iterator
		MethodInvocation whileExpression = (MethodInvocation) whileStatement
				.getExpression();
		SimpleName iteratorSimpleName = (SimpleName) whileExpression
				.getExpression();
		String iteratorName = iteratorSimpleName.getIdentifier();

		// Get type binding of while-loop iterator
		ITypeBinding iteratorTypeBinding = iteratorSimpleName
				.resolveTypeBinding();
		if (iteratorTypeBinding == null) {
			try {
				throw new Exception("No binding exists.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// Get while-loop body. Eliminate iterator-related statements.
		Block loopBody = (Block) whileStatement.getBody();

		for (int i = 0; i < loopBody.statements().size(); i++) {
			ExpressionStatement temp = (ExpressionStatement) loopBody
					.statements().get(i);
			MethodInvocation method = (MethodInvocation) temp.getExpression();
			if (method.getExpression() instanceof SimpleName) {
				System.out.println("in if");
				SimpleName relatedName = (SimpleName) method.getExpression();
				String relateName = relatedName.getIdentifier();
				System.out.println(relateName);
				if (relateName.equals(iteratorName)) {
					System.out.println("before remove");
					loopBody.statements().remove(i);
				}
			}
		}
		Block whileLoopBody = (Block) ASTNode.copySubtree(ast, loopBody);
		/*
		 * Find iterator assignment where the iterator's name == while-loop
		 * iterator's name. Then delete it.
		 */
		SimpleName newSetName = null;
		VariableDeclarationStatement iteratorStatement = null;
		for (Statement eachStatement : statements) {
			if (eachStatement instanceof WhileStatement) {
				whileStatement = (WhileStatement) eachStatement;
			}
			if (eachStatement.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				iteratorStatement = (VariableDeclarationStatement) eachStatement;
				VariableDeclarationFragment iteratorAssignment = (VariableDeclarationFragment) iteratorStatement
						.fragments().get(0);
				SimpleName temp = (SimpleName) iteratorAssignment.getName();
				if (temp.getIdentifier().equals(iteratorName)) {
					MethodInvocation assignmentRHS = (MethodInvocation) iteratorAssignment
							.getInitializer();
					SimpleName oldSetName = (SimpleName) assignmentRHS
							.getExpression();
					newSetName = ast.newSimpleName(oldSetName.getIdentifier());
					astRewrite.remove(iteratorStatement, null);
				}
			}
		}

		// get the name of the iterable set
		/*
		 * VariableDeclarationStatement iteratorStatement =
		 * (VariableDeclarationStatement) statements .get(0);
		 * VariableDeclarationFragment iteratorAssignment =
		 * (VariableDeclarationFragment) iteratorStatement .fragments().get(0);
		 * MethodInvocation assignmentRHS = (MethodInvocation)
		 * iteratorAssignment .getInitializer(); SimpleName oldSetName =
		 * (SimpleName) assignmentRHS.getExpression(); SimpleName newSetName =
		 * ast.newSimpleName(oldSetName.getIdentifier());
		 */

		// create a new EnhancedForStatement
		EnhancedForStatement newForStatement = ast.newEnhancedForStatement();

		// create the parameter for the EnhancedForStatement
		SingleVariableDeclaration forParameter = ast
				.newSingleVariableDeclaration();

		// Create Variable Type and Variable Name of the Parameter
		// Create and set variable type
		SimpleName simpleTypeName = ast.newSimpleName(iteratorTypeBinding
				.getTypeArguments()[0].getName());
		SimpleType simpleType = ast.newSimpleType(simpleTypeName);
		forParameter.setType(simpleType);

		// Create and set variable name
		SimpleName simpleVariableName = ast.newSimpleName("s");
		forParameter.setName(simpleVariableName);

		// Set parameter of the EnhancedForStatement
		newForStatement.setParameter(forParameter);

		// Set expression of the EnhancedForStatement
		newForStatement.setExpression(newSetName);

		// Set body of the EnhancedForStatement
		// astRewrite.replace(newForStatement, whileLoopBody, null);
		newForStatement.setBody(whileLoopBody);

		astRewrite.replace(whileStatement, newForStatement, null);

		// astRewrite.remove(iteratorStatement, null);

		org.eclipse.jface.text.Document document = new Document(fCu.getSource());
		textEdits = astRewrite.rewriteAST(document, null);
		try {
			textEdits.apply(document);
		} catch (MalformedTreeException e) {
			e.printStackTrace();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		System.out.println(document.get());

		// need to change the return value
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		final Map arguments = new HashMap();
		String description = refactoringName;
		String comment = refactoringName + "does blah...";
		int flags = JavaRefactoringDescriptor.JAR_MIGRATION
				| JavaRefactoringDescriptor.JAR_REFACTORING
				| RefactoringDescriptor.STRUCTURAL_CHANGE
				| RefactoringDescriptor.MULTI_CHANGE;

		final JavaRefactoringDescriptor descriptor = new JavaRefactoringDescriptor(
				"ourID", "name", description, comment, arguments, flags) {
		};

		MultiTextEdit edit = new MultiTextEdit();
		edit.addChild(astRewrite.rewriteAST());

		CompilationUnitChange compilationUnitChange = new CompilationUnitChange(
				refactoringName, fCu);
		compilationUnitChange.setEdit(edit);
		return compilationUnitChange;
	}

	public RefactoringStatus initialize(Map fArguments) {
		return null;
	}

}
