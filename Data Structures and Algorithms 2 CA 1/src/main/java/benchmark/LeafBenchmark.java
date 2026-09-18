package benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import utils.ImageProcessor;
import utils.LeafCluster;
import utils.MergeSort;
import utils.UnionFind;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH micro-benchmarks for the performance-critical methods of the
 * Autumn Leaves Identification application.
 *
 * Benchmarked methods:
 *  1. {@link UnionFind#find}     — hot path in union-find loop
 *  2. {@link UnionFind#union}    — merging adjacent pixel sets
 *  3. Full union-find over a simulated image grid
 *  4. {@link MergeSort#sortBySizeDesc}
 *  5. {@link ImageProcessor#applyIQRFilter}
 *
 * Run with:
 *   mvn verify  (if pom.xml has jmh-maven-plugin configured)
 *  OR
 *   java -jar target/benchmarks.jar
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class LeafBenchmark {

    // =====================================================================
    // BENCHMARK STATE
    // =====================================================================

    /** Side length of the simulated image grid (512 × 512 = 262 144 pixels). */
    @Param({"256", "512", "1024"})
    private int imageSize;

    private UnionFind          uf;
    private boolean[][]        whiteMask;
    private List<LeafCluster>  unsortedClusters;
    private List<LeafCluster>  clustersForIQR;

    /** Pixel indices used repeatedly in find benchmarks. */
    private int[]              randomIndices;

    @Setup(Level.Invocation)
    public void setUp() {
        int n = imageSize * imageSize;

        // ---- UnionFind ----
        uf = new UnionFind(n);

        // ---- White mask: ~50 % of pixels are "white" ----
        Random rng = new Random(0xDEADBEEF);
        whiteMask = new boolean[imageSize][imageSize];
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                whiteMask[y][x] = rng.nextBoolean();
            }
        }

        // ---- Random index array for find benchmarks ----
        randomIndices = new int[1000];
        for (int i = 0; i < randomIndices.length; i++) {
            randomIndices[i] = rng.nextInt(n);
        }

        // ---- Clusters for sort benchmark (500 random sizes) ----
        unsortedClusters = new ArrayList<>(500);
        for (int i = 0; i < 500; i++) {
            LeafCluster c = new LeafCluster(i);
            int sz = 10 + rng.nextInt(5000);
            for (int p = 0; p < sz; p++) c.addPixel(p, 0);
            unsortedClusters.add(c);
        }

        // ---- Clusters for IQR filter benchmark ----
        clustersForIQR = new ArrayList<>(unsortedClusters);
        // Add a few extreme outliers
        for (int i = 500; i < 510; i++) {
            LeafCluster outlier = new LeafCluster(i);
            for (int p = 0; p < 500_000; p++) outlier.addPixel(p, 0);
            clustersForIQR.add(outlier);
        }
    }

    // =====================================================================
    // 1. UnionFind.find  (path compression hot-path)
    // =====================================================================

    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    public int benchmarkFind() {
        int result = 0;
        for (int idx : randomIndices) {
            result ^= uf.find(idx);  // XOR to prevent dead-code elimination
        }
        return result;
    }

    // =====================================================================
    // 2. UnionFind.union  (simulated adjacent unions)
    // =====================================================================

    @Benchmark
    public int benchmarkUnion() {
        // Re-create a fresh UF every invocation (setUp is called per invocation)
        UnionFind fresh = new UnionFind(imageSize * imageSize);
        int merges = 0;
        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize - 1; x++) {
                if (fresh.union(y * imageSize + x, y * imageSize + x + 1)) merges++;
            }
        }
        return merges;
    }

    // =====================================================================
    // 3. Full grid union-find (4-connectivity, white pixels only)
    // =====================================================================

    @Benchmark
    public int benchmarkFullGridUnionFind() {
        UnionFind local = new UnionFind(imageSize * imageSize);
        int unions = 0;

        for (int y = 0; y < imageSize; y++) {
            for (int x = 0; x < imageSize; x++) {
                if (!whiteMask[y][x]) continue;
                int idx = y * imageSize + x;

                if (x + 1 < imageSize && whiteMask[y][x + 1]) {
                    if (local.union(idx, y * imageSize + (x + 1))) unions++;
                }
                if (y + 1 < imageSize && whiteMask[y + 1][x]) {
                    if (local.union(idx, (y + 1) * imageSize + x)) unions++;
                }
            }
        }

        return unions;
    }

    // =====================================================================
    // 4. MergeSort  (500 clusters, descending by size)
    // =====================================================================

    @Benchmark
    public int benchmarkMergeSort() {
        // Work on a copy so the state is reset each measurement
        List<LeafCluster> copy = new ArrayList<>(unsortedClusters);
        MergeSort.sortBySizeDesc(copy);
        return copy.get(0).getSize();  // prevent DCE
    }

    // =====================================================================
    // 5. IQR Filter
    // =====================================================================

    @Benchmark
    public int benchmarkIQRFilter() {
        List<LeafCluster> filtered = ImageProcessor.applyIQRFilter(clustersForIQR);
        return filtered.size();
    }

    // =====================================================================
    // MAIN  —  run from IDE or CLI
    // =====================================================================

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LeafBenchmark.class.getSimpleName())
                .warmupIterations(3)
                .measurementIterations(5)
                .forks(1)
                .build();
        new Runner(opt).run();
    }
}
