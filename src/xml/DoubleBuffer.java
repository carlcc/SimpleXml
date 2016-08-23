package xml;


import java.io.*;

/**
 * Created by carl on 2016/4/7.
 */
public class DoubleBuffer implements Closeable {
	private static final int SINGLE_BUFFER_SIZE = 4096;

	private char[][] buffer;
	/**
	 * which buffer currently using
	 */
	private int bufferIndex;
	/**
	 * offset in current buffer
	 */
	private int inBufferIndex;
	/**
	 * how many valid characters in each buffer.
	 */
	private int[] count;

	/**
	 * which buffer is newer
	 */
	private int newerBuffer;

	private WordBuilder wordBuilder;

	private Reader reader;

	public DoubleBuffer(InputStream is) throws UnsupportedEncodingException {
		this(is, "utf-8");
	}

	public DoubleBuffer(InputStream is, String charset) throws UnsupportedEncodingException {
		reader = charset == null ? new InputStreamReader(is) : new InputStreamReader(is, charset);
		count = new int[]{0, 0};
		buffer = new char[2][SINGLE_BUFFER_SIZE];
		wordBuilder = new WordBuilder();
		bufferIndex = 0;
		inBufferIndex = 0;
		newerBuffer = -1;
	}

	@Override
	public void close() throws IOException {
		if (reader != null)
			reader.close();
	}

	/**
	 * load character sequence from input stream until the buffer is
	 * full or no more character in input stream.
	 * @param buffer #
	 * @return The number of characters actually read.
	 * @throws IOException #
     */
	private int loadSequence(char[] buffer) throws IOException {
		int cnt = 0;
		int len = buffer.length;
		while (true) {
			int c = reader.read(buffer, cnt, len - cnt);
			if (c < 0)
				return cnt;
			cnt += c;
			if (cnt == len)
				return cnt;
		}
	}

	public int read() throws IOException {
		if (inBufferIndex == 0 && bufferIndex != newerBuffer) {
			// This means current buffer should be newer now, we should update current buffer.
			count[bufferIndex] = loadSequence(buffer[bufferIndex]);
			newerBuffer = bufferIndex;
		}
		// There are more character(s) in current buffer, read and return it.
		if (inBufferIndex < count[bufferIndex])
			return buffer[bufferIndex][inBufferIndex++] & 0x0000ffff;
		// When reaching here, it means all the valid characters in current buffer are read.
		// We assert that the input stream reaches EOF if the number of valid characters is less than SINGLE_BUFFER_SIZE.
		// So here we return -1 as the sign of EOF.
		if (count[bufferIndex] < SINGLE_BUFFER_SIZE)
			return -1;

		// else, there are more characters, load and read it.
		inBufferIndex = 0;
		bufferIndex = 1 - bufferIndex;
		return read();
	}

	/**
	 * Read the character which has an offset relative to the character we last read(). This method will NOT change
	 * the reading position. <br/>
	 * <FONT color="red">NOTE: If you try to read a character after the last read, the return value might be wrong. DO NOT USE.</FONT>
	 * @param offset The offset relative to last read(), 0 - represents the last read()，positive means to read a character before last read(), DO NOT input a negative.
	 *               And this number should not be too large.
	 * @return #
	 */
	public char readChar(int offset) {
		int index = inBufferIndex - 1 - offset;
		if (index < 0) {
			index += SINGLE_BUFFER_SIZE;
			return buffer[1 - bufferIndex][index];
		}
		return buffer[bufferIndex][index];
	}

	/**
	 * backtrack, move the reading position to several characters back. This method assumes that
	 * @param n Number of characters to backtrack, should not be too large and should not be a negative.
	 */
	public void backTrack(int n) {
		inBufferIndex -= n;
		if (inBufferIndex < 0) {
			// 2 situations:
			// 1) original inBufferIndex == SINGLE_BUFFER_SIZE
			// 2) original inBufferIndex < SINGLE_BUFFER_SIZE
			inBufferIndex += SINGLE_BUFFER_SIZE;
			// it seems the following line is redundant? I forgot why I wrote this line.
			newerBuffer = bufferIndex;
			bufferIndex = 1 - bufferIndex;
		}
	}

	/**
	 * Get the context around the reading position.
	 * @return #
	 */
	public String getContext() {
		int lastIndex = inBufferIndex - 1;
		int startIndex = lastIndex - 100;
		int endIndex = lastIndex + 100;
		char []chars = new char[200];

		int j = 0;
		if (startIndex < 0) {
			startIndex += SINGLE_BUFFER_SIZE;
			int bufIndex = 1 - bufferIndex;
			for (int i = startIndex; i < SINGLE_BUFFER_SIZE; ++i)
				chars[j++] = buffer[bufIndex][i];
		} else {
			for (int i = startIndex; i < lastIndex; ++i)
				chars[j++] = buffer[bufferIndex][i];
		}
		if (endIndex > count[bufferIndex])
			endIndex = count[bufferIndex] - 1;
		for (int i = lastIndex; i < endIndex; ++i) {
			chars[j++] = buffer[bufferIndex][i];
		}
		return new String(chars, 0, j);
	}

	public String nextLine() throws IOException {
		wordBuilder.clear();
		int ch;
		while ((ch = read()) != -1) {
			if (ch == '\r') {
				if (!((ch = read()) == '\n'))
					backTrack(1);
				break;
			}
			if (ch == '\n')
				break;
			wordBuilder.append(ch);
		}
		return wordBuilder.toString();
	}
}

