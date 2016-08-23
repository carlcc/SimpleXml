package xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

/**
 * A simple Xml parser, it does not support DTD or Schema now.
 * Created by carl on 2016/4/7.
 */
public class XmlParser {
	private static final String TAG = "XmlParser";

	private DoubleBuffer doubleBuffer;
	private Stack<XmlNode> stack;


	public XmlParser(InputStream is) throws IOException {
		this.stack = new Stack<XmlNode>();
		this.doubleBuffer = new DoubleBuffer(is);
	}

	/**
	 * Parsing a complete Xml data stream
	 * @return The root element of this xml
	 * @throws IOException #
	 */
	public XmlFile parseFile() throws IOException {
		int ch;

		XmlFile xmlFile = new XmlFile();
		ch = doubleBuffer.read();
		if (ch != '<') {
			// if the first character is not '<', we think this xml has no XML declare.
			doubleBuffer.backTrack(1);
			xmlFile.root = parse();
			return xmlFile;
		}
		ch = doubleBuffer.read();
		if (ch != '?') {
			// if it is not start with "<?", we think this xml has no XML declare.
			doubleBuffer.backTrack(2);
			xmlFile.root = parse();
			return xmlFile;
		}
		// start with "<?"
		matchXmlHeader(xmlFile);

		xmlFile.root = parse();
		return xmlFile;
	}

	/**
	 * Parsing an xml fragment. (Or specifically, an Xml element)
	 * @return The root element of this xml fragment
	 * @throws IOException
	 */
	public XmlNode parse() throws IOException {
		int ch;
		//noinspection StatementWithEmptyBody
		while ((ch = doubleBuffer.read()) != -1) {
			if (Character.isWhitespace(ch)) {
				continue;
			}
			if (ch == '<') {
				ch = doubleBuffer.read();
				if (ch == '!')
					matchComment();
				else if (Character.isWhitespace(ch)) {
					throw produceException(XmlSyntaxException.ERROR_START_TAG_SHOULD_NEXT_TO_ANGLE);
				} else if (ch == '/' || ch == '?') {
					throw produceException(XmlSyntaxException.ERROR_WRONG_TAG_NAME);
//			} else if (ch == '"' || ch == '/' || ch == '\\') {
//				throw produceException(XmlSyntaxException.ERROR_WRONG_TAG_NAME);
				} else {
					// if which next to '<' is not SPACE or '!' or '/', we regard it as part of Tag name.
					stack.push(new XmlNode());
					doubleBuffer.backTrack(1);
					matchTag();
					// tag process complete
					return stack.pop();
					// Comments may be here, ignore by now.
				}
			} else {
				throw produceException(XmlSyntaxException.ERROR_UNEXPECTED_CONTENT);
			}
		}
		throw produceException(XmlSyntaxException.ERROR_ENDS_UNEXPECTEDLY);
	}

	/**
	 * parsing from "&lt;?", currently, skip all the xml declare
	 * @param xmlFile #
	 * @throws IOException
	 */
	private void matchXmlHeader(XmlFile xmlFile) throws IOException {
		int ch;
		while ((ch = doubleBuffer.read()) != -1) {
			if (ch == '?') {
				ch = doubleBuffer.read();
				if (ch == '>')
					return;
				else
					throw produceException(XmlSyntaxException.ERROR_UNEXPECTED_HEADER_TOKEN);
			}
		}
		throw produceException(XmlSyntaxException.ERROR_ENDS_UNEXPECTEDLY);
	}

	/**
	 * match from"<!" to comment end, the whole comment will be discarded
	 */
	private void matchComment() throws IOException {
		int ch;
		ch = doubleBuffer.read();
		if (ch == -1) {
			throw produceException(XmlSyntaxException.ERROR_ENDS_UNEXPECTEDLY);
		} else if (ch != '-') {
			throw produceException('-', (char) ch);
		}
		ch = doubleBuffer.read();
		if (ch == -1) {
			throw produceException(XmlSyntaxException.ERROR_ENDS_UNEXPECTEDLY);
		} else if (ch != '-') {
			throw produceException('-', (char) ch);
		}
		// comment starts, there are at least 2 characters belongs to comment. Thus, invoke read twice.
		if (doubleBuffer.read() == -1)
			throw produceException("Unclosed Comment.");
		if (doubleBuffer.read() == -1)
			throw produceException("Unclosed Comment.");
		while ((ch = doubleBuffer.read()) != -1) {
			if (ch == '>') {
				// meeting "-->"
				if (doubleBuffer.readChar(1) == '-' && doubleBuffer.readChar(2) == '-')
					return;
				// else the following 2 characters must not be the END SIGN of a comment, read 2 characters.
				if (doubleBuffer.read() == -1)
					throw produceException("Unclosed Comment.");
				if (doubleBuffer.read() == -1)
					throw produceException("Unclosed Comment.");
			} else if (ch != '-') {
				// the following 2 characters must not be the END SIGN of a comment, read 2 characters.
				if (doubleBuffer.read() == -1)
					throw produceException("Unclosed Comment.");
				if (doubleBuffer.read() == -1)
					throw produceException("Unclosed Comment.");
			}
		}
		throw produceException("Unclosed Comment.");
	}

