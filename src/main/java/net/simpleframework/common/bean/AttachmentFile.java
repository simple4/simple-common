package net.simpleframework.common.bean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

import net.simpleframework.common.AlgorithmUtils;
import net.simpleframework.common.ID;
import net.simpleframework.common.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class AttachmentFile implements Serializable {
	private final File attachment;

	/* 摘要值 */
	private String md5;

	/* 标题 */
	private String topic;

	/* 下载次数 */
	private int downloads;

	/* 文件大小 */
	private long size;

	/* 描述 */
	private String description;

	private String id;

	public AttachmentFile(final File attachment) throws IOException {
		this(attachment, null);
	}

	public AttachmentFile(final File attachment, final String md5) throws IOException {
		this.attachment = attachment;
		this.md5 = StringUtils.hasText(md5) ? md5 : AlgorithmUtils.md5Hex(new FileInputStream(
				attachment));
	}

	public File getAttachment() {
		return attachment;
	}

	public String getTopic() {
		if (StringUtils.hasText(topic)) {
			return topic;
		}
		return getAttachment().getName();
	}

	public AttachmentFile setTopic(final String topic) {
		this.topic = topic;
		return this;
	}

	public int getDownloads() {
		return downloads;
	}

	public AttachmentFile setDownloads(final int downloads) {
		this.downloads = downloads;
		return this;
	}

	public String getDescription() {
		return description;
	}

	public AttachmentFile setDescription(final String description) {
		this.description = description;
		return this;
	}

	public String getMd5() {
		return md5;
	}

	public AttachmentFile setMd5(final String md5) {
		this.md5 = md5;
		return this;
	}

	public String getId() {
		if (id == null) {
			id = ID.Gen.uuid().toString();
		}
		return id;
	}

	public AttachmentFile setId(final String id) {
		this.id = id;
		return this;
	}

	public long getSize() {
		return size > 0 ? size : getAttachment().length();
	}

	public AttachmentFile setSize(final long size) {
		this.size = size;
		return this;
	}

	private static final long serialVersionUID = -7838133087121815920L;
}
