package com.nexus.ytdui;

import com.nexus.ytd.YTFormat;
import com.nexus.ytd.YTFormatInfo;
import com.nexus.ytd.YTFormats;
import com.nexus.ytd.YTSearchResult;

import java.util.ArrayList;

class Utils {

	static void printFormatsTable(ArrayList<YTFormat> list) {
		// Create String table with data
		int numOfRows = list.size() + 1;
		int numOfColumns = 7;
		String[][] table = new String[numOfRows][];

		// Header
		table[0] = new String[] {"Itag", "Type", "Container", "Resolution", "Video Encoding", "Audio Encoding", "Audio Bitrate"};

		// Actual data
		for (int i = 1; i < numOfRows; i++) {
			String itag = list.get(i - 1).getItag();
			YTFormatInfo f = YTFormats.ItagsMap.get(itag);
			table[i] = new String[] { itag, f.Type.toString(), f.Container, f.VideoResolution, f.VideoEncoding, f.AudioEncoding, f.AudioBitrate };
		}

		// Calculate width for each column
		int[] colWidths = new int[numOfColumns];
		for (int j = 0; j < numOfColumns; j++) {
			for (int i = 0; i < numOfRows; i++) {
				if (table[i][j] == null) continue;
				int length = table[i][j].length();
				if (colWidths[j] < length) colWidths[j] = length;
			}
		}

		// Print table
		for (int i = 0; i < numOfRows + 1; i++) {
			// Add horizontal line around header and after last line
			if (i < 2 || i == numOfRows) {
				for (int j = 0; j < numOfColumns; j++) {
					System.out.print("+");
					for (int k = 0; k < colWidths[j] + 2; k++) System.out.print("-");
				}
				System.out.print("+\n");
			}

			// Print lines
			if (i < numOfRows) {
				for (int j = 0; j < numOfColumns; j++) {
					System.out.format("| %-" + colWidths[j] + "s ", (table[i][j] != null) ? table[i][j] : "");
				}
				System.out.print("|\n");
			}
		}
	}

	static void printFormat(YTFormatInfo info) {
		if (info == null) return;
		boolean audio = false, video = false;
		switch (info.Type) {
			case AV:
			case AV_LIVE:
				audio = video = true;
				break;
			case AUDIO:
				audio = true;
			break;
			case VIDEO:
				video = true;
			break;
		}

		System.out.println("Type: " + info.Type);
		if (video || audio)
			System.out.println("Container: " + info.Container);
		if (video) {
			System.out.println("Video Resolution: " + info.VideoResolution);
			System.out.println("Video Encoding: " + info.VideoEncoding);
			System.out.println("Video Profile: " + info.VideoProfile);
			System.out.println("Video Bitrate: " + info.VideoBitrate);

		}
		if (audio) {
			System.out.println("Audio Encoding: " + info.AudioEncoding);
			System.out.println("Audio Bitrate: " + info.AudioBitrate);
		}
	}

	static void printSearchResult(YTSearchResult result) {
		System.out.println("\"" + result.getId() + "\": {\n\ttitle: \"" + result.getTitle() + "\",\n\tauthor: \"" + result.getAuthor() + "\",\n\tthumbnail: \"" + result.getThumbnail() + "\"\n}");
	}

}
