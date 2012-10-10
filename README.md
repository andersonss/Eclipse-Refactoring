Eclipse-Refactoring
====

Source files require integration with platform components of Eclipse plug-in framework, available on http://www.eclipse.org/platform/.

Example of the refactoring:

Before
public void foo(Set<String> set) {
	Iterator<String> itr = set.iterator();
	while (itr.hasNext()) {
		itr.next();
		doSomething();
	}
}

After
public void foo(Set<String> set) {
	for (String tempItr : set) {
		doSomething();
	}
}
