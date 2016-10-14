/**
 * @file main.java
 * @brief Entry point for simple LISP interpreter.
 *
 * @section License
 *
 *          This file is a part of my Lisp interpreter. Copyright (C) 2016
 *          Robert B. Colton
 *
 *          This program is free software: you can redistribute it and/or modify
 *          it under the terms of the GNU General Public License as published by
 *          the Free Software Foundation, either version 3 of the License, or
 *          (at your option) any later version.
 *
 *          This program is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          General Public License for more details.
 *
 *          You should have received a copy of the GNU General Public License
 *          along with this program. If not, see <http://www.gnu.org/licenses/>.
 **/

package lispinterpreter.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class LISP {
	private static Environment globalEnvironment = new Environment();

	private static class Environment {
		private Map<Object, Object> inner = new HashMap<Object, Object>();
		private Environment outer;
		
		public Environment(Environment outer, Object[] params, Object[] args) {
			this.outer = outer;
			if (params != null) {
				for (int i = 0; i < params.length; ++i) {
					inner.put(params[i], args[i]);
				}
			}
		}

		public Environment() {
			this(null, null, null);
		}

		public void put(Object key, Object value) {
			inner.put(key, value);
		}

		public Object get(Object exp) {
			Object value = inner.get(exp);
			if (value != null)
				return value;
			if (outer != null)
				return outer.get(exp);
			return null;
		}

		public boolean containsKey(Object key) {
			if (inner.containsKey(key))
				return true;
			if (outer != null)
				return outer.containsKey(key);
			return false;
		}
	}

	private static String[] tokenize(String code) {
		return code.replaceAll("\\(", " ( ").replaceAll("\\)", " ) ").trim().split("\\s+");
	}

	private static Object atom(String token) {
		try {
			return Integer.parseInt(token);
		} catch (NumberFormatException e) {
			try {
				return Float.parseFloat(token);
			} catch (NumberFormatException e2) {
				try {
					return Double.parseDouble(token);
				} catch (NumberFormatException e3) {
					return token;
				}
			}
		}
	}

	private static Object read(List<String> tokens) throws Exception {
		if (tokens.isEmpty()) {
			throw new IllegalArgumentException("unexpected EOF while reading");
		}
		String token = tokens.remove(0);

		if (token.equals("(")) {
			List<Object> atoms = new ArrayList<Object>(tokens.size() - 1);
			while (!tokens.get(0).equals(")"))
				atoms.add(read(tokens));
			tokens.remove(0);
			return atoms;
		} else if (token.equals(")")) {
			throw new Exception("unexpected ')'");
		} else {
			return atom(token);
		}
	}

	private static Object parse(String code) throws Exception {
		return read(new ArrayList<String>(Arrays.asList(tokenize(code))));
	}

	private static Object eval(Object exp, Environment env) {
		if (exp instanceof String) {
			return env.get(exp);
		} else if (exp instanceof List) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) exp;
			Object token = list.get(0);
			if (token instanceof String) {
				String symbol = (String) token;
				switch (symbol) {
				case "if": {
					boolean result = !eval(list.get(1), env).equals(0);
					Object action = result ? list.get(2) : list.get(3);
					return eval(action, env);
				}
				case "define": {
					Object result = eval(list.get(2), env);
					env.put(list.get(1), result);
					return result;
				}
				case "set!":
					Object key = list.get(1);
					if (!env.containsKey(key))
						System.out.println(
							"WARNING: setting undefined variable '" + list.get(1) + '\'');
					Object result = eval(list.get(2));
					env.put(key, result);
					return result;
				case "lambda":
					@SuppressWarnings("unchecked")
					List<Object> params = (List<Object>) list.get(1);
					return (Function<Object[], Object>)(x) -> { 
						return eval(list.get(2), new Environment(env, params.toArray(), x));
					};
				case "quote":
					return list.get(1);
				case "repeat":
					int count = (Integer) list.get(1);
					for (int i = 0; i < count; ++i) {
						System.out.println(eval(list.get(2), env));
					}
					return null;
				default:
					Object proc = env.get(token);
					Object[] args = new Object[list.size() - 1];
					for (int i = 0; i < args.length; ++i) {
						args[i] = eval(list.get(i + 1), env);
					}
					if (proc instanceof UnaryOperator) {
						@SuppressWarnings("unchecked")
						UnaryOperator<Object> op = (UnaryOperator<Object>) proc;
						return op.apply(args[0]);
					} else if (proc instanceof BinaryOperator) {
						@SuppressWarnings("unchecked")
						BinaryOperator<Object> op = (BinaryOperator<Object>) proc;
						return op.apply(args[0], args[1]);
					} else if (proc instanceof Function) {
						System.out.println(proc);
						@SuppressWarnings("unchecked")
						Function<Object[], ?> fnc = (Function<Object[], ?>) proc;
						return fnc.apply(args);
					}
				}
			}
		} else if (exp instanceof Number) {
			return exp;
		}
		return null;
	}

	private static Object eval(Object exp) {
		return eval(exp, globalEnvironment);
	}

	private static Number add(Number one, Number two) {
		if (one instanceof Double || two instanceof Double) {
			return one.doubleValue() + two.doubleValue();
		} else if (one instanceof Float || two instanceof Float) {
			return one.floatValue() + two.floatValue();
		} else if (one instanceof Integer || two instanceof Integer) {
			return one.intValue() + two.intValue();
		} else if (one instanceof Long || two instanceof Long) {
			return one.longValue() + two.longValue();
		} else if (one instanceof Short || two instanceof Short) {
			return one.shortValue() + two.shortValue();
		}

		return one.byteValue() + two.byteValue();
	}

	private static Number multiply(Number one, Number two) {
		if (one instanceof Double || two instanceof Double) {
			return one.doubleValue() * two.doubleValue();
		} else if (one instanceof Float || two instanceof Float) {
			return one.floatValue() * two.floatValue();
		} else if (one instanceof Integer || two instanceof Integer) {
			return one.intValue() * two.intValue();
		} else if (one instanceof Long || two instanceof Long) {
			return one.longValue() * two.longValue();
		} else if (one instanceof Short || two instanceof Short) {
			return one.shortValue() * two.shortValue();
		}

		return one.byteValue() * two.byteValue();
	}

	private static Number abs(Number val) {
		if (val instanceof Double) {
			return Math.abs(val.doubleValue());
		} else if (val instanceof Float) {
			return Math.abs(val.floatValue());
		} else if (val instanceof Long) {
			return Math.abs(val.longValue());
		}

		return Math.abs(val.intValue());
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Simple LISP interpreter. Copyright (C) 2016 Robert B. Colton");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		globalEnvironment.put("pi", Math.PI);
		globalEnvironment.put("+", (BinaryOperator<Number>) LISP::add);
		globalEnvironment.put("*", (BinaryOperator<Number>) LISP::multiply);
		globalEnvironment.put("abs", (UnaryOperator<Number>) LISP::abs);

		while (true) {
			System.out.print(">>>");
			String input = br.readLine();
			if (input == null || input.equals("quit")) break;
			try {
				System.out.println(Arrays.toString(tokenize(input)));
				System.out.println(parse(input));
				System.out.println(eval(parse(input)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
