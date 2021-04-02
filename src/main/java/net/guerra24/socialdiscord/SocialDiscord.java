package net.guerra24.socialdiscord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

public class SocialDiscord {

	private static final String BASE_PATH = "config/";

	private static final Gson gson = new Gson();
	private static List<String> ids = new ArrayList<>();

	private static final File file = new File(BASE_PATH + "posts.json");

	private static final File fileHooks = new File(BASE_PATH + "hooks.json");
	private static List<String> hooks = new ArrayList<>();

	private static String reportHook;

	private static String page;

	public static boolean running = true;

	public static final boolean DEV = false;

	private static void loadHooksFile() {
		try (Reader reader = new FileReader(fileHooks);) {
			Type listType = new TypeToken<List<String>>() {
			}.getType();
			hooks = gson.fromJson(reader, listType);
		} catch (Exception e) {
		}
	}

	private static void loadFile() {
		try (Reader reader = new FileReader(file);) {
			Type listType = new TypeToken<List<String>>() {
			}.getType();
			ids = gson.fromJson(reader, listType);
		} catch (Exception e) {
		}
	}

	private static void saveFile() {
		try (Writer writer = new FileWriter(file)) {
			gson.toJson(ids, writer);
		} catch (Exception e) {
		}
	}

	private static String getImage(String path) {
		int i = 0;
		while (i < 5) {
			try {
				Document doc;
				String url = "https://mobile.facebook.com" + path;
				doc = Jsoup.parse(new URL(url).openStream(), "ISO-8859-1", url);
				Element image = doc.getElementById("MPhotoContent");
				Element imageRoot = image.child(0).child(1).child(0).child(0);
				String imagePath = imageRoot.child(imageRoot.childNodeSize() - 1).child(0).attr("href");
				return imagePath;
			} catch (Exception e) {
				i++;
			}
		}
		return "";
	}

	public static String embedLink(Element story) {
		String ret = "";
		String refBase = story.child(0).child(0).child(0).child(2).html();
		if (refBase.indexOf("l.php?u=") != -1) {
			String refPath = refBase.substring(refBase.indexOf("l.php?u=") + 8, refBase.indexOf("&amp;"));
			try {
				refPath = URLDecoder.decode(refPath, "UTF-8");
			} catch (UnsupportedEncodingException e1) {
			}
			String pageTitle = story.child(0).child(0).child(0).child(2).child(0).child(0).child(0).child(0).child(0)
					.child(1).child(0).html();
			ret = "[" + pageTitle + "](" + refPath + ")";
		}
		return ret;
	}

