import javax.imageio.ImageIO;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Builds the deterministic visual variants for Echo Warrior's brushable grass and dirt.
 *
 * Usage:
 *   java GenerateSuspiciousBlockTextures.java <minecraft-client.jar> <mark-directory> <resource-root>
 *
 * Five artist-authored 16x16 marks produce ten unordered pairs across four brushing stages.
 * Blockstate weighted models reuse each generated model in four vanilla Y rotations, selecting
 * one of forty visual identities from the block-position seed without saved data.
 */
public final class GenerateSuspiciousBlockTextures {
	private static final int SIZE = 16;
	private static final int MARK_COUNT = 5;
	private static final int STAGE_COUNT = 4;
	private static final int MODEL_ROTATION_COUNT = 4;
	private static final double GLOBAL_MARK_OPACITY = 0.5D;
	private static final int GRASS_SIDE_Y_OFFSET = 2;
	private static final int ITEM_COMBINATION = 5; // Marks 2 + 4 in lexicographic pair order.
	private static final String MOD_ID = "echo_warrior";

	private GenerateSuspiciousBlockTextures() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			throw new IllegalArgumentException(
					"Expected <minecraft-client.jar> <mark-directory> <resource-root>");
		}

		Path clientJar = Path.of(args[0]);
		Path markDirectory = Path.of(args[1]);
		Path resourceRoot = Path.of(args[2]);
		Path textureOutput = resourceRoot.resolve("textures/block");
		Path blockModelOutput = resourceRoot.resolve("models/block");
		Path itemModelOutput = resourceRoot.resolve("models/item");
		Path blockstateOutput = resourceRoot.resolve("blockstates");
		for (Path directory : List.of(textureOutput, blockModelOutput, itemModelOutput, blockstateOutput)) {
			Files.createDirectories(directory);
		}

		List<BufferedImage> marks = readMarks(markDirectory);
		try (ZipFile zip = new ZipFile(clientJar.toFile())) {
			BufferedImage dirt = readTexture(zip, "dirt");
			BufferedImage grassTop = readTexture(zip, "grass_block_top");
			BufferedImage grassSide = readTexture(zip, "grass_block_side");
			BufferedImage[] wearStages = new BufferedImage[STAGE_COUNT];
			for (int stage = 0; stage < STAGE_COUNT; stage++) {
				wearStages[stage] = readTexture(zip, "suspicious_sand_" + stage);
			}

			cleanGeneratedFiles(textureOutput, GenerateSuspiciousBlockTextures::isGeneratedTexture);
			cleanGeneratedFiles(blockModelOutput, GenerateSuspiciousBlockTextures::isGeneratedBlockModel);

			int combination = 0;
			for (int first = 0; first < marks.size(); first++) {
				for (int second = first + 1; second < marks.size(); second++) {
					for (int stage = 0; stage < STAGE_COUNT; stage++) {
						String suffix = suffix(combination, stage);
						BufferedImage wear = wearStages[stage];
						write(compose(dirt, marks.get(first), marks.get(second), wear,
								wearStages[0], Surface.DIRT), textureOutput.resolve("suspicious_dirt_" + suffix + ".png"));
						write(compose(grassTop, marks.get(first), marks.get(second), wear,
								wearStages[0], Surface.GRASS_TOP), textureOutput.resolve(
								"suspicious_grass_block_top_" + suffix + ".png"));
						write(compose(grassSide, marks.get(first), marks.get(second), wear,
								wearStages[0], Surface.GRASS_SIDE), textureOutput.resolve(
								"suspicious_grass_block_side_" + suffix + ".png"));
						writeDirtModel(blockModelOutput, suffix);
						writeGrassModel(blockModelOutput, suffix);
					}
					combination++;
				}
			}
			if (combination != 10) {
				throw new IllegalStateException("Expected ten mark combinations, generated " + combination);
			}
		}

		writeBlockstate(blockstateOutput, "suspicious_dirt");
		writeBlockstate(blockstateOutput, "suspicious_grass_block");
		writeItemModel(itemModelOutput, "suspicious_dirt");
		writeItemModel(itemModelOutput, "suspicious_grass_block");
	}

	private static List<BufferedImage> readMarks(Path directory) throws IOException {
		List<BufferedImage> marks = new ArrayList<>(MARK_COUNT);
		for (int index = 1; index <= MARK_COUNT; index++) {
			Path path = directory.resolve("suspicious_mark_" + index + ".png");
			BufferedImage image = ImageIO.read(path.toFile());
			validateTexture(image, path.toString());
			marks.add(normalize(image));
		}
		return marks;
	}

	private static BufferedImage readTexture(ZipFile zip, String name) throws IOException {
		ZipEntry entry = zip.getEntry("assets/minecraft/textures/block/" + name + ".png");
		if (entry == null) {
			throw new IOException("Missing vanilla texture: " + name);
		}
		try (InputStream stream = zip.getInputStream(entry)) {
			BufferedImage image = ImageIO.read(stream);
			validateTexture(image, name);
			return normalize(image);
		}
	}

	private static void validateTexture(BufferedImage image, String name) throws IOException {
		if (image == null || image.getWidth() != SIZE || image.getHeight() != SIZE) {
			throw new IOException("Expected a 16x16 texture: " + name);
		}
	}

	private static BufferedImage compose(BufferedImage base, BufferedImage first, BufferedImage second,
			BufferedImage wearStage, BufferedImage wearBase, Surface surface) {
		BufferedImage result = copy(base);
		applyMark(result, first, surface);
		applyMark(result, second, surface);
		applyWear(result, wearBase, wearStage);
		verifyNoBrightening(base, result);
		return result;
	}

	private static void verifyNoBrightening(BufferedImage base, BufferedImage result) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				int baseColor = base.getRGB(x, y);
				int resultColor = result.getRGB(x, y);
				for (int shift : new int[]{16, 8, 0}) {
					int baseComponent = (baseColor >>> shift) & 0xFF;
					int resultComponent = (resultColor >>> shift) & 0xFF;
					if (resultComponent > baseComponent) {
						throw new IllegalStateException("Generated texture brightened pixel " + x + "," + y);
					}
				}
			}
		}
	}

	private static BufferedImage normalize(BufferedImage source) {
		BufferedImage result = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		ColorModel colorModel = source.getColorModel();
		Raster raster = source.getRaster();
		Raster alphaRaster = colorModel.hasAlpha() ? source.getAlphaRaster() : null;
		boolean rawGray = colorModel.getNumColorComponents() == 1
				&& colorModel.getColorSpace().getType() == ColorSpace.TYPE_GRAY;
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (rawGray) {
					int gray = raster.getSample(x, y, 0);
					int alpha = alphaRaster == null ? 255 : alphaRaster.getSample(x, y, 0);
					result.setRGB(x, y, alpha << 24 | gray << 16 | gray << 8 | gray);
				} else {
					result.setRGB(x, y, source.getRGB(x, y));
				}
			}
		}
		return result;
	}

	private static BufferedImage copy(BufferedImage source) {
		return normalize(source);
	}

	private static void applyMark(BufferedImage target, BufferedImage mark, Surface surface) {
		for (int sourceY = 0; sourceY < SIZE; sourceY++) {
			for (int sourceX = 0; sourceX < SIZE; sourceX++) {
				int markColor = mark.getRGB(sourceX, sourceY);
				int alpha = markColor >>> 24;
				if (alpha == 0) continue;
				int targetX = sourceX;
				int targetY = sourceY + (surface == Surface.GRASS_SIDE ? GRASS_SIDE_Y_OFFSET : 0);
				if (targetX < 0 || targetX >= SIZE || targetY < 0 || targetY >= SIZE) continue;

				double opacity = alpha / 255.0D * GLOBAL_MARK_OPACITY;
				int source = target.getRGB(targetX, targetY);
				int red = multiply((source >>> 16) & 0xFF, (markColor >>> 16) & 0xFF, opacity);
				int green = multiply((source >>> 8) & 0xFF, (markColor >>> 8) & 0xFF, opacity);
				int blue = multiply(source & 0xFF, markColor & 0xFF, opacity);
				target.setRGB(targetX, targetY, source & 0xFF000000 | red << 16 | green << 8 | blue);
			}
		}
	}

	private static int multiply(int base, int mark, double opacity) {
		double multiplier = 1.0D - opacity + opacity * mark / 255.0D;
		return clamp((int)Math.round(base * multiplier));
	}

	private static void applyWear(BufferedImage target, BufferedImage wearBase, BufferedImage wearStage) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				int darkening = Math.max(0, luminance(wearBase, x, y) - luminance(wearStage, x, y));
				if (darkening == 0) continue;
				int source = target.getRGB(x, y);
				int red = clamp(((source >>> 16) & 0xFF) - darkening);
				int green = clamp(((source >>> 8) & 0xFF) - darkening);
				int blue = clamp((source & 0xFF) - darkening);
				target.setRGB(x, y, source & 0xFF000000 | red << 16 | green << 8 | blue);
			}
		}
	}

	private static int luminance(BufferedImage image, int x, int y) {
		int color = image.getRGB(x, y);
		int red = (color >>> 16) & 0xFF;
		int green = (color >>> 8) & 0xFF;
		int blue = color & 0xFF;
		return (int)Math.round(red * 0.2126D + green * 0.7152D + blue * 0.0722D);
	}

	private static int clamp(int value) {
		return Math.max(0, Math.min(255, value));
	}

	private static String suffix(int combination, int stage) {
		return String.format(Locale.ROOT, "c%02d_s%d", combination, stage);
	}

	private static void writeDirtModel(Path output, String suffix) throws IOException {
		String json = """
				{
				  "parent": "minecraft:block/cube_all",
				  "textures": {
				    "all": "echo_warrior:block/suspicious_dirt_%s"
				  }
				}
				""".formatted(suffix);
		writeText(output.resolve("suspicious_dirt_" + suffix + ".json"), json);
	}

	private static void writeGrassModel(Path output, String suffix) throws IOException {
		String json = """
				{
				  "parent": "minecraft:block/grass_block",
				  "textures": {
				    "particle": "echo_warrior:block/suspicious_dirt_%s",
				    "bottom": "echo_warrior:block/suspicious_dirt_%s",
				    "top": "echo_warrior:block/suspicious_grass_block_top_%s",
				    "side": "echo_warrior:block/suspicious_grass_block_side_%s"
				  }
				}
				""".formatted(suffix, suffix, suffix, suffix);
		writeText(output.resolve("suspicious_grass_block_" + suffix + ".json"), json);
	}

	private static void writeBlockstate(Path output, String blockName) throws IOException {
		StringBuilder json = new StringBuilder("{\n  \"variants\": {\n");
		for (int stage = 0; stage < STAGE_COUNT; stage++) {
			json.append("    \"dusted=").append(stage).append("\": [\n");
			int entry = 0;
			for (int combination = 0; combination < 10; combination++) {
				for (int rotation = 0; rotation < MODEL_ROTATION_COUNT; rotation++) {
					json.append("      { \"model\": \"").append(MOD_ID).append(":block/")
							.append(blockName).append('_').append(suffix(combination, stage)).append("\"");
					if (rotation != 0) json.append(", \"y\": ").append(rotation * 90);
					json.append(" }");
					entry++;
					json.append(entry == 40 ? '\n' : ",\n");
				}
			}
			json.append("    ]");
			json.append(stage == STAGE_COUNT - 1 ? '\n' : ",\n");
		}
		json.append("  }\n}\n");
		writeText(output.resolve(blockName + ".json"), json.toString());
	}

	private static void writeItemModel(Path output, String blockName) throws IOException {
		String json = """
				{
				  "parent": "echo_warrior:block/%s_%s"
				}
				""".formatted(blockName, suffix(ITEM_COMBINATION, 0));
		writeText(output.resolve(blockName + ".json"), json);
	}

	private static boolean isGeneratedTexture(Path path) {
		String name = path.getFileName().toString();
		return name.matches("suspicious_dirt_(?:[0-3]|c\\d{2}(?:_r\\d)?_s\\d)\\.png")
				|| name.matches("suspicious_grass_block_(?:top|side)_(?:[0-3]|c\\d{2}(?:_r\\d)?_s\\d)\\.png");
	}

	private static boolean isGeneratedBlockModel(Path path) {
		String name = path.getFileName().toString();
		return name.matches("suspicious_(?:dirt|grass_block)_(?:[0-3]|c\\d{2}(?:_r\\d)?_s\\d)\\.json");
	}

	private static void cleanGeneratedFiles(Path directory, Predicate<Path> predicate) throws IOException {
		try (var files = Files.list(directory)) {
			for (Path path : files.filter(Files::isRegularFile).filter(predicate).toList()) {
				Files.delete(path);
			}
		}
	}

	private static void write(BufferedImage image, Path output) throws IOException {
		if (!ImageIO.write(image, "png", output.toFile())) {
			throw new IOException("No PNG writer available for " + output);
		}
	}

	private static void writeText(Path output, String value) throws IOException {
		Files.writeString(output, value, StandardCharsets.UTF_8);
	}

	private enum Surface {
		DIRT,
		GRASS_TOP,
		GRASS_SIDE
	}
}
