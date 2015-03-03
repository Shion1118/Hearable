package com.shion1118.hearable.workflow;

import jp.ne.docomo.smt.dev.characterrecognition.constants.ImageContentType;
import jp.ne.docomo.smt.dev.characterrecognition.constants.Lang;

public class RecognitionAsyncTaskParam {
	private ImageContentType imageType;
	private Lang lang;
	private String imagePath;
	private String jobId;

	public ImageContentType getImageType() {
		return imageType;
	}
	public void setImageType(ImageContentType imageType) {
		this.imageType = imageType;
	}
	public Lang getLang() {
		return lang;
	}
	public void setLang(Lang lang) {
		this.lang = lang;
	}
	public String getImagePath() {
		return imagePath;
	}
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
	

	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	
}
