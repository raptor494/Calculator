package calculator;

import static calculator.Functions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;

public class MethodFunction implements Function {
	Method[] methods;
	String name;
	private int maxArgCount, minArgCount;
	private boolean returnsValue;
	
	public MethodFunction(Class<?> containingClass, String name) {
		this.name = name.startsWith("_")? name.substring(1) : name;
		ArrayList<Method> validMethods = new ArrayList<>();
		minArgCount = -1;
		for (Method method : containingClass.getDeclaredMethods()) {
			if (method.getName().equals(name)) {
				int mods = method.getModifiers();
				if (Modifier.isStatic(mods)
						&& Modifier.isPublic(mods)) {
					int argCount = method.getParameterCount();
					if (minArgCount == -1 || argCount < minArgCount)
						minArgCount = argCount;
					
					if (argCount > maxArgCount)
						maxArgCount = argCount;
					validMethods.add(method);
					
					if (method.getReturnType() == void.class)
						returnsValue = false;
				}
			}
		}
		
		if (validMethods.isEmpty())
			throw new RuntimeException("No method found with name "
					+ name + " in " + containingClass);
		
		methods = validMethods.toArray(new Method[0]);
	}
	
	@Override
	public boolean returnsValue() {
		return returnsValue;
	}
	
	@Override
	public int minArgCount() {
		return minArgCount;
	}
	
	@Override
	public int maxArgCount() {
		return maxArgCount;
	}
	
	@Override
	public String getDescription() {
		List<String> descs = new ArrayList<>();
		for (Method method : methods) {
			String s = name + "(";
			Parameter[] paramTypes = method.getParameters();
			for (int i = 0; i < paramTypes.length; i++) {
				if (i != 0)
					s += ", ";
				Class<?> type = paramTypes[i].getType();
				String typename;
				param param = paramTypes[i].getAnnotation(param.class);
				if (param != null) {
					typename = param.value();
				} else if (type == Number.class) {
					typename = "number";
				} else if (type == Number[].class) {
					typename = "array";
				} else if (type == Number[][].class) {
					typename = "matrix";
				} else {
					typename = type.getSimpleName().toLowerCase();
				}
				s += typename;
			}
			s += ")";
			func func = method.getAnnotation(func.class);
			if (func != null && !func.value().isEmpty()) {
				s += " - " + func.value();
				descs.add(0, s);
			} else
				descs.add(s);
		}
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < descs.size(); i++) {
			if (i != 0)
				b.append('\n');
			b.append(descs.get(i));
		}
		return b.toString();
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public Class<?> getDeclaringClass() {
		return methods[0].getDeclaringClass();
	}
	
	public String getQualifiedName() {
		return methods[0].getDeclaringClass().getName() + "."
				+ methods[0].getName();
	}
	
	@Override
	public String toString() {
		return methods[0].getDeclaringClass().getName() + "."
				+ methods[0].getName();
	}
	
	@Override
	public Object call(Scope scope, Object... args) {
		Object result = callOptionalValue(scope, args);
		if (result == null)
			throw new CalculatorError(
					getName() + " does not return a value");
		return result;
	}
	
	@Override
	@SneakyThrows
	public Object callOptionalValue(Scope scope, Object... args) {
		if (args.length < minArgCount())
			throw new DimensionError();
		if (args.length > maxArgCount())
			throw new DimensionError();
		Method method = dispatch(methods, args);
		if (method == null)
			throw new TypeError();
		Object result;
		try {
			result = method.invoke(null, args);
		} catch (InvocationTargetException ex) {
			throw ex.getCause();
		}
		if (result instanceof Boolean)
			return toNumber((Boolean) result);
		else if (result instanceof Integer)
			return Real.valueOf(((Integer) result).doubleValue());
		else if (result instanceof Double)
			return Real.valueOf((Double) result);
		// else if (result == null)
		// return scope.getVariable("ans");
		return result;
	}
	
	public static Method dispatch(Method[] options, Object... args) {
		Class<?>[] givenTypes = new Class<?>[args.length];
		
		for (int i = 0; i < args.length; i++)
			givenTypes[i] = args[i].getClass();
		
		for (Method method : options) {
			Class<?>[] paramTypes = method.getParameterTypes();
			
			if (paramTypes.length != givenTypes.length)
				continue;
			
			boolean matched = true;
			
			for (int i = 0; i < args.length; i++) {
				Class<?> c1 = paramTypes[i];
				Class<?> c2;
				if (c1 != givenTypes[i]) {
					if (args[i] instanceof Double) {
						c2 = Real.class;
						args[i] = Real.valueOf((Double) args[i]);
					} else if (args[i] instanceof Integer) {
						c2 = Real.class;
						args[i] = Real.valueOf((Integer) args[i]);
					} else if (args[i] instanceof Function) {
						c2 = Function.class;
					} else
						c2 = givenTypes[i];
					
					if (c1 != c2 && !c1.isAssignableFrom(c2)) {
						matched = false;
						break;
					}
				}
			}
			
			if (!matched) {
				if (givenTypes.length == 2) {
					operator operator =
							method.getAnnotation(operator.class);
					if (operator.reversible()) {
						matched = true;
						for (int i = 0; i < 2; i++) {
							Class<?> c1 = paramTypes[1 - i];
							Class<?> c2;
							if (c1 != givenTypes[i]) {
								if (args[i] instanceof Double) {
									c2 = Real.class;
									args[i] = Real.valueOf(
											(Double) args[i]);
								} else if (args[i] instanceof Integer) {
									c2 = Real.class;
									args[i] = Real.valueOf(
											(Integer) args[i]);
								} else if (args[i] instanceof Function) {
									c2 = Function.class;
								} else
									c2 = givenTypes[i];
								
								if (c1 != c2
										&& !c1.isAssignableFrom(c2)) {
									matched = false;
									break;
								}
							}
						}
						
						if (matched) {
							Object temp = args[0];
							args[0] = args[1];
							args[1] = temp;
							return method;
						}
						
					}
				}
			} else
				return method;
		}
		
		return null;
	}
}