	public static String readUrl(String urlString) throws Exception {
		URL url = new URL(urlString);

		URLConnection hc = url.openConnection();
		hc.setRequestProperty("User-Agent", HttpConnection.DEFAULT_UA);
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(hc.getInputStream(), "UTF-8"))) {
			StringBuffer buffer = new StringBuffer();
			int read;
			char[] chars = new char[1024];
			while ((read = reader.read(chars)) != -1)
				buffer.append(chars, 0, read);
			return buffer.toString();
		}
	}

	public static String getCombinedText(JsonArray json) {
		JsonArray root = json.get(0).getAsJsonArray();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < root.size(); i++) {
			String current = root.get(i).getAsJsonArray().get(0).toString();
			sb.append(current.substring(1, current.length() - 1));
		}
		return sb.toString();
	}

	public static String convertUTF8(String src) {
		try {
			return new String(src.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			return src;
		}
	}

	public static String cleanText(String post) {
		post = post.replace("\\", "\\\\");
		post = post.replace("\n", "");
		post = post.replace("<br>", "\\n");
		post = post.replace("</p>", "\\n");
		post = post.replace("\"", "\\\"");
		while (true) {
			if (post.indexOf("<") == -1)
				break;
			String replacement = post.substring(post.indexOf("<"), post.indexOf(">") + 1);
			post = post.replace(replacement, "");
		}
		post = post.replace("&lt;", "<");
		post = post.replace("&gt;", ">");
		post = post.replace("&amp;", "&");
		post = post.trim();
		return post;
	}

	public static String postFallback(String path) {
		Document doc;
		String url = "https://mobile.facebook.com" + path;
		try (InputStream stream = new URL(url).openStream()) {
			String content = "";
			try (Scanner scanner = new Scanner(stream)) {
				while (scanner.hasNextLine()) {
					content += scanner.nextLine();
				}
			}
			content = convertUTF8(content);
			doc = Jsoup.parse(content, url);
			Element story = doc.getElementById("MPhotoContent");
			return story.child(0).child(0).child(2).html();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private static void processArticle(String path, String postId) {
		Document doc;
		String url = "https://mobile.facebook.com" + path;

		try (InputStream stream = new URL(url).openStream()) {
			String content = "";
			try (Scanner scanner = new Scanner(stream)) {
				while (scanner.hasNextLine()) {
					content += scanner.nextLine();
				}
			}
			content = convertUTF8(content);
			doc = Jsoup.parse(content, url);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		if (ids.stream().anyMatch(str -> str.trim().equals(postId.trim())))
			return;
		System.out.println("Sending post: " + postId);

		String post = "";
		List<String> imagesPath = new ArrayList<>();

		if (path.startsWith("/story.php")) {
			Element story = doc.getElementById("m_story_permalink_view");

			if (story.child(0).child(0).child(0).childrenSize() > 2) {
				Element imageBase = story.child(0).child(0).child(0).child(2);
				if (imageBase.html().indexOf("/" + page + "/photos/") != -1)
					for (Element image : imageBase.getElementsByAttribute("data-ft").get(0).child(0).children()) {
						String imageText = image.attr("href");
						if (imageText.isEmpty()) {
							imageText = image.child(0).attr("href");
						}
						if (!imageText.isEmpty())
							imagesPath.add(getImage(imageText.substring(0, imageText.indexOf("?type"))));
					}
			}

			// String linkPath = embedLink(story); Fix this
			post = convertUTF8(story.child(0).child(0).child(0).child(1).html());
			if (post.isEmpty()) {
				// fuck me
				String fallbackPath = story.child(0).child(0).child(0).child(2).child(0).child(0).attr("href");
				fallbackPath = fallbackPath.substring(0, fallbackPath.lastIndexOf("/") + 1);
				post = postFallback(fallbackPath);
			}
		} else if (path.startsWith("/" + page + "/photos/")) {
			Element story = doc.getElementById("MPhotoContent");
			post = story.child(0).child(0).child(2).html();
			imagesPath.add(story.child(0).child(1).child(0).child(0).child(1).child(0).attr("href"));
		}
		String linkPath = "";

		post = cleanText(post);

		// Handle fucking emojis
		/*
		 * post = EmojiParser.parseFromUnicode(post, new EmojiTransformer() {
		 * 
		 * @Override public String transform(UnicodeCandidate unicodeCandidate) { if
		 * (unicodeCandidate.hasFitzpatrick()) { return "{" +
		 * unicodeCandidate.getEmoji().getAliases().get(0) + "|" +
		 * unicodeCandidate.getFitzpatrickType() + "}"; } return "{" +
		 * unicodeCandidate.getEmoji().getAliases().get(0) + "}"; } });
		 * 
		 * // Translation post = post.substring(0, post.lastIndexOf("\\n")); post =
		 * post.replace("\\n", "<br>"); post = translate(post); post =
		 * post.replace(" <br> ", "\\n"); post = post.replace("<br>", "\\n"); while
		 * (true) { if (post.indexOf("{") == -1) break; String replacement =
		 * post.substring(post.indexOf("{"), post.indexOf("}") + 1); post =
		 * post.replace(replacement, replacement.toLowerCase().replace("{",
		 * ":").replace("}", ":")); }
		 * 
		 * post += "\\n";
		 * 
		 * post = EmojiParser.parseToUnicode(post);
		 */

		if (!linkPath.isBlank())
			post += "\\n" + linkPath + "\\n";

		post += "\\n[Original Post](https://www.facebook.com/" + page + "/posts/" + postId + ")";
		Random rand = new Random();
		int color = rand.nextInt(0xffffff + 1);
		String payload;

		if (post.length() > 2048) {
			List<String> fields = new ArrayList<>();
			String tmp = post;
			int limit = 2047;
			while (true) {
				if (tmp.length() < 1023) {
					fields.add(tmp);
					break;
				}
				int index = post.lastIndexOf("\\n", limit);
				fields.add(tmp.substring(0, index));
				tmp = tmp.substring(index + 2).trim();
				limit = 1023;
			}
			payload = "{\"embeds\": [{\"description\": \"" + fields.get(0) + "\", \"color\": " + color
					+ ",\"fields\": [";
			for (int i = 1; i < fields.size(); i++) {
				String field = fields.get(i);
				String title = " ‏‏‎ ";
				String content = field;
				if (i == fields.size() - 1)
					payload += "{\"name\": \"" + title + "\",\"value\":\"" + content + "\"}";
				else
					payload += "{\"name\": \"" + title + "\",\"value\":\"" + content + "\"},";
			}
			payload += "]}";
		} else
			payload = "{\"embeds\": [{\"description\": \"" + post + "\",\"color\": " + color + "}";

		for (String img : imagesPath)
			payload += ",{\"color\": " + color + ",\"image\": {\"url\": \"" + img + "\"}}";
		payload += "]}";

		try {
			StringEntity entity = new StringEntity(payload, ContentType.APPLICATION_JSON);

			HttpClient client = HttpClientBuilder.create().build();
			for (String hook : hooks) {
				HttpPost request = new HttpPost(hook);
				request.setEntity(entity);
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json");

				HttpResponse response = client.execute(request);
				if (response.getStatusLine().getStatusCode() != 204) {
					BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

					StringBuffer result = new StringBuffer();
					String line = "";
					while ((line = rd.readLine()) != null) {
						result.append(line);
					}
					System.out.println(payload);
					System.out.println("Error: " + response.getStatusLine().getStatusCode());
					System.out.println(result.toString());
				} else {
					ids.add(0, postId);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void handleHTML() {
		Document doc;
		try {
			doc = Jsoup.connect("https://mobile.facebook.com/" + page).userAgent("Mozilla").get();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		Element recent = doc.getElementById("recent");
		Elements articles = recent.child(0).child(0).children();
		for (Element article : articles) {
			Element inner = article.child(1).child(1);
			if (inner.childrenSize() == 0)
				continue;
			String path = inner.child(0).attr("href");
			if (!(path.startsWith("/story.php") || path.startsWith("/" + page + "/photos/")))
				continue;
			String postId;
			int start = path.indexOf("mf_story_key");
			if (start == -1) {
				try {
					System.out.println("Waiting for post id");
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
				handleHTML();
				return;
			}
			start += path.substring(start).indexOf(".") + 1;
			postId = path.substring(start, start + path.substring(start).indexOf("%3"));
			if (ids.stream().anyMatch(str -> str.trim().equals(postId.trim())))
				continue;
			processArticle(path, postId);
			try {
				System.out.println("Waiting for a bit");
				Thread.sleep(25000);
			} catch (InterruptedException e) {
			}
		}
	}

	private static void notifyError(String message) {
		try {
			String payload = "{\"content\":\"" + message + "\"}";
			StringEntity entity = new StringEntity(payload);

			HttpClient client = HttpClientBuilder.create().build();
			HttpPost request = new HttpPost(reportHook);
			request.setEntity(entity);
			request.setHeader("Accept", "application/json");
			request.setHeader("Content-type", "application/json");

			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() != 204) {
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				System.out.println(result.toString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.out.println("Starting...");
		Thread main = Thread.currentThread();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("Terminating");
			running = false;
			main.interrupt();
		}));
		try (BufferedReader br = new BufferedReader(new FileReader(new File(BASE_PATH + "page.txt")))) {
			page = br.lines().reduce("", String::concat);
		} catch (Exception e) {
			System.out.println("Missing page.txt");
			return;
		}
		try (BufferedReader br = new BufferedReader(new FileReader(new File(BASE_PATH + "report.txt")))) {
			reportHook = br.lines().reduce("", String::concat);
		} catch (Exception e) {
			System.out.println("Report hook mising, disabling");
		}
		loadHooksFile();
		loadFile();
		while (running) {
			try {
				handleHTML();
			} catch (Exception e) {
				e.printStackTrace();
				if (!DEV)
					saveFile();
				System.out.println("Unable to process posts... Notifying");
				if (!DEV && !reportHook.isEmpty())
					notifyError("Unable to process posts...");
				try {
					Thread.sleep(Long.MAX_VALUE);
				} catch (InterruptedException e1) {
				}
				break;
			}
			if (!DEV)
				saveFile();
			try {
				System.out.println("Waiting 5min...");
				Thread.sleep(300000);
			} catch (InterruptedException e) {
			}
		}
		System.out.println("Stopping...");
	}
}