	private WordBuilder wb = new WordBuilder();
	/**
	 * match from '<' to tag end (tag close)
	 */
	private void matchTag() throws IOException {
		matchTagName();
		int ch;

		while ((ch = doubleBuffer.read()) != -1) {
			if (Character.isWhitespace(ch))		//  the first character must not be a whitespace
				continue;
			if (ch == '/') {
				// tag end
				ch = doubleBuffer.read();
				if (ch == '>')
					return;		// 该节点就此处理完
				throw produceException('>', (char) ch);
			} else if (ch == '>') {
				// OPEN-TAG end, the following must be a child / children element(s) or CLOSE-TAG or inner text
				matchNodeOrCloseTagOrInnerTextOrComment();
				return;
			} else if (ch == '_' || Character.isAlphabetic(ch)) {
				// read properties/attributes
				doubleBuffer.backTrack(1);
				matchTagAttribute();
			}
		}
	}

	private void matchTagName() throws IOException {
		int ch;
		wb.clear();
		while ((ch = doubleBuffer.read()) != -1) {
			if (Character.isWhitespace(ch) || ch == '/' || ch == '>') {
				stack.peek().setName(wb.toString());
				doubleBuffer.backTrack(1);
				return;
			} else {
				wb.append(ch);
			}
		}
	}

	/**
	 * match a group of attributes, you must ensure there are at least 1 attribute.
	 */
	private void matchTagAttribute() throws IOException {
		int ch;
		wb.clear();
		String attrName = null;
		String attrValue = null;
		while ((ch = doubleBuffer.read()) != -1) {
			if (ch == '-' || ch == '_' || Character.isAlphabetic(ch) || Character.isDigit(ch) || ch == ':') {
				wb.append(ch);
			} else if (Character.isWhitespace(ch)) {
				// stop parsing attribute name
				while ((ch = doubleBuffer.read()) != -1) {
					if (Character.isWhitespace(ch))
						continue;
					if (ch == '=')
						break;
					throw produceException('=', (char) ch);
				}
				// meets an '=' sign
				attrName = wb.toString();
				attrValue = matchAttributeValue();
				stack.peek().setAttr(attrName, attrValue);
				return;
			} else if (ch == '=') {
				// match an attribute value
				attrName = wb.toString();
				attrValue = matchAttributeValue();
				stack.peek().setAttr(attrName, attrValue);
				return;
			}
		}
		throw produceException(XmlSyntaxException.ERROR_ENDS_UNEXPECTEDLY);
	}

	/**
	 * Parsing after '=', the next character might be WHITESPACE or ' or ", the method match an attribute value string.
	 * @return attribute value
	 */
	private String matchAttributeValue() throws IOException {
		int ch;
		wb.clear();

		char quot = 0;
		while ((ch = doubleBuffer.read()) != -1) {
			if (ch == '"') {
				quot = '"';
				break;
			}
			if (ch == '\'') {
				quot = '\'';
				break;
			}
			if (!Character.isWhitespace(ch))
				throw produceException('"', (char) ch);
		}

		while ((ch = doubleBuffer.read()) != -1) {
			if (ch == '&') {
				wb.append(matchEscape());
			} else if (ch == quot) {
				return wb.toString();
			} else if (ch == '>' || ch == '<') {
				throw produceException(XmlSyntaxException.ERROR_VALUE_STRING_SHOULD_NOT_CONTAIN_RESERVES);
			} else if (Character.isWhitespace(ch)) {
				wb.append(' ');	// replace WHITESPACE with WHITESPACE_KEY(' ')
			} else {
				wb.append(ch);
			}
		}

		throw produceException(XmlSyntaxException.ERROR_ENDS_UNEXPECTEDLY);
	}

	private void matchNodeOrCloseTagOrInnerTextOrComment() throws IOException {
		int ch;
		wb.clear();
		while ((ch = doubleBuffer.read()) != -1) {
			if (Character.isWhitespace(ch))
				continue;
			if (ch == '<') {
				// this should be an new Element or a close tag or a commetn.
				ch = doubleBuffer.read();
				if (ch == '/') {
					// close tag
					matchCloseTag();
					return;
				} else if (ch == '!') {
					// comment
					matchComment();
					continue;
				} else {
					// child/children
					doubleBuffer.backTrack(2);
					XmlNode child = parse();
					child.setParent(stack.peek());
					child.getParent().addChild(child);
					continue;
				}
			}
//			else {
				// match inner text
			doubleBuffer.backTrack(1);
			matchInnerText();
			// when matchInnerText done the next character must be  '<'
			doubleBuffer.backTrack(1);

//			ch = doubleBuffer.read();
//			if (ch == '/') {
//				matchCloseTag();
//				return;
//			} else {
//				throw produceException(XmlSyntaxException.ERROR_END_TAG_SHOULD_NEXT_TO_SLASH);
//			}
//			}
		}
		throw produceException(XmlSyntaxException.ERROR_ENDS_UNEXPECTEDLY);
	}

