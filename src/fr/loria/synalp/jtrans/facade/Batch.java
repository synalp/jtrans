package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.markup.MarkupLoader;
import fr.loria.synalp.jtrans.markup.TRSLoader;
import fr.loria.synalp.jtrans.markup.TextGridLoader;
import fr.loria.synalp.jtrans.utils.StdoutProgressDisplay;

import java.io.File;
import java.io.FilenameFilter;

public class Batch {
	private static void fatal(File f, Object reason) {
		System.err.println("Error in file " + f + ": " + reason);
		if (reason instanceof Throwable) {
			((Throwable) reason).printStackTrace();
		}
	}

	public static void main(String[] args) {
		final String suffix = args[0];
		final File inDir = new File(args[1]);
		final File outDir = new File(args[2]);

		MarkupLoader loader;

		if (suffix.equals("trs")) {
			loader = new TRSLoader();
		} else if (suffix.equals("textgrid")) {
			loader = new TextGridLoader();
		} else {
			System.err.println("Unknown output format");
			System.exit(1);
			return;
		}

		File[] files = inDir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith("." + suffix);
			}
		});

		for (File f: files) {
			String stripped = f.getName().substring(
					0, f.getName().lastIndexOf('.'));

			if (new File(outDir, stripped + ".jtr").exists()) {
				System.out.println("Skipping " + stripped);
				continue;
			}

			System.out.println("\n\n\n*********************************************");
			System.out.println(f);
			System.out.println("*********************************************\n\n\n");

			String audio = new File(f.getParentFile(), stripped + ".wav")
					.getAbsolutePath();

			if (!f.exists()) {
				fatal(f, "no audio file found");
				continue;
			}

			try {
				Project project = loader.parse(f);
				project.setAudio(audio);
				project.align(true, new StdoutProgressDisplay());

				System.out.println("Alignment done!");

				outDir.mkdirs();

				System.out.println("Writing output...");
				project.saveJson(new File(outDir, stripped + ".jtr"));
				project.savePraat(new File(outDir, stripped + ".w+p.textgrid"), true, true);
				project.savePraat(new File(outDir, stripped + ".w.textgrid"), true, false);
				project.savePraat(new File(outDir, stripped + ".p.textgrid"), false, true);
			} catch (Exception ex) {
				fatal(f, ex);
			}
		}

		System.out.println("All files done");
		System.exit(0);
	}
}
