package xml;

/**
 * Created by carl on 2016/4/7.
 */
public class WordBuilder {
	private static final int DEFAULT_SIZE = 4096;

	private char[] buffer;
	private int index;

	public WordBuilder(int bufferSize) {
		buffer = new char[bufferSize];
		index = 0;
	}

	public WordBuilder() {
		this(DEFAULT_SIZE);
	}

	public WordBuilder append(char ch) {
		if (fullFilled()) {
			char[] newBuf = new char[buffer.length * 2];
			System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
			buffer = newBuf;
		}
		buffer[index++] = ch;
		return this;
	}

	public boolean fullFilled() {
		return index == buffer.length;
	}

	public WordBuilder append(int ch) {
		return append((char) ch);
	}

	public void clear() {
		index = 0;
	}

	public String toString() {
		return new String(buffer, 0, index);
	}
}

