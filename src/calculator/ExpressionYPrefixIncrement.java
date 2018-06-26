package calculator;

import lombok.AllArgsConstructor;
import lombok.experimental.ExtensionMethod;

@ExtensionMethod(String.class)
@AllArgsConstructor
public class ExpressionYPrefixIncrement
		implements ExpressionPrefixIncrement, ExpressionVectorElement {
	Expression array;
	
	@Override
	public Object eval(Scope scope) {
		Object obj = array.eval(scope);
		
		if (!(obj instanceof Number[]))
			throw new TypeError("not an array");
		
		Number[] array = (Number[]) obj;
		
		if (array.length != 2 && array.length != 3)
			throw new DimensionError();
		
		return array[1] = array[1].plus(Real.ONE);
	}
	
	@Override
	public Object evalReference(Scope scope) {
		return array.eval(scope);
	}
	
	@Override
	public String toEvalString() {
		return "++" + getNameString();
	}
	
	@Override
	public String getNameString() {
		return array.toEvalString() + ".y";
	}
	
	@Override
	public String toCompiledString() {
		return "<INC <.Y %s>>".format(
				(Object) array.toCompiledString());
	}
	
	@Override
	public String toString() {
		return "YPrefixIncrement{array=%s}".format(array);
	}
	
	@Override
	public void accept(Visitor v) {
		v.visitPrefixIncrement(this);
	}
	
}
