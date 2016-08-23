package xml;

import java.io.*;

/**
 * Created by carl on 2016/4/13.
 */
public class UTF8Reader extends Reader {

	private static final int[] MASKS = {
		0xff, 0x7f, 0x3f, 0x1f, 0x0f, 0x07, 0x03, 0x01
	};

	private InputStream inputStream;

	public UTF8Reader(InputStream is) {
		this.inputStream = is;
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		int ch;
		for (int i = 0; i < len; ++i) {
			ch = readChar();
			if (ch == -1) {
				if (i == 0)
					return -1;
				return i;
			}
			cbuf[i + off] = (char) ch;
		}
		return len;
	}

	@Override
	public void close() throws IOException {
		this.inputStream = null;
	}

	private int readChar() throws IOException {
		int ch;
		int result;

		ch = inputStream.read();
		if (ch == -1)
			return -1;
		int count1 = countLeadingOne((byte) ch);
		result = ch & MASKS[count1+1];
		for (int i = 1; i < count1; ++i) {
			ch = inputStream.read();
			if (ch == -1)
				return -1;
			result <<= 6;
			result |= ch & 0x3f;
		}

		return result;
	}

	private int countLeadingOne(byte n) {
		int count = 0;
		int i = 0;
		while (((n<<i++) & 0x80) != 0)
			count++;
		return count;
	}
}
