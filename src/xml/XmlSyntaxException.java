package xml;

/**
 * Created by carl on 2016/4/7.
 */
public class XmlSyntaxException extends RuntimeException {
	public static final String ERROR_ENDS_UNEXPECTEDLY = "Xml ends unexpectedly.";
	public static final String ERROR_UNEXPECTED_HEADER_TOKEN = "Unexpected token in Xml Header.";
	public static final String ERROR_START_TAG_SHOULD_NEXT_TO_ANGLE = "Tag name should be next to angle bracket.";
	public static final String ERROR_END_TAG_SHOULD_NEXT_TO_SLASH = "Tag name should be next to slash";
	public static final String ERROR_OPEN_CLOSE_TAG_NAME_NOT_MATCH = "The the name of open tag and that of close tag not match.";
	public static final String ERROR_CLOSE_TAG_SHOULD_CONTAIN_ONLY_TAG_NAME = "The close tag should contains only tag name";
	public static final String ERROR_UNRECOGNIZED_ESCAPE_TOKEN = "Unrecognized escape token, or you might forget to write '&' as '&amp;'.";
	public static final String ERROR_VALUE_STRING_SHOULD_NOT_CONTAIN_RESERVES = "Reserve characters should not be contained in quotations.";
	public static final String ERROR_UNEXPECTED_TOKEN_IN_TAG_BODY = "Unexpected token in tag body.";
	public static final String ERROR_WRONG_TAG_NAME = "Tag name is not correct.";
	public static final String ERROR_UNEXPECTED_CONTENT = "Unexpected content";

	public XmlSyntaxException() {
	}

	public XmlSyntaxException(String message) {
		super("Syntax Error." + message);
	}

	public XmlSyntaxException(String message, Throwable cause) {
		super("Syntax Error." + message, cause);
	}

	public XmlSyntaxException(Throwable cause) {
		super(cause);
	}
}
