package xml;

import java.io.*;

/**
 * Created by carl on 2016/4/7.
 */
public class XmlFile {
	XmlNode root;

	public XmlNode getRoot() {
		return this.root;
	}

	public static void write(String file, String firstLine, XmlNode root, String encoding) {
		write(new File(file), firstLine, root, encoding);
	}

	public static void write(File file, String firstLine, XmlNode root, String encoding) {
		file.getParentFile().mkdirs();
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		OutputStreamWriter osw = null;
		PrintWriter pw = null;
		try {
			fos = new FileOutputStream(file);
			bos = new BufferedOutputStream(fos);
			osw = new OutputStreamWriter(bos, encoding);
			pw = new PrintWriter(osw);
			if (firstLine != null) {
				pw.println(firstLine);
			}
			pw.print(root.toString());
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (pw != null) pw.close();
				if (osw != null) osw.close();
				if (bos != null) bos.close();
				if (fos != null) fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
