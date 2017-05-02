package com.god.http;

public class HttpInfo {

	/**
	 * http 返回code
	 */
	private int httpCode;
	/**
	 * http 异常
	 */
	private String codeExplain;
	/**
	 * http 返回数据
	 */
	private String result;

	public HttpInfo() {
	}

	public HttpInfo(int httpCode, String codeExplain, String result) {
		this.httpCode = httpCode;
		this.codeExplain = codeExplain;
		this.result = result;
	}

	public int getHttpCode() {
		return httpCode;
	}
	public void setHttpCode(int httpCode) {
		this.httpCode = httpCode;
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getCodeExplain() {
		return codeExplain;
	}
	public void setCodeExplain(String codeExplain) {
		this.codeExplain = codeExplain;
	}
}