	/**
	 * match from "</"  to close tag end.
	 */
	private void matchCloseTag() throws IOException {
		int ch;
		wb.clear();
		ch = doubleBuffer.read();
		if (Character.isWhitespace(ch))
			throw produceException(XmlSyntaxException.ERROR_END_TAG_SHOULD_NEXT_TO_SLASH);
		doubleBuffer.backTrack(1);
		while ((ch = doubleBuffer.read()) != -1) {
			if (Character.isWhitespace(ch) || ch == '>') {
				// tag name end
				break;
			}
			wb.append(ch);
		}
		if (!stack.peek().getName().equals(wb.toString())) {
			// OPEN TAG and CLOSE TAG name mismatch
			throw produceException(XmlSyntaxException.ERROR_OPEN_CLOSE_TAG_NAME_NOT_MATCH);
		}
		if (ch == '>')
			return;
		while ((ch = doubleBuffer.read()) != -1) {
			if (Character.isWhitespace(ch))
				continue;
			if (ch == '>')
				return;
			throw produceException(XmlSyntaxException.ERROR_CLOSE_TAG_SHOULD_CONTAIN_ONLY_TAG_NAME);
		}
		throw produceException(XmlSyntaxException.ERROR_ENDS_UNEXPECTEDLY);
	}

	private void matchInnerText() throws IOException {
		int ch;
		wb.clear();
		// the next character must be content.

		while ((ch = doubleBuffer.read()) != -1) {
			if (ch == '\n' || ch == '\r') {
				while ((ch = doubleBuffer.read()) != -1)
					if (!Character.isWhitespace(ch))
						break;
				doubleBuffer.backTrack(1);
				continue;
			}
			if (ch == '\t')			// ignore tab
				continue;
			if (Character.isWhitespace(ch)) {
				wb.append(' ');
//				while ((ch = doubleBuffer.read()) != -1) {
//					if (Character.isWhitespace(ch))
//						continue;
//					doubleBuffer.backTrack(1);
//					break;
//				}
			} else if (ch == '&') {
				// escape symbol
				wb.append(matchEscape());
			} else if (ch == '<') {
				// parsing inner text end
				stack.peek().setText(wb.toString());
				return;
			} else {
				// even '>' is allowed in inner text
				wb.append(ch);
			}
		}
		throw produceException(XmlSyntaxException.ERROR_ENDS_UNEXPECTEDLY);
	}

	private char matchEscape() throws IOException {
		int ch;
		WordBuilder escapeWb = new WordBuilder(8);

		while ((ch = doubleBuffer.read()) != -1) {
			if (ch == ';') {
				return getEscape(escapeWb.toString());
			} else {
				if (escapeWb.fullFilled())
					throw produceException(XmlSyntaxException.ERROR_UNRECOGNIZED_ESCAPE_TOKEN);
				escapeWb.append(ch);
			}
		}
		return 0;
	}

	private boolean isHexCharacter(char c) {
		if (c >= '0' && c <= '9' ||
				c >= 'A' && c <= 'F' ||
				c >= 'a' && c <= 'f')
			return true;
		return false;
	}

	private char getEscape(String escape) {
		if (escape.charAt(0) == '#') {
			if (escape.charAt(1) == 'x') {
				try {
					return (char)Integer.parseInt(escape.substring(2, escape.length()), 16);
				} catch (NumberFormatException ignore) {
					return 0;
				}
			} else if (isHexCharacter(escape.charAt(1))) {
				try {
					return (char) Integer.parseInt(escape.substring(2, escape.length()));
				} catch (NumberFormatException ignore) {
					return 0;
				}
			} else {
				throw produceException(XmlSyntaxException.ERROR_UNRECOGNIZED_ESCAPE_TOKEN);
			}
		}
		if ("gt".equals(escape))
			return '>';
		if ("lt".equals(escape))
			return '<';
		if ("amp".equals(escape))
			return '&';
		if ("quot".equals(escape))
			return '"';
		if ("apos".equals(escape))
			return '\'';
		return 0;
	}

	private XmlSyntaxException produceException(String message) {
		return new XmlSyntaxException(message + " Context: " + doubleBuffer.getContext());
	}

	private XmlSyntaxException produceException(char expect, char actual) {
		return new XmlSyntaxException("'" + expect + "' is expected, but actually '" + actual +
				"' is got. The context: " + doubleBuffer.getContext());
	}
}
