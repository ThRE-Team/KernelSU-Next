package id.thre.term;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.os.Message;

public class ThRE extends Thread implements Runnable {
	private volatile boolean done = false;
	private final Term term;
	private final Process process;
	private final OutputStream stdIn;
	private final BufferedReader stdOut;
	private Thread outReader;

	public ThRE(Term t3rm) {
		term = t3rm;
		Process p = null;
		OutputStream in = null;
		BufferedReader out = null;
		try {
			ProcessBuilder pb = new ProcessBuilder("/system/bin/sh");
			pb.redirectErrorStream(true); // merge stderr into stdout
			p = pb.start();
			in = p.getOutputStream();
			out = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
		} catch (IOException e) {
			e.printStackTrace();
		}
		process = p;
		stdIn = in;
		stdOut = out;
	}

	private void startReaders() {
		if (stdOut != null) {
			outReader = new Thread(new Runnable() {
					@Override
					public void run() {
						readLoop(stdOut);
					}
				});
			outReader.start();
		}
	}

	private void readLoop(BufferedReader reader) {
		try {
			int ch;
			StringBuilder sb = new StringBuilder();
			while (!done && (ch = reader.read()) != -1) {
				sb.append((char) ch);
				String current = sb.toString();
				if (current.contains("[DONE]")) {
					String outputBeforeDone = current.substring(0, current.indexOf("[DONE]"));
					if (!outputBeforeDone.isEmpty()) {
						postToHandler(outputBeforeDone);
					}
					term.showNextPrompt();
					sb.setLength(0);
				} else if (ch == '\n') {
					postToHandler(current);
					sb.setLength(0);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void postToHandler(final String text) {
		if (term == null || term.handle == null) return;
		term.handle.post(new Runnable() {
				@Override
				public void run() {
					String cleanText = text.replaceAll("\\u001b\\[[;\\d]*[A-Za-z]", "");
					term.getUI().output.append(cleanText);

				}
			});
	}

	@Override
	public void run() {
		startReaders();
		try {
			if (process != null) {
				process.waitFor();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			shutdown();
		}
	}

	public synchronized void exec(String command) {
		try {
			if (stdIn != null && process != null) {
				String[] lines = command.split("\n");
				for (String line : lines) {
					if (line.trim().isEmpty()) continue;
					stdIn.write((line + "\n").getBytes(StandardCharsets.UTF_8));
					stdIn.flush();
					try { Thread.sleep(50); } catch (InterruptedException e) {}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void shutdown() {
		done = true;
		try {
			if (stdIn != null) stdIn.close();
		} catch (IOException ignored) {}
		try {
			if (stdOut != null) stdOut.close();
		} catch (IOException ignored) {}
		if (process != null) {
			process.destroy();
		}
		if (outReader != null) {
			outReader.interrupt();
		}
	}
}
