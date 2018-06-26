package calculator;

public class SyntaxError extends CalculatorError {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8101770738717087361L;
	
	public SyntaxError() {
		super("syntax");
	}
	
	public SyntaxError(int pos, int c) {
		super("syntax: at position " + pos + ", character '"
				+ new String(Character.toChars(c)) + "'");
	}
	
	public SyntaxError(int pos, TokenKind expected, TokenKind got) {
		super("syntax: at position " + pos + ": expected " + expected
				+ ", got " + got);
	}
	
	public SyntaxError(int pos, String msg) {
		super("syntax: at position " + pos + ": " + msg);
	}
	
	public SyntaxError(Token token) {
		super("syntax: at position " + token.pos + " on token " + token);
	}
}
