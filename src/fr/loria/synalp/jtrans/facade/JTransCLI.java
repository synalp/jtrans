package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.gui.JTransGUI;
import fr.loria.synalp.jtrans.markup.in.*;
import fr.loria.synalp.jtrans.markup.out.MarkupSaver;
import fr.loria.synalp.jtrans.markup.out.MarkupSaverPool;
import fr.loria.synalp.jtrans.utils.CrossPlatformFixes;
import fr.loria.synalp.jtrans.utils.FileUtils;
import fr.loria.synalp.jtrans.utils.PrintStreamProgressDisplay;
import fr.loria.synalp.jtrans.utils.ProgressDisplay;
import joptsimple.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.LogManager;

public class JTransCLI {

	public MarkupLoader loader;
	public File inputFile;
	public File audioFile;
	public File outputDir;
	public List<String> outputFormats;
	public boolean clearTimes = false;
	public boolean align = true;
	public boolean runAnchorDiffTest = false;


	public final static String[] AUDIO_EXTENSIONS = "wav,ogg,mp3".split(",");
	public final static String[] MARKUP_EXTENSIONS = "jtr,trs,txt,textgrid".split(",");


	private static void printHelp(OptionParser parser) {
		try {
			parser.printHelpOn(System.out);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}


	private static void listMarkupLoaders() {
		MarkupLoaderPool pool = MarkupLoaderPool.getInstance();

		StringBuilder vanilla = new StringBuilder("Vanilla markup loaders:");
		StringBuilder preproc = new StringBuilder("Preprocessors:");

		for (String name: pool.getNames()) {
			StringBuilder appendTo =
					pool.isVanillaLoader(name)? vanilla: preproc;
			appendTo.append("\n    ").append(name).append(" (")
					.append(pool.getDescription(name)).append(")");
		}

		System.out.println(vanilla);
		System.out.println(preproc);
	}


	private static void listMarkupSavers() {
		MarkupSaverPool pool = MarkupSaverPool.getInstance();
		System.out.println("Markup savers:");
		for (String name: pool.getNames()) {
			System.out.println(name + " (" + pool.getDescription(name) + ")");
		}
	}


	public JTransCLI(String[] args) throws ReflectiveOperationException {
		OptionParser parser = new OptionParser() {
			{
				accepts("h", "help screen").forHelp();

				accepts("f", "markup file (jtr, trs, txt, textgrid)")
						.withRequiredArg().ofType(File.class);

				accepts("a", "audio file (wav, ogg, mp3)")
						.withRequiredArg().ofType(File.class);

				accepts("outdir", "output directory")
						.withRequiredArg().ofType(File.class)
						.defaultsTo(new File("."));

				accepts("outfmt", "Output format. Use this argument several " +
						"times to output to several different formats.")
						.withRequiredArg();

				acceptsAll(
						Arrays.asList("i", "infmt"),
						"Input markup loader. If omitted, guess vanilla " +
						"format from filename extension.")
						.withRequiredArg().describedAs("loader");

				accepts("list-infmt",
						"Displays a list of markup loaders to use with --infmt")
						.forHelp();

				accepts("list-outfmt",
						"Displays a list of markup savers to use with --outfmt")
						.forHelp();

				acceptsAll(
						Arrays.asList("C", "clear-times"),
						"Clear manual anchor times before aligning");

				acceptsAll(
						Arrays.asList("N", "no-align"),
						"Don't align after loading the project. Useful to " +
								"convert between formats.");

				accepts(
						"anchor-diff-test",
						"Regenerate anchor times and gauge deviation wrt. " +
								"reference times.");

				acceptsAll(
						Arrays.asList("B", "bypass-cache"),
						"Don't read objects from cache.");
			}
		};


		OptionSet optset = parser.parse(args);

		//----------------------------------------------------------------------

		if (optset.has("h")) {
			printHelp(parser);
			System.exit(0);
		}

		if (optset.has("list-infmt")) {
			listMarkupLoaders();
			System.exit(0);
		}

		if (optset.has("list-outfmt")) {
			listMarkupSavers();
			System.exit(0);
		}

		//----------------------------------------------------------------------

		if (optset.has("bypass-cache")) {
			Cache.READ_FROM_CACHE = false;
			System.out.println("Won't read objects from cache.");
		}

		inputFile = (File)optset.valueOf("f");
		audioFile = (File)optset.valueOf("a");
		outputDir = (File)optset.valueOf("outdir");
		clearTimes = optset.has("C");
		align = !optset.has("N");
		runAnchorDiffTest = optset.has("anchor-diff-test");

		clearTimes |= runAnchorDiffTest;

		if (!align && runAnchorDiffTest) {
			System.err.println("Can't run the anchor diff test without aligning!");
			System.exit(1);
		}

		if (optset.has("infmt")) {
			String className = (String)optset.valueOf("infmt");
			loader = MarkupLoaderPool.getInstance().make(className);
		}

		outputFormats = (List<String>)optset.valuesOf("outfmt");

		//----------------------------------------------------------------------

		for (Object o: optset.nonOptionArguments()) {
			String arg = (String)o;
			int dotIdx = arg.lastIndexOf('.');
			String ext = dotIdx >= 0? arg.substring(dotIdx+1): null;

			if (Arrays.asList(AUDIO_EXTENSIONS).contains(ext)) {
				if (audioFile != null) {
					throw new IllegalArgumentException("audio file already set");
				}
				audioFile = new File(arg);
			}

			else {
				if (inputFile != null) {
					throw new IllegalArgumentException("markup file already set");
				}
				inputFile = new File(arg);
			}
		}

		if (loader == null && inputFile != null) {
			String fn = inputFile.getName().toLowerCase();
			if (fn.endsWith(".jtr")) {
				loader = new JTRLoader();
			} else if (fn.endsWith(".trs")) {
				loader = new TRSLoader();
			} else if (fn.endsWith(".textgrid")) {
				loader = new TextGridLoader();
			} else if (fn.endsWith(".txt")) {
				loader = new RawTextLoader();
			}
		}
	}


	public static void loadLoggingProperties() throws IOException {
		LogManager.getLogManager().readConfiguration(
				JTransCLI.class.getResourceAsStream("/logging.properties"));
	}


	public static void printAnchorDiffStats(List<Integer> diffs) {
		System.out.println("===== ANCHOR DIFF TEST =====");

		int absDiffSum = 0;
		int absDiffMax = 0;
		float sumOfSquares = 0;

		for (Integer d: diffs) {
			int abs = Math.abs(d);
			absDiffSum += abs;
			sumOfSquares += d * d;
			absDiffMax = Math.max(absDiffMax, abs);
		}

		float avg = (float)absDiffSum / diffs.size();
		float variance = sumOfSquares / diffs.size() - avg * avg;
		float stdDev = (float)Math.sqrt(variance);

		System.out.println("Abs diff avg.....: " + avg + " frames");
		System.out.println("Variance.........: " + variance);
		System.out.println("Std dev..........: " + stdDev);
		System.out.println("Worst abs diff...: " + absDiffMax);
	}


	public static void main(String args[]) throws Exception {
		loadLoggingProperties();

		JTransCLI cli = new JTransCLI(args);

		if (!new File("res").exists()) {
			JTransGUI.installResources();
		}

		if (!cli.runAnchorDiffTest &&
				(cli.outputFormats == null || cli.outputFormats.isEmpty()))
		{
			CrossPlatformFixes.setNativeLookAndFeel();
			new JTransGUI(cli);
			return;
		}

		ProgressDisplay progress =
				new PrintStreamProgressDisplay(2500, System.out);

		Project project = cli.loader.parse(cli.inputFile);
		System.out.println("Project loaded.");

		if (null != cli.audioFile) {
			project.setAudio(cli.audioFile);
			System.out.println("Audio loaded.");
		}

		if (cli.clearTimes) {
			project.clearAllAnchorTimes();
			System.out.println("Anchor times cleared.");
		}

		if (cli.align) {
			System.out.println("Aligning...");
			if (cli.clearTimes) {
				project.alignInterleaved(progress);
			} else {
				project.align(true, progress);
			}
			System.out.println("Alignment done.");
		}

		if (cli.runAnchorDiffTest) {
			Project reference = cli.loader.parse(cli.inputFile);
			List<Integer> diffs = reference.anchorFrameDiffs(project);
			printAnchorDiffStats(diffs);
			System.exit(0);
		}

		cli.outputDir.mkdirs();

		for (String fmt: cli.outputFormats) {
			System.out.println("Output: format '" + fmt + "' to directory "
					+ cli.outputDir);

			fmt = fmt.toLowerCase();
			String base = FileUtils.noExt(new File(cli.outputDir,
					cli.inputFile.getName()).getAbsolutePath());

			MarkupSaver saver = MarkupSaverPool.getInstance().make(fmt);
			saver.save(project, new File(base + saver.getExt()));
		}
	}

}
