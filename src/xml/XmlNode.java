package xml;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by carl on 2016/4/7.
 */
public class XmlNode {
	private XmlNode parent;

	private String name;
	private String text;

	private Map<String, String> attributes;
	private List<XmlNode> children;

	XmlNode() {
		defaultSetting();
	}

	public XmlNode(String name) {
		this();
		this.name = name;
	}

	private void defaultSetting() {
		parent = null;
		text = "";
		attributes = new LinkedHashMap<>();
		children = new ArrayList<>();
	}

	private String procString(String str) {
		str = str.replace("&", "&amp;");
		str = str.replace("<", "&lt;");
		str = str.replace(">", "&gt;");
		str = str.replace("'", "&apos;");
		str = str.replace("\"", "&quot;");
		return str;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public XmlNode getParent() {
		return parent;
	}

	public void setParent(XmlNode parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void removeAttr(String attrName) {
		attributes.remove(attrName);
	}

	public void setAttr(String attrName, String val) {
		attributes.put(attrName, val);
	}

	public void addChild(XmlNode child) {
		children.add(child);
	}

	public Map<String, String> getAttrs() {
		return this.attributes;
	}

	public String getAttr(String attr) {
		return this.attributes.get(attr);
	}

	public List<XmlNode> getChildrenList() {
		return this.children;
	}

	public List<XmlNode> getChildrenList(String name) {
		List<XmlNode> result = getChildrenList().stream().filter(node -> node.getName().equals(name)).collect(Collectors.toList());
		return result;
	}

	public XmlNode getChild(String name) {
		List<XmlNode> l = getChildrenList(name);
		if (l.size() == 0)
			return null;
		return l.get(0);
	}

	public XmlNode getChildWithAttr(String name, String attrName, String attrValue) {
		for (XmlNode node : getChildrenList(name)) {
			String value = node.getAttr(attrName);
			if (value == null)
				continue;
			if (value.equals(attrValue))
				return node;
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, 0);
		return sb.toString();
	}

	private void toString(StringBuilder sb, int ident) {
		for (int i = 0; i < ident; ++i)
			sb.append('\t');
		sb.append('<');
		sb.append(getName());
		for (Map.Entry<String, String> entry : getAttrs().entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			sb.append(' ').append(key).append('=').append('"')
					.append(procString(value)).append('"');
		}

		List<XmlNode> children = getChildrenList();
		if (children.size() != 0) {
			sb.append(">\n");
			for (XmlNode child : children) {
				child.toString(sb, ident+1);
			}
			for (int i = 0; i < ident; ++i)
				sb.append('\t');
			sb.append("</").append(getName()).append(">\n");
		} else if (getText() != null && !getText().isEmpty()) {
			sb.append(">").append(getText());
			sb.append("</").append(getName()).append(">\n");
		} else {
			sb.append("/>\n");
		}
	}
}
