package org.springblade.workflow.odoc.esign.core;

import lombok.extern.slf4j.Slf4j;
import org.springblade.workflow.odoc.esign.config.EsignProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * e签宝 OpenAPI 请求执行器：负责**构造鉴权头**与发送 HTTP 请求。
 *
 * <p>鉴权头移植自官方 Demo {@code EsignHttpHelper#signAndBuildSignAndJsonHeader}，字段固定为：</p>
 * <pre>
 * X-Tsign-Open-App-Id      应用ID
 * X-Tsign-Open-Version-Sdk SDK 版本（官方 Demo 常量 Esign-Sdk-Core1.0）
 * X-Tsign-Open-Ca-Timestamp 毫秒时间戳
 * Accept                   *&#47;*
 * Content-MD5              POST/PUT 为 Base64(MD5(body))；GET/DELETE 为空串
 * Content-Type             application/json; charset=UTF-8
 * X-Tsign-Open-Auth-Mode   Signature
 * X-Tsign-Open-Ca-Signature Base64(HmacSHA256(待签串, secret))
 * </pre>
 *
 * <p>待签串 = {@code METHOD\nAccept\nContentMD5\nContentType\n\n排序后的URL}。</p>
 */
@Slf4j
@Component
public class EsignHttpExecutor {

	/** 官方 Demo 中 EsignCoreSdkInfo 的 SDK 版本常量 */
	private static final String SDK_VERSION = "Esign-Sdk-Core1.0";
	private static final String ACCEPT = "*/*";
	private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
	private static final String CONTENT_TYPE_STREAM = "application/octet-stream";
	private static final String AUTH_MODE = "Signature";

	private final EsignProperties props;
	private final HttpClient client;

	public EsignHttpExecutor(EsignProperties props) {
		this.props = props;
		this.client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofMillis(props.getTimeoutMs()))
			.build();
	}

	/**
	 * 构造鉴权请求头。
	 *
	 * @param apiPath 接口路径（如 {@code /v3/sign-flow/create-by-file}，可带 Query）
	 * @param method  GET / POST / PUT / DELETE
	 * @param body    JSON 报文（GET/DELETE 传 null）
	 */
	public Map<String, String> buildHeaders(String apiPath, String method, String body) {
		String m = method == null ? "GET" : method.toUpperCase();
		String contentMd5;
		if ("GET".equals(m) || "DELETE".equals(m)) {
			contentMd5 = "";
		} else if ("POST".equals(m) || "PUT".equals(m)) {
			contentMd5 = EsignSigner.contentMd5(body);
		} else {
			throw new IllegalArgumentException("不支持的请求方法：" + m);
		}
		Map<String, String> headers = new LinkedHashMap<>();
		headers.put("X-Tsign-Open-App-Id", props.getAppId());
		headers.put("X-Tsign-Open-Version-Sdk", SDK_VERSION);
		headers.put("X-Tsign-Open-Ca-Timestamp", EsignSigner.timestamp());
		headers.put("Accept", ACCEPT);
		headers.put("Content-MD5", contentMd5);
		headers.put("Content-Type", CONTENT_TYPE_JSON);
		headers.put("X-Tsign-Open-Auth-Mode", AUTH_MODE);

		String sortedUrl = EsignSigner.sortApiUrl(apiPath);
		String message = EsignSigner.appendSignData(m, contentMd5, ACCEPT, CONTENT_TYPE_JSON, "", "", sortedUrl);
		headers.put("X-Tsign-Open-Ca-Signature", EsignSigner.signature(message, props.getAppSecret()));
		return headers;
	}

	/**
	 * 发送接口请求，返回响应体字符串。
	 */
	public String request(String apiPath, String method, String body) {
		String m = method == null ? "GET" : method.toUpperCase();
		Map<String, String> headers = buildHeaders(apiPath, m, body);
		HttpRequest.Builder builder = HttpRequest.newBuilder()
			.uri(URI.create(props.getHost() + apiPath))
			.timeout(Duration.ofMillis(props.getTimeoutMs()));
		headers.forEach(builder::header);
		boolean hasBody = "POST".equals(m) || "PUT".equals(m);
		builder.method(m, hasBody
			? HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body)
			: HttpRequest.BodyPublishers.noBody());
		try {
			HttpResponse<String> resp = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
			String respBody = resp.body();
			log.debug("[esign] {} {} -> {} body={}", m, apiPath, resp.statusCode(), respBody);
			return respBody;
		} catch (Exception e) {
			throw new IllegalStateException("调用 e签宝接口失败 " + m + " " + apiPath, e);
		}
	}

	/**
	 * 下载文件字节（签署完成件归档用）。
	 *
	 * <p>下载地址是 e签宝返回的临时直链，<b>不需要</b>签名头，直接 GET 即可。</p>
	 */
	public byte[] download(String url) {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.timeout(Duration.ofMillis(props.getTimeoutMs()))
			.GET()
			.build();
		try {
			HttpResponse<byte[]> resp = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (resp.statusCode() >= 400) {
				throw new IllegalStateException("下载文件失败，HTTP " + resp.statusCode());
			}
			return resp.body();
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("下载文件失败：" + url, e);
		}
	}

	/**
	 * 文件流上传（第二步）：直接 PUT 到官方返回的文件上传地址，
	 * 只需 {@code Content-MD5} 与 {@code Content-Type}，无需签名头。
	 */
	public void uploadStream(String uploadUrl, byte[] content, String contentMd5) {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(uploadUrl))
			.timeout(Duration.ofMillis(props.getTimeoutMs()))
			.header("Content-MD5", contentMd5)
			.header("Content-Type", CONTENT_TYPE_STREAM)
			.PUT(HttpRequest.BodyPublishers.ofByteArray(content))
			.build();
		try {
			HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
			log.debug("[esign] 文件上传 -> {} body={}", resp.statusCode(), resp.body());
		} catch (Exception e) {
			throw new IllegalStateException("上传文件到 e签宝失败", e);
		}
	}
}
