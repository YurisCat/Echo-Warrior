import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Builds the 16x16 source textures for Echo Warrior's brushable grass and dirt.
 *
 * Usage:
 *   java GenerateSuspiciousBlockTextures.java <minecraft-client.jar> <output-directory>
 *
 * The generator keeps the biome-tinted vanilla grass structure, then draws an original
 * broken echo-ring and increasingly exposed brush marks over it. Keeping this source file
 * beside the project makes the placeholder art reproducible without committing vanilla inputs.
 */
public final class GenerateSuspiciousBlockTextures {
	private static final int[][] ECHO_RING = {
			{5, 3}, {6, 3}, {9, 4}, {10, 5}, {11, 6}, {11, 7}, {10, 9},
			{9, 10}, {7, 11}, {6, 10}, {4, 9}, {4, 7}, {5, 6}, {7, 5},
			{8, 5}, {9, 6}, {9, 7}, {8, 8}, {7, 8}
	};
	private static final int[][] RING_HIGHLIGHTS = {
			{4, 3}, {7, 3}, {10, 4}, {12, 6}, {11, 9}, {8, 11}, {5, 10},
			{3, 8}, {5, 5}, {7, 4}, {10, 7}, {9, 9}, {6, 9}
	};
	private static final int[][] STAGE_ONE = {
			{2, 5}, {3, 5}, {12, 3}, {12, 4}, {13, 10}, {12, 11}, {3, 12}, {4, 12}
	};
	private static final int[][] STAGE_TWO = {
			{1, 5}, {2, 6}, {3, 6}, {12, 2}, {13, 3}, {13, 4}, {14, 10},
			{13, 11}, {12, 12}, {2, 12}, {3, 13}, {4, 13}, {8, 1}, {8, 2}
	};
	private static final int[][] STAGE_THREE = {
			{0, 5}, {1, 6}, {2, 7}, {14, 3}, {14, 4}, {15, 4}, {15, 10},
			{14, 11}, {13, 12}, {12, 13}, {2, 13}, {3, 14}, {8, 0}, {9, 1},
			{6, 13}, {6, 14}, {5, 15}
	};

	private GenerateSuspiciousBlockTextures() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			throw new IllegalArgumentException("Expected <minecraft-client.jar> <output-directory>");
		}
		Path clientJar = Path.of(args[0]);
		Path output = Path.of(args[1]);
		Files.createDirectories(output);

		try (ZipFile zip = new ZipFile(clientJar.toFile())) {
			BufferedImage dirt = readTexture(zip, "dirt");
			BufferedImage grassTop = readTexture(zip, "grass_block_top");
			BufferedImage grassSide = readTexture(zip, "grass_block_side");
			for (int stage = 0; stage < 4; stage++) {
				write(mark(dirt, stage, Surface.DIRT), output.resolve("suspicious_dirt_" + stage + ".png"));
				write(mark(grassTop, stage, Surface.GRASS_TOP),
						output.resolve("suspicious_grass_block_top_" + stage + ".png"));
				write(mark(grassSide, stage, Surface.GRASS_SIDE),
						output.resolve("suspicious_grass_block_side_" + stage + ".png"));
			}
		}
	}

	private static BufferedImage readTexture(ZipFile zip, String name) throws IOException {
		ZipEntry entry = zip.getEntry("assets/minecraft/textures/block/" + name + ".png");
		if (entry == null) throw new IOException("Missing vanilla texture: " + name);
		try (InputStream stream = zip.getInputStream(entry)) {
			BufferedImage image = ImageIO.read(stream);
			if (image == null || image.getWidth() != 16 || image.getHeight() != 16) {
				throw new IOException("Expected a 16x16 texture: " + name);
			}
			return image;
		}
	}

	private static BufferedImage mark(BufferedImage base, int stage, Surface surface) {
		BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++) {
			for (int x = 0; x < 16; x++) result.setRGB(x, y, base.getRGB(x, y));
		}

		List<int[]> dark = new ArrayList<>(List.of(ECHO_RING));
		if (stage >= 1) dark.addAll(List.of(STAGE_ONE));
		if (stage >= 2) dark.addAll(List.of(STAGE_TWO));
		if (stage >= 3) dark.addAll(List.of(STAGE_THREE));
		int darkAmount = -38 - stage * 8;
		for (int[] point : dark) shade(result, transform(point, surface), darkAmount, surface, true);
		for (int[] point : RING_HIGHLIGHTS) {
			shade(result, transform(point, surface), 24 + stage * 3, surface, false);
		}
		return result;
	}

	private static int[] transform(int[] point, Surface surface) {
		if (surface == Surface.GRASS_SIDE) {
			// Keep most of the readable mark below the biome-tinted grass fringe.
			return new int[]{point[0], Math.min(15, point[1] + 2)};
		}
		return point;
	}

	private static void shade(
			BufferedImage image,
			int[] point,
			int amount,
			Surface surface,
			boolean accent
	) {
		int x = point[0];
		int y = point[1];
		if (x < 0 || x >= 16 || y < 0 || y >= 16) return;
		Color source = new Color(image.getRGB(x, y), true);
		int red = clamp(source.getRed() + amount);
		int green = clamp(source.getGreen() + amount);
		int blue = clamp(source.getBlue() + amount);
		if (accent && surface != Surface.GRASS_TOP) {
			double blend = 0.28;
			red = (int)Math.round(red * (1.0 - blend) + 88 * blend);
			green = (int)Math.round(green * (1.0 - blend) + 55 * blend);
			blue = (int)Math.round(blue * (1.0 - blend) + 104 * blend);
		}
		image.setRGB(x, y, new Color(red, green, blue, source.getAlpha()).getRGB());
	}

	private static int clamp(int value) {
		return Math.max(0, Math.min(255, value));
	}

	private static void write(BufferedImage image, Path output) throws IOException {
		ImageIO.write(image, "png", output.toFile());
	}

	private enum Surface {
		DIRT,
		GRASS_TOP,
		GRASS_SIDE
	}
}
