package fr.loria.synalp.jtrans.facade;

import fr.loria.synalp.jtrans.gui.JTransGUI;
import fr.loria.synalp.jtrans.markup.*;
import fr.loria.synalp.jtrans.utils.CrossPlatformFixes;
import fr.loria.synalp.jtrans.utils.FileUtils;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.LogManager;

public class JTransCLI {

	public MarkupLoader loader;
	public File inputFile;
	public File audioFile;
	public File outputDir;
	public List<String> outputFormats;
	public boolean clearTimes = false;


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
		Set<String> names = MarkupLoaderPool.getLoaderNames();

		System.out.println("Vanilla markup loaders:");
		for (String name: names) {
			if (MarkupLoaderPool.isVanillaLoader(name)) {
				System.out.println("    " + name);
			}
		}

		System.out.println("Preprocessors:");
		for (String name: names) {
			if (!MarkupLoaderPool.isVanillaLoader(name)) {
				System.out.println("    " + name);
			}
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

				accepts("outfmt", "output format (you may use this argument " +
						"several times to output to several different formats)")
						.withRequiredArg()
						.describedAs("jtr, praatw, praatp, praatwp");

				acceptsAll(
						Arrays.asList("infmt", "input-format"),
						"input format (if omitted, guess from filename extension)")
						.withRequiredArg().describedAs("markup loader class");

				accepts("list-infmt",
						"Displays a list of markup loaders to use with --infmt")
						.forHelp();
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

		//----------------------------------------------------------------------

		inputFile = (File)optset.valueOf("f");
		audioFile = (File)optset.valueOf("a");
		outputDir = (File)optset.valueOf("outdir");

		if (optset.has("infmt")) {
			String className = (String)optset.valueOf("infmt");
			loader = MarkupLoaderPool.newLoader(className);
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


	public static void main(String args[]) throws Exception {
		loadLoggingProperties();

		JTransCLI cli = new JTransCLI(args);

		if (!new File("res").exists()) {
			JTransGUI.installResources();
		}

		if (cli.outputFormats == null || cli.outputFormats.isEmpty()) {
			CrossPlatformFixes.setNativeLookAndFeel();
			new JTransGUI(cli);
			return;
		}

		Project project = cli.loader.parse(cli.inputFile);
		project.setAudio(cli.audioFile);
		project.align(true, null);

		System.out.println("Alignment done.");

		cli.outputDir.mkdirs();

		for (String fmt: cli.outputFormats) {
			System.out.println("Output: format '" + fmt + "' to directory "
					+ cli.outputDir);

			fmt = fmt.toLowerCase();
			String base = FileUtils.noExt(new File(cli.outputDir,
					cli.inputFile.getName()).getAbsolutePath());

			if (fmt.equals("jtr")) {
				project.saveJson(new File(base + ".jtr"));
			} else if (fmt.equals("praatw")) {
				project.savePraat(new File(base + ".w.textgrid"), true, false);
			} else if (fmt.equals("praatp")) {
				project.savePraat(new File(base + ".p.textgrid"), false, true);
			} else if (fmt.equals("praatwp") || fmt.equals("praatpw")) {
				project.savePraat(new File(base + ".w+p.textgrid"), true, true);
			} else {
				throw new IllegalArgumentException("Unknown output format " + fmt);
			}
		}
	}

}
