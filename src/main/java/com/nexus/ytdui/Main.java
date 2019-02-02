package com.nexus.ytdui;

import com.nexus.ytd.*;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.*;

class Main {

    public static void main(String[] args) {
	    // Get URL
        System.out.print("Enter URL: ");
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

        String url;
        try { url = input.readLine(); }
        catch (IOException e) { return; }

		YTUrl ytUrl = YTUrl.fromUrl(url);
        if (ytUrl == null) {
        	System.out.println("Invalid URL, performing search...");
        	try {
				YTSearchResult[] ytResults = YTSearch.performSearch(url);
				for (YTSearchResult result : ytResults) {
					Utils.printSearchResult(result);
				}
				return;
			} catch (IOException e) {
				System.err.println("Could not perform search");
				e.printStackTrace();
				System.exit(1);
			}
		}

		System.out.println("Protocol: " + ytUrl.getProtocol());
		System.out.println("Video ID: " + ytUrl.getId());
		YTExtractor ytExtractor = new YTExtractor(ytUrl);
		try {
			System.out.println("Url: " + ytExtractor.getYTUrl().getUrl());
			System.out.println("Title: " + ytExtractor.getTitle());
			System.out.println("Author: " + ytExtractor.getAuthor());
			System.out.println("Thumbnail: " + ytExtractor.getThumbnailUrl());
			System.out.println("Full thumbnail: " + ytExtractor.getFullThumbnailUrl());
			System.out.println("Next video title: " + ytExtractor.getNextVideo().getTitle());
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("Extracting formats");

		ArrayList<YTFormat> ytFormats;
        try {
			ytFormats = ytExtractor.getFormats();
		}
		catch (Exception e) {
			System.out.println("Could not extract formats: " + e.getMessage());
			return;
		}
        if (ytFormats.isEmpty()) {
        	System.out.println("No formats found");
        	return;
		}

        System.out.println("Available formats:");
        Utils.printFormatsTable(ytFormats);

        // Read itag from user
        YTFormat ytFormat = null;
        while (ytFormat == null) {
            String itag;
            System.out.print("Enter itag: ");
            try { itag = input.readLine(); }
            catch (IOException e) { return; }

            if (itag != null && !itag.isEmpty()) {
                for (YTFormat ytf : ytFormats) {
                    if (ytf.getItag().equals(itag)) {
                        ytFormat = ytf;
                        break;
                    }
                }
            }
            if (ytFormat == null) {
                System.out.println("Invalid itag!");
            }
        }

        Utils.printFormat(YTFormats.ItagsMap.get(ytFormat.getItag()));

        if (ytFormat.isEncrypted()) {
            YTPlayer player;
			try {
				player = ytExtractor.getPlayer();
			}
			catch (Exception e) {
				System.out.println("Could not get player: " + e.getMessage());
				return;
			}
            player.decryptFormat(ytFormat);
        }

        boolean useDirect = true;
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            System.out.println("Launching URL");
            try {
                desktop.browse(new URI(ytFormat.getURL()));
                useDirect = false;
            }
            catch (Exception e) {
                System.out.println("Could not launch browser: " + e.getMessage());
            }
        }
        if (useDirect) {
            System.out.println("Direct link: " + ytFormat.getURL());
        }

        System.out.println("Done!");

    }
}
